-- ========================================
-- DeepSeek API 配置 SQL
-- 说明：
-- 1. 修改下面的 API Key 为你的真实密钥（从 https://platform.deepseek.com/ 获取）
-- 2. 复制此脚本中的 UPDATE 语句到你的 MySQL 客户端执行
-- ========================================

-- 【重要】修改这里的 API Key - 用你从 DeepSeek 平台获取的真实 Key
UPDATE ai_config SET 
  api_key = 'sk-YOUR_DEEPSEEK_API_KEY_HERE',  -- 👉 替换为你的真实 Key
  base_url = 'https://api.deepseek.com/v1',
  model = 'deepseek-chat',
  max_tokens = 2000,
  temperature = 0.7,
  is_active = 0,
  updated_at = NOW()
WHERE provider = 'deepseek';

-- 若上述 UPDATE 没有更新任何行（第一次配置），则执行此 INSERT
INSERT IGNORE INTO ai_config 
(provider, api_key, base_url, model, max_tokens, temperature, is_active, created_at, updated_at) 
VALUES 
('deepseek', 'sk-YOUR_DEEPSEEK_API_KEY_HERE', 'https://api.deepseek.com/v1', 'deepseek-chat', 2000, 0.7, 0, NOW(), NOW());

-- 验证配置是否成功（执行下面的 SELECT 查看结果）
SELECT 
  id,
  provider,
  CONCAT('***', SUBSTR(api_key, -8)) as api_key_masked,
  base_url,
  model,
  max_tokens,
  temperature,
  is_active,
  created_at,
  updated_at
FROM ai_config
WHERE provider IN ('chatanywhere', 'deepseek')
ORDER BY is_active DESC, created_at DESC;

-- ========================================
-- 可选：切换激活的 AI 提供商
-- ========================================

-- 方案 1：激活 DeepSeek，禁用 ChatAnywhere
-- UPDATE ai_config SET is_active = 1 WHERE provider = 'deepseek';
-- UPDATE ai_config SET is_active = 0 WHERE provider = 'chatanywhere';

-- 方案 2：激活 ChatAnywhere，禁用 DeepSeek （保持现状）
-- UPDATE ai_config SET is_active = 1 WHERE provider = 'chatanywhere';
-- UPDATE ai_config SET is_active = 0 WHERE provider = 'deepseek';

-- ========================================
-- DeepSeek 价格参考 (根据官方公开价格)
-- ========================================
-- Input tokens:  $0.28 / 1M tokens
-- Output tokens: $1.12 / 1M tokens
--
-- 例：10000 tokens 消耗 ≈ $0.003-0.011
-- 非常经济，非常适合毕业设计！
