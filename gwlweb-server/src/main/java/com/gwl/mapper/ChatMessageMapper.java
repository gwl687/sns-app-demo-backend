package com.gwl.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

import com.gwl.pojo.vo.GroupMessageVo;
import com.gwl.pojo.vo.PrivateMessageVO;

@Mapper
public interface ChatMessageMapper {
    /**
     * 发送私聊消息
     * 
     * @param sendPrivateMessageDTO
     */
    void sendPrivateMessage(Long myId, Long friendId, String context);

    /**
     * 更新聊天列表最后时间
     * 
     * @param friendOrGroupId
     */
    void updateLastMessageTime(Long myId, Long friendOrGroupId);

    /**
     * 获取聊天列表里的全部私聊消息
     * 
     * @return
     */
    List<PrivateMessageVO> getPrivateMessages(Long myId);

    /**
     * 发送群聊消息
     * 
     * @param sendPrivateMessageDTO
     */
    void sendGroupMessage(Long myId, Long groupId, String context);

    /**
     * 获取聊天列表里的全部群聊消息
     * 
     * @return
     */
    List<GroupMessageVo> getGroupMessages(Long myId);
}
