package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.entity.UserMessage;
import com.timemanager.service.UserMessageService;
import com.timemanager.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user/messages")
public class UserMessageController {

    @Autowired
    private UserMessageService userMessageService;

    @GetMapping
    public Result<Map<String, Object>> listMessages(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = UserUtil.getCurrentUserId();
            if (userId == null) return Result.error(401, "未登录");
            int offset = (page - 1) * size;
            List<UserMessage> list = userMessageService.listForUser(userId, offset, size);
            int total = userMessageService.countForUser(userId);
            Map<String, Object> resp = new HashMap<>();
            resp.put("content", list);
            resp.put("total", total);
            resp.put("pageNum", page);
            resp.put("pageSize", size);
            resp.put("pages", (total + size - 1) / size);
            return Result.success(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取消息失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/read")
    public Result<String> markRead(@PathVariable Long id) {
        try {
            Long userId = UserUtil.getCurrentUserId();
            if (userId == null) return Result.error(401, "未登录");
            boolean ok = userMessageService.markRead(userId, id);
            if (ok) return Result.success("已标记为已读");
            return Result.error(404, "消息未找到或操作失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "标记已读失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteMessage(@PathVariable Long id) {
        try {
            Long userId = UserUtil.getCurrentUserId();
            if (userId == null) return Result.error(401, "未登录");
            boolean ok = userMessageService.softDelete(userId, id);
            if (ok) return Result.success("已删除");
            return Result.error(404, "消息未找到或删除失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }
}
