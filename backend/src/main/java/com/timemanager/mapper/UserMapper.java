package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 总用户数
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM user WHERE deleted=0")
    int countAll();

    // 近N天注册用户数
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM user WHERE deleted=0 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)")
    int countRegisterInLastDays(int days);

    // 某天活跃用户数（有操作日志或打卡记录）
    @org.apache.ibatis.annotations.Select("SELECT COUNT(DISTINCT user_id) FROM (\n" +
            "SELECT user_id FROM time_record WHERE deleted=0 AND record_date = #{date}\n" +
            "UNION ALL\n" +
            "SELECT id as user_id FROM user WHERE deleted=0 AND DATE(updated_at) = #{date}\n" +
            ") t")
    int countActiveUserByDate(String date);
}
