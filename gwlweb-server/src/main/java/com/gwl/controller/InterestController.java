package com.gwl.controller;

import java.sql.ResultSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gwl.mapper.InterestMapper;
import com.gwl.result.Result;

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

    @GetMapping(path = "getallinterests", produces = "application/json")
    @Operation(summary = "getallinterests")
    Result<List<String>> getAllInterests() {
        return Result.success(interestMapper.getAllInterests());
    }
}
