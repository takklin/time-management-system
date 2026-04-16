package com.timemanager.service;

import com.timemanager.mapper.TaskMapper;
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
    
    /**
     * 获取用户增长数据（按天统计）
     */
    public List<Map<String, Object>> getUserGrowthData(int days) {
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            // 示例数据 - 实际应该从数据库查询
            int newUsers = 10; // 占位符

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("newUsers", newUsers);

            data.add(item);
        }

        return data;
    }
    
    /**
     * 获取分类排行
     */
    public List<Map<String, Object>> getCategoryRanking() {
        List<Map<String, Object>> ranking = new ArrayList<>();
        
        // 示例数据 - 实际应该从数据库查询
        Map<String, Object> item1 = new HashMap<>();
        item1.put("name", "工作");
        item1.put("taskCount", 45);
        ranking.add(item1);
        
        Map<String, Object> item2 = new HashMap<>();
        item2.put("name", "学习");
        item2.put("taskCount", 32);
        ranking.add(item2);
        
        Map<String, Object> item3 = new HashMap<>();
        item3.put("name", "生活");
        item3.put("taskCount", 28);
        ranking.add(item3);
        
        return ranking;
    }
    
    /**
     * 获取用户排行榜
     */
    public List<Map<String, Object>> getUserRanking(int limit) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        
        // 示例数据 - 实际应该从数据库查询
        for (int i = 1; i <= Math.min(limit, 10); i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("username", "user_" + i);
            item.put("completedTasks", 100 - i * 5);
            item.put("totalTime", (100 - i * 5) * 60); // 分钟
            ranking.add(item);
        }
        
        return ranking;
    }
    
    /**
     * 获取最近7天活跃用户数
     */
    public Integer getActiveUsersLast7Days() {
        // 示例数据 - 实际应该从数据库查询
        return 150;
    }
    
    /**
     * 计算用户的完成率
     */
    public Double calculateCompletionRate(Long userId) {
        // 示例数据 - 实际应该从数据库查询
        return 75.0;
    }
}
