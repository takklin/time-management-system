@echo off
REM ========== 操作日志重新部署脚本 ==========
REM 用于快速重新编译、打包和测试操作日志功能

echo.
echo [1/4] 清理旧的编译文件...
cd backend
call mvn clean -q
if %ERRORLEVEL% neq 0 (
    echo ✗ 清理失败
    exit /b 1
)

echo [2/4] 编译 Java 代码...
call mvn compile -q
if %ERRORLEVEL% neq 0 (
    echo ✗ 编译失败
    exit /b 1
)

echo [3/4] 打包项目...
call mvn package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo ✗ 打包失败
    exit /b 1
)

echo [4/4] 启动后端服务...
echo.
echo ===== 后端已启动，监听 8080 端口 =====
echo 前端地址: http://localhost:5173
echo 后端地址: http://localhost:8080
echo.
echo [测试步骤]
echo 1. 打开浏览器进入: http://localhost:5173
echo 2. 登录账号: qiqi
echo 3. 输入密码并点击登录
echo 4. 登录成功后，在 MySQL 执行:
echo    SELECT * FROM operation_log WHERE operator='qiqi' ORDER BY created_at DESC LIMIT 1;
echo 5. 应该看到登录记录 ✓
echo.
echo 按任意键启动后端...
pause >nul

java -jar target\time-manager-backend-0.0.1-SNAPSHOT.jar --server.port=8080
