# AI 助手功能 - 测试与故障排查指南

## 🔍 问题诊断

### 问题 1: 测试连接报"无效的响应格式"

**原因**: 前端响应处理逻辑过于复杂，导致解析失败

**已修复的改进**:
- ✅ 简化了响应处理逻辑
- ✅ 移除了多层嵌套判断
- ✅ 添加了详细的日志记录

**调试步骤**:
1. 打开浏览器开发者工具 (F12)
2. 进入 Console 标签
3. 点击"测试连接"
4. 查看 `[AI]` 开头的日志消息
5. 如果看到 "连接测试成功" 说明没问题，否则查看错误信息

### 问题 2: 刷新页面后聊天记录消失

**解决方案**: 已实现 localStorage 持久化

**工作流程**:
1. ✅ 页面加载时自动从 localStorage 恢复聊天记录
2. ✅ 每条消息发送后自动保存
3. ✅ 提供"清空记录"按钮手动清理
4. ✅ 切换页面后返回仍能看到历史

---

## 🧪 验证步骤

### 步骤 1: 验证数据库配置

在 MySQL 执行 [verify_ai_config.sql](verify_ai_config.sql):

```bash
# 应该看到：
- is_active = 1 的提供商有且仅有一个
- api_key 不包含 'replace' 字样
- base_url 格式正确
```

### 步骤 2: 验证后端编译

```bash
cd backend
mvn clean package -DskipTests
# 应该显示 BUILD SUCCESS
```

### 步骤 3: 验证 API Key

数据库中的 `api_key` 应该是真实的密钥，格式为:
```
sk-xxx...
```

**不是这个**:
```
sk-replace-with-your-xxx-key
```

如果是占位符，执行更新:
```sql
UPDATE `ai_config` 
SET `api_key` = 'sk-你的真实KEY' 
WHERE `provider` = 'chatanywhere';
```

### 步骤 4: 测试连接

1. 重启后端服务
2. 打开 `/admin/ai-assistant`
3. 点击"测试连接"按钮
4. 应该看到:
   - ✅ "连接成功" 消息
   - ✅ 显示提供商和模型信息

### 步骤 5: 测试聊天记录持久化

1. 进行一条查询
2. 按 F5 刷新页面
3. **预期结果**: 之前的聊天记录应该还在
4. 可以点击"🗑️ 清空记录"手动清除

### 步骤 6: 测试查询功能

1. 输入查询问题（例如"今天新增多少用户"）
2. 点击"查询"或按 Enter
3. 应该看到 AI 的回复（来自 `rawData` 的自然语言转述）

---

## 🐛 常见问题排查

### Q1: 测试连接超时或连接失败

**检查清单**:
```bash
□ API Key 是否正确？
□ API Key 是否已过期？
□ 是否有网络连接？
□ 防火墙是否阻止了 HTTPS 请求？
```

**解决方案**:
```sql
-- 查看当前 API Key
SELECT provider, api_key, base_url FROM ai_config WHERE is_active = 1;

-- 更新 API Key
UPDATE ai_config 
SET api_key = 'sk-新的KEY' 
WHERE provider = 'chatanywhere';
```

### Q2: 聊天记录仍然消失

**检查清单**:
```bash
□ 浏览器是否允许 localStorage？
□ localStorage 是否已满？
□ 浏览器是否为私密/隐身模式？
□ 是否手动清空了浏览器数据？
```

**调试**:
在浏览器 Console 执行:
```javascript
// 查看保存的数据
localStorage.getItem('ai_assistant_chat_history')

// 手动清空
localStorage.removeItem('ai_assistant_chat_history')
```

### Q3: 查询返回错误消息

**错误示例**:
```
❌ 查询失败: 404 Not Found
```

**解决方案**:
1. 检查 base_url 在数据库中是否正确
2. 查看后端日志中的完整错误信息
3. 使用 [verify_ai_config.sql](verify_ai_config.sql) 检查配置

### Q4: 后端显示"配置不存在"

**解决**:
```sql
-- 检查配置是否存在
SELECT COUNT(*) FROM ai_config WHERE provider = 'chatanywhere';

-- 如果为 0，需要插入
INSERT INTO ai_config (provider, api_key, base_url, model, is_active)
VALUES ('chatanywhere', 'sk-你的KEY', 'https://api.chatanywhere.tech/v1', 'gpt-3.5-turbo', 1);
```

---

## 📝 配置对照表

| 项目 | ChatAnywhere | DeepSeek |
|------|--------------|----------|
| Base URL | `https://api.chatanywhere.tech/v1` | `https://api.deepseek.com/v1` |
| Model | `gpt-3.5-turbo` | `deepseek-chat` |
| API Key | ChatAnywhere 官方 key | DeepSeek 官方 key |
| 免费额度 | 有（限制较多） | 有（限制较宽松） |

---

## 🚀 性能优化建议

1. **聊天记录大小**
   - localStorage 限制通常为 5-10MB
   - 建议限制保存最近 100 条消息
   - 可定期清空旧记录

2. **API 调用优化**
   - 添加请求去重，避免重复查询
   - 实现查询结果缓存
   - 设置合理的超时时间

3. **前端性能**
   - 考虑虚拟滚动（消息很多时）
   - 添加加载状态指示
   - 实现消息分页加载

---

## 📞 获取更多帮助

如果问题仍未解决，请收集以下信息:
1. 浏览器控制台的完整错误日志
2. 后端应用的日志输出
3. 数据库中的配置截图
4. 网络请求的详细信息（F12 → Network 标签）

