package com.timemanager.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timemanager.entity.DbBackup;
import com.timemanager.mapper.DbBackupMapper;
import com.timemanager.mapper.OperationLogMapper;
import com.timemanager.mapper.TaskMapper;
import com.timemanager.mapper.TimeRecordMapper;
import com.timemanager.mapper.UserMapper;
import com.timemanager.service.BackupManagerService;
import com.timemanager.service.DbBackupService;
import com.timemanager.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 备份管理实现（导出 / 恢复）
 */
@Service
public class BackupManagerServiceImpl implements BackupManagerService {

    @Value("${app.backup-dir:./backups}")
    private String backupDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DbBackupService dbBackupService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TimeRecordMapper timeRecordMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private OperationLogService operationLogService;

    private static final List<String> DEFAULT_TABLES = List.of("user", "task", "time_record", "operation_log");

    @Override
    public DbBackup createBackup(String format, List<String> tables, String operator, String ip, String userAgent) throws Exception {
        if (format == null || (!format.equalsIgnoreCase("json") && !format.equalsIgnoreCase("sql"))) {
            format = "json";
        }
        if (tables == null || tables.isEmpty()) {
            tables = DEFAULT_TABLES;
        }

        Path dir = Path.of(backupDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("backup_%s_%s.%s", format.toLowerCase(), ts, format.equalsIgnoreCase("json") ? "json" : "sql");
        Path file = dir.resolve(filename);

        if (format.equalsIgnoreCase("json")) {
            Map<String, Object> export = new LinkedHashMap<>();
            for (String t : tables) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + t + "`");
                export.put(t, rows);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), export);
        } else {
            StringBuilder sb = new StringBuilder();
            for (String t : tables) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + t + "`");
                for (Map<String, Object> row : rows) {
                    StringBuilder cols = new StringBuilder();
                    StringBuilder vals = new StringBuilder();
                    boolean first = true;
                    for (Map.Entry<String, Object> e : row.entrySet()) {
                        if (!first) { cols.append(", "); vals.append(", "); }
                        cols.append("`" + e.getKey() + "`");
                        vals.append(toSqlValue(e.getValue()));
                        first = false;
                    }
                    sb.append("INSERT INTO `").append(t).append("` (").append(cols).append(") VALUES (").append(vals).append(");\n");
                }
                sb.append("\n");
            }
            Files.writeString(file, sb.toString());
        }

        long size = Files.size(file);
        DbBackup backup = new DbBackup();
        backup.setBackupName(filename);
        backup.setBackupFile(file.toString());
        backup.setBackupType("full");
        backup.setFileSize(size);
        backup.setStatus("success");
        backup.setBackupTime(LocalDateTime.now());
        backup.setCreatedAt(LocalDateTime.now());
        dbBackupService.save(backup);

        // 记录导出操作日志
        try {
            operationLogService.recordOperation(operator, "EXPORT_BACKUP", "backup:" + filename, "SUCCESS");
        } catch (Exception ignored) {}

        return backup;
    }

    private String toSqlValue(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number) return v.toString();
        if (v instanceof Boolean) return ((Boolean) v) ? "1" : "0";
        // LocalDate/LocalDateTime etc
        if (v instanceof java.time.LocalDate || v instanceof java.time.LocalDateTime) {
            return "'" + v.toString() + "'";
        }
        String s = v.toString().replace("'", "''");
        return "'" + s + "'";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> restoreFromFile(MultipartFile file, boolean confirm, String operator, String ip, String userAgent) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("文件为空");

        // 保存到临时文件
        Path tmp = Files.createTempFile("restore_", "");
        try (InputStream in = file.getInputStream(); FileOutputStream out = new FileOutputStream(tmp.toFile())) {
            in.transferTo(out);
        }

        String name = file.getOriginalFilename() == null ? tmp.getFileName().toString() : file.getOriginalFilename();
        String lower = name.toLowerCase();
        Map<String, Object> res = new HashMap<>();

        try {
            if (lower.endsWith(".json")) {
                Map<String, List<Map<String, Object>>> data = objectMapper.readValue(tmp.toFile(), new TypeReference<>() {});
                if (!confirm) {
                    res.put("status", "need_confirm");
                    res.put("tables", data.keySet());
                    return res;
                }

                // 恢复每个表：先删除再插入
                for (Map.Entry<String, List<Map<String, Object>>> e : data.entrySet()) {
                    String table = e.getKey();
                    List<Map<String, Object>> rows = e.getValue();
                    jdbcTemplate.execute("DELETE FROM `" + table + "`");
                    for (Map<String, Object> row : rows) {
                        StringBuilder cols = new StringBuilder();
                        StringBuilder vals = new StringBuilder();
                        boolean first = true;
                        for (Map.Entry<String, Object> c : row.entrySet()) {
                            if (!first) { cols.append(", "); vals.append(", "); }
                            cols.append("`" + c.getKey() + "`");
                            vals.append(toSqlValue(c.getValue()));
                            first = false;
                        }
                        String sql = "INSERT INTO `" + table + "` (" + cols + ") VALUES (" + vals + ")";
                        jdbcTemplate.execute(sql);
                    }
                }
            } else if (lower.endsWith(".sql")) {
                String content = Files.readString(tmp);
                if (!confirm) {
                    res.put("status", "need_confirm");
                    res.put("estimatedStatements", content.split(";").length);
                    return res;
                }
                // 简单按分号拆分执行（注意：仅在受控文件中使用）
                String[] stmts = content.split(";\n|;\r\n|;");
                for (String s : stmts) {
                    String t = s.trim();
                    if (t.isEmpty()) continue;
                    jdbcTemplate.execute(t);
                }
            } else {
                throw new IllegalArgumentException("不支持的文件类型");
            }

            // 记录恢复操作为高危
            try {
                operationLogService.recordOperation(operator, "RESTORE_BACKUP", "file:" + name, "SUCCESS", ip, userAgent);
            } catch (Exception ignored) {}

            res.put("status", "success");
            return res;
        } catch (Exception ex) {
            // 记录失败
            try { operationLogService.recordOperation(operator, "RESTORE_BACKUP", "file:" + name, "FAILED", ip, userAgent); } catch (Exception ignored) {}
            throw ex;
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }
}
