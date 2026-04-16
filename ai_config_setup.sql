-- ========== AI 配置初始化脚本 ==========
-- 使用说明：
-- 1. 只有一个 API 运营商: https://api.chatanywhere.tech/v1
-- 2. 两个提供商配置，通过不同的 model 参数区分
--    - chatgpt3.5: model='gpt-3.5-turbo'
--    - deepseek: model='deepseek-chat'
-- 3. 替换下面的 API Key 为你的真实密钥
-- 4. 在 MySQL 中执行此脚本

-- 【ChatGPT3.5】配置 (通过 ChatAnywhere 中转)
-- API 后台: https://api.chatanywhere.tech/
-- Base URL: https://api.chatanywhere.tech/v1
-- Model: gpt-3.5-turbo
INSERT INTO ai_config (provider, api_key, base_url, model, max_tokens, temperature, is_active, updated_at) 
VALUES ('chatgpt3.5', '***REMOVED***', 'https://api.chatanywhere.tech/v1', 'gpt-3.5-turbo', 2000, 0.7, 1, NOW())
ON DUPLICATE KEY UPDATE
  api_key = '***REMOVED***',
  base_url = 'https://api.chatanywhere.tech/v1',
  model = 'gpt-3.5-turbo',
  max_tokens = 2000,
  temperature = 0.7,
  is_active = 1,
  updated_at = NOW();

-- 【DeepSeek】配置 (同样使用 ChatAnywhere API)
-- 说明: 使用同一个 ChatAnywhere API 端点，但指定 deepseek-chat 模型
-- 这样可以用同一个 API Key 支持多个模型
INSERT INTO ai_config (provider, api_key, base_url, model, max_tokens, temperature, is_active, updated_at) 
VALUES ('deepseek', '***REMOVED***', 'https://api.chatanywhere.tech/v1', 'deepseek-chat', 2000, 0.7, 1, NOW())
ON DUPLICATE KEY UPDATE
  api_key = '***REMOVED***',
  base_url = 'https://api.chatanywhere.tech/v1',
  model = 'deepseek-chat',
  max_tokens = 2000,
  temperature = 0.7,
  is_active = 1,
  updated_at = NOW();

-- 验证配置
SELECT id, provider, 
       CONCAT('***', SUBSTR(api_key, -6)) as api_key_masked,
       base_url, 
       model, 
       is_active,
       updated_at 
FROM ai_config 
ORDER BY provider DESC;

-- =========================================
-- 说明：两个 provider 都激活，前端/管理员端可以通过选择 provider 来切换模型
-- - provider='chatgpt3.5' → model='gpt-3.5-turbo'
-- - provider='deepseek' → model='deepseek-chat'
-- 两个都指向同一个 API 运营商（ChatAnywhere），但通过 model 参数区分
-- =========================================

-- ========== ai_call_log 表（用于记录每次 AI 调用及 tokens 用量）==========
CREATE TABLE IF NOT EXISTS `ai_call_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT,
  `provider` VARCHAR(64) NOT NULL,
  `model` VARCHAR(128),
  `prompt` TEXT,
  `request_body` LONGTEXT,
  `response_body` LONGTEXT,
  `prompt_tokens` INT DEFAULT NULL,
  `completion_tokens` INT DEFAULT NULL,
  `total_tokens` INT DEFAULT NULL,
  `cost_in_micros` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- - 切换提供商: POST /api/v1/admin/ai-config/switch/{provider}
-- - 自然语言查询: POST /api/v1/admin/ai/query
-- - 获取激活配置: GET /api/v1/admin/ai-config/current

-- ========== ai_call_log 表（用于记录每次 AI 调用及 tokens 用量）==========
CREATE TABLE IF NOT EXISTS `ai_call_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `provider` VARCHAR(64) NOT NULL,
  `model` VARCHAR(128),
  `prompt` TEXT,
  `request_body` LONGTEXT,
  `response_body` LONGTEXT,
  `prompt_tokens` INT DEFAULT NULL,
  `completion_tokens` INT DEFAULT NULL,
  `total_tokens` INT DEFAULT NULL,
  `cost_in_micros` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入示例（可选）
-- INSERT INTO ai_call_log (provider, model, prompt, request_body, response_body, prompt_tokens, completion_tokens, total_tokens)
-- VALUES ('chatanywhere', 'gpt-3.5-turbo-ca', '测试', '{}', '{}', 5, 10, 15);

