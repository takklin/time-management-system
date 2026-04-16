package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String operator;      // 操作者用户名
    private String action;        // 操作类型
    private String target;        // 操作对象
    private String result;        // 操作结果（success/failed）
    private String riskLevel;     // 风险等级（critical/high/medium/low）
    private String ip;            // IP地址
    @TableField("user_agent")
    private String userAgent;     // User-Agent
    private LocalDateTime createdAt;  // 创建时间
    private String ipAddress;     // IP地址（备用）
    private String status;        // 操作状态
    private String errorMessage;  // 错误信息
}
