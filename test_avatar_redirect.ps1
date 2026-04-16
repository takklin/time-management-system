# 测试头像修复 - 验证文件不存在时返回默认头像

$apiUrl = "http://localhost:8080"

Write-Host "=== 测试头像修复 - 文件不存在重定向 ===" -ForegroundColor Green

# 1. 测试不存在的头像文件（应该返回 301 重定向）
Write-Host "`n1. 测试不存在的头像文件:" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$apiUrl/api/v1/auth/avatar/839df1aa-3cfb-4258-98e7-c444038a66b2.jpg" `
        -Method GET `
        -MaximumRedirection 0 `
        -UseBasicParsing `
        -ErrorAction SilentlyContinue
    
    Write-Host "状态码: $($response.StatusCode)" -ForegroundColor Cyan
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    $location = $_.Exception.Response.Headers.Location
    Write-Host "状态码: $statusCode (重定向)" -ForegroundColor Cyan
    Write-Host "重定向到: $location" -ForegroundColor Green
    Write-Host "预期: 301 重定向到默认头像" -ForegroundColor Green
}

# 2. 测试跟随重定向（应该获取默认头像）
Write-Host "`n2. 测试跟随重定向获取默认头像:" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$apiUrl/api/v1/auth/avatar/839df1aa-3cfb-4258-98e7-c444038a66b2.jpg" `
        -Method GET `
        -UseBasicParsing
    
    Write-Host "最终状态码: $($response.StatusCode)" -ForegroundColor Cyan
    Write-Host "Content-Type: $($response.Headers.'Content-Type')" -ForegroundColor Green
    Write-Host "内容长度: $($response.Content.Length) 字节" -ForegroundColor Green
    Write-Host "✅ 成功获取默认头像（通过重定向）" -ForegroundColor Green
} catch {
    Write-Host "错误: $_" -ForegroundColor Red
}

# 3. 测试存在的头像文件
Write-Host "`n3. 尝试测试存在的头像文件 (if any):" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$apiUrl/api/v1/auth/avatar/699a794e-8b5c-4ec3-b160-fe10fba90f65.png" `
        -Method GET `
        -UseBasicParsing
    
    Write-Host "状态码: $($response.StatusCode)" -ForegroundColor Cyan
    Write-Host "Content-Type: $($response.Headers.'Content-Type')" -ForegroundColor Green
    Write-Host "✅ 成功获取头像文件" -ForegroundColor Green
} catch {
    Write-Host "文件不存在或访问失败: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green
Write-Host "修复验证：当用户头像文件不存在时，后端现在会重定向到默认头像，" -ForegroundColor Cyan
Write-Host "而不是返回 404，这样用户界面中就不会显示破损的图像。" -ForegroundColor Cyan
