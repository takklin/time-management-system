-- ================================================================
-- 数据库迁移脚本 - 增强操作日志和系统监控
-- 执行时间: 2026-04-16
-- ================================================================

-- 1. 为 operation_log 表添加 risk_level 字段（如果不存在）
ALTER TABLE `operation_log` 
ADD COLUMN `risk_level` VARCHAR(20) DEFAULT 'low' COMMENT '风险等级(critical/high/medium/low)' AFTER `result`;

-- 2. 为 operation_log 表创建索引（优化查询性能）
ALTER TABLE `operation_log` ADD INDEX IF NOT EXISTS `idx_risk_level` (`risk_level`);
ALTER TABLE `operation_log` ADD INDEX IF NOT EXISTS `idx_ip_time` (`ip`, `created_at`);

-- 3. 创建异常预警表
CREATE TABLE IF NOT EXISTS `alert_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `alert_type` VARCHAR(50) NOT NULL COMMENT '预警类型(LOGIN_BURST/BATCH_DELETE/OFF_HOURS_OPERATION/PRIVILEGE_ESCALATION/RESTORE_BACKUP)',
  `description` VARCHAR(255) NOT NULL COMMENT '预警描述',
  `severity` ENUM('high','critical') NOT NULL DEFAULT 'high' COMMENT '严重级别(high/critical)',
  `related_log_ids` TEXT COMMENT '关联的日志ID列表(JSON数组)',
  `related_username` VARCHAR(100) COMMENT '关联的用户名',
  `related_ip` VARCHAR(50) COMMENT '关联的IP地址',
  `status` TINYINT DEFAULT 0 COMMENT '0未处理，1已读，2已确认',
  `handled_by` VARCHAR(100) COMMENT '处理人',
  `handled_at` DATETIME COMMENT '处理时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_alert_type` (`alert_type`),
  INDEX `idx_alert_status` (`status`),
  INDEX `idx_alert_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统异常预警日志表';

-- 4. 创建系统性能指标表（可选，用于长期存储而不是仅在内存中）
CREATE TABLE IF NOT EXISTS `system_health_metrics` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `metric_time` DATETIME NOT NULL COMMENT '指标统计时间',
  `avg_response_time` DECIMAL(10, 2) COMMENT '平均响应时间(ms)',
  `error_count` INT DEFAULT 0 COMMENT '错误请求数',
  `error_rate` DECIMAL(5, 4) COMMENT '错误率(0-1)',
  `slow_query_count` INT DEFAULT 0 COMMENT '慢查询数(>2000ms)',
  `total_requests` INT DEFAULT 0 COMMENT '总请求数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_metric_time` (`metric_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统性能指标历史表';

-- 5. 创建登录失败统计临时表（用于快速检测登录爆破）
CREATE TABLE IF NOT EXISTS `login_failure_stat` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `ip` VARCHAR(50) NOT NULL COMMENT 'IP地址',
  `failure_count` INT DEFAULT 1 COMMENT '失败次数',
  `last_failure_time` DATETIME COMMENT '最后一次失败时间',
  `status` TINYINT DEFAULT 0 COMMENT '0正常，1已锁定',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录失败IP统计表';

-- 6. 添加注释说明（可选）
-- risk_level 说明：
--   critical: 超高风险 - 可能导致数据永久丢失、系统不可用的操作
--   high: 高危 - 影响单用户数据安全或隐私的操作
--   medium: 中危 - 有一定风险但可逆的操作
--   low: 低危 - 常规操作
