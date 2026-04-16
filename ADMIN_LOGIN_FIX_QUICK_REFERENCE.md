# 管理员权限 403 问题 - 快速参考卡

## 问题一句话概括
Spring Security 权限前缀不一致导致管理员账号无法通过权限检查。

## 三个关键修复

| 文件 | 修复内容 | 优先级 |
|------|---------|--------|
| `JwtAuthenticationFilter.java` | 添加 SLF4J 日志用于调试 | 低 |
| `SecurityConfig.java` - userDetailsService() | 添加 "ROLE_" 前缀 | 🔴 高 |
| `SecurityConfig.java` - authorizeRequests() | 移除不必要 permitAll，使用 hasRole() | 🔴 高 |

## 验证修复的操作清单

```
[ ] Step 1: 重新编译后端
    mvn clean compile package -DskipTests

[ ] Step 2: 重启 Spring Boot 服务
    java -jar target/time-manager-backend-0.0.1-SNAPSHOT.jar

[ ] Step 3: 测试管理员登录
    POST /api/v1/auth/login
    Body: {"username":"admin","password":"admin123"}
    Expected: code=200 + token

[ ] Step 4: 用 token 访问管理员端点
    GET /api/v1/admin/system/stat
    Header: Authorization: Bearer <token>
    Expected: 200 OK（不是 403）

[ ] Step 5: 检查后端日志
    搜索：[JWT认证] 解析成功
    搜索：[JWT认证] 用户认证成功

[ ] Step 6: 前端登录测试
    - 清除浏览器 localStorage
    - 用管理员账号登录
    - 验证不被重定向到 /login
    - 能进入 /admin/dashboard
```

## 最常见的错误症状和解决方案

### 症状 1：登录后立即被重定向到 /login
**原因：** 前端无法获取用户信息（可能 /api/v1/auth/user 返回 401）
**解决：** 
```bash
# 检查后端日志
grep "[JWT认证]" your-log-file.log
# 如果看不到认证信息，说明 token 解析失败
```

### 症状 2：管理员端点返回 403 Forbidden
**原因：** 权限检查失败，不在权限列表中
**解决方案 a（权限前缀）：**
```java
// ✓ 正确
authorities("ROLE_" + role)
hasRole("ADMIN")

// ✗ 错误
authorities(role)
hasAuthority("ADMIN")  // 不会自动添加前缀
```

**解决方案 b（检查数据库）：**
```sql
-- 确保 admin 用户的 role 字段是 'admin'（小写）
SELECT id, username, role FROM user WHERE username = 'admin';
```

### 症状 3：后端日志没有 [JWT认证] 信息
**原因：** 日志级别设置不对
**解决：** 在 `application.properties` 添加
```properties
logging.level.com.timemanager.config.JwtAuthenticationFilter=DEBUG
logging.level.com.timemanager.config.SecurityConfig=DEBUG
```

## 三行代码总结修复

```java
// 修复 1：SecurityConfig.userDetailsService() 第一个地方
authorities("ROLE_" + role)  // 从 authorities(role) 改为加 ROLE_

// 修复 2：SecurityConfig.authorizeRequests() 
.antMatchers("/api/v1/admin/**").hasRole("ADMIN")  // 从 hasAuthority 改为 hasRole

// 修复 3：SecurityConfig.authorizeRequests()
// 删除这两行：
// .antMatchers("/api/v1/admin/ai-config/test-connection/**").permitAll()
// .antMatchers("/api/v1/admin/ai/query").permitAll()
```

## 权限匹配速查表

| Spring Security | 对应权限值 | 说明 |
|----------------|-----------|------|
| `hasRole("ADMIN")` | "ROLE_ADMIN" | 自动添加 ROLE_ 前缀 |
| `hasAuthority("ROLE_ADMIN")` | "ROLE_ADMIN" | 需要包含完整前缀 |
| `hasAuthority("ADMIN")` | ❌ 不匹配 | 严格字符串匹配 |
| `@PreAuthorize("hasRole('ADMIN')")` | "ROLE_ADMIN" | 注解方式，自动加前缀 |
| `@Secured("ROLE_ADMIN")` | "ROLE_ADMIN" | 旧式注解，需含前缀 |

## 命令速查

### 快速编译测试
```bash
# Windows Batch
build-and-test-admin-fix.bat

# PowerShell
.\build-and-test-admin-fix.ps1
```

### 快速测试 API
```bash
# 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 用返回的 token 测试管理员端点
curl -X GET http://localhost:8080/api/v1/admin/system/stat \
  -H "Authorization: Bearer <YOUR_TOKEN_HERE>"
```

## 相关文件位置

```
项目根目录/
├── backend/
│   └── src/main/java/com/timemanager/config/
│       ├── JwtAuthenticationFilter.java  ← 已修改，添加日志
│       └── SecurityConfig.java           ← 已修改，权限前缀 & permitAll
│
├── ADMIN_LOGIN_FIX_GUIDE.md               ← 详细修复指南
├── ADMIN_LOGIN_FIX_SUMMARY.md             ← 完整问题分析
├── build-and-test-admin-fix.bat           ← Windows 快速测试脚本
└── build-and-test-admin-fix.ps1           ← PowerShell 快速测试脚本
```

## 预期结果

### 修复前 ❌
```
POST /api/v1/auth/login → 200 OK ✓
GET /api/v1/admin/system/stat + token → 403 Forbidden ❌
```

### 修复后 ✓
```
POST /api/v1/auth/login → 200 OK ✓
GET /api/v1/admin/system/stat + token → 200 OK ✓
前端登录后 → 进入 /admin/dashboard ✓（不是重定向到 /login）
```

## 需要的帮助

- [ ] 不清楚如何重启后端？ → 查看 `ADMIN_LOGIN_FIX_GUIDE.md` 的"重启后端应用"部分
- [ ] 不知道如何测试 API？ → 运行 `build-and-test-admin-fix.bat` 或 `.ps1`
- [ ] 仍然返回 403？ → 查看"最常见错误症状"部分
- [ ] 需要完整解释？ → 阅读 `ADMIN_LOGIN_FIX_SUMMARY.md`

---

**关键提示：** 这个问题的 99% 原因就是权限前缀 "ROLE_" 不一致。修复后重启，问题一般就解决了。
