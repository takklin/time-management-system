package com.timemanager.controller;

import com.timemanager.ai.config.AiConfigManager;
import com.timemanager.entity.AiConfig;
import com.timemanager.mapper.AiConfigMapper;
import com.timemanager.common.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 配置管理控制器
 * 提供管理员管理 AI 提供商配置的接口
 */
@RestController
@RequestMapping("/api/v1/admin/ai-config")
public class AdminAiConfigController {
    
    @Autowired
    private AiConfigManager configManager;
    
    @Autowired
    private AiConfigMapper aiConfigMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 获取所有 AI 配置
     * GET /api/v1/admin/ai-config/list
     */
    @GetMapping("/list")
    public Result<List<AiConfig>> listConfigs() {
        List<AiConfig> configs = configManager.listAll();
        // 隐藏敏感的 API Key
        configs.forEach(c -> c.setApiKey("***" + c.getApiKey().substring(c.getApiKey().length() - 6)));
        return Result.success(configs);
    }
    
    /**
     * 切换到指定的 AI 提供商
     * POST /api/v1/admin/ai-config/switch/{provider}
     */
    @PostMapping("/switch/{provider}")
    public Result<Map<String, Object>> switchProvider(@PathVariable String provider) {
        try {
            configManager.switchTo(provider);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已切换到 " + provider);
            response.put("provider", provider);
            
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(500, "切换失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试连接
     * POST /api/v1/admin/ai-config/test-connection/{provider}
     */
    @PostMapping("/test-connection/{provider}")
    public Result<Map<String, Object>> testConnection(@PathVariable String provider) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 先检查数据库中是否有任何配置
            List<AiConfig> allConfigs = aiConfigMapper.selectList(null);
            if (allConfigs == null || allConfigs.isEmpty()) {
                response.put("success", false);
                response.put("message", "数据库中没有AI配置，请先执行 ai_config_setup.sql 初始化");
                response.put("provider", provider);
                return Result.success(response);
            }
            
            // 2. 获取指定提供商的配置
            AiConfigManager.AiProperties props = configManager.getConfigByProvider(provider);
            
            if (props == null) {
                response.put("success", false);
                response.put("message", "配置不存在，可用配置:" + 
                    allConfigs.stream().map(AiConfig::getProvider).toList());
                response.put("provider", provider);
                response.put("availableProviders", 
                    allConfigs.stream().map(AiConfig::getProvider).toList());
                return Result.success(response);
            }
            
            // 3. 发送测试请求到 AI API
            testAiConnection(props);
            
            // 4. 测试成功，返回成功响应
            response.put("success", true);
            response.put("message", "连接成功");
            response.put("provider", props.getProvider());
            response.put("model", props.getModel());
            response.put("baseUrl", props.getBaseUrl());
            
            return Result.success(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "连接失败: " + e.getMessage());
            response.put("provider", provider);
            return Result.success(response);
        }
    }
    
    /**
     * 实际测试 AI API 连接
     */
    private void testAiConnection(AiConfigManager.AiProperties props) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 构建测试请求
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", props.getModel());
        requestBody.put("temperature", props.getTemperature());
        requestBody.put("max_tokens", 100);
        
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", "Hi");
        
        requestBody.put("messages", List.of(message));
        
        // 构建 HTTP 请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + props.getApiKey());
        
        String requestJson = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
        
        // 构建 API URL
        String apiUrl = props.getBaseUrl();
        if (!apiUrl.endsWith("/")) {
            apiUrl += "/";
        }
        apiUrl += "chat/completions";
        
        // 发送请求
        String responseJson = restTemplate.postForObject(apiUrl, request, String.class);
        
        // 验证响应
        JsonNode root = objectMapper.readTree(responseJson);
        if (root.has("error")) {
            throw new Exception(root.path("error").path("message").asText("Unknown error"));
        }
        if (!root.has("choices") || root.path("choices").size() == 0) {
            throw new Exception("Invalid response from AI API");
        }
    }
    
    /**
     * 更新 AI 配置
     * PUT /api/v1/admin/ai-config/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> updateConfig(@PathVariable Long id, @RequestBody AiConfig config) {
        try {
            configManager.updateConfig(id, config);
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前激活的配置
     * GET /api/v1/admin/ai-config/current
     */
    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrentConfig() {
        AiConfigManager.AiProperties props = configManager.getActiveConfig();
        
        Map<String, Object> response = new HashMap<>();
        response.put("provider", props.getProvider());
        response.put("model", props.getModel());
        response.put("maxTokens", props.getMaxTokens());
        response.put("temperature", props.getTemperature());
        
        return Result.success(response);
    }
}
