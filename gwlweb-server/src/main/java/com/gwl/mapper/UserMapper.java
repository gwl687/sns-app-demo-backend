package com.gwl.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.gwl.pojo.dto.AddFriendToChatListDTO;
import com.gwl.pojo.dto.UserInfoDTO;
import com.gwl.pojo.entity.ChatFriend;
import com.gwl.pojo.entity.ChatListId;
import com.gwl.pojo.entity.GroupChat;
import com.gwl.pojo.entity.Message;
import com.gwl.pojo.entity.User;
import com.gwl.pojo.vo.GroupChatVO;
import com.gwl.pojo.vo.SearchForUserVO;

@Mapper
public interface UserMapper {
    /**
     * 用户登录
     * 
     * @param username
     * @return
     */

    @Select("select * from user  where username = #{username}")
    User getByUsername(String username);

    /**
     * 插入新用户数据
     * 
     * @param user
     */
    void insert(User user);

    /**
     * 改名
     * 
     * @param userName
     */
    @Update("update test_user set username = #{userName} where id = #{userId}")
    void changeUsername(String userName, Long userId);

    /**
     * 更新头像url
     * 
     * @param avatarUrl
     */
    @Update("update test_user set avatarurl = #{avatarUrl} where id = #{myId}")
    void updateAvatarUrl(Long myId, String avatarUrl);

    /**
     * 根据邮箱获取用户信息
     * 
     * @param emailaddress
     * @return
     */
    @Select("select * from test_user where emailaddress = #{emailaddress}")
    User getByUserEmail(String emailaddress);

    /**
     * 遍历有自己的群id
     * 
     * @param emailaddress
     * @return
     */
    @Select("select group_id from group_members where user_id = #{myId}")
    List<Long> getMyGroupIds(Long myId);

    /**
     * 遍历有自己的朋友id
     * 
     * @param emailaddress
     * @return
     */
    @Select("select friend_id from friend_relation where user_id = #{myId}")
    List<Long> getMyFriendIds(Long myId);

    /**
     * 根据id获取用户信息
     * 
     * @param id
     * @return
     */
    @Select("select * from test_user  where id = #{id}")
    User getByUserId(Long id);

    /**
     * 更新用户信息
     * 
     * @param updateUserInfo
     * @return
     */
    void updateUserInfo(UserInfoDTO userInfo);

    /**
     * 获取指定ID的用户名
     */
    @Select("select username from test_user where id = #{id}")
    String getUserName(Long id);

    /**
     * 新建群聊
     * 
     * @param groupMembers
     */
    void createGroupChat(GroupChat groupChat);

    /**
     * 插入成员表数据
     * 
     * @param groupChat
     */
    void insertGroupChatMember(GroupChatVO groupChat);

    /**
     * 获取所有群成员id
     * 
     * @param groupId
     */
    @Select("select user_id from group_members where group_id = #{groupId}")
    List<Long> getGroupMemberIds(Long groupId);

    /**
     * 获取群信息
     * 
     * @param groupId
     * @return
     */

    GroupChat getGroupChat(Long groupId);

    /**
     * 添加朋友或群到聊天列表
     */
    @Insert("insert into chatlist (user_id, friend_id ,isgroup) values (#{userId},#{friendId},#{isGroup})")
    void addFriendToChatList(AddFriendToChatListDTO addFriendToChatListDTO);

     /**
     * 从聊天列表里移除朋友或群
     */
    @Insert("delete from chatlist where user_id = #{userId} and friend_id=#{friendId} and isgroup = #{isGroup}")
    void removeFriendFromChatList(AddFriendToChatListDTO addFriendToChatListDTO);
    /*
     * 添加群成员
     */
    @Insert("insert into group_members (group_id,user_id) values (#{groupId},#{userId})")
    Boolean addGroupMembers(Long groupId, Long userId);
    
    /**
     * 移除群成员
     * @param groupId
     * @param userId
     * @return
     */
    @Insert("delete from group_members where group_id = #{groupId} and user_id = #{userId}")
    Boolean removeGroupMembers(Long groupId, Long userId);

    /*
     * 更改群名
     */
    @Insert("update chatgroups set name = #{groupName} where id = #{groupId}")
    Boolean updateGroupName(Long groupId, String groupName);

    /**
     * 获取聊天列表的各个id
     * 
     * @param id
     * @return
     */
    @Select("select friend_id As id, isgroup As isGroup from chatlist where user_id = #{id} order by last_message_time desc")
    List<ChatListId> getChatListIdById(Long id);

    /**
     * 获取聊天朋友
     * 
     * @param chatListId
     * @return
     */
    @Select("select id, username, avatarurl from test_user where id=#{id}")
    ChatFriend getChatFriendByChatId(Long id);

    /**
     * 获取聊天群
     * 
     * @param id
     * @return
     */

    GroupChat getChatGroupByChatId(Long id);


    /**
     * 保存群消息
     * 
     * @param msg
     */
    @Insert("insert into group_messages(group_id, sender_id, content, type) values(#{toUser},#{fromUser},#{content},#{type})")
    void saveGroupMessage(Message message);

    /**
     * 获取用户头像url
     * 
     * @param userId
     * @return
     */
    @Select("select avatarurl from test_user where id = #{userId}")
    String getUserAvatarUrl(Long userId);

    /**
     * 根据关键词查找用户
     * 
     * @param keyword
     * @return
     */
    List<SearchForUserVO> searchForUsers(String keyword, Long userId);

    /**
     * 申请好友
     * 
     * @param friendId
     * @param myId
     */
    @Insert("insert into friend_relation(user_id,friend_id,status) values (#{myId},#{friendId},2)")
    void sendFriendRequest(Long myId, Long friendId);

    @Select("select i.name from user_interest ui join interest i on ui.interest_id = i.id where user_id =#{userId}")
    List<String> getInterestsByUserId(Long userId);
}
