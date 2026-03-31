<template>
  <header class="app-header">
    <div class="header-left">
      <div class="logo">
        <div class="logo-placeholder">T</div>
        <span class="system-name">时间管理</span>
      </div>
    </div>

    <div class="header-right">
      <div class="search-box">
        <el-input v-model="searchText" placeholder="搜索任务..." clearable>
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <el-button type="link" @click="showNotifications">
        <el-icon><Bell /></el-icon>
        <el-badge :value="notificationCount" class="badge" />
      </el-button>

      <el-dropdown>
        <div class="user-info">
          <el-avatar :src="userAvatar" :size="40" />
          <span class="username">{{ userStore.user?.username }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="goToProfile">个人中心</el-dropdown-item>
            <el-dropdown-item @click="changePassword">修改密码</el-dropdown-item>
            <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { Search, Bell } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const searchText = ref('')
const notificationCount = ref(0)

const userAvatar = computed(() => {
  const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0f476b6411127aa8e8b9be76153.jpeg'
  const avatar = userStore.user?.avatar || ''
  return avatar ? (avatar.startsWith('http') ? avatar : avatar) : defaultAvatar
})

const showNotifications = () => {
  ElMessage.info('暂无新通知')
}

const goToProfile = () => {
  router.push('/profile')
}

const changePassword = () => {
  // TODO: 实现修改密码功能
  ElMessage.info('修改密码功能开发中')
}

const logout = async () => {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  background-color: #fff;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-placeholder {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #409eff, #667eea);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 18px;
}

.system-name {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
  justify-content: flex-end;
}

.search-box {
  width: 200px;
}

.badge {
  margin-left: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #333;
}
</style>
