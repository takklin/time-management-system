<template>
  <div class="ai-chat-drawer" v-if="isOpen">
    <!-- 抽屉头部 -->
    <div class="drawer-header">
      <div class="title-bar">
        <span class="title">🤖 AI 智能助手</span>
        <el-select v-model="selectedModel" size="small" @change="onModelChange" style="width: 120px">
          <el-option label="ChatGPT3.5" value="gpt-3.5" />
          <el-option label="DeepSeek" value="deepseek" />
        </el-select>
      </div>
      <button @click="isOpen = false" class="close-btn">✕</button>
    </div>

    <!-- 对话消息区 -->
    <div class="messages-container" ref="messagesContainer">
      <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
        <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
        <div class="content">
          <!-- 文本消息 -->
          <div v-if="msg.type === 'text'" class="text">{{ msg.content }}</div>
          
          <!-- 任务建议 -->
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
              <el-button type="primary" size="small" @click="confirmTask(msg.taskData)" style="width: 100%">
                ✓ 添加到任务列表
              </el-button>
            </el-form>
          </div>
          
          <!-- 加载动画 -->
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
      <button @click="quickAction('help')" class="shortcut-btn">❓ 帮助</button>
    </div>

    <!-- 输入框 -->
    <div class="input-area">
      <el-input 
        v-model="userInput"
        @keyup.enter="sendMessage"
        placeholder="输入你的需求或问题..."
        :disabled="loading"
        clearable
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
  <button v-else class="chat-fab" @click="isOpen = true" title="打开 AI 助手">
    💬
  </button>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as userAiApi from '@/api/user/ai'

interface Message {
  id: string
  role: 'user' | 'assistant'
  type: 'text' | 'loading'
  content?: string
}


const isOpen = ref(false)
const messages = ref<Message[]>([])
const userInput = ref('')
const loading = ref(false)
const selectedModel = ref('gpt-3.5')
const messagesContainer = ref<HTMLElement>()
let messageIdCounter = 0

// 会话ID，和管理员端一致
const STORAGE_SESSION_ID = 'user_ai_session_id'
const sessionId = ref('')
const initSessionId = () => {
  let saved = localStorage.getItem(STORAGE_SESSION_ID)
  if (!saved) {
    saved = generateSessionId()
    localStorage.setItem(STORAGE_SESSION_ID, saved)
  }
  sessionId.value = saved
}
const generateSessionId = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c == 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

// 欢迎消息
onMounted(async () => {
  initSessionId()
  
  // 恢复用户之前选择的模型，如果没有则默认为 'gpt-3.5'
  const savedModel = localStorage.getItem('ai_user_selected_model')
  if (savedModel) {
    selectedModel.value = savedModel
  }
  
  messages.value = [{
    id: String(messageIdCounter++),
    role: 'assistant',
    type: 'text',
    content: '👋 你好！我是你的 AI 助手。我可以帮你：\n• 💡 用自然语言创建任务\n• 📊 生成今日总结\n• 🔍 查询任务信息\n\n试试输入"帮我创建一个任务"吧！'
  }]
})

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

/**
 * 处理模型选择变更
 * 保存用户选择到 localStorage，下次打开浮窗时恢复
 */
const onModelChange = (newModel: string) => {
  localStorage.setItem('ai_user_selected_model', newModel)
  console.log('[AI助手] 用户选择了模型:', newModel)
  // 发送一条提示消息
  messages.value.push({
    id: String(messageIdCounter++),
    role: 'assistant',
    type: 'text',
    content: `已切换到 ${newModel === 'gpt-3.5' ? 'ChatGPT3.5' : 'DeepSeek'} 模型`
  })
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
    // 调用用户专用AI接口，传递选中的模型
    // 将前端的值映射到后端的 provider 名称
    const modelMapping: { [key: string]: string } = {
      'gpt-3.5': 'chatgpt3.5',
      'deepseek': 'deepseek'
    }
    const provider = modelMapping[selectedModel.value] || selectedModel.value
    
    const answer = await userAiApi.chat({ 
      message: userMsg,
      model: provider  // 传递模型参数给后端
    })
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    
    // 调试日志
    console.log('[AI助手] 收到回复:', { userMsg, answer, type: typeof answer, model: provider })
    
    messages.value.push({
      id: String(messageIdCounter++),
      role: 'assistant',
      type: 'text',
      content: (answer && String(answer).trim()) || '🤖 暂无回复内容，请稍后重试或换种说法。'
    })
  } catch (error: any) {
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    console.error('[AI助手] 请求出错:', error)
    messages.value.push({
      id: String(messageIdCounter++),
      role: 'assistant',
      type: 'text',
      content: '❌ 出错了，请稍后重试: ' + (error.message || '未知错误')
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const quickAction = (action: string) => {
  if (action === 'create') {
    userInput.value = '帮我创建一个任务：'
  } else if (action === 'summary') {
    // 直接获取总结
    getTodaySummary()
  } else if (action === 'help') {
    userInput.value = '你能帮我做什么？'
  }
}

const getTodaySummary = async () => {
  const loadingMsgId = String(messageIdCounter++)
  messages.value.push({
    id: loadingMsgId,
    role: 'assistant',
    type: 'loading'
  })
  
  loading.value = true
  await scrollToBottom()
  
  try {
    const summary = await userAiApi.getTodaySummary()
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    messages.value.push({
      id: String(messageIdCounter++),
      role: 'assistant',
      type: 'text',
      content: '📊 今日总结:\n\n' + summary
    })
  } catch (error) {
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    messages.value.push({
      id: String(messageIdCounter++),
      role: 'assistant',
      type: 'text',
      content: '❌ 获取总结失败'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const confirmTask = async (_taskData: any) => {
  try {
    // const _formattedData = {
    //   title: _taskData.title,
    //   deadline: _taskData.deadline ? new Date(_taskData.deadline).toISOString() : null,
    //   estimatedMinutes: _taskData.estimatedMinutes,
    //   categoryName: _taskData.categoryName
    // }
    
    // 这里应该调用创建任务 API
    // await taskApi.createTask(_formattedData)
    
    ElMessage.success('✓ 任务已添加到列表')
  } catch (error) {
    ElMessage.error('❌ 添加任务失败')
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
  border: 1px solid #e8e8e8;
  
  .drawer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 12px 12px 0 0;
    
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
      color: white;
      opacity: 0.8;
      
      &:hover {
        opacity: 1;
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
      animation: messageSlide 0.3s ease-in;
      
      &.user {
        justify-content: flex-end;
        
        .avatar {
          order: 2;
        }
        
        .content {
          background: #e8f5e9;
          order: 1;
          color: #333;
        }
      }
      
      &.assistant {
        justify-content: flex-start;
        
        .content {
          background: #f5f5f5;
          color: #333;
        }
      }
      
      .avatar {
        font-size: 20px;
        flex-shrink: 0;
        width: 28px;
        height: 28px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      
      .content {
        max-width: 280px;
        padding: 10px 12px;
        border-radius: 8px;
        word-wrap: break-word;
        font-size: 13px;
        line-height: 1.6;
        
        .text {
          white-space: pre-wrap;
        }
        
        .task-suggestion {
          h4 {
            margin: 0 0 12px 0;
            font-size: 13px;
            font-weight: 600;
          }
        }
        
        .loading {
          display: flex;
          gap: 4px;
          height: 6px;
          
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
      font-weight: 500;
      
      &:hover {
        background: #f5f5f5;
        border-color: #667eea;
        color: #667eea;
      }
      
      &:active {
        transform: scale(0.98);
      }
    }
  }
  
  .input-area {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid #f0f0f0;
    background: #fafafa;
  }
}

.chat-fab {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 24px;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.4);
  transition: all 0.3s;
  z-index: 999;
  
  &:hover {
    transform: scale(1.1);
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.6);
  }
  
  &:active {
    transform: scale(0.95);
  }
}

@keyframes messageSlide {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 深色模式支持
@media (prefers-color-scheme: dark) {
  .ai-chat-drawer {
    background: #1e1e1e;
    border-color: #333;
    
    .drawer-header {
      border-bottom-color: #333;
    }
    
    .messages-container {
      .message {
        &.user {
          .content {
            background: #2d5a2d;
            color: #e0e0e0;
          }
        }
        
        &.assistant {
          .content {
            background: #2a2a2a;
            color: #e0e0e0;
          }
        }
      }
    }
    
    .input-area {
      background: #2a2a2a;
      border-top-color: #333;
    }
  }
}
</style>
