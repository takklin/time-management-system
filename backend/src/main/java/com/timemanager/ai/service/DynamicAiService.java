package com.timemanager.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timemanager.ai.config.AiConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.time.LocalDateTime;

import com.timemanager.entity.AiCallLog;
import com.timemanager.mapper.AiCallLogMapper;

/**
 * 动态 AI 服务
 * 所有 AI 调用的唯一入口
 * 自动使用当前激活的提供商配置
 */
@Slf4j
@Service
public class DynamicAiService {
    
    @Autowired
    private AiConfigManager configManager;
    
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AiCallLogMapper aiCallLogMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 简单对话 - 返回完整回复
     * 每次调用都会检查当前激活的配置
     */
    public String chat(String systemPrompt, String userMessage) {
        long startTime = System.currentTimeMillis();
        
        try {
            AiConfigManager.AiProperties props = configManager.getActiveConfig();
            
            if (props == null) {
                log.error("[AI] 没有可用的AI配置");
                return "AI配置未初始化，请联系管理员";
            }
            
            log.info("[AI] 加载的配置: provider={}, model={}, apiKey长度={}, baseUrl={}", 
                props.getProvider(), props.getModel(), 
                props.getApiKey() != null ? props.getApiKey().length() : 0,
                props.getBaseUrl());
            log.debug("[AI] 使用提供商: {}, 模型: {}", props.getProvider(), props.getModel());
            
            // 构建请求
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", props.getModel());
            requestBody.put("temperature", props.getTemperature());
            requestBody.put("max_tokens", props.getMaxTokens());
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);
            
            // HTTP 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + props.getApiKey());
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
            
            // 构建 API URL（baseUrl 已包含 /v1）
            String apiUrl = props.getBaseUrl();
            if (!apiUrl.endsWith("/")) {
                apiUrl += "/";
            }
            apiUrl += "chat/completions";
            
            log.debug("[AI] 请求 URL: {}", apiUrl);
            log.debug("[AI] 请求体: {}", requestJson);
            
            // 使用postForEntity获取完整响应，支持各种API返回格式
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            
            log.debug("[AI] 响应状态: {}", response.getStatusCode());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map body = response.getBody();
                if (body != null) {
                    // 尝试解析 usage
                    Integer promptTokens = null;
                    Integer completionTokens = null;
                    Integer totalTokens = null;
                    try {
                        Object usageObj = body.get("usage");
                        if (usageObj instanceof Map) {
                            Map usage = (Map) usageObj;
                            promptTokens = usage.get("prompt_tokens") != null ? ((Number)usage.get("prompt_tokens")).intValue() : null;
                            completionTokens = usage.get("completion_tokens") != null ? ((Number)usage.get("completion_tokens")).intValue() : null;
                            totalTokens = usage.get("total_tokens") != null ? ((Number)usage.get("total_tokens")).intValue() : null;
                        }
                    } catch (Exception ex) {
                        log.debug("[AI] 无法解析 usage 字段", ex);
                    }

                    List choices = (List) body.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map choice = (Map) choices.get(0);
                        Map message = (Map) choice.get("message");
                        if (message != null) {
                            String content = (String) message.get("content");

                            long elapsed = System.currentTimeMillis() - startTime;
                            log.info("[AI] 调用成功 - 耗时: {}ms, 提供商: {}", elapsed, props.getProvider());

                            // 保存调用日志（映射到现有 ai_call_log 表结构）
                            try {
                                AiCallLog logRecord = new AiCallLog();
                                logRecord.setProvider(props.getProvider());
                                logRecord.setModule(props.getModel());
                                logRecord.setAction("chat.completions");
                                logRecord.setPromptTokens(promptTokens);
                                logRecord.setCompletionTokens(completionTokens);
                                logRecord.setTotalTokens(totalTokens);
                                logRecord.setResponseTimeMs((int) elapsed);
                                logRecord.setStatus("success");
                                String respBodyStr = objectMapper.writeValueAsString(body);
                                logRecord.setErrorMessage(respBodyStr);
                                logRecord.setCreatedAt(LocalDateTime.now());
                                aiCallLogMapper.insert(logRecord);
                            } catch (Exception ex) {
                                log.warn("[AI] 保存调用日志失败", ex);
                            }

                            return content;
                        }
                    }
                }
            }
            
            log.warn("[AI] 返回异常状态码: {}", response.getStatusCode());
            return "AI返回了异常响应，状态码: " + response.getStatusCode();
            
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[AI] 调用失败 - 耗时: {}ms", elapsed, e);
            // 记录错误日志到数据库
            try {
                AiConfigManager.AiProperties props = configManager.getActiveConfig();
                AiCallLog logRecord = new AiCallLog();
                if (props != null) {
                    logRecord.setProvider(props.getProvider());
                    logRecord.setModule(props.getModel());
                }
                logRecord.setAction("chat.completions");
                logRecord.setResponseTimeMs((int) elapsed);
                logRecord.setStatus("error");
                logRecord.setErrorMessage(e.getMessage());
                logRecord.setCreatedAt(LocalDateTime.now());
                aiCallLogMapper.insert(logRecord);
            } catch (Exception ex) {
                log.warn("[AI] 保存错误调用日志失败", ex);
            }

            return "抱歉，AI暂时无法响应，请稍后重试（错误: " + e.getMessage() + "）";
        }
    }
    
    /**
     * 对话 - 指定提供商
     * 与 chat() 相同，但可以指定使用哪个 AI 提供商（模型）
     * @param systemPrompt 系统提示
     * @param userMessage 用户消息
     * @param provider 指定的提供商 (e.g., "chatgpt3.5", "deepseek")，为空时使用当前激活的配置
     */
    public String chat(String systemPrompt, String userMessage, String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            // 为空则使用默认激活配置
            return chat(systemPrompt, userMessage);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 根据 provider 获取对应的配置
            AiConfigManager.AiProperties props = configManager.getConfigByProvider(provider);
            
            if (props == null) {
                log.error("[AI] 指定的提供商配置不存在: {}", provider);
                return "指定的AI提供商配置不存在: " + provider;
            }
            
            log.info("[AI] 使用指定提供商 - provider={}, model={}", props.getProvider(), props.getModel());
            log.debug("[AI] 使用提供商: {}, 模型: {}", props.getProvider(), props.getModel());
            
            // 构建请求（同原方法）
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", props.getModel());
            requestBody.put("temperature", props.getTemperature());
            requestBody.put("max_tokens", props.getMaxTokens());
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);
            
            // HTTP 请求
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
            
            log.debug("[AI] 请求 URL: {}", apiUrl);
            
            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map body = response.getBody();
                if (body != null) {
                    // 解析 usage（如果有）
                    Integer promptTokens = null;
                    Integer completionTokens = null;
                    Integer totalTokens = null;
                    try {
                        Object usageObj = body.get("usage");
                        if (usageObj instanceof Map) {
                            Map usage = (Map) usageObj;
                            promptTokens = usage.get("prompt_tokens") != null ? ((Number)usage.get("prompt_tokens")).intValue() : null;
                            completionTokens = usage.get("completion_tokens") != null ? ((Number)usage.get("completion_tokens")).intValue() : null;
                            totalTokens = usage.get("total_tokens") != null ? ((Number)usage.get("total_tokens")).intValue() : null;
                        }
                    } catch (Exception ex) {
                        log.debug("[AI] 无法解析 usage 字段", ex);
                    }

                    List choices = (List) body.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map choice = (Map) choices.get(0);
                        Map message = (Map) choice.get("message");
                        if (message != null) {
                            String content = (String) message.get("content");

                            long elapsed = System.currentTimeMillis() - startTime;
                            log.info("[AI] 调用成功 - 耗时: {}ms, 提供商: {}", elapsed, props.getProvider());

                            // 保存调用日志
                            try {
                                AiCallLog logRecord = new AiCallLog();
                                logRecord.setProvider(props.getProvider());
                                logRecord.setModule(props.getModel());
                                logRecord.setAction("chat.completions");
                                logRecord.setPromptTokens(promptTokens);
                                logRecord.setCompletionTokens(completionTokens);
                                logRecord.setTotalTokens(totalTokens);
                                logRecord.setResponseTimeMs((int) elapsed);
                                logRecord.setStatus("success");
                                String respBodyStr = objectMapper.writeValueAsString(body);
                                logRecord.setErrorMessage(respBodyStr);
                                logRecord.setCreatedAt(LocalDateTime.now());
                                aiCallLogMapper.insert(logRecord);
                            } catch (Exception ex) {
                                log.warn("[AI] 保存调用日志失败", ex);
                            }

                            return content;
                        }
                    }
                }
            }
            
            log.warn("[AI] 返回异常状态码: {}", response.getStatusCode());
            return "AI返回了异常响应，状态码: " + response.getStatusCode();
            
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[AI] 调用失败 - 耗时: {}ms, 指定提供商: {}", elapsed, provider, e);
            // 记录错误日志
            try {
                AiCallLog logRecord = new AiCallLog();
                logRecord.setProvider(provider);
                logRecord.setAction("chat.completions");
                logRecord.setResponseTimeMs((int) elapsed);
                logRecord.setStatus("error");
                logRecord.setErrorMessage(e.getMessage());
                logRecord.setCreatedAt(LocalDateTime.now());
                aiCallLogMapper.insert(logRecord);
            } catch (Exception ex) {
                log.warn("[AI] 保存错误调用日志失败", ex);
            }

            return "抱歉，AI暂时无法响应，请稍后重试（错误: " + e.getMessage() + "）";
        }
    }

    /**
     * 与模型对话（带历史消息列表） - 指定提供商
     * @param systemPrompt 系统提示
     * @param chatHistory 已格式化的消息列表，每项包含 role/content
     * @param provider 指定提供商（可为 null 使用默认）
     */
    public String chat(String systemPrompt, List<Map<String, String>> chatHistory, String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            // 使用默认激活配置
            return chat(systemPrompt, chatHistory);
        }

        long startTime = System.currentTimeMillis();

        try {
            AiConfigManager.AiProperties props = configManager.getConfigByProvider(provider);

            if (props == null) {
                log.error("[AI] 指定的提供商配置不存在: {}", provider);
                return "指定的AI提供商配置不存在: " + provider;
            }

            log.info("[AI] 使用指定提供商 - provider={}, model={}", props.getProvider(), props.getModel());
            log.debug("[AI] 使用提供商: {}, 模型: {}", props.getProvider(), props.getModel());

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", props.getModel());
            requestBody.put("temperature", props.getTemperature());
            requestBody.put("max_tokens", props.getMaxTokens());

            List<Map<String, String>> messages = new ArrayList<>();
            // 系统提示作为首条 system 消息
            messages.add(Map.of("role", "system", "content", systemPrompt));
            if (chatHistory != null && !chatHistory.isEmpty()) {
                for (Map<String, String> m : chatHistory) {
                    // 保证 role 和 content 存在
                    String role = m.getOrDefault("role", "user");
                    String content = m.getOrDefault("content", "");
                    messages.add(Map.of("role", role, "content", content));
                }
            }

            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + props.getApiKey());

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

            String apiUrl = props.getBaseUrl();
            if (!apiUrl.endsWith("/")) apiUrl += "/";
            apiUrl += "chat/completions";

            log.debug("[AI] 请求 URL: {}", apiUrl);
            log.debug("[AI] 请求体: {}", requestJson);

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map body = response.getBody();
                if (body != null) {
                    Integer promptTokens = null;
                    Integer completionTokens = null;
                    Integer totalTokens = null;
                    try {
                        Object usageObj = body.get("usage");
                        if (usageObj instanceof Map) {
                            Map usage = (Map) usageObj;
                            promptTokens = usage.get("prompt_tokens") != null ? ((Number)usage.get("prompt_tokens")).intValue() : null;
                            completionTokens = usage.get("completion_tokens") != null ? ((Number)usage.get("completion_tokens")).intValue() : null;
                            totalTokens = usage.get("total_tokens") != null ? ((Number)usage.get("total_tokens")).intValue() : null;
                        }
                    } catch (Exception ex) { log.debug("[AI] 无法解析 usage 字段", ex); }

                    List choices = (List) body.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map choice = (Map) choices.get(0);
                        Map message = (Map) choice.get("message");
                        if (message != null) {
                            String content = (String) message.get("content");
                            long elapsed = System.currentTimeMillis() - startTime;
                            log.info("[AI] 调用成功 - 耗时: {}ms, 提供商: {}", elapsed, props.getProvider());

                            try {
                                AiCallLog logRecord = new AiCallLog();
                                logRecord.setProvider(props.getProvider());
                                logRecord.setModule(props.getModel());
                                logRecord.setAction("chat.completions");
                                logRecord.setPromptTokens(promptTokens);
                                logRecord.setCompletionTokens(completionTokens);
                                logRecord.setTotalTokens(totalTokens);
                                logRecord.setResponseTimeMs((int) elapsed);
                                logRecord.setStatus("success");
                                String respBodyStr = objectMapper.writeValueAsString(body);
                                logRecord.setErrorMessage(respBodyStr);
                                logRecord.setCreatedAt(LocalDateTime.now());
                                aiCallLogMapper.insert(logRecord);
                            } catch (Exception ex) { log.warn("[AI] 保存调用日志失败", ex); }

                            return content;
                        }
                    }
                }
            }

            log.warn("[AI] 返回异常状态码: {}", response.getStatusCode());
            return "AI返回了异常响应，状态码: " + response.getStatusCode();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[AI] 调用失败 - 耗时: {}ms, 指定提供商: {}", elapsed, provider, e);
            try {
                AiCallLog logRecord = new AiCallLog();
                logRecord.setProvider(provider);
                logRecord.setAction("chat.completions");
                logRecord.setResponseTimeMs((int) elapsed);
                logRecord.setStatus("error");
                logRecord.setErrorMessage(e.getMessage());
                logRecord.setCreatedAt(LocalDateTime.now());
                aiCallLogMapper.insert(logRecord);
            } catch (Exception ex) { log.warn("[AI] 保存错误调用日志失败", ex); }

            return "抱歉，AI暂时无法响应，请稍后重试（错误: " + e.getMessage() + ")";
        }
    }

    /**
     * 与模型对话（带历史消息列表） - 使用默认提供商
     */
    public String chat(String systemPrompt, List<Map<String, String>> chatHistory) {
        AiConfigManager.AiProperties props = configManager.getActiveConfig();
        if (props == null) {
            log.error("[AI] 没有可用的AI配置");
            return "AI配置未初始化，请联系管理员";
        }
        return chat(systemPrompt, chatHistory, props.getProvider());
    }
    
    /**
     * 仅获取当前配置信息（用于测试连接等）
     */
    public AiConfigManager.AiProperties getCurrentConfig() {
        return configManager.getActiveConfig();
    }
}
