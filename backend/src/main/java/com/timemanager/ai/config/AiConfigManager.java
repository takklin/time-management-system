package com.timemanager.ai.config;

import com.timemanager.entity.AiConfig;
import com.timemanager.mapper.AiConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 配置管理器
 * 负责动态加载、缓存和切换 AI 提供商配置
 * 支持零停机切换（无需重启应用）
 */
@Slf4j
@Component
public class AiConfigManager {
    
    @Autowired
    private AiConfigMapper aiConfigMapper;
    
    private volatile AiProperties currentConfig;
    
    /**
     * 获取当前激活的 AI 配置
     * 每次都从数据库检查，确保最新配置
     */
    public AiProperties getActiveConfig() {
        // 始终从数据库加载最新配置，避免缓存过期
        loadFromDB();
        return currentConfig;
    }
    
    /**
     * 从数据库加载激活的配置
     */
    public synchronized void loadFromDB() {
        QueryWrapper<AiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("is_active", 1);
        
        try {
            AiConfig config = aiConfigMapper.selectOne(wrapper);
            if (config != null) {
                currentConfig = new AiProperties(config);
                log.info("[AI] 已加载配置: provider={}, model={}", config.getProvider(), config.getModel());
            } else {
                log.warn("[AI] 没有找到激活的AI配置");
            }
        } catch (Exception e) {
            // 处理多个激活配置的异常情况
            log.error("[AI] 配置加载异常：可能存在多个激活配置，正在修复...");
            fixDuplicateActiveConfigs();
            // 递归重试一次
            QueryWrapper<AiConfig> retryWrapper = new QueryWrapper<>();
            retryWrapper.eq("is_active", 1);
            AiConfig config = aiConfigMapper.selectOne(retryWrapper);
            if (config != null) {
                currentConfig = new AiProperties(config);
                log.info("[AI] 已修复并加载配置: provider={}, model={}", config.getProvider(), config.getModel());
            }
        }
    }
    
    /**
     * 修复重复的激活配置
     * 禁用所有配置，然后激活 deepseek（或第一个可用配置）
     */
    private void fixDuplicateActiveConfigs() {
        try {
            // 1. 禁用所有配置
            QueryWrapper<AiConfig> disableAllWrapper = new QueryWrapper<>();
            disableAllWrapper.eq("is_active", 1);
            
            AiConfig disableAllConfig = new AiConfig();
            disableAllConfig.setIsActive(0);
            aiConfigMapper.update(disableAllConfig, disableAllWrapper);
            
            // 2. 优先激活 deepseek，否则激活第一个
            QueryWrapper<AiConfig> deepseekWrapper = new QueryWrapper<>();
            deepseekWrapper.eq("provider", "deepseek");
            AiConfig deepseekConfig = aiConfigMapper.selectOne(deepseekWrapper);
            
            if (deepseekConfig != null) {
                deepseekConfig.setIsActive(1);
                aiConfigMapper.updateById(deepseekConfig);
                log.info("[AI] 已激活 deepseek 配置");
            } else {
                // 激活第一个配置
                List<AiConfig> allConfigs = aiConfigMapper.selectList(null);
                if (!allConfigs.isEmpty()) {
                    AiConfig firstConfig = allConfigs.get(0);
                    firstConfig.setIsActive(1);
                    aiConfigMapper.updateById(firstConfig);
                    log.info("[AI] 已激活第一个配置: {}", firstConfig.getProvider());
                }
            }
        } catch (Exception e) {
            log.error("[AI] 配置修复失败", e);
        }
    }
    
    /**
     * 切换到指定的提供商
     * 原子操作：禁用其他，激活新配置
     */
    public synchronized void switchTo(String provider) {
        try {
            // 1. 查找目标配置
            QueryWrapper<AiConfig> findWrapper = new QueryWrapper<>();
            findWrapper.eq("provider", provider);
            AiConfig newConfig = aiConfigMapper.selectOne(findWrapper);
            
            if (newConfig == null) {
                log.error("[AI] 配置不存在: provider={}", provider);
                return;
            }
            
            // 2. 禁用所有其他配置
            QueryWrapper<AiConfig> updateWrapper = new QueryWrapper<>();
            updateWrapper.eq("is_active", 1);
            
            AiConfig disableConfig = new AiConfig();
            disableConfig.setIsActive(0);
            disableConfig.setUpdatedAt(LocalDateTime.now());
            aiConfigMapper.update(disableConfig, updateWrapper);
            
            // 3. 激活新配置
            newConfig.setIsActive(1);
            newConfig.setUpdatedAt(LocalDateTime.now());
            aiConfigMapper.updateById(newConfig);
            
            // 4. 更新内存缓存
            currentConfig = new AiProperties(newConfig);
            
            log.info("[AI] 已切换到: provider={}, model={}", provider, newConfig.getModel());
        } catch (Exception e) {
            log.error("[AI] 切换配置失败", e);
        }
    }
    
    /**
     * 列出所有可用的配置
     */
    public List<AiConfig> listAll() {
        return aiConfigMapper.selectList(null);
    }
    
    /**
     * 获取指定提供商的配置（不激活）
     */
    public AiProperties getConfigByProvider(String provider) {
        QueryWrapper<AiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("provider", provider);
        AiConfig config = aiConfigMapper.selectOne(wrapper);
        return config != null ? new AiProperties(config) : null;
    }
    
    /**
     * 更新配置（不改变激活状态）
     */
    public synchronized void updateConfig(Long id, AiConfig config) {
        config.setId(id);
        config.setUpdatedAt(LocalDateTime.now());
        aiConfigMapper.updateById(config);
        
        // 如果更新的是当前激活配置，需要重新加载
        if (currentConfig != null && currentConfig.provider.equals(config.getProvider())) {
            loadFromDB();
        }
    }
    
    /**
     * AI 配置属性封装
     */
    @Data
    @AllArgsConstructor
    public static class AiProperties {
        private String provider;       // deepseek / chatanywhere
        private String apiKey;         // API密钥
        private String baseUrl;        // 请求地址
        private String model;          // 模型名称
        private Integer maxTokens;     // 最大tokens
        private Double temperature;    // 温度参数
        
        public AiProperties(AiConfig config) {
            this.provider = config.getProvider();
            this.apiKey = config.getApiKey();
            this.baseUrl = config.getBaseUrl();
            this.model = config.getModel();
            this.maxTokens = config.getMaxTokens();
            this.temperature = config.getTemperature() != null 
                ? config.getTemperature().doubleValue() 
                : 0.7;
        }
    }
}
