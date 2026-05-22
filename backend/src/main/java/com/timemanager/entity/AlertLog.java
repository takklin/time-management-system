package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统异常预警日志
 * 用于记录需要管理员关注的异常事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("alert_log")
public class AlertLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警类型：LOGIN_BURST, BATCH_DELETE, OFF_HOURS_OPERATION, PRIVILEGE_ESCALATION, RESTORE_BACKUP */
    private String alertType;

    /** 预警描述 */
    private String description;

    /** 严重级别：high, critical */
    private String severity;

    /** 关联的日志ID列表（JSON格式） */
    private String relatedLogIds;

    /** 关联的用户名 */
    private String relatedUsername;

    /** 关联的IP地址 */
    private String relatedIp;

    /** 状态：0未处理，1已读，2已确认 */
    private Integer status;

    /** 处理人 */
    private String handledBy;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 风险评分（可选） */
    @TableField("risk_score")
    private Integer riskScore;

    /** AI 建议（可选） */
    @TableField("ai_suggestion")
    private String aiSuggestion;

    /** 软删除标记：0=有效,1=已删除 */
    @TableField("is_deleted")
    private Integer isDeleted;
}
