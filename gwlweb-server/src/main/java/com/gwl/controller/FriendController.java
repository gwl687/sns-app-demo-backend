package com.gwl.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.gwl.pojo.entity.User;
import com.gwl.pojo.vo.RecommendedFriendVO;
import com.gwl.pojo.vo.SearchForUserVO;
import com.gwl.pojo.vo.UserInfoVO;
import com.gwl.result.Result;
import com.gwl.service.FriendService;
import com.gwl.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/friend")
@Tag(name = "好友相关接口")
public class FriendController {
    @Autowired
    UserService userService;
    @Autowired
    FriendService friendService;

    /**
     * 获取好友列表
     * 
     * @return
     */
    @GetMapping(path = "/getfriendlist", produces = "application/json")
    @Operation(summary = "获取好友列表")
    Result<List<UserInfoVO>> getFriendList() {
        List<User> users = friendService.getFriendList();
        List<UserInfoVO> userInfoVOs = new ArrayList<>();
        if (users != null) {
            for (User user : users) {
                UserInfoVO userInfoVO = UserInfoVO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .sex(user.getSex())
                        .avatarurl(user.getAvatarurl())
                        .emailaddress(user.getEmailaddress())
                        .build();
                userInfoVOs.add(userInfoVO);
            }
        }
        return Result.success(userInfoVOs);
    }

    /**
     * 获取正在申请成为我好友的用户
     * 
     * @return
     */
    @GetMapping(path = "/getrequestfriends", produces = "application/json")
    @Operation(summary = "获取正在申请成为我好友的用户")
    Result<List<SearchForUserVO>> getRequestFriends() {
        List<User> users = friendService.getRequestFriends();
        List<SearchForUserVO> searchForUserVOs = new ArrayList<>();
        if (users != null) {
            for (User user : users) {
                SearchForUserVO searchForUserVO = SearchForUserVO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .sex(user.getSex())
                        .avatarurl(user.getAvatarurl())
                        .emailaddress(user.getEmailaddress())
                        .status(2)
                        .build();
                searchForUserVOs.add(searchForUserVO);
            }
        }
        return Result.success(searchForUserVOs);
    }

    /**
     * 获取聊天列表
     * 
     * @return
     */
    @GetMapping(path = "/getchatlist", produces = "application/json")
    @Operation(summary = "获取聊天列表")
    // 用GroupChat，包含了返回普通朋友所需字段
    Result<?> getChatList() {
        log.info("获取聊天列表");
        List<?> result = userService.getChatList();
        return Result.success(result);
    }

    /**
     * 按关键词搜索用户
     * 
     * @param keyword
     * @return
     */
    @GetMapping(path = "/searchforusers", produces = "application/json")
    @Operation(summary = "按关键词搜索用户")
    Result<List<SearchForUserVO>> searchForUsers(@RequestParam String keyword) {
        List<SearchForUserVO> users = userService.searchForUsers(keyword);
        return Result.success(users);
    }

    /**
     * 申请好友
     * 
     * @param userId
     * @return
     */
    @PostMapping(path = "/sendfriendrequest", produces = "application/json")
    @Operation(summary = "申请好友")
    Result<Void> sendFriendRequest(@RequestParam Long userId) {
        friendService.sendFriendRequest(userId);
        return Result.success();
    }

    /**
     * 回复好友申请
     * 
     * @param userId
     * @return
     */
    @PostMapping(path = "/friendrequestresponse", produces = "application/json")
    @Operation(summary = "回复好友申请")
    Result<Void> friendRequestResponse(@RequestParam Long friendId,
            @RequestBody Integer res) {
        friendService.friendRequestResponse(friendId, res);
        return Result.success();
    }

    /**
     * 添加朋友到聊天列表
     * 
     * @param friendId
     */
    @PostMapping(path = "/addtochatlist", produces = "application/json")
    @Operation(summary = "添加朋友到聊天列表")
    Result<Void> addToChatList(@RequestParam Long friendId) {
        friendService.addToChatList(friendId);
        return Result.success();
    }

    /**
     * 获取推荐好友
     * @return
     */
    @GetMapping(path = "/getrecommandedfriends", produces = "application/json")
    @Operation(summary = "获取推荐朋友")
    Result<List<UserInfoVO>> getRecommandedFriends() {
        return Result.success(friendService.getRecommendedFriends());
    }
}
