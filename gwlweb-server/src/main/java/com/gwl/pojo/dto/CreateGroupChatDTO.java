package com.gwl.pojo.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreateGroupChatDTO {
    @Schema(description = "群Id")
    private Long groupId;
    @Schema(description = "群聊人员列表")
    private List<Long> selectedFriends;

}