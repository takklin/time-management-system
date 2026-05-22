package com.timemanager.controller.admin;

import com.timemanager.common.result.Result;
import com.timemanager.entity.UserMessage;
import com.timemanager.service.UserMessageService;
import com.timemanager.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/messages")
public class AdminMessageController {

    @Autowired
    private UserMessageService userMessageService;

    /**
     * 管理员向指定用户发送消息
     * POST /api/v1/admin/messages/send
     * body: { userId: number, title: string, content: string }
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> body) {
        try {
            Long userId = null;
            Object uid = body.get("userId");
            if (uid instanceof Number) userId = ((Number) uid).longValue();
            else if (uid instanceof String) {
                try { userId = Long.valueOf((String) uid); } catch (Exception ignored) {}
            }
            String title = (String) body.getOrDefault("title", "管理员消息");
            String content = (String) body.getOrDefault("content", "");
            if (userId == null) return Result.error(400, "缺少 userId 参数");

            Long fromAdminId = UserUtil.getCurrentUserId();
            UserMessage m = userMessageService.sendFromAdmin(fromAdminId, userId, title, content);
            if (m == null) return Result.error(500, "发送失败");
            Map<String, Object> resp = new HashMap<>();
            resp.put("id", m.getId());
            resp.put("createdAt", m.getCreatedAt());
            return Result.success(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "发送消息失败: " + e.getMessage());
        }
    }
}
