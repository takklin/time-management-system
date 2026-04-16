package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户行为统计表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_behavior_stats")
public class UserBehaviorStats {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long userId;           // 用户ID
    private LocalDate statDate;    // 统计日期
    private Integer taskCount;     // 任务数量
    private Integer taskCompleted; // 完成任务数
    private Integer focusMinutes;  // 专注时长(分钟)
    private String categoryUsage;  // 分类使用情况(JSON)
    private String activeHours;    // 活跃时段(JSON)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
