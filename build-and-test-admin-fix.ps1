# Admin Login Fix - Build and Test Script (PowerShell)
# This script rebuilds the backend and provides quick API tests

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "管理员登录权限问题修复 - 编译和测试脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Maven
$mvnCheck = mvn --version 2>&1 | Select-Object -First 1
if ($null -eq $mvnCheck) {
    Write-Host "错误：Maven 未找到，请确保 Maven 已安装" -ForegroundColor Red
    Read-Host "按 Enter 键继续"
    exit 1
}

# 进入项目根目录
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $projectRoot

try {
    # Step 1: Clean
    Write-Host "[1/4] 清理旧的构建文件..." -ForegroundColor Green
    mvn clean -q 2>&1 | Out-Null

    # Step 2: Compile
    Write-Host "[2/4] 编译后端代码（包括权限修复）..." -ForegroundColor Green
    $compileResult = mvn compile -q 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "编译失败！" -ForegroundColor Red
        Write-Host $compileResult
        Read-Host "按 Enter 键继续"
        exit 1
    }

    # Step 3: Test
    Write-Host "[3/4] 运行单元测试..." -ForegroundColor Green
    mvn test -q 2>&1 | Out-Null

    # Step 4: Package
    Write-Host "[4/4] 生成可执行 JAR..." -ForegroundColor Green
    $packageResult = mvn package -DskipTests -q 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "打包失败！" -ForegroundColor Red
        Write-Host $packageResult
        Read-Host "按 Enter 键继续"
        exit 1
    }

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "构建完成！JAR 文件已生成" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""

    Write-Host "后端 JAR 文件位置: $projectRoot\target\time-manager-backend-0.0.1-SNAPSHOT.jar" -ForegroundColor Yellow
    Write-Host ""

    Write-Host "开启后端服务的几种方式：" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "方式 1（使用 Maven Spring Boot 插件）：" -ForegroundColor Cyan
    Write-Host "  mvn spring-boot:run"
    Write-Host ""
    Write-Host "方式 2（使用 PowerShell 运行 JAR）：" -ForegroundColor Cyan
    Write-Host "  & 'C:\Program Files\Java\jdkXX\bin\java.exe' -jar target\time-manager-backend-0.0.1-SNAPSHOT.jar"
    Write-Host ""
    Write-Host "方式 3（IDE 中运行主类）：" -ForegroundColor Cyan
    Write-Host "  运行 com.timemanager.TimeManagementApplication"
    Write-Host ""

    # 提示测试步骤
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "测试管理员登录" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "开启另一个 PowerShell 终端，执行以下命令：" -ForegroundColor Yellow
    Write-Host ""

    Write-Host "1. 管理员登录（获取 token）：" -ForegroundColor Cyan
    Write-Host ""
    $loginCmd = @'
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"username":"admin","password":"admin123"}'
$token = $response.data.token
Write-Host "Token: $token"
'@
    Write-Host $loginCmd
    Write-Host ""

    Write-Host "2. 将 token 保存到变量，然后测试管理员端点：" -ForegroundColor Cyan
    Write-Host ""
    $testCmd = @'
$token = "<从上面的 token 替换>"
$headers = @{"Authorization"="Bearer $token"}

# 测试 system/stat
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/admin/system/stat" `
  -Method GET -Headers $headers | ConvertTo-Json

# 测试 metrics/health
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/admin/metrics/health" `
  -Method GET -Headers $headers | ConvertTo-Json

# 测试 alerts/unhandled
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/admin/alerts/unhandled" `
  -Method GET -Headers $headers | ConvertTo-Json
'@
    Write-Host $testCmd
    Write-Host ""

    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "验证修复清单" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "[ ] 后端成功启动（查看日志中是否有错误）" -ForegroundColor Yellow
    Write-Host "[ ] 使用管理员账号登录成功，返回 code=200 和 token" -ForegroundColor Yellow
    Write-Host "[ ] 用 token 访问 /api/v1/admin/system/stat，返回 200（不是 403）" -ForegroundColor Yellow
    Write-Host "[ ] 用 token 访问 /api/v1/admin/metrics/health，返回 200" -ForegroundColor Yellow
    Write-Host "[ ] 用 token 访问 /api/v1/admin/alerts/unhandled，返回 200" -ForegroundColor Yellow
    Write-Host "[ ] 在后端日志中看到 [JWT认证] 相关信息" -ForegroundColor Yellow
    Write-Host ""

    Write-Host "如果仍然返回 403，请：" -ForegroundColor Red
    Write-Host "  1. 查看后端日志，搜索 'JWT认证' 或 'Unauthorized'" -ForegroundColor Red
    Write-Host "  2. 检查数据库中 user 表的 admin 用户的 role 字段是否为 'admin'" -ForegroundColor Red
    Write-Host "  3. 在 application.properties 中添加日志级别：" -ForegroundColor Red
    Write-Host "     logging.level.com.timemanager.config.JwtAuthenticationFilter=DEBUG" -ForegroundColor Red
    Write-Host "     logging.level.com.timemanager.config.SecurityConfig=DEBUG" -ForegroundColor Red
    Write-Host ""

} finally {
    Pop-Location
}

Read-Host "按 Enter 键关闭此窗口"
