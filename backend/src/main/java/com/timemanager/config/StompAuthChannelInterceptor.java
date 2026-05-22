package com.timemanager.config;

import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
import com.timemanager.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                List<String> auth = accessor.getNativeHeader("Authorization");
                String token = null;
                if (auth != null && !auth.isEmpty()) token = auth.get(0);
                if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
                if (token != null && !token.isEmpty()) {
                    Long userId = JwtUtil.parseUserId(token);
                    if (userId != null) {
                        User user = userMapper.selectById(userId);
                        if (user != null) {
                            String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
                            UsernamePasswordAuthenticationToken principal =
                                    new UsernamePasswordAuthenticationToken(user.getUsername(), null,
                                            AuthorityUtils.createAuthorityList("ROLE_" + role));
                            accessor.setUser(principal);
                            log.debug("[WebSocket] STOMP CONNECT authenticated: user={} id={}", user.getUsername(), userId);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[WebSocket] STOMP CONNECT auth failed: {}", e.getMessage());
            }
        }
        return message;
    }
}
