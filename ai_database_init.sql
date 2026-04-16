-- ============================================================================
-- 时间管理系统 AI 功能数据库脚本
-- 执行此脚本添加 AI 相关表结构
-- ============================================================================

-- ============================================================================
-- 1. AI 提供商配置表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `ai_config` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  `provider` VARCHAR(20) NOT NULL UNIQUE COMMENT '提供商: deepseek / chatanywhere',
  `api_key` VARCHAR(500) NOT NULL COMMENT 'API密钥（加密存储建议）',
  `base_url` VARCHAR(200) NOT NULL COMMENT '请求地址',
  `model` VARCHAR(50) NOT NULL COMMENT '模型名称: deepseek-chat / gpt-3.5-turbo',
  `is_active` TINYINT DEFAULT 0 COMMENT '是否激活(0=未激活, 1=活跃)',
  `max_tokens` INT DEFAULT 2000 COMMENT '单次最大tokens',
  `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数(0.0-1.0)',
  `description` VARCHAR(200) COMMENT '描述信息',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_provider (provider),
  INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='AI提供商配置表';

-- ============================================================================
-- 2. AI 调用日志表（用于成本追踪和调试）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `ai_call_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `provider` VARCHAR(20) NOT NULL COMMENT '使用的提供商',
  `user_id` BIGINT COMMENT '用户ID(null表示系统自动调用)',
  `module` VARCHAR(50) NOT NULL COMMENT '模块: user_chat / admin_query / log_alert / task_parse',
  `action` VARCHAR(100) COMMENT '具体操作',
  `prompt_tokens` INT COMMENT '输入tokens',
  `completion_tokens` INT COMMENT '输出tokens',
  `total_tokens` INT COMMENT '总tokens',
  `estimated_cost` DECIMAL(10,6) COMMENT '预估成本(美元)',
  `response_time_ms` INT COMMENT '响应时间(毫秒)',
  `status` VARCHAR(20) COMMENT '状态: success / error / timeout',
  `error_message` TEXT COMMENT '错误信息',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_provider (provider),
  INDEX idx_module (module),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='AI调用日志(成本控制与调试)';

-- ============================================================================
-- 3. AI 预警表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `ai_alert` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '预警ID',
  `alert_type` VARCHAR(50) NOT NULL COMMENT '预警类型: ABNORMAL_LOGIN / BULK_DELETE / etc',
  `severity` ENUM('HIGH','MEDIUM','LOW') DEFAULT 'MEDIUM' COMMENT '严重程度',
  `title` VARCHAR(200) NOT NULL COMMENT '预警标题',
  `description` TEXT COMMENT 'AI生成的详细分析',
  `suggestion` TEXT COMMENT 'AI给出的安全建议',
  `related_log_ids` JSON COMMENT '关联的operation_log ID列表',
  `source_data` JSON COMMENT '原始数据(用于问题追踪)',
  `is_handled` TINYINT DEFAULT 0 COMMENT '是否已处理(0=未处理, 1=已处理)',
  `handler_id` BIGINT COMMENT '处理者ID',
  `handle_note` VARCHAR(500) COMMENT '处理备注',
  `handled_at` DATETIME COMMENT '处理时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_severity (severity),
  INDEX idx_handled (is_handled),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='AI智能预警表';

-- ============================================================================
-- 4. 用户AI对话历史表（可选，用于改进AI模型）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `ai_conversation` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role` ENUM('user','assistant') NOT NULL COMMENT '消息角色',
  `content` LONGTEXT NOT NULL COMMENT '消息内容',
  `tokens_used` INT COMMENT '此条消息的tokens',
  `feedback_score` TINYINT COMMENT '用户评分(1-5)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='用户AI对话历史';

-- ============================================================================
-- 5. 初始化测试数据
-- ============================================================================

-- 清除旧配置（如果存在）
DELETE FROM `ai_config` WHERE provider IN ('deepseek', 'chatanywhere');

-- 插入 DeepSeek 配置
INSERT INTO `ai_config` (
  `provider`, 
  `api_key`, 
  `base_url`, 
  `model`, 
  `is_active`,
  `max_tokens`,
  `temperature`,
  `description`
) VALUES (
  'deepseek',
  'sk-replace-with-your-deepseek-key', 
  'https://api.deepseek.com/v1',
  'deepseek-chat',
  1,
  2000,
  0.7,
  'DeepSeek官方API - 主要提供商'
);

-- 插入 ChatAnywhere 配置
INSERT INTO `ai_config` (
  `provider`, 
  `api_key`, 
  `base_url`, 
  `model`, 
  `is_active`,
  `max_tokens`,
  `temperature`,
  `description`
) VALUES (
  'chatanywhere',
  'sk-replace-with-your-chatanywhere-key', 
  'https://api.chatanywhere.tech/v1',
  'gpt-3.5-turbo',
  0,
  2000,
  0.7,
  'ChatAnywhere代理API - 备用提供商'
);

-- ============================================================================
-- 6. 可选：修改 operation_log 表（如果还没有）
-- ============================================================================
-- 注意：如果你已有 operation_log 表，请确保包含以下字段
-- 参考结构如下：

ALTER TABLE `operation_log` 
ADD COLUMN IF NOT EXISTS `ip_address` VARCHAR(50) COMMENT 'IP地址',
ADD COLUMN IF NOT EXISTS `user_agent` VARCHAR(500) COMMENT '用户代理',
ADD COLUMN IF NOT EXISTS `status` VARCHAR(20) COMMENT '操作状态: success/failed',
ADD COLUMN IF NOT EXISTS `error_message` TEXT COMMENT '错误信息',
ADD INDEX IF NOT EXISTS idx_action (action),
ADD INDEX IF NOT EXISTS idx_created_at (created_at);

-- ============================================================================
-- 7. 验证脚本（执行后查看结果）
-- ============================================================================

-- 检查表是否创建成功
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (
  'ai_config', 'ai_call_log', 'ai_alert', 'ai_conversation'
);

-- 查看 ai_config 表内容
SELECT `id`, `provider`, `model`, `is_active` FROM `ai_config`;

-- ============================================================================
-- 脚本执行完毕！
-- ============================================================================
-- 
-- 下一步：
-- 1. 替换 api_key 为实际的 DeepSeek 和 ChatAnywhere 密钥
-- 2. 确认 ai_config 表中恰好有一个 is_active=1 的记录
-- 3. 启动后端应用，验证 AiConfigManager 加载成功
-- 
-- 常见问题排查：
-- - 如果表创建失败，检查字符集是否为 utf8mb4
-- - 如果外键冲突，请先删除旧表：DROP TABLE IF EXISTS ai_config;
-- - 确保 MySQL 版本 >= 5.7（支持 JSON 字段）
-- 
-- ============================================================================
