package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_message")
public class UserMessage {
    private Long id;
    private Long userId;
    private Long fromAdminId;
    private String title;
    private String content;
    private Boolean isRead;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime readTime;
}
