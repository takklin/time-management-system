import service from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: {
    id: number
    username: string
    email: string
  }
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

/**
 * 用户登录
 */
export function login(data: LoginRequest) {
  return service.post('/v1/auth/login', data)
}

/**
 * 用户注册
 */
export function register(data: RegisterRequest) {
  return service.post('/v1/auth/register', data)
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return service.get('/v1/auth/user')
}

/**
 * 修改密码
 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return service.post('/v1/auth/change-password', data)
}

/**
 * 上传头像（file form-data）
 */
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return service.post('/v1/auth/avatar', formData)
}

/**
 * 更新个人资料
 */
export function updateProfile(data: { email: string; nickname: string }) {
  return service.put('/v1/auth/profile', data)
}

/**
 * 用户登出
 */
export function logout() {
  return service.post('/v1/auth/logout')
}
