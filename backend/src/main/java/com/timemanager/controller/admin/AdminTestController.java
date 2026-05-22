package com.timemanager.controller.admin;

import com.timemanager.common.result.Result;
import com.timemanager.service.AlertPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/test")
public class AdminTestController {

    @Autowired
    private AlertPushService alertPushService;

    @PostMapping("/push-alert")
    public Result<Boolean> pushAlert(@RequestBody(required = false) Map<String, Object> body) {
        try {
            if (body == null) body = new HashMap<>();
            String username = (String) body.getOrDefault("username", "admin");
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", body.getOrDefault("type", "TEST"));
            payload.put("title", body.getOrDefault("title", "测试告警"));
            payload.put("message", body.getOrDefault("message", "这是一条测试告警"));
            payload.put("severity", body.getOrDefault("severity", "medium"));
            payload.put("createdAt", java.time.LocalDateTime.now().toString());
            alertPushService.sendToUser(username, payload);
            return Result.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, e.getMessage());
        }
    }
}
