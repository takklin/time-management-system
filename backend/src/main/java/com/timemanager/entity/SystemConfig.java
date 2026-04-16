package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统配置表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_config")
public class SystemConfig {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String configKey;      // 配置键
    private String configValue;    // 配置值
    private String description;    // 描述
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
