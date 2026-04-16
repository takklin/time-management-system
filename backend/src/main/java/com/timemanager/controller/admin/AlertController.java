package com.timemanager.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timemanager.common.result.Result;
import com.timemanager.entity.AlertLog;
import com.timemanager.mapper.AlertLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统异常预警控制器
 * 用于管理和查询异常预警日志
 */
@RestController
@RequestMapping("/api/v1/admin/alerts")
public class AlertController {

    @Autowired
    private AlertLogMapper alertLogMapper;

    /**
     * 获取未处理的预警（用于仪表盘显示）
     */
    @GetMapping("/unhandled")
    public Result<List<AlertLog>> getUnhandledAlerts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AlertLog> alerts = alertLogMapper.getUnhandledAlerts(limit);
            return Result.success(alerts);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取预警失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有预警（支持筛选和分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Integer status) {
        try {
            QueryWrapper<AlertLog> wrapper = new QueryWrapper<>();
            if (alertType != null && !alertType.isEmpty()) {
                wrapper.eq("alert_type", alertType);
            }
            if (severity != null && !severity.isEmpty()) {
                wrapper.eq("severity", severity);
            }
            if (status != null) {
                wrapper.eq("status", status);
            }
            wrapper.orderByDesc("created_at");

            long total = alertLogMapper.selectCount(wrapper);
            int offset = (page - 1) * size;
            
            wrapper.last("LIMIT " + offset + "," + size);
            List<AlertLog> alerts = alertLogMapper.selectList(wrapper);

            return Result.success(Map.of(
                    "content", alerts,
                    "total", total,
                    "pages", (total + size - 1) / size,
                    "pageNum", page,
                    "pageSize", size
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取预警列表失败: " + e.getMessage());
        }
    }

    /**
     * 标记预警为已读
     */
    @PostMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id) {
        try {
            AlertLog alert = new AlertLog();
            alert.setId(id);
            alert.setStatus(1); // 已读
            alertLogMapper.updateById(alert);
            return Result.success("预警已标记为已读");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "更新预警失败: " + e.getMessage());
        }
    }

    /**
     * 标记预警为已确认
     */
    @PostMapping("/{id}/confirm")
    public Result<String> confirmAlert(
            @PathVariable Long id,
            @RequestParam(required = false) String handledBy,
            @RequestParam(required = false) String remark) {
        try {
            AlertLog alert = new AlertLog();
            alert.setId(id);
            alert.setStatus(2); // 已确认
            alert.setHandledBy(handledBy != null ? handledBy : "admin");
            alert.setHandledAt(LocalDateTime.now());
            alertLogMapper.updateById(alert);
            return Result.success("预警已确认");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "确认预警失败: " + e.getMessage());
        }
    }

    /**
     * 获取预警统计（按类型分组）
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getAlertStatistics() {
        try {
            // 获取各种类型的预警数量
            int loginBurstCount = alertLogMapper.countByTypeAndStatus("LOGIN_BURST", 0);
            int batchDeleteCount = alertLogMapper.countByTypeAndStatus("BATCH_DELETE", 0);
            int offHoursCount = alertLogMapper.countByTypeAndStatus("OFF_HOURS_OPERATION", 0);
            int privilegeCount = alertLogMapper.countByTypeAndStatus("PRIVILEGE_ESCALATION", 0);
            int restoreCount = alertLogMapper.countByTypeAndStatus("RESTORE_BACKUP", 0);

            Map<String, Object> stats = Map.of(
                    "loginBurst", loginBurstCount,
                    "batchDelete", batchDeleteCount,
                    "offHours", offHoursCount,
                    "privilege", privilegeCount,
                    "restore", restoreCount,
                    "total", loginBurstCount + batchDeleteCount + offHoursCount + privilegeCount + restoreCount
            );

            return Result.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取预警统计失败: " + e.getMessage());
        }
    }

    /**
     * 批量确认预警
     */
    @PostMapping("/batch-confirm")
    public Result<String> batchConfirmAlerts(
            @RequestBody List<Long> alertIds,
            @RequestParam(required = false) String handledBy) {
        try {
            for (Long id : alertIds) {
                AlertLog alert = new AlertLog();
                alert.setId(id);
                alert.setStatus(2);
                alert.setHandledBy(handledBy != null ? handledBy : "admin");
                alert.setHandledAt(LocalDateTime.now());
                alertLogMapper.updateById(alert);
            }
            return Result.success("批量确认成功，共" + alertIds.size() + "条预警");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "批量确认失败: " + e.getMessage());
        }
    }
}
