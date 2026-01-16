package com.gwl.service.Impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gwl.context.BaseContext;
import com.gwl.mapper.InterestMapper;
import com.gwl.service.InterestService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class InterestServiceImpl implements InterestService {
    @Autowired
    InterestMapper interestMapper;

    /**
     * 更新用户兴趣
     * 
     * @return
     */
    @Override
    public void updateUserInterests(List<Long> InterestIds) {
        interestMapper.updateUserInterests(BaseContext.getCurrentId(), InterestIds);
    }
}
