package com.timemanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 在应用启动时确保 db_backup 表存在，避免运行时 500 错误。
 */
@Component
public class DbBackupInitializer implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DbBackupInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            logger.info("Ensuring db_backup table exists...");
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
            logger.info("db_backup table ensured.");
        } catch (Exception ex) {
            logger.warn("Failed to ensure db_backup table: {}", ex.getMessage());
        }
    }
}
