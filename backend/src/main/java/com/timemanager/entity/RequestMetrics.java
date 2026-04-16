package com.timemanager.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求性能指标
 * 用于记录每个HTTP请求的性能数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetrics {
    /** 请求的URI路径 */
    private String path;

    /** 请求开始时间（毫秒） */
    private long startTime;

    /** 请求结束时间（毫秒） */
    private long endTime;

    /** 请求耗时（毫秒） */
    private long duration;

    /** 请求是否成功 */
    private boolean success;

    /** HTTP模态（GET/POST等） */
    private String method;

    /** 客户端IP */
    private String clientIp;

    /** HTTP状态码 */
    private int statusCode;

    /** 异常信息（如有异常） */
    private String errorMessage;

    public RequestMetrics(String path, String method, long startTime) {
        this.path = path;
        this.method = method;
        this.startTime = startTime;
    }

    public void complete(long endTime, boolean success, int statusCode, String errorMessage) {
        this.endTime = endTime;
        this.duration = endTime - startTime;
        this.success = success;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
    }

    /**
     * 判断是否为慢查询（响应时间 > 2000ms）
     */
    public boolean isSlowQuery() {
        return duration > 2000;
    }
}
