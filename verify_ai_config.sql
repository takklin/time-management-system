-- ============================================================================
-- 验证 AI 配置的完整性脚本
-- ============================================================================

-- 1. 检查 ai_config 表是否存在
SHOW TABLES LIKE 'ai_config';

-- 2. 查看所有配置
SELECT 
  `id`, 
  `provider`, 
  `base_url`, 
  `model`, 
  `is_active`,
  `api_key`,
  `created_at`
FROM `ai_config` 
ORDER BY id;

-- 3. 检查是否恰好有一个激活配置
SELECT COUNT(*) as active_count 
FROM `ai_config` 
WHERE `is_active` = 1;

-- 4. 查看激活的配置详情
SELECT * FROM `ai_config` WHERE `is_active` = 1;

-- 5. 常见问题排查
-- 如果没有激活的配置，执行以下命令激活 chatanywhere：
-- UPDATE `ai_config` SET `is_active` = 1 WHERE `provider` = 'chatanywhere';

-- 6. 如果 API Key 包含占位符，需要替换为真实 Key：
-- UPDATE `ai_config` 
-- SET `api_key` = 'sk-你的真实-API-KEY' 
-- WHERE `provider` = 'chatanywhere' AND api_key LIKE '%replace%';

-- ============================================================================
-- 脚本完毕
-- ============================================================================
