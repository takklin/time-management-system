package com.timemanager.config;

import com.timemanager.entity.User;
import com.timemanager.mapper.UserMapper;
import com.timemanager.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                if (token.isEmpty()) {
                    throw new IllegalArgumentException("Token is empty");
                }
                Long userId = JwtUtil.parseUserId(token);
                if (userId == null) {
                    throw new IllegalArgumentException("UserId missing in token");
                }
                request.setAttribute("userId", userId);
                log.debug("[JWT认证] 解析成功：userId={}", userId);

                User user = userMapper.selectById(userId);
                String role;
                UserDetails userDetails;
                if (user == null) {
                    // 兼容：token 解析成功，用户可能刚创建但查询异常、或数据不一致。
                    log.warn("[JWT认证] 用户ID在数据库中不存在：userId={}, 使用默认USER角色", userId);
                    role = "USER";
                    userDetails = org.springframework.security.core.userdetails.User
                            .withUsername("user-" + userId)
                            .password("")
                            .authorities("ROLE_USER")
                            .build();
                } else {
                    role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
                    userDetails = org.springframework.security.core.userdetails.User
                            .withUsername(user.getUsername())
                            .password(user.getPassword())
                            .authorities("ROLE_" + role)
                            .build();
                    log.debug("[JWT认证] 用户认证成功：userId={}, username={}, role={}", userId, user.getUsername(), role);
                }
                // 保存到 request 属性，供 Controller 使用
                request.setAttribute("role", role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // 对于无效的 token，记录错误并清除上下文
                log.warn("[JWT认证] Token 认证失败，原因：{}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        // 继续过滤链，让 SecurityConfig 的 permitAll 规则处理不需要认证的请求
        filterChain.doFilter(request, response);
    }
}
