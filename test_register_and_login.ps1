# 注册测试用户并测试
$apiUrl = "http://localhost:8080"

Write-Host "=== 测试用户注册和登录 ===" -ForegroundColor Green

# 1. 注册新用户
Write-Host "`n1. 注册新用户:" -ForegroundColor Yellow
try {
    $registerBody = @{
        username = "testuser"
        email = "test@example.com"
        password = "test123456"
    } | ConvertTo-Json
    
    $registerResponse = Invoke-WebRequest -Uri "$apiUrl/api/v1/auth/register" `
        -Method POST `
        -Headers @{ "Content-Type" = "application/json" } `
        -Body $registerBody `
        -UseBasicParsing
    
    $registerData = $registerResponse.Content | ConvertFrom-Json
    Write-Host "注册响应: $($registerData.code)" -ForegroundColor Cyan
    Write-Host "消息: $($registerData.message)" -ForegroundColor Cyan
    
    if ($registerData.code -eq 200) {
        Write-Host "注册成功！" -ForegroundColor Green
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    $errBody = $_.Exception.Response.GetResponseStream()
    $reader = [System.IO.StreamReader]::new($errBody)
    $errContent = $reader.ReadToEnd()
    Write-Host "注册错误 (状态码: $statusCode): $errContent" -ForegroundColor Yellow
}

# 2. 登录
Write-Host "`n2. 尝试登录:" -ForegroundColor Yellow
try {
    $loginBody = @{
        username = "testuser"
        password = "test123456"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-WebRequest -Uri "$apiUrl/api/v1/auth/login" `
        -Method POST `
        -Headers @{ "Content-Type" = "application/json" } `
        -Body $loginBody `
        -UseBasicParsing
    
    $loginData = $loginResponse.Content | ConvertFrom-Json
    Write-Host "登录响应代码: $($loginData.code)" -ForegroundColor Cyan
    Write-Host "消息: $($loginData.message)" -ForegroundColor Cyan
    
    if ($loginData.code -eq 200) {
        $token = $loginData.data.token
        $user = $loginData.data.user
        Write-Host "用户名: $($user.username)" -ForegroundColor Green
        Write-Host "头像路径: $($user.avatar)" -ForegroundColor Green
        Write-Host "Token: $($token.Substring(0, 20))..." -ForegroundColor Green
        
        # 3. 测试获取用户信息
        Write-Host "`n3. 获取当前用户信息:" -ForegroundColor Yellow
        $userResponse = Invoke-WebRequest -Uri "$apiUrl/api/v1/auth/me" `
            -Method GET `
            -Headers @{ "Authorization" = "Bearer $token" } `
            -UseBasicParsing
        
        $userData = $userResponse.Content | ConvertFrom-Json
        Write-Host "状态码: $($userResponse.StatusCode)" -ForegroundColor Cyan
        Write-Host "用户ID: $($userData.data.id)" -ForegroundColor Green
        Write-Host "用户头像: $($userData.data.avatar)" -ForegroundColor Green
        
        Write-Host "`n修复验证成功！" -ForegroundColor Green
    } else {
        Write-Host "登录失败: $($loginData.message)" -ForegroundColor Red
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    $errBody = $_.Exception.Response.GetResponseStream()
    $reader = [System.IO.StreamReader]::new($errBody)
    $errContent = $reader.ReadToEnd()
    Write-Host "登录错误 (状态码: $statusCode): $errContent" -ForegroundColor Red
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green
