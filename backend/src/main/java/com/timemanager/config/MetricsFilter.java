package com.timemanager.config;

import com.timemanager.entity.RequestMetrics;
import com.timemanager.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 请求性能指标收集过滤器
 * 记录每个HTTP请求的性能数据，用于健康度统计
 */
@Slf4j
@Component
public class MetricsFilter extends OncePerRequestFilter {

    /** 存储最近的请求指标（线程安全） */
    public static final List<RequestMetrics> METRICS_LIST = new CopyOnWriteArrayList<>();

    /** 最多保留的记录数 */
    private static final int MAX_METRICS = 20000;

    /**
     * 忽略记录指标的路径前缀
     */
    private static final String[] IGNORE_PATHS = {
            "/static/",
            "/assets/",
            "/js/",
            "/css/",
            "/images/",
            "/favicon.ico",
            "/swagger-ui",
            "/v2/api-docs",
            "/webjars/"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // 检查是否应该忽略此路径
        if (shouldIgnore(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        RequestMetrics metrics = new RequestMetrics(requestUri, method, startTime);
        metrics.setClientIp(HttpUtil.getClientIp(request));

        try {
            filterChain.doFilter(request, response);
            metrics.complete(
                    System.currentTimeMillis(),
                    true,
                    response.getStatus(),
                    null
            );
        } catch (Exception e) {
            metrics.complete(
                    System.currentTimeMillis(),
                    false,
                    response.getStatus(),
                    e.getMessage()
            );
            throw e;
        } finally {
            // 记录指标
            recordMetrics(metrics);
        }
    }

    /**
     * 记录指标，并维持指定大小的循环缓冲区
     */
    private void recordMetrics(RequestMetrics metrics) {
        METRICS_LIST.add(metrics);

        // 如果超过最大数量，移除最旧的一半
        if (METRICS_LIST.size() > MAX_METRICS) {
            List<RequestMetrics> toRemove = new CopyOnWriteArrayList<>(
                    METRICS_LIST.subList(0, MAX_METRICS / 2)
            );
            METRICS_LIST.removeAll(toRemove);
        }
    }

    /**
     * 检查是否应该忽略此路径
     */
    private boolean shouldIgnore(String path) {
        for (String ignorePath : IGNORE_PATHS) {
            if (path.startsWith(ignorePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取最近N毫秒内的指标
     */
    public static List<RequestMetrics> getMetricsInRange(int milliseconds) {
        long now = System.currentTimeMillis();
        long startTime = now - milliseconds;
        
        return Collections.synchronizedList(
                METRICS_LIST.parallelStream()
                        .filter(m -> m.getStartTime() >= startTime)
                        .toList()
        );
    }

    /**
     * 获取指定路径的平均响应时间
     */
    public static double getAverageResponseTime(String path, int milliseconds) {
        return getMetricsInRange(milliseconds).parallelStream()
                .filter(m -> m.getPath().equals(path))
                .mapToLong(RequestMetrics::getDuration)
                .average()
                .orElse(0);
    }

    /**
     * 清空所有指标（仅用于测试）
     */
    public static void clearMetrics() {
        METRICS_LIST.clear();
    }
}
