package com.timemanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timemanager.entity.AlertLog;
import com.timemanager.entity.OperationLog;
import com.timemanager.mapper.AlertLogMapper;
import com.timemanager.mapper.OperationLogMapper;
import com.timemanager.ai.service.DynamicAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定时风控扫描器：按照规则扫描 operation_log，生成 alert_log 并推送
 */
@Component
@Slf4j
public class RiskScanScheduler {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private AlertLogMapper alertLogMapper;

    @Autowired(required = false)
    private AlertPushService alertPushService;

    @Autowired(required = false)
    private DynamicAiService dynamicAiService;

    // 每5分钟执行一次，首次延迟1分钟
    @Scheduled(initialDelayString = "60000", fixedDelayString = "300000")
    public void scanRisk() {
        try {
            log.info("[RiskScanScheduler] start scanning operation_log");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fiveMinutesAgo = now.minusMinutes(5);

            detectBatchDelete(fiveMinutesAgo, now);
            // detectPrivilegeEscalation 已移至 OperationLogService 的实时记录逻辑，避免重复基于审计表/扫描产生告警
            detectLoginFailureBurst(fiveMinutesAgo, now);
        } catch (Exception e) {
            log.error("[RiskScanScheduler] scanRisk failed", e);
        }
    }

    // 规则1：单用户5分钟内删除次数 >= 10
    private void detectBatchDelete(LocalDateTime since, LocalDateTime now) {
        try {
            List<Map<String, Object>> heavy = operationLogMapper.countHeavyDeletes(since);
            if (heavy == null) return;
            for (Map<String, Object> row : heavy) {
                String operator = row.get("operator") == null ? null : row.get("operator").toString();
                Number cntNum = (Number) row.get("delete_count");
                long count = cntNum == null ? 0L : cntNum.longValue();
                if (operator == null) continue;
                if (count < 10) continue;

                AlertLog alert = new AlertLog();
                alert.setAlertType("BATCH_DELETE");
                alert.setDescription(String.format("用户 %s 在5分钟内删除了 %d 条记录", operator, count));
                alert.setSeverity("high");
                alert.setRelatedUsername(operator);
                alert.setRiskScore(80);
                alert.setStatus(0);
                alert.setCreatedAt(now);
                alertLogMapper.insert(alert);

                try {
                    String suggestion = null;
                    try {
                        if (dynamicAiService != null) {
                            suggestion = dynamicAiService.chat("安全告警：" + alert.getDescription(), "请给出处理建议。");
                        }
                    } catch (Exception ex) {
                        log.warn("[RiskScanScheduler] ai suggestion invocation failed: {}", ex.getMessage());
                    }
                    if (suggestion == null || suggestion.trim().isEmpty() || suggestion.startsWith("抱歉") || suggestion.startsWith("AI返回")) {
                        suggestion = "请管理员及时处理";
                    }
                    alert.setAiSuggestion(suggestion);
                    alertLogMapper.updateById(alert);
                } catch (Exception ex) {
                    log.warn("[RiskScanScheduler] ai suggestion failed: {}", ex.getMessage());
                }

                try {
                    if (alertPushService != null) alertPushService.sendAlertLogToAdmins(alert);
                } catch (Exception ex) {
                    log.warn("[RiskScanScheduler] push to admins failed: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[RiskScanScheduler] detectBatchDelete error", e);
        }
    }


    // 规则3：同一用户5分钟内登录失败 >=5 次
    private void detectLoginFailureBurst(LocalDateTime since, LocalDateTime now) {
        try {
            List<Map<String, Object>> rows = operationLogMapper.countLoginFailures(since);
            if (rows == null) return;
            for (Map<String, Object> r : rows) {
                String operator = r.get("operator") == null ? null : r.get("operator").toString();
                Number cntNum = (Number) r.get("fail_count");
                long failCount = cntNum == null ? 0L : cntNum.longValue();
                if (operator == null) continue;
                if (failCount < 5) continue;

                AlertLog alert = new AlertLog();
                alert.setAlertType("LOGIN_FAILURE_BURST");
                alert.setDescription(String.format("用户 %s 在5分钟内登录失败 %d 次，可能存在暴力破解", operator, failCount));
                alert.setSeverity("high");
                alert.setRelatedUsername(operator);
                alert.setRiskScore(75);
                alert.setStatus(0);
                alert.setCreatedAt(now);
                alertLogMapper.insert(alert);

                try {
                    String suggestion = null;
                    try {
                        if (dynamicAiService != null) {
                            suggestion = dynamicAiService.chat("安全告警：" + alert.getDescription(), "请给出处理建议。");
                        }
                    } catch (Exception ex) {
                        log.warn("[RiskScanScheduler] ai suggestion invocation failed: {}", ex.getMessage());
                    }
                    if (suggestion == null || suggestion.trim().isEmpty() || suggestion.startsWith("抱歉") || suggestion.startsWith("AI返回")) {
                        suggestion = "请管理员及时处理";
                    }
                    alert.setAiSuggestion(suggestion);
                    alertLogMapper.updateById(alert);
                } catch (Exception ex) {
                    log.warn("[RiskScanScheduler] ai suggestion failed: {}", ex.getMessage());
                }

                try {
                    if (alertPushService != null) alertPushService.sendAlertLogToAdmins(alert);
                } catch (Exception ex) {
                    log.warn("[RiskScanScheduler] push to admins failed: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[RiskScanScheduler] detectLoginFailureBurst error", e);
        }
    }

    // 注意：权限提升（提权）相关审计逻辑已移出本定时扫描器，改为在操作记录写入时直接生成（OperationLogService）或由审计/管理员人工判断
}
