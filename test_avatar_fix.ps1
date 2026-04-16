# 测试头像 401 错误修复
$apiUrl = "http://localhost:8080"

Write-Host "=== 测试头像修复 ===" -ForegroundColor Green

# 1. 测试未认证请求头像 (应该返回 404 或 200，不再是 401)
Write-Host "`n1. 测试未认证请求头像端点:" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$apiUrl/api/v1/auth/avatar/test.jpg" -Method GET -UseBasicParsing
    Write-Host "状态码: $($response.StatusCode)" -ForegroundColor Cyan
    Write-Host "预期: 404 (文件不存在) 或 200，不应该是 401" -ForegroundColor Green
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    Write-Host "状态码: $statusCode" -ForegroundColor Cyan
    Write-Host "预期: 404 (文件不存在) 或 200，不应该是 401" -ForegroundColor Green
}

# 2. 测试登录
Write-Host "`n2. 测试用户登录:" -ForegroundColor Yellow
try {
    $loginBody = @{
        username = "guest"
        password = "123456"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-WebRequest -Uri "$apiUrl/api/v1/auth/login" `
        -Method POST `
        -Headers @{ "Content-Type" = "application/json" } `
        -Body $loginBody `
        -UseBasicParsing
    
    $loginData = $loginResponse.Content | ConvertFrom-Json
    Write-Host "登录响应代码: $($loginData.code)" -ForegroundColor Cyan
    
    if ($loginData.code -eq 200) {
        $token = $loginData.data.token
        $user = $loginData.data.user
        Write-Host "用户名: $($user.username)" -ForegroundColor Green
        Write-Host "头像路径: $($user.avatar)" -ForegroundColor Green
        
        # 3. 测试获取当前用户信息
        Write-Host "`n3. 测试获取当前用户信息 (带 Authorization 头):" -ForegroundColor Yellow
        $userResponse = Invoke-WebRequest -Uri "$apiUrl/api/v1/auth/me" `
            -Method GET `
            -Headers @{ "Authorization" = "Bearer $token" } `
            -UseBasicParsing
        
        $userData = $userResponse.Content | ConvertFrom-Json
        Write-Host "状态码: $($userResponse.StatusCode)" -ForegroundColor Cyan
        Write-Host "用户头像: $($userData.data.avatar)" -ForegroundColor Green
    }
} catch {
    Write-Host "登录错误: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green
