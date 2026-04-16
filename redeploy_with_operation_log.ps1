#!/usr/bin/env pwsh

# ========== 操作日志重新部署脚本 (PowerShell 版本) ==========

Write-Host ""
Write-Host "========== 操作日志修复部署流程 ==========" -ForegroundColor Cyan
Write-Host ""

# 第 1 步：清理
Write-Host "[1/4] 清理旧的编译文件..." -ForegroundColor Yellow
Set-Location backend
& mvn clean -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 清理失败" -ForegroundColor Red
    exit 1
}
Write-Host "✓ 清理完成" -ForegroundColor Green

# 第 2 步：编译
Write-Host "[2/4] 编译 Java 代码..." -ForegroundColor Yellow
& mvn compile -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 编译失败" -ForegroundColor Red
    exit 1
}
Write-Host "✓ 编译完成" -ForegroundColor Green

# 第 3 步：打包
Write-Host "[3/4] 打包项目..." -ForegroundColor Yellow
& mvn package -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 打包失败" -ForegroundColor Red
    exit 1
}
Write-Host "✓ 打包完成" -ForegroundColor Green

# 第 4 步：启动
Write-Host ""
Write-Host "===== 后端已准备就绪！=========" -ForegroundColor Green
Write-Host ""
Write-Host "【测试步骤】" -ForegroundColor Cyan
Write-Host "1. 打开浏览器: http://localhost:5173" -ForegroundColor White
Write-Host "2. 登录账号: qiqi" -ForegroundColor White
Write-Host "3. 输入密码并点击登录" -ForegroundColor White
Write-Host "4. 成功登录后，执行 SQL 验证:" -ForegroundColor White
Write-Host ""
Write-Host "  SELECT * FROM operation_log" -ForegroundColor Gray
Write-Host "  WHERE operator='qiqi' " -ForegroundColor Gray
Write-Host "  ORDER BY created_at DESC LIMIT 1;" -ForegroundColor Gray
Write-Host ""
Write-Host "5. 应该看到登录记录（action='LOGIN', result='SUCCESS'）✓" -ForegroundColor White
Write-Host ""
Write-Host "按 Enter 键启动后端服务..." -ForegroundColor Yellow
Read-Host

Write-Host ""
Write-Host "启动后端服务..." -ForegroundColor Cyan
java -jar target/time-manager-backend-0.0.1-SNAPSHOT.jar --server.port=8080
