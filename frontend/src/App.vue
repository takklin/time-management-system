<template>
  <div class="app-container">
    <el-config-provider>
      <router-view />
    </el-config-provider>
    <!-- AI 助手浮窗（仅普通用户登录后显示，管理员和未登录用户不显示） -->
    <AIChatAssistant v-if="userStore.isLoggedIn && userStore.user?.role !== 'admin'" />
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'
import { onMounted } from 'vue'
import AIChatAssistant from '@/components/user/AIChatAssistant.vue'

const userStore = useUserStore()

onMounted(() => {
  // 检查是否已登录
  const token = localStorage.getItem('token')
  if (token) {
    userStore.setToken(token)
  }
})
</script>

<style scoped>
.app-container {
  width: 100%;
  height: 100%;
}
</style>
