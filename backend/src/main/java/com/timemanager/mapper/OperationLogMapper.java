package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("SELECT * FROM operation_log WHERE deleted=0 AND user_id = #{userId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<OperationLog> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM operation_log WHERE deleted=0 AND user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    // 兼容旧表中使用 operator 字段（存储用户名/操作者）
    @Select("SELECT * FROM operation_log WHERE operator = #{operator} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<OperationLog> selectByOperator(@Param("operator") String operator, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM operation_log WHERE operator = #{operator}")
    int countByOperator(@Param("operator") String operator);

    @Select("SELECT COUNT(*) FROM operation_log WHERE action = #{action} AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    int countActionInLastMinutes(@Param("action") String action, @Param("minutes") int minutes);

    @Select("SELECT COUNT(*) FROM operation_log WHERE action = #{action} AND result = #{result} AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    int countActionResultInLastMinutes(@Param("action") String action, @Param("result") String result, @Param("minutes") int minutes);

    @Select("SELECT * FROM operation_log WHERE action = 'LOGIN' AND result = 'FAIL' AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) ORDER BY created_at DESC")
    List<OperationLog> getFailedLoginInLastMinutes(@Param("minutes") int minutes);

    @Select("SELECT * FROM operation_log WHERE action = 'DELETE' AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) ORDER BY created_at DESC")
    List<OperationLog> getDeleteOperationsInLastMinutes(@Param("minutes") int minutes);

    @Select("SELECT COUNT(*) FROM operation_log WHERE operator = #{operator} AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    int countUserActionsInLastMinutes(@Param("operator") String operator, @Param("minutes") int minutes);

    @Select("SELECT action, result, COUNT(*) as count FROM operation_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) GROUP BY action, result")
    List<Map<String, Object>> groupActionsByTypeInLastMinutes(@Param("minutes") int minutes);

    @Select("SELECT COUNT(*) FROM operation_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int countOperationsInLastDays(@Param("days") int days);

    @Select("SELECT COUNT(*) FROM operation_log WHERE result = 'FAIL' AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int countFailureInLastDays(@Param("days") int days);

    @Select("SELECT COUNT(DISTINCT operator) FROM operation_log WHERE action = 'login_success' AND created_at >= #{start} AND created_at <= #{end}")
    int countDistinctLoginSuccessOperators(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Select("SELECT ip, COUNT(*) as count FROM operation_log " +
            "WHERE (action = 'login_failed' OR (LOWER(action) = 'login' AND LOWER(result) IN ('fail','failed'))) " +
            "AND created_at > #{since} " +
            "GROUP BY ip HAVING COUNT(*) >= 5")
        List<Map<String, Object>> selectFailedLoginIps(@Param("since") LocalDateTime since);
}
