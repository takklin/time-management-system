package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.dto.ScheduleDTO;
import com.timemanager.entity.Schedule;
import com.timemanager.service.ScheduleService;
import com.timemanager.service.TaskService;
import com.timemanager.entity.Task;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private TaskService taskService;

    private Long currentUserId() { return com.timemanager.util.UserUtil.getCurrentUserId(); }

    @GetMapping
    public Result<List<Schedule>> list(@RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate) {
        return Result.success(scheduleService.list(currentUserId(), startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result<Schedule> get(@PathVariable Long id) {
        Schedule s = scheduleService.getById(id);
        if (s == null) return Result.error(404, "日程不存在");
        Long uid = currentUserId();
        if (s.getUserId() == null || !s.getUserId().equals(uid)) {
            return Result.error(403, "无权限访问该日程");
        }
        return Result.success(s);
    }

    @PostMapping
    public Result<?> create(@RequestBody ScheduleDTO dto) {
        Schedule schedule = dto.toEntity();
        // 基本校验：开始/结束时间不能为空并且顺序正确
        LocalDateTime st = schedule.getStartTime();
        LocalDateTime et = schedule.getEndTime();
        if (st == null || et == null) {
            return Result.error(400, "startTime 和 endTime 不能为空");
        }
        if (st.isAfter(et)) {
            return Result.error(400, "startTime 必须早于 endTime");
        }
        schedule.setUserId(currentUserId());
        // 验证传入的 taskId 是否存在且属于当前用户；若无效则置空，避免外键约束错误
        if (schedule.getTaskId() != null) {
            Task t = taskService.getById(schedule.getTaskId());
            if (t == null || t.getUserId() == null || !t.getUserId().equals(currentUserId())) {
                schedule.setTaskId(null);
            }
        }
        scheduleService.create(schedule);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody ScheduleDTO dto) {
        Schedule schedule = dto.toEntity();
        schedule.setId(id);
        // 更新时也做时间校验（若前端不传则允许为空，但若传了要校验顺序）
        LocalDateTime st = schedule.getStartTime();
        LocalDateTime et = schedule.getEndTime();
        if (st != null && et != null && st.isAfter(et)) {
            return Result.error(400, "startTime 必须早于 endTime");
        }
        // 验证当前用户是否有权更新该日程
        Schedule existing = scheduleService.getById(id);
        if (existing == null) {
            return Result.error(404, "日程不存在");
        }
        Long uid = currentUserId();
        if (existing.getUserId() == null || !existing.getUserId().equals(uid)) {
            return Result.error(403, "无权限更新该日程");
        }
        // 同样在更新时验证 taskId
        if (schedule.getTaskId() != null) {
            Task t = taskService.getById(schedule.getTaskId());
            if (t == null || t.getUserId() == null || !t.getUserId().equals(currentUserId())) {
                schedule.setTaskId(null);
            }
        }
        // 强制绑定当前用户，防止越权修改 userId
        schedule.setUserId(uid);
        scheduleService.update(schedule);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Schedule existing = scheduleService.getById(id);
        if (existing == null) {
            return Result.error(404, "日程不存在");
        }
        Long uid = currentUserId();
        if (existing.getUserId() == null || !existing.getUserId().equals(uid)) {
            return Result.error(403, "无权限删除该日程");
        }
        scheduleService.delete(id);
        return Result.success();
    }
}
