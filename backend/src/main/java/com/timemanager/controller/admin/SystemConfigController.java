package com.timemanager.controller.admin;

import com.timemanager.common.result.Result;
import com.timemanager.entity.SystemConfig;
import com.timemanager.service.OperationLogService;
import com.timemanager.service.SystemConfigService;
import com.timemanager.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统配置管理（Admin）
 */
@RestController
@RequestMapping("/api/v1/admin/config")
public class SystemConfigController {
    private static final Logger logger = LoggerFactory.getLogger(SystemConfigController.class);

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private OperationLogService operationLogService;

    private static final Map<String, String> DEFAULTS = Map.of(
        "allow_registration", "true",
        "default_task_reminder_minutes", "30",
        "max_timer_minutes", "480",
        "log_retention_days", "90"
    );

    private static final Map<String, String> DESCRIPTIONS = Map.of(
        "allow_registration", "是否开放新用户注册",
        "default_task_reminder_minutes", "任务截止前默认提醒分钟数",
        "max_timer_minutes", "单次计时最大分钟数（防误操作）",
        "log_retention_days", "操作日志保留天数（自动清理)"
    );

    /**
     * 列出所有系统配置；如果缺少关键配置则以默认值创建
     */
    @GetMapping("/list")
    public Result<List<SystemConfig>> listConfigs() {
        try {
            // ensure defaults exist
            for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
                String key = e.getKey();
                String val = systemConfigService.getConfigValue(key);
                if (val == null) {
                    SystemConfig cfg = new SystemConfig();
                    cfg.setConfigKey(key);
                    cfg.setConfigValue(e.getValue());
                    cfg.setDescription(DESCRIPTIONS.getOrDefault(key, null));
                    cfg.setCreatedAt(LocalDateTime.now());
                    cfg.setUpdatedAt(LocalDateTime.now());
                    try { systemConfigService.save(cfg); } catch (Exception ex) { logger.warn("无法创建默认配置 {}", key, ex); }
                }
            }

            List<SystemConfig> list = systemConfigService.list();
            // sort by configKey for stable UI
            list.sort(Comparator.comparing(SystemConfig::getConfigKey));
            return Result.success(list);
        } catch (Exception ex) {
            logger.error("listConfigs failed", ex);
            return Result.error(500, "读取系统配置失败: " + ex.getMessage());
        }
    }

    /**
     * 更新单个配置项（存在则更新，否则插入）
     */
    @PostMapping("/update")
    public Result<Boolean> updateConfig(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String key = body.get("configKey");
            String value = body.get("configValue");
            if (key == null || value == null) return Result.error(400, "缺少 configKey 或 configValue");

            boolean ok = systemConfigService.updateConfig(key, value);
            if (!ok) {
                // insert new
                SystemConfig cfg = new SystemConfig();
                cfg.setConfigKey(key);
                cfg.setConfigValue(value);
                cfg.setDescription(DESCRIPTIONS.getOrDefault(key, null));
                cfg.setCreatedAt(LocalDateTime.now());
                cfg.setUpdatedAt(LocalDateTime.now());
                ok = systemConfigService.save(cfg);
            }

            // 记录操作日志
            String operator = UserUtil.getCurrentUsername();
            String ip = request != null ? (request.getHeader("X-Forwarded-For")==null?request.getRemoteAddr():request.getHeader("X-Forwarded-For")) : null;
            String ua = request != null ? request.getHeader("User-Agent") : null;
            try {
                operationLogService.recordOperation(operator, "UPDATE_CONFIG", "key:" + key + " value:" + value, ok ? "SUCCESS" : "FAILED", ip, ua);
            } catch (Exception ignored) {}

            return Result.success(ok);
        } catch (Exception ex) {
            logger.error("updateConfig failed", ex);
            return Result.error(500, "更新配置失败: " + ex.getMessage());
        }
    }
}

