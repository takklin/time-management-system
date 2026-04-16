-- ========════════════════════════════════════════════════════════
-- AI 提供商名称迁移脚本
-- 功能：从 "chatanywhere" 提供商迁移到 "chatgpt3.5"
-- 作用：确保前端选择器与数据库配置一致
-- ========════════════════════════════════════════════════════════

-- 第1步：检查现有配置
SELECT id, provider, api_key, model, is_active FROM ai_config;

-- 第2步：如果存在 "chatanywhere" 记录，改名为 "chatgpt3.5"
UPDATE ai_config SET 
  provider = 'chatgpt3.5'
WHERE provider = 'chatanywhere' 
  AND is_active = 1;

-- 第3步：清理任何残留的 "chatanywhere" 记录（可选）
-- DELETE FROM ai_config WHERE provider = 'chatanywhere';

-- 第4步：确保 chatgpt3.5 配置正确
UPDATE ai_config SET 
  api_key = 'sk-H4un53BqEQ0D9VpvbbeqCCrusdY9icJ1OYzaWeVxy0n',  -- 👉 替换为你的真实 API Key
  base_url = 'https://api.chatanywhere.tech/v1',
  model = 'gpt-3.5-turbo',
  max_tokens = 2000,
  temperature = 0.7,
  is_active = 1
WHERE provider = 'chatgpt3.5';

-- 第5步：确保 deepseek 配置正确
UPDATE ai_config SET 
  api_key = 'sk-H4un53BqEQ0D9VpvbbeqCzSyFusdY9icJ1OYzaWeVxy0n',  -- 👉 替换为你的真实 DeepSeek API Key
  base_url = 'https://api.deepseek.com/v1',
  model = 'deepseek-chat',
  max_tokens = 2000,
  temperature = 0.7,
  is_active = 0
WHERE provider = 'deepseek';

-- 第6步：验证最終結果
-- 应该看到：
-- - chatgpt3.5: is_active = 1 (主要提供商)
-- - deepseek: is_active = 0 (备选提供商)
SELECT id, provider, 
       CONCAT('***', SUBSTR(api_key, -6)) as api_key_masked,
       base_url, 
       model, 
       max_tokens,
       temperature,
       is_active,
       DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%S') as updated_at
FROM ai_config 
ORDER BY is_active DESC, id ASC;

-- ========════════════════════════════════════════════════════════
-- 验证清单
-- ========════════════════════════════════════════════════════════
-- ✅ 前端 AIAssistant.vue:
--   - 选择器选项标签: "ChatGPT3.5" / "DeepSeek" ✓
--   - 选择器选项值: "chatgpt3.5" / "deepseek" ✓
--   - 初始值: ref('chatgpt3.5') ✓

-- ✅ 后端代码:
--   - switchProvider() 接受参数，从数据库查询 ✓
--   - DynamicAiService 支持任意 provider 值 ✓

-- ✅ 数据库 ai_config 表:
--   - provider='chatgpt3.5', is_active=1 (需要通过此脚本配置)
--   - provider='deepseek', is_active=0 (可选)

-- ========════════════════════════════════════════════════════════
