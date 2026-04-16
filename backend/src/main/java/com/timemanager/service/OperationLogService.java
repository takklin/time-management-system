package com.timemanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timemanager.entity.OperationLog;
import com.timemanager.entity.AlertLog;
import com.timemanager.mapper.OperationLogMapper;
import com.timemanager.mapper.AlertLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务
 * 用于记录操作日志、判断风险等级、生成警告
 */
@Service
public class OperationLogService {
    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private AlertLogMapper alertLogMapper;

    /**
     * 分页获取操作日志（支持筛选）
     */
    public Page<OperationLog> getLogs(int page, int size, String operator, String action, String riskLevel, String result, String startDate, String endDate) {
        Page<OperationLog> pageObj = new Page<>(page, size);
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();
        
        if (operator != null && !operator.isEmpty()) {
            wrapper.like("operator", operator);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.like("action", action);
        }
        if (result != null && !result.isEmpty()) {
            wrapper.eq("result", result);
        }
        if (riskLevel != null && !riskLevel.isEmpty()) {
            wrapper.eq("risk_level", riskLevel);
        }
        
        // 日期范围筛选
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge("created_at", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le("created_at", endDate + " 23:59:59");
        }
        
        wrapper.orderByDesc("created_at");
        return operationLogMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 分页获取操作日志
     */
    public Page<OperationLog> getLogs(int page, int size) {
        Page<OperationLog> pageObj = new Page<>(page, size);
        return operationLogMapper.selectPage(pageObj, new QueryWrapper<OperationLog>().orderByDesc("created_at"));
    }

    /**
     * 获取高风险操作（失败登录 + 删除操作）
     */
    public List<OperationLog> getHighRiskLogs(int minutes) {
        // 获取失败登录
        List<OperationLog> failedLogins = operationLogMapper.getFailedLoginInLastMinutes(minutes);
        // 获取删除操作
        List<OperationLog> deleteActions = operationLogMapper.getDeleteOperationsInLastMinutes(minutes);
        
        // 合并
        failedLogins.addAll(deleteActions);
        return failedLogins;
    }

    /**
     * 判断操作的风险等级
     * critical: 超高风险 - 可能导致数据永久丢失、系统不可用的操作
     * high: 高危 - 影响单用户数据安全或隐私的操作
     * medium: 中危 - 有一定风险但可逆的操作
     * low: 低危 - 常规操作
     */
    public String determineRiskLevel(String action, String result) {
        if (action == null) action = "";
        if (result == null) result = "";

        // 超高风险操作
        if (action.toUpperCase().contains("DROP_TABLE") ||
            action.toUpperCase().contains("TRUNCATE") ||
            action.toUpperCase().contains("RESTORE_BACKUP") ||
            action.toUpperCase().contains("SHUTDOWN") ||
            action.toUpperCase().contains("GRANT_ADMIN")) {
            return "critical";
        }

        // 高危操作
        if ((action.toUpperCase().contains("DELETE") && result.toUpperCase().contains("SUCCESS")) ||
            action.toUpperCase().contains("DISABLE_USER") ||
            action.toUpperCase().contains("RESET_PASSWORD") ||
            action.toUpperCase().contains("BATCH_DELETE") ||
            action.toUpperCase().contains("EXPORT_ALL")) {
            return "high";
        }

        // 中危操作
        if (action.toUpperCase().contains("LOGIN") && result.toUpperCase().contains("FAIL")) {
            return "medium";
        }
        if (action.toUpperCase().contains("VIEW_SENSITIVE") ||
            action.toUpperCase().contains("DOWNLOAD_BACKUP")) {
            return "medium";
        }

        // 默认低危
        return "low";
    }

    /**
     * 记录操作日志（完整版，包含风险等级）
     */
    public void recordOperation(String operator, String action, String target, String result, String ip, String userAgent) {
        try {
            OperationLog log = new OperationLog();
            log.setOperator(operator != null && !operator.isEmpty() ? operator : "anonymous");
            log.setAction(action);
            log.setTarget(target);
            log.setResult(result);
            log.setIp(ip != null && !ip.isEmpty() ? ip : "0.0.0.0");
            log.setUserAgent(userAgent != null ? truncate(userAgent, 500) : null);
            log.setCreatedAt(LocalDateTime.now());
            
            // 自动判断风险等级
            String riskLevel = determineRiskLevel(action, result);
            log.setRiskLevel(riskLevel);
            
            operationLogMapper.insert(log);

            // 如果是高风险或超高风险操作，且操作成功或是特定类型，生成预警
            if (("high".equals(riskLevel) || "critical".equals(riskLevel)) && 
                "success".equalsIgnoreCase(result)) {
                createAlertIfNeeded(log, riskLevel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 简化版：记录操作日志
     */
    public void recordOperation(String operator, String action, String target, String result) {
        recordOperation(operator, action, target, result, null, null);
    }

    /**
     * 如果需要，创建预警
     */
    private void createAlertIfNeeded(OperationLog log, String riskLevel) {
        try {
            String action = log.getAction().toUpperCase();
            AlertLog alert = null;

            // DELETE 操作生成预警
            if (action.contains("DELETE")) {
                alert = AlertLog.builder()
                        .alertType("BATCH_DELETE")
                        .description("用户 " + log.getOperator() + " 执行了删除操作: " + log.getTarget())
                        .severity("critical".equals(riskLevel) ? "critical" : "high")
                        .relatedLogIds("[" + log.getId() + "]")
                        .relatedUsername(log.getOperator())
                        .relatedIp(log.getIp())
                        .status(0)
                        .createdAt(LocalDateTime.now())
                        .build();
            }
            // GRANT_ADMIN 操作生成超高风险预警
            else if (action.contains("GRANT_ADMIN")) {
                alert = AlertLog.builder()
                        .alertType("PRIVILEGE_ESCALATION")
                        .description("用户 " + log.getOperator() + " 尝试授予管理员权限给 " + log.getTarget())
                        .severity("critical")
                        .relatedLogIds("[" + log.getId() + "]")
                        .relatedUsername(log.getOperator())
                        .relatedIp(log.getIp())
                        .status(0)
                        .createdAt(LocalDateTime.now())
                        .build();
            }
            // RESTORE_BACKUP 操作生成预警
            else if (action.contains("RESTORE_BACKUP")) {
                alert = AlertLog.builder()
                        .alertType("RESTORE_BACKUP")
                        .description("用户 " + log.getOperator() + " 要求恢复备份")
                        .severity("critical")
                        .relatedLogIds("[" + log.getId() + "]")
                        .relatedUsername(log.getOperator())
                        .relatedIp(log.getIp())
                        .status(0)
                        .createdAt(LocalDateTime.now())
                        .build();
            }

            if (alert != null) {
                alertLogMapper.insert(alert);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取最近N条未处理的预警（用于仪表盘展示）
     */
    public List<AlertLog> getRecentAlerts(int limit) {
        return alertLogMapper.getUnhandledAlerts(limit);
    }

    /**
     * 获取统计数据
     */
    public Map<String, Object> getStatistics(int minutesRange) {
        List<OperationLog> logsInRange = operationLogMapper.selectList(
                new QueryWrapper<OperationLog>()
                        .ge("created_at", 
                            LocalDateTime.now().minusMinutes(minutesRange))
        );

        long criticalCount = logsInRange.stream()
                .filter(l -> "critical".equals(l.getRiskLevel()))
                .count();
        long highCount = logsInRange.stream()
                .filter(l -> "high".equals(l.getRiskLevel()))
                .count();

        return Map.of(
                "total", logsInRange.size(),
                "critical", criticalCount,
                "high", highCount,
                "criticalPercentage", logsInRange.isEmpty() ? 0 : (double) criticalCount / logsInRange.size()
        );
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        return str != null && str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}


