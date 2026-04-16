package com.timemanager.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timemanager.common.result.Result;
import com.timemanager.entity.DbBackup;
import com.timemanager.service.DbBackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库备份管理Controller
 */
@RestController
@RequestMapping("/api/v1/admin/backup")
public class DbBackupController {
    
    @Autowired
    private DbBackupService dbBackupService;
    
    /**
     * 获取备份列表（分页）
     */
    @GetMapping("/list")
    public Result<IPage<DbBackup>> getBackupList(
        @RequestParam(defaultValue = "1") Integer current,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        Page<DbBackup> page = new Page<>(current, size);
        IPage<DbBackup> result = dbBackupService.page(
            page,
            new QueryWrapper<DbBackup>().orderByDesc("created_at")
        );
        return Result.success(result);
    }
    
    /**
     * 创建备份
     */
    @PostMapping("/create")
    public Result<DbBackup> createBackup() {
        // 这里是备份逻辑的占位符
        // 实际实现需要连接数据库执行备份操作
        DbBackup backup = new DbBackup();
        backup.setBackupName("backup_" + System.currentTimeMillis());
        backup.setBackupType("full");
        backup.setStatus("success");
        backup.setFileSize(0L);
        backup.setBackupTime(LocalDateTime.now());
        
        dbBackupService.save(backup);
        return Result.success(backup);
    }
    
    /**
     * 删除备份
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteBackup(@PathVariable Long id) {
        boolean deleted = dbBackupService.removeById(id);
        if (deleted) {
            return Result.success(true);
        }
        return Result.error(500, "删除失败");
    }
    
    /**
     * 获取备份统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getBackupStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalBackups = dbBackupService.count();
        long totalSize = dbBackupService.list().stream()
            .mapToLong(b -> b.getFileSize() != null ? b.getFileSize() : 0)
            .sum();
        
        stats.put("totalBackups", totalBackups);
        stats.put("totalSize", totalSize);
        stats.put("lastBackupTime", dbBackupService.list().stream()
            .max((a, b) -> a.getBackupTime().compareTo(b.getBackupTime()))
            .map(DbBackup::getBackupTime)
            .orElse(null));
        
        return Result.success(stats);
    }
}
