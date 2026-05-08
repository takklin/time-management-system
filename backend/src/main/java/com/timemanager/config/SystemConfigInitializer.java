package com.timemanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 在应用启动时确保 system_config 表存在，避免在首次访问时抛出 500。
 */
@Component
public class SystemConfigInitializer implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SystemConfigInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            logger.info("Ensuring system_config table exists...");
            String ddl = "CREATE TABLE IF NOT EXISTS `system_config` (" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                    "`config_key` VARCHAR(255) NOT NULL, " +
                    "`config_value` TEXT, " +
                    "`description` VARCHAR(255), " +
                    "`created_at` DATETIME, " +
                    "`updated_at` DATETIME, " +
                    "PRIMARY KEY (`id`), UNIQUE KEY `uk_config_key` (`config_key`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            jdbcTemplate.execute(ddl);
            logger.info("system_config table ensured.");
        } catch (Exception ex) {
            logger.warn("Failed to ensure system_config table: {}", ex.getMessage());
        }
    }
}
