# Git 提交执行时间线和检查清单

> **生成时间**: 2026-04-16  
> **总耗时**: 约18小时  
> **预计完成**: 2026-04-17

---

## 📅 详细时间线分解

### **第1天上午 - 后端基础阶段 (5小时)**

#### 🕐 9:00 - 9:50 | 提交1️⃣: 升级依赖库
```
提交前准备 (10min):
  ✓ 保存当前工作状态
  ✓ 查看pom.xml修改内容
  ✓ 验证Spring Boot版本兼容性

提交执行 (10min):
  git add backend/pom.xml
  git commit -m "feat(backend/deps): ..."

提交后验证 (30min):
  mvn clean dependency:tree | grep -E "(guava|httpclient|websocket|jpa)"
  ✓ 无版本冲突
  ✓ 编译成功

累计耗时: 50 min  ✅
```

#### 🕐 10:00 - 11:50 | 提交2️⃣: JWT认证和安全配置
```
前期分析 (30min):
  ✓ 分析401循环闪屏原因
  ✓ 查看Filter依赖注入问题
  ✓ 设计CORS和JWT流程

代码实现 (50min):
  ✓ SecurityConfig.java - UserDetailsService、CORS、Filter链
  ✓ JwtAuthenticationFilter.java - 异常处理、OPTIONS放行
  ✓ JwtUtil.java - JWT生成和解析

测试验证 (30min):
  mvn clean compile
  ✓ 无编译错误
  ✓ 本地启动测试登录流程

累计耗时: 110 min (总计: 160 min) ✅
```

#### 🕐 12:00 - 13:00 | **午休** 🍽️

#### 🕐 13:00 - 14:20 | 提交3️⃣: 认证服务
```
代码实现 (60min):
  ✓ AuthServiceImpl.java - login逻辑、密码兼容性
  ✓ LoginVO.java - 响应结构
  ✓ AuthController.java - 端点绑定

测试验证 (20min):
  POST /v1/auth/login 用户名登录
  POST /v1/auth/login 邮箱登录
  ✓ 返回正确的token和用户信息
  ✓ 老密码自动加密保存

累计耗时: 80 min (总计: 240 min) ✅
```

#### 🕐 14:30 - 15:50 | 提交4️⃣: 用户角色权限
```
代码修改 (60min):
  ✓ User.java - 添加role字段
  ✓ UserMapper.java - 扩展查询方法
  ✓ UserUtil.java - 权限工具类

集成和测试 (30min):
  ✓ SecurityConfig集成权限读取
  ✓ 权限检查测试

累计耗时: 90 min (总计: 330 min) ✅

**第1天上午总计: 330 分钟 ≈ 5.5 小时** ✅
```

---

### **第1天下午 - 后端高级功能 (5.5小时)**

#### 🕐 16:00 - 17:50 | 提交5️⃣: 管理员接口
```
架构设计 (30min):
  ✓ AOP切面设计
  ✓ 操作日志表结构
  ✓ 异步处理方案

代码实现 (80min):
  ✓ AdminController.java
  ✓ OperationLog.java/Mapper.java/Service.java
  ✓ AOP切面实现

测试验证 (20min):
  POST /v1/admin/users/1/role
  GET /v1/admin/logs
  ✓ 操作正确记录到数据库
  ✓ 异步处理无阻塞

累计耗时: 130 min (总计: 460 min) ✅
```

#### 🕐 18:00 - 19:50 | 提交6️⃣: 任务管理增强
```
SQL设计 (30min):
  ✓ 复杂查询语句编写
  ✓ 聚合统计优化

代码实现 (70min):
  ✓ TaskService.java - 查询和统计方法
  ✓ TaskMapper.java - SQL映射
  ✓ TaskController.java - API端点

性能测试 (20min):
  1000条任务查询性能 < 500ms
  ✓ 索引优化有效

累计耗时: 120 min (总计: 580 min) ✅

**第1天下午总计: 250 分钟 ≈ 4.2 小时**  
**第1天总计: ~9.5 小时** ✅
```

---

### **第2天上午 - 前端基础设施 (4.5小时)**

#### 🕐 9:00 - 10:50 | 提交7️⃣: 前端核心基础
```
依赖更新 (30min):
  npm install echarts dayjs (etc)
  ✓ package-lock.json更新
  ✓ 无依赖冲突

架构实现 (80min):
  ✓ main.ts - Pinia/Router初始化
  ✓ router/index.ts - 权限路由
  ✓ store/user.ts/task.ts - 状态管理
  ✓ request.ts - 拦截器配置

集成测试 (20min):
  npm run build
  ✓ TypeScript无类型错误
  ✓ 前后端跨域请求正常

累计耗时: 130 min (总计: 130 min) ✅
```

#### 🕐 10:50 - 11:50 | **咖啡休息** ☕

#### 🕐 12:00 - 13:50 | 提交8️⃣: 前端页面实现
```
页面设计 (30min):
  ✓ Login/Dashboard/Profile/Tasks布局
  ✓ UserManage/OperationLogs页面设计

代码实现 (100min):
  ✓ 6个View页面
  ✓ 5个Component组件
  ✓ Layout布局

UI调试和测试 (20min):
  npm run dev
  ✓ 页面正常显示
  ✓ 响应式设计验证

累计耗时: 150 min (总计: 280 min) ✅

**第2天上午总计: 280 分钟 ≈ 4.7 小时**
```

---

### **第2天下午 - 前端API和系统功能 (4.5小时)**

#### 🕐 14:00 - 15:20 | 提交9️⃣: 前端API定义
```
接口设计 (20min):
  ✓ 整理API需求
  ✓ 定义通用响应类型

代码实现 (70min):
  ✓ auth.ts
  ✓ tasks.ts
  ✓ admin/* 系列接口

类型检查 (10min):
  npm run build
  ✓ 类型检查通过

累计耗时: 100 min (总计: 100 min) ✅
```

#### 🕐 15:30 - 17:50 | 提交🔟: AI和系统功能
```
架构设计 (30min):
  ✓ AI适配器模式
  ✓ 监控系统设计
  ✓ 备份方案规划

后端实现 (100min):
  ✓ AI模块 (20+文件)
  ✓ 系统配置模块
  ✓ 备份和监控

集成测试 (25min):
  ✓ AI API调用测试
  ✓ 备份恢复测试
  ✓ 监控指标收集

累计耗时: 155 min (总计: 255 min) ✅

**第2天下午总计: 255 分钟 ≈ 4.3 小时**  
**第2天总计: ~9 小时** ✅
```

---

## 📊 总时间统计

```
第1天:  9.5 小时 (630分钟)
第2天:  9.3 小时 (555分钟)
─────────────────────
总计:  18.8 小时 (1185分钟)

分类时间分配:
├─ 后端实现:      8 小时  (42%)
├─ 前端实现:      7 小时  (37%)
├─ 测试验证:     2.5 小时 (13%)
└─ 分析设计:     1.3 小时 (8%)
```

---

## ✅ 每个提交的检查清单

### 提交1️⃣ 提前执行检查
- [ ] 查看pom.xml diff，确认无冲突
- [ ] 验证Spring Boot版本号一致性
- [ ] `mvn clean compile` 成功
- [ ] 提交信息准备完整

### 提交2️⃣ 前执行检查
- [ ] 所有Security相关文件已修改
- [ ] Filter依赖注入模式已更新
- [ ] CORS配置包含所有需要的端口
- [ ] 本地启动，开发工具验证401处理

### 提交3️⃣ 前执行检查
- [ ] BCryptPasswordEncoder已配置
- [ ] AuthService、LoginVO、AuthController已完成
- [ ] 测试验证用户名/邮箱登录都成功
- [ ] 老密码自动加密保存已验证

### 提交4️⃣ 前执行检查
- [ ] User实体添加role字段
- [ ] UserMapper查询方法已完成
- [ ] SecurityConfig读取role并转换为GrantedAuthority
- [ ] 权限检查方法已测试

### 提交5️⃣ 前执行检查
- [ ] AdminController所有方法已实现
- [ ] OperationLog表结构设计完成
- [ ] AOP切面已配置并测试
- [ ] 异步日志处理已验证

### 提交6️⃣ 前执行检查
- [ ] TaskService所有查询方法已实现
- [ ] SQL语句已优化并测试
- [ ] 统计功能验证准确
- [ ] 性能测试达到预期

### 提交7️⃣ 前执行检查
- [ ] 所有依赖已更新且兼容
- [ ] Pinia stores已配置
- [ ] 路由权限控制已实现
- [ ] axios拦截器已验证

### 提交8️⃣ 前执行检查
- [ ] 所有View页面已完成
- [ ] Component组件已集成
- [ ] UI样式和响应式设计已验证
- [ ] 页面功能端到端测试通过

### 提交9️⃣ 前执行检查
- [ ] 所有API接口已定义
- [ ] TypeScript类型检查通过
- [ ] 接口返回类型一致
- [ ] 接口文档已补充

### 提交🔟 前执行检查
- [ ] AI模块所有文件已创建
- [ ] 系统监控模块已配置
- [ ] 备份恢复功能已测试
- [ ] 配置文件已更新

---

## 🔍 关键检验步骤

### 每个提交后必做的验证

```bash
# 1. 验证提交信息格式
git log --oneline -1

# 2. 验证提交包含的文件
git show --stat

# 3. 后端编译验证
mvn clean compile

# 4. 前端编译验证
npm run build

# 5. 类型检查
npm run build:only -C frontend/

# 6. 目标功能测试
# 根据具体提交进行测试 (见下表)
```

---

## 🧪 功能测试验证表

| 提交 | 测试项 | 验证命令/步骤 | 预期结果 |
|------|--------|-------------|---------|
| 1️⃣ | 依赖检查 | `mvn dependency:tree` | 无版本冲突 |
| 2️⃣ | JWT生成 | `POST /auth/login` | 返回有效token |
| 2️⃣ | CORS预检 | 前端OPTIONS请求 | 200响应 |
| 3️⃣ | 密码验证 | 旧密码登录 | 成功&自动加密 |
| 4️⃣ | 权限检查 | admin访问/验证管理员 | 有权限 |
| 4️⃣ | 权限拒绝 | 普通用户访问 | 403 Forbidden |
| 5️⃣ | 日志记录 | 修改用户&查询日志 | 日志出现 |
| 6️⃣ | 任务查询 | `GET /tasks?status=PENDING` | 返回过滤结果 |
| 6️⃣ | 统计数据 | `GET /tasks/stats` | 返回准确统计 |
| 7️⃣ | 路由权限 | URL访问admin路由 | 权限检查工作 |
| 8️⃣ | 页面渲染 | `npm run dev` 打开浏览器 | 页面正常显示 |
| 9️⃣ | 类型检查 | `npm run build` | 无TypeScript错误 |
| 🔟 | AI调用 | `POST /admin/ai/chat` | 返回AI响应 |
| 🔟 | 备份功能 | `POST /admin/backup` | 生成备份文件 |

---

## ⚠️ 常见问题和快速解决

### Maven编译错误
```bash
# 清理缓存
mvn clean -U

# 检查依赖树
mvn dependency:tree | grep ERROR

# 解决方案：删除~/.m2/repository中的冲突依赖
rm -rf ~/.m2/repository/org/springframework
mvn clean compile
```

### 前端类型错误
```bash
# 检查所有类型错误
npx tsc --noEmit

# 修复错误后重新检查
npm run build
```

### 认证失败
```bash
# 检查JWT secret是否一致
grep "SECRET" backend/src/main/java/com/timemanager/util/JwtUtil.java

# 检查前端token存储
localStorage.getItem('token')

# 检查请求头
network请求headers中是否有Authorization：Bearer token
```

### CORS错误
```bash
# 检查前端端口号是否在白名单
grep "localhost:5\|127.0.0.1:5" backend/src/main/java/com/timemanager/config/SecurityConfig.java

# 检查请求Origin
network请求headers中Origin字段
```

---

## 📝 提交信息模板

### 标题行 (50字以内)
```
feat(backend/auth): 完善认证服务，支持密码加密迁移
```

### 正文 (详细说明)
```
## 核心改动
- 实现login()方法支持username/email双通道登录
- 新增BCrypt密码验证，兼容历史明文密码自动迁移
- 创建LoginVO对象统一返回格式

## 遇到的困难和解决方案
- 问题：历史数据密码存储不一致
- 解决：自动检测密码格式，明文改加密后重新保存
- 问题：认证信息需要返回token和用户信息
- 解决：创建专用LoginVO对象进行数据组装

## 测试验证
- POST /v1/auth/login with username
- POST /v1/auth/login with email
- 验证返回token和用户信息
- 验证老密码被自动加密保存

## 相关文件
- backend/src/main/java/com/timemanager/service/impl/AuthServiceImpl.java
- backend/src/main/java/com/timemanager/vo/LoginVO.java
- backend/src/main/java/com/timemanager/controller/AuthController.java
```

---

## 🚀 快速开始第一个提交

```bash
# 1. 确认当前状态
git status

# 2. Stage提交1️⃣的文件
git add backend/pom.xml

# 3. 查看变更
git diff --staged backend/pom.xml

# 4. 提交
git commit -m "feat(backend/deps): 升级后端依赖库，新增AI、缓存、WebSocket支持"

# 5. 验证
git log --oneline -1
mvn clean compile
```

---

**准备就绪？开始执行提交！** 🚀

*最后更新: 2026-04-16*

