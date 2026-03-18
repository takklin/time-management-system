package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("schedule")
public class Schedule {
    private Long id;
    private Long userId;
    private Long taskId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer remindBefore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
