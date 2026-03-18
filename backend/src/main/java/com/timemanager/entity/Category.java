package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {
    private Long id;
    private Long userId;
    private String name;
    private String color;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
