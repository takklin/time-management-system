import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import JSONbig from 'json-bigint'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const service: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
})

// Use json-bigint to parse JSON responses so very large integer IDs are preserved as strings
const JSONbigParser = JSONbig({ storeAsString: true })
service.defaults.transformResponse = [
  function (data: any) {
    if (!data) return data
    // data is a raw string here; try JSONbig first to preserve big integers
    try {
      return JSONbigParser.parse(data)
    } catch (e) {
      try {
        return JSON.parse(data)
      } catch (err) {
        return data
      }
    }
  },
]

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
    // 支持二进制/文件下载：responseType === 'blob' or 'arraybuffer' 时直接返回 response
    if (response.config && (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer')) {
      return response
    }
    const { data } = response
    const silent = (response.config && (response.config as any).silent) || false
    // 假设后端返回的数据结构为 { code, msg, data }
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
    } else if (data.code === 403) {
      // 权限不足
      if (!silent) ElMessage.error(data.msg || '权限不足，无法访问该接口')
      return Promise.reject(new Error(data.msg || '权限不足'))
    } else {
      // 其他业务错误
      if (!silent) ElMessage.error(data.msg || data.message || '请求失败')
      return Promise.reject(new Error(data.msg || data.message))
    }
  },
  (error) => {
    const silent = (error.config && (error.config as any).silent) || false
    if (error.response) {
      const { status, data, config } = error.response
      if (!silent) console.warn('[API ERROR]', config?.url, status, data)
      // 优先展示后端返回的 msg 字段，其次尝试 message 或 error
      const serverMsg = data && (data.msg || data.message || data.error || (typeof data === 'string' ? data : JSON.stringify(data)))
      if (status === 401 || status === 403) {
        const userStore = useUserStore()
        userStore.logout()
        if (!silent) ElMessage.error('登录已过期或无权限，请重新登录')

        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      } else {
        // 对 5xx/4xx 返回给出更有信息的提示
        if (!silent) ElMessage.error(serverMsg || `Error: ${status}`)
      }
    } else if (error.message === 'Network Error') {
      if (!silent) ElMessage.error('网络错误，请检查网络连接')
    } else {
      if (!silent) ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default service
