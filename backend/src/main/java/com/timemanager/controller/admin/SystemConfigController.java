package com.timemanager.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timemanager.common.result.Result;
import com.timemanager.entity.SystemConfig;
import com.timemanager.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置管理Controller
 */
@RestController
@RequestMapping("/api/v1/admin/config")
public class SystemConfigController {
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    /**
     * 获取所有配置
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> getAllConfigs() {
        Map<String, Object> configMap = new HashMap<>();
        systemConfigService.list().forEach(config -> 
            configMap.put(config.getConfigKey(), config.getConfigValue())
        );
        return Result.success(configMap);
    }
    
    /**
     * 获取单个配置
     */
    @GetMapping("/{configKey}")
    public Result<SystemConfig> getConfig(@PathVariable String configKey) {
        SystemConfig config = systemConfigService.getOne(
            new QueryWrapper<SystemConfig>()
                .eq("config_key", configKey)
        );
        if (config != null) {
            return Result.success(config);
        }
        return Result.error(404, "配置不存在");
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/{configKey}")
    public Result<Boolean> updateConfig(
        @PathVariable String configKey,
        @RequestBody Map<String, String> request
    ) {
        String configValue = request.get("configValue");
        if (configValue == null) {
            return Result.error(400, "配置值不能为空");
        }
        
        boolean result = systemConfigService.updateConfig(configKey, configValue);
        if (result) {
            return Result.success(true);
        }
        return Result.error(500, "更新失败");
    }
    
    /**
     * 添加新配置
     */
    @PostMapping
    public Result<SystemConfig> createConfig(@RequestBody SystemConfig config) {
        if (config.getConfigKey() == null || config.getConfigKey().trim().isEmpty()) {
            return Result.error(400, "配置键不能为空");
        }
        
        boolean saved = systemConfigService.save(config);
        if (saved) {
            return Result.success(config);
        }
        return Result.error(500, "创建失败");
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/{configKey}")
    public Result<Boolean> deleteConfig(@PathVariable String configKey) {
        boolean result = systemConfigService.remove(
            new QueryWrapper<SystemConfig>()
                .eq("config_key", configKey)
        );
        if (result) {
            return Result.success(true);
        }
        return Result.error(500, "删除失败");
    }
}
