package com.timemanager.service;

import com.timemanager.entity.AlertLog;
import com.timemanager.mapper.AlertLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.timemanager.entity.RiskRecord;
import com.timemanager.ai.service.DynamicAiService;

@Service
@Slf4j
public class AlertPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 最近已推送的告警签名（用于去重） -> signature -> timestamp
    private final ConcurrentHashMap<String, Long> recentSignatures = new ConcurrentHashMap<>();
    // 去重窗口（毫秒）
    private final long DEDUPE_WINDOW_MS = 2 * 60 * 1000; // 2 minutes

    @Autowired
    private AlertLogMapper alertLogMapper;

    @Autowired(required = false)
    private DynamicAiService dynamicAiService;

    @Autowired
    public AlertPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToUser(String username, Object payload) {
        try {
            // 尝试将推送持久化到 alert_log 表，便于前端拉取历史告警（当后端未持久化时，前端打开面板可能覆盖本地实时告警）
            AlertLog persisted = null;
            try {
                AlertLog alert = new AlertLog();
                if (payload instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) payload;
                    Object typeObj = map.get("type");
                    if (typeObj == null) typeObj = map.get("alertType");
                    alert.setAlertType(typeObj != null ? typeObj.toString() : "USER_ALERT");

                    Object msgObj = map.get("message");
                    if (msgObj == null) msgObj = map.get("description");
                    alert.setDescription(msgObj != null ? msgObj.toString() : "");

                    Object sevObj = map.get("severity");
                    alert.setSeverity(sevObj != null ? sevObj.toString() : "low");

                    Object related = map.get("relatedUsername");
                    alert.setRelatedUsername(related != null ? related.toString() : username);
                } else {
                    alert.setAlertType("USER_ALERT");
                    alert.setDescription(objectMapper.writeValueAsString(payload));
                    alert.setSeverity("low");
                    alert.setRelatedUsername(username);
                }
                alert.setStatus(0);
                alert.setCreatedAt(LocalDateTime.now());
                if (alertLogMapper != null) {
                    alertLogMapper.insert(alert);
                    persisted = alert;
                    // 若没有显式 riskScore，则根据 severity 设默认值并尝试补全 AI 建议，确保前端 payload 一致
                    try {
                        if (persisted.getRiskScore() == null || persisted.getRiskScore() == 0) {
                            persisted.setRiskScore(mapSeverityToScore(persisted.getSeverity()));
                        }
                        String suggestion = null;
                        try {
                            if (dynamicAiService != null) {
                                suggestion = dynamicAiService.chat("安全告警：" + persisted.getDescription(), "请给出处理建议。");
                            }
                        } catch (Exception ex) {
                            log.warn("[AlertPush] ai invocation failed: {}", ex.getMessage());
                        }
                        if (suggestion == null || suggestion.trim().isEmpty() || suggestion.startsWith("抱歉") || suggestion.startsWith("AI返回")) {
                            suggestion = "请管理员及时处理";
                        }
                        persisted.setAiSuggestion(suggestion);
                        alertLogMapper.updateById(persisted);
                    } catch (Exception ex) {
                        log.warn("[AlertPush] post-persist augmentation failed: {}", ex.getMessage());
                    }
                }
                // persist completed; we'll build a safe output payload below
                // expose persisted alert via a local variable outside inner try
                // (keep 'alert' reference)
            } catch (Exception ex) {
                log.warn("[AlertPush] persist user alert failed: {}", ex.getMessage());
            }

            // 构造类型安全的输出 Map，避免未经检查的原始 Map 操作
            Map<String, Object> out = new HashMap<>();
            if (payload instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) payload;
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    Object k = e.getKey();
                    if (k instanceof String) out.put((String) k, e.getValue());
                }
            } else {
                out.put("message", objectMapper.writeValueAsString(payload));
            }
            // 如果在上面成功持久化 alert，则从 out 中确保 id/createdAt 可见；否则保留已有字段
            if (persisted != null) {
                out.putIfAbsent("id", persisted.getId());
                out.putIfAbsent("createdAt", persisted.getCreatedAt());
            }

            // 去重：构建签名（user + id 或 user + type + description 摘要）
            String sig = null;
            try {
                Object idObj = out.get("id");
                if (idObj != null) sig = "user:" + username + ":id:" + String.valueOf(idObj);
                else sig = "user:" + username + ":hash:" + Integer.toHexString((String.valueOf(out.get("type")) + "|" + String.valueOf(out.get("message"))).hashCode());
            } catch (Exception ex) { sig = "user:" + username + ":raw:" + System.identityHashCode(out); }

            if (!shouldSendSignature(sig)) {
                log.warn("[AlertPush] skip duplicate sendToUser sig={} payload={}", sig, objectMapper.writeValueAsString(out));
                return;
            }

            messagingTemplate.convertAndSendToUser(username, "/queue/alerts", out);
            log.debug("[AlertPush] sendToUser {} payload={} ", username, objectMapper.writeValueAsString(out));
        } catch (Exception e) {
            log.error("[AlertPush] sendToUser failed: {}", e.getMessage());
        }
    }

    private int mapSeverityToScore(String severity) {
        if (severity == null) return 30;
        String s = severity.toLowerCase();
        if (s.contains("critical")) return 95;
        if (s.contains("high")) return 80;
        if (s.contains("medium")) return 60;
        return 30;
    }

    public void sendToAdmins(Object payload) {
        try {
            String sig = null;
            try {
                if (payload instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) payload;
                    Object id = map.get("id");
                    if (id != null) sig = "admins:id:" + String.valueOf(id);
                    else sig = "admins:hash:" + Integer.toHexString((String.valueOf(map.get("type")) + "|" + String.valueOf(map.get("message"))).hashCode());
                } else {
                    sig = "admins:raw:" + System.identityHashCode(payload);
                }
            } catch (Exception ex) { sig = "admins:raw:" + System.identityHashCode(payload); }

            if (!shouldSendSignature(sig)) {
                log.warn("[AlertPush] skip duplicate sendToAdmins sig={}", sig);
                return;
            }

            messagingTemplate.convertAndSend("/topic/admin/alerts", payload);
            log.debug("[AlertPush] sendToAdmins payload={}", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("[AlertPush] sendToAdmins failed: {}", e.getMessage());
        }
    }

    public void sendAlertLogToAdmins(AlertLog alert) {
        if (alert == null) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", alert.getId());
            payload.put("type", alert.getAlertType());
            payload.put("severity", alert.getSeverity());
            payload.put("title", alert.getAlertType());
            payload.put("message", alert.getDescription());
            payload.put("relatedUsername", alert.getRelatedUsername());
            payload.put("relatedIp", alert.getRelatedIp());
            payload.put("riskScore", alert.getRiskScore());
            payload.put("aiSuggestion", alert.getAiSuggestion());
            payload.put("createdAt", alert.getCreatedAt());
            // 直接使用 sendToAdmins（内部有去重），但为确保按 alert id 去重，优先使用 id 签名
            sendToAdmins(payload);
        } catch (Exception ex) {
            log.error("[AlertPush] sendAlertLogToAdmins failed: {}", ex.getMessage());
        }
    }

    public void sendAlertLogToUser(String username, AlertLog alert) {
        if (alert == null || username == null) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", alert.getId());
            payload.put("type", alert.getAlertType());
            payload.put("severity", alert.getSeverity());
            payload.put("title", alert.getAlertType());
            payload.put("message", alert.getDescription());
            payload.put("relatedUsername", alert.getRelatedUsername());
            payload.put("relatedIp", alert.getRelatedIp());
            payload.put("riskScore", alert.getRiskScore());
            payload.put("aiSuggestion", alert.getAiSuggestion());
            payload.put("createdAt", alert.getCreatedAt());
            String sig = null;
            try { sig = "user:" + username + ":id:" + String.valueOf(alert.getId()); } catch (Exception ex) { sig = "user:" + username + ":raw:" + System.identityHashCode(payload); }
            if (!shouldSendSignature(sig)) {
                log.warn("[AlertPush] skip duplicate sendAlertLogToUser sig={} payloadId={}", sig, alert.getId());
                return;
            }
            messagingTemplate.convertAndSendToUser(username, "/queue/alerts", payload);
            log.debug("[AlertPush] sendAlertLogToUser {} payload={}", username, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("[AlertPush] sendAlertLogToUser failed: {}", e.getMessage());
        }
    }

    public void sendRiskRecordToAdmins(RiskRecord record) {
        if (record == null) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", record.getId());
            payload.put("type", record.getRiskType());
            payload.put("score", record.getScore());
            payload.put("message", record.getDescription());
            payload.put("relatedLogIds", record.getRelatedLogIds());
            payload.put("relatedUsername", record.getRelatedUsername());
            payload.put("relatedIp", record.getRelatedIp());
            payload.put("createdAt", record.getCreatedAt());
            sendToAdmins(payload);
        } catch (Exception ex) {
            log.error("[AlertPush] sendRiskRecordToAdmins failed: {}", ex.getMessage());
        }
    }

    /**
     * 将管理员发送的用户消息推送到指定用户的 /user/queue/messages
     */
    public void sendUserMessageToUser(String username, Object payload) {
        if (username == null) return;
        try {
            String sig = null;
            try { sig = "usermsg:" + username + ":hash:" + Integer.toHexString(String.valueOf(payload).hashCode()); } catch (Exception ex) { sig = "usermsg:" + username + ":raw:" + System.identityHashCode(payload); }
            if (!shouldSendSignature(sig)) {
                log.warn("[AlertPush] skip duplicate sendUserMessageToUser sig={}", sig);
                return;
            }
            messagingTemplate.convertAndSendToUser(username, "/queue/messages", payload);
            log.debug("[AlertPush] sendUserMessageToUser {} payload={}", username, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("[AlertPush] sendUserMessageToUser failed: {}", e.getMessage());
        }
    }

    private boolean shouldSendSignature(String sig) {
        try {
            long now = System.currentTimeMillis();
            Long prev = recentSignatures.get(sig);
            if (prev == null) {
                recentSignatures.put(sig, now);
                // occasional cleanup to avoid无限增长
                if (recentSignatures.size() > 2000) {
                    long cutoff = now - DEDUPE_WINDOW_MS * 4;
                    recentSignatures.entrySet().removeIf(e -> e.getValue() < cutoff);
                }
                return true;
            }
            if (now - prev > DEDUPE_WINDOW_MS) {
                recentSignatures.put(sig, now);
                return true;
            }
            return false;
        } catch (Exception ex) {
            return true;
        }
    }
}
