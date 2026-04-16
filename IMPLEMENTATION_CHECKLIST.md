# AI 功能实现检查清单

**项目**: 时间管理系统 AI 智能功能  
**完成目标**: 完整的多API切换 + 用户端AI助手 + 管理员端AI助手 + 自动预警系统  
**预计工作量**: 3-5天（取决于开发经验）  

---

## 📋 第一阶段：准备工作（0.5天）

### 1.1 环境准备
- [ ] 获取 DeepSeek API Key（访问 https://platform.deepseek.com）
- [ ] 获取 ChatAnywhere API Key（访问 https://api.chatanywhere.com）
- [ ] 验证 MySQL 版本 >= 5.7（支持 JSON 字段）
- [ ] 本地环境已装 JDK 11+ 和 Maven 3.6+

### 1.2 数据库初始化
- [ ] 执行 `ai_database_init.sql` 脚本创建 4 个新表
  ```sql
  -- 在 MySQL 中运行
  source /path/to/ai_database_init.sql;
  ```
- [ ] 验证表创建成功：`show tables like 'ai_%';`
- [ ] 检查 `ai_config` 表里有 DeepSeek 和 ChatAnywhere 的配置
- [ ] **替换 API Key**: 修改 `ai_config` 表中的 `api_key` 字段为实际的密钥

### 1.3 代码准备
- [ ] 查看本项目根目录的 `AI_DESIGN_PLAN.md` 了解整体设计
  ```text
  e:\githubProject\time-management-system\
  ├── AI_DESIGN_PLAN.md          ← 完整设计文档
  ├── AI_QUICK_REFERENCE.md      ← 快速参考指南
  └── ai_database_init.sql       ← 数据库脚本（已执行）
  ```

---

## 🔧 第二阶段：后端开发（1.5-2天）

### 2.1 Backend 依赖配置
- [ ] 打开 `backend/pom.xml`
- [ ] 添加 Spring AI 依赖
  ```xml
  <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
      <version>0.11.0</version>
  </dependency>
  ```
- [ ] 添加 WebSocket 依赖
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-websocket</artifactId>
  </dependency>
  ```
- [ ] 添加 Guava 缓存库
  ```xml
  <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
      <version>32.0.0-jre</version>
  </dependency>
  ```
- [ ] 执行 `mvn clean install`

### 2.2 创建 Entity 和 Mapper
- [ ] 创建 `backend/src/main/java/com/timemanager/entity/AiConfig.java`
  ```java
  @Data
  @TableName("ai_config")
  public class AiConfig {
      private Long id;
      private String provider;        // deepseek / chatanywhere
      private String apiKey;
      private String baseUrl;
      private String model;
      private Integer isActive;       // 0 or 1
      private LocalDateTime createdAt;
      private LocalDateTime updatedAt;
  }
  ```
- [ ] 创建 `backend/src/main/java/com/timemanager/entity/AiAlert.java`
  ```java
  @Data
  @TableName("ai_alert")
  public class AiAlert {
      private Long id;
      private String alertType;       // ABNORMAL_LOGIN, BULK_DELETE, etc
      private String severity;        // HIGH, MEDIUM, LOW
      private String title;
      private String description;
      private String suggestion;
      private String relatedLogIds;   // JSON array
      private Integer isHandled;      // 0 or 1
      private Long handlerId;
      private LocalDateTime handledAt;
      private LocalDateTime createdAt;
  }
  ```
- [ ] 创建 `backend/src/main/java/com/timemanager/mapper/AiConfigMapper.java`
  ```java
  @Mapper
  public interface AiConfigMapper extends BaseMapper<AiConfig> {
  }
  ```
- [ ] 创建 `backend/src/main/java/com/timemanager/mapper/AiAlertMapper.java`
  ```java
  @Mapper
  public interface AiAlertMapper extends BaseMapper<AiAlert> {
  }
  ```

### 2.3 创建 AI 配置管理器
- [ ] 创建目录 `backend/src/main/java/com/timemanager/ai/config/`
- [ ] 创建 `AiConfigManager.java` 
  - [ ] 实现 `getActiveConfig()` 方法
  - [ ] 实现 `switchTo(provider)` 方法
  - [ ] 实现 `loadFromDB()` 方法
  - [ ] 实现 `listAll()` 方法
- [ ] 测试配置加载：启动应用并检查日志

### 2.4 创建统一 AI 服务
- [ ] 创建目录 `backend/src/main/java/com/timemanager/ai/service/`
- [ ] 创建 `DynamicAiService.java`
  - [ ] 实现 `chat(systemPrompt, userMessage)` 方法
  - [ ] 该方法应调用 `configManager.getActiveConfig()` 获取当前提供商
  - [ ] 使用 Spring AI 的 `OpenAiApi` 和 `ChatClient` 调用 API
  - [ ] 添加异常处理和日志记录
- [ ] 测试：在浏览器调用一个简单的 API 端点验证

### 2.5 用户端 AI 服务
- [ ] 创建 `UserAiService.java`
  - [ ] 实现 `chat(userId, message)` - 基础对话
  - [ ] 实现 `parseTaskFromNaturalLanguage(input)` - 自然语言解析任务
  - [ ] 实现 `generateDailySummary(userId)` - 生成今日总结
  - [ ] 实现 `extractJson(text)` - JSON提取工具方法
- [ ] 创建 `UserAiController.java` 
  - [ ] 路由：`POST /api/v1/user/ai/chat`
  - [ ] 路由：`POST /api/v1/user/ai/parse-task`
  - [ ] 路由：`GET /api/v1/user/ai/summary/today`
- [ ] 测试各个端点

### 2.6 管理员 AI 服务
- [ ] 创建 `AdminAiService.java`
  - [ ] 实现 `handleNaturalLanguageQuery(question)` - 处理自然语言查询
  - [ ] 实现 `executeQuery(question)` - 数据查询执行（根据关键词调用不同Mapper）
  - [ ] 实现 `getUnhandledAlerts()` - 获取未处理预警
  - [ ] 实现 `markAlertHandled(alertId, handlerId)` - 标记预警已处理
  - [ ] 实现 `scanAndGenerateAlerts()` - 扫描日志生成预警
  - [ ] 实现私有方法：`analyzeAbnormalLogin()`, `analyzeDeleteOperations()` 等
  - [ ] 实现 `createAlert()` - 创建预警并推送
- [ ] 创建 `AdminAiController.java`
  - [ ] 路由：`POST /api/v1/admin/ai/query`
  - [ ] 路由：`GET /api/v1/admin/ai/alerts/unhandled`
  - [ ] 路由：`PUT /api/v1/admin/ai/alert/{id}/handle`
  - [ ] 路由：`POST /api/v1/admin/ai/scan-logs`

### 2.7 日志异常检测器
- [ ] 创建目录 `backend/src/main/java/com/timemanager/ai/detector/`
- [ ] 创建 `LogAnomalyDetector.java`
  - [ ] 创建 Caffeine 缓存：`Cache<String, String> aiResponseCache`
  - [ ] 实现 `@Scheduled(fixedDelay = 600000)` 定时扫描方法
  - [ ] 实现 `analyzeWithCache(logs, alertType)` - 带缓存的分析
  - [ ] 实现 `buildLogSummary()` - 生成日志摘要
  - [ ] 实现 `generateCacheKey()` - 生成MD5缓存键
- [ ] 验证定时任务能正常运行

### 2.8 WebSocket 配置
- [ ] 创建 `WebSocketConfig.java`
  ```java
  @Configuration
  @EnableWebSocketMessageBroker
  public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
      @Override
      public void configureMessageBroker(MessageBrokerRegistry config) {
          config.enableSimpleBroker("/topic");
          config.setApplicationDestinationPrefixes("/app");
      }
      
      @Override
      public void registerStompEndpoints(StompEndpointRegistry registry) {
          registry.addEndpoint("/ws-alert").withSockJS();
      }
  }
  ```
- [ ] 在 `AdminAiService.createAlert()` 中使用 `messagingTemplate.convertAndSend()`

### 2.9 配置管理 API
- [ ] 创建 `AdminAiConfigController.java`
  - [ ] 路由：`GET /api/v1/admin/ai-config/list` - 列出所有配置
  - [ ] 路由：`POST /api/v1/admin/ai-config/switch/{provider}` - 切换提供商
  - [ ] 路由：`POST /api/v1/admin/ai-config/test-connection/{provider}` - 测试连接
  - [ ] 路由：`PUT /api/v1/admin/ai-config/{id}` - 更新配置

### 2.10 修复搜索功能
- [ ] 打开 `AdminController.java`
- [ ] 修改 `listUsers()` 方法使用 `QueryWrapper` 进行实际过滤
- [ ] 支持按 `keyword`（用户名/邮箱）和 `status` 过滤
- [ ] 正确实现分页逻辑

### 2.11 配置文件
- [ ] 打开 `backend/src/main/resources/application.properties`
- [ ] 添加配置
  ```properties
  # AI配置
  ai.default-provider=deepseek
  ai.cache-minutes=5
  
  # WebSocket
  server.servlet.context-path=/
  
  # 日志扫描
  ai.log-scan-interval=600000
  ai.log-anomaly-threshold=10
  ```

### 2.12 后端测试
- [ ] 启动 Spring Boot 应用
- [ ] 检查启动日志中AI配置是否正确加载
- [ ] 用 Postman 测试各个 AI API 端点
- [ ] 验证模型切换功能正常
- [ ] 验证预警生成和推送

---

## 🎨 第三阶段：前端开发（1-1.5天）

### 3.1 全局 TypeScript 类型
- [ ] 创建 `frontend/src/types/ai.ts`
  ```typescript
  export interface ChatMessage {
    id: string;
    role: 'user' | 'assistant';
    type: 'text' | 'task' | 'loading';
    content?: string;
    taskData?: any;
  }
  
  export interface AiAlert {
    id: number;
    title: string;
    description: string;
    suggestion: string;
    severity: 'HIGH' | 'MEDIUM' | 'LOW';
  }
  ```

### 3.2 用户端 API 模块
- [ ] 创建 `frontend/src/api/user/ai.ts`
  ```typescript
  export function chat(data: ChatRequest) {
    return request.post('/api/v1/user/ai/chat', data)
  }
  
  export function parseTask(data: ParseTaskRequest) {
    return request.post('/api/v1/user/ai/parse-task', data)
  }
  
  export function getTodaySummary() {
    return request.get('/api/v1/user/ai/summary/today')
  }
  ```
- [ ] 包含所有接口定义

### 3.3 管理员端 API 模块
- [ ] 创建 `frontend/src/api/admin/ai.ts`
  ```typescript
  export function queryData(data: QueryRequest): Promise<QueryResponse> {
    return request.post('/api/v1/admin/ai/query', data)
  }
  
  export function getAlerts() {
    return request.get('/api/v1/admin/ai/alerts/unhandled')
  }
  
  export function switchAiProvider(provider: string) {
    return request.post(`/api/v1/admin/ai-config/switch/${provider}`)
  }
  ```

### 3.4 用户端 AI 助手浮窗
- [ ] 创建 `frontend/src/components/user/AIChatAssistant.vue`
  - [ ] 页面布局：头部 + 消息区 + 快捷按钮 + 输入框
  - [ ] 实现消息展示（用户气泡 + 助手气泡）
  - [ ] 实现 `sendMessage()` 方法调用后端 chat API
  - [ ] 实现 `quickAction()` 快捷按钮逻辑
  - [ ] 实现 `confirmTask()` 方法创建任务
  - [ ] 支持模型切换下拉框
  - [ ] 自动滚动到最新消息
  - [ ] 添加加载动画
- [ ] 测试各项功能

### 3.5 用户端集成
- [ ] 在 `frontend/src/views/Dashboard.vue` 中添加浮窗组件
  ```vue
  <AIChatAssistant />
  ```
- [ ] 验证浮窗在用户端能正常打开/关闭

### 3.6 管理员端 AI 助手页面
- [ ] 创建 `frontend/src/views/admin/AIAssistant.vue`
  - [ ] 布局：左上角配置，左侧对话区，右侧预警面板
  - [ ] 实现配置卡片：模型选择 + 测试连接
  - [ ] 实现对话区：消息列表 + 快捷查询 + 输入框
  - [ ] 实现预警面板：预警列表 + 处理按钮
  - [ ] 实现 `sendQuery()` 调用 AI 查询
  - [ ] 实现 `quickQuery()` 快捷查询按钮
  - [ ] 实现 `handleAlert()` 标记预警已处理
  - [ ] 实现 `testConnection()` 测试API连接
  - [ ] 实现 `switchProvider()` 切换提供商
- [ ] 样式美化（使用 Element Plus 组件库）

### 3.7 管理员导航集成
- [ ] 打开 `frontend/src/router/adminMenu.ts`
- [ ] 添加 AI 助手菜单项
  ```typescript
  {
    path: '/admin/ai-assistant',
    component: AIAssistant,
    meta: { title: 'AI 智能助手' }
  }
  ```
- [ ] 在 `frontend/src/components/AppSidebar.vue` 中显示该菜单

### 3.8 WebSocket 前端连接
- [ ] 创建 `frontend/src/utils/websocket.ts`
  ```typescript
  const client = new StompClient({
    brokerURL: 'ws://localhost:8080/ws-alert'
  });
  
  client.subscribe('/topic/ai-alerts', (message) => {
    // 处理预警推送
    ElNotification({
      title: '⚠️ 安全预警',
      message: message.body,
      type: 'warning'
    });
  });
  ```
- [ ] 在 AIAssistant.vue 中调用连接
- [ ] 验证能收到后端推送的预警

### 3.9 前端样式调整
- [ ] 确保浮窗在移动端也能正常显示
- [ ] 调整预警面板的响应式布局
- [ ] 验证所有交互流畅性

### 3.10 前端测试
- [ ] 测试用户浮窗的所有功能
- [ ] 测试管理员查询功能
- [ ] 测试预警实时推送
- [ ] 测试模型切换
- [ ] 验证在慢网络下的表现

---

## ✅ 第四阶段：集成测试（0.5-1天）

### 4.1 功能集成测试
- [ ] **测试场景1：用户创建任务**
  - [ ] 打开用户端浮窗
  - [ ] 输入自然语言（如"明天下午3点开会"）
  - [ ] 验证AI成功解析
  - [ ] 确认任务添加到列表
  - [ ] 检查数据库中的 task 记录

- [ ] **测试场景2：管理员查询**
  - [ ] 打开管理员AI助手
  - [ ] 输入查询（如"今天新增了多少用户"）
  - [ ] 验证AI正确理解意图
  - [ ] 检查数据库查询结果
  - [ ] 验证自然语言回答

- [ ] **测试场景3：预警推送**
  - [ ] 在 `operation_log` 表插入异常数据
  - [ ] 等待定时任务运行或手动触发
  - [ ] 检查 `ai_alert` 表是否有新预警
  - [ ] 验证管理员页面实时显示预警

- [ ] **测试场景4：模型切换**
  - [ ] 切换 DeepSeek → ChatAnywhere
  - [ ] 测试连接验证新配置
  - [ ] 提问同一问题比较响应

### 4.2 性能测试
- [ ] 并发对话测试（10个用户同时提问）
- [ ] 日志扫描性能测试（查询10万+条日志的耗时）
- [ ] 缓存命中率测试（验证相同问题是否命中缓存）
- [ ] WebSocket 推送延迟测试

### 4.3 错误处理测试
- [ ] API Key 错误时的处理
- [ ] 网络超时时的处理
- [ ] 数据库连接中断时的处理
- [ ] 模型不支持的参数时的处理

### 4.4 安全性检查
- [ ] 验证 API Key 不会在前端暴露
- [ ] 验证某个用户不能查看其他用户的数据
- [ ] 验证只有 admin 用户能访问管理员 AI
- [ ] SQL 注入防护（ORM已保护）

### 4.5 文档完善
- [ ] 补充 API 接口文档（Swagger/OpenAPI）
- [ ] 编写部署手册
- [ ] 编写用户使用指南
- [ ] 整理常见问题和解决方案

---

## 📦 第五阶段：部署与交付（0.5天）

### 5.1 生产环境配置
- [ ] 替换 API Key 为正式密钥
- [ ] 更改 `ai.cache-minutes` 为 10（生产环境）
- [ ] 启用数据库连接池
- [ ] 配置日志级别为 INFO

### 5.2 数据库备份
- [ ] 导出当前数据库结构和数据
- [ ] 保存迁移脚本

### 5.3 部署检查清单
- [ ] [ ] 验证所有环境变量已正确设置
- [ ] [ ] 验证数据库迁移脚本成功执行
- [ ] [ ] 测试生产环境下的AI功能
- [ ] [ ] 验证日志监控配置
- [ ] [ ] 准备故障恢复方案

### 5.4 交付物清单
- [ ] [ ] 源代码（已注释和文档完善）
- [ ] [ ] 数据库脚本
- [ ] [ ] 部署文档
- [ ] [ ] API 文档
- [ ] [ ] 用户手册
- [ ] [ ] 故障排除指南

---

## 🐛 常见问题与解决方案

| 问题 | 症状 | 解决方案 |
|------|------|--------|
| AI API 连接失败 | 500错误，日志显示连接超时 | 检查API Key是否正确，测试网络连接 |
| WebSocket 推送不工作 | 预警不显示 | 检查浏览器控制台是否有错误，验证后端WebSocket配置 |
| 自然语言解析失败 | 任务创建表单里都是 null | 检查 AI 返回的 JSON 格式，调整 systemPrompt |
| 缓存未命中导致API调用过多 | 成本激增 | 增加 `ai.cache-minutes` 或优化日志过滤逻辑 |
| 模型切换后服务无法访问 | 503 Service Unavailable | 重启应用或手动调用 `AiConfigManager.loadFromDB()` |

---

## 📞 技术支持

遇到问题？按照以下步骤排查：

1. **查看日志**  
   ```bash
   tail -f logs/application.log | grep -i "ai"
   ```

2. **检查数据库**  
   ```sql
   -- 查看当前激活的配置
   SELECT provider, model, is_active FROM ai_config;
   
   -- 查看最近的API调用日志
   SELECT * FROM ai_call_log ORDER BY created_at DESC LIMIT 10;
   
   -- 查看预警列表
   SELECT * FROM ai_alert WHERE is_handled = 0;
   ```

3. **测试API连接**  
   ```bash
   curl -X POST http://localhost:8080/api/v1/user/ai/chat \
     -H "Content-Type: application/json" \
     -d '{"message":"你好"}'
   ```

4. **查看前端浏览器控制台**  
   按 F12 打开开发者工具，查看 Network 和 Console 标签

---

## 🎉 完成标志

当以下条件都满足时，项目可视为完成：

- ✅ 用户端浮窗能正常对话
- ✅ 用户能用自然语言创建任务
- ✅ 管理员能进行自然语言查询
- ✅ 管理员能看到实时预警
- ✅ 能在两个API之间无缝切换
- ✅ 所有单元测试通过
- ✅ 所有文档已完善
- ✅ 能成功演示所有功能

**预计总工作量**: 3-5 天（一个全栈开发者）  
**推荐分工**:
- 后端开发：1.5-2 天（重点在 AI Service 和 WebSocket）
- 前端开发：1-1.5 天（重点在UI交互和 WebSocket 前端）
- 测试交付：0.5-1 天（集成测试 + 文档 + 部署）

---

**最后更新**: 2026-04-15  
**版本**: 1.0
