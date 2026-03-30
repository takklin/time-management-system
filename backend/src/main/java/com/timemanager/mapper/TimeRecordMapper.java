package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.TimeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}
