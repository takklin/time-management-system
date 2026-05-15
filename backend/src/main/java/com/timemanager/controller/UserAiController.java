package com.timemanager.controller;

import com.timemanager.ai.service.UserAiService;
import java.util.Map;
import java.util.List;
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
    // NOTE: parse-task endpoint removed to avoid frontend sending/receiving structured parse results
    // (历史原因：前端将结构化建议作为会话历史发送给模型，导致模型生成重复/错误的建议)。
    
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

    /**
     * 增强对话：接收前端传入的结构化上下文（例如 todo 列表摘要），后端将上下文拼接到 prompt 中并调用 LLM
     */
    @PostMapping("/promote")
    public Result<Object> promote(@RequestBody PromoteRequest request, HttpServletRequest httpRequest) {
        // 只允许普通用户访问
        Object roleObj = httpRequest.getAttribute("role");
        String role = roleObj != null ? roleObj.toString() : null;
        if (role == null || role.equalsIgnoreCase("ADMIN")) {
            return Result.error(403, "仅普通用户可用该接口");
        }
        Long userId = UserUtil.getCurrentUserId();
        Object resp = userAiService.promote(userId, request.getMessages(), request.getQuestion(), request.getContext(), request.getModel());
        return Result.success(resp);
    }
    
    // 请求对象
    @Data
    public static class ChatRequest {
        private String message;
        private String model;  // 可选：指定 AI 模型 (e.g., "chatgpt3.5", "deepseek")
    }
    
    // ParseTaskRequest removed — parsing now handled internally where needed, avoid exposing parse-task API
    
    @Data
    public static class TaskSuggestionRequest {
        private String mainTask;
    }

    @Data
    public static class PromoteRequest {
        private String question;
        private Map<String, Object> context;
        private String model;
        private List<Map<String, Object>> messages;
    }
}
