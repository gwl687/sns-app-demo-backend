package com.gwl.service.Impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.gwl.context.BaseContext;
import com.gwl.mapper.ChatMessageMapper;
import com.gwl.pojo.dto.SendPrivateMessageDTO;
import com.gwl.pojo.vo.GroupMessageVo;
import com.gwl.pojo.vo.PrivateMessageVO;
import com.gwl.service.ChatMessageService;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    @Autowired
    ChatMessageMapper chatMessageMapper;

    /**
     * 获取聊天列表里的全部私聊消息
     * @return
     */
    @Override
    public List<PrivateMessageVO> getPrivateMessages() {
        return chatMessageMapper.getPrivateMessages(BaseContext.getCurrentId());
    }

    /**
     * 获取聊天列表里的全部群聊消息
     * @return
     */
    @Override
    public List<GroupMessageVo> getGroupMessages() {
        return chatMessageMapper.getGroupMessages(BaseContext.getCurrentId());
    }
}
