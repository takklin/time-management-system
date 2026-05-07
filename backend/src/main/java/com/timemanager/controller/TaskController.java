package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.entity.Task;
import com.timemanager.service.TaskService;
import com.timemanager.mapper.UserMapper;
import com.timemanager.entity.User;
import com.timemanager.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OperationLogService operationLogService;

    private Long currentUserId() { return com.timemanager.util.UserUtil.getCurrentUserId(); }

    @GetMapping
    public Result<List<Task>> list(@RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) String priority,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) List<String> dateRange,
                                   @RequestParam(required = false) String startDeadline,
                                   @RequestParam(required = false) String endDeadline) {
        Long userId = currentUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }

        LocalDateTime start = null;
        LocalDateTime end = null;
        if (dateRange != null && dateRange.size() >= 2) {
            start = parseToStartOfDay(dateRange.get(0));
            end = parseToEndOfDay(dateRange.get(1));
        } else if (startDeadline != null && endDeadline != null) {
            start = parseToStartOfDay(startDeadline);
            end = parseToEndOfDay(endDeadline);
        }

        Boolean completed = null;
        if (status != null) {
            if ("completed".equalsIgnoreCase(status)) completed = true;
            else if ("active".equalsIgnoreCase(status)) completed = false;
        }

        List<Task> tasks = taskService.listWithFilters(userId, categoryId, priority, start, end, completed);
        return Result.success(tasks);
    }

    private LocalDateTime parseToStartOfDay(String s) {
        if (s == null) return null;
        try {
            LocalDate d = LocalDate.parse(s);
            return d.atStartOfDay();
        } catch (DateTimeParseException e) {
            try { return LocalDateTime.parse(s); } catch (DateTimeParseException ex) { return null; }
        }
    }

    private LocalDateTime parseToEndOfDay(String s) {
        if (s == null) return null;
        try {
            LocalDate d = LocalDate.parse(s);
            return d.atTime(23,59,59);
        } catch (DateTimeParseException e) {
            try { return LocalDateTime.parse(s); } catch (DateTimeParseException ex) { return null; }
        }
    }

    @GetMapping("/{id}")
    public Result<Task> get(@PathVariable Long id) {
        return Result.success(taskService.getById(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody Task task) {
        task.setUserId(currentUserId());
        taskService.create(task);
        // 记录操作日志：创建任务
        try {
            Long uid = currentUserId();
            String operator = "anonymous";
            if (uid != null) {
                User u = userMapper.selectById(uid);
                operator = u != null && u.getUsername() != null ? u.getUsername() : String.valueOf(uid);
            }
            String target = task.getTitle() != null ? "task:" + task.getTitle() : "task:" + (task.getId() != null ? task.getId() : "unknown");
            operationLogService.recordOperation(operator, "CREATE_TASK", target, "SUCCESS");
        } catch (Exception ignored) {}
        // 返回创建的实体（包含数据库生成的 id），便于前端使用真实 id
        return Result.success(task);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        // ensure user ownership
        task.setUserId(currentUserId());
        logger.info("[DEBUG] Received update request for task id={}, payload={}", id, task);

        // Log DB record before update for debugging
        try {
            Task before = taskService.getById(id);
            logger.info("[DEBUG] DB before update id={} -> {}", id, before);
        } catch (Exception e) {
            logger.warn("[DEBUG] Failed to fetch DB before update for id={}: {}", id, e.getMessage());
        }

        Task persisted = taskService.update(task);

        // If persisted is null, target not found -> return 404
        if (persisted == null) {
            logger.warn("[DEBUG] update failed: task id={} not found", id);
            return Result.error(404, "Task not found");
        }

        // Log DB record after update for debugging
        try {
            Long returnedId = persisted.getId();
            Task after = returnedId != null ? taskService.getById(returnedId) : null;
            logger.info("[DEBUG] DB after update id={} -> {}", returnedId, after);
        } catch (Exception e) {
            logger.warn("[DEBUG] Failed to fetch DB after update for id={}: {}", id, e.getMessage());
        }
        logger.info("[DEBUG] updateById invoked for task id={}", id);

        try {
            Long uid = currentUserId();
            String operator = "anonymous";
            if (uid != null) {
                User u = userMapper.selectById(uid);
                operator = u != null && u.getUsername() != null ? u.getUsername() : String.valueOf(uid);
            }
            operationLogService.recordOperation(operator, "UPDATE_TASK", "task:" + id, "SUCCESS");
        } catch (Exception ignored) {}
        return Result.success(persisted);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Long uid = currentUserId();
        logger.info("[DEBUG] Received delete request for task id={}, userId={}", id, uid);
        boolean ok = taskService.delete(id);
        if (!ok) {
            logger.warn("[DEBUG] delete failed: task id={} not found or delete unsuccessful", id);
            return Result.error(404, "Task not found");
        }
        try {
            String operator = "anonymous";
            if (uid != null) {
                User u = userMapper.selectById(uid);
                operator = u != null && u.getUsername() != null ? u.getUsername() : String.valueOf(uid);
            }
            operationLogService.recordOperation(operator, "DELETE_TASK", "task:" + id, "SUCCESS");
        } catch (Exception ignored) {}
        return Result.success();
    }

    @PutMapping("/{id}/complete")
    public Result<?> complete(@PathVariable Long id, @RequestParam(required=false) Integer actualMinutes) {
        taskService.complete(id, actualMinutes);
        try {
            Long uid = currentUserId();
            String operator = "anonymous";
            if (uid != null) {
                User u = userMapper.selectById(uid);
                operator = u != null && u.getUsername() != null ? u.getUsername() : String.valueOf(uid);
            }
            operationLogService.recordOperation(operator, "COMPLETE_TASK", "task:" + id, "SUCCESS");
        } catch (Exception ignored) {}
        return Result.success();
    }
}
