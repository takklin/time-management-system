package com.timemanager.controller;

import com.timemanager.ai.service.AdminAiService;
import com.timemanager.entity.AiAlert;
import com.timemanager.common.result.Result;
import com.timemanager.util.UserUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 管理员 AI 控制器
 * 提供管理员端 AI 功能的 API 端点
 */
@RestController
@RequestMapping("/api/v1/admin/ai")
public class AdminAiController {
    
    @Autowired
    private AdminAiService adminAiService;
    
    /**
     * 自然语言数据查询（支持会话历史）
     * POST /api/v1/admin/ai/query
     * 
     * 请求体：
     * {
     *   "question": "最近一小时登录失败多少次",
     *   "sessionId": "uuid-xxx"  // 可选，不提供则自动生成
     * }
     * 
     * 响应：
     * {
     *   "answer": "最近一小时登录失败0次",
     *   "rawData": "最近一小时失败登录次数: 0"
     * }
     */
    @PostMapping("/query")
    public Result<AdminAiService.AdminQueryResponse> query(@RequestBody QueryRequest request, HttpServletRequest httpRequest) {
        // 只允许管理员角色访问
        Object roleObj = httpRequest.getAttribute("role");
        String role = roleObj != null ? roleObj.toString() : null;
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            return Result.error(403, "仅管理员可用该接口");
        }
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = java.util.UUID.randomUUID().toString();
            request.setSessionId(sessionId);
        }
        AdminAiService.AdminQueryResponse response = 
            adminAiService.handleNaturalLanguageQuery(request.getQuestion(), sessionId);
        return Result.success(response);
    }
    
    /**
     * 获取未处理的预警列表
     * GET /api/v1/admin/ai/alerts/unhandled
     */
    @GetMapping("/alerts/unhandled")
    public Result<List<AiAlert>> getUnhandledAlerts() {
        List<AiAlert> alerts = adminAiService.getUnhandledAlerts();
        return Result.success(alerts);
    }
    
    /**
     * 标记预警已处理
     * PUT /api/v1/admin/ai/alert/{id}/handle
     */
    @PutMapping("/alert/{id}/handle")
    public Result<Void> handleAlert(@PathVariable Long id, @RequestBody HandleAlertRequest request) {
        Long adminId = UserUtil.getCurrentUserId();
        adminAiService.markAlertHandled(id, adminId, request.getNote());
        return Result.success();
    }
    
    /**
     * 手动触发日志扫描和预警生成
     * POST /api/v1/admin/ai/scan-logs
     */
    @PostMapping("/scan-logs")
    public Result<Void> triggerLogScan() {
        adminAiService.scanLogsAndGenerateAlerts();
        return Result.success();
    }
    
    // 请求对象
    @Data
    public static class QueryRequest {
        private String question;      // 用户问题（必需）
        private String sessionId;     // 会话ID（可选，用于维持对话上下文）
    }
    
    @Data
    public static class HandleAlertRequest {
        private String note;
    }
}
