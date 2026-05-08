package com.timemanager.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 额外统计字段
    private Integer registrationDays;
    private Double completionRate; // 百分比
    private Integer completedTaskCount;
    private Integer uncompletedTaskCount;
    private String lastActiveTime; // ISO 字符串或本地格式
    private Integer status; // 0=正常，1=禁用 (对应 user.deleted)
    private Integer usageMinutes; // 累计使用时长，单位：分钟
}
