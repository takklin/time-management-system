<!-- 配置更新安装步骤 (Configuration Update Installation Steps) -->

## 🎯 配置更新完成清单 (Configuration Update Completion Checklist)

### 已完成的修改 (Completed Changes)

#### 1️⃣ 前端 UI 标签更新 ✅
- **文件**: `frontend/src/views/admin/AIAssistant.vue`
- **改动**:
  - 第11行: `el-option label` 从 **"ChatAnywhere"** 改为 **"ChatGPT3.5"**
  - 第11行: `el-option value` 从 **"chatanywhere"** 改为 **"chatgpt3.5"**
  - 第133行: `selectedProvider` 初始值从 **'chatanywhere'** 改为 **'chatgpt3.5'**
- **验证**: ✅ 已修改

#### 2️⃣ SQL 配置脚本更新 ✅
- **文件**: `ai_config_setup.sql`
- **改动**:
  - ChatGPT3.5 段: provider 改为 **'chatgpt3.5'** (原为 'chatanywhere')
  - DeepSeek 段: base_url 修正为 **'https://api.deepseek.com/v1'** (原错误为 chatanywhere.tech)
- **验证**: ✅ 已修改

#### 3️⃣ 数据库迁移脚本创建 ✅
- **文件**: `migrate_ai_provider_names.sql` (新建)
- **功能**: 从旧提供商名称("chatanywhere")迁移到新名称("chatgpt3.5")
- **验证**: ✅ 已创建

#### 4️⃣ 文档指南创建 ✅
- **文件**: `PROVIDER_UPDATE_GUIDE.md` (新建)
- **内容**: 部署步骤 + 验证方法 + 故障排查
- **验证**: ✅ 已创建

#### 5️⃣ 后端代码验证 ✅
- **编译**: `mvn clean compile` - 通过 ✓
- **代码检查**:
  - ✓ AdminAiConfigController.switchProvider() - 动态参数（不硬编码）
  - ✓ AiConfigManager.switchTo() - 从数据库查询（支持任意 provider 值）
  - ✓ DynamicAiService - 集成 provider 支持
- **验证**: ✅ 无编译错误

---

## 📋 后续部署步骤 (Next Deployment Steps)

### ⚠️ 重要: 这些步骤需要手动执行

#### 步骤 1: 更新数据库
```bash
# 在 MySQL 中执行这些命令之一

# 方式 A (推荐): 如果数据库中有 "chatanywhere" 旧记录
mysql -h localhost -u root -pWORD time_management < migrate_ai_provider_names.sql

# 或方式 B: 直接用新配置文件
mysql -h localhost -u root -pWORD time_management < ai_config_setup.sql

# 或方式 C: 手动 SQL 执行
UPDATE ai_config SET provider = 'chatgpt3.5' WHERE provider = 'chatanywhere';
UPDATE ai_config SET is_active = 1 WHERE provider = 'chatgpt3.5';
UPDATE ai_config SET is_active = 0 WHERE provider = 'deepseek';
```

#### 步骤 2: 验证数据库
```sql
-- 验证 AI 提供商配置
SELECT id, provider, model, is_active, updated_at FROM ai_config ORDER BY id;

-- 预期结果:
-- id | provider    | model         | is_active | updated_at
-- 1  | chatgpt3.5  | gpt-3.5-turbo | 1         | 2024-xx-xx 
-- 2  | deepseek    | deepseek-chat | 0         | 2024-xx-xx
```

#### 步骤 3: 重启后端
```bash
# 在后端项目目录执行
cd backend
mvn clean package -DskipTests

# 启动后端
java -jar target/time-manager-backend-0.0.1-SNAPSHOT.jar

# 或在 IDE 中点击 Run/Debug 按钮
```

#### 步骤 4: 测试前端
1. 打开浏览器: `http://localhost:5173`
2. 登录为管理员
3. 进入 **"/admin/ai-assistant"** 页面
4. 检查提供商下拉菜单 - 应显示 **"ChatGPT3.5"** 和 **"DeepSeek"**
5. 点击 **"测试连接"** 按钮 - 应显示 ✓
6. 输入查询并发送 - 应收到 AI 回复

---

## 📊 配置对比表 (Configuration Comparison)

| 组件 | 之前 (Before) | 之后 (After) | 状态 |
|------|--------------|-------------|------|
| **前端标签** | ChatAnywhere | ChatGPT3.5 | ✅ |
| **前端值** | chatanywhere | chatgpt3.5 | ✅ |
| **数据库 provider** | chatanywhere | chatgpt3.5 | 📋 需执行 SQL |
| **API 端点** | https://api.chatanywhere.org/v1 | https://api.chatanywhere.tech/v1 | ✅ |
| **模型名称** | gpt-3.5-turbo-ca | gpt-3.5-turbo | ✅ |
| **DeepSeek 端点** | (错误) | https://api.deepseek.com/v1 | ✅ |

---

## 🔍 故障排查 (Troubleshooting)

### ❌ 问题 1: 前端还显示 "ChatAnywhere"
**可能原因**: 浏览器缓存
**解决方案**:
```bash
# 操作步骤:
1. 按 F12 打开开发者工具
2. 右键刷新按钮，选择"清除缓存并进行硬刷新"
3. 或按 Ctrl+Shift+R (Windows) / Cmd+Shift+R (Mac)
```

### ❌ 问题 2: 测试连接失败 (404 Not Found)
**可能原因**: 数据库中没有 'chatgpt3.5' 提供商配置
**解决方案**:
```bash
# 步骤:
1. 执行 SQL 迁移脚本
2. 重启后端服务
3. 再次测试连接
```

### ❌ 问题 3: 后端日志显示 "[AI] 配置不存在: provider=chatgpt3.5"
**可能原因**: SQL 尚未执行
**解决方案**:
```bash
# 检查数据库状态
mysql> SELECT * FROM ai_config;

# 如果看不到 provider='chatgpt3.5' 的记录，执行:
mysql> UPDATE ai_config SET provider='chatgpt3.5' WHERE provider='chatanywhere';
mysql> UPDATE ai_config SET is_active=1 WHERE provider='chatgpt3.5';
```

---

## 📈 预期收益 (Expected Benefits)

✨ 完成此配置后，你将获得:

1. **正确的提供商标签** - UI 显示 "ChatGPT3.5" (而非 "ChatAnywhere")
2. **一致的前后端配置** - 前端值匹配数据库 provider 列
3. **灵活的提供商切换** - 支持在 ChatGPT3.5 和 DeepSeek 之间切换
4. **更加专业的系统** - 使用官方 provider 名称而非中介商名称

---

## ✨ 总结

所有代码和配置修改已完成！⭐

| 项目 | 状态 |
|-----|------|
| 前端修改 | ✅ 完成 |
| SQL 脚本更新 | ✅ 完成 |
| 迁移脚本 | ✅ 创建 |
| 后端编译 | ✅ 通过 |
| 文档 | ✅ 完成 |

**下一步**: 按照《后续部署步骤》执行数据库和后端更新。

预计修改耗时: **5-10 分钟**

---

*配置时间*: 2024-12-20  
*更新范围*: Frontend (AIAssistant.vue) + Database (ai_config_setup.sql) + Documentation

