package com.timemanager.util;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HTTP 工具类
 * 用于获取请求的IP地址、User-Agent等信息
 */
public class HttpUtil {

    /**
     * 获取客户端IP地址
     * 支持代理环境（X-Forwarded-For 等）
     */
    public static String getClientIp() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            return "0.0.0.0";
        }
        return getClientIp(request);
    }

    /**
     * 获取客户端IP地址
     * 支持代理环境（X-Forwarded-For 等）
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        String ip = null;

        // 1. 检查 X-Forwarded-For（常见代理头）
        ip = request.getHeader("x-forwarded-for");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 可能有多个IP，取第一个
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index).trim();
            }
            return ip.trim();
        }

        // 2. 检查 Proxy-Client-IP
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 3. 检查 WL-Proxy-Client-IP
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 4. 检查 HTTP_CLIENT_IP
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 5. 检查 HTTP_X_FORWARDED_FOR
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 6. 最后使用默认方法
        ip = request.getRemoteAddr();
        return ip != null ? ip : "0.0.0.0";
    }

    /**
     * 获取User-Agent
     */
    public static String getUserAgent() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            return null;
        }
        return getUserAgent(request);
    }

    /**
     * 获取User-Agent
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ua = request.getHeader("User-Agent");
        return ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua;
    }

    /**
     * 获取当前Request对象
     */
    public static HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            // 如果在非Web环境下调用，会抛异常
        }
        return null;
    }

    /**
     * 获取请求的完整URL（不含查询参数）
     */
    public static String getRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if (port != 80 && port != 443) {
            url.append(":").append(port);
        }
        url.append(contextPath).append(requestUri);
        return url.toString();
    }

    /**
     * 判断是否为AJAX请求
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    /**
     * 解析User-Agent获取设备信息
     */
    public static String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        if (userAgent.contains("Windows NT")) {
            return "Windows";
        } else if (userAgent.contains("Mac OS X")) {
            return "Mac";
        } else if (userAgent.contains("Linux")) {
            if (userAgent.contains("Android")) {
                return "Android";
            }
            return "Linux";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        } else if (userAgent.contains("Android")) {
            return "Android";
        }
        return "Unknown";
    }
}
