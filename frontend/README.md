# 个人时间管理系统 - 前端项目

这是一个基于 Vue 3 + TypeScript + Element Plus 的个人时间管理系统前端应用，提供完整的任务管理、日程安排、时间记录和数据统计功能。

## 🚀 项目特点

- ✅ **Vue 3 Composition API** - 现代化的 Vue 框架
- ✅ **TypeScript** - 完整的类型支持
- ✅ **Element Plus** - 企业级 UI 组件库
- ✅ **Pinia** - 轻量级状态管理
- ✅ **Vite** - 极速开发体验
- ✅ **Responsive Design** - 响应式布局支持
- ✅ **ECharts** - 数据可视化
- ✅ **Axios** - HTTP 请求库

## 📁 项目结构

```
frontend/
├── src/
│   ├── api/                    # API 请求模块
│   │   ├── auth.ts             # 认证相关
│   │   ├── tasks.ts            # 任务相关
│   │   ├── schedules.ts        # 日程相关
│   │   └── time-records.ts     # 时间记录相关
│   ├── assets/                 # 静态资源
│   │   └── styles/
│   │       └── main.css        # 全局样式
│   ├── components/             # 全局组件
│   │   ├── AppHeader.vue       # 顶部导航栏
│   │   ├── AppSidebar.vue      # 侧边菜单栏
│   │   ├── ChartCard.vue       # 图表卡片
│   │   └── TaskItem.vue        # 任务项组件
│   ├── layout/                 # 布局组件
│   │   └── AppLayout.vue       # 主布局
│   ├── router/                 # 路由配置
│   │   └── index.ts
│   ├── store/                  # Pinia 状态管理
│   │   ├── user.ts             # 用户信息
│   │   ├── task.ts             # 任务数据
│   │   ├── schedule.ts         # 日程数据
│   │   └── time-record.ts      # 时间记录数据
│   ├── utils/                  # 工具函数
│   │   ├── request.ts          # Axios 配置
│   │   └── auth.ts             # 认证工具
│   ├── views/                  # 页面组件
│   │   ├── Dashboard.vue       # 仪表盘
│   │   ├── Tasks.vue           # 任务管理
│   │   ├── Schedules.vue       # 日程安排
│   │   ├── Todos.vue           # 待办事项
│   │   ├── TimeRecords.vue     # 时间记录
│   │   ├── Statistics.vue      # 数据统计
│   │   ├── Profile.vue         # 个人中心
│   │   ├── Login.vue           # 登录页
│   │   ├── Register.vue        # 注册页
│   │   └── NotFound.vue        # 404 页面
│   ├── App.vue                 # 根组件
│   ├── main.ts                 # 入口文件
│   └── env.d.ts                # TypeScript 类型定义
├── index.html                  # HTML 模板
├── vite.config.ts              # Vite 配置
├── tsconfig.json               # TypeScript 配置
├── package.json                # 依赖配置
└── dist/                        # 构建输出目录
```

## 🛠️ 开发环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0 或 yarn / pnpm

## 📦 安装依赖

```bash
# 进入项目目录
cd frontend

# 安装项目依赖
npm install
```

## 🚀 启动开发服务器

```bash
npm run dev
```

开发服务器将运行在 `http://localhost:5173`

## 📦 构建生产版本

```bash
npm run build
```

编译后的文件将输出到 `dist` 目录，可直接部署到生产环境。

## 🔍 预览生产构建

```bash
npm run preview
```

## 🔐 API 配置

默认 API 基地址为 `/api`，将代理到 `http://localhost:8080`。

修改 `vite.config.ts` 中的 proxy 配置可以改变代理地址：

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',  // 修改此处
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, '/api'),
  },
}
```

## 🎨 功能模块

### 1. 登录/注册页
- 用户身份验证
- 注册表单验证
- 密码强度提示

### 2. 仪表盘
- 今日待办任务概览
- 本周任务完成率
- 本周专注时长统计
- 快速操作入口

### 3. 任务管理
- 创建/编辑/删除任务
- 多条件筛选
- 优先级管理
- 截止时间提醒

### 4. 日程安排
- 日历视图
- 日程创建和编辑
- 日程关联任务
- 提醒功能

### 5. 待办事项
- 快速添加待办
- 按时间分组显示
- 优先级标记
- 快速完成/删除

### 6. 时间记录
- 实时计时器
- 手动记录
- 时间统计
- 任务关联

### 7. 数据统计
- 时间分配饼图
- 任务完成率趋势
- 每日专注时长
- 预估 vs 实际对比
- 任务排行榜
- 数据导出

### 8. 个人中心
- 用户信息管理
- 密码修改
- 分类管理
- 数据导入/导出

## 🚢 部署指南

### 本地部署

1. 构建项目
```bash
npm run build
```

2. 使用任意 Web 服务器提供 `dist` 目录中的内容

### Docker 部署

创建 `Dockerfile`：
```dockerfile
FROM node:18 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --cache ./npm_cache
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

构建并运行：
```bash
docker build -t time-management-frontend .
docker run -p 80:80 time-management-frontend
```

## 🔗 后端 API 约定

API 响应格式（成功）：
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

API 响应格式（失败）：
```json
{
  "code": 400,
  "message": "错误信息"
}
```

## 🎯 开发建议

1. **代码组织** - 按模块组织代码，每个模块包含相关的组件、API 和状态管理
2. **命名规范** - 使用清晰的变量和函数名称
3. **类型安全** - 充分利用 TypeScript 进行类型检查
4. **组件复用** - 提取可复用的组件到 `components` 目录
5. **Error Handling** - 统一的错误处理机制
6. **Performance** - 使用动态导入进行代码分割

## 📝 待改进项

- [ ] 优化首屏加载速度（ECharts 和 Element Plus 体积较大）
- [ ] 添加离线支持（PWA）
- [ ] 实现暗黑主题
- [ ] 添加国际化支持
- [ ] 完善移动端适配
- [ ] 添加单元测试和集成测试
- [ ] 数据导入/导出功能完善

## 🐛 常见问题

### Q: 修改了代码但浏览器没有自动更新？
A: 尝试清空浏览器缓存或重启开发服务器

### Q: API 请求返回 401？
A: Token 可能已过期，请重新登录

### Q: 构建后的包体积很大？
A: 这主要是因为 ECharts 库体积较大，可以考虑按需加载

## 📞 联系方式

如有问题或建议，请提交 Issue 或联系开发团队。

## 📄 许可证

MIT
