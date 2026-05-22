package com.timemanager.controller.admin;

import com.timemanager.common.result.Result;
import com.timemanager.entity.OperationLog;
import com.timemanager.service.OperationLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 操作日志控制器（增强版）
 */
@RestController
@RequestMapping("/api/v1/admin/logs")
public class OperationLogController {

    @Autowired
    private OperationLogService logService;


    /**
     * 获取操作日志列表（支持筛选和分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            Page<OperationLog> pageObj = logService.getLogs(page, size, operator, action, riskLevel, result, startDate, endDate);
            
            Map<String, Object> data = new HashMap<>();
            data.put("content", pageObj.getRecords());
            data.put("total", pageObj.getTotal());
            data.put("pages", pageObj.getPages());
            data.put("pageNum", pageObj.getCurrent());
            data.put("pageSize", pageObj.getSize());
            
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取操作日志失败:" + e.getMessage());
        }
    }

    /**
     * 获取用户操作统计
     */
    @GetMapping("/stats/user-operations")
    public Result<List<Map<String, Object>>> getUserOperationStats(
            @RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> stats = new ArrayList<>();
        // 占位符实现
        return Result.success(stats);
    }

    /**
     * 获取高危操作统计
     */
    @GetMapping("/stats/high-risk")
    public Result<List<Map<String, Object>>> getHighRiskOperationStats(
            @RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> stats = new ArrayList<>();
        // 占位符实现
        return Result.success(stats);
    }

    /**
     * 手动触发异常检测（用于调试或计划任务触发）
     */
    @PostMapping("/detect-anomalies")
    public Result<Boolean> detectAnomalies() {
        try {
            logService.detectAnomalies();
            return Result.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "检测异常失败: " + e.getMessage());
        }
    }

    /**
     * 插入演示用的高危/超高风险日志，便于前端演示（会使用 recordOperation 来确保风险判定与预警生成）
     */
    @PostMapping("/seed-demo")
    public Result<Boolean> seedDemoLogs() {
        try {
            // 生成一批删除操作以触发批量删除规则（例如在5分钟内删除15次）
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < 15; i++) {
                // recordOperation 会使用当前时间作为 createdAt，循环插入会得到近似时间序列
                logService.recordOperation("qiqi5", "DELETE_TASK", "task:" + (1000 + i), "success", "127.0.0.1", "DemoAgent/1.0");
            }
            // 插入一次提权示例以触发 PRVILEGE_ESCALATION
            logService.recordOperation("qiqi5", "GRANT_ADMIN", "user:1002", "success", "127.0.0.1", "DemoAgent/1.0");
            // 一条失败登录示例
            logService.recordOperation("demo_user", "LOGIN", "login", "fail", "192.168.100.50", "DemoUA/1.0");

            // 触发一次异常检测以立刻生成基于历史的聚合预警（如 BATCH_DELETE）
            try { logService.detectAnomalies(); } catch (Exception ex) { /* ignore */ }

            return Result.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "生成示例数据失败: " + e.getMessage());
        }
    }
}
