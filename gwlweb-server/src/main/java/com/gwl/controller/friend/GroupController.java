package com.gwl.controller.friend;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.gwl.pojo.dto.AddFriendToChatListDTO;
import com.gwl.pojo.dto.CreateGroupChatDTO;
import com.gwl.pojo.dto.GroupmessageDTO;
import com.gwl.pojo.entity.GroupChat;
import com.gwl.pojo.vo.GroupChatVO;
import com.gwl.result.Result;
import com.gwl.service.GroupChatService;
import com.gwl.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/group")
@Tag(name = "群聊相关接口")
public class GroupController {
    @Autowired
    GroupChatService groupService;
    @Autowired
    UserService userService;

    /**
     * 添加群成员
     * 
     * @param groupId
     * @param createGroupChatDTO
     * @return
     */
    @PostMapping(path = "/addgroupmembers", produces = "application/json")
    @Operation(summary = "添加群成员")
    Result<Boolean> addGroupMembers(
            @RequestBody CreateGroupChatDTO createGroupChatDTO) {
        log.info("添加群成员：{}", createGroupChatDTO);
        groupService.addGroupMembers(createGroupChatDTO);
        return Result.success(true);
    }

    /**
     * 移除群成员
     * 
     * @param groupId
     * @param createGroupChatDTO
     * @return
     */
    @PostMapping(path = "/removegroupmembers/{groupId}", produces = "application/json")
    @Operation(summary = "移除群成员")
    Result<Boolean> removeGroupMembers(@PathVariable Long groupId,
            @RequestBody CreateGroupChatDTO createGroupChatDTO) {
        log.info("移除群成员：{}", createGroupChatDTO);
        groupService.removeGroupMembers(groupId, createGroupChatDTO);
        return Result.success(true);
    }

    @GetMapping(path = "/getlivekittoken/{groupId}", produces = "application/json")
    @Operation(summary = "获取livekittoken")
    Result<String> getLiveKitToken(@PathVariable Long groupId) {
        String token = groupService.createLivekitToken(groupId.toString());
        return Result.success(token);
    }
    
    /**
     * 创建群聊
     * 
     * @param createGroupChatDTO
     * @return
     */
    @PostMapping(path = "/creategroupchat", produces = "application/json")
    @Operation(summary = "创建群聊")
    Result<GroupChatVO> createGroupChat(
            @RequestBody CreateGroupChatDTO createGroupChatDTO) {
        log.info("创建群：{}", createGroupChatDTO);
        GroupChatVO groupChatVO = new GroupChatVO();
        groupChatVO = groupService.createGroupChat(createGroupChatDTO);
        return Result.success(groupChatVO);
    }

    // /**
    //  * 获取群聊信息
    //  * 
    //  * @param getGroupChatDTO
    //  * @return
    //  */
    // @GetMapping(path = "/getgroupchat", produces = "application/json")
    // @Operation(summary = "获取群信息")
    // Result<GroupChatVO> getGroupChat(@RequestParam("groupId") Long groupId) {
    //     GroupChat groupChat = userService.getGroupChat(groupId);
    //     List<Long> memberIds = Arrays.stream(groupChat.getMemberIds()
    //             .split(","))
    //             .filter(s -> !s.isBlank())
    //             .map(Long::valueOf)
    //             .toList();
    //     GroupChatVO groupChatVO = GroupChatVO.builder()
    //             .groupId(groupChat.getGroupId())
    //             .memberIds(memberIds)
    //             .ownerId(groupChat.getOwnerId())
    //             .groupName(groupChat.getGroupName())
    //             .avatarUrl("")
    //             .build();
    //     return Result.success(groupChatVO);
    // }

    // /**
    //  * 添加朋友或群到聊天列表
    //  * 
    //  * @param addFriendToChatListDTO
    //  * @return
    //  */
    // @PostMapping(path = "/addfriendtochatlist", produces = "application/json")
    // @Operation(summary = "添加朋友或群到聊天列表")
    // Result addFriendToChatList(
    //         @org.springframework.web.bind.annotation.RequestBody AddFriendToChatListDTO addFriendToChatListDTO) {
    //     log.info("添加朋友或群到聊天列表：{}", addFriendToChatListDTO);
    //     userService.addFriendToChatList(addFriendToChatListDTO);
    //     return Result.success();
    // }

}
