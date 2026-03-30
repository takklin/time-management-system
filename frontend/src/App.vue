<template>
  <div class="app-container">
    <el-config-provider>
      <router-view />
    </el-config-provider>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'
import { onMounted } from 'vue'

// const router = useRouter()
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
