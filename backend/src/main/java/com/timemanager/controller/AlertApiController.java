package com.timemanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timemanager.common.result.Result;
import com.timemanager.entity.AlertLog;
import com.timemanager.entity.User;
import com.timemanager.mapper.AlertLogMapper;
import com.timemanager.mapper.UserMapper;
import com.timemanager.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertApiController {

    @Autowired
    private AlertLogMapper alertLogMapper;

    @Autowired
    private UserMapper userMapper;

    @GetMapping
    public Result<Map<String, Object>> getMyAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = UserUtil.getCurrentUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        String username = UserUtil.getCurrentUsername();

        try {
            QueryWrapper<AlertLog> wrapper = new QueryWrapper<>();
            wrapper.eq("related_username", username);
            wrapper.eq("is_deleted", 0);
            wrapper.orderByDesc("created_at");

            long total = alertLogMapper.selectCount(wrapper);
            int offset = (page - 1) * size;
            wrapper.last("LIMIT " + offset + "," + size);
            List<AlertLog> alerts = alertLogMapper.selectList(wrapper);

            return Result.success(Map.of(
                    "content", alerts,
                    "total", total,
                    "pages", (total + size - 1) / size,
                    "pageNum", page,
                    "pageSize", size
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "获取告警失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id) {
        Long userId = UserUtil.getCurrentUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        try {
            AlertLog alert = alertLogMapper.selectById(id);
            if (alert == null) return Result.error(404, "预警未找到");
            String username = UserUtil.getCurrentUsername();
            // 仅允许归属用户或管理员操作
            if (!username.equals(alert.getRelatedUsername())) {
                User u = userMapper.selectById(userId);
                if (u == null || u.getRole() == null || !u.getRole().equalsIgnoreCase("admin")) {
                    return Result.error(403, "Forbidden");
                }
            }

            AlertLog update = new AlertLog();
            update.setId(id);
            update.setStatus(1); // 已读
            update.setHandledAt(LocalDateTime.now());
            alertLogMapper.updateById(update);
            return Result.success("已标记为已读");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> ignoreAlert(@PathVariable Long id) {
        Long userId = UserUtil.getCurrentUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        try {
            AlertLog alert = alertLogMapper.selectById(id);
            if (alert == null) return Result.error(404, "预警未找到");
            String username = UserUtil.getCurrentUsername();
            if (!username.equals(alert.getRelatedUsername())) {
                User u = userMapper.selectById(userId);
                if (u == null || u.getRole() == null || !u.getRole().equalsIgnoreCase("admin")) {
                    return Result.error(403, "Forbidden");
                }
            }
            // 标记为已读以表示忽略
            AlertLog update = new AlertLog();
            update.setId(id);
            update.setStatus(1);
            update.setHandledAt(LocalDateTime.now());
            alertLogMapper.updateById(update);
            return Result.success("已忽略");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "忽略失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-read")
    public Result<String> batchRead(@RequestBody List<Long> ids) {
        Long userId = UserUtil.getCurrentUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        try {
            for (Long id : ids) {
                AlertLog a = alertLogMapper.selectById(id);
                if (a == null) continue;
                String username = UserUtil.getCurrentUsername();
                if (!username.equals(a.getRelatedUsername())) {
                    User u = userMapper.selectById(userId);
                    if (u == null || u.getRole() == null || !u.getRole().equalsIgnoreCase("admin")) {
                        continue; // 跳过非本用户项
                    }
                }
                AlertLog update = new AlertLog();
                update.setId(id);
                update.setStatus(1);
                update.setHandledAt(LocalDateTime.now());
                alertLogMapper.updateById(update);
            }
            return Result.success("批量已读");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "批量更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/all")
    public Result<String> clearAll() {
        Long userId = UserUtil.getCurrentUserId();
        if (userId == null) return Result.error(401, "Unauthorized");
        String username = UserUtil.getCurrentUsername();
        try {
            QueryWrapper<AlertLog> wrapper = new QueryWrapper<>();
            wrapper.eq("related_username", username);
            List<AlertLog> list = alertLogMapper.selectList(wrapper);
            for (AlertLog a : list) {
                AlertLog update = new AlertLog();
                update.setId(a.getId());
                update.setStatus(1);
                update.setHandledAt(LocalDateTime.now());
                alertLogMapper.updateById(update);
            }
            return Result.success("已清空");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "清空失败: " + e.getMessage());
        }
    }

}
