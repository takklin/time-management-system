package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    // 非数据库字段：用于返回时携带分类名称
    @TableField(exist = false)
    private String categoryName;

    private String title;
    private String description;
    private String priority;
    @TableField("start_time")
    private LocalDateTime startTime;
    private Integer estimatedMinutes;
    private Integer actualMinutes;
    private Integer status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
