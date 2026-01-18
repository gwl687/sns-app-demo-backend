package com.gwl.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.gwl.context.BaseContext;
import com.gwl.mapper.InterestMapper;
import com.gwl.mapper.UserMapper;
import com.gwl.pojo.dto.GoogleLoginDto;
import com.gwl.pojo.dto.RegisterDTO;
import com.gwl.pojo.dto.UserInfoDTO;
import com.gwl.pojo.dto.UserLoginDTO;
import com.gwl.pojo.entity.User;
import com.gwl.pojo.vo.FriendListVO;
import com.gwl.pojo.vo.InterestVo;
import com.gwl.pojo.vo.UserInfoVO;
import com.gwl.pojo.vo.UserLoginVO;
import com.gwl.result.Result;
import com.gwl.service.CommonService;
import com.gwl.service.InterestService;
import com.gwl.service.UserService;
import com.gwl.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/user")
@Slf4j
@Tag(name = "用户相关接口")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private CommonService commonService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private InterestService interestService;
    @Autowired
    private InterestMapper interestMapper;

    /**
     * 用户登录相关
     * 
     * @param userLoginDTO
     * @return
     */
    @PostMapping(path = "/login", produces = "application/json")
    @Operation(summary = "用户登录相关")
    Result<UserLoginVO> Login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录：{}", userLoginDTO);
        User user = userService.userLogin(userLoginDTO);
        String oldUserId = redis.opsForValue().get("push_token_user:" + userLoginDTO.getPushToken());
        if (oldUserId != null && !oldUserId.equals(user.getId().toString())) {
            redis.delete("push_token:" + oldUserId);
        }
        redis.opsForValue().set("push_token:" + user.getId(), userLoginDTO.getPushToken());
        redis.opsForValue().set("push_token_user:" + userLoginDTO.getPushToken(), user.getId().toString());
        // 登录成功后生成令牌
        String token = JwtUtil.generateToken(user.getId());
        UserLoginVO userloginVO = UserLoginVO.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .avatarUrl(user.getAvatarurl())
                .token(token)
                .emailaddress(user.getEmailaddress())
                .build();
        return Result.success(userloginVO);
    }

    /**
     * 获取用户信息
     * 
     * @return
     */
    @GetMapping(path = "getuserinfo", produces = "application/json")
    Result<UserInfoVO> getUserInfo() {
        User user = userService.getUserInfo();
        List<Long> interestVos = interestMapper.getUserInterests(BaseContext.getCurrentId());
        UserInfoVO userInfoVO = UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .sex(user.getSex())
                .avatarurl(user.getAvatarurl())
                .emailaddress(user.getEmailaddress())
                .interests(interestVos)
                .build();
        return Result.success(userInfoVO);
    }

    /**
     * 更新用户信息
     * 
     * @param updateUserInfoDTO
     * @return
     */
    @PostMapping(path = "/updateuserinfo", produces = "application/json")
    Result<Void> updateUserInfo(UserInfoDTO userInfoDTO) {
        userService.updateUserInfo(userInfoDTO);
        return Result.success();
    }

    /**
     * 获取指定用户信息
     * 
     * @return
     */
    @GetMapping(path = "getuserinfobyid", produces = "application/json")
    Result<UserInfoVO> getUserInfoById(@RequestParam Long userId) {
        User user = userService.getUserInfoById(userId);
        UserInfoVO userInfoVO = UserInfoVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .sex(user.getSex())
                .avatarurl(user.getAvatarurl())
                .emailaddress(user.getEmailaddress())
                .build();
        return Result.success(userInfoVO);
    }

    /**
     * 发送验证码
     * 
     * @return
     */
    @PostMapping(path = "/sendverificationcode", produces = "application/json")
    @Operation(summary = "sendverificationcod")
    Result<Void> sendVerificationCode(@RequestParam String emailaddress) {
        System.out.println("发送验证码:" + emailaddress);
        userService.sendVerificationCode(emailaddress);
        return Result.success();
    }

    /**
     * 注册
     * 
     * @param registerDTO
     * @return
     */
    @PostMapping(path = "/register", produces = "application/json")
    @Operation(summary = "Register")
    Result<Void> userRegister(@RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    /**
     * 改名
     * 
     * @param registerDTO
     * @return
     */
    @PostMapping(path = "/changeusername", produces = "application/json")
    @Operation(summary = "changeUsername")
    Result<Void> changeUsername(@RequestParam String newUsername) {
        userService.changeUsername(newUsername);
        return Result.success();
    }

    /**
     * 上传新头像
     * 
     * @param file
     * @return
     * @throws IOException
     */
    @PutMapping(path = "/uploadavatar", produces = "application/json")
    @Operation(summary = "upload avatar")
    Result<Boolean> uploadAvatar(@RequestParam("file") MultipartFile file) {
        userService.uploadAvatar(file);

        return Result.success(true);
    }

    /**
     * 获取用户头像url
     * 
     * @param userId
     * @return
     */
    @GetMapping(path = "/getuseravatar", produces = "application/json")
    @Operation(summary = "getUserAvatar")
    Result<String> getUserAvatar(@RequestParam("userId") Long userId) {
        return Result.success(userMapper.getUserAvatarUrl(userId));
    }

    /**
     * google login
     * 
     * @param googleLoginDto
     * @return
     */
    @PostMapping(path = "/googlelogin", produces = "application/json")
    Result<UserLoginVO> googleLogin(@RequestBody GoogleLoginDto googleLoginDto) {
        log.info("google login: {}", googleLoginDto);
        return Result.success(userService.googleLogin(googleLoginDto));
    }
}
