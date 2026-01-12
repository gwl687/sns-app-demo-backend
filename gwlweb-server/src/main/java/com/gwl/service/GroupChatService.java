package com.gwl.service;

import com.gwl.pojo.dto.CreateGroupChatDTO;
import com.gwl.pojo.vo.GroupChatVO;

public interface GroupChatService {
    /**
     * 建群
     * @param createGroupChatDTO
     * @return
     */
    GroupChatVO createGroupChat(CreateGroupChatDTO createGroupChatDTO);

    /*
     * 添加群成员
     */
    void addGroupMembers(CreateGroupChatDTO createGroupChatDTO);

    /*
     * 移除群成员
     */
    void removeGroupMembers(CreateGroupChatDTO createGroupChatDTO);

    /*
     * 创建视频聊天token
     */
    String createLivekitToken(String roomName);
}