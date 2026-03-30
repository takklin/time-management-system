package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.dto.LoginDTO;
import com.timemanager.service.AuthService;
import com.timemanager.vo.LoginVO;
import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody LoginDTO dto) {
        // Validate input
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return Result.error(400, "用户名不能为空");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            return Result.error(400, "邮箱不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return Result.error(400, "密码不能为空");
        }

        // Check if user already exists
        User existingUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", dto.getUsername())
                        .or()
                        .eq("email", dto.getEmail()));
        if (existingUser != null) {
            return Result.error(400, "用户名或邮箱已存在");
        }

        // Create new user
        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        newUser.setEmail(dto.getEmail());
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(newUser);
        
        // Auto login after registration
        return Result.success(authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    @GetMapping("/user")
    public Result<User> currentUser() {
        Long userId = com.timemanager.util.UserUtil.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @GetMapping("/me")
    public Result<User> me() {
        return currentUser();
    }

    @GetMapping("/test")
    public List<User> list() {
        return userMapper.selectList(null);
    }
}

