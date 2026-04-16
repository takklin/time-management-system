package com.timemanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 禁用 JPA Repository 扫描（项目使用 MyBatis Plus）
 * 通过设置 basePackages 为空来禁用 JPA 的 @EnableJpaRepositories
 */
@Configuration
@EnableJpaRepositories(basePackages = {})
public class JpaDisableConfig {
    // 禁用 JPA 的自动扫描，避免与 MyBatis Plus 冲突
}
