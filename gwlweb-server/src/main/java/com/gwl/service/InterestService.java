package com.gwl.service;

import java.util.List;

import com.gwl.pojo.vo.InterestVo;

public interface InterestService {
    /**
     * 更新用户兴趣
     */
    void updateUserInterests(List<Long> InterestIds);
}
