package com.gwl.controller;

import java.sql.ResultSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.BaseContainer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gwl.context.BaseContext;
import com.gwl.mapper.InterestMapper;
import com.gwl.pojo.vo.InterestVo;
import com.gwl.result.Result;
import com.gwl.service.InterestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/interest")
@Tag(name = "兴趣相关接口")
public class InterestController {
    @Autowired
    InterestMapper interestMapper;
    @Autowired
    InterestService interestService;

    /**
     * 获取全部兴趣
     * 
     * @return
     */
    @GetMapping(path = "getallinterests", produces = "application/json")
    @Operation(summary = "getallinterests")
    Result<List<InterestVo>> getAllInterests() {
        return Result.success(interestMapper.getAllInterests());
    }

    /**
     * 更新用户兴趣
     * 
     * @return
     */
    @GetMapping(path = "updateuserinterests", produces = "application/json")
    @Operation(summary = "updateuserinterests")
    Result<Void> updateUserInterests(List<Long> InterestIds) {
        interestService.updateUserInterests(InterestIds);
        return Result.success();
    }
}
