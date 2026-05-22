package com.timemanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timemanager.entity.OperationLog;
import com.timemanager.entity.AlertLog;
import com.timemanager.entity.RiskRecord;
import com.timemanager.mapper.OperationLogMapper;
import com.timemanager.mapper.AlertLogMapper;
import com.timemanager.mapper.RiskRecordMapper;
import com.timemanager.ai.service.DynamicAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import com.timemanager.service.AlertPushService;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务
 * 用于记录操作日志、判断风险等级、生成警告
 */
@Service
public class OperationLogService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OperationLogService.class);
    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private AlertLogMapper alertLogMapper;

    @Autowired(required = false)
    private DynamicAiService dynamicAiService;

    @Autowired(required = false)
    private RiskRecordMapper riskRecordMapper;

    @Autowired
    private AlertPushService alertPushService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
            // 规范化 result 字段，统一写入 success / failed
            String normalizedResult = (result == null) ? "failed" : result.trim().toLowerCase();
            if ("ok".equals(normalizedResult) || "succeeded".equals(normalizedResult) || "true".equals(normalizedResult)) {
                normalizedResult = "success";
            } else if ("failure".equals(normalizedResult) || "false".equals(normalizedResult)) {
                normalizedResult = "failed";
            }
            log.setResult(normalizedResult);
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
    private void createAlertIfNeeded(OperationLog operationLog, String riskLevel) {
        try {
            String action = operationLog.getAction() != null ? operationLog.getAction().toUpperCase() : "";
            AlertLog alert = null;

            // NOTE: 单次删除操作不再立即创建 alert_log
            // 批量删除（阈值触发）由定时任务 RiskScanScheduler 或 OperationLogService.detectAnomalies 扫描生成
            // 已移除：对 GRANT_ADMIN 的角色变更审计（不再为提权生成告警）
            if (action.contains("RESTORE_BACKUP")) {
                alert = AlertLog.builder()
                        .alertType("RESTORE_BACKUP")
                        .description("用户 " + operationLog.getOperator() + " 要求恢复备份")
                        .severity("critical")
                        .relatedLogIds("[" + operationLog.getId() + "]")
                        .relatedUsername(operationLog.getOperator())
                        .relatedIp(operationLog.getIp())
                        .status(0)
                        .createdAt(LocalDateTime.now())
                        .build();
            }

            // 恢复：对 GRANT_ADMIN 的直接操作生成告警（保留 GRANT_ADMIN 的直接告警，但禁用额外的审计表/批量扫描触发）
            if (action.contains("GRANT_ADMIN")) {
                String targetUser = extractTargetUsername(operationLog.getTarget());
                String desc = String.format("用户 %s 尝试授予管理员权限给 %s", operationLog.getOperator(), targetUser);
                alert = AlertLog.builder()
                        .alertType("PRIVILEGE_ESCALATION")
                        .description(desc)
                        .severity("critical")
                        .relatedLogIds("[" + operationLog.getId() + "]")
                        .relatedUsername(operationLog.getOperator())
                        .relatedIp(operationLog.getIp())
                        .status(0)
                        .createdAt(LocalDateTime.now())
                        .riskScore(95)
                        .build();
            }

            if (alert != null) {
                alertLogMapper.insert(alert);
                try { createAndPushRiskRecord(alert.getAlertType(), "critical".equals(alert.getSeverity()) ? 95 : 75, alert.getDescription(), alert.getRelatedLogIds(), alert.getRelatedUsername(), alert.getRelatedIp()); } catch (Exception ex) { ex.printStackTrace(); }

                // 生成短 AI 建议（不超过50字），若失败使用默认建议
                    try {
                        String suggestion = null;
                        try {
                            if (dynamicAiService != null) {
                                suggestion = dynamicAiService.chat("安全告警：" + alert.getDescription(), "请给出处理建议。");
                            }
                        } catch (Exception ex) { log.warn("[OperationLogService] ai invocation failed: {}", ex.getMessage()); }
                        if (suggestion == null || suggestion.trim().isEmpty() || suggestion.startsWith("抱歉") || suggestion.startsWith("AI返回")) {
                            suggestion = "请管理员及时处理";
                        }
                        alert.setAiSuggestion(suggestion);
                        alertLogMapper.updateById(alert);
                    } catch (Exception ex) {
                        log.warn("[OperationLogService] ai suggestion failed: {}", ex.getMessage());
                    }

                try {
                    // 立即推送给在线管理员
                    alertPushService.sendAlertLogToAdmins(alert);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 风险记录表初始化标识
    private volatile boolean riskTableInitialized = false;

    private synchronized void ensureRiskTableExists() {
        if (riskTableInitialized) return;
        try {
            String ddl = "CREATE TABLE IF NOT EXISTS `risk_record` ("+
                    "`id` BIGINT PRIMARY KEY AUTO_INCREMENT,"+
                    "`risk_type` VARCHAR(100),"+
                    "`score` INT,"+
                    "`description` VARCHAR(512),"+
                    "`related_log_ids` TEXT,"+
                    "`related_username` VARCHAR(100),"+
                    "`related_ip` VARCHAR(50),"+
                    "`status` TINYINT DEFAULT 0,"+
                    "`action_taken` VARCHAR(255),"+
                    "`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,"+
                    "INDEX `idx_risk_type` (`risk_type`), INDEX `idx_created_at` (`created_at`)"+
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            jdbcTemplate.execute(ddl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        riskTableInitialized = true;
    }

    private void createAndPushRiskRecord(String type, int score, String description, String relatedLogIds, String relatedUsername, String relatedIp) {
        try {
            if (riskRecordMapper == null) return;
            ensureRiskTableExists();
            RiskRecord r = RiskRecord.builder()
                    .riskType(type)
                    .score(score)
                    .description(description)
                    .relatedLogIds(relatedLogIds)
                    .relatedUsername(relatedUsername)
                    .relatedIp(relatedIp)
                    .status(0)
                    .createdAt(LocalDateTime.now())
                    .build();
            riskRecordMapper.insert(r);
            try { alertPushService.sendRiskRecordToAdmins(r); } catch (Exception ex) { ex.printStackTrace(); }
        } catch (Exception ex) {
            ex.printStackTrace();
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
     * 扫描最近的操作日志以检测异常模式并生成预警（可被定时任务或手动触发）
     */
    public void detectAnomalies() {
        try {
            // 1) 连续失败登录：同一IP在最近10分钟内失败次数 >= 5
            String sql1 = "SELECT ip, COUNT(*) AS cnt FROM operation_log " +
                    "WHERE action LIKE '%LOGIN%' AND LOWER(result) <> 'success' " +
                    "AND created_at >= DATE_SUB(NOW(), INTERVAL 10 MINUTE) " +
                    "GROUP BY ip HAVING COUNT(*) >= 5";
            List<Map<String, Object>> rows1 = jdbcTemplate.queryForList(sql1);
            for (Map<String, Object> r : rows1) {
                String ip = (String) r.get("ip");
                Number cnt = (Number) r.get("cnt");
                AlertLog alert = AlertLog.builder()
                        .alertType("LOGIN_BURST")
                        .description("检测到来自 IP " + ip + " 的连续失败登录 " + cnt + " 次")
                        .severity("high")
                        .relatedIp(ip)
                        .relatedLogIds(null)
                        .relatedUsername(null)
                        .riskScore(70)
                        .status(0)
                        .createdAt(LocalDateTime.now())
                        .build();
                alertLogMapper.insert(alert);
                // 创建风险记录并尝试生成 AI 建议（若失败使用默认文本）
                try { createAndPushRiskRecord(alert.getAlertType(), 70, alert.getDescription(), alert.getRelatedLogIds(), alert.getRelatedUsername(), alert.getRelatedIp()); } catch (Exception ex) { ex.printStackTrace(); }
                try {
                    String suggestion = null;
                    try {
                        if (dynamicAiService != null) {
                            suggestion = dynamicAiService.chat("安全告警：" + alert.getDescription(), "请给出处理建议。");
                        }
                    } catch (Exception ex) { log.warn("[OperationLogService] ai invocation failed: {}", ex.getMessage()); }
                    if (suggestion == null || suggestion.trim().isEmpty() || suggestion.startsWith("抱歉") || suggestion.startsWith("AI返回")) {
                        suggestion = "请管理员及时处理";
                    }
                    alert.setAiSuggestion(suggestion);
                    alertLogMapper.updateById(alert);
                } catch (Exception ex) { log.warn("[OperationLogService] ai suggestion failed: {}", ex.getMessage()); }
            }

            // 2) 批量删除：同一用户在最近10分钟内成功删除记录 >= 10
            String sql2 = "SELECT operator, COUNT(*) AS cnt FROM operation_log " +
                    "WHERE action LIKE '%DELETE%' AND LOWER(result) = 'success' " +
                    "AND created_at >= DATE_SUB(NOW(), INTERVAL 10 MINUTE) " +
                    "GROUP BY operator HAVING COUNT(*) >= 10";
            List<Map<String, Object>> rows2 = jdbcTemplate.queryForList(sql2);
            for (Map<String, Object> r : rows2) {
                String operator = (String) r.get("operator");
                Number cnt = (Number) r.get("cnt");
                AlertLog alert = AlertLog.builder()
                        .alertType("BATCH_DELETE")
                        .description("用户 " + operator + " 在最近10分钟内执行了 " + cnt + " 次删除操作")
                        .severity("critical")
                        .relatedUsername(operator)
                        .relatedLogIds(null)
                        .relatedIp(null)
                        .riskScore(90)
                        .status(0)
                        .createdAt(LocalDateTime.now())
                        .build();
                alertLogMapper.insert(alert);
                try { createAndPushRiskRecord(alert.getAlertType(), 90, alert.getDescription(), alert.getRelatedLogIds(), alert.getRelatedUsername(), alert.getRelatedIp()); } catch (Exception ex) { ex.printStackTrace(); }
                try {
                    String suggestion = null;
                    try {
                        if (dynamicAiService != null) {
                            suggestion = dynamicAiService.chat("安全告警：" + alert.getDescription(), "请给出处理建议。");
                        }
                    } catch (Exception ex) { log.warn("[OperationLogService] ai invocation failed: {}", ex.getMessage()); }
                    if (suggestion == null || suggestion.trim().isEmpty() || suggestion.startsWith("抱歉") || suggestion.startsWith("AI返回")) {
                        suggestion = "请管理员及时处理";
                    }
                    alert.setAiSuggestion(suggestion);
                    alertLogMapper.updateById(alert);
                } catch (Exception ex) { log.warn("[OperationLogService] ai suggestion failed: {}", ex.getMessage()); }
            }

            // 3) 授权/提权操作的检测已移除（不再为 GRANT_ADMIN 生成告警）
            
            // 新增：基于 mapper 的细粒度检测（用于 RiskScanScheduler 集成）
            // 1) 批量删除（按用户）
            try {
                List<Map<String, Object>> heavy = operationLogMapper.countHeavyDeletes(LocalDateTime.now().minusMinutes(5));
                for (Map<String, Object> h : heavy) {
                    String operator = (String) h.get("operator");
                    Number cnt = (Number) h.get("delete_count");
                    if (operator != null && cnt != null && cnt.intValue() >= 10) {
                        AlertLog a = AlertLog.builder()
                                .alertType("BATCH_DELETE")
                                .description("用户 " + operator + " 在5分钟内删除了 " + cnt + " 条记录")
                                .severity("high")
                                .relatedUsername(operator)
                                .riskScore(80)
                                .status(0)
                                .createdAt(LocalDateTime.now())
                                .build();
                        alertLogMapper.insert(a);
                        try { createAndPushRiskRecord(a.getAlertType(), 80, a.getDescription(), a.getRelatedLogIds(), a.getRelatedUsername(), a.getRelatedIp()); } catch (Exception ex) { ex.printStackTrace(); }
                        // 生成 AI 建议，若 AI 不可用或返回异常则回退到默认建议
                        try {
                            String suggestion = null;
                            try {
                                if (dynamicAiService != null) {
                                    suggestion = dynamicAiService.chat("安全告警：" + a.getDescription(), "请给出处理建议。");
                                }
                            } catch (Exception ex) {
                                log.warn("[OperationLogService] ai invocation failed: {}", ex.getMessage());
                            }
                            if (suggestion == null || suggestion.trim().isEmpty() || suggestion.startsWith("抱歉") || suggestion.startsWith("AI返回")) {
                                suggestion = "请管理员及时处理";
                            }
                            a.setAiSuggestion(suggestion);
                            alertLogMapper.updateById(a);
                        } catch (Exception ex) {
                            log.warn("[OperationLogService] ai suggestion failed: {}", ex.getMessage());
                        }
                        try { if (alertPushService != null) alertPushService.sendAlertLogToAdmins(a); } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
            } catch (Exception ex) { log.warn("countHeavyDeletes failed", ex); }

            // 2) 登录失败（按用户）
            try {
                List<Map<String, Object>> fails = operationLogMapper.countLoginFailures(LocalDateTime.now().minusMinutes(5));
                for (Map<String, Object> f : fails) {
                    String operator = (String) f.get("operator");
                    Number cnt = (Number) f.get("fail_count");
                    if (operator != null && cnt != null && cnt.intValue() >= 5) {
                        AlertLog a = AlertLog.builder()
                                .alertType("LOGIN_FAILURE_BURST")
                                .description("用户 " + operator + " 在5分钟内登录失败 " + cnt + " 次，可能存在暴力破解")
                                .severity("high")
                                .relatedUsername(operator)
                                .riskScore(75)
                                .status(0)
                                .createdAt(LocalDateTime.now())
                                .build();
                        alertLogMapper.insert(a);
                        try { createAndPushRiskRecord(a.getAlertType(), 75, a.getDescription(), a.getRelatedLogIds(), a.getRelatedUsername(), a.getRelatedIp()); } catch (Exception ex) { ex.printStackTrace(); }
                        try {
                            String suggestion = null;
                            try {
                                if (dynamicAiService != null) {
                                    suggestion = dynamicAiService.chat("安全告警：" + a.getDescription(), "请给出处理建议。");
                                }
                            } catch (Exception ex) {
                                log.warn("[OperationLogService] ai invocation failed: {}", ex.getMessage());
                            }
                            if (suggestion == null || suggestion.trim().isEmpty() || suggestion.startsWith("抱歉") || suggestion.startsWith("AI返回")) {
                                suggestion = "请管理员及时处理";
                            }
                            a.setAiSuggestion(suggestion);
                            alertLogMapper.updateById(a);
                        } catch (Exception ex) {
                            log.warn("[OperationLogService] ai suggestion failed: {}", ex.getMessage());
                        }
                        try { if (alertPushService != null) alertPushService.sendAlertLogToAdmins(a); } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
            } catch (Exception ex) { log.warn("countLoginFailures failed", ex); }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        return str != null && str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    /**
     * 从 target 字段中尝试提取用户名或可读标识
     */
    private String extractTargetUsername(String target) {
        if (target == null || target.isEmpty()) return "未知用户";
        try {
            if (target.startsWith("user:")) {
                String t = target.substring(5);
                return t == null || t.isEmpty() ? target : t;
            }
            // 如果包含 ':'，返回最后一段
            if (target.contains(":")) {
                String[] parts = target.split(":");
                return parts[parts.length - 1];
            }
            return target;
        } catch (Exception ex) {
            return target;
        }
    }
}


