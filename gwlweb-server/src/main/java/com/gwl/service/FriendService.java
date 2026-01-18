package com.gwl.service;

import java.util.List;
import com.gwl.pojo.entity.User;
import com.gwl.pojo.vo.RecommendedFriendVO;
import com.gwl.pojo.vo.UserInfoVO;

public interface FriendService {

    /**
     * 获取好友列表
     * 
     * @return
     */
    List<User> getFriendList();

    /**
     * 获取正在申请成为我好友的用户
     * 
     * @return
     */
    List<User> getRequestFriends();

    /**
     * 申请好友
     * @param userId
     */
    public void sendFriendRequest(Long userId);

    /**
     * 回复好友申请
     * @param userId
     * @param response
     */
    void friendRequestResponse(Long friendId, Integer res);

    /**
     * 添加朋友到聊天列表
     * @param friendId
     */
    void addToChatList(Long friendId);

    /**
     * 获取推荐好友
     * @return
     */
    List<UserInfoVO> getRecommendedFriends();
}
