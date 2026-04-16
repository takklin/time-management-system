package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * AI 智能预警表
 * 存储系统自动检测到的异常事件和 AI 生成的预警信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_alert")
public class AiAlert {
    
    private Long id;
    
    /**
     * 预警类型: ABNORMAL_LOGIN / BULK_DELETE / etc
     */
    private String alertType;
    
    /**
     * 严重程度: HIGH / MEDIUM / LOW
     */
    private String severity;
    
    /**
     * 预警标题
     */
    private String title;
    
    /**
     * AI 生成的详细分析
     */
    private String description;
    
    /**
     * AI 给出的安全建议
     */
    private String suggestion;
    
    /**
     * 关联的 operation_log ID 列表 (JSON 格式)
     */
    private String relatedLogIds;
    
    /**
     * 原始数据 (JSON 格式，用于问题追踪)
     */
    private String sourceData;
    
    /**
     * 是否已处理 (0=未处理, 1=已处理)
     */
    private Integer isHandled;
    
    /**
     * 处理者 ID (管理员 ID)
     */
    private Long handlerId;
    
    /**
     * 处理备注
     */
    private String handleNote;
    
    /**
     * 处理时间
     */
    private LocalDateTime handledAt;
    
    private LocalDateTime createdAt;
}
