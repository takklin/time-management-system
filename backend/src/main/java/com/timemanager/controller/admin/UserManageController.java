package com.timemanager.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timemanager.common.result.Result;
import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
import com.timemanager.vo.UserVO;
import com.timemanager.vo.AdminUserDTO;
import com.timemanager.mapper.TaskMapper;
import com.timemanager.mapper.TimeRecordMapper;
import com.timemanager.mapper.OperationLogMapper;
import com.timemanager.entity.OperationLog;
import com.timemanager.entity.Task;
import com.timemanager.entity.TimeRecord;
import com.timemanager.service.OperationLogService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * 用户管理Controller
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserManageController {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TimeRecordMapper timeRecordMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 获取用户列表（分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getUserList(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String orderBy,
        @RequestParam(defaultValue = "desc") String orderType
    ) {
        Page<User> pageObj = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("username", keyword).or().like("email", keyword);
        }
        if (status != null) {
            wrapper.eq("deleted", status);
        }
        
        // 支持按使用时长排序（usageMinutes）——需要在所有匹配用户上聚合使用时长，再在内存中排序并分页
        if (orderBy != null && orderBy.equals("usageMinutes")) {
            List<User> allUsers = userMapper.selectList(wrapper);
            List<AdminUserDTO> allDtos = new ArrayList<>();
            for (User u : allUsers) {
                AdminUserDTO dto = new AdminUserDTO();
                dto.setId(u.getId());
                dto.setUsername(u.getUsername());
                dto.setEmail(u.getEmail());
                dto.setNickname(u.getNickname());
                dto.setAvatar(u.getAvatar());
                dto.setRole(u.getRole());
                dto.setCreatedAt(u.getCreatedAt());
                dto.setUpdatedAt(u.getUpdatedAt());

                // 注册天数
                if (u.getCreatedAt() != null) {
                    int days = (int) ChronoUnit.DAYS.between(u.getCreatedAt().toLocalDate(), LocalDate.now());
                    dto.setRegistrationDays(days >= 0 ? days : 0);
                } else {
                    dto.setRegistrationDays(0);
                }

                // 任务统计
                int totalTasks = taskMapper.countByUserId(u.getId());
                int finished = taskMapper.countFinishedByUserId(u.getId());
                dto.setCompletedTaskCount(finished);
                dto.setUncompletedTaskCount(Math.max(0, totalTasks - finished));
                dto.setCompletionRate(totalTasks == 0 ? 0.0 : (Math.round((finished * 10000.0 / totalTasks)) / 100.0));

                // 最后活跃时间（优先使用 time_record 的 end_time，否则使用 updatedAt）
                TimeRecord lastRecord = null;
                try {
                    List<TimeRecord> recs = timeRecordMapper.selectRecentByUser(u.getId(), 1);
                    if (recs != null && !recs.isEmpty()) lastRecord = recs.get(0);
                } catch (Exception ignored) {}
                if (lastRecord != null && lastRecord.getEndTime() != null) {
                    dto.setLastActiveTime(lastRecord.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } else if (u.getUpdatedAt() != null) {
                    dto.setLastActiveTime(u.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } else {
                    dto.setLastActiveTime(null);
                }

                dto.setStatus(u.getDeleted());

                // 计算使用时长（分钟）
                try {
                    Integer mins = timeRecordMapper.selectTotalDurationByUser(u.getId());
                    dto.setUsageMinutes(mins == null ? 0 : mins);
                } catch (Exception ignored) {
                    dto.setUsageMinutes(0);
                }

                allDtos.add(dto);
            }

            // 排序
            if ("asc".equalsIgnoreCase(orderType)) {
                allDtos.sort((a, b) -> Integer.compare(a.getUsageMinutes() == null ? 0 : a.getUsageMinutes(), b.getUsageMinutes() == null ? 0 : b.getUsageMinutes()));
            } else {
                allDtos.sort((a, b) -> Integer.compare(b.getUsageMinutes() == null ? 0 : b.getUsageMinutes(), a.getUsageMinutes() == null ? 0 : a.getUsageMinutes()));
            }

            // 内存分页
            int totalAll = allDtos.size();
            int from = Math.max(0, (page - 1) * size);
            int to = Math.min(totalAll, from + size);
            List<AdminUserDTO> pageRows = from >= totalAll ? List.of() : allDtos.subList(from, to);

            Map<String, Object> resp = new HashMap<>();
            resp.put("rows", pageRows);
            resp.put("total", totalAll);
            resp.put("pageNum", page);
            resp.put("pageSize", size);
            return Result.success(resp);
        }

        wrapper.orderByDesc("created_at");
        
        IPage<User> result = userMapper.selectPage(pageObj, wrapper);

        List<AdminUserDTO> rows = new ArrayList<>();
        for (User u : result.getRecords()) {
            AdminUserDTO dto = new AdminUserDTO();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setEmail(u.getEmail());
            dto.setNickname(u.getNickname());
            dto.setAvatar(u.getAvatar());
            dto.setRole(u.getRole());
            dto.setCreatedAt(u.getCreatedAt());
            dto.setUpdatedAt(u.getUpdatedAt());

            // 注册天数
            if (u.getCreatedAt() != null) {
                int days = (int) ChronoUnit.DAYS.between(u.getCreatedAt().toLocalDate(), LocalDate.now());
                dto.setRegistrationDays(days >= 0 ? days : 0);
            } else {
                dto.setRegistrationDays(0);
            }

            // 任务统计
            int totalTasks = taskMapper.countByUserId(u.getId());
            int finished = taskMapper.countFinishedByUserId(u.getId());
            dto.setCompletedTaskCount(finished);
            dto.setUncompletedTaskCount(Math.max(0, totalTasks - finished));
            dto.setCompletionRate(totalTasks == 0 ? 0.0 : (Math.round((finished * 10000.0 / totalTasks)) / 100.0));

            // 最后活跃时间（优先使用 time_record 的 end_time，否则使用 updatedAt）
            TimeRecord lastRecord = null;
            try {
                List<TimeRecord> recs = timeRecordMapper.selectRecentByUser(u.getId(), 1);
                if (recs != null && !recs.isEmpty()) lastRecord = recs.get(0);
            } catch (Exception ignored) {}
            if (lastRecord != null && lastRecord.getEndTime() != null) {
                dto.setLastActiveTime(lastRecord.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else if (u.getUpdatedAt() != null) {
                dto.setLastActiveTime(u.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                dto.setLastActiveTime(null);
            }

            dto.setStatus(u.getDeleted());

            // 计算使用时长（分钟），用于表格显示
            try {
                Integer mins = timeRecordMapper.selectTotalDurationByUser(u.getId());
                dto.setUsageMinutes(mins == null ? 0 : mins);
            } catch (Exception ignored) {
                dto.setUsageMinutes(0);
            }

            rows.add(dto);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("rows", rows);
        response.put("total", result.getTotal());
        response.put("pageNum", result.getCurrent());
        response.put("pageSize", result.getSize());

        return Result.success(response);
    }

        /**
         * 管理员禁用用户（设置 status=disabled + deleted=1），由管理员页面的“冻结账号”按钮调用
         */
        @PostMapping("/{username}/disable")
        public Result<String> disableUser(@PathVariable String username) {
            try {
                User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username).last("LIMIT 1"));
                if (user == null) return Result.error(404, "用户不存在");
                user.setStatus("disabled");
                user.setDeleted(1);
                userMapper.updateById(user);

                String operator = "system";
                try {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getName() != null) operator = auth.getName();
                } catch (Exception ignored) {}
                try { operationLogService.recordOperation(operator, "DISABLE_USER", username, "success"); } catch (Exception ignored) {}

                return Result.success("已禁用用户 " + username);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(500, "禁用用户失败: " + e.getMessage());
            }
        }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}/detail")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setStatus(user.getDeleted());

        // 补充统计
        int totalTasks = taskMapper.countByUserId(userId);
        int finished = taskMapper.countFinishedByUserId(userId);
        dto.setCompletedTaskCount(finished);
        dto.setUncompletedTaskCount(Math.max(0, totalTasks - finished));
        dto.setCompletionRate(totalTasks == 0 ? 0.0 : (Math.round((finished * 10000.0 / totalTasks)) / 100.0));
        if (user.getCreatedAt() != null) {
            int days = (int) ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now());
            dto.setRegistrationDays(days >= 0 ? days : 0);
        } else {
            dto.setRegistrationDays(0);
        }

        // 最后活跃时间
        try {
            List<TimeRecord> recs = timeRecordMapper.selectRecentByUser(userId, 1);
            if (recs != null && !recs.isEmpty() && recs.get(0).getEndTime() != null) {
                dto.setLastActiveTime(recs.get(0).getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else if (user.getUpdatedAt() != null) {
                dto.setLastActiveTime(user.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        } catch (Exception ignored) {}

        Map<String, Object> result = new HashMap<>();
        result.put("user", dto);
        // 最近若干任务（前 20 条）
        try {
            List<Task> tasks = taskMapper.selectWithCategoryName(userId);
            result.put("tasks", tasks == null ? List.of() : tasks);
        } catch (Exception e) {
            result.put("tasks", List.of());
        }

        // 最近时间记录
        try {
            List<TimeRecord> records = timeRecordMapper.selectRecentByUser(userId, 20);
            result.put("timeRecords", records == null ? List.of() : records);
        } catch (Exception e) {
            result.put("timeRecords", List.of());
        }

        return Result.success(result);
    }
    
    /**
     * 获取用户分析数据
     */
    @GetMapping("/{userId}/analytics")
    public Result<Map<String, Object>> getUserAnalytics(@PathVariable Long userId) {
        Map<String, Object> analytics = buildUserAnalytics(userId);
        return Result.success(analytics);
    }

    // 导出用户分析为 CSV 文件
    @GetMapping("/{userId}/analytics/export")
    public ResponseEntity<byte[]> exportUserAnalytics(@PathVariable Long userId) {
        Map<String, Object> analytics = buildUserAnalytics(userId);

        // 生成 CSV
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            writer.write("用户行为分析报告\r\n\r\n");

            // 完成率趋势
            Map<String, Object> trend = (Map<String, Object>) analytics.getOrDefault("completionRateTrend", Map.of("dates", List.of(), "finished", List.of(), "total", List.of()));
            writer.write("任务完成率周趋势\r\n");
            writer.write("日期,完成数,总数,完成率(%)\r\n");
            List<String> dates = (List<String>) trend.getOrDefault("dates", List.of());
            List<Number> finished = (List<Number>) trend.getOrDefault("finished", List.of());
            List<Number> total = (List<Number>) trend.getOrDefault("total", List.of());
            for (int i = 0; i < dates.size(); i++) {
                String date = dates.get(i);
                int f = i < finished.size() ? finished.get(i).intValue() : 0;
                int t = i < total.size() ? total.get(i).intValue() : 0;
                double rate = t > 0 ? (f * 100.0 / t) : 0.0;
                writer.write(String.format("%s,%d,%d,%.2f\r\n", date, f, t, rate));
            }
            writer.write("\r\n");

            // 专注时长分布
            writer.write("专注时长分布\r\n");
            writer.write("分类,时长(分钟)\r\n");
            List<Map<String, Object>> focus = (List<Map<String, Object>>) analytics.getOrDefault("focusTimeDistribution", List.of());
            for (Map<String, Object> m : focus) {
                String name = m.getOrDefault("name", m.getOrDefault("NAME", "未分类")).toString();
                Object val = m.getOrDefault("value", m.getOrDefault("VALUE", 0));
                writer.write(String.format("%s,%s\r\n", name.replaceAll("\r|\n", " "), val.toString()));
            }
            writer.write("\r\n");

            // 活跃时段
            writer.write("活跃时段(小时),分钟\r\n");
            List<Map<String, Object>> hours = (List<Map<String, Object>>) analytics.getOrDefault("activeHoursHeatmap", List.of());
            for (Map<String, Object> m : hours) {
                Object h = m.getOrDefault("hour", m.getOrDefault("HOUR", 0));
                Object v = m.getOrDefault("value", m.getOrDefault("VALUE", 0));
                writer.write(String.format("%s,%s\r\n", h.toString(), v.toString()));
            }

            writer.flush();
            byte[] bytes = baos.toByteArray();
            String filename = "analytics_user_" + userId + "_" + LocalDate.now().toString() + ".csv";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDispositionFormData("attachment", filename);
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("export failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    // 构建用户分析数据的可重用方法
    private Map<String, Object> buildUserAnalytics(Long userId) {
        Map<String, Object> analytics = new HashMap<>();

        // 完成率趋势（近7天）
        int days = 7;
        List<Map<String, Object>> created = taskMapper.countCreatedByDayUser(userId, days);
        List<Map<String, Object>> finished = taskMapper.countFinishedByDayUser(userId, days);

        List<String> dates = new ArrayList<>();
        List<Integer> finishedCounts = new ArrayList<>();
        List<Integer> totalCounts = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            String ds = d.toString();
            dates.add(ds);
            int f = 0, t = 0;
            for (Map<String, Object> m : finished) {
                if (ds.equals(String.valueOf(m.get("date")))) {
                    f = ((Number) m.get("count")).intValue();
                    break;
                }
            }
            for (Map<String, Object> m : created) {
                if (ds.equals(String.valueOf(m.get("date")))) {
                    t = ((Number) m.get("count")).intValue();
                    break;
                }
            }
            finishedCounts.add(f);
            totalCounts.add(t);
        }
        analytics.put("completionRateTrend", Map.of("dates", dates, "finished", finishedCounts, "total", totalCounts));

        // 专注时长分布（近30天）
        String start30 = LocalDate.now().minusDays(29).toString();
        String endToday = LocalDate.now().toString();
        List<Map<String, Object>> focusDist = timeRecordMapper.selectTimeDistribution(userId, start30, endToday);
        // 处理可能的空 name，并保证至少返回一项（用于前端展示）
        if (focusDist == null || focusDist.isEmpty()) {
            focusDist = new ArrayList<>();
            focusDist.add(Map.of("name", "无数据", "value", 0));
        } else {
            for (Map<String, Object> m : focusDist) {
                if (m.get("name") == null) m.put("name", "未分类");
            }
        }
        analytics.put("focusTimeDistribution", focusDist);

        // 活跃时段热力图（近7天按小时汇总）——使用记录逐分钟拆分以获得更准确分布
        String start7 = LocalDate.now().minusDays(6).toString();
        List<TimeRecord> recs = timeRecordMapper.selectRecordsByUserInRange(userId, start7, endToday);
        int[] hourMins = new int[24];
        if (recs != null) {
            for (TimeRecord r : recs) {
                try {
                    java.time.LocalDateTime st = r.getStartTime();
                    java.time.LocalDateTime et = r.getEndTime();
                    if (st == null) continue;
                    if (et == null && r.getDurationMinutes() != null) {
                        et = st.plusMinutes(r.getDurationMinutes());
                    }
                    if (et == null) continue;
                    if (et.isBefore(st)) continue;
                    long mins = java.time.temporal.ChronoUnit.MINUTES.between(st, et);
                    for (long m = 0; m < mins; m++) {
                        java.time.LocalDateTime minute = st.plusMinutes(m);
                        // 仅统计在查询范围内的日期
                        if (minute.toLocalDate().isBefore(LocalDate.now().minusDays(6)) || minute.toLocalDate().isAfter(LocalDate.now())) continue;
                        int h = minute.getHour();
                        hourMins[h]++;
                    }
                } catch (Exception ignored) {}
            }
        }
        List<Map<String, Object>> activeHours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            activeHours.add(Map.of("hour", h, "value", hourMins[h]));
        }
        analytics.put("activeHoursHeatmap", activeHours);

        return analytics;
    }

    // 管理员查看某用户的任务列表
    @GetMapping("/{userId}/tasks")
    public Result<Map<String, Object>> getUserTasks(@PathVariable Long userId,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "20") Integer size) {
        List<Task> tasks = taskMapper.selectWithCategoryName(userId);
        int total = tasks == null ? 0 : tasks.size();
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        int from = (p - 1) * s;
        int to = Math.min(total, from + s);
        List<Task> rows;
        if (tasks == null || total == 0 || from >= total) {
            rows = List.of();
        } else {
            rows = tasks.subList(from, to);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("rows", rows);
        res.put("total", total);
        res.put("pageNum", p);
        res.put("pageSize", s);
        return Result.success(res);
    }

    // 管理员查看某用户的近期时间记录（支持分页）
    @GetMapping("/{userId}/time-records")
    public Result<Map<String, Object>> getUserTimeRecords(@PathVariable Long userId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer size) {
        // 为兼容性简单处理：查询一定数量的最近记录后在内存分页
        List<TimeRecord> all = timeRecordMapper.selectRecentByUser(userId, 1000);
        int total = all == null ? 0 : all.size();
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        int from = (p - 1) * s;
        int to = Math.min(total, from + s);
        List<TimeRecord> rows;
        if (all == null || total == 0 || from >= total) {
            rows = List.of();
        } else {
            rows = all.subList(from, to);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("rows", rows);
        res.put("total", total);
        res.put("pageNum", p);
        res.put("pageSize", s);
        return Result.success(res);
    }

    // 管理员查看某用户的行为轨迹（操作日志），分页
    @GetMapping("/{userId}/logs")
    public Result<Map<String, Object>> getUserLogs(@PathVariable Long userId,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "20") Integer size) {
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        int offset = (p - 1) * s;
        List<OperationLog> logs = List.of();
        int total = 0;

        // 兼容多种 schema：优先按 operator(用户名) 查询（许多老表使用 operator 字段），
        // 若没有 operator 数据或查询失败，再尝试按 user_id 字段查询。
        try {
            // 先从用户表查用户名
            var user = userMapper.selectById(userId);
            if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
                try {
                    logs = operationLogMapper.selectByOperator(user.getUsername(), offset, s);
                    total = operationLogMapper.countByOperator(user.getUsername());
                } catch (Exception exOp) {
                    // operator 查询失败或列不存在，回退到按 user_id 查询
                    try {
                        logs = operationLogMapper.selectByUserId(userId, offset, s);
                        total = operationLogMapper.countByUserId(userId);
                    } catch (Exception exId) {
                        logs = List.of();
                        total = 0;
                    }
                }
            } else {
                // 没有用户名，直接尝试按 user_id
                try {
                    logs = operationLogMapper.selectByUserId(userId, offset, s);
                    total = operationLogMapper.countByUserId(userId);
                } catch (Exception ex) {
                    logs = List.of();
                    total = 0;
                }
            }
        } catch (Exception e) {
            logs = List.of();
            total = 0;
        }
        Map<String, Object> res = new HashMap<>();
        res.put("rows", logs == null ? List.of() : logs);
        res.put("total", total);
        return Result.success(res);
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/{userId}/status")
    public Result<Boolean> updateUserStatus(
        @PathVariable Long userId,
        @RequestBody Map<String, Object> request
    ) {
        Object statusObj = request.get("status");
        if (statusObj == null) {
            return Result.error(400, "状态值不能为空");
        }

        int status;
        if (statusObj instanceof Number) {
            status = ((Number) statusObj).intValue();
        } else {
            try {
                status = Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException e) {
                return Result.error(400, "状态值格式不正确");
            }
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 数据库中用户表使用 `deleted` 表示启用/禁用状态（0=启用，1=禁用），直接更新该字段
        user.setDeleted(status == 0 ? 0 : 1);
        user.setUpdatedAt(LocalDateTime.now());
        int updated = userMapper.updateById(user);

        // 记录操作日志（由当前登录管理员执行）
        try {
            Long adminId = com.timemanager.util.UserUtil.getCurrentUserId();
            String operator = "system";
            if (adminId != null) {
                User adminUser = userMapper.selectById(adminId);
                operator = adminUser != null && adminUser.getUsername() != null ? adminUser.getUsername() : String.valueOf(adminId);
            }
            String action = user.getDeleted() == 1 ? "DISABLE_USER" : "ENABLE_USER";
            operationLogService.recordOperation(operator, action, "user:" + userId, updated > 0 ? "SUCCESS" : "FAILED");
        } catch (Exception ignored) {}

        return updated > 0 ? Result.success(true) : Result.error(500, "更新失败");
    }
    
    /**
     * 重置用户密码
     */
    @PutMapping("/{userId}/reset-password")
    public Result<Boolean> resetUserPassword(
        @PathVariable Long userId,
        @RequestBody Map<String, String> request
    ) {
        String password = request.get("password");
        if (password == null || password.isEmpty()) {
            return Result.error(400, "密码不能为空");
        }
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        int updated = userMapper.updateById(user);
        
        // 记录操作日志：重置密码
        try {
            Long adminId = com.timemanager.util.UserUtil.getCurrentUserId();
            String operator = "system";
            if (adminId != null) {
                User adminUser = userMapper.selectById(adminId);
                operator = adminUser != null && adminUser.getUsername() != null ? adminUser.getUsername() : String.valueOf(adminId);
            }
            operationLogService.recordOperation(operator, "RESET_PASSWORD", "user:" + userId, updated > 0 ? "SUCCESS" : "FAILED");
        } catch (Exception ignored) {}

        return updated > 0 ? Result.success(true) : Result.error(500, "重置失败");
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    public Result<Boolean> deleteUser(@PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        
        user.setDeleted(1);
        user.setUpdatedAt(LocalDateTime.now());
        int updated = userMapper.updateById(user);
        
        // 记录操作日志：删除用户
        try {
            Long adminId = com.timemanager.util.UserUtil.getCurrentUserId();
            String operator = "system";
            if (adminId != null) {
                User adminUser = userMapper.selectById(adminId);
                operator = adminUser != null && adminUser.getUsername() != null ? adminUser.getUsername() : String.valueOf(adminId);
            }
            operationLogService.recordOperation(operator, "DELETE_USER", "user:" + userId, updated > 0 ? "SUCCESS" : "FAILED");
        } catch (Exception ignored) {}

        return updated > 0 ? Result.success(true) : Result.error(500, "删除失败");
    }
}
