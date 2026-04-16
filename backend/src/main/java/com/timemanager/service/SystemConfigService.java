package com.timemanager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.timemanager.entity.SystemConfig;

/**
 * 系统配置Service
 */
public interface SystemConfigService extends IService<SystemConfig> {
    
    /**
     * 获取配置值
     */
    String getConfigValue(String configKey);
    
    /**
     * 获取配置值，如果不存在返回默认值
     */
    String getConfigValueOrDefault(String configKey, String defaultValue);
    
    /**
     * 更新配置
     */
    boolean updateConfig(String configKey, String configValue);
}
