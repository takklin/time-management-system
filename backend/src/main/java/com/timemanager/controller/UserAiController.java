package com.timemanager.controller;

import com.timemanager.ai.service.UserAiService;
import com.timemanager.common.result.Result;
import com.timemanager.util.UserUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户 AI 控制器
 * 提供用户端 AI 功能的 API 端点
 */
@RestController
@RequestMapping("/api/v1/user/ai")
public class UserAiController {
    
    @Autowired
    private UserAiService userAiService;
    
    /**
     * 基础对话
     * POST /api/v1/user/ai/chat
     * 支持指定 AI 模型（选填，不指定则使用当前激活的）
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        // 只允许普通用户访问
        Object roleObj = httpRequest.getAttribute("role");
        String role = roleObj != null ? roleObj.toString() : null;
        if (role == null || role.equalsIgnoreCase("ADMIN")) {
            return Result.error(403, "仅普通用户可用该接口");
        }
        Long userId = UserUtil.getCurrentUserId();
        // 如果指定了 model，则使用指定的模型；否则使用默认激活的模型
        String response = userAiService.chat(userId, request.getMessage(), request.getModel());
        return Result.success(response);
    }
    
    /**
     * 自然语言解析任务
     * POST /api/v1/user/ai/parse-task
     */
    @PostMapping("/parse-task")
    public Result<UserAiService.TaskParseResult> parseTask(@RequestBody ParseTaskRequest request) {
        UserAiService.TaskParseResult result = 
            userAiService.parseTaskFromNaturalLanguage(request.getMessage());
        return Result.success(result);
    }
    
    /**
     * 生成今日任务总结
     * GET /api/v1/user/ai/summary/today
     */
    @GetMapping("/summary/today")
    public Result<String> getTodaySummary() {
        Long userId = UserUtil.getCurrentUserId();
        String summary = userAiService.generateDailySummary(userId);
        return Result.success(summary);
    }
    
    /**
     * 生成任务分解建议
     * POST /api/v1/user/ai/task-suggestions
     */
    @PostMapping("/task-suggestions")
    public Result<String> getTaskSuggestions(@RequestBody TaskSuggestionRequest request) {
        String suggestions = userAiService.generateTaskSuggestions(request.getMainTask());
        return Result.success(suggestions);
    }
    
    // 请求对象
    @Data
    public static class ChatRequest {
        private String message;
        private String model;  // 可选：指定 AI 模型 (e.g., "chatgpt3.5", "deepseek")
    }
    
    @Data
    public static class ParseTaskRequest {
        private String message;
    }
    
    @Data
    public static class TaskSuggestionRequest {
        private String mainTask;
    }
}
