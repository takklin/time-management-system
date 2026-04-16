# AI 模型切换功能说明

## 📋 功能概述

用户和管理员都可以在系统中灵活切换 AI 模型。系统支持两个 AI 模型：
- **ChatGPT3.5** （通过 ChatAnywhere API）
- **DeepSeek** （通过 ChatAnywhere API）

## 🎯 用户端模型切换（用户浮窗）

### 功能介绍

用户在右下角浮窗中可以随时切换 AI 模型。每条消息都可以使用不同的模型进行处理。

### 操作流程

1. **打开 AI 助手浮窗**
   - 点击右下角的 💬 浮动按钮

2. **选择模型**
   - 在浮窗头部看到下拉菜单
   - 选项：`ChatGPT3.5` 或 `DeepSeek`
   - 系统会显示"已切换到 xxx 模型"的提示

3. **发送消息**
   - 选定模型后输入消息并发送
   - 该消息会使用选中的 AI 模型处理

4. **模型持久化**
   - 选择的模型会自动保存到浏览器本地存储 (`localStorage`)
   - 下次打开浮窗时会自动恢复上次选择的模型

### 前端实现细节

**文件Modified:**
- `frontend/src/components/user/AIChatAssistant.vue`
- `frontend/src/api/user/ai.ts`

**关键改动：**

1. **AIChatAssistant.vue**
   - 在 `<template>` 中：`<el-select v-model="selectedModel" @change="onModelChange">`
   - 在 `<script>` 中新增 `onModelChange()` 方法，保存选择到 localStorage
   - 修改 `sendMessage()` 方法，调用 API 时传递 `model` 参数
   - 模型映射：`'gpt-3.5' -> 'chatgpt3.5'`, `'deepseek' -> 'deepseek'`

2. **api/user/ai.ts**
   - 更新 `ChatRequest` 接口，添加可选的 `model` 字段
   ```typescript
   export interface ChatRequest {
     message: string
     model?: string  // 可选：指定 AI 模型
   }
   ```

---

## 👨‍💼 管理员端模型切换

### 功能介绍

管理员可以在 AI 配置页面全局切换使用的 AI 模型。这是一个全局配置，影响管理员所有的查询。

### 操作流程

1. **进入 AI 助手页面**
   - 管理后台 → AI 助手

2. **配置卡片** - 顶部显示配置管理
   - 在"🔧 AI 配置"卡片中
   - 下拉菜单选择：`ChatGPT3.5` 或 `DeepSeek`
   - 点击"切换"或选择后自动应用

3. **测试连接**
   - (可选) 点击"测试连接"按钮验证所选模型可用
   - 显示连接状态和当前模型信息

4. **查询数据**
   - 模型切换后，所有新的查询都会使用新模型
   - 在"💬 智能查询助手"区域输入问题并查询

### 后端实现细节

**文件Modified:**
- `backend/src/main/java/com/timemanager/controller/AdminAiController.java` (无需改动)
- `backend/src/main/java/com/timemanager/ai/service/AdminAiService.java` (无需改动)

**工作流程：**
1. 管理员调用 `POST /api/v1/admin/ai-config/switch/{provider}` 
2. 系统全局切换配置（数据库更新 `is_active` 字段）
3. 后续所有 `AdminAiService.handleNaturalLanguageQuery()` 调用
4. 使用 `DynamicAiService.chat()` 获取当前激活配置
5. 执行查询时使用当前模型

---

## 🔌 后端 API 接口

### 用户 AI 接口 - `/api/v1/user/ai/chat`

**请求方式：** `POST`

**请求体：**
```json
{
  "message": "帮我创建一个任务",
  "model": "chatgpt3.5"  // 可选，不指定则使用当前激活的
}
```

**响应：**
```json
{
  "success": true,
  "code": 200,
  "message": "",
  "data": "📝 好的！告诉我任务是什么呢？..."
}
```

**支持的 model 值：**
- `"chatgpt3.5"` - 使用 ChatGPT3.5 模型
- `"deepseek"` - 使用 DeepSeek 模型
- `null` 或空 - 使用当前激活的模型

### 管理员配置切换接口 - `/api/v1/admin/ai-config/switch/{provider}`

**请求方式：** `POST`

**路径参数：**
- `provider`: 提供商名称
  - `"chatgpt3.5"`
  - `"deepseek"`

**响应：**
```json
{
  "success": true,
  "code": 200,
  "message": "",
  "data": {
    "success": true,
    "message": "已切换到 chatgpt3.5"
  }
}
```

---

## 🔧 系统架构

```
┌─────────────────────────────────────────────────────┐
│  前端 (Vue 3)                                       │
│  ┌──────────────────────────────────────────────┐  │
│  │ 用户浮窗 (AIChatAssistant.vue)               │  │
│  │ - 显示模型选择下拉菜单                         │  │
│  │ - 发送消息时传递 model 参数                   │  │
│  │ - 保存模型选择到 localStorage                 │  │
│  └──────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────┐  │
│  │ 管理员页面 (AIAssistant.vue)                 │  │
│  │ - 全局模型切换下拉菜单                         │  │
│  │ - 测试连接按钮                               │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
             ↓ API 调用
┌─────────────────────────────────────────────────────┐
│  后端 (Spring Boot)                                 │
│  ┌──────────────────────────────────────────────┐  │
│  │ UserAiController (/api/v1/user/ai)           │  │
│  │ - chat(ChatRequest)                          │  │
│  │   - message: 用户消息                        │  │
│  │   - model: 指定的模型 (可选)                 │  │
│  └──────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────┐  │
│  │ AdminAiController                            │  │
│  │ - POST /switch/{provider}  → 全局切换        │  │
│  │ - POST /query             → 使用当前模型    │  │
│  └──────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────┐  │
│  │ UserAiService                                │  │
│  │ - chat(userId, message, model?)              │  │
│  │   - 支持指定 model 参数                      │  │
│  │   - 调用 DynamicAiService                    │  │
│  └──────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────┐  │
│  │ DynamicAiService                             │  │
│  │ - chat(systemPrompt, userMessage)            │  │
│  │   使用当前激活的配置                          │  │
│  │ - chat(systemPrompt, userMessage, provider)  │  │
│  │   使用指定的 provider 配置                   │  │
│  └──────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────┐  │
│  │ AiConfigManager                              │  │
│  │ - getActiveConfig()    → 获取激活配置        │  │
│  │ - getConfigByProvider() → 获取指定配置        │  │
│  │ - switchTo(provider)   → 切换配置            │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
             ↓ 加载配置
┌─────────────────────────────────────────────────────┐
│  数据库 (MySQL)                                     │
│  ┌──────────────────────────────────────────────┐  │
│  │ ai_config 表                                 │  │
│  │ - id, provider, api_key, base_url            │  │
│  │ - model, is_active, temperature, max_tokens │  │
│  │                                              │  │
│  │ 数据示例：                                   │  │
│  │ - id:1, provider:chatgpt3.5, is_active:1    │  │
│  │ - id:2, provider:deepseek, is_active:1      │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## 📝 修改清单

### 后端修改

1. **DynamicAiService.java** ✅
   - 新增 `chat(String systemPrompt, String userMessage, String provider)` 重载方法
   - 支持指定 provider 调用 AI
   - 若 provider 为空，使用当前激活配置

2. **UserAiService.java** ✅
   - 修改 `chat(Long userId, String message)` 为 `chat(Long userId, String message, String model)`
   - 新增重载方法支持 model 参数
   - 调用 DynamicAiService 时传递 model

3. **UserAiController.java** ✅
   - 修改 ChatRequest 添加 `private String model;` 字段
   - 更新 `/api/v1/user/ai/chat` 端点，传递 model 到 service

### 前端修改

1. **frontend/src/api/user/ai.ts** ✅
   - 更新 ChatRequest 接口，添加 `model?: string` 字段

2. **frontend/src/components/user/AIChatAssistant.vue** ✅
   - 新增 `onModelChange()` 方法处理模型选择变更
   - 修改 `sendMessage()` 方法，传递选中的模型给后端
   - 修改 `onMounted()` 从 localStorage 恢复用户选择的模型
   - 添加模型映射逻辑

---

## 🧪 测试步骤

### 用户端测试

1. **打开用户浮窗**
   ```bash
   # 浏览器中访问应用
   # 点击右下角💬按钮打开浮窗
   ```

2. **选择模型并发送消息**
   ```
   - 选择"ChatGPT3.5"
   - 输入："帮我创建一个任务" → 发送
   - 观察响应 (应使用 ChatGPT3.5)
   
   - 选择"DeepSeek"
   - 输入："我今天很累" → 发送
   - 观察响应 (应使用 DeepSeek)
   ```

3. **验证持久化**
   ```
   - 选择 DeepSeek
   - 关闭浮窗
   - 刷新页面
   - 重新打开浮窗 → 应显示 DeepSeek 被选中
   ```

### 管理员端测试

1. **获取管理员权限并登录**
   ```bash
   # 使用管理员账号登录
   ```

2. **切换模型**
   ```
   - 注册/登录 → 管理后台 → AI 助手
   - 在"🔧 AI 配置"卡片选择"DeepSeek"
   - (可选) 点击"测试连接"验证可用
   - 点击切换或选择后应显示"已切换到 deepseek"
   ```

3. **验证查询使用新模型**
   ```
   - 在"💬 智能查询助手"输入：
     "最近的登录失败多少次" → 查询
   - 应返回使用 DeepSeek 模型的结果
   ```

---

## 🐛 常见问题

### Q: 用户浮窗下拉菜单里只显示两个选项，为什么不能看到其他模型？

A: 目前系统配置只有两个 AI 模型（ChatGPT3.5 和 DeepSeek）。如需添加更多模型：
1. 在 `ai_config` 表中插入新的配置行
2. 在 AIChatAssistant.vue 中添加对应的 `<el-option>`
3. 在模型映射中添加映射关系

### Q: 模型选择后为什么没有立即生效？

A: 
- **用户端**：模型选择在下一条消息时生效，因为当前消息已经在发送中
- **管理员端**：切换后的模型在点击"查询"后生效

### Q: 如何从代码中确认是否使用了正确的模型？

A: 检查浏览器开发者工具的 Network 标签：
```json
// 请求 payload
{
  "message": "test",
  "model": "chatgpt3.5"
}
```

或查看后端日志：
```
[用户AI] 检测到意图: ..., 用户消息: ..., 指定模型: chatgpt3.5
[AI] 使用指定提供商 - provider=chatgpt3.5, model=gpt-3.5-turbo
```

---

## 📊 模型配置信息

| 模型 | Provider | Model 值 | 说明 |
|------|----------|----------|------|
| ChatGPT3.5 | chatgpt3.5 | gpt-3.5-turbo | 快速、經濟的选择 |
| DeepSeek | deepseek | deepseek-chat | 功能完整的中文模型 |

**配置来源**: 使用 ChatAnywhere API (https://api.chatanywhere.tech/v1)

---

## 🔐 权限要求

- **用户端浮窗**: 所有已登录用户可以使用所有模型
- **管理员端**: 仅 `ROLE_ADMIN` 权限可以切换模型和发起查询

---

## 📌 配置初始化

如需重新初始化数据库配置，运行：

```bash
# 执行初始化脚本
mysql -u root -p database_name < AI_CONFIG_QUICK_INIT.sql
```

或手动插入：

```sql
-- 查看现有配置
SELECT * FROM ai_config;

-- 检查 is_active 状态（应该两个都是 1）
SELECT provider, model, is_active FROM ai_config;
```

---

## ✅ 完成状态

- ✅ 后端支持每条用户消息指定模型
- ✅ 用户端浮窗可以选择和切换模型
- ✅ 模型选择持久化到本地存储
- ✅ 管理员端全局模型切换
- ✅ 数据库配置管理完成
- ✅ 所有包括日志记录和错误处理
