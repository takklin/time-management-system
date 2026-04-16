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
