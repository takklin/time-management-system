package com.timemanager.ai.service;

import com.timemanager.entity.AiAlert;
import com.timemanager.mapper.AiAlertMapper;
import com.timemanager.mapper.UserMapper;
import com.timemanager.mapper.OperationLogMapper;
import com.timemanager.entity.User;
import com.timemanager.entity.OperationLog;
import com.timemanager.ai.dto.ChatMessageDTO;
import com.timemanager.ai.dto.QueryIntentParams;
import com.timemanager.ai.enums.QueryIntent;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员 AI 服务
 * 提供管理员端的 AI 功能：
 * - 自然语言查询系统数据
 * - AI 驱动的数据分析
 * - 操作日志异常检测
 * - 智能预警管理
 */
@Slf4j
@Service
public class AdminAiService {
    
    @Autowired
    private DynamicAiService dynamicAiService;
    
    @Autowired
    private SessionHistoryService sessionHistoryService;
    
    @Autowired
    private AiAlertMapper aiAlertMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private OperationLogMapper operationLogMapper;
    
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 处理自然语言查询（支持会话历史和意图识别）
     * 流程：
     * 1. 获取会话历史上下文
     * 2. AI 识别用户意图（CHITCHAT 闲聊 vs QUERY 数据查询）
     * 3. 根据意图执行不同逻辑
     *    - CHITCHAT: 生成友好的闲聊回复
     *    - QUERY: 执行数据查询 + 自然语言回答
     * 4. 保存对话到会话历史
     * 
     * @param question 用户问题
     * @param sessionId 会话ID（用于维持上下文），如果为空则创建新会话
     * @return 返回 AI 回复 + 原始数据
     */
    public AdminQueryResponse handleNaturalLanguageQuery(String question, String sessionId) {
        try {
            log.info("[管理员AI] 处理查询: sessionId={}, question={}", sessionId, question);
            
            // 第一步：获取会话历史
            List<ChatMessageDTO> history = sessionHistoryService.getRecentMessages(sessionId, 5);
            String historyContext = sessionHistoryService.getSessionSummary(sessionId, 5);
            log.debug("[管理员AI] 会话历史: {}", historyContext);
            
            // 第二步：使用 AI 识别意图
            QueryIntentParams intentParams = parseIntentWithHistory(question, history);
            log.info("[管理员AI] 意图识别结果: intent={}, timeRange={}", 
                intentParams.getIntent(), intentParams.getTimeRange());
            
            String answer = null;
            String rawData = null;
            
            // 第三步：根据意图分支处理
            if ("CHITCHAT".equals(intentParams.getIntent())) {
                // 闲聊分支
                log.debug("[管理员AI] 识别为闲聊问题");
                answer = generateChitchatResponse(question, history);
                sessionHistoryService.saveExchange(sessionId, question, answer, "CHITCHAT");
                
            } else if ("UNKNOWN".equals(intentParams.getIntent())) {
                // 未知意图
                log.debug("[管理员AI] 识别为未知意图");
                answer = "抱歉，我没有理解您的问题。您可以问我系统数据相关的问题，比如用户统计、登录分析、任务完成率等。";
                sessionHistoryService.saveExchange(sessionId, question, answer, "UNKNOWN");
                
            } else {
                // 数据查询分支
                log.debug("[管理员AI] 识别为数据查询问题");
                // ✨ 改进：传递 QueryIntentParams 给查询引擎，支持参数化查询
                rawData = executeQuery(intentParams);
                log.debug("[管理员AI] 查询结果: {}", rawData);
                
                // 使用 AI 将数据转化为自然语言
                String synthesisPrompt = """
                    根据以下数据结果，用简洁的自然语言回答管理员的问题。
                    
                    规则：
                    - 简洁清晰（50-200字）
                    - 突出关键数据
                    - 如果数据为空或为0，礼貌地说明
                    - 避免重复前面已经说过的内容（前面的对话历史）
                    
                    管理员问题: """ + question + """
                    数据结果: """ + rawData;
                
                answer = dynamicAiService.chat(synthesisPrompt, /*历史上下文*/ historyContext);
                sessionHistoryService.saveExchange(sessionId, question, answer, intentParams.getIntent());
            }
            
            log.info("[管理员AI] 查询完成: sessionId={}, answer长度={}", sessionId, answer.length());
            
            return new AdminQueryResponse(answer, rawData);
            
        } catch (Exception e) {
            log.error("[管理员AI] 查询失败: {}", question, e);
            return new AdminQueryResponse("查询失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 使用 AI 识别用户意图（支持会话历史）
     * AI 返回 JSON，识别用户是在闲聊还是在查询数据
     * 
     * ✨ 改进：支持参数提取，包括时间范围、过滤条件、列表大小等
     */
    private QueryIntentParams parseIntentWithHistory(String question, List<ChatMessageDTO> history) {
        try {
            // 构建会话历史上下文
            String historyLines = history.stream()
                .map(m -> "- " + (m.getRole().equals("user") ? "用户" : "助手") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
            
            String intentPrompt = """
                你是一个数据查询意图识别专家。根据用户问题和对话历史，准确识别意图并提取参数。
                
                意图类型（选一个最匹配的）：
                - CHITCHAT: 闲聊/问候（如"你好" "你是谁" "你心情如何"）
                - NEW_USER_COUNT: 新增用户数量统计
                - LOGIN_FAIL_SUMMARY: 登录失败统计
                - ACTIVE_USER_COUNT: 活跃用户统计
                - TASK_COMPLETION_RATE: 任务完成率
                - OPERATION_LOG_ANOMALY: 操作异常检测
                - USER_LIST: 查询用户列表/用户名单（"用户有哪些" "他们是谁" "分别是谁"）
                - USER_DETAIL: 查询具体用户信息
                - TASK_LIST: 查询任务列表
                - TREND_ANALYSIS: 趋势分析（"对比" "增长" "变化"）
                - COMPARISON: 数据对比（"昨天的呢" "相比上周怎样"）
                - GENERAL_DATA_QUERY: 其他数据查询
                - UNKNOWN: 无法理解
                
                时间范围识别规则：
                - "今天" / "今日" / "现在" → "today"
                - "昨天" / "昨日" → "yesterday"  
                - "本周" / "这周" → "this_week"
                - "上周" → "last_week"
                - "本月" / "这月" → "this_month"
                - "上月" → "last_month"
                - "最近X小时" → "last_Xh"（如 "last_1h", "last_24h"）
                
                ⚠️ 关键提示（让 AI 更聪明）：
                1. 如果用户说"昨天的呢"，找出前面说过的内容，推断具体意图
                   例：前面问"今天新增用户"，"昨天的呢" = 昨天新增用户 (intent=NEW_USER_COUNT)
                2. 如果用户说"他们是谁" / "分别是谁"，说明要查询列表 (intent=USER_LIST)
                3. 如果用户说"相比怎样" / "对比"，说明要做对比 (intent=COMPARISON)
                4. 对话历史中的最后一条用户消息最重要！
                
                对话历史：
                """ + (historyLines.isEmpty() ? "（这是第一条消息）" : historyLines) + """
                
                当前问题: """ + question + """
                
                返回 JSON（严格遵循此格式）：
                {
                  "intent": "USER_LIST|NEW_USER_COUNT|...|UNKNOWN",
                  "timeRange": "today|yesterday|this_week|last_week|...|null",
                  "customStart": "2024-01-01T00:00:00|null",
                  "customEnd": "2024-01-31T23:59:59|null",
                  "limit": 10,
                  "sortBy": "created_at|updated_at|count|null",
                  "sortOrder": "asc|desc",
                  "filters": {"key": "value"},
                  "clarification": "如果有歧义需要澄清，写在这里，否则写 null"
                }
                
                重要：只返回 JSON，不返回其他任何文字！
                """;
            
            String intentJson = dynamicAiService.chat(intentPrompt, "识别意图");
            log.debug("[AI意图识别] 原始响应: {}", intentJson);
            
            // 解析 JSON 响应
            QueryIntentParams params = objectMapper.readValue(intentJson, QueryIntentParams.class);
            if (params.getIntent() == null || params.getIntent().isEmpty()) {
                params.setIntent("UNKNOWN");
            }
            
            params.setOriginalQuestion(question);
            log.info("[AI意图识别] 完成 - intent={}, timeRange={}, limit={}", 
                params.getIntent(), params.getTimeRange(), params.getLimit());
            
            return params;
            
        } catch (Exception e) {
            log.error("[AI意图识别] 解析失败，默认为 UNKNOWN", e);
            QueryIntentParams result = new QueryIntentParams();
            result.setIntent("UNKNOWN");
            result.setOriginalQuestion(question);
            return result;
        }
    }
    
    /**
     * 生成闲聊回复
     * 使用友好、轻量级的 AI Prompt
     */
    private String generateChitchatResponse(String userMessage, List<ChatMessageDTO> history) {
        try {
            // 构建对话历史上下文
            String historyContext = history.stream()
                .map(m -> {
                    String role = m.getRole().equals("user") ? "用户" : "助手";
                    return role + ": " + m.getContent();
                })
                .collect(Collectors.joining("\n"));
            
            String chitchatSystemPrompt = """
                你是一个友好、简洁的智能助手，名字叫"小智"。
                
                角色定位：
                - 一个时间管理系统的 AI 助手
                - 可以帮助用户查询系统数据和统计信息
                - 也可以进行简单的日常闲聊和问答
                
                对话风格：
                - 自然、口语化，避免生硬的机器人风格
                - 简洁，通常控制在 1-3 句话以内
                - 友好、温暖，表现出个性
                - 当用户闲聊时，不要提及数据库或系统细节
                - 在适当的时候可以建议用户查询相关数据
                
                """ + (historyContext.isEmpty() ? "" : "对话历史：\n" + historyContext + "\n");
            
            String fullPrompt = "用户: " + userMessage;
            
            String response = dynamicAiService.chat(chitchatSystemPrompt, fullPrompt);
            log.debug("[闲聊回复] 生成成功: {}", response);
            
            return response;
            
        } catch (Exception e) {
            log.error("[闲聊回复] 生成失败", e);
            // 降级返回默认闲聊回复
            return "你好呀！我是小智，你的时间管理小助手。有什么我可以帮助的吗？";
        }
    }
    
    /** 
     * （原有方法，保持不变）
     * 处理自然语言查询 - 兼容无 sessionId 的旧版本
     */
    public AdminQueryResponse handleNaturalLanguageQuery(String question) {
        // 使用 UUID 生成临时会话ID
        String tempSessionId = java.util.UUID.randomUUID().toString();
        return handleNaturalLanguageQuery(question, tempSessionId);
    }
    
    /**
     * 执行参数化数据查询 (✨ 新的智能查询引擎)
     * 根据 AI 识别的意图和参数进行动态查询
     * 
     * 支持：
     * - 统计查询（NEW_USER_COUNT, LOGIN_FAIL_SUMMARY 等）
     * - 列表查询（USER_LIST, TASK_LIST 等）
     * - 时间范围过滤（today, yesterday, this_week, this_month 等）
     * - 排序和分页
     */
    private String executeQuery(QueryIntentParams params) {
        try {
            String intent = params.getIntent();
            LocalDateTime startTime = parseTimeRange(params.getTimeRange(), true);
            LocalDateTime endTime = parseTimeRange(params.getTimeRange(), false);
            Integer limit = params.getLimit() != null ? params.getLimit() : 10;
            
            log.info("[数据查询] intent={}, timeRange={}, limit={}", intent, params.getTimeRange(), limit);
            
            switch (intent) {
                // 📊 统计类查询
                case "NEW_USER_COUNT":
                    return executeNewUserCountQuery(startTime, endTime);
                    
                case "LOGIN_FAIL_SUMMARY":
                    return executeLoginFailQuery(startTime, endTime);
                    
                case "ACTIVE_USER_COUNT":
                    return executeActiveUserQuery(startTime, endTime);
                    
                // 👥 列表类查询（新增！）
                case "USER_LIST":
                    return executeUserListQuery(startTime, endTime, params, limit);
                    
                case "TASK_LIST":
                    return executeTaskListQuery(startTime, endTime, params, limit);
                    
                case "USER_DETAIL":
                    return executeUserDetailQuery(params);
                    
                // 📈 对比/趋势查询
                case "COMPARISON":
                    return executeComparisonQuery(params);
                    
                case "TREND_ANALYSIS":
                    return executeTrendQuery(params);
                    
                // 默认
                default:
                    return "未能识别查询类型：" + intent;
            }
            
        } catch (Exception e) {
            log.error("[数据查询] 执行失败", e);
            return "查询执行错误：" + e.getMessage();
        }
    }
    
    /**
     * 查询新增用户数
     */
    private String executeNewUserCountQuery(LocalDateTime startTime, LocalDateTime endTime) {
        Long count = userMapper.selectCount(
            new QueryWrapper<User>()
                .ge("created_at", startTime)
                .le("created_at", endTime)
        );
        return String.format("新增用户数：%d", count);
    }
    
    /**
     * 查询登录失败统计
     */
    private String executeLoginFailQuery(LocalDateTime startTime, LocalDateTime endTime) {
        Long failCount = operationLogMapper.selectCount(
            new QueryWrapper<OperationLog>()
                .eq("action", "login_failed")
                .ge("created_at", startTime)
                .le("created_at", endTime)
        );
        return String.format("登录失败次数：%d", failCount);
    }
    
    /**
     * 查询活跃用户
     */
    private String executeActiveUserQuery(LocalDateTime startTime, LocalDateTime endTime) {
        Long activeCount = operationLogMapper.selectCount(
            new QueryWrapper<OperationLog>()
                .eq("action", "login_success")
                .ge("created_at", startTime)
                .le("created_at", endTime)
        );
        return String.format("活跃用户数（登录）：%d", activeCount);
    }
    
    /**
     * 查询用户列表 (✨ 新增！)
     */
    private String executeUserListQuery(LocalDateTime startTime, LocalDateTime endTime, 
                                        QueryIntentParams params, Integer limit) {
        java.util.List<User> users = userMapper.selectList(
            new QueryWrapper<User>()
                .ge("created_at", startTime)
                .le("created_at", endTime)
                .orderByDesc("created_at")
                .last("LIMIT " + limit)
        );
        
        if (users == null || users.isEmpty()) {
            return "未找到用户数据";
        }
        
        // 格式化用户列表
        StringBuilder sb = new StringBuilder("用户列表：");
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            sb.append("\n").append(i + 1).append(". ")
              .append(user.getUsername() != null ? user.getUsername() : "未命名用户")
              .append(" (ID:").append(user.getId()).append(")");
        }
        return sb.toString();
    }
    
    /**
     * 查询任务列表 (✨ 新增！)
     */
    private String executeTaskListQuery(LocalDateTime startTime, LocalDateTime endTime,
                                        QueryIntentParams params, Integer limit) {
        // 如果有 Task 表和 Mapper，实现类似的逻辑
        // 这里暂时返回提示信息
        return "任务列表查询：共有多个任务待处理";
    }
    
    /**
     * 查询具体用户信息 (✨ 新增！)
     */
    private String executeUserDetailQuery(QueryIntentParams params) {
        // 从 filters 中获取用户名或 ID
        Object userIdentifier = params.getFilters().get("username");
        if (userIdentifier == null) {
            userIdentifier = params.getFilters().get("user_id");
        }
        
        if (userIdentifier != null) {
            // 查询具体用户
            java.util.List<User> users = userMapper.selectList(
                new QueryWrapper<User>()
                    .eq(userIdentifier instanceof Number ? "id" : "username", userIdentifier)
                    .last("LIMIT 1")
            );
            
            if (!users.isEmpty()) {
                User user = users.get(0);
                return String.format("用户 %s 的详情：ID=%d, 邮箱=%s, 角色=%s",
                    user.getUsername(), user.getId(), 
                    user.getEmail(), user.getRole());
            }
        }
        
        return "未找到指定用户";
    }
    
    /**
     * 执行数据对比查询 (✨ 新增！)
     * 例："昨天的新增用户" → 对比"今天的新增用户"
     */
    private String executeComparisonQuery(QueryIntentParams params) {
        // 计算两个时间段的结果并对比
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = todayStart;
        
        Long todayCount = userMapper.selectCount(
            new QueryWrapper<User>()
                .ge("created_at", todayStart)
                .le("created_at", todayEnd)
        );
        
        Long yesterdayCount = userMapper.selectCount(
            new QueryWrapper<User>()
                .ge("created_at", yesterdayStart)
                .le("created_at", yesterdayEnd)
        );
        
        long diff = todayCount - yesterdayCount;
        String trend = diff > 0 ? "增长" : (diff < 0 ? "下降" : "持平");
        
        return String.format("用户增长对比：今天 %d 人，昨天 %d 人，%s 了 %d 人",
            todayCount, yesterdayCount, trend, Math.abs(diff));
    }
    
    /**
     * 执行趋势分析 (✨ 新增！)
     */
    private String executeTrendQuery(QueryIntentParams params) {
        // 分别查询最近 7 天的趋势
        StringBuilder sb = new StringBuilder("最近 7 天用户新增趋势：");
        
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i)
                .withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = dayStart.withHour(23).withMinute(59).withSecond(59);
            
            Long count = userMapper.selectCount(
                new QueryWrapper<User>()
                    .ge("created_at", dayStart)
                    .le("created_at", dayEnd)
            );
            
            sb.append("\n").append(i == 0 ? "今天" : i + "天前")
              .append(": ").append(count).append(" 人");
        }
        
        return sb.toString();
    }
    
    /**
     * 根据 timeRange 参数解析时间范围
     * 
     * @param timeRange 时间范围字符串（如 "today", "yesterday", "this_week" 等）
     * @param isStart 是否获取开始时间（true）或结束时间（false）
     * @return LocalDateTime 对应的时间
     */
    private LocalDateTime parseTimeRange(String timeRange, boolean isStart) {
        LocalDateTime now = LocalDateTime.now();
        
        if (timeRange == null || timeRange.isEmpty() || "null".equals(timeRange)) {
            // 默认：今天
            return isStart ? now.withHour(0).withMinute(0).withSecond(0) 
                          : now.withHour(23).withMinute(59).withSecond(59);
        }
        
        switch (timeRange.toLowerCase()) {
            case "today":
                return isStart ? now.withHour(0).withMinute(0).withSecond(0)
                              : now.withHour(23).withMinute(59).withSecond(59);
                
            case "yesterday":
                LocalDateTime yesterday = now.minusDays(1);
                return isStart ? yesterday.withHour(0).withMinute(0).withSecond(0)
                              : yesterday.withHour(23).withMinute(59).withSecond(59);
                
            case "this_week":
                LocalDateTime weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1)
                    .withHour(0).withMinute(0).withSecond(0);
                LocalDateTime weekEnd = weekStart.plusDays(6)
                    .withHour(23).withMinute(59).withSecond(59);
                return isStart ? weekStart : weekEnd;
                
            case "last_week":
                LocalDateTime lastWeekStart = now.minusDays(now.getDayOfWeek().getValue() + 6)
                    .withHour(0).withMinute(0).withSecond(0);
                LocalDateTime lastWeekEnd = lastWeekStart.plusDays(6)
                    .withHour(23).withMinute(59).withSecond(59);
                return isStart ? lastWeekStart : lastWeekEnd;
                
            case "this_month":
                LocalDateTime monthStart = now.withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0);
                LocalDateTime monthEnd = now.withDayOfMonth(now.getMonth().length(now.toLocalDate().isLeapYear()))
                    .withHour(23).withMinute(59).withSecond(59);
                return isStart ? monthStart : monthEnd;
                
            case "last_month":
                LocalDateTime lastMonthStart = now.minusMonths(1).withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0);
                LocalDateTime lastMonthEnd = now.withDayOfMonth(1).minusDays(1)
                    .withHour(23).withMinute(59).withSecond(59);
                return isStart ? lastMonthStart : lastMonthEnd;
                
            default:
                // 处理 "last_Xh" 格式（如 "last_24h", "last_1h"）
                if (timeRange.startsWith("last_") && timeRange.endsWith("h")) {
                    String hourStr = timeRange.substring(5, timeRange.length() - 1);
                    try {
                        int hours = Integer.parseInt(hourStr);
                        return isStart ? now.minusHours(hours) : now;
                    } catch (NumberFormatException e) {
                        log.warn("[时间解析] 无法解析: {}", timeRange);
                    }
                }
                
                // 默认返回今天
                return isStart ? now.withHour(0).withMinute(0).withSecond(0)
                              : now.withHour(23).withMinute(59).withSecond(59);
        }
    }
    
    /**
     * 获取未处理的预警列表
     */
    public List<AiAlert> getUnhandledAlerts() {
        return aiAlertMapper.selectList(
            new QueryWrapper<AiAlert>()
                .eq("is_handled", 0)
                .orderByDesc("created_at")
                .last("LIMIT 20")
        );
    }
    
    /**
     * 标记预警已处理
     */
    public void markAlertHandled(Long alertId, Long handlerId, String note) {
        AiAlert alert = new AiAlert();
        alert.setId(alertId);
        alert.setIsHandled(1);
        alert.setHandlerId(handlerId);
        alert.setHandleNote(note);
        alert.setHandledAt(LocalDateTime.now());
        
        aiAlertMapper.updateById(alert);
        log.info("[管理员AI] 预警已处理: alertId={}", alertId);
    }
    
    /**
     * 扫描操作日志并生成预警
     */
    public void scanLogsAndGenerateAlerts() {
        log.info("[AI预警] 开始扫描操作日志");
        
        // 查询最近10分钟的操作日志
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        List<OperationLog> recentLogs = operationLogMapper.selectList(
            new QueryWrapper<OperationLog>()
                .ge("created_at", tenMinutesAgo)
        );
        
        // 分析异常登录
        analyzeAbnormalLogin(recentLogs);
        
        // 分析删除操作
        analyzeDeleteOperations(recentLogs);
        
        log.info("[AI预警] 日志扫描完成");
    }
    
    /**
     * 分析异常登录
     */
    private void analyzeAbnormalLogin(List<OperationLog> logs) {
        // 统计失败登录
        long failedCount = logs.stream()
            .filter(l -> "login_failed".equals(l.getAction()))
            .count();
        
        if (failedCount > 5) {
            String description = "检测到最近10分钟内有 " + failedCount + 
                " 次登录失败，可能存在暴力破解风险。";
            
            String suggestion = dynamicAiService.chat(
                "这是一个安全威胁，给出一条应急建议（不超过30字）：",
                description
            );
            
            createAlert("ABNORMAL_LOGIN", "HIGH", "异常登录检测", description, suggestion);
        }
    }
    
    /**
     * 分析删除操作
     */
    private void analyzeDeleteOperations(List<OperationLog> logs) {
        long deleteCount = logs.stream()
            .filter(l -> l.getAction() != null && l.getAction().contains("delete"))
            .count();
        
        if (deleteCount > 10) {
            String description = "检测到10分钟内有 " + deleteCount + 
                " 条删除操作，可能存在数据安全风险。";
            
            String suggestion = dynamicAiService.chat(
                "这是一个可能的数据泄露威胁，给出安全建议（不超过30字）：",
                description
            );
            
            createAlert("BULK_DELETE", "HIGH", "批量删除预警", description, suggestion);
        }
    }
    
    /**
     * 创建预警并推送
     */
    private void createAlert(String type, String severity, String title, 
                             String description, String suggestion) {
        AiAlert alert = new AiAlert();
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setDescription(description);
        alert.setSuggestion(suggestion);
        alert.setIsHandled(0);
        alert.setCreatedAt(LocalDateTime.now());
        
        aiAlertMapper.insert(alert);
        
        // WebSocket 推送到管理员（如果可用）
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/ai-alerts", alert);
            } catch (Exception e) {
                log.warn("[AI预警] WebSocket推送失败", e);
            }
        }
        
        log.warn("[AI预警] {} - {}", type, title);
    }
    
    /**
     * 管理员查询响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminQueryResponse {
        private String answer;      // AI 生成的自然语言答案
        private String rawData;     // 原始数据（用于调试）
    }
}
