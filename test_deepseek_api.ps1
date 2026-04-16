#!/usr/bin/env pwsh
# ========================================
# DeepSeek API 连接测试脚本
# 用途：验证 DeepSeek API Key 和连接是否正常
# 使用：.\test_deepseek_api.ps1
# ========================================

# 配置部分 - 修改这里为你的真实 API Key
$apiKey = "sk-YOUR_DEEPSEEK_API_KEY_HERE"  # 👉 替换为你的真实 API Key
$apiUrl = "https://api.deepseek.com/v1/chat/completions"
$model = "deepseek-chat"

# 验证 API Key 是否已配置
if ($apiKey -match "YOUR_DEEPSEEK_API_KEY" -or $apiKey -match "xxx") {
    Write-Host "❌ 错误：请先配置真实的 DeepSeek API Key" -ForegroundColor Red
    Write-Host "   1. 访问 https://platform.deepseek.com/" -ForegroundColor Yellow
    Write-Host "   2. 在 API Keys 页面创建新 Key" -ForegroundColor Yellow
    Write-Host "   3. 将 Key 替换到此脚本的第 9 行" -ForegroundColor Yellow
    exit 1
}

Write-Host "🔧 DeepSeek API 连接测试" -ForegroundColor Green
Write-Host "API Key (masked): ***$($apiKey.Substring($apiKey.Length - 6))" -ForegroundColor Cyan
Write-Host "Model: $model" -ForegroundColor Cyan
Write-Host "URL: $apiUrl" -ForegroundColor Cyan
Write-Host ""

# 准备请求头
$headers = @{
    "Authorization" = "Bearer $apiKey"
    "Content-Type" = "application/json"
}

# 准备请求体（简单的问候测试）
$body = @{
    model = $model
    messages = @(
        @{
            role = "system"
            content = "你是一个友好的 AI 助手，帮助用户进行数据分析和查询。"
        },
        @{
            role = "user"
            content = "你好！请简单介绍一下你自己（不超过 30 字）。"
        }
    )
    temperature = 0.7
    max_tokens = 150
} | ConvertTo-Json

Write-Host "📤 发送测试请求..." -ForegroundColor Cyan

try {
    # 发送 HTTP 请求
    $response = Invoke-WebRequest -Uri $apiUrl `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -UseBasicParsing

    # 解析响应
    $result = $response.Content | ConvertFrom-Json

    if ($result.choices -and $result.choices[0].message.content) {
        Write-Host "✅ 连接成功！" -ForegroundColor Green
        Write-Host ""
        Write-Host "AI 回复：" -ForegroundColor Green
        Write-Host $result.choices[0].message.content -ForegroundColor White
        Write-Host ""
        
        # 显示 Token 使用情况
        if ($result.usage) {
            Write-Host "📊 Token 使用情况：" -ForegroundColor Cyan
            Write-Host "  - Prompt tokens:     $($result.usage.prompt_tokens)" -ForegroundColor Gray
            Write-Host "  - Completion tokens: $($result.usage.completion_tokens)" -ForegroundColor Gray
            Write-Host "  - Total tokens:      $($result.usage.total_tokens)" -ForegroundColor Gray
        }
        
        Write-Host ""
        Write-Host "✨ DeepSeek API 已准备就绪，可以集成到项目中！" -ForegroundColor Green
        exit 0
    } else {
        Write-Host "❌ API 返回异常响应" -ForegroundColor Red
        Write-Host "响应内容：" -ForegroundColor Yellow
        Write-Host ($result | ConvertTo-Json) -ForegroundColor Yellow
        exit 1
    }

} catch {
    Write-Host "❌ API 请求失败" -ForegroundColor Red
    Write-Host "错误信息：$($_.Exception.Message)" -ForegroundColor Yellow
    
    # 尝试提供更详细的错误信息
    if ($_.Exception.Response) {
        $streamReader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $errorBody = $streamReader.ReadToEnd()
        Write-Host "错误详情：" -ForegroundColor Yellow
        Write-Host $errorBody -ForegroundColor Yellow
        $streamReader.Close()
    }
    
    exit 1
}
