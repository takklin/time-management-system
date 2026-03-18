package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private String description;
    private String priority;
    private LocalDateTime deadline;
    private Integer estimatedMinutes;
    private Integer actualMinutes;
    private Integer status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
