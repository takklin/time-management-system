package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String nickname;
    private String avatar;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
