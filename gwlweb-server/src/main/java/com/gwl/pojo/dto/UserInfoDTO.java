package com.gwl.pojo.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoDTO implements Serializable {
    private Long id;

    private String username;

    private Integer sex;

    private Integer age;

    private String avatarurl;

    private List<Long> interests;
}
