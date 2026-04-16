package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志 Mapper
 * 用于查询和分析系统操作，支持异常检测
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    
    /**
     * 查询最近 N 分钟内特定操作的记录数
     */
    @Select("SELECT COUNT(*) FROM operation_log WHERE action = #{action} AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    int countActionInLastMinutes(String action, int minutes);
    
    /**
     * 查询最近 N 分钟内特定操作和结果的记录数
     */
    @Select("SELECT COUNT(*) FROM operation_log WHERE action = #{action} AND result = #{result} AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    int countActionResultInLastMinutes(String action, String result, int minutes);
    
    /**
     * 查询最近 N 分钟内登录失败的记录
     */
    @Select("SELECT * FROM operation_log WHERE action = 'LOGIN' AND result = 'FAIL' AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) ORDER BY created_at DESC")
    List<OperationLog> getFailedLoginInLastMinutes(int minutes);
    
    /**
     * 查询最近 N 分钟内删除操作的记录
     */
    @Select("SELECT * FROM operation_log WHERE action = 'DELETE' AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) ORDER BY created_at DESC")
    List<OperationLog> getDeleteOperationsInLastMinutes(int minutes);
    
    /**
     * 查询特定用户最近 N 分钟内的操作次数
     */
    @Select("SELECT COUNT(*) FROM operation_log WHERE operator = #{operator} AND created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)")
    int countUserActionsInLastMinutes(String operator, int minutes);
    
    /**
     * 查询最近 N 分钟内按操作类型分组的统计
     */
    @Select("SELECT action, result, COUNT(*) as count FROM operation_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) GROUP BY action, result")
    List<Map<String, Object>> groupActionsByTypeInLastMinutes(int minutes);
    
    /**
     * 查询最近 N 天内的操作记录总数
     */
    @Select("SELECT COUNT(*) FROM operation_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int countOperationsInLastDays(int days);
    
    /**
     * 查询最近 N 天内失败操作的统计
     */
    @Select("SELECT COUNT(*) FROM operation_log WHERE result = 'FAIL' AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int countFailureInLastDays(int days);
}
