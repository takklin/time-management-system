package com.timemanager.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timemanager.common.result.Result;
import com.timemanager.entity.DbBackup;
import com.timemanager.service.DbBackupService;
import com.timemanager.service.BackupManagerService;
import com.timemanager.util.UserUtil;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;


/**
 * 数据库备份管理Controller
 */
@RestController
@RequestMapping("/api/v1/admin/backup")
public class DbBackupController {
    private static final Logger logger = LoggerFactory.getLogger(DbBackupController.class);
    
    @Autowired
    private DbBackupService dbBackupService;

    @Autowired
    private BackupManagerService backupManagerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 获取备份列表（分页）
     */
    @GetMapping("/list")
    public Result<IPage<DbBackup>> getBackupList(
        @RequestParam(defaultValue = "1") Integer current,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        try {
            Page<DbBackup> page = new Page<>(current, size);
            IPage<DbBackup> result = dbBackupService.page(
                page,
                new QueryWrapper<DbBackup>().orderByDesc("created_at")
            );
            // 补齐文件大小：如果数据库中为 null 或 0，则尝试读取文件系统实际大小并更新记录
            try {
                List<DbBackup> recs = result.getRecords();
                for (DbBackup b : recs) {
                    try {
                        Long fs = b.getFileSize();
                        if (fs == null || fs == 0) {
                            String bf = b.getBackupFile();
                            if (bf != null && !bf.isBlank()) {
                                java.nio.file.Path p = java.nio.file.Path.of(bf);
                                if (!p.isAbsolute()) {
                                    p = java.nio.file.Path.of(System.getProperty("user.dir")).resolve(bf).normalize();
                                }
                                if (java.nio.file.Files.exists(p)) {
                                    long real = java.nio.file.Files.size(p);
                                    b.setFileSize(real);
                                    // 同步回数据库以保持一致
                                    try { dbBackupService.updateById(b); } catch (Exception ignored) {}
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        // 忽略单条记录的读取错误，继续处理其它记录
                    }
                }
            } catch (Exception ignored) {}
            return Result.success(result);
        } catch (Exception ex) {
            logger.error("getBackupList failed", ex);
            // 如果是缺表导致，尝试创建表并返回空分页
            try {
                String msg = ex.getMessage() == null ? "" : ex.getMessage();
                if (msg.contains("db_backup") && msg.contains("doesn't exist")) {
                    logger.info("db_backup 表不存在，尝试创建表...");
                    String ddl = "CREATE TABLE IF NOT EXISTS `db_backup` (" +
                        "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                        "`backup_name` VARCHAR(255), " +
                        "`backup_file` TEXT, " +
                        "`backup_type` VARCHAR(50), " +
                        "`file_size` BIGINT, " +
                        "`status` VARCHAR(50), " +
                        "`backup_time` DATETIME, " +
                        "`created_at` DATETIME, " +
                        "`updated_at` DATETIME, " +
                        "PRIMARY KEY (`id`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                    jdbcTemplate.execute(ddl);
                    Page<DbBackup> empty = new Page<>(current, size);
                    empty.setTotal(0);
                    empty.setRecords(List.of());
                    return Result.success(empty);
                }
            } catch (Exception e2) {
                logger.error("尝试创建 db_backup 表失败", e2);
            }

            return Result.error(500, "获取备份列表失败: " + ex.getMessage());
        }
    }
    
    /**
     * 创建备份
     */
    @PostMapping("/create")
    public Result<DbBackup> createBackup(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        try {
            String format = body != null && body.get("format") != null ? String.valueOf(body.get("format")) : "json";
            List<String> tables = null;
            if (body != null && body.get("tables") instanceof List) {
                tables = (List<String>) body.get("tables");
            }

            String operator = UserUtil.getCurrentUsername();
            String ip = request != null ? (request.getHeader("X-Forwarded-For")==null?request.getRemoteAddr():request.getHeader("X-Forwarded-For")) : null;
            String ua = request != null ? request.getHeader("User-Agent") : null;

            DbBackup backup = backupManagerService.createBackup(format, tables, operator, ip, ua);
            return Result.success(backup);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "创建备份失败: " + e.getMessage());
        }
    }

    /**
     * 下载备份文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable Long id) {
        DbBackup b = dbBackupService.getById(id);
        if (b == null || b.getBackupFile() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path p = Path.of(b.getBackupFile());
            if (!Files.exists(p)) return ResponseEntity.notFound().build();
            byte[] data = Files.readAllBytes(p);
            String filename = p.getFileName().toString();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 上传并恢复备份（谨慎）
     */
    @PostMapping("/restore/upload")
    public Result<Map<String, Object>> restoreUpload(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(defaultValue = "false") boolean confirm,
                                                     HttpServletRequest request) {
        try {
            String operator = UserUtil.getCurrentUsername();
            String ip = request != null ? (request.getHeader("X-Forwarded-For")==null?request.getRemoteAddr():request.getHeader("X-Forwarded-For")) : null;
            String ua = request != null ? request.getHeader("User-Agent") : null;

            Map<String, Object> res = backupManagerService.restoreFromFile(file, confirm, operator, ip, ua);
            return Result.success(res);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "恢复失败: " + e.getMessage());
        }
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
