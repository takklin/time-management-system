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

                User user = userMapper.selectById(userId);
                UserDetails userDetails;
                if (user == null) {
                    // 兼容：token 解析成功，用户可能刚创建但查询异常、或数据不一致。
                    userDetails = org.springframework.security.core.userdetails.User
                            .withUsername("user-" + userId)
                            .password("")
                            .authorities("USER")
                            .build();
                } else {
                    String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
                    userDetails = org.springframework.security.core.userdetails.User
                            .withUsername(user.getUsername())
                            .password(user.getPassword())
                            .authorities(role)
                            .build();
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"Unauthorized\",\"data\":null}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
