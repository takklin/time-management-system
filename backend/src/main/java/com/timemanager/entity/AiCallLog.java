package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_call_log")
public class AiCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String provider;       // provider name
    @TableField(fill = FieldFill.INSERT)
    private Long userId;          // optional user id
    private String module;        // model or module name
    private String action;        // action, e.g., chat/completions

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private Integer responseTimeMs;
    private String status;       // success / error
    private String errorMessage; // store response or error info

    private LocalDateTime createdAt;
}
