package com.timemanager.service;

import com.timemanager.mapper.TaskMapper;
import com.timemanager.mapper.TimeRecordMapper;
import com.timemanager.mapper.UserBehaviorStatsMapper;
import com.timemanager.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 统计数据服务
 */
@Service
public class StatisticsDataService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private TaskMapper taskMapper;
    
    @Autowired
    private UserBehaviorStatsMapper userBehaviorStatsMapper;
    
    @Autowired
    private TimeRecordMapper timeRecordMapper;
    
    /**
     * 获取用户增长数据（按天统计）
     */
    public List<Map<String, Object>> getUserGrowthData(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate now = LocalDate.now();

        // 从数据库查询近 N 天每天注册数
        List<Map<String, Object>> rows = userMapper.countRegisterByDay(days);
        Map<String, Integer> dayMap = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Object d = r.get("date");
            Object c = r.get("count");
            if (d != null && c != null) {
                dayMap.put(d.toString(), ((Number) c).intValue());
            }
        }

        int cumulative = 0;
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            String ds = date.toString();
            int newUsers = dayMap.getOrDefault(ds, 0);
            cumulative += newUsers;
            Map<String, Object> item = new HashMap<>();
            item.put("date", ds);
            item.put("newUsers", newUsers);
            item.put("cumulative", cumulative);
            result.add(item);
        }

        return result;
    }
    
    /**
     * 获取分类排行
     */
    public List<Map<String, Object>> getCategoryRanking() {
        // 使用 TaskMapper 聚合按分类的任务数
        return taskMapper.getCategoryRanking(10);
    }
    
    /**
     * 获取用户排行榜
     */
    public List<Map<String, Object>> getUserRanking(int limit) {
        return userMapper.getUserRanking(limit);
    }
    
    /**
     * 获取最近7天活跃用户数
     */
    public Integer getActiveUsersLast7Days() {
        LocalDate now = LocalDate.now();
        String end = now.toString();
        String start = now.minusDays(6).toString();
        return userMapper.countActiveUsersBetween(start, end);
    }
    
    /**
     * 计算用户的完成率
     */
    public Double calculateCompletionRate(Long userId) {
        int total = taskMapper.countByUserId(userId);
        if (total == 0) return 0.0;
        int finished = taskMapper.countFinishedByUserId(userId);
        return (finished * 100.0) / total;
    }

    /**
     * 聚合近 N 天的专注热力图数据（按星期与小时）
     */
    public List<Map<String, Object>> getFocusHeatmapData() {
        LocalDate now = LocalDate.now();
        String end = now.toString();
        String start = now.minusDays(29).toString();
        List<Map<String, Object>> rows = timeRecordMapper.selectHeatmapAggregated(start, end);
        // 将 SQL 的 DAYOFWEEK（1=Sunday,2=Monday...）映射为 1=Monday..7=Sunday，方便前端显示
        for (Map<String, Object> r : rows) {
            Object dowObj = r.get("dayOfWeek");
            if (dowObj != null) {
                int raw = ((Number) dowObj).intValue();
                int mapped = ((raw + 5) % 7) + 1; // raw:1->7,2->1,3->2,...
                r.put("dayOfWeek", mapped);
            }
        }
        return rows;
    }
}
