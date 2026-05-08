package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志表实体，字段与现有数据库表 `operation_log` 对应。
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

    @TableField("risk_level")
    private String riskLevel;     // 风险等级（critical/high/medium/low）

    private String ip;            // IP 地址

    @TableField("ip_address")
    private String ipAddress;     // 备用 IP 地址字段

    @TableField("user_agent")
    private String userAgent;     // User-Agent

    @TableField("created_at")
    private LocalDateTime createdAt;  // 创建时间

    private String status;        // 操作状态（success/failed）

    @TableField("error_message")
    private String errorMessage;  // 错误信息

    @TableField("request_params")
    private String requestParams; // 请求参数

    @TableField("response_data")
    private String responseData;  // 响应数据
}
