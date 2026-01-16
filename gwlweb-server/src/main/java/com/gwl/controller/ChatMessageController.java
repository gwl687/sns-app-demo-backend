package com.gwl.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gwl.pojo.dto.SendPrivateMessageDTO;
import com.gwl.pojo.vo.GroupMessageVo;
import com.gwl.pojo.vo.PrivateMessageVO;
import com.gwl.result.Result;
import com.gwl.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/chatmessage") 
@Tag(name = "聊天消息相关接口")  
public class ChatMessageController {
    @Autowired
    ChatMessageService chatMessageService;

    /**
     * 获取聊天列表里的全部私聊消息
     * 
     * @return
     */
    @GetMapping(path = "/getprivatemessages", produces = "application/json")
    @Operation(summary = "获取聊天列表里的全部私聊消息")
    Result<List<PrivateMessageVO>> getPrivateMessages() {
        return Result.success(chatMessageService.getPrivateMessages());
    }

    /**
     * 获取聊天列表里的全部群聊消息
     * 
     * @return
     */
    @GetMapping(path = "/getgroupmessages", produces = "application/json")
    @Operation(summary = "获取聊天列表里的全部群聊消息")
    Result<List<GroupMessageVo>> getGroupMessages() {
        return Result.success(chatMessageService.getGroupMessages());
    }
}
