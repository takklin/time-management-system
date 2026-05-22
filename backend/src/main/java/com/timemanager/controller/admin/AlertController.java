package com.timemanager.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timemanager.common.result.Result;
import com.timemanager.entity.AlertLog;
import com.timemanager.mapper.AlertLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.timemanager.service.AlertPushService;
import com.timemanager.service.UserMessageService;
import com.timemanager.mapper.UserMapper;
import com.timemanager.entity.User;
import com.timemanager.util.UserUtil;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.*;
import com.timemanager.ai.service.DynamicAiService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Autowired(required = false)
    private AlertPushService alertPushService;

    @Autowired(required = false)
    private UserMessageService userMessageService;

    @Autowired(required = false)
    private UserMapper userMapper;

    @Autowired(required = false)
    private DynamicAiService dynamicAiService;

    /**
     * 获取未处理的预警（用于仪表盘显示）
     */
    @GetMapping("/unhandled")
    public Result<List<AlertLog>> getUnhandledAlerts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            QueryWrapper<AlertLog> wrapper = new QueryWrapper<>();
            // 未处理且未删除
            wrapper.eq("status", 0);
            wrapper.eq("is_deleted", 0);
            wrapper.orderByDesc("created_at");
            wrapper.last("LIMIT " + limit);
            List<AlertLog> alerts = alertLogMapper.selectList(wrapper);
            return Result.success(alerts);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取预警失败: " + e.getMessage());
        }
    }

    /**
     * 管理员：修复历史告警中缺失的 risk_score 或 ai_suggestion，并修正权限提升文案
     */
    @PostMapping("/fix")
    public Result<String> fixAlerts() {
        try {
            LambdaQueryWrapper<AlertLog> w = new LambdaQueryWrapper<>();
            w.isNull(AlertLog::getAiSuggestion).or().eq(AlertLog::getRiskScore, 0);
            List<AlertLog> alerts = alertLogMapper.selectList(w);
            for (AlertLog alert : alerts) {
                boolean changed = false;
                if (alert.getRiskScore() == null || alert.getRiskScore() == 0) {
                    int score = "PRIVILEGE_ESCALATION".equals(alert.getAlertType()) ? 95 : 80;
                    alert.setRiskScore(score);
                    changed = true;
                }
                if (alert.getAiSuggestion() == null || alert.getAiSuggestion().trim().isEmpty()) {
                    String suggestion = null;
                    try {
                        if (dynamicAiService != null) {
                            suggestion = dynamicAiService.chat("安全告警：" + alert.getDescription(), "请给出处理建议。");
                        }
                    } catch (Exception ex) { /* best-effort */ }
                    if (suggestion == null || suggestion.trim().isEmpty() || suggestion.startsWith("抱歉") || suggestion.startsWith("AI返回")) suggestion = "请管理员及时处理";
                    alert.setAiSuggestion(suggestion);
                    changed = true;
                }
                if ("PRIVILEGE_ESCALATION".equals(alert.getAlertType()) && alert.getDescription() != null && alert.getDescription().contains("成功授予")) {
                    alert.setDescription(alert.getDescription().replace("成功授予", "尝试授予"));
                    changed = true;
                }
                if (changed) alertLogMapper.updateById(alert);
            }
            return Result.success("修复完成");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "修复失败: " + e.getMessage());
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
            // 仅返回未被删除的记录
            wrapper.eq("is_deleted", 0);
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

    /**
     * 管理员主动通知用户：将指定预警通过点对点消息发送给 alert.relatedUsername
     */
    @PostMapping("/{id}/notify")
    public Result<String> notifyUser(@PathVariable Long id,
                                     @RequestParam(required = false) String message,
                                     @RequestParam(required = false) String username) {
        try {
            AlertLog alert = alertLogMapper.selectById(id);
            if (alert == null) return Result.error(404, "告警未找到");

            // 如果请求中传递了 username，则覆盖告警关联用户名（用于演示或手动指定接收者）
            String targetUsername = username != null && !username.isEmpty() ? username : alert.getRelatedUsername();
            if (targetUsername == null || targetUsername.isEmpty()) return Result.error(400, "该告警未关联用户名，无法通知");

            // 可选：覆盖告警描述（仅修改用于构造收件箱内容的变量，不修改数据库中的 alert）
            if (message != null && !message.isEmpty()) {
                // 使用传入的 message 作为最终通知内容，但不覆盖数据库中的 alert 描述
                // 保留 alert.description 不变
            }

            // 构造用于写入收件箱的默认标题与内容（若调用方未提供 message，则以告警时间和描述为内容）
            String title = "系统通知";
            if (alert.getAlertType() != null && !alert.getAlertType().isEmpty()) {
                title = "系统告警：" + alert.getAlertType();
            }

            String contentToSend;
            if (message != null && !message.isEmpty()) {
                contentToSend = message;
            } else {
                try {
                    String timeStr = alert.getCreatedAt() != null ? alert.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "某时某刻";
                    String descRaw = alert.getDescription() != null ? alert.getDescription() : (alert.getAlertType() != null ? alert.getAlertType() : "相关操作");
                    String actionDetail = descRaw;
                    try {
                        String origUser = alert.getRelatedUsername();
                        // 如果描述中包含原用户名，移除它，避免暴露执行者
                        if (origUser != null && !origUser.isEmpty() && actionDetail.contains(origUser)) {
                            actionDetail = actionDetail.replace(origUser, "");
                        }
                        // 如果描述以 "用户 <name> 在/于 ..." 开头，提取后半部分作为动作描述
                        Pattern p = Pattern.compile("(?i)^\\s*用户\\s*[^\\s]+\\s*(?:在|于)\\s*(.*)");
                        Matcher m = p.matcher(actionDetail);
                        if (m.find()) {
                            actionDetail = m.group(1);
                        }
                        actionDetail = actionDetail.replaceAll("^[在于\\s:：,，]+", "").trim();
                        if (actionDetail.isEmpty()) actionDetail = descRaw;
                    } catch (Exception ex2) {
                        actionDetail = descRaw;
                    }
                    contentToSend = String.format("您的账号于 %s 发生了 %s，如果不是您本人操作，请及时修改密码。", timeStr, actionDetail);
                } catch (Exception ex) {
                    contentToSend = alert.getDescription() != null ? alert.getDescription() : "如果不是您本人操作，请及时修改密码。";
                }
            }

            try {
                // 先解析并查找目标用户（支持按 username、nickname、email 查找），确保我们能写入收件箱再推送
                com.timemanager.entity.User targetUser = null;
                try {
                    if (userMapper != null) {
                        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.timemanager.entity.User> q = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                        q.eq("username", targetUsername);
                        targetUser = userMapper.selectOne(q);
                        if (targetUser == null) {
                            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.timemanager.entity.User> q2 = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                            q2.eq("nickname", targetUsername);
                            targetUser = userMapper.selectOne(q2);
                        }
                        if (targetUser == null) {
                            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.timemanager.entity.User> q3 = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                            q3.eq("email", targetUsername);
                            targetUser = userMapper.selectOne(q3);
                        }
                    }
                } catch (Exception ex) {
                    // ignore lookup error
                }

                if (targetUser == null) {
                    return Result.error(404, "目标用户不存在: " + targetUsername);
                }

                // 先写入收件箱（持久化），确保后续前端标记已读有对应记录
                Long adminId = UserUtil.getCurrentUserId();
                com.timemanager.entity.UserMessage persistedMsg = null;
                try {
                    if (userMessageService != null) {
                        persistedMsg = userMessageService.sendFromAdmin(adminId, targetUser.getId(), title, contentToSend);
                    }
                } catch (Exception ex) {
                    System.err.println("write inbox failed: " + ex.getMessage());
                }

                // 再发送实时浮动告警（仅发送给目标用户），使用清理后的内容以避免暴露其他用户名
                if (alertPushService != null) {
                    try {
                        AlertLog pushAlert = new AlertLog();
                        pushAlert.setId(alert.getId());
                        pushAlert.setAlertType(alert.getAlertType());
                        pushAlert.setSeverity(alert.getSeverity());
                        pushAlert.setDescription(contentToSend);
                        pushAlert.setRelatedUsername(targetUser.getUsername());
                        pushAlert.setRelatedIp(alert.getRelatedIp());
                        pushAlert.setRiskScore(alert.getRiskScore());
                        pushAlert.setAiSuggestion(alert.getAiSuggestion());
                        pushAlert.setCreatedAt(alert.getCreatedAt());
                        alertPushService.sendAlertLogToUser(targetUser.getUsername(), pushAlert);
                    } catch (Exception ex) {
                        // best-effort fallback
                        try { alertPushService.sendAlertLogToUser(targetUser.getUsername(), alert); } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return Result.error(500, "发送通知失败: " + ex.getMessage());
            }

            return Result.success("通知已发送");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "发送通知失败: " + e.getMessage());
        }
    }

    /**
     * 管理员删除单条告警（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteAlert(@PathVariable Long id) {
        try {
            AlertLog update = new AlertLog();
            update.setId(id);
            update.setStatus(3);
            update.setIsDeleted(1);
            update.setHandledAt(LocalDateTime.now());
            alertLogMapper.updateById(update);
            return Result.success("已删除");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 管理员批量删除（软删除）
     */
    @DeleteMapping
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        try {
            for (Long id : ids) {
                AlertLog update = new AlertLog();
                update.setId(id);
                update.setStatus(3);
                update.setIsDeleted(1);
                update.setHandledAt(LocalDateTime.now());
                alertLogMapper.updateById(update);
            }
            return Result.success("批量删除完成");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 管理员清空所有告警（软删除/归档）
     */
    @DeleteMapping("/all")
    public Result<String> clearAllAlerts() {
        try {
            AlertLog update = new AlertLog();
            update.setStatus(3); // 3 表示已归档/删除
            update.setIsDeleted(1);
            // 将所有未归档的告警标记为已归档
            QueryWrapper<AlertLog> w = new QueryWrapper<>();
            w.ne("status", 3);
            alertLogMapper.update(update, w);
            return Result.success("已清空所有告警");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "清空告警失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否存在相似的告警（用于前端在接收推送前做重复判定）
     */
    @GetMapping("/exists")
    public Result<Boolean> existsAlert(@RequestParam(required = false) String alertType,
                                       @RequestParam(required = false) String relatedUsername,
                                       @RequestParam(required = false) String relatedIp) {
        try {
            QueryWrapper<AlertLog> w = new QueryWrapper<>();
            if (alertType != null && !alertType.isEmpty()) w.eq("alert_type", alertType);
            if (relatedUsername != null && !relatedUsername.isEmpty()) w.eq("related_username", relatedUsername);
            if (relatedIp != null && !relatedIp.isEmpty()) w.eq("related_ip", relatedIp);
            w.eq("is_deleted", 0);
            // 只关注未归档的告警
            w.lt("status", 3);
            long cnt = alertLogMapper.selectCount(w);
            return Result.success(cnt > 0);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "检查告警存在性失败: " + e.getMessage());
        }
    }
}
