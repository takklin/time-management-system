package com.timemanager.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * AI 提供商配置表
 * 支持 DeepSeek 和 ChatAnywhere 两个提供商的动态切换
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_config")
public class AiConfig {
    
    private Long id;
    
    /**
     * 提供商: deepseek / chatanywhere
     */
    private String provider;
    
    /**
     * API 密钥
     */
    private String apiKey;
    
    /**
     * 请求基础地址
     */
    private String baseUrl;
    
    /**
     * 模型名称: deepseek-chat / gpt-3.5-turbo
     */
    private String model;
    
    /**
     * 是否激活 (0=未激活, 1=活跃)
     * 同一时刻仅有一个配置处于激活状态
     */
    private Integer isActive;
    
    /**
     * 单次最大 tokens
     */
    private Integer maxTokens;
    
    /**
     * 温度参数 (0.0-1.0)
     * 0=确定性, 1=随机性
     */
    private BigDecimal temperature;
    
    /**
     * 配置描述
     */
    private String description;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
