package com.gwl.pojo.vo;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoVO {
    private Long userId;

    private int sex;

    private int age;

    private String username;

    private String avatarurl;

    private String emailaddress;

    private List<String> interests;
}
