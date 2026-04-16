package com.timemanager.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.setFieldValByName("userId", currentUserId, metaObject);
        }
    }
    @Override
    public void updateFill(MetaObject metaObject) {}
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.User) {
                    // 你的 UserDetails 实现需包含 userId，可根据实际情况调整
                    // 这里假设 username 就是 userId
                    String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                    try {
                        return Long.parseLong(username);
                    } catch (NumberFormatException ignore) {}
                }
            }
        } catch (Exception e) {}
        return null;
    }
}
