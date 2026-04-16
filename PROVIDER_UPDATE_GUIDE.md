# 🔧 AI 提供商配置更新指南

## 📋 更新内容总结

已完成以下配置更新，确保前端和后端配置一致：

### ✅ 前端更新 (Frontend)

**文件**: `frontend/src/views/admin/AIAssistant.vue`

1. **选择器选项更新**
   ```vue
   <!-- 前: -->
   <el-option label="ChatAnywhere" value="chatanywhere" />
   
   <!-- 后: -->
   <el-option label="ChatGPT3.5" value="chatgpt3.5" />
   ```

2. **初始提供商更新** (第133行)
   ```typescript
   // 前:
   const selectedProvider = ref('chatanywhere')
   
   // 后:
   const selectedProvider = ref('chatgpt3.5')
   ```

### ✅ SQL 配置更新 (Database)

**文件**: `ai_config_setup.sql`

1. **ChatGPT3.5 配置** (现已改为主要提供商)
   ```sql
   UPDATE ai_config SET 
     api_key = 'sk-H4un53BqEQ0D9VpvbbeqCCrusdY9icJ1OYzaWeVxy0n',
     base_url = 'https://api.chatanywhere.tech/v1',
     model = 'gpt-3.5-turbo',
     is_active = 1
   WHERE provider = 'chatgpt3.5';
   ```

2. **DeepSeek 配置** (修正 API 端点)
   ```sql
   UPDATE ai_config SET 
     api_key = 'sk-H4un53BqEQ0D9VpvbbeqCzSyFusdY9icJ1OYzaWeVxy0n',
     base_url = 'https://api.deepseek.com/v1',  -- ✓ 已修正（之前错误为 chatanywhere.tech）
     model = 'deepseek-chat',
     is_active = 0
   WHERE provider = 'deepseek';
   ```

## 🚀 部署步骤

### 步骤 1️⃣ : 更新数据库配置

**方式 A** - 如果数据库中仍有 "chatanywhere" 提供商：
```bash
# 迁移旧提供商名称到新名称
mysql -h localhost -u root -p time_management < migrate_ai_provider_names.sql
```

**或者方式 B** - 直接运行配置脚本：
```bash
mysql -h localhost -u root -p time_management < ai_config_setup.sql
```

### 步骤 2️⃣ : 验证数据库配置

```sql
-- 在 MySQL 中执行验证查询
SELECT id, provider, model, is_active, 
       CONCAT('***', SUBSTR(api_key, -8)) as api_key_tail
FROM ai_config
ORDER BY is_active DESC;
```

**预期结果**:
```
| id | provider    | model         | is_active | api_key_tail |
|----|-------------|---------------|-----------|--------------|
| 1  | chatgpt3.5  | gpt-3.5-turbo | 1         | ****xy0n     |
| 2  | deepseek    | deepseek-chat | 0         | ****xy0n     |
```

### 步骤 3️⃣ : 重启后端服务

```bash
# 方式 A: 重新编译并启动
cd backend
mvn clean package -DskipTests
java -jar target/time-manager-backend-0.0.1-SNAPSHOT.jar

# 或方式 B: 直接重启（如果已编译）
# 在 IDE 中点击「重新运行」或用 kill+restart
```

### 步骤 4️⃣ : 刷新前端并测试 (可选)

1. 在浏览器中清除缓存:
   ```
   F12 打开开发者工具 → Application/Storage → Clear All
   ```

2. 重新加载页面: `Ctrl+Shift+R` (硬刷新)

3. 前往 `/admin/ai-assistant` 查看更新效果

## 🧪 功能验证

### 测试 1 - 提供商切换

1. 在前端 AI 助手页面，查看提供商下拉菜单
2. 确认选项显示为: **"ChatGPT3.5"** 和 **"DeepSeek"**
3. 默认选中: **"ChatGPT3.5"**

### 测试 2 - 连接测试

1. 点击 **"测试连接"** 按钮
2. **预期结果**: `✓ 连接成功 (ChatGPT3.5)`
3. 切换到 DeepSeek 再测一次

### 测试 3 - 查询功能

1. 输入查询: `今天新增用户数`
2. 点击 **"查询"**
3. 应该收到 AI 回复（使用 ChatGPT3.5）

## 📝 配置说明

### ChatGPT3.5 (via ChatAnywhere)
- **提供商名称**: `chatgpt3.5` (不是 "chatanywhere")
- **服务商**: ChatAnywhere (中转)
- **官方网站**: https://www.chatanywhere.com.cn/
- **API 端点**: https://api.chatanywhere.tech/v1
- **模型**: gpt-3.5-turbo
- **状态**: 激活 ✅

### DeepSeek
- **提供商名称**: `deepseek`
- **官方网站**: https://platform.deepseek.com/
- **API 端点**: https://api.deepseek.com/v1
- **模型**: deepseek-chat
- **状态**: 备用 (可激活)
- **价格**: $0.28/1M tokens (超便宜！)

## ⚠️ 常见问题

### Q1: 测试连接失败

**症状**: `✗ 连接失败: 404 Not Found`

**解决方案**:
1. 检查 API Key 是否正确复制
2. 检查 base_url 是否正确 (注意 `/v1` 后缀)
3. 运行 SQL 验证 `SELECT * FROM ai_config;`

### Q2: 刷新后提供商还是 "ChatAnywhere"

**症状**: 页面显示 "ChatAnywhere" 而不是 "ChatGPT3.5"

**解决方案**:
1. 清除浏览器缓存: `Ctrl+Shift+R`
2. 检查 `frontend/src/views/admin/AIAssistant.vue` 第133行
3. 确保改为: `const selectedProvider = ref('chatgpt3.5')`

### Q3: 后端找不到提供商配置

**症状**: `[AI] 配置不存在: provider=chatgpt3.5`

**解决方案**:
1. 执行 SQL 迁移脚本: `migrate_ai_provider_names.sql`
2. 验证数据库中确实存在 provider='chatgpt3.5' 的记录
3. 重启后端服务加载新配置

## 🔗 相关文件

- 🔨 SQL 脚本: [`ai_config_setup.sql`](./ai_config_setup.sql)
- 📋 迁移脚本: [`migrate_ai_provider_names.sql`](./migrate_ai_provider_names.sql)
- 💻 前端: [`frontend/src/views/admin/AIAssistant.vue`](./frontend/src/views/admin/AIAssistant.vue)
- ⚙️ 后端配置: [`backend/src/main/java/com/timemanager/ai/config/AiConfigManager.java`](./backend/src/main/java/com/timemanager/ai/config/AiConfigManager.java)

## ✨ 配置已完成

所有更新已准备就绪！按照上述步骤部署即可。

**预计耗时**: ~5 分钟 (包括数据库 + 后端重启)

---

*更新时间*: 2024-12-20  
*涉及组件*: Frontend + Database
