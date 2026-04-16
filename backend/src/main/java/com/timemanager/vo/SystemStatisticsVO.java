package com.timemanager.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 系统统计VO（数据传输对象）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatisticsVO {
    
    // 用户增长数据
    private List<Map<String, Object>> userGrowth;
    
    // 用户活跃度数据
    private List<Map<String, Object>> userActivity;
    
    // 任务趋势数据
    private List<Map<String, Object>> taskTrend;
    
    // 专注热力图数据
    private List<Map<String, Object>> focusHeatmap;
    
    // 分类排行
    private List<Map<String, Object>> categoryRanking;
    
    // 用户排行榜
    private List<Map<String, Object>> userRanking;
    
    // 概览统计
    private Map<String, Object> overview;
}
