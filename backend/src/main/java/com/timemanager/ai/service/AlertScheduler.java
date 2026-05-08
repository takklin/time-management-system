package com.timemanager.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时触发器：定期扫描操作日志并生成 AI 预警
 */
@Slf4j
@Component
public class AlertScheduler {

    @Autowired
    private AdminAiService adminAiService;

    /**
     * 每5分钟执行一次扫描（首次延迟1分钟）
     */
    @Scheduled(initialDelayString = "60000", fixedDelayString = "300000")
    public void runPeriodicScan() {
        try {
            log.info("[AlertScheduler] 定时触发日志扫描和预警生成");
            adminAiService.scanLogsAndGenerateAlerts();
        } catch (Exception e) {
            log.error("[AlertScheduler] 执行扫描失败", e);
        }
    }
}
