# AI 功能实现快速参考

## 📋 方案文件位置
📄 完整设计方案：[AI_DESIGN_PLAN.md](AI_DESIGN_PLAN.md)

---

## 🎯 核心模块速览

### 1️⃣ 多API切换（第一部分）
| 项目 | 位置 | 说明 |
|------|------|------|
| 配置表 | `ai_config` | 存储 DeepSeek + ChatAnywhere 两个API密钥 |
| 配置管理器 | `AiConfigManager.java` | 动态加载、切换激活的AI提供商 |
| 统一AI服务 | `DynamicAiService.java` | 所有AI调用的唯一入口（自动选择当前激活的提供商） |
| 管理员配置API | `AdminAiConfigController.java` | 供管理员在Web界面切换模型 |

**关键流程**：
```
AdminAiConfigController.switchProvider(provider)
  → AiConfigManager.switchTo(provider)
  → 数据库更新(is_active)
  → DynamicAiService 自动读取最新配置
```

---

### 2️⃣ 用户端AI助手（第二部分）
| 功能 | 文件 | 作用 |
|------|------|------|
| 浮窗UI | `AIChatAssistant.vue` | 界面：聊天框 + 快捷按钮 + 模型切换 |
| 自然语言创建任务 | `UserAiService.parseTask()` | AI理解"明天下午3点开会"→ 提取标题/时间/时长 |
| 今日总结 | `UserAiService.generateDailySummary()` | 查询用户今日完成情况 → AI生成鼓励性总结 |
| 前端API | `api/user/ai.ts` | `chat()` / `parseTask()` / `getTodaySummary()` |
| 后端API | `UserAiController.java` | `/api/v1/user/ai/*` 路由 |

**用户交互流**：
```
用户在浮窗输入 → UserAiController.chat() 
  → UserAiService 调用 DynamicAiService
  → AI返回回复 → 显示在浮窗
```

---

### 3️⃣ 管理员端AI助手（第三部分）
| 功能 | 文件 | 作用 |
|------|------|------|
| 后台页面 | `admin/AIAssistant.vue` | 左：对话区 + 快捷查询; 右：预警面板 |
| 自然语言查询 | `AdminAiService.handleQuery()` | "最近一周新增用户" → AI理解 → SQL查询 → 自然语言回答 |
| 预警表 | `ai_alert` | 存储AI生成的高/中/低风险预警 |
| 预警管理 | `AdminAiService.getUnhandledAlerts()` | 显示待处理的预警列表 |
| 后端API | `AdminAiController.java` | `/api/v1/admin/ai/*` 路由 |

**管理员查询流**：
```
管理员提问 → AdminAiController.query()
  → AdminAiService 让AI理解意图 + 提取参数
  → 执行SQL查询
  → AI将数据转成自然语言
  → 返回给管理员
```

---

### 4️⃣ 操作日志AI预警（第四部分）
| 功能 | 文件 | 说明 |
|------|------|------|
| 异常检测器 | `LogAnomalyDetector.java` | 每10分钟扫描操作日志，检测异常 |
| 缓存机制 | 缓存5分钟 | 相同日志摘要5分钟内只调用一次AI（节约成本） |
| WebSocket推送 | `WebSocketConfig.java` | 预警实时推送到管理员浏览器 |
| 定时任务 | `@Scheduled` | 后台自动运行异常检测 |

**预警工作流**：
```
定时任务 (10分钟扫描一次)
  → 查询操作日志
  → 缓存检查（如果5分钟内分析过此类日志，直接返回缓存）
  → 调用AI分析（如果缓存未命中）
  → 生成预警 + 存入数据库 + WebSocket推送
  → 管理员页面实时显示
```

---

## 🗄️ 数据库表

| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `ai_config` | AI提供商配置 | provider, api_key, base_url, model, is_active |
| `ai_alert` | 预警存储 | alert_type, severity, title, description, suggestion, is_handled |
| `ai_call_log` | AI调用日志（可选，成本追踪） | provider, module, tokens, cost |

---

## 🔧 配置 & 初始化

### SQL 初始化
```sql
-- 在数据库执行这些语句
INSERT INTO ai_config (provider, api_key, base_url, model, is_active) VALUES
('deepseek', 'sk-your-deepseek-key-here', 'https://api.deepseek.com/v1', 'deepseek-chat', 1),
('chatanywhere', 'sk-your-chatanywhere-key-here', 'https://api.chatanywhere.tech/v1', 'gpt-3.5-turbo', 0);
```

### application.properties
```properties
# AI配置
ai.default-provider=deepseek
ai.cache-minutes=5

# WebSocket
spring.websocket.path=/ws-alert

# 日志扫描
ai.log-scan-interval=600000
```

### pom.xml 新增依赖
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>0.11.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.0.0-jre</version>
</dependency>
```

---

## 📁 文件结构

### 后端（Java）
```
src/main/java/com/timemanager/
├── ai/
│   ├── config/
│   │   ├── AiConfigManager.java          ← 配置管理
│   │   └── WebSocketConfig.java          ← 实时推送
│   ├── service/
│   │   ├── DynamicAiService.java         ← 核心（动态选择模型）
│   │   ├── UserAiService.java            ← 用户功能
│   │   └── AdminAiService.java           ← 管理员功能
│   └── detector/
│       └── LogAnomalyDetector.java       ← 自动预警
├── controller/
│   ├── UserAiController.java             ← 用户API
│   ├── AdminAiController.java            ← 管理员查询API
│   └── AdminAiConfigController.java      ← 配置管理API
├── entity/
│   ├── AiConfig.java
│   └── AiAlert.java
└── mapper/
    ├── AiConfigMapper.java
    └── AiAlertMapper.java
```

### 前端（Vue 3）
```
src/
├── api/
│   ├── user/ai.ts                        ← 用户API调用
│   └── admin/ai.ts                       ← 管理员API调用
├── components/
│   └── user/AIChatAssistant.vue          ← 用户浮窗（主要UI）
└── views/
    └── admin/AIAssistant.vue             ← 管理员AI页面（主要UI）
```

---

## 🚀 快速启动步骤

### 第一步：数据库准备
1. 执行上面的 SQL 初始化
2. 获取 DeepSeek 和 ChatAnywhere 的 API Key

### 第二步：后端开发
1. 创建 `ai/config/AiConfigManager.java`
2. 创建 `ai/service/DynamicAiService.java`
3. 创建 `ai/service/UserAiService.java`
4. 创建 `ai/service/AdminAiService.java`
5. 创建 `ai/detector/LogAnomalyDetector.java`
6. 创建对应的 Controller（3个）和 Mapper（2个）
7. 配置 pom.xml + application.properties

### 第三步：前端开发
1. 创建 `components/user/AIChatAssistant.vue` （浮窗）
2. 创建 `views/admin/AIAssistant.vue` （管理页面）
3. 创建 `api/user/ai.ts` 和 `api/admin/ai.ts`
4. 在用户主页和管理员导航栏集成这两个组件

### 第四步：测试
- [ ] 用户浮窗能正常对话
- [ ] 自然语言解析任务成功
- [ ] 管理员查询功能正常
- [ ] WebSocket 预警推送实时显示
- [ ] API 切换不中断服务

---

## 💡 关键实现细节

### 动态模型选择核心代码
```java
// DynamicAiService 中
public String chat(String systemPrompt, String userMessage) {
    AiConfigManager.AiProperties props = configManager.getActiveConfig();
    // props.apiKey, props.baseUrl, props.model 都是当前激活的
    
    OpenAiApi api = new OpenAiApi(props.getBaseUrl(), props.getApiKey());
    // 创建新的 ChatModel，每次都用最新的配置
    OpenAiChatModel chatModel = new OpenAiChatModel(api, 
        OpenAiChatOptions.builder()
            .withModel(props.getModel())
            .build()
    );
    // ... 调用API
}
```

### 用户自然语言解析示例
```java
String input = "明天下午3点到5点准备PPT";
String prompt = """
    从用户输入中提取任务信息，返回JSON：
    { "title": "...", "deadline": "...", "estimatedMinutes": ... }
    """;
String response = dynamicAiService.chat(prompt, input);
// AI 返回: {"title":"准备PPT","deadline":"2026-04-16 15:00","estimatedMinutes":120}
```

### 缓存机制示例
```java
// 相同的日志摘要5分钟内只调用一次AI
String summary = buildLogSummary(logs);
String cacheKey = DigestUtils.md5DigestAsHex(summary.getBytes());

String cached = aiResponseCache.getIfPresent(cacheKey);
if (cached != null) {
    // 使用缓存，节约API调用
    return cached;
}

// 缓存未命中，调用API
String response = dynamicAiService.chat(...);
aiResponseCache.put(cacheKey, response); // 保存5分钟
```

---

## 📊 成本结构

| 操作 | 成本 | 说明 |
|------|-----|------|
| 用户对话 | 按token计费 | 每条消息可能 100-500 token |
| 任务解析 | 低 | 大约 200 token/条 |
| 管理员查询 | 中 | 200-1000 token（取决于数据量） |
| 日志预警 | 可控 | 缓存机制限制频率 |

**成本优化**：
- ✅ 缓存：5分钟相同日志只调用一次
- ✅ 模型切换：免费额度用完可切到其他提供商
- ✅ 选择性预警：仅高风险操作才需要AI分析

---

## 🎓 答辩演示脚本

### 演示1：用户端创建任务
```
1. 打开用户端，点击右下角浮窗
2. 输入："我明天上午9点到11点写毕业论文，大概需要2小时"
3. AI自动识别并填充表单
4. 确认后任务自动添加到任务列表
5. 数据库中新增一条 task 记录
```

### 演示2：管理员查询
```
1. 打开管理员后台 → AI助手页面
2. 点击"今日新增用户"快捷按钮
3. AI理解意图 → 查询数据库 → 返回"今天新增15位用户"
4. 再手动输入："最近一周哪一天活跃用户最多"
5. AI查询后返回详细数据
```

### 演示3：主动预警
```
1. 在 operation_log 表预置异常数据（如5次失败登录）
2. 等待定时任务运行（或手动触发）
3. 管理员页面右上角出现红色预警通知
4. 点击查看AI描述的风险 + 建议（如"建议清单+冻结账号"）
```

### 演示4：API切换
```
1. 管理员配置页 → AI模型选择 → 切换到 ChatAnywhere
2. 点击"测试连接"验证
3. 再提一个问题，对比响应速度
4. 可选：展示两个API的成本差异
```

---

## ❓ 常见问题

**Q: 如何在用户端隐藏AI Key？**
A: API Key 只存在后端数据库（ai_config表），前端通过后端API调用，永远看不到实际的Key。

**Q: 预警系统怎样避免误触发？**
A: 在 LogAnomalyDetector 中设置阈值（如连续5次失败登录才预警），同时使用缓存避免重复分析。

**Q: DeepSeek额度用完了怎么办？**
A: 在管理员后台点"切换到ChatAnywhere"，系统自动切换，无需重启应用。

**Q: WebSocket推送对性能有影响吗？**
A: 预警是低频事件（10分钟扫描一次），对性能几乎无影响。

**Q: 如何自定义AI的行为？**
A: 修改各个 Service 中的 `systemPrompt`（系统提示词），可改变AI的回答风格和信息深度。

---

## 📞 后续支持

需要的帮助：
- [ ] AI Service 层的具体实现
- [ ] 前端 WebSocket 连接示例
- [ ] 更复杂的NLU规则（如时间推断）
- [ ] 性能压测与优化
- [ ] 与业务的深度集成

---

**✅ 版本：1.0 | 最后更新：2026-04-15**
