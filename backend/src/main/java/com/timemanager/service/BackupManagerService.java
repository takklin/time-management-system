package com.timemanager.service;

import com.timemanager.entity.DbBackup;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BackupManagerService {
    DbBackup createBackup(String format, List<String> tables, String operator, String ip, String userAgent) throws Exception;

    Map<String, Object> restoreFromFile(MultipartFile file, boolean confirm, String operator, String ip, String userAgent) throws Exception;
}
