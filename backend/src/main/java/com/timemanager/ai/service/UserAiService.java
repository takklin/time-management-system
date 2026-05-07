package com.timemanager.ai.service;

import com.timemanager.mapper.TaskMapper;
import com.timemanager.entity.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 用户 AI 服务
 * 提供用户端的 AI 功能：
 * - 自然语言创建任务
 * - 任务智能解析
 * - 今日总结生成
 * - 通用对话
 */
@Slf4j
@Service
public class UserAiService {
    
    @Autowired
    private DynamicAiService dynamicAiService;
    
    @Autowired
    private TaskMapper taskMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 基础对话 - 用户端 AI 智能助手
     * 针对普通用户的任务管理场景
     * @param userId 用户ID
     * @param message 用户消息
     * @param model 可选的模型提供商 (e.g., "chatgpt3.5", "deepseek")，为空时使用当前激活的
     */
    public String chat(Long userId, String message, String model) {
        // 智能判断用户意图
        String intent = detectUserIntent(message);
        
        String systemPrompt = """
            你是一个超级聪慧的个人时间管理助手，专门帮助用户高效管理任务和时间。
            
            【你的职责】：
            1. 📝 帮助创建任务：当用户说"我要...", "帮我...", "创建"时，主动询问任务详情
            2. 📊 查询任务：当用户问"我的任务", "完成了多少", "今天的任务"时，描述可能的查询方式
            3. 💡 效率建议：基于任务数量、完成率给出时间管理建议
            4. 🎯 激励鼓励：当用户表达困难或疲劳时，给予积极鼓励
            5. ⏰ 时间规划：帮助用户制定合理的任务计划
            
            【交互风格】：
            - ✅ 回答简洁清晰，一般不超过100字
            - ✅ 语气友善热情，表现出真正的关心
            - ✅ 如果用户提到具体任务，主动给出建议
            - ✅ 避免重复相同的欢迎语，每次都个性化回复
            - ✅ 使用表情符号增加交互感
            - ✅ 如果理解不了，主动询问"你是想..."
            
            【检测意图】：""" + intent + """
            
            用户ID: """ + userId + """
            当前时间: """ + LocalDateTime.now() + """
            
            【示例回复】：
            用户："我今天很累"
            你的回复："☕ 累的时候要好好休息！不过我发现你很坚持呢。要不要把今天的任务列出来，看看什么可以先完成，什么可以后延？这样压力会小一些。"
            
            用户："帮我创建一个任务"
            你的回复："📝 好的！告诉我任务是什么呢？比如：任务的名称、大概要花多长时间、什么时候想完成它？"
            """;
        
        log.info("[用户AI] 检测到意图: {}, 用户消息: {}, 指定模型: {}", intent, message, model);
        
        // 如果指定了模型则使用指定模型，否则使用默认激活的
        return dynamicAiService.chat(systemPrompt, message, model);
    }

    /**
     * 增强对话：接收前端传入的上下文（结构化）以及会话历史，将其格式化并插入到用户消息中，再调用 DynamicAiService
     * 要求 LLM 尽量返回纯 JSON 格式，格式示例：
     * {
     *   "type": "answer" | "create_task" | "create_schedule",
     *   "content": "可读文本回复",
     *   "data": { ... 结构化数据 ... }
     * }
     * 如果 LLM 返回无法解析为 JSON，则会回退为 { type: 'answer', content: 原始回复 }
     *
     * @param userId 用户ID
     * @param messages 会话历史（按时间顺序）
     * @param question 用户的原始问题
     * @param context 前端传入的上下文（可为 null）
     * @param model 可选的模型提供商
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<String, Object> promote(Long userId, List<Map<String, Object>> messages, String question, Map<String, Object> context, String model) {
        String intent = detectUserIntent(question);

        String systemPrompt = """
            你是一个超级聪慧的个人时间管理助手，专门帮助用户高效管理任务和时间。

            输出格式要求：
            - 必须返回有效的 JSON（不要包含其它多余的文字），JSON 结构如下：
            {
              "type": "answer" | "create_task" | "create_schedule",
              "content": "对用户的可读回复（简洁）",
              "data": { ... 可选的结构化字段 ... }
            }

            当 "type" 为 "create_task" 时，"data" 应包含以下字段（若未知可置为 null）：
            - title: 任务标题
            - deadline: ISO 格式时间字符串或 null
            - startTime: ISO 格式时间字符串或 null
            - estimatedMinutes: 预估分钟数或 null
            - categoryName: 分类名或 null
            - description: 任务描述或空字符串

            当 "type" 为 "create_schedule" 时，"data" 应包含：
            - title, startTime, endTime, reminderTime(分钟), description

            如果无法满足创建操作，请返回 type 为 "answer" 并在 content 中给出建议。

            【交互风格】：保持友善并直接给出可执行的下一步。
            """ + "\n检测意图:" + intent + "\n用户ID:" + userId + "\n当前时间:" + LocalDateTime.now();

        StringBuilder userMsgBuilder = new StringBuilder();

        // 附加最近的会话历史（最多 recent 12 条）
        if (messages != null && !messages.isEmpty()) {
            int from = Math.max(0, messages.size() - 12);
            userMsgBuilder.append("=== 最近会话历史（最近优先） ===\n");
            for (int i = from; i < messages.size(); i++) {
                Map msg = messages.get(i);
                Object role = msg.get("role");
                Object content = msg.get("content");
                userMsgBuilder.append(role == null ? "?" : role.toString()).append(": ");
                userMsgBuilder.append(content == null ? "" : content.toString()).append("\n");
            }
            userMsgBuilder.append("=== 会话历史结束 ===\n\n");
        }

        userMsgBuilder.append("用户问题: ").append(question).append("\n\n");

        if (context != null && !context.isEmpty()) {
            userMsgBuilder.append("=== 前端传入的上下文开始 ===\n");

            Object highObj = context.get("high_priority_tasks");
            if (highObj instanceof List) {
                userMsgBuilder.append("高优任务:\n");
                for (Object item : (List) highObj) {
                    if (item instanceof Map) {
                        Map map = (Map) item;
                        Object title = map.get("title");
                        Object deadline = map.get("deadline");
                        Object est = map.get("estimatedMinutes");
                        userMsgBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                        if (deadline != null) userMsgBuilder.append(" 截止:").append(deadline.toString());
                        if (est != null) userMsgBuilder.append(" 预估:").append(est.toString()).append("分");
                        userMsgBuilder.append("\n");
                    }
                }
            }

            Object mediumObj = context.get("medium_priority_tasks");
            if (mediumObj instanceof List) {
                userMsgBuilder.append("中优任务:\n");
                for (Object item : (List) mediumObj) {
                    if (item instanceof Map) {
                        Map map = (Map) item;
                        Object title = map.get("title");
                        Object deadline = map.get("deadline");
                        Object est = map.get("estimatedMinutes");
                        userMsgBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                        if (deadline != null) userMsgBuilder.append(" 截止:").append(deadline.toString());
                        if (est != null) userMsgBuilder.append(" 预估:").append(est.toString()).append("分");
                        userMsgBuilder.append("\n");
                    }
                }
            }

            Object procObj = context.get("procrastinate_tasks");
            if (procObj instanceof List) {
                userMsgBuilder.append("可拖延鱼塘（低优）:\n");
                for (Object item : (List) procObj) {
                    if (item instanceof Map) {
                        Map map = (Map) item;
                        Object title = map.get("title");
                        Object deadline = map.get("deadline");
                        userMsgBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                        if (deadline != null) userMsgBuilder.append(" 截止:").append(deadline.toString());
                        userMsgBuilder.append("\n");
                    }
                }
            }

            Object completedObj = context.get("completed_tasks");
            if (completedObj instanceof List) {
                List completedList = (List) completedObj;
                if (!completedList.isEmpty()) {
                    userMsgBuilder.append("已完成任务（共 ").append(completedList.size()).append(" 项）:\n");
                    for (Object item : completedList) {
                        if (item instanceof Map) {
                            Map map = (Map) item;
                            Object title = map.get("title");
                            Object compAt = map.get("completedAt");
                            userMsgBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                            if (compAt != null) userMsgBuilder.append(" 完成时间:").append(compAt.toString());
                            userMsgBuilder.append("\n");
                        }
                    }
                }
            }

            Object countsObj = context.get("counts");
            if (countsObj instanceof Map) {
                Map counts = (Map) countsObj;
                userMsgBuilder.append(String.format("任务计数 - 高:%s 中:%s 低:%s 今日:%s\n",
                    counts.getOrDefault("high", 0), counts.getOrDefault("medium", 0), counts.getOrDefault("low", 0), counts.getOrDefault("today", 0)
                ));
            }

            Object overload = context.get("overload");
            if (overload != null) {
                userMsgBuilder.append("是否过载: ").append(overload.toString()).append("\n");
            }

            Object weekly = context.get("weekly_core_done");
            if (weekly != null) {
                userMsgBuilder.append("本周核心任务完成数: ").append(weekly.toString()).append("\n");
            }

            userMsgBuilder.append("=== 上下文结束 ===\n\n");
        }

        // 最终用户消息文本
        String userMessage = userMsgBuilder.toString();

        // 调用 DynamicAiService（支持指定 provider/model）
        String aiResp = dynamicAiService.chat(systemPrompt, userMessage, model);

        // 尝试解析为 JSON
        try {
            String jsonStr = extractJson(aiResp);
            Map parsed = objectMapper.readValue(jsonStr, Map.class);
            return parsed;
        } catch (Exception e) {
            log.warn("[用户AI] 无法解析为 JSON，返回原始文本。错误: {}", e.getMessage());
            Map<String, Object> fallback = new java.util.HashMap<>();
            fallback.put("type", "answer");
            fallback.put("content", aiResp);
            fallback.put("data", null);
            return fallback;
        }
    }
    
    /**
     * 基础对话 - 用户端 AI 智能助手（使用默认模型）
     * 针对普通用户的任务管理场景
     */
    public String chat(Long userId, String message) {
        return chat(userId, message, null);
    }
    
    /**
     * 检测用户意图（本地快速判断）
     */
    private String detectUserIntent(String message) {
        if (message.contains("累") || message.contains("困") || message.contains("累") || message.contains("难")) {
            return "USER_FEELING_TIRED - 用户表达困难/疲劳";
        } else if (message.contains("创建") || message.contains("新建") || message.contains("帮我") || message.contains("要") || message.contains("想")) {
            return "CREATE_TASK - 用户想创建新任务";
        } else if (message.contains("完成") || message.contains("多少") || message.contains("几个") || message.contains("查询") || message.contains("统计")) {
            return "QUERY_TASKS - 用户想查询任务信息";
        } else if (message.contains("建议") || message.contains("如何") || message.contains("怎样") || message.contains("方法")) {
            return "REQUEST_ADVICE - 用户请求建议";
        } else if (message.equalsIgnoreCase("你好") || message.equalsIgnoreCase("hi") || message.equalsIgnoreCase("hello")) {
            return "GREETING - 问候";
        } else {
            return "GENERAL_CHAT - 一般对话";
        }
    }
    
    /**
     * 自然语言解析为任务结构化数据
     * 例如: "明天下午3点开会" -> JSON 包含标题、时间、时长等
     */
    public TaskParseResult parseTaskFromNaturalLanguage(String input) {
        String systemPrompt = """
            你是一个任务解析专家。从用户的自然语言输入中提取任务信息。
            
            必须返回纯 JSON 格式（不要有其他文字）：
            {
                "title": "任务标题",
                "deadline": "YYYY-MM-DD HH:mm",
                "estimatedMinutes": 预估时长（分钟），
                "categoryName": "工作/学习/个人/其他"
            }
            
            规则：
            - 如果用户没有提供某些信息，则该字段置为 null
            - 标题必须清晰简洁（不超过50字）
            - 时间推断：如果用户说"明天"，推算为明天当前时间
            - 如果没有指定时间，deadline 为 null
            - 如果没有指定分类，categoryName 为 null
            
            只返回 JSON，不要有其他解释。
            """;
        
        try {
            String userPrompt = "用户输入：" + input;
            String aiResp = dynamicAiService.chat(systemPrompt, userPrompt);
            
            // 提取 JSON 部分
            String jsonStr = extractJson(aiResp);
            TaskParseResult result = objectMapper.readValue(jsonStr, TaskParseResult.class);
            
            log.info("[用户AI] 任务解析成功: {}", result.getTitle());
            return result;
            
        } catch (Exception e) {
            log.error("[用户AI] 任务解析失败", e);
            return new TaskParseResult();
        }
    }
    
    /**
     * 生成今日总结
     * 查询用户今日完成情况，让 AI 生成鼓励性总结
     */
    public String generateDailySummary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        // TODO: 实现数据库查询方法
        // 这里使用示例数据
        int completedCount = 5;
        long totalMinutes = 480;
        int createdCount = 8;
        
        String stats = String.format(
            "今日完成%d个任务，总耗时%d分钟，新建%d个任务。",
            completedCount, totalMinutes, createdCount
        );
        
        String systemPrompt = """
            你是一个鼓励型的效率助手。根据用户今日的任务完成情况，生成一段简短有趣的总结和鼓励。
            
            要求：
            - 不超过 80 字
            - 语气友善、充满正能量
            - 如果完成数量少，要给予鼓励
            - 如果完成数量多，要表示庆祝和认可
            
            只返回总结内容，不要前缀。
            """;
        
        return dynamicAiService.chat(systemPrompt, "数据：" + stats);
    }
    
    /**
     * 提示生成任务列表
     * 输入如："复习期末" -> 生成子任务建议列表
     */
    public String generateTaskSuggestions(String mainTask) {
        String systemPrompt = """
            你是一个任务分解专家。用户给出一个主任务，你需要分解成具体的子任务。
            
            返回格式：
            - 子任务1
            - 子任务2
            - 子任务3
            （通常 3-5 个为佳）
            
            每个子任务应该：
            - 具体、可执行
            - 相对独立
            - 有明确的完成标准
            """;
        
        return dynamicAiService.chat(systemPrompt, "主任务：" + mainTask);
    }
    
    /**
     * 从 AI 响应中提取 JSON 字符串
     */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
    
    /**
     * 任务解析结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskParseResult {
        private String title;                    // 任务标题
        private String deadline;                 // 截止时间 (YYYY-MM-DD HH:mm)
        private Integer estimatedMinutes;        // 预估时长（分钟）
        private String categoryName;             // 分类名称
    }
}
