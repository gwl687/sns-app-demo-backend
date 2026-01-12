package com.gwl.service.Impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.checkerframework.checker.units.qual.t;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.gwl.constant.BaseConstant;
import com.gwl.constant.MailConstant;
import com.gwl.constant.MessageConstant;
import com.gwl.constant.StatusConstant;
import com.gwl.context.BaseContext;
import com.gwl.exception.BaseException;
import com.gwl.mapper.FriendMapper;
import com.gwl.mapper.UserMapper;
import com.gwl.pojo.dto.AddFriendToChatListDTO;
import com.gwl.pojo.dto.CreateGroupChatDTO;
import com.gwl.pojo.dto.GoogleLoginDto;
import com.gwl.pojo.dto.RegisterDTO;
import com.gwl.pojo.dto.UserInfoDTO;
import com.gwl.pojo.dto.UserLoginDTO;
import com.gwl.pojo.entity.ChatFriend;
import com.gwl.pojo.entity.ChatListId;
import com.gwl.pojo.entity.GroupChat;
import com.gwl.pojo.entity.Message;
import com.gwl.pojo.entity.TimelinePushEvent;
import com.gwl.pojo.entity.UpdateUserInfoPushEvent;
import com.gwl.pojo.entity.User;
import com.gwl.pojo.vo.GroupChatVO;
import com.gwl.pojo.vo.SearchForUserVO;
import com.gwl.pojo.vo.UserLoginVO;
import com.gwl.properties.MailProperties;
import com.gwl.service.CommonService;
import com.gwl.service.UserService;
import com.gwl.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedis;

    @Autowired
    private CommonService commonService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MailProperties mailProperties;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${google.web-client-id}")
    private String googleWebClientId;

    @Override
    public User userLogin(UserLoginDTO userLoginDTO) {
        String emailaddress = userLoginDTO.getEmailaddress();
        String password = userLoginDTO.getPassword();
        User user = userMapper.getByUserEmail(emailaddress);
        if (user == null) {
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 密码比对
        if (!password.equals(user.getPassword())) {
            // 密码错误
            throw new BaseException(MessageConstant.PASSWORD_ERROR);
        }
        if (user.getStatus() == StatusConstant.DISABLE) {
            // 账号被锁定
            throw new BaseException(MessageConstant.ACCOUNT_LOCKED);
        }
        // device token
        stringRedis.opsForValue().set("push_token:" + user.getId(), userLoginDTO.getPushToken());
        return user;
    }

    @Override
    public UserLoginVO googleLogin(GoogleLoginDto googleLoginDto) {
        GoogleIdToken.Payload payload;
        UserLoginVO userLoginVO = new UserLoginVO();
        // s校验 Google id_token
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory())
                .setAudience(List.of(googleWebClientId))
                .build();
        try {
            GoogleIdToken idToken = verifier.verify(googleLoginDto.getIdTokenString());
            log.info("idToken={}", idToken);
            payload = idToken.getPayload();
            // String googleSub = payload.getSubject();
            String email = payload.getEmail();
            // boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            // 查/注册用户
            User user = userMapper.getByUserEmail(email);
            String userName = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            if (user == null) {
                user = User.builder()
                        .emailaddress(email)
                        .username(userName)
                        .status(1)
                        .avatarurl(BaseConstant.DEFAULTAVATARURL)
                        .build();
                userMapper.insert(user);
            }
            // 签发JWT
            String token = JwtUtil.generateToken(user.getId());
            userLoginVO = UserLoginVO.builder()
                    .id(user.getId())
                    .token(token)
                    .emailaddress(user.getEmailaddress())
                    .userName(user.getUsername())
                    .avatarUrl(user.getAvatarurl()).build();
            // device token
            stringRedis.opsForValue().set("push_token:" + user.getId(), googleLoginDto.getPushToken());
            log.info("google login success ,id = {}", user.getId());
            return userLoginVO;
        } catch (Exception e) {
            log.error("google login failed", e);
        }
        return userLoginVO;
    }

    /**
     * 发送验证码
     * 
     * @param emailaddress
     */
    @Override
    public void sendVerificationCode(String emailaddress) {
        User user = userMapper.getByUserEmail(emailaddress);
        // 用户已注册
        if (user != null) {
            throw new BaseException(MessageConstant.EMAIL_REGISTERD);
        }
        // 60秒以内不能重复发给同一邮箱
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey("verifycodeCd:" + emailaddress))) {
            throw new BaseException(MessageConstant.REPEAT_SENDVERITY_REQUEST);
        }
        // generate token
        Random random = new Random();
        int num = random.nextInt(1_000_000); // 0 - 999999
        String verifyCode = String.format("%06d", num);
        // save verification code to redis
        stringRedisTemplate.opsForValue().set("verifycode:" + emailaddress, verifyCode, 3, TimeUnit.MINUTES);
        // save cooldown to redis
        stringRedisTemplate.opsForValue().set("verifycodeCd:" + emailaddress, verifyCode, 60, TimeUnit.SECONDS);
        // send mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getSenderEmailaddress()); // sernder emailaddress
        message.setTo(emailaddress); // accepter
        message.setSubject(MailConstant.VERIFICODE); // topic
        message.setText(verifyCode); // context mailSender.send(message);
        mailSender.send(message);
    }

    /**
     * 注册
     * 
     * @param registerDTO
     */
    @Override
    public void register(RegisterDTO registerDTO) {
        // 判断邮箱是否已注册
        User user = userMapper.getByUserEmail(registerDTO.getEmailaddress());
        if (user != null) {
            throw new BaseException(MessageConstant.EMAIL_REGISTERD);
        }
        // 判断验证码是否正确
        String verifyCode = stringRedisTemplate.opsForValue().get("verifycode:" + registerDTO.getEmailaddress());
        String receivedVerifyCode = registerDTO.getVerificationCode();
        if (verifyCode == null || !verifyCode.equals(receivedVerifyCode)) {
            throw new BaseException(MessageConstant.VERIFICATION_WRONG);
        }
        // 把新用户数据保存进mysql
        String userName = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        user = User.builder()
                .emailaddress(registerDTO.getEmailaddress())
                .password(registerDTO.getPassword())
                .username(userName)
                .avatarurl(BaseConstant.DEFAULTAVATARURL)
                .status(1)
                .build();
        userMapper.insert(user);
    }

    /**
     * 改名
     * 
     * @param registerDTO
     */
    @Override
    public void changeUsername(String userName) {
        // 改自己名字
        userMapper.changeUsername(userName, BaseContext.getCurrentId());
        // 改和自己相关的群名
        List<Long> myGroupIds = userMapper.getMyGroupIds(BaseContext.getCurrentId());
        for (Long myGroupId : myGroupIds) {
            List<String> memberNames = friendMapper.getGroupMemberNames(myGroupId);
            String newGroupName = String.join(",", memberNames);
            friendMapper.changeGroupName(myGroupId, newGroupName);
        }
        // 推送给好友
        List<Long> friendIds = userMapper.getMyFriendIds(BaseContext.getCurrentId());
        // 也要推给自己
        friendIds.add(BaseContext.getCurrentId());
        UpdateUserInfoPushEvent event = UpdateUserInfoPushEvent.builder()
                .fromUser(BaseContext.getCurrentId())
                .friendIds(friendIds).build();
        log.info("用户 {} 改名", BaseContext.getCurrentId());
        kafkaTemplate.send(
                "update_user_info",
                event);
    }

    /**
     * 上传新头像
     */
    @Override
    public void uploadAvatar(MultipartFile file) {
        String uploadKey = "avatar/" + BaseContext.getCurrentId() + "_" + System.currentTimeMillis();
        commonService.uploadToS3(file, uploadKey);
        // 推送给好友
        List<User> myFriends = friendMapper.getFriendListByUserId(BaseContext.getCurrentId());
        List<Long> friendIds = myFriends.stream()
                .map(User::getId)
                .toList();
        UpdateUserInfoPushEvent event = UpdateUserInfoPushEvent.builder()
                .fromUser(BaseContext.getCurrentId())
                .friendIds(friendIds).build();
        kafkaTemplate.send(
                "update_user_info",
                event);
    }

    /**
     * 更新用户信息的推送
     * 
     * @param event
     */
    @Override
    @KafkaListener(topics = "update_user_info", groupId = "update_user_info_group")
    public void onUpdateUserInfoPush(@Payload UpdateUserInfoPushEvent event) {
        log.info("kafka收到更新用户信息的推送，开始推送给好友");
        for (Long friendId : event.getFriendIds()) {
            commonService.sendPush(friendId, event.getFromUser(), "", "",
                    "friendinfochange", true);
        }
    }

    /**
     * 获取用户信息
     * 
     * @return
     */
    public User getUserInfo() {
        return userMapper.getByUserId(BaseContext.getCurrentId());
    }

    /**
     * 根据id获取用户信息
     * 
     * @return
     */
    @Override
    public User getUserInfoById(Long userId) {
        return userMapper.getByUserId(userId);
    }

    /**
     * 更新用户信息
     * 
     * @return
     */
    public Boolean updateUserInfo(UserInfoDTO userInfoDTO) {
        Boolean result = userMapper.updateUserInfo(userInfoDTO) > 0 ? true : false;
        return result;
    }

    /*
     * 添加朋友或群到聊天列表
     */
    @Override
    public void addFriendToChatList(AddFriendToChatListDTO addFriendToChatListDTO) {
        userMapper.addFriendToChatList(addFriendToChatListDTO);
    }

    /**
     * 获取聊天列表
     */
    @Override
    public List<?> getChatList() {
        // 获取好友的，和群的，然后放入同一个list返回
        List<Object> result = new ArrayList<>();
        List<ChatListId> chatListIds = userMapper.getChatListIdById(BaseContext.getCurrentId());
        for (ChatListId chatListId : chatListIds) {
            // 如果是群
            if (chatListId.getIsGroup()) {
                GroupChat groupChat = userMapper.getChatGroupByChatId(chatListId.getId());
                List<Long> memberIds = Arrays.stream(groupChat.getMemberIds().split(","))
                        .map(Long::valueOf)
                        .toList();
                GroupChatVO groupChatVO = GroupChatVO.builder()
                        .groupId(groupChat.getGroupId())
                        .groupName(groupChat.getGroupName())
                        .ownerId(groupChat.getOwnerId())
                        .memberIds(memberIds)
                        // 先传默认头像,测试用
                        .avatarUrl("https://i.pravatar.cc/150?img=3")
                        // .avatarUrl(groupChat.getAvatarUrl())
                        .build();
                result.add(groupChatVO);
            } else { // 是好友
                ChatFriend chatFriend = userMapper.getChatFriendByChatId(chatListId.getId());
                // 先传默认头像,测试用
                // chatFriend.setAvatarurl("https://i.pravatar.cc/150?img=3");
                result.add(chatFriend);
            }
        }
        return result;
    }

    /**
     * 创建群聊
     */
    @Override
    public GroupChatVO createGroupChat(CreateGroupChatDTO createGroupChatDTO) {
        List<Long> groupChatMembers = createGroupChatDTO.getSelectedFriends();
        List<String> groupNameList = new ArrayList<>();
        User owner = userMapper.getByUserId(BaseContext.getCurrentId());
        groupNameList.add(owner.getUsername());
        for (Long memberId : groupChatMembers) {
            com.gwl.pojo.entity.User groupMember = userMapper.getByUserId(memberId);
            groupNameList.add(groupMember.getUsername());
        }
        String groupName = String.join(",", groupNameList);
        String groupChatMembersStr = groupChatMembers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        GroupChat groupchat = GroupChat.builder()
                .memberIds(groupChatMembersStr)
                .ownerId(BaseContext.getCurrentId())
                .groupName(groupName)
                .avatarUrl("https://i.pravatar.cc/150?img=1")
                .build();
        groupChatMembers.add(BaseContext.getCurrentId());
        userMapper.createGroupChat(groupchat);
        GroupChatVO groupChatVO = GroupChatVO.builder()
                .groupId(groupchat.getGroupId())
                .memberIds(groupChatMembers)
                .ownerId(groupchat.getOwnerId())
                .groupName(groupchat.getGroupName())
                .avatarUrl(groupchat.getAvatarUrl())
                .build();
        userMapper.insertGroupChatMember(groupChatVO);
        // 添加每个群成员的聊天列表
        for (Long userId : groupChatMembers) {
            AddFriendToChatListDTO addFriendToChatListDTO = AddFriendToChatListDTO.builder()
                    .userId(userId)
                    .friendId(groupchat.getGroupId())
                    .isGroup(true).build();
            userMapper.addFriendToChatList(addFriendToChatListDTO);
        }

        // 推送给群组成员
        for (Long id : groupChatMembers) {
            commonService.sendPush(id, BaseContext.getCurrentId(), "chatgroup invite",
                    owner.getUsername() + "invite you to join a chat group", "joingroup", false);
        }
        log.info("用户" + BaseContext.getCurrentId() + "创建群聊");
        return groupChatVO;
    }

    /**
     * 获取群信息
     */
    @Override
    public GroupChat getGroupChat(Long groupId) {
        GroupChat groupChat = userMapper.getGroupChat(groupId);
        // for (Long id : groupChat.getMemberIds()) {
        // System.out.println("群成员id为: " + id);
        // }
        return groupChat;
    }

    /**
     * 发送群消息
     */
    @Override
    public void saveGroupMessage(Message message) {
        userMapper.saveGroupMessage(message);
    }

    /**
     * 根据关键词查找用户
     * 
     * @param keyword
     * @return
     */
    @Override
    public List<SearchForUserVO> searchForUsers(String keyword) {
        return userMapper.searchForUsers(keyword, BaseContext.getCurrentId());
    }

}
