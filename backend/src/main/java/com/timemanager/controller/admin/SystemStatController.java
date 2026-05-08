
package com.timemanager.controller.admin;

import com.timemanager.common.result.Result;
import com.timemanager.entity.OperationLog;
import com.timemanager.mapper.TaskMapper;
import com.timemanager.mapper.UserMapper;
import com.timemanager.monitor.ApiStatMonitorFilter;
import com.timemanager.service.OperationLogService;
import com.timemanager.service.StatisticsDataService;
import com.timemanager.vo.SystemStatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统统计控制器（增强版）
 */
@RestController
@RequestMapping("/api/v1/admin/system")
public class SystemStatController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private StatisticsDataService statisticsDataService;

    @Autowired
    private OperationLogService operationLogService;

    // 简单的内存缓存，1 分钟过期，避免频繁全表聚合查询
    private volatile SystemStatisticsVO cachedStatistics = null;
    private volatile long cachedAtMillis = 0L;
    private static final long STAT_CACHE_MS = 60 * 1000L; // 1 minute

    /**
     * 获取完整的系统统计数据
     */
    @GetMapping("/statistics")
    public Result<SystemStatisticsVO> getFullStatistics(
            @RequestParam(defaultValue = "30") int days) {

        long now = System.currentTimeMillis();
        // 简单并发保护
        if (cachedStatistics != null && (now - cachedAtMillis) < STAT_CACHE_MS) {
            return Result.success(cachedStatistics);
        }

        SystemStatisticsVO statistics = new SystemStatisticsVO();

        // 用户增长数据
        statistics.setUserGrowth(statisticsDataService.getUserGrowthData(days));

        // 用户活跃度数据
        statistics.setUserActivity(getUserActivityData(7));

        // 任务趋势数据
        statistics.setTaskTrend(getTaskTrendData(days));

        // 专注热力图
        statistics.setFocusHeatmap(statisticsDataService.getFocusHeatmapData());

        // 分类排行
        statistics.setCategoryRanking(statisticsDataService.getCategoryRanking());

        // 用户排行榜
        statistics.setUserRanking(statisticsDataService.getUserRanking(10));

        // 概览
        statistics.setOverview(getOverviewStats());

        // 更新缓存
        cachedStatistics = statistics;
        cachedAtMillis = System.currentTimeMillis();

        return Result.success(statistics);
    }

    /**
     * 获取旧的仪表盘统计（兼容性）- 包含所有动态数据
     */
    @GetMapping("/stat")
    public Result<Map<String, Object>> getSystemStat() {
        Map<String, Object> stat = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 核心指标
        int totalUserCount = userMapper.countAll();
        int last7DaysRegisterCount = userMapper.countRegisterInLastDays(7);
        int todayActiveUserCount = userMapper.countActiveUserByDate(today.toString());
        int yesterdayActiveUserCount = userMapper.countActiveUserByDate(yesterday.toString());
        
        int last7DaysTaskCreated = taskMapper.countCreatedInLastDays(7);
        int last7DaysTaskFinished = taskMapper.countFinishedInLastDays(7);
        Double last7DaysTaskFinishRate = taskMapper.calcFinishRateInLastDays(7);
        if (last7DaysTaskFinishRate == null) last7DaysTaskFinishRate = 0.0;

        double dauChange = yesterdayActiveUserCount == 0 ? 100 : 
                ((todayActiveUserCount - yesterdayActiveUserCount) * 100.0 / yesterdayActiveUserCount);

        stat.put("totalUserCount", totalUserCount);
        stat.put("last7DaysRegisterCount", last7DaysRegisterCount);
        stat.put("todayActiveUserCount", todayActiveUserCount);
        stat.put("yesterdayActiveUserCount", yesterdayActiveUserCount);
        stat.put("dauChange", Math.round(dauChange * 100.0) / 100.0);
        stat.put("last7DaysTaskCreated", last7DaysTaskCreated);
        stat.put("last7DaysTaskFinished", last7DaysTaskFinished);
        stat.put("last7DaysTaskFinishRate", Math.round(last7DaysTaskFinishRate * 100.0) / 100.0);

        // API 相关指标 - 默认值（可扩展为从监控系统获取）
        stat.put("apiRequestCount", 0);
        stat.put("apiAvgRespTime", 0.0);
        stat.put("apiErrorRate", 0.0);
        stat.put("slowQueryCount", 0);

        // 异常行为预警 - 从操作日志中获取高风险操作（最近60分钟）
        List<OperationLog> highRiskLogs = operationLogService.getHighRiskLogs(60);
        List<Map<String, Object>> highRiskLogsList = new ArrayList<>();
        for (OperationLog log : highRiskLogs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", log.getId());
            item.put("operator", log.getOperator());
            item.put("action", log.getAction());
            item.put("target", log.getTarget());
            item.put("result", log.getResult());
            item.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
            highRiskLogsList.add(item);
        }
        stat.put("highRiskLogs", highRiskLogsList.size() > 0 ? highRiskLogsList.subList(0, Math.min(5, highRiskLogsList.size())) : new ArrayList<>());

        // 图表数据：近30天活跃用户趋势
        List<Map<String, Object>> dauTrend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            int dau = userMapper.countActiveUserByDate(date.toString());
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", dau);
            dauTrend.add(item);
        }
        stat.put("dauTrend", dauTrend);

        // 图表数据：近7天任务创建/完成对比
        List<Map<String, Object>> taskTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            int created = taskMapper.countCreatedInRange(startOfDay, endOfDay);
            int finished = taskMapper.countFinishedInRange(startOfDay, endOfDay);
            
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("created", created);
            item.put("finished", finished);
            taskTrend.add(item);
        }
        stat.put("taskTrend", taskTrend);

        return Result.success(stat);
    }

    /**
     * 获取用户活跃度数据
     */
    private List<Map<String, Object>> getUserActivityData(int days) {
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            int dau = userMapper.countActiveUserByDate(date.toString());
            String startWeek = date.minusDays(6).toString();
            String endWeek = date.toString();
            int wau = userMapper.countActiveUsersBetween(startWeek, endWeek);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("dau", dau);
            item.put("wau", wau);

            data.add(item);
        }

        return data;
    }

    /**
     * 获取任务趋势数据
     */
    private List<Map<String, Object>> getTaskTrendData(int days) {
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            // 这些方法需要在TaskMapper中实现
            int created = taskMapper.countCreatedInRange(startOfDay, endOfDay);
            int completed = taskMapper.countFinishedInRange(startOfDay, endOfDay);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("created", created);
            item.put("completed", completed);

            data.add(item);
        }

        return data;
    }

    

    /**
     * 获取概览统计
     */
    private Map<String, Object> getOverviewStats() {
        Map<String, Object> overview = new HashMap<>();

        overview.put("totalUsers", userMapper.countAll());
        overview.put("activeUsersLast7Days", statisticsDataService.getActiveUsersLast7Days());
        overview.put("averageCompletionRate", getAverageCompletionRate());
        overview.put("systemHealth", "normal"); // 可扩展为更复杂的计算

        return overview;
    }

    /**
     * 计算平均完成率
     */
    private Double getAverageCompletionRate() {
        List<Map<String, Object>> users = userMapper.selectMaps(null);
        if (users.isEmpty()) return 0.0;

        double totalRate = 0;
        for (Map<String, Object> user : users) {
            Long userId = ((Number) user.get("id")).longValue();
            totalRate += statisticsDataService.calculateCompletionRate(userId);
        }

        return totalRate / users.size();
    }

    /**
     * 获取用户增长趋势
     */
    @GetMapping("/user-growth")
    public Result<List<Map<String, Object>>> getUserGrowth(
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(statisticsDataService.getUserGrowthData(days));
    }

    /**
     * 获取任务完成率
     */
    @GetMapping("/completion-rate")
    public Result<List<Map<String, Object>>> getCompletionRate(
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(getTaskTrendData(days));
    }

    /**
     * 获取用户排行榜
     */
    @GetMapping("/user-ranking")
    public Result<List<Map<String, Object>>> getUserRanking(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(statisticsDataService.getUserRanking(limit));
    }
}
