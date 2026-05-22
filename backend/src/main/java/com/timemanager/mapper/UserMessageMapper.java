package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {

    // Order by id (fallback) to avoid SQL errors when `created_at` column is missing in some databases.
    @Select("SELECT * FROM user_message WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY id DESC LIMIT #{offset}, #{limit}")
    List<UserMessage> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_message WHERE user_id = #{userId} AND is_deleted = 0")
    int countByUserId(@Param("userId") Long userId);

    @Update("UPDATE user_message SET is_read = 1, read_time = NOW() WHERE id = #{id} AND user_id = #{userId}")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_message SET is_deleted = 1 WHERE id = #{id} AND user_id = #{userId}")
    int softDelete(@Param("id") Long id, @Param("userId") Long userId);
}
