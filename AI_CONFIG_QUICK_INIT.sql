-- ===== AI 配置快速初始化脚本 =====
-- 执行步骤：
-- 1. 启动 MySQL: mysql -u root -p
-- 2. 选择数据库: USE time_management;
-- 3. 粘贴并执行本脚本

-- 检查 ai_config 表是否存在
CREATE TABLE IF NOT EXISTS `ai_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
  `provider` VARCHAR(50) NOT NULL UNIQUE COMMENT '提供商: deepseek / chatgpt3.5',
  `api_key` VARCHAR(500) NOT NULL COMMENT 'API密钥',
  `base_url` VARCHAR(200) NOT NULL COMMENT '请求地址',
  `model` VARCHAR(50) NOT NULL COMMENT '模型名称',
  `max_tokens` INT DEFAULT 2000 COMMENT '最大tokens',
  `temperature` DOUBLE DEFAULT 0.7 COMMENT '温度参数',
  `is_active` TINYINT DEFAULT 0 COMMENT '是否激活',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  COMMENT='AI提供商配置表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 清空现有配置（可选，如果需要重新初始化）
-- DELETE FROM ai_config;

-- 插入或更新 ChatGPT3.5 配置
INSERT INTO ai_config (provider, api_key, base_url, model, max_tokens, temperature, is_active, updated_at) 
VALUES ('chatgpt3.5', '***REMOVED***', 'https://api.chatanywhere.tech/v1', 'gpt-3.5-turbo', 2000, 0.7, 1, NOW())
ON DUPLICATE KEY UPDATE
  api_key = '***REMOVED***',
  base_url = 'https://api.chatanywhere.tech/v1',
  model = 'gpt-3.5-turbo',
  is_active = 1,
  updated_at = NOW();

-- 插入或更新 DeepSeek 配置
INSERT INTO ai_config (provider, api_key, base_url, model, max_tokens, temperature, is_active, updated_at) 
VALUES ('deepseek', '***REMOVED***', 'https://api.chatanywhere.tech/v1', 'deepseek-chat', 2000, 0.7, 1, NOW())
ON DUPLICATE KEY UPDATE
  api_key = '***REMOVED***',
  base_url = 'https://api.chatanywhere.tech/v1',
  model = 'deepseek-chat',
  is_active = 1,
  updated_at = NOW();

-- 验证配置已插入
SELECT id, provider, CONCAT('***', SUBSTR(api_key, -6)) as api_key_masked, base_url, model, is_active, updated_at 
FROM ai_config 
ORDER BY provider DESC;

-- 如果需要只激活一个，执行以下命令（例如只激活 chatgpt3.5）：
-- UPDATE ai_config SET is_active = 0;
-- UPDATE ai_config SET is_active = 1 WHERE provider = 'chatgpt3.5';

-- 验证激活状态
SELECT CONCAT('激活提供商数: ', COUNT(*)) as active_count FROM ai_config WHERE is_active = 1 UNION ALL
SELECT CONCAT('总配置数: ', COUNT(*)) as total_count FROM ai_config;
