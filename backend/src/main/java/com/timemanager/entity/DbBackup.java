package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据库备份记录表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_backup")
public class DbBackup {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String backupName;     // 备份名称
    private String backupFile;     // 备份文件路径
    private String backupType;     // 备份类型: full/incremental
    private Long fileSize;         // 文件大小(字节)
    private String status;         // 备份状态: success/failed
    private LocalDateTime backupTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
