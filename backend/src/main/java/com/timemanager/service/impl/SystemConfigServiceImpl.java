package com.timemanager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timemanager.entity.SystemConfig;
import com.timemanager.mapper.SystemConfigMapper;
import com.timemanager.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统配置Service实现
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {
    
    @Autowired
    private SystemConfigMapper systemConfigMapper;
    
    @Override
    public String getConfigValue(String configKey) {
        return systemConfigMapper.getConfigValue(configKey);
    }
    
    @Override
    public String getConfigValueOrDefault(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }
    
    @Override
    public boolean updateConfig(String configKey, String configValue) {
        SystemConfig config = this.getOne(
            new QueryWrapper<SystemConfig>()
                .eq("config_key", configKey)
        );
        
        if (config != null) {
            config.setConfigValue(configValue);
            return this.updateById(config);
        }
        return false;
    }
}
