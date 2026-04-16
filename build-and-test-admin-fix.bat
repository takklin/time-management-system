@echo off
REM Admin Login Fix - Build and Test Script
REM This script rebuilds the backend and provides quick API tests

echo.
echo ========================================
echo 管理员登录权限问题修复 - 编译和测试脚本
echo ========================================
echo.

cd /d %~dp0

REM 检查 Maven 是否可用
mvn --version > nul 2>&1
if errorlevel 1 (
    echo 错误：Maven 未找到，请确保 Maven 已安装并在 PATH 中
    pause
    exit /b 1
)

echo [1/4] 清理旧的构建文件...
call mvn clean -q

echo [2/4] 编译后端代码（包括权限修复）...
call mvn compile -q
if errorlevel 1 (
    echo 编译失败！
    pause
    exit /b 1
)

echo [3/4] 运行单元测试...
call mvn test -q

echo [4/4] 生成可执行 JAR...
call mvn package -DskipTests -q
if errorlevel 1 (
    echo 打包失败！
    pause
    exit /b 1
)

echo.
echo ========================================
echo 构建完成！JAR 文件已生成
echo ========================================
echo.
echo 后端 JAR 文件位置: target\time-manager-backend-0.0.1-SNAPSHOT.jar
echo.

REM 提示启动方式
echo 开启后端服务的几种方式：
echo.
echo 方式 1（使用 Maven Spring Boot 插件）：
echo   mvn spring-boot:run
echo.
echo 方式 2（使用 Java 直接运行 JAR）：
echo   java -jar target\time-manager-backend-0.0.1-SNAPSHOT.jar
echo.
echo 方式 3（IDE 中运行主类）：
echo   运行 com.timemanager.TimeManagementApplication
echo.

echo ========================================
echo 测试管理员登录
echo ========================================
echo.
echo 开启另一个终端或使用 Postman/CURL，执行以下命令：
echo.
echo 1. 管理员登录（获取 token）：
echo.
echo   curl -X POST http://localhost:8080/api/v1/auth/login ^
echo     -H "Content-Type: application/json" ^
echo     -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
echo.
echo 2. 复制返回的 token，然后测试管理员端点（替换 <token>）：
echo.
echo   curl -X GET http://localhost:8080/api/v1/admin/system/stat ^
echo     -H "Authorization: Bearer <token>"
echo.
echo   curl -X GET http://localhost:8080/api/v1/admin/metrics/health ^
echo     -H "Authorization: Bearer <token>"
echo.
echo   curl -X GET http://localhost:8080/api/v1/admin/alerts/unhandled ^
echo     -H "Authorization: Bearer <token>"
echo.
echo ========================================
echo 验证修复清单
echo ========================================
echo.
echo [ ] 后端成功启动（查看日志中是否有错误）
echo [ ] 使用管理员账号登录成功，返回 code=200 和 token
echo [ ] 用 token 访问 /api/v1/admin/system/stat，返回 200（不是 403）
echo [ ] 用 token 访问 /api/v1/admin/metrics/health，返回 200
echo [ ] 访问 /api/v1/admin/alerts/unhandled，返回 200
echo [ ] 在后端日志中看到 [JWT认证] 相关信息
echo.
echo 如果仍然返回 403，请：
echo   1. 查看后端日志，搜索 "JWT认证" 或 "Unauthorized"
echo   2. 检查数据库中 user 表的 admin 用户的 role 字段是否为 "admin"
echo   3. 在 application.properties 中添加日志级别：
echo      logging.level.com.timemanager.config.JwtAuthenticationFilter=DEBUG
echo      logging.level.com.timemanager.config.SecurityConfig=DEBUG
echo.

pause
