package com.timemanager.controller.admin;

import com.timemanager.common.result.Result;
import com.timemanager.config.MetricsFilter;
import com.timemanager.entity.RequestMetrics;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统健康度指标控制器
 * 提供API响应时间、错误率、慢查询等实时统计
 */
@RestController
@RequestMapping("/api/v1/admin/metrics")
public class MetricsController {

    /**
     * 获取系统健康度指标
     * @param timeRange 统计时间范围（分钟），默认60分钟
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> getHealthMetrics(
            @RequestParam(defaultValue = "60") int timeRange) {
        try {
            long now = System.currentTimeMillis();
            int milliseconds = timeRange * 60 * 1000;
            long startTime = now - milliseconds;

            // 获取指定时间范围内的指标
            List<RequestMetrics> recentMetrics = MetricsFilter.METRICS_LIST.stream()
                    .filter(m -> m.getStartTime() >= startTime)
                    .collect(Collectors.toList());

            if (recentMetrics.isEmpty()) {
                return Result.success(Map.of(
                        "avgResponseTime", 0,
                        "errorCount", 0,
                        "errorRate", 0.0,
                        "slowQueryCount", 0,
                        "totalRequests", 0,
                        "successCount", 0,
                        "successRate", 0.0
                ));
            }

            // 计算统计数据
            double avgResponseTime = recentMetrics.stream()
                    .mapToLong(RequestMetrics::getDuration)
                    .average()
                    .orElse(0);

            long errorCount = recentMetrics.stream()
                    .filter(m -> !m.isSuccess())
                    .count();

            double errorRate = (double) errorCount / recentMetrics.size();

            long slowQueryCount = recentMetrics.stream()
                    .filter(RequestMetrics::isSlowQuery)
                    .count();

            long successCount = recentMetrics.size() - errorCount;
            double successRate = (double) successCount / recentMetrics.size();

            // 按APIPath分组统计
            Map<String, Map<String, Object>> pathStats = recentMetrics.stream()
                    .collect(Collectors.groupingBy(
                            RequestMetrics::getPath,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                        double pathAvg = list.stream()
                                                .mapToLong(RequestMetrics::getDuration)
                                                .average()
                                                .orElse(0);
                                        long pathErrors = list.stream()
                                                .filter(m -> !m.isSuccess())
                                                .count();
                                        long pathSlowCount = list.stream()
                                                .filter(RequestMetrics::isSlowQuery)
                                                .count();
                                        return Map.of(
                                                "count", (Object) list.size(),
                                                "avgTime", pathAvg,
                                                "errorCount", pathErrors,
                                                "slowCount", pathSlowCount
                                        );
                                    }
                            )
                    ));

            Map<String, Object> result = new HashMap<>();
            result.put("avgResponseTime", Math.round(avgResponseTime * 100.0) / 100.0); // 精确到小数点后2位
            result.put("errorCount", errorCount);
            result.put("errorRate", Math.round(errorRate * 10000.0) / 10000.0);
            result.put("slowQueryCount", slowQueryCount);
            result.put("totalRequests", recentMetrics.size());
            result.put("successCount", successCount);
            result.put("successRate", Math.round(successRate * 10000.0) / 10000.0);
            result.put("timeRange", timeRange);
            result.put("pathStats", pathStats);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取健康度指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取最慢的API（Top 10）
     */
    @GetMapping("/slowest-apis")
    public Result<List<Map<String, Object>>> getSlowestApis(
            @RequestParam(defaultValue = "60") int timeRange) {
        try {
            long now = System.currentTimeMillis();
            int milliseconds = timeRange * 60 * 1000;
            long startTime = now - milliseconds;

            List<Map<String, Object>> slowestApis = MetricsFilter.METRICS_LIST.stream()
                    .filter(m -> m.getStartTime() >= startTime)
                    .sorted((a, b) -> Long.compare(b.getDuration(), a.getDuration()))
                    .limit(10)
                    .map(m -> Map.of(
                            "path", (Object) m.getPath(),
                            "method", m.getMethod(),
                            "duration", m.getDuration(),
                            "success", m.isSuccess(),
                            "timestamp", m.getStartTime()
                    ))
                    .collect(Collectors.toList());

            return Result.success(slowestApis);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取最慢API信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取错误的请求（最近100条）
     */
    @GetMapping("/failed-requests")
    public Result<List<Map<String, Object>>> getFailedRequests(
            @RequestParam(defaultValue = "60") int timeRange) {
        try {
            long now = System.currentTimeMillis();
            int milliseconds = timeRange * 60 * 1000;
            long startTime = now - milliseconds;

            List<Map<String, Object>> failedRequests = MetricsFilter.METRICS_LIST.stream()
                    .filter(m -> m.getStartTime() >= startTime && !m.isSuccess())
                    .sorted((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()))
                    .limit(100)
                    .map(m -> Map.of(
                            "path", (Object) m.getPath(),
                            "method", m.getMethod(),
                            "statusCode", m.getStatusCode(),
                            "errorMessage", m.getErrorMessage() != null ? m.getErrorMessage() : "Unknown Error",
                            "clientIp", m.getClientIp() != null ? m.getClientIp() : "",
                            "timestamp", m.getStartTime()
                    ))
                    .collect(Collectors.toList());

            return Result.success(failedRequests);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取失败请求信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时QPS（每秒请求数）
     */
    @GetMapping("/qps")
    public Result<Map<String, Object>> getQps() {
        try {
            long now = System.currentTimeMillis();
            long oneSecondAgo = now - 1000;
            long oneMinuteAgo = now - 60 * 1000;
            long fiveMinutesAgo = now - 5 * 60 * 1000;

            long qps1s = MetricsFilter.METRICS_LIST.stream()
                    .filter(m -> m.getStartTime() >= oneSecondAgo)
                    .count();

            double qps1m = MetricsFilter.METRICS_LIST.stream()
                    .filter(m -> m.getStartTime() >= oneMinuteAgo)
                    .count() / 60.0;

            double qps5m = MetricsFilter.METRICS_LIST.stream()
                    .filter(m -> m.getStartTime() >= fiveMinutesAgo)
                    .count() / 300.0;

            Map<String, Object> result = new HashMap<>();
            result.put("lastSecond", qps1s);
            result.put("lastMinute", Math.round(qps1m * 100.0) / 100.0);
            result.put("last5Minutes", Math.round(qps5m * 100.0) / 100.0);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取QPS失败: " + e.getMessage());
        }
    }

    /**
     * 清空指标数据（仅用于测试和维护）
     */
    @PostMapping("/clear")
    public Result<String> clearMetrics() {
        try {
            MetricsFilter.clearMetrics();
            return Result.success("指标数据已清空");
        } catch (Exception e) {
            return Result.error(500, "清空指标数据失败: " + e.getMessage());
        }
    }
}
