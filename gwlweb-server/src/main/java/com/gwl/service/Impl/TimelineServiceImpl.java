package com.gwl.service.Impl;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.gwl.properties.AwsProperties;
import com.gwl.context.BaseContext;
import com.gwl.exception.BaseException;
import com.gwl.mapper.FriendMapper;
import com.gwl.mapper.TimelineMapper;
import com.gwl.mapper.UserMapper;
import com.gwl.pojo.dto.PostCommentDTO;
import com.gwl.pojo.dto.TimelineDTO;
import com.gwl.pojo.entity.TimelineComment;
import com.gwl.pojo.entity.TimelineContent;
import com.gwl.pojo.entity.TimelineLikeHitEvent;
import com.gwl.pojo.entity.TimelinePushEvent;
import com.gwl.pojo.entity.TimelineUserLike;
import com.gwl.pojo.entity.User;
import com.gwl.pojo.vo.LikeUserVO;
import com.gwl.pojo.vo.TimelineVO;
import com.gwl.service.CommonService;
import com.gwl.service.TimelineService;
import com.gwl.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TimelineServiceImpl implements TimelineService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FriendMapper friendMapper;
    @Autowired
    private TimelineMapper timelineMapper;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private CommonService commonService;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private AwsProperties aws;

    /**
     * 推送帖子
     * 
     * @throws IOException
     */
    @Override
    public void postTimeline(TimelineDTO timelineDTO) {
        // 添加到数据库
        TimelineContent timelineContent = TimelineContent.builder()
                .userId(timelineDTO.getUserId())
                .context(timelineDTO.getContext())
                .imgUrls(null)
                .build();
        timelineMapper.postTimeline(timelineContent);
        Long postId = timelineContent.getId();
        TimelineVO timelineVO = timelineMapper.getTimelineContent(postId, BaseContext.getCurrentId());
        // mysql操作
        // timeline里先给自己加记录
        timelineMapper.addTimeline(BaseContext.getCurrentId(), postId, timelineVO.getCreatedAt());
        String publisherName = userMapper.getByUserId(timelineDTO.getUserId()).getUsername();
        String key = "timeline:post:" + postId;
        Map<String, String> timelineMap = new HashMap<>();
        List<String> imgUrls = new ArrayList<>();
        // 存图片到S3
        if (timelineDTO.getFiles() != null) {
            for (MultipartFile file : timelineDTO.getFiles()) {
                String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
                String randomId = UUID.randomUUID().toString();
                String imgUrl = aws.getUrl() + "timeline/" + postId + "/" + randomId + extension;
                String uploadKey = "timeline/" + postId + "/" + randomId + extension;
                commonService.uploadToS3(file, uploadKey);
                imgUrls.add(imgUrl);
            }
            // 更新mysql的img_urls
            timelineMapper.updateTimelineImgs(imgUrls, postId);
            timelineMap.put("imgUrls", JSON.toJSONString(imgUrls));
        }
        // 写入redis timelinecontent
        timelineMap.put("postId", postId.toString());
        timelineMap.put("userId", timelineDTO.getUserId().toString());
        timelineMap.put("userName", publisherName);
        timelineMap.put("context", timelineDTO.getContext());
        timelineMap.put("createdAt", timelineVO.getCreatedAt().toString());
        redis.opsForHash().putAll(key, timelineMap);
        // 获取好友列表 推送kafka
        List<User> friends = friendMapper.getFriendListByUserId(BaseContext.getCurrentId());
        List<Long> friendIds = new ArrayList<>();
        for (User f : friends) {
            friendIds.add(f.getId());
        }
        log.info("遍历friendIds:{}", friendIds);
        int batchSize = 1000;
        for (int i = 0; i < friendIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, friendIds.size());
            List<Long> batch = friendIds.subList(i, end).stream().map(Long::valueOf).toList();
            log.info("遍历batch:{}", batch);
            TimelinePushEvent timelinePushEvent = TimelinePushEvent.builder()
                    .fromUser(BaseContext.getCurrentId())
                    .postId(postId)
                    .fanIds(batch)
                    .publisherName(publisherName)
                    .createdAt(timelineVO.getCreatedAt())
                    .content(timelineDTO.getContext()).build();
            kafkaTemplate.send(
                    "timeline_publish",
                    timelinePushEvent);
            log.info("推送消息");
        }
    }

    // 推送给粉丝的消费者/
    @Override
    @KafkaListener(topics = "timeline_publish", groupId = "timeline_publish_group")
    public void onTimelinePush(@Payload TimelinePushEvent event) {
        // 推送给多个好友
        Long postId = event.getPostId();
        List<Long> fanIds = event.getFanIds();
        Long fromUser = event.getFromUser();
        String publisherName = event.getPublisherName();
        String content = event.getContent();
        Instant createdAt = event.getCreatedAt();
        if (fanIds == null || fanIds.isEmpty()) {
            log.info("fanid为空");
            return;
        }
        // 将帖子推送到每个粉丝的 Redis 时间线
        log.info("遍历fanIds:{}", fanIds);
        for (Long fanId : fanIds) {
            String key = "timeline:user:" + fanId;
            // 将 postId 放到用户时间线头部
            redis.opsForList().leftPush(key, postId.toString());
            // 数据库更新user的timeline表
            timelineMapper.addTimeline(Long.valueOf(fanId), postId, createdAt);
            // 发送 iOS / Android 推送通知
            String title = publisherName + " posted a new timeline";
            String type = "timelinepost";
            commonService.sendPush(fanId, fromUser, title, content, type, false);
        }
    }

    /**
     * 获取帖子(刷新)
     */
    @Override
    public List<TimelineVO> getTimelinePost(Integer limit, Instant cursor, Long cursorId) {
        List<TimelineVO> timelineVOs = new ArrayList<>();
        List<Long> timelineIds = timelineMapper.getTimelineIds(BaseContext.getCurrentId(), limit, cursor, cursorId);
        for (Long timelineId : timelineIds) {
            // redis里有数据就取redis的
            if (redis.hasKey("timeline:post:" + timelineId)) {
                Map<Object, Object> map = redis.opsForHash().entries("timeline:post:" + timelineId);
                String userName = map.get("userName").toString();
                String context = map.get("context").toString();
                Object imgUrlsObj = map.get("imgUrls");
                List<String> imgUrls = imgUrlsObj == null
                        ? Collections.emptyList()
                        : JSON.parseArray(imgUrlsObj.toString(), String.class);
                Instant createdAt = Instant.parse(map.get("createdAt").toString());
                String totalLikeStr = redis.opsForValue().get("timeline:like:totalcount:" + timelineId);
                Integer totalLikedCount = totalLikeStr == null ? 0 : Integer.parseInt(totalLikeStr);
                Map<Long, Integer> userLikeMap = redis.opsForHash().entries("timeline:like:user:" + timelineId)
                        .entrySet()
                        .stream()
                        // 按 value 倒序
                        .sorted((e1, e2) -> {
                            int v1 = Integer.parseInt(e1.getValue().toString());
                            int v2 = Integer.parseInt(e2.getValue().toString());
                            return Integer.compare(v2, v1);
                        })
                        // 只取前 20
                        .limit(20)
                        // 收集成 Map
                        .collect(Collectors.toMap(
                                e -> Long.parseLong(e.getKey().toString()),
                                e -> Integer.parseInt(e.getValue().toString()),
                                (a, b) -> a,
                                LinkedHashMap::new // 保持排序后的顺序
                        ));
                List<LikeUserVO> likeUserVOs = new ArrayList<>();
                // 用户点赞数据
                Integer likedByMeCount = 0;
                log.info("BaseContext.getCurrentId():{}", BaseContext.getCurrentId());
                for (Map.Entry<Long, Integer> entry : userLikeMap.entrySet()) {
                    Long userId = entry.getKey();
                    log.info("userId:{}", userId);
                    Integer likeCount = entry.getValue();
                    LikeUserVO likeUserVO = LikeUserVO.builder()
                            .userId(userId)
                            .avatarUrl(redis.opsForValue().get("useravatarurl:" + userId))
                            .userLikeCount(likeCount)
                            .build();
                    if (Objects.equals(userId, BaseContext.getCurrentId())) {
                        log.info("likedByMe:{}", likedByMeCount);
                        likedByMeCount = likeCount;
                    }
                    likeUserVOs.add(likeUserVO);
                }
                // 用户评论数据
                String key = "timelinecomment:" + timelineId;
                List<String> jsonList = redis.opsForList().range(key, 0, 100);
                List<TimelineComment> timelineComments = new ArrayList<>();
                for (String json : jsonList) {
                    try {
                        JsonNode node = CommonUtil.mapper.readTree(json);
                        TimelineComment comment = new TimelineComment();
                        comment.setCommentId(node.get("commentId").asLong());
                        comment.setUserId(node.get("userId").asLong());
                        comment.setComment(node.get("comment").asText());
                        comment.setCreatedAt(
                                Instant.parse(node.get("createdAt").asText()));
                        comment.setTimelineId(timelineId);
                        timelineComments.add(comment);
                    } catch (Exception e) {
                        log.error("解析 redis 评论失败: {}", json, e);
                    }
                }
                // 希望评论下方是新的
                Collections.reverse(timelineComments);
                TimelineVO timelineVO = TimelineVO.builder()
                        .timelineId(timelineId)
                        .topLikeUsers(likeUserVOs)
                        .userName(userName)
                        .context(context)
                        .imgUrls(imgUrls)
                        .createdAt(createdAt)
                        .likedByMeCount(likedByMeCount)
                        .totalLikeCount(totalLikedCount)
                        .comments(timelineComments)
                        .build();
                timelineVOs.add(timelineVO);
            } else {
                // log.info("没有取到redis里的timeline数据，查询mysql");
                // timelineVOs.add(timelineMapper.getTimelineContent(timelineId,
                // BaseContext.getCurrentId()));
            }
        }
        log.info("获取到{}条timeline", timelineVOs.size());
        return timelineVOs;
    }

    /**
     * 获取单个帖子
     * 
     * @return
     */
    @Override
    public TimelineVO getTimelinePostByTimelineId(Long timelineId) {
        TimelineVO timelineVO;
        if (redis.hasKey("timeline:post:" + timelineId)) {
            Map<Object, Object> map = redis.opsForHash().entries("timeline:post:" + timelineId);
            String userName = map.get("userName").toString();
            String context = map.get("context").toString();
            Object imgUrlsObj = map.get("imgUrls");
            List<String> imgUrls = imgUrlsObj == null
                    ? Collections.emptyList()
                    : JSON.parseArray(imgUrlsObj.toString(), String.class);
            Instant createdAt = Instant.parse(map.get("createdAt").toString());
            String totalLikeStr = redis.opsForValue().get("timeline:like:totalcount:" + timelineId);
            Integer totalLikedCount = totalLikeStr == null ? 0 : Integer.parseInt(totalLikeStr);
            Map<Long, Integer> userLikeMap = redis.opsForHash().entries("timeline:like:user:" + timelineId)
                    .entrySet()
                    .stream()
                    // 按 value 倒序
                    .sorted((e1, e2) -> {
                        int v1 = Integer.parseInt(e1.getValue().toString());
                        int v2 = Integer.parseInt(e2.getValue().toString());
                        return Integer.compare(v2, v1);
                    })
                    // 只取前 20
                    .limit(20)
                    // 收集成 Map
                    .collect(Collectors.toMap(
                            e -> Long.parseLong(e.getKey().toString()),
                            e -> Integer.parseInt(e.getValue().toString()),
                            (a, b) -> a,
                            LinkedHashMap::new // 保持排序后的顺序
                    ));
            List<LikeUserVO> likeUserVOs = new ArrayList<>();
            // 用户点赞数据
            Integer likedByMeCount = 0;
            for (Map.Entry<Long, Integer> entry : userLikeMap.entrySet()) {
                Long userId = entry.getKey();
                Integer likeCount = entry.getValue();
                LikeUserVO likeUserVO = LikeUserVO.builder()
                        .userId(userId)
                        .avatarUrl(redis.opsForValue().get("useravatarurl:" + userId))
                        .userLikeCount(likeCount)
                        .build();
                if (userId == BaseContext.getCurrentId()) {
                    likedByMeCount = likeCount;
                    // log.info("likedByMe:{}", likedByMeCount);
                }
                likeUserVOs.add(likeUserVO);
            }
            // 用户评论数据
            String key = "timelinecomment:" + timelineId;
            List<String> jsonList = redis.opsForList().range(key, 0, 9);
            List<TimelineComment> timelineComments = new ArrayList<>();

            for (String json : jsonList) {
                try {
                    JsonNode node = CommonUtil.mapper.readTree(json);
                    TimelineComment comment = new TimelineComment();
                    comment.setCommentId(node.get("commentId").asLong());
                    comment.setUserId(node.get("userId").asLong());
                    comment.setComment(node.get("comment").asText());
                    comment.setCreatedAt(
                            Instant.parse(node.get("createdAt").asText()));
                    comment.setTimelineId(timelineId);

                    timelineComments.add(comment);
                } catch (Exception e) {
                    log.error("解析 redis 评论失败: {}", json, e);
                }
            }
            // 希望评论下方是新的
            Collections.reverse(timelineComments);
            timelineVO = TimelineVO.builder()
                    .timelineId(timelineId)
                    .topLikeUsers(likeUserVOs)
                    .userName(userName)
                    .context(context)
                    .imgUrls(imgUrls)
                    .createdAt(createdAt)
                    .likedByMeCount(likedByMeCount)
                    .totalLikeCount(totalLikedCount)
                    .comments(timelineComments)
                    .build();
        } else {
            log.info("没有取到redis里的timeline数据，查询mysql");
            timelineVO = timelineMapper.getTimelineContent(timelineId, BaseContext.getCurrentId());
        }
        return timelineVO;
    }

    /**
     * 给帖子点赞
     */
    @Override
    public void likeHit(Long timelineId) {
        Long userId = BaseContext.getCurrentId();
        // 帖子总点赞数 +1
        Long totalLikeCount = redis.opsForValue()
                .increment("timeline:like:totalcount:" + timelineId);
        // 用户对该帖的点赞数 +1
        redis.opsForHash()
                .increment("timeline:like:user:" + timelineId,
                        userId.toString(),
                        1);
        // 标记为 dirty（发生过变化）
        redis.opsForSet()
                .add("dirty:timeline:set", timelineId.toString());
        // 触发阈值刷（例如 300）
        if (totalLikeCount != null && totalLikeCount % 300 == 0) {
            TimelineUserLike timelineUserLike = TimelineUserLike.builder()
                    .timelineId(timelineId)
                    .userLikeCount(redis.opsForHash()
                            .entries("timeline:like:user:" + timelineId)
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    e -> Long.valueOf(e.getKey().toString()),
                                    e -> Integer.valueOf(e.getValue().toString()))))
                    .build();
            timelineMapper.flushLikeToDB(timelineUserLike);
            // 已刷库，移出 dirty
            redis.opsForSet()
                    .remove("dirty:timeline:set", timelineId);
        }
    }

    /**
     * 给帖子评论
     */
    @Override
    public void postComment(PostCommentDTO postCommentDTO) {
        String comment = postCommentDTO.getComment();
        Long userId = BaseContext.getCurrentId();
        Long timelineId = postCommentDTO.getTimelineId();
        Instant createdAt = Instant.now();
        TimelineComment timelineComment = TimelineComment.builder()
                .comment(comment)
                .userId(userId)
                .createdAt(createdAt)
                .timelineId(timelineId).build();
        timelineMapper.postComment(timelineComment);
        // mysql save
        Long commentId = timelineComment.getCommentId();
        // redis save
        String key = "timelinecomment:" + postCommentDTO.getTimelineId();
        Map<String, Object> value = new HashMap<>();
        value.put("commentId", commentId);
        value.put("userId", userId);
        value.put("comment", comment);
        value.put("createdAt", createdAt);
        try {
            redis.opsForList().leftPush(
                    key,
                    CommonUtil.mapper.writeValueAsString(value));
        } catch (Exception e) {
            log.error("发帖子评论存redis失败", e);
            throw new BaseException("发帖子评论存redis失败");
        }
    }

    /**
     * kafka点赞消费者
     */
    // 没有使用这个topic
    @Override
    @KafkaListener(topics = "timeline_likehit", groupId = "timeline-group")
    public void onLikeHit(@Payload TimelineLikeHitEvent event) {
        Long timelineId = event.getTimelineId();
        Long userId = event.getUserId();
        // 帖子总点赞数 +1
        Long totalLikeCount = redis.opsForValue()
                .increment("timeline:like:totalcount:" + timelineId);
        // 用户对该帖的点赞数 +1
        redis.opsForHash()
                .increment("timeline:like:user:" + timelineId,
                        userId.toString(),
                        1);
        // 标记为 dirty（发生过变化）
        redis.opsForSet()
                .add("dirty:timeline:set", timelineId.toString());
        // 触发阈值刷（例如 300）
        if (totalLikeCount != null && totalLikeCount % 300 == 0) {
            TimelineUserLike timelineUserLike = TimelineUserLike.builder()
                    .timelineId(timelineId)
                    .userLikeCount(redis.opsForHash()
                            .entries("timeline:like:user:" + timelineId)
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    e -> Long.valueOf(e.getKey().toString()),
                                    e -> Integer.valueOf(e.getValue().toString()))))
                    .build();
            timelineMapper.flushLikeToDB(timelineUserLike);
            // 已刷库，移出 dirty
            redis.opsForSet()
                    .remove("dirty:timeline:set", timelineId.toString());
        }
    }

    // 每分钟刷盘点赞的脏数据
    @Scheduled(fixedDelayString = "60000")
    @Override
    public void flushTimelineLikeToMySQL() {
        try {
            Set<Long> dirtyTimelineIds = redis.opsForSet()
                    .members("dirty:timeline:set").stream().map(Long::valueOf).collect(Collectors.toSet());
            for (Long dirtyTimeline : dirtyTimelineIds) {
                TimelineUserLike timelineUserLike = TimelineUserLike.builder()
                        .timelineId(dirtyTimeline)
                        .userLikeCount(redis.opsForHash()
                                .entries("timeline:like:user:" + dirtyTimeline)
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(
                                        e -> Long.valueOf(e.getKey().toString()),
                                        e -> Integer.valueOf(e.getValue().toString()))))
                        .build();
                timelineMapper.flushLikeToDB(timelineUserLike);
                // 已刷库，移出 dirty
                redis.opsForSet()
                        .remove("dirty:timeline:set", dirtyTimeline.toString());
            }
        } catch (Exception e) {
            log.warn("redis error: ", e);
        }
    }
}
