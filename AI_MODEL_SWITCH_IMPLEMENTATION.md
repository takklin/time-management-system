# 用户和管理员 AI 模型切换功能 - 改动总结

## ✅ 完成状态

用户和管理员都可以充分地切换 AI 模型。

## 🔧 主要改动

### 后端 (Java/Spring Boot)

#### 1. `DynamicAiService.java`
- ✅ 新增方法重载：`chat(String systemPrompt, String userMessage, String provider)`
- 支持指定 provider（模型）调用 AI
- 若 provider 为空则使用当前激活配置
- 保持原有 `chat(String, String)` 方法向后兼容

#### 2. `UserAiService.java`  
- ✅ 修改 `chat()` 方法支持可选的 `model` 参数
- `chat(Long userId, String message, String model)` - 新签名
- `chat(Long userId, String message)` - 重载方法调用新签名（model=null）
- 支持用户指定每条消息的模型

#### 3. `UserAiController.java`
- ✅ 更新 `ChatRequest` 内部类，添加 `String model` 字段
- ✅ 更新 `/api/v1/user/ai/chat` 端点，传递 model 到 service

### 前端 (Vue 3 + TypeScript)

#### 1. `frontend/src/api/user/ai.ts`
- ✅ 更新 `ChatRequest` 接口：添加 `model?: string` 字段

#### 2. `frontend/src/components/user/AIChatAssistant.vue` 
- ✅ 新增 `onModelChange()` 方法
  - 保存用户选择的模型到 `localStorage`
  - 显示"已切换到 xxx 模型"提示
- ✅ 修改 `sendMessage()` 方法
  - 在调用 API 时传递 `model` 参数
  - 包含前端→后端的映射：`'gpt-3.5' -> 'chatgpt3.5'`, `'deepseek' -> 'deepseek'`
- ✅ 修改 `onMounted()` 钩子
  - 从 `localStorage` 恢复用户上次选择的模型
  - 若无记录则默认为 'gpt-3.5'
- ✅ 删除了重复的 `onModelChange` 函数定义

---

## 🎯 功能说明

### 用户端（浮窗）

```
用户打开浮窗 
  ↓
看到模型下拉菜单（ChatGPT3.5 / DeepSeek）
  ↓  
选择模型
  ↓
输入消息并发送
  ↓
选中的模型处理该消息
  ↓
模型选择自动保存
  ↓
下次打开浮窗时自动恢复选择
```

### 管理员端

```
进入AI助手页面
  ↓
在顶部"🔧 AI 配置"卡片选择模型
  ↓
切换为全局配置
  ↓  
后续查询都使用新模型
```

---

## 📊 API 调用示例

### 用户端请求（带模型指定）

```json
POST /api/v1/user/ai/chat
Content-Type: application/json

{
  "message": "帮我创建一个任务",
  "model": "deepseek"
}
```

### 响应

```json
{
  "success": true,
  "code": 200,
  "data": "📝 好的！告诉我任务是什么呢？..."
}
```

---

## 🧪 快速测试

### 前置条件
- ✅ 后端已编译通过
- ✅ 数据库 `ai_config` 表已初始化（两个模型都 is_active=1）
- ✅ ChatAnywhere API key 配置正确

### 用户端测试

1. **打开应用**，找到右下角 💬 浮窗

2. **测试 ChatGPT3.5**
   ```
   - 选择 ChatGPT3.5
   - 输入："我今天很忙"
   - 发送 → 观察是否使用了正确的模型
   ```

3. **切换到 DeepSeek**
   ```
   - 选择 DeepSeek  
   - 看到 "已切换到 DeepSeek 模型" 提示
   - 输入："创建一个任务"
   - 发送 → 观察是否使用了 DeepSeek
   ```

4. **验证持久化**
   ```
   - 关闭浮窗（点击 ✕）
   - 刷新页面（F5）
   - 重新打开浮窗 → 应该选中了 DeepSeek
   ```

### 管理员端测试

1. **进入管理员页面** → AI 助手

2. **测试切换**
   ```
   - 在"🔧 AI 配置"选择 DeepSeek
   - 点击"测试连接"验证可用
   - 看到"✓ 连接成功"
   ```

3. **验证查询**
   ```
   - 在"💬 智能查询助手"输入问题
   - "最近一小时内的登录失败情况" → 查询
   - 应返回 DeepSeek 的处理结果
   ```

---

## 📋 验证清单

- [ ] 后端 Maven 编译无错误
- [ ] 数据库 ai_config 表两个模型都存在且 is_active=1
- [ ] 用户浮窗显示模型选择下拉菜单
- [ ] 用户可以选择不同模型
- [ ] 消息发送时传递了模型参数 (检查浏览器 Network)
- [ ] 模型选择保存到 localStorage
- [ ] 刷新后恢复之前选择的模型
- [ ] 管理员可以切换全局模型
- [ ] 管理员切换后新查询使用新模型
- [ ] 浏览器控制台没有报错

---

## 🔗 相关文件

| 文件 | 类型 | 改动 |
|------|------|------|
| `DynamicAiService.java` | 后端 | ✅ 新增 chat() 重载 |
| `UserAiService.java` | 后端 | ✅ 支持 model 参数 |
| `UserAiController.java` | 后端 | ✅ 更新 ChatRequest |
| `api/user/ai.ts` | 前端 | ✅ 更新 ChatRequest 接口 |
| `components/user/AIChatAssistant.vue` | 前端 | ✅ 实现模型切换逻辑 |

---

## 💡 提示

- 模型选择是**可选的**，如果前端不指定 `model`，后端会使用当前激活的配置
- 用户端是**每条消息可指定不同模型**，管理员端是**全局切换**
- localStorage 键名：`ai_user_selected_model`
- 模型映射（前端 → 后端）：
  - `'gpt-3.5'` → `'chatgpt3.5'`
  - `'deepseek'` → `'deepseek'`

---

## 🎉 完成！

所有功能已实现，用户和管理员都可以灵活地在 ChatGPT3.5 和 DeepSeek 之间切换 AI 模型！
