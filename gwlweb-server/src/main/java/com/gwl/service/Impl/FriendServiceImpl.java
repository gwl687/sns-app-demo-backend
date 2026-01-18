package com.gwl.service.Impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.auth.UserInfo;
import com.gwl.context.BaseContext;
import com.gwl.mapper.FriendMapper;
import com.gwl.mapper.UserMapper;
import com.gwl.pojo.entity.User;
import com.gwl.pojo.vo.RecommendedFriendVO;
import com.gwl.pojo.vo.UserInfoVO;
import com.gwl.service.CommonService;
import com.gwl.service.FriendService;

@Service
public class FriendServiceImpl implements FriendService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    FriendMapper friendMapper;
    @Autowired
    CommonService commonService;

    /**
     * 获取好友列表
     * 
     * @return
     */
    @Override
    public List<User> getFriendList() {
        return friendMapper.getFriendListByUserId(BaseContext.getCurrentId());
    }

    /**
     * 获取正在申请成为我好友的用户
     * 
     * @return
     */
    @Override
    public List<User> getRequestFriends() {
        return friendMapper.getRequestFriends(BaseContext.getCurrentId());
    }

    /**
     * 申请好友
     * 
     * @param userId
     */
    @Override
    public void sendFriendRequest(Long userId) {
        System.out.println("向用户" + userId + "申请好友");
        String userName = userMapper.getByUserId(userId).getUsername();
        // 更新friend_relation状态
        userMapper.sendFriendRequest(BaseContext.getCurrentId(), userId);
        // 推送消息
        commonService.sendPush(userId, BaseContext.getCurrentId(), "a new friend request",
                "user " + userName + " has sent you a friend request",
                "friendrequest", true);
    }

    /**
     * 回复好友申请
     * 
     * @param userId
     */
    @Override
    public void friendRequestResponse(Long friendId, Integer res) {
        friendMapper.friendRequestResponse(BaseContext.getCurrentId(), friendId, res);
        String type = "friendRequestResponse";
        commonService.sendPush(friendId, BaseContext.getCurrentId(), "friendRequestResponse", res.toString(), type,
                true);
    }

    /**
     * 添加朋友到聊天列表
     * 
     * @param friendId
     */
    @Override
    @Transactional
    public void addToChatList(Long friendId) {
        friendMapper.addToChatList(BaseContext.getCurrentId(), friendId);
    }

    /**
     * 获取推荐好友
     * 
     * @return
     */
    public List<UserInfoVO> getRecommendedFriends() {
        List<UserInfoVO> recommendedFriendVOs = new ArrayList<>();
        List<Long> recommandedFriendIds = friendMapper.getRecommendedFriendIds(BaseContext.getCurrentId());
        for (Long recommandedFriendId : recommandedFriendIds) {
            User friend = userMapper.getByUserId(recommandedFriendId);
            List<Long> interests = userMapper.getInterestsByUserId(recommandedFriendId);
            UserInfoVO recommendedFriendVO = UserInfoVO.builder()
                    .userId(friend.getId())
                    .sex(friend.getSex())
                    .age(friend.getAge())
                    .username(friend.getUsername())
                    .avatarurl(friend.getAvatarurl())
                    .emailaddress(friend.getEmailaddress())
                    .interests(interests).build();
            recommendedFriendVOs.add(recommendedFriendVO);
        }
        return recommendedFriendVOs;
    }
}
