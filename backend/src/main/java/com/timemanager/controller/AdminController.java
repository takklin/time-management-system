package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String username,
                                                  @RequestParam(required = false) String email,
                                                  @RequestParam(required = false) Integer status) {
        int offset = (page - 1) * size;
        List<User> list = userMapper.selectList(null);
        // 简单示例：不过滤/分页，由于项目简单，客户端可分页
        Map<String, Object> data = new HashMap<>();
        data.put("total", list.size());
        data.put("rows", list);
        return Result.success(data);
    }

    @GetMapping("/users/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = (Integer) body.get("status");
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        user.setDeleted(status == null || status == 0 ? 0 : 1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password == null || password.trim().isEmpty()) {
            return Result.error(400, "Password cannot be empty");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    @GetMapping("/system/stat")
    public Result<Map<String, Object>> systemStat() {
        Map<String, Object> stat = new HashMap<>();
        stat.put("userCount", userMapper.selectCount(null));
        stat.put("taskCount", 0); // 后续补充
        stat.put("scheduleCount", 0);
        return Result.success(stat);
    }
}
