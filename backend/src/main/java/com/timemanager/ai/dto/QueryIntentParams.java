package com.timemanager.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 查询意图参数 DTO
 * 用于承接 AI 解析结果，支持参数化查询
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryIntentParams {
    /** 意图类型 */
    private String intent;
    
    /** 时间范围 (today / yesterday / last_week / this_week / this_month / last_month / last_Xh / custom) */
    private String timeRange;
    
    /** 自定义开始时间 (YYYY-MM-DD HH:mm:ss) */
    private String customStart;
    
    /** 自定义结束时间 (YYYY-MM-DD HH:mm:ss) */
    private String customEnd;
    
    /** 返回结果数量限制 (默认 10) */
    private Integer limit = 10;
    
    /** 排序字段 (created_at / updated_at / count / name) */
    private String sortBy;
    
    /** 排序方向 (asc / desc) */
    private String sortOrder = "desc";
    
    /** 其他过滤条件 */
    private Map<String, Object> filters = new HashMap<>();
    
    /** 需要澄清的信息（如有歧义） */
    private String clarification;
    
    /** 原始问题（用于中间处理） */
    private String originalQuestion;
}
