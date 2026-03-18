package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("time_record")
public class TimeRecord {
    private Long id;
    private Long userId;
    private Long taskId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private String note;
    private LocalDate recordDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
