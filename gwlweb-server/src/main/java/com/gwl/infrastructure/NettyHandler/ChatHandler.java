package com.gwl.infrastructure.NettyHandler;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.gwl.infrastructure.Manager.ChannelManager;
import com.gwl.mapper.ChatMessageMapper;
import com.gwl.mapper.FriendMapper;
import com.gwl.mapper.UserMapper;
import com.gwl.pojo.entity.Message;
import com.gwl.pojo.entity.User;
import com.gwl.service.CommonService;
import com.gwl.service.UserService;
import com.gwl.util.CommonUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelHandler;

@Component
@Slf4j
@ChannelHandler.Sharable
public class ChatHandler extends SimpleChannelInboundHandler<Message> {
    AttributeKey<Long> USER_ID = AttributeKey.valueOf("userId");
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FriendMapper friendMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private CommonService commonService;

    /**
     * 发送聊天消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        long toUser = msg.getToUser();
        long fromUser = ctx.channel().attr(ChannelManager.USER_ID).get();
        String content = msg.getContent();
        String type = msg.getType();
        msg.setFromUser(fromUser);
        String sendMessage = CommonUtil.mapper.writeValueAsString(msg);
        System.out.println("sendMessage=" + sendMessage);
        Channel toChannel = ChannelManager.userChannelMap.get(toUser);
        // 对方在线的话，长连接发消息
        if (toChannel != null && toChannel.isActive()) {
            log.info("对方在线，发送消息");
            toChannel.writeAndFlush(new TextWebSocketFrame(sendMessage));
        }
        switch (type) {
            // 私聊
            case "private":
                log.info("收到私聊消息: " + msg.getContent());
                // 存数据库
                chatMessageMapper.sendPrivateMessage(fromUser, toUser, content);
                chatMessageMapper.updateLastMessageTime(fromUser, toUser);
                // FCMpush
                User user = userService.getUserInfoById(fromUser);
                commonService.sendPush(toUser, fromUser, user.getUsername(), content, "privatemessage", false);
                break;
            // 群聊
            case "group":
                log.info("收到群聊消息: " + msg.getContent());
                // 存数据库
                chatMessageMapper.sendGroupMessage(fromUser, toUser, content);
                chatMessageMapper.updateLastMessageTime(fromUser, toUser);
                // FCMpush
                List<Long> groupMemberIds = friendMapper.getGroupMembers(toUser);
                String groupName = friendMapper.getGroupName(toUser);
                for (Long groupMemberId : groupMemberIds) {
                    commonService.sendPush(groupMemberId, fromUser, groupName, fromUser + ": " + content,
                            "groupmessage",
                            false);
                }
                break;
            // 视频聊天请求
            case "videochatrequest":
                commonService.sendPush(toUser, fromUser, "video chat request", content, "videochatrequest", false);
                break;
            default:
                break;
        }
    }
}
