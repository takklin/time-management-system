# Git 提交快速参考 - 一页纸版本

## 📋 10个提交块一览表

| 序号 | 类别 | 主题 | 关键文件数 | 预期工作量 |
|------|------|------|----------|----------|
| 1️⃣ | 后端 | 升级依赖库，添加AI/WebSocket/缓存支持 | 1 | 50 min |
| 2️⃣ | 后端 | 完善JWT认证和Spring Security配置 | 3 | 120 min |
| 3️⃣ | 后端 | 重构认证服务，支持密码加密迁移 | 3 | 90 min |
| 4️⃣ | 后端 | 添加用户角色和权限控制 | 3 | 100 min |
| 5️⃣ | 后端 | 新增管理员接口和操作日志 | 5 | 130 min |
| 6️⃣ | 后端 | 增强任务管理，支持高级查询和统计 | 4 | 120 min |
| 7️⃣ | 前端 | 完善前端核心基础设施（路由/状态/请求） | 8 | 135 min |
| 8️⃣ | 前端 | 实现管理后台页面和用户功能 | 11 | 155 min |
| 9️⃣ | 前端 | 补充API接口定义和类型检查 | 9 | 100 min |
| 🔟 | 系统 | 集成AI功能、系统监控和数据备份 | 23 | 125 min |

**总计**: 70+ 文件 | ~1100分钟 (≈18.3小时) 工作量

---

## 🔗 提交依赖关系

```
提交1️⃣ (依赖库)
    ↓
提交2️⃣ (JWT安全) → 提交3️⃣ (认证服务)
    ↓                  ↓
提交4️⃣ (用户角色) ← ← ↓
    ↓
提交5️⃣ (管理接口)
    ↓
提交6️⃣ (任务管理)
    ↓
提交7️⃣ (前端基础) → 提交8️⃣ (前端页面) → 提交9️⃣ (API定义)
                          ↓
提交🔟 (AI+系统)
```

---

## 💡 各提交的关键困难和解决方案

### 提交2️⃣: JWT认证
- 问题：Filter依赖为null → 改用构造器注入
- 问题：CORS预检被拦截 → Filter中优先放行OPTIONS

### 提交3️⃣: 密码加密
- 问题：历史数据混合明文和加密 → 自动检测并迁移
- 问题：登录返回数据结构 → 创建LoginVO对象统一格式

### 提交5️⃣: 操作日志
- 问题：日志记录污染业务代码 → 使用AOP自动拦截
- 问题：日志插入影响性能 → 异步处理日志

### 提交7️⃣: 前端基础
- 问题：401循环登出 → 在拦截器中检查是否已在登录页
- 问题：权限路由动态加载 → beforeEach中间件根据角色过滤

### 提交8️⃣: 前端页面
- 问题：表格数据过多卡顿 → 实现虚拟滚动
- 问题：头像上传进度 → 手动XMLHttpRequest处理

### 提交🔟: AI集成
- 问题：多AI提供商API不统一 → 设计适配器模式
- 问题：AI失败降级 → 缓存+本地模型备选
- 问题：备份性能 → 流式处理+异步

---

## 🚀 快速执行命令

```bash
# 提交1️⃣ - 依赖
git add backend/pom.xml
git commit -m "feat(backend/deps): 升级后端依赖库，新增AI、缓存、WebSocket支持"

# 提交2️⃣ - JWT安全
git add backend/src/main/java/com/timemanager/config/SecurityConfig.java \
        backend/src/main/java/com/timemanager/config/JwtAuthenticationFilter.java \
        backend/src/main/java/com/timemanager/util/JwtUtil.java
git commit -m "refactor(backend/security): 完善JWT认证机制和Spring Security配置

- 新增UserDetailsService支持username/email双路径查询
- 优化CORS配置支持多端口开发
- 完善JwtAuthenticationFilter异常处理
- JwtUtil实现JWT令牌的生成和解析"

# 提交3️⃣ - 认证服务
git add backend/src/main/java/com/timemanager/service/impl/AuthServiceImpl.java \
        backend/src/main/java/com/timemanager/vo/LoginVO.java \
        backend/src/main/java/com/timemanager/controller/AuthController.java
git commit -m "feat(backend/auth): 完善认证服务，实现密码加密和兼容性迁移

- 实现login()方法支持username/email双通道登录  
- 新增BCrypt密码验证，兼容历史明文密码自动迁移
- 创建LoginVO返回token和用户信息"

# 提交4️⃣ - 用户角色权限
git add backend/src/main/java/com/timemanager/entity/User.java \
        backend/src/main/java/com/timemanager/mapper/UserMapper.java \
        backend/src/main/java/com/timemanager/util/UserUtil.java
git commit -m "feat(backend/user-model): 用户实体添加角色字段，实现权限管理

- User实体添加role字段支持USER/ADMIN角色
- SecurityConfig集成权限检查返回GrantedAuthority
- UserUtil工具类提供权限检查辅助方法"

# 提交5️⃣ - 管理员接口
git add backend/src/main/java/com/timemanager/controller/AdminController.java \
        backend/src/main/java/com/timemanager/entity/OperationLog.java \
        backend/src/main/java/com/timemanager/mapper/OperationLogMapper.java \
        backend/src/main/java/com/timemanager/service/OperationLogService.java \
        backend/src/main/java/com/timemanager/service/impl/OperationLogServiceImpl.java
git commit -m "feat(backend/admin-api): 实现管理员相关接口和操作日志记录

- AdminController新增用户管理、日志查询、系统统计接口
- OperationLog实体和相关Mapper/Service自动记录操作
- 使用AOP自动拦截admin包下接口
- 异步处理日志插入，避免阻塞主流程"

# 提交6️⃣ - 任务管理增强
git add backend/src/main/java/com/timemanager/service/TaskService.java \
        backend/src/main/java/com/timemanager/service/impl/TaskServiceImpl.java \
        backend/src/main/java/com/timemanager/controller/TaskController.java \
        backend/src/main/java/com/timemanager/mapper/TaskMapper.java
git commit -m "feat(backend/task-enhance): 完善任务管理，新增统计和过滤功能

- TaskService新增按状态/优先级/截止日期的过滤查询
- 新增任务统计(已完成/未完成/超期)、分类统计
- TaskController支持分页、排序、多条件过滤
- TaskMapper添加复杂SQL聚合统计"

# 提交7️⃣ - 前端基础
git add frontend/package.json \
        frontend/src/main.ts \
        frontend/src/router/index.ts \
        frontend/src/router/adminMenu.ts \
        frontend/src/store/user.ts \
        frontend/src/store/task.ts \
        frontend/src/utils/request.ts \
        frontend/package-lock.json
git commit -m "feat(frontend/core): 重构前端认证、路由和状态管理

- 升级依赖：echarts、dayjs、element-plus、@element-plus/icons
- Pinia状态管理配置user/task stores
- 路由权限控制，基于角色动态加载
- axios拦截器自使用token并处理401/403"

# 提交8️⃣ - 前端页面
git add frontend/src/views/*.vue \
        frontend/src/views/admin/*.vue \
        frontend/src/components/App*.vue \
        frontend/src/components/admin/*.vue \
        frontend/src/layouts/AdminLayout.vue \
        frontend/vite.config.ts \
        frontend/src/App.vue
git commit -m "feat(frontend/views): 完善前端页面，实现用户管理和操作日志展示

- Login.vue：支持username/email登录
- Dashboard.vue：展示统计数据和快速操作
- UserManage.vue：用户列表、搜索、过滤、修改角色
- OperationLogs.vue：日志展示、风险标签、详情抽屉、导出
- AppHeader/Sidebar/AdminLayout组件完善"

# 提交9️⃣ - 前端API
git add frontend/src/api/*.ts \
        frontend/src/api/admin/*.ts \
        frontend/src/api/user/*.ts
git commit -m "feat(frontend/api): 补充前端API接口定义，增强TypeScript类型安全

- auth.ts：login、register、getCurrentUser、changePassword、uploadAvatar
- tasks.ts：getTasks、getTaskStats、createTask、updateTask、deleteTask
- admin/userManage.ts：getUsers、getUserDetail、updateUserRole
- admin/log.ts、admin/systemStat.ts、admin/ai.ts等完整定义
- 所有接口使用TypeScript interface确保类型安全"

# 提交🔟 - AI和系统功能
git add backend/src/main/java/com/timemanager/ai/ \
        backend/src/main/java/com/timemanager/entity/Ai*.java \
        backend/src/main/java/com/timemanager/entity/System*.java \
        backend/src/main/java/com/timemanager/entity/DbBackup.java \
        backend/src/main/java/com/timemanager/entity/UserBehaviorStats.java \
        backend/src/main/java/com/timemanager/controller/AdminAi*.java \
        backend/src/main/java/com/timemanager/controller/UserAiController.java \
        backend/src/main/java/com/timemanager/service/*Config*.java \
        backend/src/main/java/com/timemanager/service/*Backup*.java \
        backend/src/main/java/com/timemanager/service/StatisticsDataService.java \
        backend/src/main/java/com/timemanager/service/UserBehaviorStatsService.java \
        backend/src/main/java/com/timemanager/util/HttpUtil.java \
        backend/src/main/java/com/timemanager/config/AppConfig.java \
        backend/src/main/java/com/timemanager/config/MetricsFilter.java \
        backend/src/main/resources/application.properties
git commit -m "feat(backend/ai+system): 集成AI功能、系统监控和数据备份

## AI功能模块
- AiConfig/AiConversation/AiCallLog/AiAlert实体
- AdminAiController/UserAiController提供AI对话接口
- 支持OpenAI/DeepSeek等多提供商，使用适配器模式
- HttpUtil工具类调用第三方API，使用缓存和降级

## 系统监控和管理
- SystemConfig实体和Service用于系统配置管理
- RequestMetrics + MetricsFilter自动记录所有请求指标
- StatisticsDataService提供数据统计和分析
- DbBackup实体和Service支持数据备份和恢复

## 用户行为分析
- UserBehaviorStats记录用户操作习惯
- 使用异步处理避免影响主流程

## 配置更新
- application.properties添加AI缓存、日志扫描、异常检测等配置"

```

---

## 📝 提交规范

### 提交消息格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type类别
- `feat`: 新功能
- `refactor`: 代码重构
- `fix`: 问题修复
- `docs`: 文档更新
- `chore`: 构建或工具

### Scope作用域示例
- `backend/deps`, `backend/security`, `backend/auth`
- `frontend/core`, `frontend/views`, `frontend/api`

### Subject主题规范
- 使用祈使句："添加" 而非 "已添加"
- 不超过50字符
- 首字母大写
- 句末无句号

### Body正文规范
- 解释WHAT和WHY，而非HOW
- 每行不超过72字符
- 分点列举关键改动
- 说明遇到的困难和解决方案

### Footer页脚规范
- 关联issue：`Fixes #123`
- 破坏性变更：`BREAKING CHANGE: 说明`

---

## ✅ 完成检查清单

执行每个提交前：
- [ ] 对应文件修改完成
- [ ] 代码已编译/检查无误
- [ ] TypeScript/Lint检查通过
- [ ] 基本功能已人工测试
- [ ] 相对提交已清晰写好详细信息

提交后：
- [ ] 验证git log显示正确的提交信息
- [ ] 执行对应功能端到端测试
- [ ] 确认无遗漏相关文件

---

## 🔍 查看完整计划

详细的困难、解决方案和时间线请参考：

📄 **GIT_COMMIT_PLAN.md** - 完整版本（推荐仔细阅读）

---

**总工作量**: ~18小时  
**预计完成**: 2026-04-16  
**提交检查**: 所有提交需通过CI/CD流程

