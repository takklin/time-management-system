-- 新增操作日志表：operation_log
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT,
  `username` VARCHAR(100),
  `action` VARCHAR(200) NOT NULL,
  `detail` TEXT,
  `ip` VARCHAR(45),
  `user_agent` VARCHAR(255),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT 0,
  INDEX `idx_operation_user` (`user_id`),
  INDEX `idx_operation_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
