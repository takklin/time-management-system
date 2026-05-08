package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.mapper.TimeRecordMapper;
import com.timemanager.mapper.TaskMapper;
import com.timemanager.entity.TimeRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    @Autowired
    private TimeRecordMapper timeRecordMapper;

    @Autowired
    private TaskMapper taskMapper;

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
        try {
            LocalDateTime start = parseToStartOfDay(startDate);
            LocalDateTime end = parseToEndOfDay(endDate);

            List<Map<String, Object>> createdRows = taskMapper.countCreatedByDayUserRange(currentUserId(), start, end);
            List<Map<String, Object>> finishedRows = taskMapper.countFinishedByDayUserRange(currentUserId(), start, end);

            Map<String, Integer> createdMap = new HashMap<>();
            for (Map<String, Object> r : createdRows) {
                if (r == null) continue;
                Object d = r.get("date");
                Object c = r.get("count");
                if (d != null && c != null) createdMap.put(d.toString(), ((Number) c).intValue());
            }

            Map<String, Integer> finishedMap = new HashMap<>();
            for (Map<String, Object> r : finishedRows) {
                if (r == null) continue;
                Object d = r.get("date");
                Object c = r.get("count");
                if (d != null && c != null) finishedMap.put(d.toString(), ((Number) c).intValue());
            }

            ArrayList<String> dates = new ArrayList<>();
            ArrayList<Integer> completed = new ArrayList<>();
            ArrayList<Integer> total = new ArrayList<>();

            LocalDate cur = start.toLocalDate();
            LocalDate last = end.toLocalDate();
            while (!cur.isAfter(last)) {
                String ds = cur.toString();
                dates.add(ds);
                completed.add(finishedMap.getOrDefault(ds, 0));
                total.add(createdMap.getOrDefault(ds, 0));
                cur = cur.plusDays(1);
            }

            Map<String, List<?>> resp = new HashMap<>();
            resp.put("dates", dates);
            resp.put("completed", completed);
            resp.put("total", total);
            return Result.success(resp);
        } catch (Exception ex) {
            Map<String, List<?>> empty = new HashMap<>();
            empty.put("dates", List.of());
            empty.put("completed", List.of());
            empty.put("total", List.of());
            return Result.success(empty);
        }
    }

    @GetMapping("/daily-focus")
    public Result<Map<String, List<?>>> dailyFocus(@RequestParam String startDate,
                                                   @RequestParam String endDate) {
        try {
            LocalDateTime start = parseToStartOfDay(startDate);
            LocalDateTime end = parseToEndOfDay(endDate);

            String startStr = start.toLocalDate().toString();
            String endStr = end.toLocalDate().toString();

            List<TimeRecord> recs = timeRecordMapper.selectRecordsByUserInRange(currentUserId(), startStr, endStr);
            Map<String, Integer> dayMap = new HashMap<>();
            for (TimeRecord r : recs) {
                if (r == null) continue;
                String d = null;
                if (r.getRecordDate() != null) d = r.getRecordDate().toString();
                else if (r.getStartTime() != null) d = r.getStartTime().toLocalDate().toString();
                if (d == null) continue;
                Integer dur = r.getDurationMinutes() != null ? r.getDurationMinutes() : 0;
                dayMap.put(d, dayMap.getOrDefault(d, 0) + dur);
            }

            ArrayList<String> dates = new ArrayList<>();
            ArrayList<Integer> minutes = new ArrayList<>();
            LocalDate cur = start.toLocalDate();
            LocalDate last = end.toLocalDate();
            while (!cur.isAfter(last)) {
                String ds = cur.toString();
                dates.add(ds);
                minutes.add(dayMap.getOrDefault(ds, 0));
                cur = cur.plusDays(1);
            }

            Map<String, List<?>> resp = new HashMap<>();
            resp.put("dates", dates);
            resp.put("minutes", minutes);
            return Result.success(resp);
        } catch (Exception ex) {
            Map<String, List<?>> empty = new HashMap<>();
            empty.put("dates", List.of());
            empty.put("minutes", List.of());
            return Result.success(empty);
        }
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

    @GetMapping("/estimate-vs-actual")
    public Result<List<Map<String, Object>>> estimateVsActual(@RequestParam String startDate,
                                                               @RequestParam String endDate) {
        List<Map<String, Object>> data = timeRecordMapper.selectEstimateVsActual(currentUserId(), startDate, endDate);
        return Result.success(data);
    }

    /**
     * 获取用户时间消耗排行（Top N）
     */
    @GetMapping("/top-tasks")
    public Result<List<Map<String, Object>>> topTasks(@RequestParam String startDate,
                                                      @RequestParam String endDate,
                                                      @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> data = timeRecordMapper.selectTopTasksByUserInRange(currentUserId(), startDate, endDate, limit);
        return Result.success(data);
    }
}