#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ChatAnywhere API 直接测试脚本
用来验证 Base URL 和 API Key 是否正常工作
"""

import requests
import json
import sys

# 配置信息（从 ai_config_setup.sql 提取）
API_URL = "https://api.chatanywhere.org/v1/chat/completions"
API_KEY = "sk-H4un53BqEQ0D9VpvbbeqCzSyFGCrusdY9icJ1OYzaWeVxy0n"
MODEL = "gpt-3.5-turbo-ca"

print("=" * 50)
print("测试 ChatAnywhere API")
print("=" * 50)
print()

print("测试信息:")
print(f"API URL  : {API_URL}")
print(f"API Key  : {API_KEY[:10]}...({len(API_KEY)}字符)")
print(f"Model    : {MODEL}")
print()

headers = {
    "Authorization": f"Bearer {API_KEY}",
    "Content-Type": "application/json"
}

# 测试1: 简单的 Ping 请求
print("【测试1】基础连接测试 (无系统提示词)")
print("发送请求...")

payload1 = {
    "model": MODEL,
    "messages": [
        {
            "role": "user",
            "content": "你好"
        }
    ],
    "temperature": 0.7
}

try:
    response = requests.post(API_URL, json=payload1, headers=headers, timeout=30)
    response.raise_for_status()
    
    result = response.json()
    reply = result.get("choices", [{}])[0].get("message", {}).get("content", "")
    
    print(f"✅ 状态码: {response.status_code}")
    print(f"回复内容: {reply}")
    print()
except Exception as e:
    print(f"❌ 请求失败: {str(e)}")
    print()
    sys.exit(1)

# 测试2: 带系统提示词的数据查询请求
print("【测试2】带系统提示词的智能查询")
print("发送请求...")

payload2 = {
    "model": MODEL,
    "messages": [
        {
            "role": "system",
            "content": "你是一个数据分析助手。根据提供的数据统计信息用简洁的自然语言回答问题。"
        },
        {
            "role": "user",
            "content": "请分析一下存储在数据库中的用户数据：系统用户总数为18，今天新增用户数为3，过去7天活跃用户数为12"
        }
    ],
    "temperature": 0.7
}

try:
    response2 = requests.post(API_URL, json=payload2, headers=headers, timeout=30)
    response2.raise_for_status()
    
    result2 = response2.json()
    reply2 = result2.get("choices", [{}])[0].get("message", {}).get("content", "")
    
    print(f"✅ 状态码: {response2.status_code}")
    print(f"回复内容: {reply2}")
    print()
    
    # 判断是否是真正的AI回复
    if "系统用户总数为18" in reply2 and len(reply2) < 50:
        print("⚠️  警告: 这看起来不像真正的 AI 回复！")
        print("原因可能是:")
        print("  1. API密钥无效或被降级到测试模式")
        print("  2. 账户没有额度")
        print("  3. 服务商已禁用")
    else:
        print("✅ 这是智能的 AI 回复！")
        
except Exception as e:
    print(f"❌ 请求失败: {str(e)}")
    print()
    
print()
print("=" * 50)
print("诊断总结:")
print("=" * 50)
print("✅ 如果测试1和2都返回智能回复  → 问题在前端/后端客户端代码")
print("❌ 如果测试1失败（无网络/无密钥）  → 检查API Key和网络连接")
print("⚠️  如果测试2返回固定数据      → 账户被降级到测试模式，需要联系ChatAnywhere客服")
print()
print("用于手动测试的 curl 命令:")
print("-" * 50)
print(f"""
curl -X POST "{API_URL}" \\
  -H "Authorization: Bearer {API_KEY}" \\
  -H "Content-Type: application/json" \\
  -d '{{
    "model": "{MODEL}",
    "messages": [
      {{"role": "user", "content": "你好"}}
    ],
    "temperature": 0.7
  }}'
""")
