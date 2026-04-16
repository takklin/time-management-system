package com.timemanager.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtUtil {
    private static final String SECRET = "my_jwt_secret_key";
    private static final long EXPIRATION = 1000L * 60 * 60 * 24; // 1 day

    public static String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    public static Long parseUserId(String token) {
        String sub = Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return Long.valueOf(sub);
    }

    /**
     * 从 Token 中解析用户名（从用户表查询）
     * 由于 JWT only stores userId，这里需要从数据库查询用户名
     * 为了简化，直接返回 userId 作为备用
     */
    public static String parseUsername(String token) {
        try {
            Long userId = parseUserId(token);
            // TODO: 可以从数据库查询真实的用户名，现在返回 userId 作为备用
            // 实际应该注入 UserMapper 从数据库查询
            return String.valueOf(userId);
        } catch (Exception e) {
            return null;
        }
    }
}
