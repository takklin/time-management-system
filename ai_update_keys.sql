-- ============================================================================
-- AI API Key 更新脚本
-- 用于更新 AI 提供商的 API Key
-- ============================================================================

-- 注意：只需在数据库中配置 API Key，后端会自动读取使用！
-- 不需要修改代码！

-- ============================================================================
-- 更新 ChatAnywhere API Key（推荐）
-- ============================================================================
UPDATE `ai_config` 
SET `api_key` = 'sk-你的ChatAnywhere-API-KEY'  -- 替换为你的实际 key
WHERE `provider` = 'chatanywhere';

-- 验证 ChatAnywhere 配置
SELECT `provider`, `api_key`, `base_url`, `model`, `is_active` 
FROM `ai_config` 
WHERE `provider` = 'chatanywhere';

-- ============================================================================
-- 更新 DeepSeek API Key（可选）
-- ============================================================================
-- UPDATE `ai_config` 
-- SET `api_key` = 'sk-你的DeepSeek-API-KEY'  -- 替换为你的实际 key
-- WHERE `provider` = 'deepseek';

-- 验证 DeepSeek 配置
-- SELECT `provider`, `api_key`, `base_url`, `model`, `is_active` 
-- FROM `ai_config` 
-- WHERE `provider` = 'deepseek';

-- ============================================================================
-- 查看所有配置
-- ============================================================================
SELECT `id`, `provider`, `api_key`, `base_url`, `model`, `is_active`, `created_at` 
FROM `ai_config` 
ORDER BY id;

-- ============================================================================
-- 说明
-- ============================================================================
-- 1. API Key 只需配置在数据库中，无需改代码
-- 2. 后端启动时会自动从 ai_config 表读取激活的配置
-- 3. base_url 已经配置好：
--    - ChatAnywhere: https://api.chatanywhere.tech/v1
--    - DeepSeek: https://api.deepseek.com/v1
-- 4. 如果 API Key 错误或无效，测试连接会返回具体错误信息
-- ============================================================================
