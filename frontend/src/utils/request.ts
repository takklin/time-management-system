import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const service: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    const token = userStore.token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    // 假设后端返回的数据结构为 { code, message, data }
    if (data.code === 200) {
      return data.data || data
    } else if (data.code === 401) {
      // Token 过期或无效
      const userStore = useUserStore()
      userStore.logout()
      ElMessage.error('登录已过期，请重新登录')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(new Error('Unauthorized'))
    } else {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
  },
  (error) => {
    if (error.response) {
      const { status, data, config } = error.response
      console.warn('[API ERROR]', config.url, status, data)
      if (status === 401 || status === 403) {
        const userStore = useUserStore()
        userStore.logout()
        ElMessage.error('登录已过期或无权限，请重新登录')

        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      } else {
        ElMessage.error(data?.message || `Error: ${status}`)
      }
    } else if (error.message === 'Network Error') {
      ElMessage.error('网络错误，请检查网络连接')
    } else {
      ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default service
