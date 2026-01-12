package com.gwl.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.gwl.pojo.entity.User;

@Mapper
public interface FriendMapper {
        /**
         * 获取好友列表
         * 
         * @param id
         * @return
         */
        @Select("""
                        SELECT tu.*
                        FROM test_user tu
                        JOIN friend_relation fr ON tu.id = fr.friend_id
                        WHERE fr.user_id = #{id} and fr.status = 1
                        """)
        List<User> getFriendListByUserId(Long id);

        /**
         * 获取正在申请成为我好友的用户
         * 
         * @param myId
         * @return
         */
        @Select("select tu.* from test_user tu join friend_relation fr on tu.id = fr.user_id where fr.status = 2 and fr.friend_id = #{myId}")
        List<User> getRequestFriends(Long myId);

        /**
         * 回复好友申请
         * 
         * @param myId
         * @param friendId
         * @param res
         */

        void friendRequestResponse(Long myId, Long friendId, Integer res);

        /**
         * 添加朋友到聊天列表
         * 
         * @param friendId
         */
        @Insert("""
                        insert ignore into chatlist(user_id, friend_id, isgroup)
                        values
                        (#{myId}, #{friendId}, 0),
                        (#{friendId}, #{myId}, 0)
                        """)
        void addToChatList(Long myId, Long friendId);

        /**
         * 获取群聊名字
         * 
         * @param groupId
         * @return
         */
        @Select("select name from chatgroups where id = #{groupId}")
        String getGroupName(Long groupId);

        /**
         * 获取群聊成员
         * 
         * @param groupId
         * @return
         */
        @Select("select user_id from group_members where group_id = #{groupId}")
        List<Long> getGroupMembers(Long groupId);

        /**
         * 获取群聊成员名字列表(改群名用)
         * 
         * @param groupId
         * @return
         */
        @Select("select username from test_user tu join group_members gm on tu.id = gm.user_id where gm.group_id = #{groupId}")
        List<String> getGroupMemberNames(Long groupId);

        /**
         * 改群名
         * 
         * @param groupId
         * @param newName
         */
        @Update("update chatgroups set name = #{newName} where id = #{groupId}")
        void changeGroupName(Long groupId, String newName);

        /**
         * 获取推荐好友
         * 
         * @param myId
         * @return
         */
        List<Long> getRecommendedFriendIds(Long myId);
}
