package com.timemanager.monitor;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 全局API监控统计Filter，统计API请求数、平均响应、错误率、慢查询等
 */
@Component
public class ApiStatMonitorFilter implements Filter {
    // 以天为key，统计数据
    private final Map<String, Stat> statMap = new ConcurrentHashMap<>();
    // 慢查询阈值（毫秒）
    private static final long SLOW_QUERY_THRESHOLD = 1000;

    public static class Stat {
        public AtomicInteger total = new AtomicInteger();
        public AtomicInteger error = new AtomicInteger();
        public AtomicInteger slow = new AtomicInteger();
        public AtomicLong totalTime = new AtomicLong();
    }

    public Stat getTodayStat() {
        return statMap.computeIfAbsent(LocalDate.now().toString(), k -> new Stat());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        long start = System.currentTimeMillis();
        boolean error = false;
        try {
            chain.doFilter(request, response);
            if (response instanceof HttpServletResponse) {
                int status = ((HttpServletResponse) response).getStatus();
                if (status >= 400) error = true;
            }
        } finally {
            long cost = System.currentTimeMillis() - start;
            Stat stat = getTodayStat();
            stat.total.incrementAndGet();
            stat.totalTime.addAndGet(cost);
            if (error) stat.error.incrementAndGet();
            if (cost >= SLOW_QUERY_THRESHOLD) stat.slow.incrementAndGet();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) { }
    @Override
    public void destroy() { }

    // 提供给Controller查询今日统计
    public Map<String, Object> getTodayStatMap() {
        Stat s = getTodayStat();
        int total = s.total.get();
        int error = s.error.get();
        int slow = s.slow.get();
        long totalTime = s.totalTime.get();
        double avg = total == 0 ? 0 : (double) totalTime / total;
        double errRate = total == 0 ? 0 : (double) error * 100 / total;
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("apiRequestCount", total);
        map.put("apiAvgRespTime", Math.round(avg * 100.0) / 100.0);
        map.put("apiErrorRate", Math.round(errRate * 100.0) / 100.0);
        map.put("slowQueryCount", slow);
        return map;
    }
}
