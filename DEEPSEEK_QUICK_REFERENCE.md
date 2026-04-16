# DeepSeek API 快速参考卡

## 🔑 API 基础信息

```
平台地址：https://platform.deepseek.com/
API 端点：https://api.deepseek.com/v1
模型名称：deepseek-chat（推荐）
价格：$0.28/M input tokens，$1.12/M output tokens
文档：https://api-docs.deepseek.com/
```

## 📋 快速配置清单

- [ ] 1. 访问 https://platform.deepseek.com/ 注册 → 登录
- [ ] 2. 左侧菜单 `API Keys` → 创建新 Key → **立即复制保存**
- [ ] 3. 打开 `configure_deepseek.sql` 文件
- [ ] 4. 将 `sk-YOUR_DEEPSEEK_API_KEY_HERE` 替换为你的真实 Key
- [ ] 5. 在 MySQL 客户端执行 SQL 脚本
- [ ] 6. 运行 `test_deepseek_api.ps1` 或 `test_deepseek_api.py` 验证
- [ ] 7. 前端选择 DeepSeek → 点击"测试连接" → ✓ 成功
- [ ] 8. 开始使用！

## 🧪 三种测试方法

### 方法 1：前端 UI (最简单)
```bash
# 启动后端
java -jar backend/target/time-manager-backend-0.0.1-SNAPSHOT.jar

# 启动前端
npm run dev

# 打开 http://localhost:5173
# 进入"小智助手" → 选择 DeepSeek → 点击"测试连接"
```

### 方法 2：PowerShell (Windows)
```powershell
# 编辑 test_deepseek_api.ps1，改第 9 行的 API Key
# 然后运行：
.\test_deepseek_api.ps1
```

### 方法 3：Python (跨平台)
```bash
pip install requests
python test_deepseek_api.py
```

## 📊 API 调用示例

### cURL
```bash
curl https://api.deepseek.com/v1/chat/completions \
  -H "Authorization: Bearer sk-YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [
      {"role": "system", "content": "你是一个有帮助的助手"},
      {"role": "user", "content": "你好"}
    ],
    "temperature": 0.7,
    "max_tokens": 2000
  }'
```

### Python
```python
import requests

headers = {
    "Authorization": "Bearer sk-YOUR_API_KEY",
    "Content-Type": "application/json"
}

payload = {
    "model": "deepseek-chat",
    "messages": [
        {"role": "user", "content": "你好"}
    ],
    "temperature": 0.7,
    "max_tokens": 2000
}

response = requests.post(
    "https://api.deepseek.com/v1/chat/completions",
    headers=headers,
    json=payload
)

print(response.json())
```

### Java/Spring Boot (你的项目)
```java
// 已内置，无需额外代码！
// DynamicAiService.chat() 会自动调用 DeepSeek 或 ChatAnywhere

String response = dynamicAiService.chat(systemPrompt, userQuestion);
```

## 🎯 常用模型参数

| 参数 | 推荐值 | 范围 | 说明 |
|------|--------|------|------|
| `temperature` | 0.7 | 0.0-2.0 | 越小越确定，越大越随意 |
| `max_tokens` | 2000 | 1-8192 | 最大输出长度 |
| `top_p` | 0.95 | 0.0-1.0 | 核心采样，控制多样性 |
| `frequency_penalty` | 0 | -2.0-2.0 | 减少重复 |

## 🔍 常见错误排查

| 错误 | 原因 | 解决 |
|------|------|------|
| 401 Unauthorized | API Key 错误 | 检查 Key 是否正确复制 |
| 429 Too Many Requests | 超过速率限制 | 降低请求频率 |
| Quota exceeded | 额度用尽 | 检查账户还有无剩余额度 |
| Connection timeout | 网络问题 | 检查网络连接/代理 |

## 💰 成本计算

### 新用户免费额度
```
赠送：$5 USD
足够：1000-2000 次查询
预期：足以完成毕业设计
```

### 示例成本
```
100 查询 × (avg 50 input + 30 output tokens)
= 100 × (50/1M × $0.28 + 30/1M × $1.12)
= 100 × (0.000014 + 0.0000336)
≈ $0.004 / 100 查询 ← 非常便宜！
```

## 📝 SQL 配置命令

```sql
-- 一句命令配置 DeepSeek（替换 API Key）
UPDATE ai_config SET 
  api_key = 'sk-YOUR_DEEPSEEK_API_KEY_HERE',
  base_url = 'https://api.deepseek.com/v1',
  model = 'deepseek-chat',
  is_active = 0
WHERE provider = 'deepseek';

-- 验证配置
SELECT provider, CONCAT('***', SUBSTR(api_key, -8)), model 
FROM ai_config 
WHERE provider = 'deepseek';
```

## 🚀 快速启动步骤

```bash
# 1. 配置 API Key
# → 编辑 configure_deepseek.sql，执行 SQL

# 2. 测试连接
python test_deepseek_api.py

# 3. 启动系统
java -jar backend/target/time-manager-backend-0.0.1-SNAPSHOT.jar &
npm run dev

# 4. 打开浏览器
# http://localhost:5173

# 5. 前端切换到 DeepSeek，测试！
```

## 📚 有用的链接

| 资源 | 链接 |
|------|------|
| DeepSeek 平台 | https://platform.deepseek.com/ |
| API 文档 | https://api-docs.deepseek.com/ |
| 定价信息 | https://www.deepseek.com/#pricing |
| 官方博客 | https://www.deepseek.com/blog |
| GitHub (模型开源) | https://github.com/deepseek-ai |

## ✨ 毕业设计建议

```
你的系统现在拥有：
✅ 双供应商支持（ChatAnywhere + DeepSeek）
✅ 意图识别（CHITCHAT vs QUERY）
✅ 会话历史（多轮对话）
✅ 上下文感知（避免重复）
✅ 经济高效（DeepSeek 价格便宜）

这足以成为一个优秀的毕业设计项目！
```

---

**最后提醒**：
- 🔐 不要在 GitHub 上提交真实 API Key
- 💾 定期保存你的 Key
- 📊 监控 API 用量，避免意外超支
- 🔄 定期切换供应商做质量对比

祝毕业设计顺利！🎉
