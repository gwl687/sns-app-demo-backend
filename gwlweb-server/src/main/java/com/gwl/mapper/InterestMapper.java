package com.gwl.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InterestMapper {
    /**
     * 获取所有兴趣种类
     * 
     * @return
     */
    @Select("select name from interest")
    List<String> getAllInterests();

    /**
     * 获取指定用户兴趣
     * 
     * @param userId
     * @return
     */
    @Select("select i.name from user_interest ui join interest i on ui.interest_id=i.id where ui.user_id = #{userId}")
    List<String> getUserInterests(Long userId);

    /**
     * 删除用户兴趣
     * 
     * @param userId
     * @return
     */
    @Update("delete from user_interest where user_id = #{userId}")
    List<String> deleteUserInterests(Long userId);

    /**
     * 更新用户兴趣
     * 
     * @param userId
     * @return
     */
    @Update("select i.name from user_interest ui join interest i on ui.interest_id=i.id where ui.user_id = #{userId}")
    List<String> updateUserInterests(Long userId, List<Long> interestIds);
}
