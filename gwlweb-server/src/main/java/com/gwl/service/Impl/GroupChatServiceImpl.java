package com.gwl.service.Impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwl.context.BaseContext;
import com.gwl.infrastructure.Manager.ChannelManager;
import com.gwl.pojo.dto.AddFriendToChatListDTO;
import com.gwl.pojo.dto.CreateGroupChatDTO;
import com.gwl.pojo.entity.GroupChat;
import com.gwl.pojo.entity.User;
import com.gwl.pojo.vo.GroupChatVO;
import com.gwl.service.CommonService;
import com.gwl.service.GroupChatService;
import com.gwl.mapper.UserMapper;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GroupChatServiceImpl implements GroupChatService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    ChannelManager channelManager;
    @Autowired
    CommonService commonService;
    private static final String API_KEY = "devkey";
    private static final String API_SECRET = "secret";
    //private static final String LIVEKIT_URL = "ws://3.112.54.245:7880";

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

    /*
     * 添加群成员
     */
    @Override
    public void addGroupMembers(CreateGroupChatDTO createGroupChatDTO) {
        GroupChat groupChat = userMapper.getGroupChat(createGroupChatDTO.getGroupId());
        String groupName = groupChat.getGroupName();
        for (Long userId : createGroupChatDTO.getSelectedFriends()) {
            userMapper.addGroupMembers(createGroupChatDTO.getGroupId(), userId);
            AddFriendToChatListDTO addFriendToChatListDTO = AddFriendToChatListDTO.builder()
                            .userId(userId)
                            .friendId(groupChat.getGroupId())
                            .isGroup(true)
                            .build();
            userMapper.addFriendToChatList(addFriendToChatListDTO);
            String friendName = userMapper.getByUserId(userId).getUsername();
            groupName += ',' + friendName;
        }
        userMapper.updateGroupName(createGroupChatDTO.getGroupId(), groupName);
        // 更新后再获取一次群聊信息
        groupChat = userMapper.getGroupChat(createGroupChatDTO.getGroupId());
        String memberIdsString = groupChat.getMemberIds();
        List<Long> memberIds = Arrays.stream(memberIdsString.split(",")).map(Long::parseLong).toList();
        for (Long memberId : memberIds) {
            // FCM
            commonService.sendPush(memberId, BaseContext.getCurrentId(), "", "", "joingroup", true);
        }
        log.info("添加群成员");
    }

    /*
     * 移除群成员
     */
    @Override
    public void removeGroupMembers(CreateGroupChatDTO createGroupChatDTO) {
        String groupName = "";
        for (Long userId : createGroupChatDTO.getSelectedFriends()) {
            userMapper.removeGroupMembers(groupId, userId);
        }
        GroupChat groupChat = userMapper.getGroupChat(groupId);
        String memberIdsString = groupChat.getMemberIds();
        List<Long> memberIds = Arrays.stream(memberIdsString
                .split(","))
                .map(Long::parseLong)
                .toList();
        for (Long id : memberIds) {
            if (id == groupChat.getOwnerId()) {
                groupName += userMapper.getByUserId(id).getUsername();
            } else {
                groupName += "," + userMapper.getByUserId(id).getUsername();
            }
        }
        userMapper.updateGroupName(groupId, groupName);

        for (Long memberId : memberIds) {
            // FCM
            commonService.sendPush(memberId, BaseContext.getCurrentId(), "", "", "joingroup", true);
        }
    }

    /**
     * 创建视频聊天token
     */
    @Override
    public String createLivekitToken(String roomName) {
        AccessToken token = new AccessToken(API_KEY, API_SECRET);
        token.setIdentity(BaseContext.getCurrentId().toString());
        User user = userMapper.getByUserId(BaseContext.getCurrentId());
        String username = user.getUsername();
        // metadata：展示用信息
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        try {
            token.setMetadata(new ObjectMapper().writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        token.addGrants(new RoomJoin(true), new RoomName(roomName));
        return token.toJwt();
    }
}
