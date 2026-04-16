# ============================================
# ChatAnywhere API 直接测试脚本
# 用来验证 Base URL 和 API Key 是否正常工作
# ============================================

# 配置信息（从 ai_config_setup.sql 提取）
$API_URL = "https://api.chatanywhere.org/v1/chat/completions"
$API_KEY = "sk-H4un53BqEQ0D9VpvbbeqCzSyFGCrusdY9icJ1OYzaWeVxy0n"
$MODEL = "gpt-3.5-turbo-ca"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试 ChatAnywhere API" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "测试信息:" -ForegroundColor Yellow
Write-Host "API URL  : $API_URL"
Write-Host "API Key  : $($API_KEY.Substring(0,10))...($($API_KEY.Length)字符)"
Write-Host "Model    : $MODEL"
Write-Host ""

# 测试1: 简单的 Ping 请求 - 测试API密钥和基本连接
Write-Host "【测试1】基础连接测试 (无系统提示词)" -ForegroundColor Green
Write-Host "发送请求..." -ForegroundColor Gray

$headers = @{
    "Authorization" = "Bearer $API_KEY"
    "Content-Type"  = "application/json"
}

$body = @{
    model       = $MODEL
    messages    = @(
        @{
            role    = "user"
            content = "你好"
        }
    )
    temperature = 0.7
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri $API_URL `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -ContentType "application/json" `
        -ErrorAction Stop
    
    $result = $response.Content | ConvertFrom-Json
    
    Write-Host "✅ 状态码: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "回复内容:" -ForegroundColor Green
    Write-Host $result.choices[0].message.content
    Write-Host ""
} 
catch {
    Write-Host "❌ 请求失败:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    Write-Host ""
}

# 测试2: 带系统提示词的数据查询请求 - 测试是否真正使用 AI
Write-Host "【测试2】带系统提示词的智能查询" -ForegroundColor Green
Write-Host "发送请求..." -ForegroundColor Gray

$body2 = @{
    model       = $MODEL
    messages    = @(
        @{
            role    = "system"
            content = "你是一个数据分析助手。根据提供的数据统计信息用简洁的自然语言回答问题。"
        },
        @{
            role    = "user"
            content = "请分析一下存储在数据库中的用户数据：系统用户总数为18，今天新增用户数为3，过去7天活跃用户数为12"
        }
    )
    temperature = 0.7
} | ConvertTo-Json

try {
    $response2 = Invoke-WebRequest -Uri $API_URL `
        -Method POST `
        -Headers $headers `
        -Body $body2 `
        -ContentType "application/json" `
        -ErrorAction Stop
    
    $result2 = $response2.Content | ConvertFrom-Json
    
    Write-Host "✅ 状态码: $($response2.StatusCode)" -ForegroundColor Green
    Write-Host "回复内容:" -ForegroundColor Green
    Write-Host $result2.choices[0].message.content
    Write-Host ""
    
    # 判断是否是真正的AI回复
    $reply = $result2.choices[0].message.content
    if ($reply -like "*系统用户总数为18*" -and $reply.Length -lt 50) {
        Write-Host "⚠️  警告: 这看起来不像真正的 AI 回复！" -ForegroundColor Yellow
        Write-Host "   原因可能是:" -ForegroundColor Yellow
        Write-Host "   1. API密钥无效或被降级到测试模式" -ForegroundColor Yellow
        Write-Host "   2. 账户没有额度" -ForegroundColor Yellow
        Write-Host "   3. 服务商已禁用" -ForegroundColor Yellow
    } else {
        Write-Host "✅ 这是智能的 AI 回复！" -ForegroundColor Green
    }
} 
catch {
    Write-Host "❌ 请求失败:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        Write-Host "响应内容:" -ForegroundColor Red
        $errorResponse = $_.Exception.Response.GetResponseStream() | ForEach-Object { 
            New-Object System.IO.StreamReader($_) | ForEach-Object { $_.ReadToEnd() }
        }
        Write-Host $errorResponse
    }
    Write-Host ""
}

# 测试3: 用最小的 curl 命令再测试一次（便于排查）
Write-Host "【测试3】使用 curl 直接调用（便于复制到其他地方调试）" -ForegroundColor Green
Write-Host "执行命令..." -ForegroundColor Gray

$curlCommand = @"
curl -X POST "https://api.chatanywhere.org/v1/chat/completions" `
  -H "Authorization: Bearer $API_KEY" `
  -H "Content-Type: application/json" `
  -d '{
    "model": "$MODEL",
    "messages": [
      {"role": "user", "content": "你好"}
    ],
    "temperature": 0.7
  }' -s | ConvertFrom-Json | Select-Object -ExpandProperty choices | Select-Object -First 1 | Select-Object -ExpandProperty message | Select-Object -ExpandProperty content
"@

Write-Host "Curl命令（可复制到Git Bash或WSL中运行）:" -ForegroundColor Gray
Write-Host @"
curl -X POST "https://api.chatanywhere.org/v1/chat/completions" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "$MODEL",
    "messages": [
      {"role": "user", "content": "你好"}
    ],
    "temperature": 0.7
  }'
"@
Write-Host ""

# 总结测试结果
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "诊断总结:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ 如果测试1和2都返回智能回复  → 问题在前端/后端客户端代码" -ForegroundColor Green
Write-Host "❌ 如果测试1失败（无网络/无密钥）  → 检查API Key和网络连接" -ForegroundColor Red
Write-Host "⚠️  如果测试2返回固定数据      → 账户被降级到测试模式，需要联系ChatAnywhere客服" -ForegroundColor Yellow
