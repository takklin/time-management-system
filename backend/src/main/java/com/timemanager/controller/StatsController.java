package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.mapper.TimeRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    @Autowired
    private TimeRecordMapper timeRecordMapper;

    private Long currentUserId() { return com.timemanager.util.UserUtil.getCurrentUserId(); }

    @GetMapping("/time-distribution")
    public Result<List<Map<String, Object>>> timeDistribution(@RequestParam String startDate, @RequestParam String endDate) {
        List<Map<String, Object>> data = timeRecordMapper.selectTimeDistribution(currentUserId(), startDate, endDate);
        return Result.success(data);
    }

    @GetMapping("/completion-trend")
    public Result<Map<String, List<?>>> completionTrend(@RequestParam String startDate,
                                                        @RequestParam String endDate,
                                                        @RequestParam(defaultValue = "day") String groupBy) {
        // simplified: return empty
        return Result.success(Map.of("dates", List.of(), "completed", List.of(), "total", List.of()));
    }

    @GetMapping("/daily-focus")
    public Result<Map<String, List<?>>> dailyFocus(@RequestParam String startDate,
                                                   @RequestParam String endDate) {
        return Result.success(Map.of("dates", List.of(), "minutes", List.of()));
    }

    @GetMapping("/estimate-vs-actual")
    public Result<List<Map<String, Object>>> estimateVsActual(@RequestParam String startDate,
                                                               @RequestParam String endDate) {
        List<Map<String, Object>> data = timeRecordMapper.selectEstimateVsActual(currentUserId(), startDate, endDate);
        return Result.success(data);
    }
}