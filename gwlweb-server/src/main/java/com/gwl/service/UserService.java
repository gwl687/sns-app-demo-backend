package com.gwl.service;

import java.util.List;

import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.multipart.MultipartFile;

import com.gwl.pojo.dto.AddFriendToChatListDTO;
import com.gwl.pojo.dto.CreateGroupChatDTO;
import com.gwl.pojo.dto.GoogleLoginDto;
import com.gwl.pojo.dto.RegisterDTO;
import com.gwl.pojo.dto.UserInfoDTO;
import com.gwl.pojo.dto.UserLoginDTO;
import com.gwl.pojo.entity.GroupChat;
import com.gwl.pojo.entity.Message;
import com.gwl.pojo.entity.UpdateUserInfoPushEvent;
import com.gwl.pojo.entity.User;
import com.gwl.pojo.vo.GroupChatVO;
import com.gwl.pojo.vo.GroupMessageVo;
import com.gwl.pojo.vo.SearchForUserVO;
import com.gwl.pojo.vo.UserLoginVO;

public interface UserService {
  /**
   * 用户登录
   * 
   * @param userLoginDTO
   * @return
   */
  User userLogin(UserLoginDTO userLoginDTO);

  /**
   * googlelogin
   * 
   * @param idToken
   * @return
   */
  UserLoginVO googleLogin(GoogleLoginDto googleLoginDto);

  /**
   * 发送验证码
   * 
   * @param emailaddress
   */
  void sendVerificationCode(String emailaddress);

  /**
   * 注册
   * 
   * @param registerDTO
   */
  void register(RegisterDTO registerDTO);

  /**
   * 改名
   * 
   * @param registerDTO
   */
  void changeUsername(String userName);

  /**
   * 上传新头像
   * 
   * @param registerDTO
   */
  void uploadAvatar(MultipartFile file);

  /*
   * 获取用户信息
   */
  User getUserInfo();

  /*
   * 根据id获取用户信息
   */
  User getUserInfoById(Long userId);

  /**
   * 更新用户信息
   * 
   * @return
   */
  void updateUserInfo(UserInfoDTO userInfoDTO);

  /**
   * 添加朋友或群到聊天列表
   */
  void addFriendToChatList(AddFriendToChatListDTO addFriendToChatListDTO);

  /**
   * 获取聊天列表
   * 
   * @return
   */
  List<?> getChatList();

  /**
   * 创建群聊
   * 
   * @param createGroupChatDTO
   * @return
   */
  GroupChatVO createGroupChat(CreateGroupChatDTO createGroupChatDTO);

  /**
   * 获取群信息
   * 
   * @param groupId
   * @return
   */
  GroupChat getGroupChat(Long GroupId);


  /**
   * 存储群消息
   * 
   * @param msg
   */
  void saveGroupMessage(Message msg);

  /**
   * 根据关键词查找用户
   * 
   * @param keyword
   * @return
   */
  List<SearchForUserVO> searchForUsers(String keyword);

  /**
   * 更新用户信息的推送
   * 
   * @param event
   */
  public void onUpdateUserInfoPush(@Payload UpdateUserInfoPushEvent event);
}
