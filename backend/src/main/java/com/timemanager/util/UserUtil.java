package com.timemanager.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.timemanager.mapper.UserMapper;
import com.timemanager.entity.User;

@Component
public class UserUtil {
    private static UserMapper userMapper;

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        UserUtil.userMapper = userMapper;
    }

    public static Long getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest request = attrs.getRequest();

        Object v = request.getAttribute("userId");
        if (v instanceof Long) return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        if (v instanceof String) {
            try {
                return Long.valueOf((String) v);
            } catch (NumberFormatException ignored) {
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return JwtUtil.parseUserId(token);
            } catch (Exception ignored) {
                // 无效 token
            }
        }

        return null;
    }

    /**
     * 获取当前用户名（用于操作日志记录）
     */
    public static String getCurrentUsername() {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) return "unknown";

            // 从数据库查询用户名
            if (userMapper != null) {
                User user = userMapper.selectById(userId);
                if (user != null && user.getUsername() != null) {
                    return user.getUsername();
                }
            }

            return String.valueOf(userId);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
