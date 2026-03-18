package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.entity.TimeRecord;
import com.timemanager.service.TimeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/time-records")
public class TimeRecordController {

    @Autowired
    private TimeRecordService timeRecordService;

    private Long currentUserId() { return com.timemanager.util.UserUtil.getCurrentUserId(); }

    @GetMapping
    public Result<List<TimeRecord>> list(@RequestParam String startDate, @RequestParam String endDate) {
        return Result.success(timeRecordService.list(currentUserId(), startDate, endDate));
    }

    @PostMapping
    public Result<?> create(@RequestBody TimeRecord record) {
        record.setUserId(currentUserId());
        timeRecordService.create(record);
        return Result.success();
    }

    @PostMapping("/start")
    public Result<?> startTimer() {
        timeRecordService.startTimer(currentUserId());
        return Result.success();
    }

    @PutMapping("/{id}/stop")
    public Result<?> stopTimer(@PathVariable Long id,
                                @RequestParam(required=false) Long taskId,
                                @RequestParam(required=false) String note) {
        timeRecordService.stopTimer(id, taskId, note);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        timeRecordService.delete(id);
        return Result.success();
    }
}