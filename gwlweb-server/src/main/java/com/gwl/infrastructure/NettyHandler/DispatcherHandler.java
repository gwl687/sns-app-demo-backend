package com.gwl.infrastructure.NettyHandler;

import java.util.List;

import org.apache.curator.framework.recipes.nodes.GroupMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.gwl.infrastructure.Manager.ChannelManager;
import com.gwl.mapper.ChatMessageMapper;
import com.gwl.mapper.FriendMapper;
import com.gwl.pojo.entity.Message;
import com.gwl.pojo.entity.User;
import com.gwl.service.CommonService;
import com.gwl.service.UserService;
import com.gwl.util.CommonUtil;
import com.gwl.util.JwtUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ChannelHandler.Sharable
public class DispatcherHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Autowired
    ChatMessageMapper chatMessageMapper;
    @Autowired
    CommonService commonService;
    @Autowired
    UserService userService;
    @Autowired
    FriendMapper friendMapper;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete handshake) {
            // 从握手请求里拿 Header
            HttpHeaders headers = handshake.requestHeaders();
            String authHeader = headers.get("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.close();
                return;
            }
            String token = authHeader.substring("Bearer ".length());
            long userId = JwtUtil.parseToken(token);
            ctx.channel().attr(ChannelManager.USER_ID).set(userId);
            log.info("保存userId: " + userId);
            ChannelManager.userChannelMap.put(userId, ctx.channel());
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg)
            throws JsonMappingException, JsonProcessingException {
        String text = msg.text();
        JsonNode jsonData = CommonUtil.mapper.readTree(text);
        log.info("收到消息" + msg);
        Message message = new Message(jsonData);
        long toUser = message.getToUser();
        String type = message.getType();
        String content = message.getContent();
        long fromUser = ctx.channel().attr(ChannelManager.USER_ID).get();
        message.setFromUser(fromUser);
        String sendMessage = CommonUtil.mapper.writeValueAsString(message);
        System.out.println("sendMessage=" + sendMessage);
        switch (type) {
            // 私聊
            case "private":
                log.info("收到私聊消息: " + message.getContent());
                Channel toChannel = ChannelManager.userChannelMap.get(toUser);
                // 对方在线的话，长连接发消息
                if (toChannel != null && toChannel.isActive()) {
                    toChannel.writeAndFlush(new TextWebSocketFrame(sendMessage));
                }
                // 存数据库
                chatMessageMapper.sendPrivateMessage(fromUser, toUser, content);
                chatMessageMapper.updateLastMessageTime(fromUser, toUser);
                // FCMpush
                User user = userService.getUserInfoById(fromUser);
                commonService.sendPush(toUser, fromUser, user.getUsername(), content, "privatemessage", false);
                break;
            // 群聊
            case "group":
                log.info("收到群聊消息: " + message.getContent());
                List<Long> groupMemberIds = friendMapper.getGroupMembers(toUser);
                log.info("groupMemberId={}", groupMemberIds);
                for (Long groupMemberId : groupMemberIds) {
                    Channel toGroupMemberChannel = ChannelManager.userChannelMap.get(groupMemberId);
                    // 对方在线的话，长连接发消息
                    if (toGroupMemberChannel != null && toGroupMemberChannel.isActive()) {
                        if (groupMemberId == fromUser) {
                            continue;
                        }
                        toGroupMemberChannel.writeAndFlush(new TextWebSocketFrame(sendMessage));
                    }
                    // FCM
                    String groupName = friendMapper.getGroupName(toUser);
                    String fromUserName = userService.getUserInfoById(fromUser).getUsername();
                    commonService.sendPush(groupMemberId, fromUser, groupName, fromUserName + ": " + content,
                            "groupmessage",
                            false);
                }
                // 存数据库
                chatMessageMapper.sendGroupMessage(fromUser, toUser, content);
                chatMessageMapper.updateLastMessageTime(fromUser, toUser);
                break;
            // 视频聊天请求
            case "videochatrequest":
                commonService.sendPush(toUser, fromUser, "video chat request", content, "videochatrequest", false);
                break;
            default:
                break;
        }
        // ctx.fireChannelRead(new Message(jsonData));
    }
}
