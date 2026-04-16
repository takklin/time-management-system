#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
DeepSeek API 连接测试脚本 (Python 版)
用途：验证 DeepSeek API Key 和连接是否正常
使用：python test_deepseek_api.py

依赖：requests 库
安装：pip install requests
"""

import requests
import json
import sys
from datetime import datetime

# ========================================
# 配置部分 - 修改这里为你的真实 API Key
# ========================================

API_KEY = "sk-YOUR_DEEPSEEK_API_KEY_HERE"  # 👉 替换为你的真实 API Key
API_URL = "https://api.deepseek.com/v1/chat/completions"
MODEL = "deepseek-chat"

# ========================================
# 主程序
# ========================================

def test_deepseek_connection():
    """测试 DeepSeek API 连接"""
    
    print("\n" + "=" * 60)
    print("🔧 DeepSeek API 连接测试")
    print("=" * 60 + "\n")
    
    # 验证 API Key
    if "YOUR_DEEPSEEK_API_KEY" in API_KEY or "xxx" in API_KEY:
        print("❌ 错误：请先配置真实的 DeepSeek API Key")
        print("\n步骤：")
        print("  1. 访问 https://platform.deepseek.com/")
        print("  2. 注册并登录账户")
        print("  3. 在 API Keys 页面创建新 Key")
        print("  4. 将 Key 复制到此脚本的第 12 行")
        print(f"\n当前 API Key: {API_KEY}\n")
        return False
    
    # 显示配置信息
    print(f"✅ API Key (masked): ***{API_KEY[-8:]}")
    print(f"📍 Model: {MODEL}")
    print(f"🌐 URL: {API_URL}")
    print("\n" + "-" * 60)
    print("📤 发送测试请求...\n")
    
    try:
        # 准备请求头
        headers = {
            "Authorization": f"Bearer {API_KEY}",
            "Content-Type": "application/json"
        }
        
        # 准备请求体
        payload = {
            "model": MODEL,
            "messages": [
                {
                    "role": "system",
                    "content": "你是一个友好的 AI 助手，帮助用户进行数据分析和查询。"
                },
                {
                    "role": "user",
                    "content": "你好！请简单介绍一下你自己（不超过 30 字）。"
                }
            ],
            "temperature": 0.7,
            "max_tokens": 150,
            "stream": False
        }
        
        # 发送 HTTP 请求
        response = requests.post(
            API_URL,
            headers=headers,
            json=payload,
            timeout=30
        )
        
        # 检查响应状态
        if response.status_code != 200:
            print(f"❌ API 请求失败 (HTTP {response.status_code})")
            print(f"\n错误详情：")
            try:
                error_data = response.json()
                print(json.dumps(error_data, indent=2, ensure_ascii=False))
            except:
                print(response.text)
            return False
        
        # 解析响应
        result = response.json()
        
        # 检查响应内容
        if "choices" not in result or not result["choices"]:
            print("❌ API 返回异常响应（没有 choices）")
            print(f"\n响应内容：")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            return False
        
        # 获取回复内容
        message = result["choices"][0]["message"]["content"]
        
        # 成功！
        print("✅ 连接成功！\n")
        print(f"🤖 AI 回复：")
        print(f"   {message}\n")
        
        # 显示 Token 使用情况
        if "usage" in result:
            usage = result["usage"]
            print(f"📊 Token 使用情况：")
            print(f"   - Prompt tokens:     {usage.get('prompt_tokens', 'N/A')}")
            print(f"   - Completion tokens: {usage.get('completion_tokens', 'N/A')}")
            print(f"   - Total tokens:      {usage.get('total_tokens', 'N/A')}\n")
            
            # 计算成本估算
            input_tokens = usage.get('prompt_tokens', 0)
            output_tokens = usage.get('completion_tokens', 0)
            input_cost = input_tokens / 1_000_000 * 0.28  # $0.28/1M input tokens
            output_cost = output_tokens / 1_000_000 * 1.12  # $1.12/1M output tokens
            total_cost = input_cost + output_cost
            
            print(f"💰 成本估算：")
            print(f"   - Input cost:       ${input_cost:.6f}")
            print(f"   - Output cost:      ${output_cost:.6f}")
            print(f"   - Total cost:       ${total_cost:.6f}\n")
        
        print("=" * 60)
        print("✨ DeepSeek API 已准备就绪！")
        print("现在可以：")
        print("  1. 在前端切换到 DeepSeek 提供商")
        print("  2. 执行 configure_deepseek.sql 配置数据库")
        print("  3. 立即开始在毕业设计中使用强大的 AI 功能")
        print("=" * 60 + "\n")
        
        return True
    
    except requests.exceptions.Timeout:
        print("❌ 请求超时（网络连接可能有问题）")
        return False
    
    except requests.exceptions.ConnectionError as e:
        print(f"❌ 连接错误：{e}")
        print("\n可能的原因：")
        print("  - 网络连接断开")
        print("  - 代理设置不正确")
        print("  - 防火墙阻止了请求")
        return False
    
    except Exception as e:
        print(f"❌ 未知错误：{e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = test_deepseek_connection()
    sys.exit(0 if success else 1)
