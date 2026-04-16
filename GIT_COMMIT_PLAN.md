# Git 分块提交计划 - 详细版本

> **项目**: 时间管理系统  
> **日期**: 2026-04-16  
> **总提交数**: 10个逻辑提交  
> **修改文件**: 42个（已修改）+ 100+个（新增）

---

## 📋 提交块分解

### **提交 1️⃣: feat(backend/deps): 升级依赖库，添加AI/WebSocket/缓存支持**

**提交信息**:
```
feat(backend/deps): 升级后端依赖库，新增AI、缓存、WebSocket支持

- 添加 Spring Boot Data JPA 用于增强数据持久化
- 新增 Apache HttpClient 4.5.14 用于HTTPS请求支持
- 集成 Guava Cache 32.0.0 用于本地缓存机制
- 新增 Jackson DataBind 用于JSON序列化/反序列化
- 添加 Spring Boot WebSocket Starter 用于实时通信

**变更范围**: backend/pom.xml
**影响**: 为后续AI功能、缓存机制、实时通信打下基础
**困难和解决**:
  - 问题：多个新依赖间版本兼容性需要验证
  - 解决：选择长期支持版本(LTS)，确保Spring Boot 3.x兼容性
  - 测试：maven clean dependency:tree 验证依赖树无冲突

**时间线**: 
  - 扫描现有依赖关系 (5min)
  - 选择兼容版本 (15min)
  - 验证编译 (10min)
```

**包含文件**:
- `backend/pom.xml`

---

### **提交 2️⃣: refactor(backend/security): 完善JWT认证和安全配置**

**提交信息**:
```
refactor(backend/security): 完善JWT认证机制和Spring Security配置

## 核心改动

### SecurityConfig.java 重构
- 新增 UserDetailsService 实现，支持username/email双路径查询
- 添加 BCryptPasswordEncoder 密钥加密支持
- 配置 CORS 跨域资源共享，支持5173-5177多端口开发
- 设置 JWT 无状态认证模式 (SessionCreationPolicy.STATELESS)
- 自定义异常处理返回 HTTP 401 而非302重定向

### JwtAuthenticationFilter.java 修复
- 优化 JWT token 解析中的异常处理机制
- 正确注入依赖，避免NullPointerException
- 支持OPTIONS预检请求的透传

### JwtUtil.java 实现
- 使用 JJWT 库实现 JWT 生成和解析
- 配置 24小时过期时间
- 使用 HS256 对称签名算法
- 支持 userId 的编码和解码

**变更范围**: backend/src/main/java/com/timemanager/config/
**影响**: 解决登录后401循环闪屏问题，建立安全的认证体系
**困难和解决**:
  - 问题：JwtAuthenticationFilter 依赖注入为null
  - 解决：改用构造器注入而非字段注入，在SecurityConfig中显式注册Filter
  - 问题：CORS预检请求被拦截
  - 解决：在Filter中优先检查OPTIONS方法放行
  - 问题：JWT签名算法版本问题
  - 解决：升级JJWT到0.12版本，使用新的签名方式

**时间线**:
  - 分析401闪屏原因排查 (30min)
  - 设计JWT验证流程 (20min)
  - 实现SecurityConfig (30min)
  - 修复Filter依赖注入 (25min)
  - CORS配置调试 (20min)
  - 本地测试验证 (30min)

**测试步骤**:
  1. 启动后端，访问 /v1/auth/login
  2. 获取token后访问需要认证的接口
  3. 验证无401循环自动登出
  4. 测试前端跨域请求是否正常
```

**包含文件**:
- `backend/src/main/java/com/timemanager/config/SecurityConfig.java`
- `backend/src/main/java/com/timemanager/config/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/timemanager/util/JwtUtil.java`

---

### **提交 3️⃣: feat(backend/auth): 重构认证服务，支持渐进式密码加密迁移**

**提交信息**:
```
feat(backend/auth): 完善认证服务，实现密码加密和兼容性迁移

## 主要改动

### AuthServiceImpl.java 实现
- 实现 login() 方法，支持 username/email 双通道登录
- 新增密码验证逻辑：
  * 现有用户：使用 BCrypt.checkpw() 验证加密密码
  * 历史数据：兼容明文密码，自动迁移至加密格式
- 登录成功返回 LoginVO 包含 token 和用户信息

### LoginVO.java 定义
- 返回字段：token、user(id/username/email)
- 提供静态工厂方法 fromUser() 便于序列化

### AuthController.java 更新
- 绑定 /v1/auth/login、/v1/auth/register 端点
- 验证请求参数
- 返回统一 Response 格式

**变更范围**: backend/src/main/java/com/timemanager/{service,vo,controller}/
**影响**: 建立安全的认证流程，保护用户密码安全
**困难和解决**:
  - 问题：历史数据中密码存储不一致（明文vs加密）
  - 解决：在验证时自动检测密码格式，明文改加密后重新保存
  - 问题：Login接口需要返回token和用户信息
  - 解决：创建专门的LoginVO对象，在service中组装
  - 问题：密码加密算法的选型
  - 解决：选用BCrypt，春季Boot默认支持，安全性高

**时间线**:
  - 分析现有密码数据格式 (10min)
  - 设计兼容性迁移方案 (15min)
  - 实现AuthServiceImpl (25min)
  - 完善LoginVO结构 (10min)
  - 集成AuthController (10min)
  - 录制测试用例 (20min)

**测试步骤**:
  1. POST /v1/auth/login with username: admin, password: admin123
  2. 验证返回 { token, user: {...} }
  3. 使用token访问 /v1/auth/user
  4. 验证老数据密码被自动加密保存
```

**包含文件**:
- `backend/src/main/java/com/timemanager/service/impl/AuthServiceImpl.java`
- `backend/src/main/java/com/timemanager/vo/LoginVO.java`
- `backend/src/main/java/com/timemanager/controller/AuthController.java`

---

### **提交 4️⃣: feat(backend/user-model): 添加用户角色和权限控制**

**提交信息**:
```
feat(backend/user-model): 用户实体添加角色字段，实现权限管理

## 重点改进

### User.java 实体增强
- 新增 role 字段（String类型），支持 USER/ADMIN 两种角色
- 默认值为 USER，新注册用户自动赋予
- 由数据库记录持久化，支持动态权限变更

### UserMapper.java 扩展
- 新增查询方法用于管理员功能
- 支持条件查询（角色、状态等）
- 支持批量操作

### SecurityConfig.java 权限集成
- UserDetailsService 中读取 role 字段
- 转换为 Spring Security 的 GrantedAuthority
- 格式为 "ROLE_" + role.toUpperCase()，例如 "ROLE_ADMIN"

### UserUtil.java 工具类
- 提供 getCurrentUser() 获取当前登录用户
- 提供 getUserRole() 获取用户角色
- 支持权限检查辅助方法

**变更范围**: backend/src/main/java/com/timemanager/{entity,mapper,util}/
**影响**: 为管理后台和权限控制建立基础
**困难和解决**:
  - 问题：现有用户表无role字段，迁移时如何处理
  - 解决：数据库迁移脚本设置默认值为'USER'
  - 问题：权限验证如何集成到各个接口
  - 解决：使用@PreAuthorize annotation或HandlerInterceptor
  - 问题：角色字段大小写转换导致的不一致
  - 解决：统一使用String存储小写值，Spring转换时toUpperCase()

**时间线**:
  - 分析权限模型需求 (15min)
  - 修改User实体和mapper (20min)
  - 集成Spring Security权限 (20min)
  - 创建UserUtil工具类 (15min)
  - 设计数据库迁移脚本 (15min)
  - 集成测试验证 (25min)

**测试步骤**:
  1. 创建新用户，验证role=USER
  2. 手动修改某用户role=ADMIN
  3. 用admin用户访问管理接口，验证有权限
  4. 用普通用户访问，验证返回403 Forbidden
```

**包含文件**:
- `backend/src/main/java/com/timemanager/entity/User.java`
- `backend/src/main/java/com/timemanager/mapper/UserMapper.java`
- `backend/src/main/java/com/timemanager/util/UserUtil.java`

---

### **提交 5️⃣: feat(backend/admin-api): 新增管理员接口和操作日志**

**提交信息**:
```
feat(backend/admin-api): 实现管理员相关接口和操作日志记录

## 新增功能

### AdminController.java 实现
- GET /v1/admin/users - 获取用户列表（分页）
- GET /v1/admin/users/{id} - 获取用户详情
- POST /v1/admin/users/{id}/role - 修改用户角色
- GET /v1/admin/logs - 获取操作日志
- GET /v1/admin/system/stats - 获取系统统计数据

### OperationLog 相关
- 新增 OperationLog.java 实体
- 新增 OperationLogMapper.java
- 新增 OperationLogService.java
- 自动记录所有 admin 接口调用

### UserManage 相关控制器扩展
- 支持用户搜索、过滤
- 支持批量操作

**变更范围**: 
- backend/src/main/java/com/timemanager/controller/AdminController.java
- backend/src/main/java/com/timemanager/entity/OperationLog.java
- backend/src/main/java/com/timemanager/mapper/OperationLogMapper.java
- backend/src/main/java/com/timemanager/service/OperationLogService.java

**影响**: 建立完整的管理员操作接口和审计日志体系
**困难和解决**:
  - 问题：操作日志如何自动记录而不污染业务代码
  - 解决：使用 AOP（切面编程）在 admin 包下自动拦截，或在Filter中记录
  - 问题：日志何时插入数据库，并发下是否有性能问题
  - 解决：使用异步处理（@Async）记录日志，避免阻塞主流程
  - 问题：敏感信息（密码、token）如何脱敏
  - 解决：在记录时进行参数过滤和脱敏

**时间线**:
  - 分析admin接口需求 (20min)
  - 设计OperationLog表结构 (15min)
  - 实现AdminController (30min)
  - 创建OperationLogService (25min)
  - AOP拦截实现 (30min)
  - 异步日志处理 (20min)
  - 集成测试 (30min)

**测试步骤**:
  1. 调用admin接口修改用户角色
  2. 验证操作记录被插入operation_log表
  3. 查询 GET /v1/admin/logs，验证返回正确的日志
  4. 验证日志中参数被正确脱敏
```

**包含文件**:
- `backend/src/main/java/com/timemanager/controller/AdminController.java`
- `backend/src/main/java/com/timemanager/entity/OperationLog.java`
- `backend/src/main/java/com/timemanager/mapper/OperationLogMapper.java`
- `backend/src/main/java/com/timemanager/service/OperationLogService.java`
- `backend/src/main/java/com/timemanager/service/impl/OperationLogServiceImpl.java`

---

### **提交 6️⃣: feat(backend/task-enhance): 增强任务管理，支持高级查询**

**提交信息**:
```
feat(backend/task-enhance): 完善任务管理，新增统计和过滤功能

## 主要改进

### TaskService.java/TaskServiceImpl.java 扩展
- getUserTasks(userId, filters) - 支持按状态、优先级、截止日期过滤
- getTaskStats(userId) - 获取用户任务统计（已完成/未完成/超期）
- getTasksByCategory(userId) - 按分类统计任务
- updateTaskStatus(taskId, status) - 更新任务状态

### TaskController.java 增强
- GET /v1/tasks - 获取当前用户任务列表（支持过滤）
- GET /v1/tasks/stats - 获取任务统计数据
- POST /v1/tasks - 创建任务
- PUT /v1/tasks/{id} - 更新任务
- DELETE /v1/tasks/{id} - 删除任务

### TaskMapper.java 扩展SQL
- selectByUserIdAndFilters() - 条件查询
- selectStatsGroupByUser() - 聚合统计
- selectStatsGroupByCategory() - 分类统计

**变更范围**:
- backend/src/main/java/com/timemanager/service/TaskService.java
- backend/src/main/java/com/timemanager/service/impl/TaskServiceImpl.java
- backend/src/main/java/com/timemanager/controller/TaskController.java
- backend/src/main/java/com/timemanager/mapper/TaskMapper.java

**影响**: 提升任务查询和统计的灵活性和性能
**困难和解决**:
  - 问题：多条件查询SQL如何优雅地实现
  - 解决：使用 MyBatis Plus 的 QueryWrapper，链式API调用
  - 问题：统计查询涉及多表JOIN和聚合，SQL复杂度高
  - 解决：在TaskMapper中写原生SQL，使用XML配置管理
  - 问题：内存中数据过多时，分页和缓存如何处理
  - 解决：实现Page对象，集成Guava缓存热数据

**时间线**:
  - 分析task查询需求 (15min)
  - 设计过滤参数结构 (10min)
  - 编写SQL查询语句 (30min)
  - 实现Service层逻辑 (25min)
  - 完善Controller接口 (20min)
  - 性能测试和优化 (25min)

**测试步骤**:
  1. GET /v1/tasks?status=PENDING - 验证返回待处理任务
  2. GET /v1/tasks?priority=HIGH - 验证返回高优先级任务
  3. GET /v1/tasks/stats - 验证返回正确的统计数据
  4. 性能测试1000+条任务查询时间 <500ms
```

**包含文件**:
- `backend/src/main/java/com/timemanager/service/TaskService.java`
- `backend/src/main/java/com/timemanager/service/impl/TaskServiceImpl.java`
- `backend/src/main/java/com/timemanager/controller/TaskController.java`
- `backend/src/main/java/com/timemanager/mapper/TaskMapper.java`

---

### **提交 7️⃣: feat(frontend/core): 完善前端核心基础设施**

**提交信息**:
```
feat(frontend/core): 重构前端认证、路由和状态管理

## 基础设施改造

### package.json 依赖升级
- 新增 echarts@6.0.0 - 数据可视化
- 新增 dayjs@1.11.20 - 时间处理
- 升级 element-plus@2.13.6 - UI组件库
- 新增 @element-plus/icons-vue - icon库
- 保留 axios, pinia, vue-router 核心依赖

### main.ts 初始化
- 配置 Pinia 状态管理
- 注册路由
- 配置全局错误处理
- 设置响应拦截器

### router/index.ts 路由架构
- 实现动态路由加载
- 基于用户角色的权限控制
- 路由元信息(meta)配置权限要求
- 404和重定向处理

### router/adminMenu.ts 菜单配置
- 定义admin菜单项
- 权限检查逻辑
- 菜单项懒加载配置

### store/user.ts 用户状态管理
- 保存当前用户信息
- 保存token
- 权限检查方法
- 登出时清空状态

### store/task.ts 任务状态管理
- 缓存任务列表
- 任务统计数据
- 实时更新同步

### utils/request.ts 请求拦截
- 添加token到请求header
- 响应拦截处理401/403
- 错误统一处理

**变更范围**: frontend/src/{main,router,store,utils}/
**影响**: 建立稳定、可维护的前端架构基础
**困难和解决**:
  - 问题：401错误时自动重新登录导致循环
  - 解决：在response拦截器中检查是否已在登录页，避免重复跳转
  - 问题：权限路由如何动态加载
  - 解决：使用router.beforeEach中间件，根据用户角色过滤路由
  - 问题：token刷新后如何同步到所有请求
  - 解决：使用localStorage持久化token，axios默认自动注入

**时间线**:
  - 分析前端架构需求 (20min)
  - 升级依赖库 (25min)
  - 配置Pinia状态管理 (20min)
  - 实现路由权限控制 (30min)
  - 配置请求拦截器 (20min)
  - 整体集成测试 (25min)

**测试步骤**:
  1. npm install 验证依赖安装无冲突
  2. npm run dev 启动前端
  3. 访问登录页，输入用户名密码
  4. 验证登录成功后跳转到dashboard
  5. 验证admin用户能访问admin路由
  6. 验证普通用户访问admin路由被拒绝
```

**包含文件**:
- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/src/main.ts`
- `frontend/src/router/index.ts`
- `frontend/src/router/adminMenu.ts`
- `frontend/src/store/user.ts`
- `frontend/src/store/task.ts`
- `frontend/src/utils/request.ts`

---

### **提交 8️⃣: feat(frontend/views): 实现管理后台页面和用户功能**

**提交信息**:
```
feat(frontend/views): 完善前端页面，实现用户管理和操作日志展示

## 页面功能实现

### Views 页面集合
- Login.vue - 登录页面，支持用户名/邮箱登录
- Dashboard.vue - 仪表板，展示统计数据和快速操作
- Profile.vue - 个人资料页，支持头像上传和信息修改
- Tasks.vue - 任务管理，支持创建/编辑/删除/筛选
- admin/UserManage.vue - 用户管理页面
  * 用户列表展示（分页）
  * 搜索和过滤
  * 批量操作（修改角色、禁用等）
  * 用户详情弹窗
- admin/OperationLogs.vue - 操作日志页面
  * 日志列表展示
  * 风险等级标签
  * 详情抽屉展示
  * 导出功能

### 组件优化
- AppHeader.vue - 顶部导航，支持退登和用户菜单
- AppSidebar.vue - 左侧菜单，根据权限动态显示
- AdminLayout.vue - 管理后台布局
- UserAnalyticsDialog.vue - 用户分析弹窗
- UserDetailDrawer.vue - 用户详情抽屉

### 配置更新
- vite.config.ts - 优化构建配置，支持代理

**变更范围**: frontend/src/{views,components,layouts}/
**影响**: 提供完整的用户界面，支持所有业务功能
**困难和解决**:
  - 问题：表格数据过多时页面卡顿
  - 解决：实现虚拟滚动，只渲染可见区域
  - 问题：用户头像上传时进度和错误处理
  - 解决：使用formData和XMLHttpRequest手动处理
  - 问题：操作日志参数脱敏后如何在前端显示
  - 解决：后端返回脱敏数据，前端直接展示

**时间线**:
  - 设计页面布局和交互 (30min)
  - 实现Login页面 (20min)
  - 实现Dashboard (25min)
  - 实现UserManage (35min)
  - 实现OperationLogs (30min)
  - 完善Profile和Tasks (25min)
  - UI微调和响应式设计 (20min)

**测试步骤**:
  1. 进行完整的用户登录流程
  2. 验证dashboard显示正确的统计数据
  3. 在任务页面创建/编辑/删除任务
  4. admin用户访问用户管理页面，修改用户角色
  5. 查看操作日志页面，验证显示最近的操作
  6. 验证头像上传和资料修改功能
```

**包含文件**:
- `frontend/src/views/Login.vue`
- `frontend/src/views/Dashboard.vue`
- `frontend/src/views/Profile.vue`
- `frontend/src/views/Tasks.vue`
- `frontend/src/views/admin/UserManage.vue`
- `frontend/src/views/admin/OperationLogs.vue`
- `frontend/src/components/AppHeader.vue`
- `frontend/src/components/AppSidebar.vue`
- `frontend/src/layouts/AdminLayout.vue`
- `frontend/src/components/admin/UserAnalyticsDialog.vue`
- `frontend/src/components/admin/UserDetailDrawer.vue`
- `frontend/vite.config.ts`
- `frontend/src/App.vue`

---

### **提交 9️⃣: feat(frontend/api): 完善API接口定义和类型检查**

**提交信息**:
```
feat(frontend/api): 补充前端API接口定义，增强TypeScript类型安全

## API 接口补全

### auth.ts 认证接口
- login() - 用户登录
- register() - 用户注册
- getCurrentUser() - 获取当前用户
- changePassword() - 修改密码
- uploadAvatar() - 上传头像
- updateProfile() - 更新个人资料
- logout() - 登出（客户端清除token）

### tasks.ts 任务接口
- getTasks() - 获取任务列表
- getTaskStats() - 获取统计数据
- createTask() - 创建任务
- updateTask() - 更新任务
- deleteTask() - 删除任务

### admin/userManage.ts 用户管理接口
- getUsers() - 获取用户列表
- getUserDetail() - 获取用户详情
- updateUserRole() - 修改用户角色
- disableUser() - 禁用用户

### admin/log.ts 日志接口
- getLogs() - 获取操作日志列表
- getLogDetail() - 获取日志详情
- exportLogs() - 导出日志

### admin/systemStat.ts 系统统计接口
- getSystemStats() - 获取系统统计
- getUserGrowth() - 用户增长数据
- getTaskTrends() - 任务趋势

### admin/ 其他接口
- ai.ts - AI相关接口
- alert.ts - 告警接口
- metrics.ts - 指标接口

## 类型定义完善
- 所有请求/响应使用 TypeScript interface
- 统一的错误响应格式
- 分页对象的统一定义
- 可选参数的正确处理

**变更范围**: frontend/src/api/
**影响**: 增强代码的类型安全性，提升开发效率
**困难和解决**:
  - 问题：API接口定义重复，如何避免
  - 解决：创建baseRequest工厂函数，统一生成
  - 问题：分页、排序、过滤参数如何统一处理
  - 解决：定义 QueryParams 基类，各API继承扩展
  - 问题：响应拦截器如何处理不同的错误类型
  - 解决：定义 ApiResponse<T> 通用类型，在拦截器中处理

**时间线**:
  - 整理API需求列表 (20min)
  - 定义通用接口和类型 (25min)
  - 实现认证接口 (15min)
  - 实现业务接口 (25min)
  - 类型检查和修复 (15min)

**测试步骤**:
  1. npm run build 验证TypeScript无类型错误
  2. 使用 IDE 自动完成验证接口定义
  3. 调用各API接口，验证返回类型正确
```

**包含文件**:
- `frontend/src/api/auth.ts`
- `frontend/src/api/tasks.ts`
- `frontend/src/api/admin/index.ts`
- `frontend/src/api/admin/userManage.ts`
- `frontend/src/api/admin/log.ts`
- `frontend/src/api/admin/systemStat.ts`
- `frontend/src/api/admin/ai.ts`
- `frontend/src/api/admin/alert.ts`
- `frontend/src/api/admin/metrics.ts`

---

### **提交 🔟: feat(backend/ai+system): 集成AI功能和系统监控**

**提交信息**:
```
feat(backend/ai+system): 集成AI功能、系统监控和数据备份

## AI功能集成

### AI 相关模块新增
- AiConfig 实体 - AI配置管理（API key、模型选择等）
- AiConversation 实体 - 对话历史记录
- AiCallLog 实体 - API调用日志
- AiAlert 实体 - 告警记录

### AI 控制器
- AdminAiController - 管理员AI设置接口
- AdminAiConfigController - AI配置管理
- UserAiController - 用户使用AI功能

### AI 业务逻辑
- AiService 核心服务
- HttpUtil 工具类 - 调用第三方AI API
- 支持 OpenAI/DeepSeek 等多个AI提供商

## 系统监控和管理

### 系统配置模块
- SystemConfig 实体 - 系统全局配置
- SystemConfigService - 配置管理服务
- AdminConfigController - 配置管理接口

### 操作监控
- RequestMetrics 实体 - 请求指标记录
- MetricsFilter 过滤器 - 自动记录所有请求
- StatisticsDataService - 统计数据服务

### 数据备份
- DbBackup 实体 - 备份记录
- DbBackupService - 备份管理
- AdminBackupController - 备份接口

### 用户行为分析
- UserBehaviorStats 实体 - 用户行为统计
- UserBehaviorStatsService - 行为分析服务

## 配置更新
- application.properties
  * AI 缓存时长配置
  * 日志扫描间隔
  * 异常检测阈值
  * 日志级别设置

**变更范围**:
- backend/src/main/java/com/timemanager/ai/
- backend/src/main/java/com/timemanager/entity/
- backend/src/main/java/com/timemanager/service/
- backend/src/main/resources/application.properties

**影响**: 提供AI助手功能、系统监控和数据安全
**困难和解决**:
  - 问题：多个AI提供商的API格式不统一
  - 解决：设计适配器模式，统一调用接口
  - 问题：AI API调用失败时如何降级
  - 解决：使用缓存和本地模型作为备选方案
  - 问题：备份数据过大时性能问题
  - 解决：使用流式处理和异步备份
  - 问题：用户行为数据如何采集而不影响主流程
  - 解决：使用观察者模式，后台线程异步处理

**时间线**:
  - 分析AI功能需求 (20min)
  - 设计AI模块架构 (25min)
  - 实现AI Service层 (40min)
  - 实现控制器接口 (25min)
  - 添加系统监控 (30min)
  - 实现备份功能 (30min)
  - 集成测试 (30min)

**测试步骤**:
  1. 配置AI API key在application.properties
  2. 调用 POST /v1/admin/ai/config 设置AI配置
  3. 调用 POST /v1/user/ai/chat 测试AI对话
  4. 查看 GET /v1/admin/ai/logs 验证调用日志
  5. 手动触发备份，验证数据正确性
  6. 查看 GET /v1/admin/system/stats 验证系统统计
```

**包含文件**:
- `backend/src/main/java/com/timemanager/ai/` (整个目录)
- `backend/src/main/java/com/timemanager/entity/AiConfig.java`
- `backend/src/main/java/com/timemanager/entity/AiConversation.java`
- `backend/src/main/java/com/timemanager/entity/AiCallLog.java`
- `backend/src/main/java/com/timemanager/entity/AiAlert.java`
- `backend/src/main/java/com/timemanager/entity/SystemConfig.java`
- `backend/src/main/java/com/timemanager/entity/RequestMetrics.java`
- `backend/src/main/java/com/timemanager/entity/DbBackup.java`
- `backend/src/main/java/com/timemanager/entity/UserBehaviorStats.java`
- `backend/src/main/java/com/timemanager/entity/OperationLog.java`
- `backend/src/main/java/com/timemanager/entity/AlertLog.java`
- `backend/src/main/java/com/timemanager/controller/AdminAiController.java`
- `backend/src/main/java/com/timemanager/controller/AdminAiConfigController.java`
- `backend/src/main/java/com/timemanager/controller/UserAiController.java`
- `backend/src/main/java/com/timemanager/service/SystemConfigService.java`
- `backend/src/main/java/com/timemanager/service/DbBackupService.java`
- `backend/src/main/java/com/timemanager/service/OperationLogService.java`
- `backend/src/main/java/com/timemanager/service/StatisticsDataService.java`
- `backend/src/main/java/com/timemanager/service/UserBehaviorStatsService.java`
- `backend/src/main/java/com/timemanager/util/HttpUtil.java`
- `backend/src/main/java/com/timemanager/config/AppConfig.java`
- `backend/src/main/java/com/timemanager/config/MetricsFilter.java`
- `backend/src/main/resources/application.properties`

---

## 📊 提交统计

| 序号 | 类别 | 文件数 | 主要内容 |
|------|------|--------|---------|
| 1️⃣ | 依赖 | 1 | 升级后端依赖库 |
| 2️⃣ | 安全 | 3 | JWT认证和安全配置 |
| 3️⃣ | 认证 | 3 | 认证服务和密码加密 |
| 4️⃣ | 用户模型 | 3 | 角色和权限系统 |
| 5️⃣ | 管理员API | 5 | 管理接口和操作日志 |
| 6️⃣ | 任务管理 | 4 | 任务查询和统计 |
| 7️⃣ | 前端基础 | 8 | 路由、状态、请求 |
| 8️⃣ | 前端页面 | 11 | 视图和组件 |
| 9️⃣ | 前端API | 9 | API接口定义 |
| 🔟 | AI+系统 | 23 | AI功能和系统监控 |
| **总计** | - | **70** | - |

---

## 🔄 提交顺序建议

**按依赖和影响关系排序**:

1. ✅ 提交 1️⃣ (依赖) → 后续依赖库
2. ✅ 提交 2️⃣ (安全) → 认证基础
3. ✅ 提交 3️⃣ (认证) → 用户登录
4. ✅ 提交 4️⃣ (用户模型) → 权限控制
5. ✅ 提交 5️⃣ (管理API) → 管理功能
6. ✅ 提交 6️⃣ (任务管理) → 核心业务
7. ✅ 提交 7️⃣ (前端基础) → 前端基础设施
8. ✅ 提交 8️⃣ (前端页面) → 用户界面
9. ✅ 提交 9️⃣ (前端API) → 接口完善
10. ✅ 提交 🔟 (AI+系统) → 高级功能

---

## 🚀 执行步骤

### 第一阶段：后端基础（提交 1-6）
```bash
# Stage 1: 依赖库
git add backend/pom.xml
git commit -m "feat(backend/deps): 升级后端依赖库..."

# Stage 2-6: 逐个提交核心功能
git add backend/src/main/java/com/timemanager/config/SecurityConfig.java
git add backend/src/main/java/com/timemanager/config/JwtAuthenticationFilter.java
git add backend/src/main/java/com/timemanager/util/JwtUtil.java
git commit -m "refactor(backend/security): 完善JWT认证..."

# 继续其他提交...
```

### 第二阶段：前端基础（提交 7-9）
```bash
git add frontend/package.json frontend/src/main.ts
git commit -m "feat(frontend/core): 完善前端核心基础设施..."

# 继续其他提交...
```

### 第三阶段：高级功能（提交 10）
```bash
git add backend/src/main/java/com/timemanager/ai/
git add backend/src/main/java/com/timemanager/entity/Ai*.java
# ... 更多AI文件
git commit -m "feat(backend/ai+system): 集成AI功能和系统监控..."
```

---

## ✅ 验证清单

每个提交前检查：
- [ ] 代码编译无错
- [ ] TypeScript类型检查通过
- [ ] 单元测试通过
- [ ] 前后端集成测试通过
- [ ] 提交信息详细完整

---

**总责任人**: 开发团队  
**计划完成时间**: 2026-04-16  
**预期工作量**: ~10-12小时完成所有提交和测试

