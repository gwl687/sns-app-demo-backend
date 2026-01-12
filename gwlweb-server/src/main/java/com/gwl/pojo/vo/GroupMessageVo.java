package com.gwl.pojo.vo;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMessageVo {
    Long id;
    Long groupId;
    Long senderId;
    String content;
    String type;
    Instant createTime;
}
