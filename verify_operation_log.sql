-- ========== 操作日志验证脚本 ==========
-- 用于检查和验证登录操作日志是否被正确记录

-- 【步骤 1】查看所有操作日志（最新的前10条）
SELECT 
  id,
  operator,
  action,
  target,
  result,
  ip,
  SUBSTR(user_agent, 1, 50) as user_agent_snippet,
  created_at
FROM operation_log
ORDER BY created_at DESC
LIMIT 10;

-- 【步骤 2】查看登录相关的日志
SELECT 
  id,
  operator,
  action,
  result,
  ip,
  created_at
FROM operation_log
WHERE action = 'LOGIN'
ORDER BY created_at DESC
LIMIT 20;

-- 【步骤 3】统计登录成功次数
SELECT COUNT(*) as 登录成功次数
FROM operation_log
WHERE action = 'LOGIN' AND result = 'SUCCESS';

-- 【步骤 4】统计登录失败次数
SELECT COUNT(*) as 登录失败次数
FROM operation_log
WHERE action = 'LOGIN' AND result LIKE 'FAILURE%';

-- 【步骤 5】按用户统计登录次数
SELECT 
  operator,
  COUNT(*) as 登录次数,
  MAX(created_at) as 最后登录时间
FROM operation_log
WHERE action = 'LOGIN' AND result = 'SUCCESS'
GROUP BY operator
ORDER BY 登录次数 DESC;

-- 【步骤 6】查看特定用户（如 qiqi）的所有操作
SELECT 
  id,
  operator,
  action,
  result,
  ip,
  created_at
FROM operation_log
WHERE operator = 'qiqi'
ORDER BY created_at DESC;

-- 【步骤 7】查看最近24小时内的登录日志
SELECT 
  operator,
  action,
  result,
  ip,
  created_at
FROM operation_log
WHERE action = 'LOGIN' 
  AND created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY created_at DESC;

-- ========== 清理脚本（谨慎使用！）==========
-- 清空所有操作日志（如需重新测试）
-- DELETE FROM operation_log;

-- 清空特定日期之前的日志
-- DELETE FROM operation_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
