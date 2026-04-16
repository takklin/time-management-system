package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.AlertLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 异常预警日志 Mapper
 */
@Mapper
public interface AlertLogMapper extends BaseMapper<AlertLog> {

    /**
     * 获取未处理或已读的预警（按创建时间倒序，最多N条）
     */
    @Select("SELECT * FROM alert_log WHERE status < 2 ORDER BY created_at DESC LIMIT #{limit}")
    List<AlertLog> getUnhandledAlerts(int limit);

    /**
     * 获取指定类型和状态的预警数量
     */
    @Select("SELECT COUNT(*) FROM alert_log WHERE alert_type = #{alertType} AND status = #{status}")
    int countByTypeAndStatus(String alertType, int status);
}
