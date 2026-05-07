import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, getCurrentUser, logout as apiLogout, changePassword as changePasswordApi, uploadAvatar as uploadAvatarApi, updateProfile as updateProfileApi } from '@/api/auth'
import { useScheduleStore } from '@/store/schedule'
import { useTaskStore } from '@/store/task'
import { useTimeRecordStore } from '@/store/time-record'
import { useTodoStore } from '@/stores/todo'
import { removeToken, getToken } from '@/utils/auth'
import { setToken as saveToken } from '@/utils/auth'

export interface User {
  id: number
  username: string
  email: string
  nickname?: string
  avatar?: string
  role?: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(getToken())
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  // 清理 AI 相关本地存储，避免在切换用户时泄露会话或触发残留的待创建数据
  function clearAiLocalCache() {
    try {
      const prefixes = [
        'ai_chat_history_',
        'user_ai_session_id',
        'ai_user_selected_model',
        'ai_pending_create_task',
        'ai_pending_create_schedule',
        'tm_starred_ids',
        'tm_archived_ids',
      ]
      const toRemove: string[] = []
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (!key) continue
        for (const p of prefixes) {
          if (key === p || key.startsWith(p + '_') || key.startsWith(p)) {
            toRemove.push(key)
            break
          }
        }
      }
      toRemove.forEach(k => localStorage.removeItem(k))
    } catch (e) {
      console.warn('clearAiLocalCache failed', e)
    }
  }

  async function loginUser(username: string, password: string) {
    loading.value = true
    try {
      const response: any = await login({ username, password })
      token.value = response.token
      if (response.user) {
        response.user.role = response.user.role?.toLowerCase()
      }
      // 清理旧用户缓存，避免切换账户时残留上一个用户的数据
      try {
        const scheduleStore = useScheduleStore()
        const taskStore = useTaskStore()
        const timeStore = useTimeRecordStore()
        const todoStore = useTodoStore()
        scheduleStore.reset()
        taskStore.reset()
        // 清理本地持久化（starred/archived）
        try { if (typeof (taskStore as any).resetPersisted === 'function') (taskStore as any).resetPersisted() } catch (e) {}
        timeStore.reset()
        // todoStore 暂无 reset 方法，使用 setTasksOrder 清空
        if (typeof (todoStore as any).setTasksOrder === 'function') (todoStore as any).setTasksOrder([])
      } catch (e) { console.warn('clear stores before login failed', e) }

      // 清理 AI 相关本地缓存，避免迁移旧会话或残留的待创建数据
      try { clearAiLocalCache() } catch (e) {}

      user.value = response.user
      saveToken(response.token)
      return response
    } finally {
      loading.value = false
    }
  }

  async function registerUser(username: string, email: string, password: string, categories?: { name: string; color?: string }[]) {
    loading.value = true
    try {
      const payload: any = { username, email, password }
      if (categories && categories.length > 0) payload.categories = categories
      // debug: log register payload to help trace category delivery
      try { console.log('register payload', JSON.parse(JSON.stringify(payload))) } catch (e) {}
      const response: any = await register(payload)
      // 清理旧用户数据，确保注册/自动登录后没有残留
      try {
        const scheduleStore = useScheduleStore()
        const taskStore = useTaskStore()
        const timeStore = useTimeRecordStore()
        const todoStore = useTodoStore()
        scheduleStore.reset()
        taskStore.reset()
        try { if (typeof (taskStore as any).resetPersisted === 'function') (taskStore as any).resetPersisted() } catch (e) {}
        timeStore.reset()
        if (typeof (todoStore as any).setTasksOrder === 'function') (todoStore as any).setTasksOrder([])
      } catch (e) { console.warn('clear stores after register failed', e) }

      // 清理 AI 相关本地缓存，避免迁移旧会话或残留的待创建数据
      try { clearAiLocalCache() } catch (e) {}

      token.value = response.token
      user.value = response.user
      saveToken(response.token)
      return response
    } finally {
      loading.value = false
    }
  }

  async function fetchUserInfo() {
    if (!token.value) return
    loading.value = true
    try {
      const response: any = await getCurrentUser()
      if (response) {
        response.role = response.role?.toLowerCase()
      }
      user.value = response
    } finally {
      loading.value = false
    }
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    const response: any = await changePasswordApi({ oldPassword, newPassword })
    return response
  }

  async function uploadAvatar(file: File) {
    const response: any = await uploadAvatarApi(file)
    if (response) {
      user.value = response
      return response
    }
    throw new Error('上传头像失败')
  }

  async function updateProfile(email: string, nickname: string) {
    const response: any = await updateProfileApi({ email, nickname })
    if (response) {
      user.value = response
      return response
    }
    throw new Error('更新资料失败')
  }

  async function logout() {
    try {
      await apiLogout()
    } catch (error) {
      console.error('Logout error:', error)
    }
    token.value = null
    user.value = null
    removeToken()
    // 清理与用户相关的前端缓存/状态，避免切换账户时残留上一个用户的数据
    try {
      const scheduleStore = useScheduleStore()
      scheduleStore.reset()
    } catch (e) { console.warn('clear schedule store failed', e) }
    try {
      const taskStore = useTaskStore()
      taskStore.reset()
    } catch (e) { console.warn('clear task store failed', e) }
    try {
      const timeStore = useTimeRecordStore()
      timeStore.reset()
    } catch (e) { console.warn('clear time record store failed', e) }
    // 清理 AI 本地缓存，确保登出后不会展示他人会话或弹窗
    try { clearAiLocalCache() } catch (e) {}
  }

  function setToken(newToken: string) {
    token.value = newToken
    saveToken(newToken)
  }

  return {
    user,
    token,
    loading,
    isLoggedIn,
    loginUser,
    registerUser,
    fetchUserInfo,
    changePassword,
    uploadAvatar,
    updateProfile,
    logout,
    setToken,
  }
})
