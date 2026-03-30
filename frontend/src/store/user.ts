import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, getCurrentUser, logout as apiLogout } from '@/api/auth'
import { removeToken, getToken } from '@/utils/auth'
import { setToken as saveToken } from '@/utils/auth'

export interface User {
  id: number
  username: string
  email: string
  role?: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(getToken())
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  async function loginUser(username: string, password: string) {
    loading.value = true
    try {
      const response: any = await login({ username, password })
      token.value = response.token
      user.value = response.user
      saveToken(response.token)
      return response
    } finally {
      loading.value = false
    }
  }

  async function registerUser(username: string, email: string, password: string) {
    loading.value = true
    try {
      const response: any = await register({ username, email, password })
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
      user.value = response
    } finally {
      loading.value = false
    }
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
    logout,
    setToken,
  }
})
