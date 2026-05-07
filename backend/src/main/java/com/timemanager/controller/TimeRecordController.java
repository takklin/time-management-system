package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.entity.TimeRecord;
import com.timemanager.service.TimeRecordService;
import com.timemanager.mapper.UserMapper;
import com.timemanager.entity.User;
import com.timemanager.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/time-records")
public class TimeRecordController {

    @Autowired
    private TimeRecordService timeRecordService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OperationLogService operationLogService;

    private Long currentUserId() { return com.timemanager.util.UserUtil.getCurrentUserId(); }

    @GetMapping
    public Result<List<TimeRecord>> list(@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        return Result.success(timeRecordService.list(currentUserId(), startDate, endDate));
    }

    @PostMapping
    public Result<?> create(@RequestBody TimeRecord record, HttpServletRequest request) {
        record.setUserId(currentUserId());
        timeRecordService.create(record);
        try {
            Long uid = currentUserId();
            String operator = "anonymous";
            if (uid != null) {
                User u = userMapper.selectById(uid);
                operator = u != null && u.getUsername() != null ? u.getUsername() : String.valueOf(uid);
            }
            String ip = request != null ? (request.getHeader("X-Forwarded-For") != null ? request.getHeader("X-Forwarded-For") : request.getRemoteAddr()) : null;
            String ua = request != null ? request.getHeader("User-Agent") : null;
            operationLogService.recordOperation(operator, "CREATE_TIME_RECORD", "time_record", "SUCCESS", ip, ua);
        } catch (Exception ignored) {}
        return Result.success();
    }

    @PostMapping("/start")
    public Result<?> startTimer(HttpServletRequest request) {
        Long recordId = timeRecordService.startTimer(currentUserId());
        try {
            Long uid = currentUserId();
            String operator = "anonymous";
            if (uid != null) {
                User u = userMapper.selectById(uid);
                operator = u != null && u.getUsername() != null ? u.getUsername() : String.valueOf(uid);
            }
            String ip = request != null ? (request.getHeader("X-Forwarded-For") != null ? request.getHeader("X-Forwarded-For") : request.getRemoteAddr()) : null;
            String ua = request != null ? request.getHeader("User-Agent") : null;
            operationLogService.recordOperation(operator, "START_TIMER", "timer", "SUCCESS", ip, ua);
        } catch (Exception ignored) {}
        return Result.success(Collections.singletonMap("recordId", recordId));
    }

    @PutMapping("/{id}/stop")
    public Result<?> stopTimer(@PathVariable Long id,
                                @RequestParam(required=false) Long taskId,
                                @RequestParam(required=false) String note,
                                HttpServletRequest request) {
        // ownership check
        TimeRecord existing = timeRecordService.getById(id);
        if (existing == null) return Result.error(404, "记录不存在");
        Long uid = currentUserId();
        if (existing.getUserId() == null || !existing.getUserId().equals(uid)) {
            return Result.error(403, "无权限停止该计时");
        }
        timeRecordService.stopTimer(id, taskId, note);
        try {
            String operator = "anonymous";
            if (uid != null) {
                User u = userMapper.selectById(uid);
                operator = u != null && u.getUsername() != null ? u.getUsername() : String.valueOf(uid);
            }
            String ip = request != null ? (request.getHeader("X-Forwarded-For") != null ? request.getHeader("X-Forwarded-For") : request.getRemoteAddr()) : null;
            String ua = request != null ? request.getHeader("User-Agent") : null;
            operationLogService.recordOperation(operator, "STOP_TIMER", "time_record:" + id, "SUCCESS", ip, ua);
        } catch (Exception ignored) {}
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        TimeRecord existing = timeRecordService.getById(id);
        if (existing == null) return Result.error(404, "记录不存在");
        Long uid = currentUserId();
        if (existing.getUserId() == null || !existing.getUserId().equals(uid)) {
            return Result.error(403, "无权限删除该记录");
        }
        timeRecordService.delete(id);
        try {
            String operator = "anonymous";
            if (uid != null) {
                User u = userMapper.selectById(uid);
                operator = u != null && u.getUsername() != null ? u.getUsername() : String.valueOf(uid);
            }
            String ip = request != null ? (request.getHeader("X-Forwarded-For") != null ? request.getHeader("X-Forwarded-For") : request.getRemoteAddr()) : null;
            String ua = request != null ? request.getHeader("User-Agent") : null;
            operationLogService.recordOperation(operator, "DELETE_TIME_RECORD", "time_record:" + id, "SUCCESS", ip, ua);
        } catch (Exception ignored) {}
        return Result.success();
    }
}