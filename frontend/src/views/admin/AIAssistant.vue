<template>
  <div class="admin-ai-assistant">
    <!-- 配置卡片 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span>🔧 AI 配置</span>
          <div class="header-actions">
            <el-select v-model="selectedProvider" @change="switchProvider" size="small" style="width: 140px">
              <el-option label="ChatGPT3.5" value="chatgpt3.5" />
              <el-option label="DeepSeek" value="deepseek" />
            </el-select>
            <el-button @click="testConnection" type="primary" size="small" :loading="testLoading">
              测试连接
            </el-button>
          </div>
        </div>
      </template>
      
      <div v-if="connectionStatus" :class="['status-box', connectionStatus.success ? 'success' : 'error']">
        <span>{{ connectionStatus.success ? '✓' : '✗' }}</span>
        {{ connectionStatus.message }}
      </div>
    </el-card>

    <!-- 主体布局：左=对话，右=预警 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 左侧：对话区 -->
      <el-col :xs="24" :md="14">
        <el-card class="chat-card">
          <template #header>
            <div class="card-header" style="justify-content: space-between;">
              <span>💬 智能查询助手</span>
              <div style="display:flex; align-items:center; gap:8px;">
                <el-checkbox v-model="includeSystemStats" size="small">附加系统统计</el-checkbox>
                <el-button 
                  text 
                  size="small" 
                  @click="clearChatHistory"
                  v-if="chatMessages.length > 0"
                >
                  🗑️ 清空记录
                </el-button>
              </div>
            </div>
          </template>
          
          <!-- 消息列表 -->
          <div class="messages" ref="messagesContainer">
            <div v-for="msg in chatMessages" :key="msg.id" :class="['msg', msg.role]">
              <div class="msg-avatar">{{ msg.role === 'user' ? '👨' : '🤖' }}</div>
              <div class="msg-content" v-html="msg.role === 'assistant' ? renderMarkdown(msg.content) : escapeHtml(msg.content)"></div>
            </div>
          </div>
          
          <!-- 快捷查询 -->
          <div class="quick-queries">
            <el-button-group>
              <el-button size="small" @click="quickQuery('users_today')">👥 今日新增用户</el-button>
              <el-button size="small" @click="quickQuery('login_failed')">❌ 登录失败分析</el-button>
              <el-button size="small" @click="quickQuery('active_users')">🟢 活跃用户统计</el-button>
            </el-button-group>
          </div>
          
          <!-- 输入区 -->
          <div class="input-group">
            <el-input 
              v-model="queryInput"
              @keyup.enter="sendQuery"
              placeholder="输入你的查询（如：统计今天新增用户数）"
              :disabled="queryLoading"
              clearable
            />
            <el-button @click="sendQuery" type="primary" :loading="queryLoading">
              查询
            </el-button>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧：预警面板 -->
      <el-col :xs="24" :md="10">
        <el-card class="alerts-card">
          <template #header>
            <div class="card-header">
              <span>⚠️ 智能预警（{{ unhandledAlerts.length }}）</span>
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
              <!-- 建议文本已从此视图移除，改为通过“AI建议”按钮将预警内容复制到左侧聊天输入 -->
              <div class="alert-actions">
                <el-button text size="small" @click="handleAlert(alert.id)">标记处理</el-button>
                <el-button text size="small" type="primary" @click="copyToAI(alert)" style="margin-left:8px">AI建议</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { ElMessage } from 'element-plus'
import * as adminAiApi from '@/api/admin/ai'
import { useAlertStore } from '@/store/alert'

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
  alertType: string
  createdAt: string
}

const selectedProvider = ref('chatgpt3.5')
const testLoading = ref(false)
const connectionStatus = ref<any>(null)
const chatMessages = ref<ChatMessage[]>([])
const queryInput = ref('')
const queryLoading = ref(false)
const alertStore = useAlertStore()
const unhandledAlerts = computed(() => {
  return (alertStore.alerts || []).filter((a: any) => !a.read)
})
const messagesContainer = ref<HTMLElement>()
const sessionId = ref('')  // 会话 ID，用于维持对话上下文
let msgIdCounter = 0
const includeSystemStats = ref(true)

// ========== 会话 ID 管理 ==========
const STORAGE_SESSION_ID = 'ai_session_id'

/**
 * 生成新的会话 ID（UUID 格式）
 */
const generateSessionId = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c == 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

/**
 * 初始化或恢复会话 ID
 */
const initSessionId = () => {
  let saved = localStorage.getItem(STORAGE_SESSION_ID)
  if (!saved) {
    saved = generateSessionId()
    localStorage.setItem(STORAGE_SESSION_ID, saved)
    console.log('[AI] 已生成新会话 ID:', saved)
  } else {
    console.log('[AI] 已恢复会话 ID:', saved)
  }
  sessionId.value = saved
}

// ========== 聊天记录持久化 ==========
const STORAGE_KEY = 'ai_assistant_chat_history'

/**
 * 从 localStorage 加载聊天记录
 */
const loadChatHistory = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      const messages = JSON.parse(saved)
      chatMessages.value = messages
      // 更新消息计数器
      if (messages.length > 0) {
        msgIdCounter = Math.max(...messages.map(m => parseInt(m.id))) + 1
      }
      console.log('[AI] 已加载聊天历史:', messages.length, '条消息')
    }
  } catch (error) {
    console.error('[AI] 加载聊天历史失败:', error)
  }
}

/**
 * 保存聊天记录到 localStorage
 */
const saveChatHistory = () => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(chatMessages.value))
  } catch (error) {
    console.error('[AI] 保存聊天历史失败:', error)
  }
}

/**
 * 清空聊天记录
 */
const clearChatHistory = () => {
  chatMessages.value = []
  sessionId.value = generateSessionId()
  localStorage.removeItem(STORAGE_KEY)
  localStorage.setItem(STORAGE_SESSION_ID, sessionId.value)
  msgIdCounter = 0
  ElMessage.success('✓ 已清空聊天记录和会话上下文')
}

/**
 * 初始化 - 从后端获取当前激活的提供商
 */
const initializeProvider = async () => {
  try {
    const config = await adminAiApi.getCurrentConfig()
    if (config?.provider) {
      selectedProvider.value = config.provider
      console.log('[AI] 已加载当前提供商:', config.provider)
    }
  } catch (error) {
    console.warn('[AI] 无法获取当前提供商，使用默认值', error)
    // 使用默认值
  }
}

onMounted(async () => {
  try {
    // 初始化会话 ID
    initSessionId()
    // 加载聊天历史
    loadChatHistory()
    
    // 初始化提供商配置（可选，失败不影响功能）
    try {
      await initializeProvider()
    } catch (error) {
      console.warn('[AI] 提供商初始化失败，使用默认值:', error)
    }
    
    // 加载预警（可选，失败不影响功能）
    try {
      await refreshAlerts()
    } catch (error) {
      console.warn('[AI] 预警加载失败:', error)
      unhandledAlerts.value = []
    }
  } catch (error) {
    console.error('[AI] 初始化失败:', error)
  }
})

const switchProvider = async (provider: string) => {
  try {
    const result = await adminAiApi.switchProvider(provider)
    if (result?.success) {
      ElMessage.success(`✓ 已切换到 ${provider}`)
      connectionStatus.value = null
    } else {
      ElMessage.error(`切换失败: ${result?.message || '未知错误'}`)
    }
  } catch (error: any) {
    ElMessage.error(`切换失败: ${error?.message || '网络错误'}`)
  }
}

const testConnection = async () => {
  testLoading.value = true
  try {
    const result = await adminAiApi.testConnection(selectedProvider.value)
    
    // 响应拦截器已经解包，result 就是数据
    if (result && typeof result === 'object') {
      connectionStatus.value = {
        success: result.success ?? false,
        message: result.message ?? '无响应',
        provider: result.provider,
        model: result.model
      }
      
      if (result.success) {
        ElMessage.success('✓ 连接成功')
        console.log('[AI] 连接测试成功:', result)
      } else {
        ElMessage.warning(`连接失败: ${result.message}`)
        console.warn('[AI] 连接测试失败:', result)
      }
    } else {
      throw new Error(`异常响应类型: ${typeof result}`)
    }
  } catch (error: any) {
    console.error('[AI] 测试连接异常:', error)
    connectionStatus.value = {
      success: false,
      message: error?.message || '测试失败'
    }
    ElMessage.error(`测试失败: ${error?.message || '未知错误'}`)
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
  saveChatHistory()
  
  queryLoading.value = true
  await nextTick()
  
  try {
    // 1) 获取危险摘要
    let dangerSummary: any = null
    try {
      dangerSummary = await adminAiApi.getDangerSummary()
    } catch (err) {
      console.warn('[AI] 无法获取危险摘要，继续请求', err)
      dangerSummary = null
    }

    // 2) 打包最近 12 条消息作为 messages
    const lastN = 12
    const recent = chatMessages.value.slice(-lastN).map(m => ({ role: m.role, content: m.content }))

    // 2.5) 可选：获取系统统计并将其并入 context
    let systemStats: any = null
    if (includeSystemStats.value) {
      try {
        systemStats = await adminAiApi.getSystemStatistics()
      } catch (err) {
        console.warn('[AI] 获取系统统计失败，继续发送查询', err)
        systemStats = null
      }
    }

    // 3) 传递 sessionId + messages + context 来维持对话上下文
    const contextObj: any = { danger_summary: dangerSummary?.summary || '' }
    if (systemStats) contextObj.system_stats = systemStats

    const response = await adminAiApi.queryData({ 
      question: query,
      sessionId: sessionId.value,
      messages: recent,
      context: contextObj
    })
    
    // 确保 response 有 answer 属性
    const answer = response?.answer || '无法获取答案'
    
    chatMessages.value.push({
      id: String(msgIdCounter++),
      role: 'assistant',
      content: answer
    })
    saveChatHistory()
  } catch (error: any) {
    console.error('查询失败:', error)
    chatMessages.value.push({
      id: String(msgIdCounter++),
      role: 'assistant',
      content: `❌ 查询失败: ${error?.message || '请稍后重试'}`
    })
    saveChatHistory()
  } finally {
    queryLoading.value = false
    scrollToBottom()
  }
}

const quickQuery = async (type: string) => {
  const queries: Record<string, string> = {
    users_today: '统计今天新增了多少用户',
    login_failed: '分析最近一小时有多少次登录失败',
    active_users: '统计今天有多少活跃用户登录'
  }
  
  queryInput.value = queries[type] || ''
  await nextTick()
  await sendQuery()
}

const refreshAlerts = async () => {
  try {
    // 使用 alertStore 的未处理告警，若需要强制同步则调用 fetchUnhandled
    try {
      await alertStore.fetchUnhandled()
    } catch (err) {
      console.warn('[AI] fetchUnhandled failed', err)
    }
  } catch (error: any) {
    console.error('加载预警失败:', error)
    unhandledAlerts.value = []
    ElMessage.error(`加载预警失败: ${error?.message || '未知错误'}`)
  }
}

const handleAlert = async (alertId: number) => {
  try {
    await alertStore.markAsRead(alertId)
    ElMessage.success('✓ 已标记处理')
  } catch (error: any) {
    console.error('标记预警失败:', error)
    ElMessage.error(`操作失败: ${error?.message || '未知错误'}`)
  }
}

/**
 * 将预警文本复制到左侧 AI 聊天输入并聚焦
 */
const copyToAI = async (alert: Alert) => {
  const text = `请分析这条安全预警：${alert.title}\n描述：${alert.description || ''}\n请给出可能的处置建议。`;
  queryInput.value = text
  await nextTick()
  // 尝试聚焦到输入框（Element Plus input 内部为 .el-input__inner）
  try {
    const inputEl = document.querySelector('.admin-ai-assistant .input-group .el-input__inner') as HTMLInputElement | null
    if (inputEl) {
      inputEl.focus()
    }
  } catch (e) {
    // ignore
  }
}

const severityType = (severity: string) => {
  const s = (severity || '').toString().toLowerCase()
  if (s === 'critical') return 'danger'
  if (s === 'high') return 'warning'
  if (s === 'medium' || s === 'med') return 'warning'
  if (s === 'low' || s === 'info') return 'info'
  return 'info'
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 配置 marked，关闭导致警告的两个特性
marked.setOptions({
  breaks: true,
  gfm: true,
  mangle: false,
  headerIds: false
})

// 渲染 Markdown 为安全的 HTML
const renderMarkdown = (text: string | undefined | null) => {
  if (!text) return ''
  const rawHtml = marked.parse(String(text))
  return DOMPurify.sanitize(rawHtml)
}

// 简单转义用于用户文本（插入到 v-html 时保持安全）
const escapeHtml = (text: string | undefined | null) => {
  if (text == null) return ''
  return String(text).replace(/[&<>]/g, (m) => (m === '&' ? '&amp;' : m === '<' ? '&lt;' : '&gt;'))
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
      display: flex;
      align-items: center;
      gap: 8px;
      
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
        animation: msgSlide 0.3s ease-in;
        
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
          display: flex;
          align-items: center;
          justify-content: center;
        }
        
        .msg-content {
          max-width: 70%;
          padding: 8px 12px;
          border-radius: 6px;
          word-wrap: break-word;
          font-size: 13px;
          line-height: 1.6;
          white-space: pre-wrap;
        }
        
        &.user .msg-content {
          background: #1890ff;
          color: white;
        }
        
        &.assistant .msg-content {
          background: white;
          border: 1px solid #e8e8e8;
          color: #333;
        }
      }
      
      @keyframes msgSlide {
        from {
          opacity: 0;
          transform: translateY(10px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
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
        /* 支持小写 severity 名称 */
        &.critical, &.CRITICAL {
          border-left-color: #ff4d4f;
          background: #fff1f0;
        }
        &.high, &.HIGH {
          border-left-color: #fa8c16;
          background: #fff7e6;
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
          line-height: 1.5;
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
      font-size: 14px;
    }
  }
}
</style>
