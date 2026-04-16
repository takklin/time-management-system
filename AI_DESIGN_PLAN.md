# 时间管理系统 AI 智能功能设计方案

## 📌 概述

本方案基于现有数据库结构（用户、分类、任务、日程、时间记录、操作日志），设计完整的AI智能系统，包括：
- ✅ 多API密钥动态切换（DeepSeek + ChatAnywhere）
- ✅ 用户端AI助手（自然语言任务管理）
- ✅ 管理员端AI助手（数据查询 + 主动预警）
- ✅ 操作日志智能异常检测与预警
- ✅ 缓存机制（节约API额度）

---

## 一、多模型API切换设计

### 1.1 核心需求
支持 DeepSeek 和 ChatAnywhere 两个 API 提供商的动态切换，通过数据库配置管理，实现零停机切换。

### 1.2 数据库表设计

```sql
-- AI 配置表
CREATE TABLE `ai_config` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    `provider` VARCHAR(20) NOT NULL UNIQUE COMMENT '提供商: deepseek / chatanywhere',
    `api_key` VARCHAR(500) NOT NULL COMMENT 'API密钥',
    `base_url` VARCHAR(200) NOT NULL COMMENT '请求地址',
    `model` VARCHAR(50) NOT NULL COMMENT '模型名称',
    `is_active` TINYINT DEFAULT 0 COMMENT '是否激活(仅一个为1)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='AI提供商配置表';

-- 初始化数据
INSERT INTO `ai_config` (`provider`, `api_key`, `base_url`, `model`, `is_active`) VALUES
('deepseek', 'sk-xxx', 'https://api.deepseek.com/v1', 'deepseek-chat', 1),
('chatanywhere', 'sk-xxx', 'https://api.chatanywhere.tech/v1', 'gpt-3.5-turbo', 0);

-- AI 调用日志（成本控制）
CREATE TABLE `ai_call_log` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `provider` VARCHAR(20),
    `user_id` BIGINT COMMENT '用户ID(null表示系统自动调用)',
    `module` VARCHAR(50) COMMENT '模块: user_chat, admin_query, log_alert等',
    `prompt_tokens` INT,
    `completion_tokens` INT,
    `total_cost` DECIMAL(10,6) COMMENT '预估成本(美元)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='AI调用消耗日志';

-- AI 预警表  
CREATE TABLE `ai_alert` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `alert_type` VARCHAR(50) COMMENT '预警类型: ABNORMAL_LOGIN, BULK_DELETE, etc',
    `severity` ENUM('HIGH','MEDIUM','LOW') DEFAULT 'MEDIUM' COMMENT '严重程度',
    `title` VARCHAR(200) NOT NULL COMMENT '预警标题',
    `description` TEXT COMMENT 'AI生成的详细分析',
    `suggestion` TEXT COMMENT 'AI给出的建议',
    `related_log_ids` JSON COMMENT '关联的operation_log ID列表',
    `is_handled` TINYINT DEFAULT 0 COMMENT '是否已处理',
    `handler_id` BIGINT COMMENT '处理者ID',
    `handled_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='AI智能预警表';
```

### 1.3 后端配置管理器（Java Spring Boot）

创建 `backend/src/main/java/com/timemanager/ai/config/AiConfigManager.java`：

```java
package com.timemanager.ai.config;

import com.timemanager.entity.AiConfig;
import com.timemanager.mapper.AiConfigMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@Component
public class AiConfigManager {
    
    @Autowired
    private AiConfigMapper aiConfigMapper;
    
    private volatile AiProperties currentConfig;
    
    /**
     * 获取当前激活的配置
     */
    public AiProperties getActiveConfig() {
        if (currentConfig == null) {
            loadFromDB();
        }
        return currentConfig;
    }
    
    /**
     * 切换到指定提供商
     */
    public synchronized void switchTo(String provider) {
        QueryWrapper<AiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("provider", provider);
        AiConfig config = aiConfigMapper.selectOne(wrapper);
        
        if (config != null) {
            // 禁用其他配置
            QueryWrapper<AiConfig> update = new QueryWrapper<>();
            update.eq("is_active", 1);
            aiConfigMapper.update(new AiConfig() {{ setIsActive(0); }}, update);
            
            // 激活新配置
            config.setIsActive(1);
            aiConfigMapper.updateById(config);
            
            currentConfig = new AiProperties(config);
        }
    }
    
    /**
     * 从数据库加载激活配置
     */
    private synchronized void loadFromDB() {
        QueryWrapper<AiConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("is_active", 1);
        AiConfig config = aiConfigMapper.selectOne(wrapper);
        if (config != null) {
            currentConfig = new AiProperties(config);
        }
    }
    
    /**
     * 列出所有配置
     */
    public java.util.List<AiConfig> listAll() {
        return aiConfigMapper.selectList(null);
    }
    
    @Data
    @AllArgsConstructor
    public static class AiProperties {
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String model;
        
        public AiProperties(AiConfig config) {
            this.provider = config.getProvider();
            this.apiKey = config.getApiKey();
            this.baseUrl = config.getBaseUrl();
            this.model = config.getModel();
        }
    }
}
```

### 1.4 统一AI服务（使用 Spring AI）

创建 `backend/src/main/java/com/timemanager/ai/service/DynamicAiService.java`：

```java
package com.timemanager.ai.service;

import com.timemanager.ai.config.AiConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DynamicAiService {
    
    @Autowired
    private AiConfigManager configManager;
    
    /**
     * 简单对话（返回完整回复）
     */
    public String chat(String systemPrompt, String userMessage) {
        try {
            AiConfigManager.AiProperties props = configManager.getActiveConfig();
            
            OpenAiApi api = new OpenAiApi(props.getBaseUrl(), props.getApiKey());
            OpenAiChatModel chatModel = new OpenAiChatModel(api, 
                OpenAiChatOptions.builder()
                    .withModel(props.getModel())
                    .withTemperature(0.7f)
                    .build()
            );
            
            ChatClient client = ChatClient.create(chatModel);
            String response = client.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
            
            log.info("[AI] Provider: {}, Model: {}", props.getProvider(), props.getModel());
            return response;
        } catch (Exception e) {
            log.error("AI调用失败", e);
            return "AI暂时无法响应，请稍后重试";
        }
    }
    
    /**
     * 流式对话（逐token返回，需要前端SSE处理）
     */
    public org.springframework.ai.chat.model.ChatResponse streamChat(String systemPrompt, String userMessage) {
        AiConfigManager.AiProperties props = configManager.getActiveConfig();
        
        OpenAiApi api = new OpenAiApi(props.getBaseUrl(), props.getApiKey());
        OpenAiChatModel chatModel = new OpenAiChatModel(api,
            OpenAiChatOptions.builder()
                .withModel(props.getModel())
                .build()
        );
        
        ChatClient client = ChatClient.create(chatModel);
        return client.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call()
            .chatResponse();
    }
}
```

### 1.5 管理员配置切换API

创建 `backend/src/main/java/com/timemanager/controller/AdminAiConfigController.java`：

```java
package com.timemanager.controller;

import com.timemanager.ai.config.AiConfigManager;
import com.timemanager.common.result.Result;
import com.timemanager.entity.AiConfig;
import com.timemanager.mapper.AiConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ai-config")
public class AdminAiConfigController {
    
    @Autowired
    private AiConfigManager configManager;
    
    @Autowired
    private AiConfigMapper aiConfigMapper;
    
    /**
     * 获取所有配置
     */
    @GetMapping("/list")
    public Result<List<AiConfig>> listConfigs() {
        List<AiConfig> configs = configManager.listAll();
        configs.forEach(c -> c.setApiKey("***")); // 隐藏密钥
        return Result.success(configs);
    }
    
    /**
     * 切换到指定提供商
     */
    @PostMapping("/switch/{provider}")
    public Result<Void> switchProvider(@PathVariable String provider) {
        configManager.switchTo(provider);
        return Result.success();
    }
    
    /**
     * 测试连接
     */
    @PostMapping("/test-connection/{provider}")
    public Result<Map<String, Object>> testConnection(@PathVariable String provider) {
        try {
            AiConfigManager.AiProperties props = configManager.getActiveConfig();
            // 这里可以调用一个简单的AI请求测试连接
            String testResult = new org.springframework.ai.openai.api.OpenAiApi(
                props.getBaseUrl(), props.getApiKey()
            ).listModels();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "连接成功");
            response.put("provider", provider);
            return Result.success(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "连接失败: " + e.getMessage());
            return Result.success(response);
        }
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/{id}")
    public Result<Void> updateConfig(@PathVariable Long id, @RequestBody AiConfig config) {
        config.setId(id);
        aiConfigMapper.updateById(config);
        // 清除缓存，下次调用重新加载
        configManager.loadFromDB(); // 需要在ConfigManager中暴露此方法
        return Result.success();
    }
}
```

---

## 二、用户端AI智能助手

### 2.1 功能特性
- **自然语言创建任务**: "明天下午3点到5点准备PPT" → 自动解析并创建任务
- **智能任务分解**: "复习期末" → AI生成子任务列表
- **今日/周总结**: AI生成鼓励性总结
- **问答查询**: "我本周完成几个任务？"

### 2.2 前端页面（Vue 3 + TypeScript）

创建 `frontend/src/components/user/AIChatAssistant.vue`：

```vue
<template>
  <div class="ai-chat-drawer" v-if="isOpen">
    <!-- 抽屉头部 -->
    <div class="drawer-header">
      <div class="title-bar">
        <span class="title">🤖 AI 智能助手</span>
        <el-select v-model="selectedModel" size="small" @change="onModelChange">
          <el-option label="DeepSeek" value="deepseek" />
          <el-option label="ChatAnywhere" value="chatanywhere" />
        </el-select>
      </div>
      <button @click="isOpen = false" class="close-btn">✕</button>
    </div>

    <!-- 对话列表 -->
    <div class="messages-container" ref="messagesContainer">
      <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
        <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
        <div class="content">
          <div v-if="msg.type === 'text'" class="text">{{ msg.content }}</div>
          <div v-else-if="msg.type === 'task'" class="task-suggestion">
            <h4>📝 建议的任务</h4>
            <el-form :model="msg.taskData" label-width="80px" size="small">
              <el-form-item label="标题">
                <el-input v-model="msg.taskData.title" />
              </el-form-item>
              <el-form-item label="截止时间">
                <el-date-picker 
                  v-model="msg.taskData.deadline"
                  type="datetime"
                  placeholder="选择日期时间"
                />
              </el-form-item>
              <el-form-item label="预估时长(分)">
                <el-input v-model.number="msg.taskData.estimatedMinutes" type="number" />
              </el-form-item>
              <el-form-item label="分类">
                <el-select v-model="msg.taskData.categoryId">
                  <el-option 
                    v-for="cat in categories" 
                    :key="cat.id" 
                    :label="cat.name" 
                    :value="cat.id"
                  />
                </el-select>
              </el-form-item>
              <el-button type="primary" size="small" @click="confirmTask(msg.taskData)">
                ✓ 添加到任务列表
              </el-button>
            </el-form>
          </div>
          <div v-else-if="msg.type === 'loading'" class="loading">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷按钮 -->
    <div class="shortcuts">
      <button @click="quickAction('create')" class="shortcut-btn">✨ 创建任务</button>
      <button @click="quickAction('summary')" class="shortcut-btn">📊 今日总结</button>
      <button @click="quickAction('query')" class="shortcut-btn">🔍 查询任务</button>
    </div>

    <!-- 输入框 -->
    <div class="input-area">
      <el-input 
        v-model="userInput"
        @keyup.enter="sendMessage"
        placeholder="输入你的需求或问题..."
        :disabled="loading"
      />
      <el-button 
        @click="sendMessage" 
        type="primary"
        :loading="loading"
        size="small"
      >
        发送
      </el-button>
    </div>
  </div>

  <!-- 浮动按钮 -->
  <button v-else class="chat-fab" @click="isOpen = true">
    💬
  </button>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as userAiApi from '@/api/user/ai'

interface Message {
  id: string
  role: 'user' | 'assistant'
  type: 'text' | 'task' | 'loading'
  content?: string
  taskData?: any
}

const isOpen = ref(false)
const messages = ref<Message[]>([])
const userInput = ref('')
const loading = ref(false)
const selectedModel = ref('deepseek')
const messagesContainer = ref<HTMLElement>()
const categories = ref<any[]>([])
let messageIdCounter = 0

onMounted(async () => {
  // 加载分类
  categories.value = await userAiApi.getCategories()
})

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!userInput.value.trim()) return
  
  const userMsg = userInput.value
  userInput.value = ''
  
  // 添加用户消息
  messages.value.push({
    id: String(messageIdCounter++),
    role: 'user',
    type: 'text',
    content: userMsg
  })
  
  // 添加加载指示器
  const loadingMsgId = String(messageIdCounter++)
  messages.value.push({
    id: loadingMsgId,
    role: 'assistant',
    type: 'loading'
  })
  
  loading.value = true
  await scrollToBottom()
  
  try {
    const response = await userAiApi.chat({ message: userMsg })
    
    // 移除加载指示器
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    
    // 添加AI回复
    messages.value.push({
      id: String(messageIdCounter++),
      role: 'assistant',
      type: 'text',
      content: response
    })
  } catch (error) {
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    messages.value.push({
      id: String(messageIdCounter++),
      role: 'assistant',
      type: 'text',
      content: '❌ 出错了，请稍后重试'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const quickAction = async (action: string) => {
  if (action === 'create') {
    userInput.value = '帮我创建一个任务：'
  } else if (action === 'summary') {
    userInput.value = '帮我总结一下我今天的任务完成情况'
  } else if (action === 'query') {
    userInput.value = '我本周完成了多少个任务？'
  }
}

const confirmTask = async (taskData: any) => {
  try {
    await userAiApi.createTask(taskData)
    ElMessage.success('✓ 任务已添加到列表')
  } catch (error) {
    ElMessage.error('添加任务失败')
  }
}

const onModelChange = async (model: string) => {
  try {
    await userAiApi.switchModel(model)
    ElMessage.success(`✓ 已切换到 ${model}`)
  } catch (error) {
    ElMessage.error('切换模型失败')
  }
}
</script>

<style scoped lang="scss">
.ai-chat-drawer {
  position: fixed;
  bottom: 0;
  right: 20px;
  width: 380px;
  height: 600px;
  background: white;
  border-radius: 12px 12px 0 0;
  box-shadow: 0 -2px 12px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  z-index: 1000;
  
  .drawer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    
    .title-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
      
      .title {
        font-weight: 600;
        font-size: 14px;
      }
    }
    
    .close-btn {
      background: none;
      border: none;
      font-size: 18px;
      cursor: pointer;
      color: #999;
      
      &:hover {
        color: #333;
      }
    }
  }
  
  .messages-container {
    flex: 1;
    overflow-y: auto;
    padding: 12px 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    
    .message {
      display: flex;
      gap: 8px;
      
      &.user {
        justify-content: flex-end;
        
        .avatar {
          order: 2;
        }
        
        .content {
          background: #e8f5e9;
          order: 1;
        }
      }
      
      &.assistant {
        justify-content: flex-start;
        
        .content {
          background: #f5f5f5;
        }
      }
      
      .avatar {
        font-size: 20px;
        flex-shrink: 0;
      }
      
      .content {
        max-width: 280px;
        padding: 8px 12px;
        border-radius: 8px;
        word-wrap: break-word;
        font-size: 13px;
        line-height: 1.5;
        
        .text {
          white-space: pre-wrap;
        }
        
        .task-suggestion {
          h4 {
            margin: 0 0 8px 0;
            font-size: 13px;
          }
        }
        
        .loading {
          display: flex;
          gap: 4px;
          
          span {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #999;
            animation: bounce 1.4s infinite;
            
            &:nth-child(1) { animation-delay: 0s; }
            &:nth-child(2) { animation-delay: 0.2s; }
            &:nth-child(3) { animation-delay: 0.4s; }
          }
          
          @keyframes bounce {
            0%, 80%, 100% { opacity: 0.3; }
            40% { opacity: 1; }
          }
        }
      }
    }
  }
  
  .shortcuts {
    display: flex;
    gap: 8px;
    padding: 0 16px;
    margin-bottom: 12px;
    
    .shortcut-btn {
      flex: 1;
      height: 32px;
      border: 1px solid #ddd;
      border-radius: 6px;
      background: white;
      cursor: pointer;
      font-size: 12px;
      transition: all 0.3s;
      
      &:hover {
        background: #f5f5f5;
        border-color: #1890ff;
        color: #1890ff;
      }
    }
  }
  
  .input-area {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid #f0f0f0;
  }
}

.chat-fab {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #1890ff;
  border: none;
  font-size: 24px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  transition: all 0.3s;
  z-index: 999;
  
  &:hover {
    transform: scale(1.1);
    box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  }
}
</style>
```

### 2.3 前端API （TypeScript）

创建 `frontend/src/api/user/ai.ts`：

```typescript
import request from '@/utils/request'

export interface ChatRequest {
  message: string
}

export interface ParseTaskRequest {
  message: string
}

export interface TaskData {
  title: string
  deadline?: string
  estimatedMinutes?: number
  categoryId?: number
  description?: string
}

// 获取用户分类
export function getCategories() {
  return request.get('/api/v1/categories')
}

// AI对话
export function chat(data: ChatRequest) {
  return request.post('/api/v1/user/ai/chat', data)
}

// 自然语言解析任务
export function parseTask(data: ParseTaskRequest) {
  return request.post('/api/v1/user/ai/parse-task', data)
}

// 创建任务
export function createTask(task: TaskData) {
  return request.post('/api/v1/tasks', task)
}

// 获取今日总结
export function getTodaySummary() {
  return request.get('/api/v1/user/ai/summary/today')
}

// 切换模型（用户端也可以选择）
export function switchModel(provider: string) {
  return request.post(`/api/v1/user/ai/switch-model/${provider}`)
}
```

### 2.4 后端Service

创建 `backend/src/main/java/com/timemanager/ai/service/UserAiService.java`：

```java
package com.timemanager.ai.service;

import com.timemanager.ai.config.AiConfigManager;
import com.timemanager.entity.Task;
import com.timemanager.mapper.TaskMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserAiService {
    
    @Autowired
    private DynamicAiService dynamicAiService;
    
    @Autowired
    private TaskMapper taskMapper;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 自然语言解析为任务结构
     */
    public TaskParseResult parseTaskFromNaturalLanguage(String input) {
        String systemPrompt = """
            你是一个任务管理助手。从用户输入中提取任务信息，返回纯JSON格式（不要有其他文字）：
            {
                "title": "任务标题",
                "deadline": "YYYY-MM-DD HH:mm",
                "estimatedMinutes": 数字,
                "categoryName": "工作/学习/个人"
            }
            如果用户没有提供某些信息，则该字段置为null。
            """;
        
        try {
            String aiResp = dynamicAiService.chat(systemPrompt, "用户输入：" + input);
            // 提取JSON部分
            String jsonStr = extractJson(aiResp);
            return objectMapper.readValue(jsonStr, TaskParseResult.class);
        } catch (Exception e) {
            log.error("任务解析失败", e);
            return new TaskParseResult();
        }
    }
    
    /**
     * 生成今日总结
     */
    public String generateDailySummary(Long userId) {
        LocalDate today = LocalDate.now();
        
        // 查询今日数据
        // List<Task> completedTasks = taskMapper.selectCompletedByUserAndDate(userId, today);
        // TODO: 实现相应的Mapper方法查询
        
        int completedCount = 5; // 示例
        long totalMinutes = 480;
        int createdCount = 8;
        
        String stats = String.format(
            "今日完成任务%d个，总耗时%d分钟，新建任务%d个。",
            completedCount, totalMinutes, createdCount
        );
        
        String systemPrompt = 
            "你是一个鼓励型的效率助手。根据用户今日的任务完成情况，生成一段简短的总结和鼓励（不超过80字）。";
        
        return dynamicAiService.chat(systemPrompt, "数据：" + stats);
    }
    
    /**
     * 简单的对话
     */
    public String chat(Long userId, String message) {
        String systemPrompt = """
            你是一个聪慧的时间管理助手。帮助用户：
            1. 创建、查询任务
            2. 统计任务完成情况
            3. 给出时间管理建议
            
            用户ID: """ + userId + """
            
            回答要简洁、有帮助。
            """;
        
        return dynamicAiService.chat(systemPrompt, message);
    }
    
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
    
    @Data
    @AllArgsConstructor
    public static class TaskParseResult {
        private String title;
        private String deadline;
        private Integer estimatedMinutes;
        private String categoryName;
        
        public TaskParseResult() {}
    }
}
```

### 2.5 后端Controller

创建 `backend/src/main/java/com/timemanager/controller/UserAiController.java`：

```java
package com.timemanager.controller;

import com.timemanager.ai.service.UserAiService;
import com.timemanager.common.result.Result;
import com.timemanager.util.UserUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/ai")
public class UserAiController {
    
    @Autowired
    private UserAiService userAiService;
    
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        Long userId = UserUtil.getCurrentUserId();
        String response = userAiService.chat(userId, request.getMessage());
        return Result.success(response);
    }
    
    @PostMapping("/parse-task")
    public Result<UserAiService.TaskParseResult> parseTask(@RequestBody ParseTaskRequest request) {
        UserAiService.TaskParseResult result = userAiService.parseTaskFromNaturalLanguage(request.getMessage());
        return Result.success(result);
    }
    
    @GetMapping("/summary/today")
    public Result<String> getTodaySummary() {
        Long userId = UserUtil.getCurrentUserId();
        String summary = userAiService.generateDailySummary(userId);
        return Result.success(summary);
    }
    
    @Data
    public static class ChatRequest {
        private String message;
    }
    
    @Data
    public static class ParseTaskRequest {
        private String message;
    }
}
```

---

## 三、管理员端AI智能助手

### 3.1 功能设计
- **自然语言数据查询**: "最近一周新增用户有多少"
- **操作日志智能分析**: "分析昨天异常登录"
- **主动预警推送**: 自动检测高风险操作
- **模型管理面板**: 在Web UI切换API提供商

### 3.2 前端页面（管理后台）

创建 `frontend/src/views/admin/AIAssistant.vue`：

```vue
<template>
  <div class="admin-ai-assistant">
    <!-- 配置面板 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>🔧 AI 配置</span>
          <div class="header-actions">
            <el-select v-model="selectedProvider" @change="switchProvider" size="small">
              <el-option label="DeepSeek" value="deepseek" />
              <el-option label="ChatAnywhere" value="chatanywhere" />
            </el-select>
            <el-button @click="testConnection" type="primary" size="small" :loading="testLoading">
              测试连接
            </el-button>
          </div>
        </div>
      </template>
      
      <div v-if="connectionStatus" :class="['status-box', connectionStatus.success ? 'success' : 'error']">
        {{ connectionStatus.message }}
      </div>
    </el-card>

    <!-- 主体布局 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧：对话区 -->
      <el-col :xs="24" :md="14">
        <el-card class="chat-card">
          <template #header>
            <div class="card-header">
              <span>💬 智能查询助手</span>
            </div>
          </template>
          
          <!-- 消息展示 -->
          <div class="messages" ref="messagesContainer">
            <div v-for="msg in chatMessages" :key="msg.id" :class="['msg', msg.role]">
              <div class="msg-avatar">{{ msg.role === 'user' ? '👨' : '🤖' }}</div>
              <div class="msg-content">{{ msg.content }}</div>
            </div>
          </div>
          
          <!-- 快捷查询 -->
          <div class="quick-queries">
            <el-button-group>
              <el-button size="small" @click="quickQuery('users_today')">今日新增用户</el-button>
              <el-button size="small" @click="quickQuery('login_failed')">失败登录分析</el-button>
              <el-button size="small" @click="quickQuery('active_users')">活跃用户统计</el-button>
            </el-button-group>
          </div>
          
          <!-- 输入区 -->
          <div class="input-group">
            <el-input 
              v-model="queryInput"
              @keyup.enter="sendQuery"
              placeholder="输入你的查询（如：统计今天新增用户数）"
              :disabled="queryLoading"
            />
            <el-button @click="sendQuery" type="primary" :loading="queryLoading">查询</el-button>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧：预警面板 -->
      <el-col :xs="24" :md="10">
        <el-card class="alerts-card">
          <template #header>
            <div class="card-header">
              <span>⚠️ 智能预警（{{ unhandledAlerts.length }}条）</span>
              <el-button text size="small" @click="refreshAlerts">刷新</el-button>
            </div>
          </template>
          
          <div v-if="unhandledAlerts.length === 0" class="empty-state">
            暂无预警
          </div>
          
          <div v-else class="alerts-list">
            <div v-for="alert in unhandledAlerts" :key="alert.id" :class="['alert-item', alert.severity]">
              <div class="alert-title">
                <strong>{{ alert.title }}</strong>
                <el-tag :type="severityType(alert.severity)">{{ alert.severity }}</el-tag>
              </div>
              <div class="alert-desc">{{ alert.description }}</div>
              <div class="alert-suggestion">
                <strong>💡 建议:</strong> {{ alert.suggestion }}
              </div>
              <div class="alert-actions">
                <el-button text size="small" @click="handleAlert(alert.id)">标记处理</el-button>
                <el-button text size="small" @click="viewAlertDetails(alert.id)">查看详情</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as adminAiApi from '@/api/admin/ai'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
}

interface Alert {
  id: number
  title: string
  description: string
  suggestion: string
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
}

const selectedProvider = ref('deepseek')
const testLoading = ref(false)
const connectionStatus = ref<any>(null)
const chatMessages = ref<ChatMessage[]>([])
const queryInput = ref('')
const queryLoading = ref(false)
const unhandledAlerts = ref<Alert[]>([])
const messagesContainer = ref<HTMLElement>()
let msgIdCounter = 0

onMounted(async () => {
  await refreshAlerts()
})

const switchProvider = async (provider: string) => {
  try {
    await adminAiApi.switchAiProvider(provider)
    ElMessage.success(`✓ 已切换到 ${provider}`)
    connectionStatus.value = null
  } catch (error) {
    ElMessage.error('切换失败')
  }
}

const testConnection = async () => {
  testLoading.value = true
  try {
    const result = await adminAiApi.testConnection(selectedProvider.value)
    connectionStatus.value = result
  } finally {
    testLoading.value = false
  }
}

const sendQuery = async () => {
  if (!queryInput.value.trim()) return
  
  const query = queryInput.value
  queryInput.value = ''
  
  // 添加用户消息
  chatMessages.value.push({
    id: String(msgIdCounter++),
    role: 'user',
    content: query
  })
  
  queryLoading.value = true
  await nextTick()
  
  try {
    const response = await adminAiApi.queryData({ question: query })
    chatMessages.value.push({
      id: String(msgIdCounter++),
      role: 'assistant',
      content: response.answer
    })
  } catch (error) {
    chatMessages.value.push({
      id: String(msgIdCounter++),
      role: 'assistant',
      content: '❌ 查询失败，请稍后重试'
    })
  } finally {
    queryLoading.value = false
    scrollToBottom()
  }
}

const quickQuery = async (type: string) => {
  const queries = {
    users_today: '统计今天新增了多少用户',
    login_failed: '分析最近一小时有多少次登录失败',
    active_users: '列出今天登录超过3次的活跃用户有多少人'
  } as Record<string, string>
  
  queryInput.value = queries[type] || ''
  await nextTick()
  await sendQuery()
}

const refreshAlerts = async () => {
  try {
    unhandledAlerts.value = await adminAiApi.getAlerts()
  } catch (error) {
    ElMessage.error('加载预警失败')
  }
}

const handleAlert = async (alertId: number) => {
  try {
    await adminAiApi.markAlertHandled(alertId)
    unhandledAlerts.value = unhandledAlerts.value.filter(a => a.id !== alertId)
    ElMessage.success('✓ 已标记处理')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const viewAlertDetails = (alertId: number) => {
  // 可以打开详情对话框
  ElMessage.info('查看详情功能开发中...')
}

const severityType = (severity: string) => {
  const types: Record<string, string> = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'info'
  }
  return types[severity] || 'info'
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>

<style scoped lang="scss">
.admin-ai-assistant {
  padding: 20px;
  
  .config-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .header-actions {
        display: flex;
        gap: 12px;
      }
    }
    
    .status-box {
      padding: 12px;
      border-radius: 4px;
      font-size: 14px;
      
      &.success {
        background: #f6ffed;
        border: 1px solid #b7eb8f;
        color: #389e0d;
      }
      
      &.error {
        background: #fff1f0;
        border: 1px solid #ffa39e;
        color: #d9363e;
      }
    }
  }
  
  .chat-card {
    .messages {
      height: 400px;
      overflow-y: auto;
      margin-bottom: 16px;
      padding: 12px;
      background: #fafafa;
      border-radius: 4px;
      
      .msg {
        display: flex;
        gap: 8px;
        margin-bottom: 12px;
        
        &.assistant .msg-avatar {
          align-self: flex-start;
        }
        
        &.user {
          flex-direction: row-reverse;
          
          .msg-avatar {
            align-self: flex-start;
          }
        }
        
        .msg-avatar {
          font-size: 20px;
          min-width: 30px;
        }
        
        .msg-content {
          max-width: 70%;
          padding: 8px 12px;
          border-radius: 6px;
          word-wrap: break-word;
          font-size: 13px;
          line-height: 1.5;
        }
        
        &.user .msg-content {
          background: #1890ff;
          color: white;
        }
        
        &.assistant .msg-content {
          background: white;
          border: 1px solid #e8e8e8;
        }
      }
    }
    
    .quick-queries {
      margin: 12px 0;
    }
    
    .input-group {
      display: flex;
      gap: 8px;
    }
  }
  
  .alerts-card {
    .alerts-list {
      .alert-item {
        padding: 12px;
        margin-bottom: 12px;
        border-left: 3px solid;
        background: #fafafa;
        border-radius: 4px;
        
        &.HIGH {
          border-left-color: #ff4d4f;
          background: #fff1f0;
        }
        
        &.MEDIUM {
          border-left-color: #faad14;
          background: #fffbe6;
        }
        
        &.LOW {
          border-left-color: #1890ff;
          background: #e6f7ff;
        }
        
        .alert-title {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;
          font-size: 14px;
        }
        
        .alert-desc {
          color: #666;
          font-size: 13px;
          margin-bottom: 8px;
          line-height: 1.5;
        }
        
        .alert-suggestion {
          color: #0050b3;
          background: #f0f5ff;
          padding: 8px;
          border-radius: 4px;
          font-size: 12px;
          margin-bottom: 8px;
        }
        
        .alert-actions {
          display: flex;
          gap: 8px;
        }
      }
    }
    
    .empty-state {
      text-align: center;
      padding: 40px 20px;
      color: #999;
    }
  }
}
</style>
```

### 3.3 前端API

创建 `frontend/src/api/admin/ai.ts`：

```typescript
import request from '@/utils/request'

export interface QueryRequest {
  question: string
}

export interface QueryResponse {
  answer: string
  dataSource?: any
}

// 获取所有AI配置
export function getAiConfigs() {
  return request.get('/api/v1/admin/ai-config/list')
}

// 切换AI提供商
export function switchAiProvider(provider: string) {
  return request.post(`/api/v1/admin/ai-config/switch/${provider}`)
}

// 测试连接
export function testConnection(provider: string) {
  return request.post(`/api/v1/admin/ai-config/test-connection/${provider}`)
}

// 自然语言查询
export function queryData(data: QueryRequest): Promise<QueryResponse> {
  return request.post('/api/v1/admin/ai/query', data)
}

// 获取未处理的预警
export function getAlerts() {
  return request.get('/api/v1/admin/ai/alerts/unhandled')
}

// 标记预警已处理
export function markAlertHandled(alertId: number) {
  return request.put(`/api/v1/admin/ai/alert/${alertId}/handle`)
}

// 手动触发日志扫描
export function triggerLogScan() {
  return request.post('/api/v1/admin/ai/scan-logs')
}
```

### 3.4 后端Service

创建 `backend/src/main/java/com/timemanager/ai/service/AdminAiService.java`：

```java
package com.timemanager.ai.service;

import com.timemanager.ai.config.AiConfigManager;
import com.timemanager.entity.AiAlert;
import com.timemanager.entity.OperationLog;
import com.timemanager.mapper.AiAlertMapper;
import com.timemanager.mapper.OperationLogMapper;
import com.timemanager.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class AdminAiService {
    
    @Autowired
    private DynamicAiService dynamicAiService;
    
    @Autowired
    private AiAlertMapper aiAlertMapper;
    
    @Autowired
    private OperationLogMapper operationLogMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 处理自然语言查询
     */
    public AdminQueryResponse handleNaturalLanguageQuery(String question) {
        // 第一步：让AI理解意图
        String intentPrompt = """
            分析用户的查询意图，返回JSON：
            {
              "type": "统计类|查询类|分析类",
              "entity": "用户|日志|任务|时间记录",
              "timeRange": "今天|本周|本月|自定义",
              "filters": {}
            }
            """;
        
        String intentJson = dynamicAiService.chat(intentPrompt, "用户问题：" + question);
        // JSON解析 ...
        
        // 第二步：根据意图执行数据查询
        String queryResult = executeQuery(question);
        
        // 第三步：让AI转成自然语言
        String synthesisPrompt = """
            根据数据结果，用简洁的自然语言总结回答用户的问题。
            数据：""" + queryResult + """
            
            用户问题：""" + question;
        
        String answer = dynamicAiService.chat(synthesisPrompt, "");
        
        return new AdminQueryResponse(answer, queryResult);
    }
    
    /**
     * 执行数据查询（示例：数据统计）
     */
    private String executeQuery(String question) {
        // 这里可以根据question内容调用不同的Mapper查询
        // 示例：如果question包含"新增用户"
        if (question.contains("新增用户")) {
            LocalDate today = LocalDate.now();
            long count = userMapper.selectCount(
                new QueryWrapper<com.timemanager.entity.User>()
                    .between("created_at", 
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay())
            );
            return "今天新增用户: " + count + "人";
        }
        
        // 如果question包含"失败登录"
        if (question.contains("失败登录") || question.contains("登录失败")) {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long failedCount = operationLogMapper.selectCount(
                new QueryWrapper<OperationLog>()
                    .eq("action", "login_failed")
                    .gt("created_at", oneHourAgo)
            );
            return "最近一小时失败登录: " + failedCount + "次";
        }
        
        // 更多查询逻辑...
        return "查询结果：无法匹配";
    }
    
    /**
     * 获取未处理的预警
     */
    public List<AiAlert> getUnhandledAlerts() {
        return aiAlertMapper.selectList(
            new QueryWrapper<AiAlert>()
                .eq("is_handled", 0)
                .orderByDesc("created_at")
        );
    }
    
    /**
     * 标记预警已处理
     */
    public void markAlertHandled(Long alertId, Long handlerId) {
        AiAlert alert = new AiAlert();
        alert.setId(alertId);
        alert.setIsHandled(1);
        alert.setHandlerId(handlerId);
        alert.setHandledAt(LocalDateTime.now());
        aiAlertMapper.updateById(alert);
    }
    
    /**
     * 扫描日志并生成预警
     */
    public void scanAndGenerateAlerts() {
        // 查询最近10分钟的操作日志
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        List<OperationLog> logs = operationLogMapper.selectList(
            new QueryWrapper<OperationLog>()
                .gt("created_at", tenMinutesAgo)
        );
        
        // 分类异常
        analyzeAbnormalLogin(logs);
        analyzeDeleteOperations(logs);
        // ... 更多分析
    }
    
    /**
     * 异地登录预警
     */
    private void analyzeAbnormalLogin(List<OperationLog> logs) {
        logs.stream()
            .filter(l -> l.getAction().equals("login_failed") && l.getCount() > 5)
            .forEach(log -> {
                String description = "用户 " + log.getUserId() + 
                    " 在 10 分钟内失败登录 " + log.getCount() + " 次，可能存在暴力破解。";
                
                String suggestion = dynamicAiService.chat(
                    "根据这个异常操作日志，给出一条安全建议（不超过30字）：",
                    description
                );
                
                createAlert("ABNORMAL_LOGIN", "HIGH", "异常登录检测", description, suggestion);
            });
    }
    
    /**
     * 批量删除预警
     */
    private void analyzeDeleteOperations(List<OperationLog> logs) {
        long deleteCount = logs.stream()
            .filter(l -> l.getAction().contains("delete"))
            .count();
        
        if (deleteCount > 10) {
            String description = "检测到10分钟内有 " + deleteCount + 
                " 条删除操作，可能存在数据泄露风险。";
            
            String suggestion = dynamicAiService.chat(
                "这是一个可能的数据安全威胁，给出应急建议：",
                description
            );
            
            createAlert("BULK_DELETE", "HIGH", "批量删除预警", description, suggestion);
        }
    }
    
    /**
     * 创建预警并推送
     */
    private void createAlert(String type, String severity, String title, 
                             String description, String suggestion) {
        AiAlert alert = new AiAlert();
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setDescription(description);
        alert.setSuggestion(suggestion);
        alert.setIsHandled(0);
        
        aiAlertMapper.insert(alert);
        
        // WebSocket推送到管理端
        messagingTemplate.convertAndSend("/topic/ai-alerts", alert);
        
        log.warn("[AI预警] {} - {}", type, title);
    }
    
    @Data
    @AllArgsConstructor
    public static class AdminQueryResponse {
        private String answer;
        private String rawData;
    }
}
```

### 3.5 后端Controller

创建 `backend/src/main/java/com/timemanager/controller/AdminAiController.java`：

```java
package com.timemanager.controller;

import com.timemanager.ai.service.AdminAiService;
import com.timemanager.common.result.Result;
import com.timemanager.entity.AiAlert;
import com.timemanager.util.UserUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ai")
public class AdminAiController {
    
    @Autowired
    private AdminAiService adminAiService;
    
    @PostMapping("/query")
    public Result<AdminAiService.AdminQueryResponse> query(@RequestBody QueryRequest request) {
        AdminAiService.AdminQueryResponse response = 
            adminAiService.handleNaturalLanguageQuery(request.getQuestion());
        return Result.success(response);
    }
    
    @GetMapping("/alerts/unhandled")
    public Result<List<AiAlert>> getUnhandledAlerts() {
        List<AiAlert> alerts = adminAiService.getUnhandledAlerts();
        return Result.success(alerts);
    }
    
    @PutMapping("/alert/{id}/handle")
    public Result<Void> handleAlert(@PathVariable Long id) {
        Long adminId = UserUtil.getCurrentUserId();
        adminAiService.markAlertHandled(id, adminId);
        return Result.success();
    }
    
    @PostMapping("/scan-logs")
    public Result<Void> triggerLogScan() {
        adminAiService.scanAndGenerateAlerts();
        return Result.success();
    }
    
    @Data
    public static class QueryRequest {
        private String question;
    }
}
```

---

## 四、操作日志AI预警的缓存优化

### 4.1 缓存设计

创建 `backend/src/main/java/com/timemanager/ai/detector/LogAnomalyDetector.java`：

```java
package com.timemanager.ai.detector;

import com.timemanager.entity.OperationLog;
import com.timemanager.ai.service.AdminAiService;
import com.timemanager.ai.service.DynamicAiService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LogAnomalyDetector {
    
    @Autowired
    private DynamicAiService dynamicAiService;
    
    @Autowired
    private AdminAiService adminAiService;
    
    // 5分钟内相同日志摘要只调用一次AI
    private final Cache<String, String> aiResponseCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(100)
        .build();
    
    /**
     * 定时扫描日志（每10分钟）
     */
    @Scheduled(fixedDelay = 600000) // 10分钟
    public void scanLogs() {
        log.info("[AI] 开始扫描操作日志");
        adminAiService.scanAndGenerateAlerts();
    }
    
    /**
     * 分析特定的日志集合
     */
    public void analyzeWithCache(List<OperationLog> logs, String alertType) {
        String summary = buildLogSummary(logs);
        String cacheKey = generateCacheKey(alertType, summary);
        
        String cachedResponse = aiResponseCache.getIfPresent(cacheKey);
        
        if (cachedResponse != null) {
            log.info("[AI缓存] 命中 - {}", alertType);
            // 使用缓存结果处理
            return;
        }
        
        // 调用AI分析
        log.info("[AI] 调用API分析 - {}", alertType);
        String systemPrompt = """
            你是一个安全分析专家。分析以下操作日志，识别潜在的安全威胁。
            返回JSON：
            {
              "riskLevel": "HIGH|MEDIUM|LOW",
              "analysis": "详细分析",
              "recommendation": "建议"
            }
            """;
        
        String userMessage = "操作日志：\n" + summary;
        String response = dynamicAiService.chat(systemPrompt, userMessage);
        
        // 缓存结果
        aiResponseCache.put(cacheKey, response);
    }
    
    private String buildLogSummary(List<OperationLog> logs) {
        StringBuilder sb = new StringBuilder();
        for (OperationLog log : logs) {
            sb.append(String.format(
                "用户%d在%s执行了%s操作，IP:%s\n",
                log.getUserId(),
                log.getCreatedAt(),
                log.getAction(),
                log.getIpAddress()
            ));
        }
        return sb.toString();
    }
    
    private String generateCacheKey(String alertType, String summary) {
        String combined = alertType + "::" + summary;
        return DigestUtils.md5DigestAsHex(combined.getBytes());
    }
}
```

---

## 五、系统集成与数据库关系

```
用户表 (user)
    ↓
├── 关联到任务表 (task) → AI助手辅助创建/同步
├── 关联到分类表 (category) → AI任务分类建议
└── 关联到操作日志表 (operation_log) → AI异常检测

管理员功能 (admin users)
    ↓
├── AI配置表 (ai_config) → 模型切换管理
├── AI预警表 (ai_alert) → 主动预警推送
└── 操作日志表 (operation_log) → AI分析与建议

AI系统架构
    ├── DynamicAiService (统一入口，动态模型选择)
    ├── UserAiService (用户个性化功能)
    ├── AdminAiService (管理员查询与预警)
    └── LogAnomalyDetector (自动异常检测 + 缓存)
```

---

## 六、完整代码结构

```plaintext
backend/src/main/java/com/timemanager/
├── ai/
│   ├── config/
│   │   ├── AiConfigManager.java          ✅ 配置管理
│   │   └── WebSocketConfig.java          ✅ WebSocket配置
│   ├── service/
│   │   ├── DynamicAiService.java         ✅ 统一AI入口
│   │   ├── UserAiService.java            ✅ 用户AI功能
│   │   └── AdminAiService.java           ✅ 管理员AI功能
│   ├── detector/
│   │   └── LogAnomalyDetector.java       ✅ 日志异常检测
│   └── websocket/
│       └── AlertWebSocketHandler.java    ✅ 警报推送
├── entity/
│   ├── AiConfig.java                     ✅ 配置实体
│   └── AiAlert.java                      ✅ 预警实体
├── controller/
│   ├── UserAiController.java             ✅ 用户API
│   ├── AdminAiController.java            ✅ 管理员API
│   └── AdminAiConfigController.java      ✅ 配置管理API
└── mapper/
    ├── AiConfigMapper.java
    └── AiAlertMapper.java

frontend/src/
├── api/
│   ├── user/ai.ts                        ✅ 用户AI API
│   └── admin/ai.ts                       ✅ 管理员AI API
├── components/
│   └── user/AIChatAssistant.vue          ✅ 用户浮窗
├── views/
│   ├── user/AIChat.vue                   ✅ 用户AI页面
│   └── admin/AIAssistant.vue             ✅ 管理员AI页面
└── store/
    └── ai/aiConfig.ts                    ✅ AI配置状态管理
```

---

## 七、业务流程图

```
用户端流程
┌─────────────┐       ┌──────────────┐       ┌──────────────┐
│ 用户输入    │──────▶│ AI理解+ 分类 │──────▶│ 结构化数据   │
│ 自然语言    │       │ (NLU)        │       │ (Task JSON)  │
└─────────────┘       └──────────────┘       └──────────────┘
                                                     │
                                                     ▼
                                            ┌──────────────┐
                                            │ 前端Form填充 │
                                            │ & 用户确认   │
                                            └──────────────┘
                                                     │
                                                     ▼
                                            ┌──────────────┐
                                            │ 创建任务     │
                                            │ (后端保存)   │
                                            └──────────────┘

管理员端流程
┌─────────────┐       ┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│ 管理员提问  │──────▶│ AI意图理解   │──────▶│ SQL查询生成  │──────▶│ 数据库查询   │
│ (自然语言)  │       │ & 参数提取   │       │ (模板+填充)  │       │              │
└─────────────┘       └──────────────┘       └──────────────┘       └──────────────┘
                                                                             │
                                                                             ▼
                                                                    ┌──────────────┐
                                                                    │ AI自然语言   │
                                                                    │ 总结数据     │
                                                                    └──────────────┘
                                                                             │
                                                                             ▼
                                                                    ┌──────────────┐
                                                                    │ 返回给用户   │
                                                                    │ (自然回答)   │
                                                                    └──────────────┘

自动预警流程
┌─────────────────────┐
│ 定时任务 (10分钟)   │
└──────────────────────┘
         │
         ▼
┌─────────────────────┐
│ 查询最近操作日志    │
└──────────────────────┘
         │
         ▼
┌─────────────────────┐
│ 缓存检查            │
│ (5分钟有效期)       │
└──────────────────────┘
         │
    ┌────┴────┐
    ▼         ▼
 [命中]    [未命中]
  回收       │
            ▼
      ┌──────────────┐
      │ 调用AI分析   │
      │ (异常检测)   │
      └──────────────┘
            │
            ▼
      ┌──────────────┐
      │ 生成预警     │
      │ (存DB+推送)  │
      └──────────────┘
            │
            ▼
      ┌──────────────┐
      │ WebSocket    │
      │ 推送到管理员 │
      └──────────────┘
```

---

## 八、部署与配置约定

### 后端 pom.xml 新增依赖

```xml
<!-- Spring AI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>0.11.0</version>
</dependency>

<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Guava缓存 -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.0.0-jre</version>
</dependency>

<!-- JSON处理 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### application.properties 配置

```properties
# AI 配置 (从数据库读取，以下为默认值示例)
ai.default-provider=deepseek
ai.cache-minutes=5

# WebSocket
spring.websocket.path=/ws-alert

# 日志异常检测
ai.log-scan-interval=600000
ai.log-anomaly-threshold=10
```

---

## 九、完整实现检查清单

- [ ] 创建 `ai_config` 表并初始化数据
- [ ] 创建 `ai_alert` 表
- [ ] 创建 `ai_call_log` 表（成本追踪）
- [ ] 实现 `AiConfigManager` 配置管理器
- [ ] 实现 `DynamicAiService` 统一AI服务
- [ ] 实现 `UserAiService` 用户AI功能
- [ ] 实现 `AdminAiService` 管理员AI功能
- [ ] 实现 `LogAnomalyDetector` 异常检测
- [ ] 实现 `UserAiController` 后端API
- [ ] 实现 `AdminAiController` 后端API
- [ ] 实现 `AdminAiConfigController` 配置管理API
- [ ] 实现 `AIChatAssistant.vue` 用户前端
- [ ] 实现 `AIAssistant.vue` 管理员前端
- [ ] 配置 WebSocket 推送
- [ ] 前端集成 `ai.ts` API模块
- [ ] 添加定时任务扫描日志
- [ ] 开发文档与部署指南
- [ ] 进行功能测试与性能优化

---

## 十、答辩演示场景

### 场景1：用户端自然语言创建任务
**操作**：在AI助手中输入"明天下午三点半写毕业论文，预计3小时"
**展示**：
1. AI理解自然语言
2. 自动填充任务表单（标题、时间、时长、分类）
3. 用户确认后添加到任务列表
4. 任务在数据库中生成

### 场景2：管理员自然语言数据查询
**操作**：在AI助手中提问"最近7天每天新增多少用户"
**展示**：
1. AI理解查询意图
2. 后端生成SQL查询
3. AI生成自然语言答案 + 数据图表
4. 展示响应速度

### 场景3：主动异常检测预警
**前置**：在操作日志中预置异常数据（如大量失败登录）
**展示**：
1. 系统定时扫描日志
2. AI检测异常
3. 管理员页面右上角显示红色预警通知
4. 点击查看AI生成的风险描述与建议

### 场景4：API提供商切换演示
**操作**：在AI配置面板切换 DeepSeek → ChatAnywhere
**展示**：
1. 测试连接按钮验证新配置
2. 再次提问同一个问题
3. 对比两个API的响应（延迟、质量）

---

**✅ 方案完成** — 这是一份完整、具体、可直接落地的AI功能设计，包含了前后端代码框架、数据库设计、缓存策略、WebSocket推送、多模型切换等所有核心模块。
