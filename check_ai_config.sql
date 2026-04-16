-- 查看 AI 配置
SELECT 
  id,
  provider,
  CASE 
    WHEN api_key IS NULL OR api_key = '' THEN '[空]'
    WHEN LENGTH(api_key) < 10 THEN api_key
    ELSE CONCAT(SUBSTRING(api_key, 1, 10), '...', SUBSTRING(api_key, -6))
  END as api_key_preview,
  base_url,
  model,
  max_tokens,
  is_active,
  updated_at
FROM ai_config
ORDER BY is_active DESC;

-- 检查是否有 5 条以上记录（可能配置重复）
SELECT COUNT(*) as total_configs FROM ai_config;
