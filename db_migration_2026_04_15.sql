-- 数据库迁移脚本 - 增强用户管理、操作日志、系统配置
-- 执行时间: 2026-04-15

-- 1. 为User表添加新字段
ALTER TABLE `user` ADD COLUMN `last_login_time` DATETIME COMMENT '最后登录时间' AFTER `avatar`;
ALTER TABLE `user` ADD COLUMN `status` VARCHAR(20) DEFAULT 'active' COMMENT '账号状态(active/disabled)' AFTER `role`;
ALTER TABLE `user` ADD COLUMN `login_count` INT DEFAULT 0 COMMENT '登录次数' AFTER `status`;

-- 2. 为Task表添加新字段（如果需要）
-- ALTER TABLE `task` ADD COLUMN `completion_rate` FLOAT DEFAULT 0 COMMENT '完成率' AFTER `actual_minutes`;

-- 3. 创建系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值',
  `description` VARCHAR(255) COMMENT '描述',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 4. 初始化系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
  ('allow_registration', 'true', '是否开放新用户注册'),
  ('default_task_reminder_minutes', '30', '任务截止前默认提醒分钟数'),
  ('max_timer_minutes', '480', '单次计时最大分钟数'),
  ('log_retention_days', '90', '操作日志保留天数'),
  ('enable_user_analysis', 'true', '是否启用用户行为分析');

-- 5. 创建数据备份表
CREATE TABLE IF NOT EXISTS `db_backup` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `backup_name` VARCHAR(100) NOT NULL COMMENT '备份名称',
  `backup_file` VARCHAR(255) COMMENT '备份文件路径',
  `backup_type` VARCHAR(20) COMMENT '备份类型(full/incremental)',
  `file_size` BIGINT COMMENT '文件大小(字节)',
  `status` VARCHAR(20) DEFAULT 'success' COMMENT '备份状态',
  `backup_time` DATETIME COMMENT '备份时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_backup_time` (`backup_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库备份记录表';

-- 6. 创建用户行为统计表（用于缓存分析结果）
CREATE TABLE IF NOT EXISTS `user_behavior_stats` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `task_count` INT DEFAULT 0 COMMENT '任务数量',
  `task_completed` INT DEFAULT 0 COMMENT '完成任务数',
  `focus_minutes` INT DEFAULT 0 COMMENT '专注时长(分钟)',
  `category_usage` TEXT COMMENT '分类使用情况(JSON)',
  `active_hours` TEXT COMMENT '活跃时段(JSON)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  UNIQUE KEY `uk_user_date` (`user_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为统计表';

-- 确保operation_log表存在且有必要字段
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `operator` VARCHAR(100) COMMENT '操作者用户名',
  `action` VARCHAR(100) COMMENT '操作类型',
  `target` VARCHAR(100) COMMENT '操作对象',
  `result` VARCHAR(20) COMMENT '操作结果(success/failed)',
  `risk_level` VARCHAR(20) DEFAULT 'low' COMMENT '风险等级(low/medium/high)',
  `ip` VARCHAR(50) COMMENT 'IP地址',
  `user_agent` VARCHAR(500) COMMENT 'User-Agent',
  `request_params` TEXT COMMENT '请求参数(脱敏)',
  `response_data` TEXT COMMENT '响应数据',
  `error_message` TEXT COMMENT '错误信息',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_operator` (`operator`),
  INDEX `idx_action` (`action`),
  INDEX `idx_risk_level` (`risk_level`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
