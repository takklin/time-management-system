# 管理员登录权限问题修复指南

## 问题概述
管理员用户登录后立即被重定向到 /login，且管理员端点返回 403 Forbidden。

## 根本原因
Spring Security 权限前缀不一致：
- **JwtAuthenticationFilter**: 设置 `authorities("ROLE_" + role)` → "ROLE_ADMIN"
- **SecurityConfig.userDetailsService()**: 原本设置 `authorities(role)` → "ADMIN" (缺少前缀)
- **SecurityConfig 权限检查**: 使用 `hasRole("ADMIN")` (正确，会自动加前缀)

结合前端路由守卫不能获取用户信息（因为 401），导致重定向。

## 实施的修复方案

### 修复 1：JwtAuthenticationFilter.java
添加详细的调试日志，方便排查认证问题：

```java
// 新增
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // 认证成功
    log.debug("[JWT认证] 解析成功：userId={}", userId);
    
    // 用户不存在（兼容处理）
    log.warn("[JWT认证] 用户ID在数据库中不存在：userId={}, 使用默认USER角色", userId);
    
    // 认证失败
    log.warn("[JWT认证] Token 认证失败，原因：{}", e.getMessage());
}
```

### 修复 2：SecurityConfig.java - userDetailsService()
确保权限前缀一致：

**之前（错误）:**
```java
.authorities(role)  // 导致 "ADMIN" 而不是 "ROLE_ADMIN"
```

**之后（正确）:**
```java
.authorities("ROLE_" + role)  // 设置 "ROLE_ADMIN"、"ROLE_USER"
```

### 修复 3：SecurityConfig.java - permitAll 规则清理
移除诊断端点的 permitAll 规则（应该需要 ADMIN 权限）：

**之前（错误）:**
```java
.antMatchers("/api/v1/admin/ai-config/test-connection/**").permitAll()
.antMatchers("/api/v1/admin/ai/query").permitAll()
```

**之后（正确）:**
```java
// 所有 /api/v1/admin/** 都需要 ADMIN 权限
.antMatchers("/api/v1/admin/**").hasRole("ADMIN")
```

## 修复后的完整权限规则流程

```
1. /api/v1/auth/**          → permitAll (登录、注册、获取用户信息等)
2. /api/v1/user/**          → authenticated (需要任何有效 token)
3. /api/v1/admin/**         → hasRole("ADMIN") (需要管理员权限)
4. 其他所有请求              → authenticated (需要任何有效 token)
```

## 认证流程图

```
客户端登录
    ↓
POST /api/v1/auth/login
    ↓
AuthController.login() 调用 AuthService.login()
    ↓
生成 JWT Token（包含 userId 和 role）
    ↓
返回 LoginVO { token, user }
    ↓
前端保存 token 到 localStorage
    ↓
前端设置 Authorization: Bearer <token>
    ↓
JwtAuthenticationFilter.doFilterInternal()
    ├─ 解析 token 获取 userId
    ├─ 从数据库查询 User
    ├─ 创建 UserDetails (authorities = "ROLE_ADMIN")
    ├─ 设置 SecurityContext authentication
    └─ 放行请求
    ↓
SecurityConfig.authorizeRequests()
    ├─ 检查权限规则
    └─ 如果是 /api/v1/admin/** 需要 hasRole("ADMIN")
           hasRole() 自动匹配 "ROLE_" 前缀
           "ROLE_ADMIN" 包含 "ADMIN" → 允许
    ↓
Controller 处理请求，返回 200
```

## 测试步骤

### 1. 重启后端应用
```bash
# 确保使用最新的编译代码
mvn clean package
# 或使用 Spring Boot 插件
mvn spring-boot:run
```

### 2. 检查后端日志
重启后在日志中搜索：
```
[JWT认证] 解析成功
[JWT认证] 用户认证成功
```

### 3. 测试管理员登录

**登录请求:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**验证返回 token:**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@test.com",
      "role": "admin"
    }
  }
}
```

### 4. 使用 token 访问管理员端点

**请求:**
```bash
curl -X GET http://localhost:8080/api/v1/admin/system/stat \
  -H "Authorization: Bearer <token>"
```

**预期结果:** 200 OK（而不是 403 Forbidden）

### 5. 前端测试步骤

1. 清除浏览器 localStorage 中的旧 token
   ```javascript
   localStorage.clear()
   ```

2. 用管理员账号登录（用户名：admin，密码：根据实际）

3. 验证以下行为：
   - ✅ 登录后不再被重定向到 /login
   - ✅ 能够进入 /admin/dashboard
   - ✅ 能够访问所有管理员功能
   - ✅ 查看浏览器网络请求，管理员端点返回 200

## 常见问题排查

### 问题：登录后仍被重定向到 /login
**原因分析:**
1. Token 生成失败 → 检查 JwtUtil.generateToken() 实现
2. Token 格式错误 → 检查 JWT token 是否是三段式（header.payload.signature）
3. 前端未保存 token → 检查浏览器 DevTools → Application → localStorage → token
4. 后端认证过滤器失败 → 查看后端日志中 JwtAuthenticationFilter 的日志

**调试方法:**
```javascript
// 前端控制台
console.log('[DEBUG] Token:', localStorage.getItem('token'))
console.log('[DEBUG] User:', JSON.parse(localStorage.getItem('user') || '{}'))
```

### 问题：管理员端点返回 403 Forbidden
**原因分析:**
1. 权限前缀不匹配 → 检查权限是否为 "ROLE_ADMIN" 而不是 "ADMIN"
2. SecurityConfig 规则顺序错误 → 特定规则应在通用规则之前
3. 用户角色不是 admin → 检查数据库 user 表中的 role 字段

**调试方法:**
```bash
# 查看用户信息
curl -X GET http://localhost:8080/api/v1/auth/user \
  -H "Authorization: Bearer <token>"
```

### 问题：后端日志看不到 JWT 认证信息
**原因:**
- 日志级别设置不对，改为 DEBUG 级别
- 在 application.properties 中添加：
  ```properties
  logging.level.com.timemanager.config.JwtAuthenticationFilter=DEBUG
  ```

## 配置验证清单

- [ ] JwtAuthenticationFilter 中权限使用 "ROLE_" 前缀
- [ ] SecurityConfig.userDetailsService() 中权限使用 "ROLE_" 前缀
- [ ] SecurityConfig.authorizeRequests() 中管理员端点使用 hasRole("ADMIN")
- [ ] 没有 permitAll 规则应用于 /api/v1/admin/** 端点
- [ ] 后端已编译并重启
- [ ] 浏览器缓存已清除
- [ ] 数据库中管理员用户的 role 字段为 "admin"（小写）

## 下一步

1. **执行 AI 配置切换**
   - 运行 `ai_config_setup.sql` 将 AI 配置从 DeepSeek 切换到 ChatGPT3.5

2. **实现查询端数据隔离**
   - 修改 AiCallLogMapper 和 AiConversationMapper
   - 所有查询添加 WHERE user_id = #{userId} 条件

3. **完整的端到端测试**
   - 管理员能否正常使用 AI 聊天
   - 用户能否正常使用 AI 聊天
   - 验证用户之间没有数据交叉

## 相关文件

- `backend/src/main/java/com/timemanager/config/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/timemanager/config/SecurityConfig.java`
- `frontend/src/store/user.ts`
- `frontend/src/utils/request.ts`
- `frontend/src/router/index.ts`
