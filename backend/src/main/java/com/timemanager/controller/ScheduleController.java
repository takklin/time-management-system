package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.entity.Schedule;
import com.timemanager.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    private Long currentUserId() { return com.timemanager.util.UserUtil.getCurrentUserId(); }

    @GetMapping
    public Result<List<Schedule>> list(@RequestParam String startDate, @RequestParam String endDate) {
        return Result.success(scheduleService.list(currentUserId(), startDate, endDate));
    }

    @PostMapping
    public Result<?> create(@RequestBody Schedule schedule) {
        schedule.setUserId(currentUserId());
        scheduleService.create(schedule);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Schedule schedule) {
        schedule.setId(id);
        scheduleService.update(schedule);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return Result.success();
    }
}
