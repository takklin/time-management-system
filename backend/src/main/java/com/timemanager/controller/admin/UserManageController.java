package com.timemanager.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.timemanager.common.result.Result;
import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
import com.timemanager.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理Controller
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserManageController {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 获取用户列表（分页）
     */
    @GetMapping
    public Result<Map<String, Object>> getUserList(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
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
        
        wrapper.orderByDesc("created_at");
        
        IPage<User> result = userMapper.selectPage(pageObj, wrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("rows", result.getRecords().stream().map(UserVO::fromUser).toList());
        response.put("total", result.getTotal());
        
        return Result.success(response);
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}/detail")
    public Result<UserVO> getUserDetail(@PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            return Result.success(UserVO.fromUser(user));
        }
        return Result.error(404, "用户不存在");
    }
    
    /**
     * 获取用户分析数据
     */
    @GetMapping("/{userId}/analytics")
    public Result<Map<String, Object>> getUserAnalytics(@PathVariable Long userId) {
        // 这是一个占位符实现，返回示例数据
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("completionRateTrend", new Object[]{});
        analytics.put("focusTimeDistribution", new Object[]{});
        analytics.put("activeHoursHeatmap", new Object[]{});
        return Result.success(analytics);
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/{userId}/status")
    public Result<Boolean> updateUserStatus(
        @PathVariable Long userId,
        @RequestBody Map<String, Object> request
    ) {
        Integer status = (Integer) request.get("status");
        if (status == null) {
            return Result.error(400, "状态值不能为空");
        }
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        
        user.setStatus(status == 0 ? "active" : "disabled");
        user.setUpdatedAt(LocalDateTime.now());
        int updated = userMapper.updateById(user);
        
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
        
        return updated > 0 ? Result.success(true) : Result.error(500, "删除失败");
    }
}
