package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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

    // 近N天每天注册用户数（按日分组）
    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS count FROM user WHERE deleted=0 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> countRegisterByDay(@Param("days") int days);

    // 统计指定日期范围内的去重活跃用户数（用于 WAU）
    @Select("SELECT COUNT(DISTINCT user_id) FROM (SELECT user_id FROM time_record WHERE deleted=0 AND record_date BETWEEN #{startDate} AND #{endDate} UNION ALL SELECT id as user_id FROM user WHERE deleted=0 AND DATE(updated_at) BETWEEN #{startDate} AND #{endDate}) t")
    int countActiveUsersBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 用户排行榜：任务数、专注总时长、完成率（使用子查询避免重复）
    @Select("SELECT u.id AS id, u.username AS username, "
            + "(SELECT COUNT(*) FROM task t WHERE t.user_id = u.id AND t.deleted = 0) AS taskCount, "
            + "(SELECT IFNULL(SUM(duration_minutes),0) FROM time_record tr WHERE tr.user_id = u.id AND tr.deleted = 0) AS focusTime, "
            + "(SELECT IFNULL(ROUND(SUM(CASE WHEN t2.status=1 THEN 1 ELSE 0 END)/GREATEST(COUNT(t2.id),1)*100,2),0) FROM task t2 WHERE t2.user_id = u.id AND t2.deleted = 0) AS completionRate "
            + "FROM user u WHERE u.deleted = 0 ORDER BY taskCount DESC LIMIT #{limit}")
    List<Map<String, Object>> getUserRanking(@Param("limit") int limit);
}
