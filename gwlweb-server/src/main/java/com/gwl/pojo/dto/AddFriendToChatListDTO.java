package com.gwl.pojo.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFriendToChatListDTO {
    @Schema(description = "用户id")
    private Long userId;
    @Schema(description = "朋友或群的id")
    private Long friendId;
    @Schema(description = "是否为群聊")
    private int isGroup;
}
