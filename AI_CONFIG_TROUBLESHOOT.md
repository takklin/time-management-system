# AI 配置 "配置不存在" 问题修复指南

## 问题现象
```
POST /api/v1/admin/ai-config/test-connection/chatgpt3.5
Response: {"code":200,"msg":"success","data":{"success":false,"message":"配置不存在"}}
```

## 根本原因
数据库的 `ai_config` 表中没有相关的配置记录，导致测试连接时查询返回 NULL。

---

## 快速修复步骤（3分钟）

### 步骤 1：打开 MySQL 命令行

**Windows:**
```bash
mysql -u root -p
# 输入密码
```

**macOS/Linux:**
```bash
mysql -u root -p
# 输入密码
```

### 步骤 2：选择数据库
```sql
USE time_management;
```

### 步骤 3：执行初始化脚本

**方式 A：直接在 MySQL 中执行脚本文件**

```bash
source AI_CONFIG_QUICK_INIT.sql;
```

**方式 B：从文件导入（Windows）**
```bash
mysql -u root -p time_management < AI_CONFIG_QUICK_INIT.sql
```

**方式 C：复制粘贴 SQL 语句**

如果上面的方法不工作，直接复制粘贴以下 SQL 到 MySQL 中：

```sql
-- 创建表（如果不存在）
CREATE TABLE IF NOT EXISTS `ai_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `provider` VARCHAR(50) NOT NULL UNIQUE,
  `api_key` VARCHAR(500) NOT NULL,
  `base_url` VARCHAR(200) NOT NULL,
  `model` VARCHAR(50) NOT NULL,
  `max_tokens` INT DEFAULT 2000,
  `temperature` DOUBLE DEFAULT 0.7,
  `is_active` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入 ChatGPT3.5 配置
INSERT INTO ai_config (provider, api_key, base_url, model, max_tokens, temperature, is_active, updated_at) 
VALUES ('chatgpt3.5', '***REMOVED***', 'https://api.chatanywhere.tech/v1', 'gpt-3.5-turbo', 2000, 0.7, 1, NOW())
ON DUPLICATE KEY UPDATE
  api_key = '***REMOVED***',
  base_url = 'https://api.chatanywhere.tech/v1',
  model = 'gpt-3.5-turbo',
  is_active = 1,
  updated_at = NOW();

-- 插入 DeepSeek 配置
INSERT INTO ai_config (provider, api_key, base_url, model, max_tokens, temperature, is_active, updated_at) 
VALUES ('deepseek', '***REMOVED***', 'https://api.chatanywhere.tech/v1', 'deepseek-chat', 2000, 0.7, 1, NOW())
ON DUPLICATE KEY UPDATE
  api_key = '***REMOVED***',
  base_url = 'https://api.chatanywhere.tech/v1',
  model = 'deepseek-chat',
  is_active = 1,
  updated_at = NOW();

-- 验证配置
SELECT * FROM ai_config;
```

### 步骤 4：验证配置已成功插入

执行以下查询确认：
```sql
SELECT id, provider, SUBSTR(api_key, 1, 10) as api_key_prefix, base_url, model, is_active 
FROM ai_config;
```

**预期输出：**
```
+----+--------+------------------+---------------------------------+---------------+-----------+
| id | provider | api_key_prefix | base_url                        | model         | is_active |
+-------+--------+------------------+---------------------------------+---------------+-----------+
|  1 | chatgpt3.5 | sk-mPovre9H | https://api.chatanywhere.tech/v1 | gpt-3.5-turbo | 1         |
|  2 | deepseek   | sk-mPovre9H | https://api.chatanywhere.tech/v1 | deepseek-chat | 1         |
+----+--------+------------------+---------------------------------+---------------+-----------+
```

### 步骤 5：重启后端应用

```bash
# 停止当前的后端
# 然后重新启动
mvn spring-boot:run
# 或
java -jar target/time-manager-backend-0.0.1-SNAPSHOT.jar
```

### 步骤 6：测试连接

在前端或 Postman 中测试：
```
POST http://localhost:8080/api/v1/admin/ai-config/test-connection/chatgpt3.5
```

**预期成功响应：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "success": true,
    "message": "连接成功",
    "provider": "chatgpt3.5",
    "model": "gpt-3.5-turbo",
    "baseUrl": "https://api.chatanywhere.tech/v1"
  }
}
```

---

## 架构说明

### 单一 API 运营商 + 多模型支持

你的配置采用了高效的设计：

```
API 运营商: ChatAnywhere (https://api.chatanywhere.tech/v1)
├── Provider 1: chatgpt3.5
│   └── Model: gpt-3.5-turbo (ChatGPT)
├── Provider 2: deepseek
│   └── Model: deepseek-chat (DeepSeek)
```

**好处：**
- ✅ 使用同一个 API Key
- ✅ 通过 `model` 参数区分不同的模型
- ✅ 前端可以通过选择 `provider` 来切换模型
- ✅ 两个提供商都激活，支持零停机切换

### 工作流程

```
用户端/管理员端
    ↓
前端选择 provider (deepseek 或 chatgpt3.5)
    ↓
POST /api/v1/admin/ai-config/switch/{provider}
    ↓
后端查询 ai_config 表
    ↓
获取对应的 api_key、base_url、model
    ↓
向 ChatAnywhere API 发送请求（model 参数区分）
    ↓
返回结果
```

---

## 常见问题排查

### Q: 插入后仍然显示"配置不存在"

**A:** 检查以下几点：

1. **确认数据插入成功**
   ```sql
   SELECT COUNT(*) FROM ai_config;
   ```
   应该返回 2（或更多）

2. **确认后端重启**
   - 停止当前的后端进程
   - 重新启动应用
   - 检查日志是否有 "[AI] 已加载配置" 信息

3. **检查 provider 名称拼写**
   - `chatgpt3.5` ✅ 正确
   - `ChatGPT3.5` ❌ 错误（区分大小写）
   - `chatgpt-3.5` ❌ 错误（应该是 chatgpt3.5）

### Q: API Key 错误

**A:** 检查 API Key 是否正确：

```bash
curl -X GET "https://api.chatanywhere.tech/v1/models" \
  -H "Authorization: Bearer ***REMOVED***"
```

如果返回 401，说明 API Key 不对，需要替换为你的真实 Key。

### Q: base_url 连接失败

**A:** ChatAnywhere 有多个端点，如果主端点不稳定，可以试试其他的：

```sql
-- 试试这个
UPDATE ai_config SET base_url = 'https://api.chatanywhere.today/v1' 
WHERE provider = 'chatgpt3.5';

-- 或这个
UPDATE ai_config SET base_url = 'https://api.chatanywhere.com.cn/v1' 
WHERE provider = 'chatgpt3.5';
```

### Q: "数据库中没有AI配置"错误

**A:** 说明 `ai_config` 表完全为空。执行初始化脚本：

```bash
mysql -u root -p time_management < AI_CONFIG_QUICK_INIT.sql
```

---

## 验证清单

- [ ] 执行了 SQL 初始化脚本
- [ ] 查询 `SELECT * FROM ai_config` 返回 2 条记录
- [ ] 后端已重启
- [ ] 访问 POST `/api/v1/admin/ai-config/test-connection/chatgpt3.5` 返回 success=true
- [ ] 访问 POST `/api/v1/admin/ai-config/test-connection/deepseek` 返回 success=true
- [ ] 前端管理员页面可以正常切换模型

---

## 相关文件

- `AI_CONFIG_QUICK_INIT.sql` - 快速初始化脚本（推荐）
- `ai_config_setup.sql` - 完整的初始化脚本

## 支持的模型

| Provider | Model | 说明 |
|----------|-------|------|
| chatgpt3.5 | gpt-3.5-turbo | ChatGPT 3.5，响应快速，成本低廉 |
| deepseek | deepseek-chat | DeepSeek，成本最低 |

---

**最后修改：2026-04-16**
