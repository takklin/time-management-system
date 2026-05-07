package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.TimeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TimeRecordMapper extends BaseMapper<TimeRecord> {

    @Select("SELECT c.name, SUM(t.duration_minutes) AS value " +
            "FROM time_record t " +
            "LEFT JOIN task ta ON t.task_id = ta.id " +
            "LEFT JOIN category c ON ta.category_id = c.id " +
            "WHERE t.user_id = #{userId} AND t.record_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY c.name")
    List<Map<String, Object>> selectTimeDistribution(Long userId, String startDate, String endDate);

    @Select("SELECT ta.title, ta.estimated_minutes AS estimated, ta.actual_minutes AS actual " +
            "FROM task ta " +
            "WHERE ta.user_id = #{userId} AND ta.completed_at BETWEEN #{startDate} AND #{endDate}")
    List<Map<String, Object>> selectEstimateVsActual(Long userId, String startDate, String endDate);

        // 按小时统计活跃时段（开始时间所在小时）
        @Select("SELECT HOUR(start_time) AS hour, IFNULL(SUM(duration_minutes),0) AS value FROM time_record WHERE deleted=0 AND user_id = #{userId} AND record_date BETWEEN #{startDate} AND #{endDate} GROUP BY HOUR(start_time) ORDER BY hour")
        List<Map<String, Object>> selectActiveHours(Long userId, String startDate, String endDate);

        // 获取指定用户的近期时间记录（按日期、开始时间排序）
        @Select("SELECT * FROM time_record WHERE deleted=0 AND user_id = #{userId} ORDER BY record_date DESC, start_time DESC LIMIT #{limit}")
        List<TimeRecord> selectRecentByUser(Long userId, int limit);

        // 获取指定用户在日期范围内的时间记录（用于按小时统计）
        @Select("SELECT * FROM time_record WHERE deleted=0 AND user_id = #{userId} AND record_date BETWEEN #{startDate} AND #{endDate} ORDER BY record_date, start_time")
        List<TimeRecord> selectRecordsByUserInRange(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);

        // 按 weekday(hour) 汇总专注时长，用于热力图（返回 dayOfWeek, hour, value）
        @Select("SELECT DAYOFWEEK(record_date) AS dayOfWeek, HOUR(start_time) AS hour, IFNULL(SUM(duration_minutes),0) AS value FROM time_record WHERE deleted=0 AND record_date BETWEEN #{startDate} AND #{endDate} GROUP BY DAYOFWEEK(record_date), HOUR(start_time) ORDER BY DAYOFWEEK(record_date), HOUR(start_time)")
        List<Map<String, Object>> selectHeatmapAggregated(@Param("startDate") String startDate, @Param("endDate") String endDate);

            // 获取指定用户在日期范围内按任务聚合的耗时排行（按总分钟数降序）
            @Select("SELECT ta.id AS taskId, ta.title AS title, IFNULL(SUM(t.duration_minutes),0) AS totalMinutes " +
                    "FROM time_record t " +
                    "LEFT JOIN task ta ON t.task_id = ta.id " +
                    "WHERE t.deleted=0 AND t.user_id = #{userId} AND t.record_date BETWEEN #{startDate} AND #{endDate} " +
                    "GROUP BY ta.id, ta.title ORDER BY totalMinutes DESC LIMIT #{limit}")
            List<Map<String, Object>> selectTopTasksByUserInRange(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

        @Select("SELECT IFNULL(SUM(duration_minutes),0) AS total FROM time_record WHERE deleted=0 AND user_id = #{userId}")
        Integer selectTotalDurationByUser(@Param("userId") Long userId);
}
