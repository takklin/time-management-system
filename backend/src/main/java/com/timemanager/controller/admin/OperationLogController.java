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
}
