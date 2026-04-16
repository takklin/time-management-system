-- ============================================================================
-- AI 配置修复脚本
-- 用于修复重复的激活配置问题
-- ============================================================================

-- 1. 查看当前状态
SELECT `id`, `provider`, `is_active`, `api_key`, `base_url`, `model` FROM `ai_config` ORDER BY id;

-- 2. 禁用所有配置
UPDATE `ai_config` SET `is_active` = 0 WHERE `is_active` = 1;

-- 3. 激活 ChatAnywhere（作为主配置）
UPDATE `ai_config` SET `is_active` = 1 WHERE `provider` = 'chatanywhere';

-- 4. 验证结果
SELECT `id`, `provider`, `is_active`, `api_key`, `base_url`, `model` FROM `ai_config` ORDER BY id;

-- ============================================================================
-- 脚本完毕（执行后应该只有一条记录 is_active=1）
-- ============================================================================
