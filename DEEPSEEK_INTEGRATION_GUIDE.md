# 🚀 DeepSeek API 集成指南（毕业设计版）

> ✨ **亮点：** DeepSeek 价格便宜（仅 ChatGPT 的 1%），功能强大，与 OpenAI API 完全兼容！

---

## 📦 快速开始（5分钟）

### 步骤 1️⃣：获取 API Key

1. **访问平台**：https://platform.deepseek.com/
2. **注册/登录**：使用邮箱或社交账号注册
3. **创建 API Key**：
   - 左侧菜单 → `API Keys`
   - 点击 `Create API Key`
   - 输入名称（如 `my-graduation-project`）
   - **立即复制保存**（关闭后不再显示！）
4. **获得额度**：新用户通常赠送 $5 免费额度

### 步骤 2️⃣：配置数据库

打开 MySQL 客户端（如 Navicat 或命令行），执行以下 SQL：

```sql
-- 【最重要】将下面的 sk-xxxx 替换为你的真实 API Key
UPDATE ai_config SET 
  api_key = 'sk-YOUR_DEEPSEEK_API_KEY_HERE',  -- ⚠️ 必须替换！
  base_url = 'https://api.deepseek.com/v1',
  model = 'deepseek-chat',
  is_active = 0
WHERE provider = 'deepseek';

-- 验证配置
SELECT provider, api_key, base_url, model, is_active FROM ai_config 
WHERE provider = 'deepseek';
```

或者更方便地，直接执行项目中的配置文件：

```bash
# 文件位置：./configure_deepseek.sql
# 用你的真实 API Key 替换其中的占位符，然后执行
mysql -u root -p your_database_name < configure_deepseek.sql
```

### 步骤 3️⃣：测试连接

#### 方案 A：PowerShell 测试（Windows 推荐）

```powershell
# 编辑 test_deepseek_api.ps1，修改第 9 行的 API Key
# 然后执行：
.\test_deepseek_api.ps1
```

**预期输出：**
```
✅ 连接成功！

AI 回复：
   你好！我是 DeepSeek AI 助手，可以帮你进行数据分析和查询。...

📊 Token 使用情况：
   - Prompt tokens:     45
   - Completion tokens: 32
   - Total tokens:      77
```

#### 方案 B：Python 测试（跨平台推荐）

```bash
# 安装依赖
pip install requests

# 编辑 test_deepseek_api.py，修改第 14 行的 API Key
# 然后执行：
python test_deepseek_api.py
```

#### 方案 C：前端 UI 测试（最简单）

1. 启动后端：`java -jar target/time-manager-backend-0.0.1-SNAPSHOT.jar`
2. 启动前端：`npm run dev`
3. 在前端打开管理员 AI 助手页面
4. 在配置卡片选择 `DeepSeek`
5. 点击 `测试连接` 按钮
6. 如果显示 `✓ 连接成功`，就说明配置正确！

---

## 🎯 使用场景与样例

### 场景 1：闲聊识别 ✨

测试输入：
```
用户: 你好，你是谁？
```

预期回复（不再是硬编码的数据统计！）：
```
AI: 你好！我是小智，你的时间管理助手。我可以帮你查询系统数据、
    分析登录情况，或者进行日常闲聊。有什么我可以帮助的吗？
```

### 场景 2：数据查询 📊

测试输入：
```
用户: 最近一小时有多少次登录失败？
```

预期回复：
```
AI: 根据最近一小时的数据，系统记录了 0 次登录失败，说明系统
    登录状态良好，没有异常的暴力破解企图。
```

### 场景 3：多轮对话 💬

```
用户: 分析一下今天的用户活跃情况

AI: [执行数据查询] → "今天共有 12 个用户登录，其中 8 个用户执行了任务操作..."

用户: 相比昨天怎么样？

AI: [记录了前面的对话上下文] → "相比昨天的 15 个用户，今天少了 3 个活跃用户，
    可能与周三的特殊情况有关。"
```

---

## ⚙️ 配置参考

### 模型选择

| 模型 | 价格 | 特点 | 用途 |
|------|------|------|------|
| `deepseek-chat` | $0.28/M in, $1.12/M out | 均衡 | ✅ **推荐**，一般用途 |
| `deepseek-reasoner` | $0.55/M in, $2.19/M out | 推理强 | 复杂逻辑分析 |

**建议**：毕业设计选择 `deepseek-chat`，性价比最高！

### 参数配置

```java
// 在你的代码中可以这样使用
String apiKey = "sk-xxx";
String baseUrl = "https://api.deepseek.com/v1";
String model = "deepseek-chat";

// 参数建议
int maxTokens = 2000;        // 足够大多数场景
double temperature = 0.7;    // 平衡创意与准确性
                             // 0.0-0.3: 更确定、逻辑严谨
                             // 0.7-1.0: 更创意、多样化
```

---

## 🔄 与你现有系统的集成

### 后端 (已支持，无需改动！)

你的系统已经通过 `DynamicAiService` 支持多供应商切换：

```java
// DynamicAiService 会自动根据 ai_config 表选择供应商
// 配置完 DeepSeek 后，下面的代码会自动调用 DeepSeek API
String response = dynamicAiService.chat(systemPrompt, userQuestion);
```

### 前端 (已支持，无需改动！)

前端 AI 助手已经支持供应商切换：

```typescript
// 在 AIAssistant.vue 中已实现
@GetMapping("/api/v1/admin/ai-config/switch/{provider}")
// 或者手动调用
await adminAiApi.switchProvider("deepseek");
```

### 新增功能：会话历史 & 意图识别 ✨

你最近实现的这些功能与 DeepSeek 完美兼容：

```
ChatAnywhere (旧) → [无意图识别] → 硬编码回复
     ↓
DeepSeek (新) → [意图识别(CHITCHAT/QUERY)] → 智能回复
           → [会话历史记录] → 避免重复
           → [上下文感知] → 自然对话
```

---

## 💡 最佳实践建议

### ✅ DO（推荐做法）

```javascript
// ✅ 好：保留 sessionId 跨请求，维持对话上下文
const response = await queryData({
  question: userInput,
  sessionId: sessionId  // 保留会话
});

// ✅ 好：使用 temperature=0.7 平衡质量
// ✅ 好：max_tokens=2000 足够大多数情况
// ✅ 好：定期切换供应商做 A/B 测试
```

### ❌ DON'T（避免的做法）

```javascript
// ❌ 坏：每次都生成新 sessionId，失去上下文
const newSessionId = uuid();  // 每次都生成新的！

// ❌ 坏：temperature=0 完全确定，不够自然
// ❌ 坏：max_tokens=200 太短，容易截断
// ❌ 坏：硬编码查询模板，不灵活
```

---

## 🧪 常见问题排查

### Q1：连接测试显示 "401 Unauthorized"

**原因**：API Key 错误或过期

**解决**：
1. 检查 api_key 是否正确复制（不要有空格）
2. 在 platform.deepseek.com 确认 Key 仍有效
3. 重新生成新 Key 并更新数据库

### Q2：显示 "超配额" 或 "Quota exceeded"

**原因**：免费额度已用尽

**解决**：
1. 登录 https://platform.deepseek.com/
2. 检查账户余额和配额
3. 如需要，可升级付费或添加充值
4. 新账号通常有 $5 免费额度

### Q3：前端切换到 DeepSeek 后无法查询

**原因**：数据库配置可能未正确保存

**解决**：
1. 检查 MySQL 的 ai_config 表：
   ```sql
   SELECT * FROM ai_config WHERE provider = 'deepseek';
   ```
2. 确认 `api_key`、`base_url`、`model` 都正确
3. 重启后端服务

### Q4：回复内容与预期不符

**原因**：系统 Prompt 需要优化

**解决**：
在 `AdminAiService.java` 中调整 Prompt，例如：
```java
String intentPrompt = """
    你是一个数据分析助手...
    [调整 Prompt 以改进回复质量]
    """;
```

---

## 📊 成本估算

### 免费试用
- **新用户额度**：$5 USD
- **足以使用**：~1,000-2,000 次查询（根据复杂度）

### 付费使用
| 使用级别 | 月成本 | 查询次数 | 用途 |
|---------|--------|---------|------|
| 轻度使用 | < $1 | 1-5K | 毕业设计、演示 |
| 中度使用 | $5-20 | 10-100K | 小型应用 |
| 生产级 | $100+ | 100K+ | 大规模应用 |

**建议**：毕业设计完全可在免费额度范围内完成！

---

## 🎓 毕业设计中的应用建议

### 核心功能
```
1. AI 智能查询助手 ✅
   - 自然语言 → SQL 查询（推荐 deepseek-reasoner 用于复杂逻辑）
   - 结果数据 → 自然语言描述

2. 会话历史管理 ✅（已实现）
   - 记录用户对话
   - 避免重复查询
   - 上下文感知

3. 意图识别 ✅（已实现）
   - CHITCHAT：闲聊回复
   - QUERY：数据查询
   - UNKNOWN：不理解
```

### 创新功能（可选）
```
4. 流式输出（SSE）
   - 实时显示 AI 思考过程
   - 提升用户体验

5. 多模型对比
   - GPT-3.5 vs Deepseek vs Claude
   - A/B 测试最佳效果

6. 成本分析仪表板
   - 实时显示 API 成本
   - Token 使用统计
```

---

## 📞 技术支持

- **DeepSeek 官方文档**：https://api-docs.deepseek.com/
- **官方 API 应用**：https://api.deepseek.com
- **平台后台**：https://platform.deepseek.com/
- **社区讨论**：DeepSeek 论坛和 Discord

---

## ✨ 总结

恭喜！你现在拥有：

- ✅ ChatAnywhere 和 DeepSeek 双供应商支持
- ✅ 意图识别和多轮对话上下文
- ✅ 经济高效的 AI 驱动毕业设计
- ✅ 可切换、可扩展的架构

**下一步**：
1. 配置 API Key
2. 测试连接
3. 启动系统
4. 开始构建你的毕业设计！

祝你毕业设计顺利！🎉
