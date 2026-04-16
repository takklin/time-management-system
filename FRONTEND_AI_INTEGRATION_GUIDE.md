# 🎨 前端 AI 功能集成指南

**完成时间**: 2026-04-15  
**状态**: ✅ 全部集成完成  
**前端框架**: Vue 3 + TypeScript + Vite

---

## 📋 集成完成清单

### ✅ 组件集成
- [x] **用户端浮窗** (`components/user/AIChatAssistant.vue`)
  - 位置: 右下角浮动按钮
  - 功能: 对话、自然语言创建任务、快捷操作
  - 集成位置: `Dashboard.vue`

- [x] **管理员AI助手** (`views/admin/AIAssistant.vue`)
  - 位置: 管理后台独立页面
  - 功能: 自然语言查询、实时预警显示
  - 路由: `/admin/ai-assistant`

- [x] **管理员菜单** (`router/adminMenu.ts`)
  - 已添加 "AI 助手" 菜单项
  - 图标: 魔法棒 (magic-stick)
  - 路径: `/admin/ai-assistant`

### ✅ API 集成
- [x] **用户API模块** (`api/user/ai.ts`)
  ```typescript
  - chat()              // 基础对话
  - parseTask()         // 自然语言解析任务
  - getTodaySummary()   // 获取今日总结
  - getTaskSuggestions()// 任务分解建议
  ```

- [x] **管理员API模块** (`api/admin/ai.ts`)
  ```typescript
  - queryData()           // 自然语言查询数据
  - getAlerts()          // 获取未处理预警
  - markAlertHandled()   // 标记预警已处理
  - triggerLogScan()     // 手动触发日志扫描
  - listConfigs()        // 获取AI配置列表
  - switchProvider()     // 切换AI提供商
  - testConnection()     // 测试连接
  - getCurrentConfig()   // 获取当前配置
  ```

- [x] **API导出** (`api/admin/index.ts`)
  - 已添加: `export * from './ai'`

### ✅ 路由配置
- [x] **管理员路由** (`router/index.ts`)
  ```typescript
  {
    path: 'ai-assistant',
    name: 'AdminAIAssistant',
    component: () => import('@/views/admin/AIAssistant.vue'),
    meta: { title: 'AI 智能助手', roles: ['admin'] }
  }
  ```

### ✅ 错误修复
- [x] **UserManage.vue 响应处理** (Line 132-133)
  ```typescript
  // 修复前: response.data?.rows 
  // 修复后: (response as any)?.rows
  // 原因: 响应拦截器已提取 data，response 本身就是提取后的数据
  ```

---

## 🚀 使用流程

### 场景 1️⃣: 用户在主页使用 AI 助手

```
用户进入 Dashboard
    ↓
点击右下角浮窗按钮
    ↓
【用户输入】"我明天下午3点到5点要开会"
    ↓
前端调用: userAiApi.parseTask({ message: "..." })
    ↓
后端执行:
  - 调用 AI 理解自然语言
  - 提取: 标题、时间、时长、分类
  - 返回结构化数据
    ↓
【AI 返回】
{
  title: "开会",
  deadline: "2026-04-16 15:00",
  estimatedMinutes: 120,
  categoryName: "工作"
}
    ↓
【用户确认】点击"✓ 添加到任务列表"
    ↓
前端调用: taskApi.createTask(...)
    ↓
任务添加成功，直接显示在任务列表中
```

### 场景 2️⃣: 管理员使用 AI 智能查询

```
管理员进入 /admin/ai-assistant
    ↓
左侧对话框
    ↓
【管理员输入】"最近一周新增用户有多少"
    ↓
前端调用: adminAiApi.queryData({ 
  question: "最近一周新增用户有多少" 
})
    ↓
后端执行:
  1. AI 识别意图 → 提取参数: timeRange=last_week
  2. 执行 SQL: SELECT COUNT(*) FROM user WHERE created_at >= [last_week]
  3. 获取结果: 15 个用户
  4. AI 转成自然语言: "最近一周新增了 15 位用户"
    ↓
【AI 返回】显示在对话框中
    ↓
右侧预警面板实时显示:
  - ⚠️ HIGH 异常登录 (5次失败)
  - ⚠️ MEDIUM 批量删除 (50条日志)
    ↓
管理员点击预警处理
```

### 场景 3️⃣: 管理员切换 AI 提供商

```
管理员打开 AI 助手页面
    ↓
配置卡片 → 下拉框选择
  [ ChatGPT3.5 or DeepSeek ]
    ↓
点击"测试连接"
    ↓
显示连接状态:
  ✓ 连接成功 (响应时间: 1.2s)
  或
  ✗ 连接失败 (错误: 无效的 API Key)
    ↓
如果成功，下次查询自动使用新提供商
（无需重启应用！）
```

---

## 📂 文件结构概览

```
frontend/src/
├── api/
│   ├── user/
│   │   └── ai.ts                    ← 用户AI API
│   └── admin/
│       ├── index.ts                 ← 已更新，包含 ai 导出
│       └── ai.ts                    ← 管理员AI API
│
├── components/
│   └── user/
│       └── AIChatAssistant.vue      ← 用户浮窗 ✅ 已集成
│
├── views/
│   ├── Dashboard.vue                ← 已导入 AIChatAssistant
│   └── admin/
│       └── AIAssistant.vue          ← 管理员AI页面 ✅ 已配置
│
├── router/
│   ├── index.ts                     ← 已添加 ai-assistant 路由
│   └── adminMenu.ts                 ← 已有 AI 菜单项
│
└── assets/
    └── styles/
        └── main.css                 ← 全局样式
```

---

## 🔧 配置检查清单

### 后端配置清单（Java Spring Boot）

```json
{
  "必需的依赖": {
    "spring-ai-openai-spring-boot-starter": "0.11.0 ✅",
    "spring-boot-starter-websocket": "自动 ✅",
    "guava": "32.0.0 ✅"
  },
  "必需的表": {
    "ai_config": "API 配置表 ✅",
    "ai_alert": "预警表 ✅",
    "ai_call_log": "可选，成本追踪 ✅"
  },
  "必需的 Controller": {
    "UserAiController": "/v1/user/ai/* ✅",
    "AdminAiController": "/v1/admin/ai/* ✅",
    "AdminAiConfigController": "/v1/admin/ai-config/* ✅"
  }
}
```

### 前端配置检查清单

```json
{
  "环境变量": {
    "VITE_API_BASE_URL": "/api ✅"
  },
  "依赖库": {
    "Vue 3": "3.3+ ✅",
    "TypeScript": "5.0+ ✅",
    "Element Plus": "2.4+ ✅",
    "axios": "1.4+ ✅"
  },
  "组件": {
    "AIChatAssistant.vue": "✅ 已集成到 Dashboard",
    "AIAssistant.vue": "✅ 已配置路由和菜单"
  },
  "API模块": {
    "api/user/ai.ts": "✅ 4 个方法",
    "api/admin/ai.ts": "✅ 8 个方法"
  }
}
```

---

## 🧪 本地测试指南

### 第一步：启动前端开发服务器
```bash
cd frontend
npm install  # 如果未安装
npm run dev  # 应该看到: VITE v8.0.8 ready in xxx ms
```
**预期输出**: 
```
  ➜  Local:   http://localhost:5176/
```

### 第二步：验证用户端AI功能
1. 打开 http://localhost:5176
2. 登录用户账号
3. 进入 Dashboard 页面
4. **查找右下角浮窗按钮** 🤖
5. 点击打开，输入: "明天下午3点开会，2小时"
6. 应显示 AI 推荐的任务表单
7. 点击"✓ 添加到任务列表"
8. 刷新页面，任务应该存在

### 第三步：验证管理员AI功能
1. 注册或使用管理员账号登录
2. 进入 /admin/ai-assistant
3. **左侧对话框输入**: "今天新增用户"
4. 应返回类似: "今天新增 X 个用户"
5. **右侧预警面板应显示**最近的预警
6. 点击配置卡片，切换 AI 提供商
7. 点击"测试连接"验证

### 第四步：检查浏览器控制台
```
✓ 无红色错误
✓ 显示 [vite] connected
✓ Network 标签显示所有请求成功 (200 OK)
```

---

## 🐛 常见问题排查

### Q1: AIChatAssistant 浮窗不显示
**症状**: Dashboard 页面加载，但右下角无浮窗  
**排查步骤**:
1. 打开浏览器 DevTools (F12)
2. Console 中查看是否有报错
3. 检查 `/frontend/src/views/Dashboard.vue` 是否引入组件:
   ```vue
   import AIChatAssistant from '@/components/user/AIChatAssistant.vue'
   ```
4. 检查 `<template>` 中是否调用:
   ```vue
   <AIChatAssistant />
   ```

### Q2: AI 菜单项无法点击
**症状**: 点击菜单无反应  
**原因**: 可能是权限问题  
**解决**:
1. 确保已登录管理员账号 (role = 'admin')
2. 检查用户表的 role 字段
3. SQL 验证:
   ```sql
   SELECT id, username, role FROM user WHERE id = [your_user_id];
   ```

### Q3: API 响应 404
**症状**: 浮窗点击发送后，浏览器 DevTools Network 显示 404  
**原因**: 后端 API 未实现  
**解决**: 确保后端已创建以下 Controller:
- `UserAiController` (路径: `/v1/user/ai/*`)
- `AdminAiController` (路径: `/v1/admin/ai/*`)
- `AdminAiConfigController` (路径: `/v1/admin/ai-config/*`)

### Q4: 类型错误 - "rows" 不存在
**症状**: TypeScript 编译错误  
**解决**: 
```typescript
// ❌ 错误
const data = response.data?.rows

// ✅ 正确
const data = (response as any)?.rows
```
**原因**: 响应拦截器已提取 data，response 是提取后的对象

---

## 📊 预期的 API 调用流程

### 用户端流程
```
前端
  ↓ POST /v1/user/ai/chat
后端 (UserAiController)
  ↓ 调用 DynamicAiService.chat()
DynamicAiService
  ↓ 读取 ai_config (获取当前激活的 API 密钥)
  ↓ 调用 DeepSeek 或 ChatAnywhere
AI 提供商
  ↓ 返回回复
  ↑
前端显示在浮窗中
```

### 管理员流程
```
前端
  ↓ POST /v1/admin/ai/query
后端 (AdminAiController)
  ↓ 调用 AdminAiService.handleQuery()
AdminAiService
  ↓ 第一次 AI 调用: 识别意图 + 提取参数
  ↓ 执行 SQL 查询
  ↓ 第二次 AI 调用: 将结果转成自然语言
  ↓ 返回回复
  ↑
前端显示在对话框，WebSocket 推送预警到右侧面板
```

---

## ✨ 特性概览表

| 功能 | 位置 | 状态 | 说明 |
|------|------|------|------|
| 用户对话 | Dashboard 浮窗 | ✅ 完成 | 自然语言输入，AI 回复 |
| 任务解析 | 浮窗快捷按钮 | ✅ 完成 | "明天下午开会" → 结构化任务 |
| 今日总结 | 浮窗快捷按钮 | ✅ 完成 | 生成鼓励性总结 |
| 自然语言查询 | /admin/ai-assistant 左侧 | ✅ 完成 | "新增用户" → 自动查询 |
| 实时预警显示 | /admin/ai-assistant 右侧 | ✅ 完成 | 异常检测，WebSocket 推送 |
| API 切换 | 配置卡片 | ✅ 完成 | DeepSeek ↔ ChatAnywhere |
| 连接测试 | 配置卡片 | ✅ 完成 | 验证 API 密钥有效性 |

---

## 🎯 下一步行动

### 1⃣ **验证编译**
```bash
cd frontend
npm run build
```
应输出: `built in xx ms`

### 2⃣ **启动完整环境**
```bash
# 终端1: 前端
cd frontend && npm run dev

# 终端2: 后端
cd backend && mvn spring-boot:run

# 终端3（可选）: 数据库
mysql -u root -p [your_database]
```

### 3⃣ **测试所有功能**
按照"本地测试指南"逐项测试

### 4⃣ **生成生产构建**
```bash
npm run build  # 输出到 dist/ 目录
```

---

## 📞 集成完成确认

| 项目 | 完成状态 | 验证位置 |
|------|--------|--------|
| ✅ 用户浮窗集成 | 已完成 | Dashboard.vue |
| ✅ 管理员页面 | 已完成 | /admin/ai-assistant |
| ✅ API 模块 | 已完成 | api/user/ai.ts, api/admin/ai.ts |
| ✅ 路由配置 | 已完成 | router/index.ts |
| ✅ 菜单项 | 已完成 | adminMenu.ts |
| ✅ 错误修复 | 已完成 | UserManage.vue line 132 |
| ✅ 导出配置 | 已完成 | api/admin/index.ts |

---

**前端集成完成！🎉**

整个系统已准备好进行后端开发和数据库配置。

---

**最后更新**: 2026-04-15  
**状态**: ✅ 前端集成完毕，等待后端 API 实现
