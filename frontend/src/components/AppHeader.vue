<template>
  <header class="app-header">
    <div class="header-left">
      <div class="logo">
        <div class="logo-placeholder">T</div>
        <span class="system-name">时间管理</span>
      </div>
    </div>

    <div class="header-right">
        <div class="search-box" v-if="showSearchBox">
          <el-input v-model="searchText" placeholder="搜索任务..." clearable>
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

      <el-button type="primary" :text="true" ref="bellBtn" @click="showNotifications" :aria-expanded="drawerVisible">
        <el-icon><Bell /></el-icon>
        <el-badge :value="isAdmin ? alertStore.unreadCount : messageStore.unreadCount" class="badge" />
      </el-button>

      <el-drawer v-model="drawerVisible" :title="isAdmin ? '告警中心' : '收件箱'" direction="rtl" size="480px" :teleported="true">
        <!-- Only render dynamic panel when component is available to avoid HMR/undefined errors -->
        <component v-if="(isAdmin ? AlertPanel : MessagePanel)" :is="isAdmin ? AlertPanel : MessagePanel" :key="drawerVisible ? 'open' : 'closed'" />
      </el-drawer>

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
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAlertStore } from '@/store/alert'
import { useMessageStore } from '@/store/message'
import { ElMessage } from 'element-plus'
import { Search, Bell } from '@element-plus/icons-vue'
import AlertPanel from '@/components/AlertPanel.vue'
import MessagePanel from '@/components/MessagePanel.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const searchText = ref('')
const alertStore = useAlertStore()
const messageStore = useMessageStore()
const notificationCount = ref(0)
const drawerVisible = ref(false)

const isAdmin = computed(() => ((userStore.user?.role || '').toLowerCase() === 'admin'))

  const userAvatar = computed(() => {
    const defaultAvatar = '/api/v1/auth/avatar/placeholder.png'
  const avatar = userStore.user?.avatar || ''
  if (!avatar) {
    return defaultAvatar
  }
  // 如果已经是完整 URL，直接返回
  if (avatar.startsWith('http')) {
    return avatar
  }
  // 如果是相对路径，需要检查是否已包含 /api
  if (avatar.startsWith('/api/')) {
    // 如果已经包含 /api，直接使用（由代理转发）
    return avatar
  }
  // 其他情况拼接后端地址
  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return `${baseURL}${avatar}`
})

const showNotifications = async () => {
  try { console.info('[AppHeader] bell clicked -> opening drawer') } catch (e) {}
  try {
    if (isAdmin.value) await alertStore.fetchUnhandled().catch(() => {})
    else await messageStore.fetchMessages({ page: 1, size: 20 }).catch(() => {})
  } catch (e) {}
  drawerVisible.value = true
}

const goToProfile = () => {
  const isAdmin = (userStore.user?.role || '').toLowerCase() === 'admin'
  router.push(isAdmin ? '/admin/profile' : '/dashboard/profile')
}

const changePassword = () => {
  // 跳转到个人中心，由个人中心内实现修改密码
  const isAdmin = (userStore.user?.role || '').toLowerCase() === 'admin'
  router.push(isAdmin ? '/admin/profile' : '/dashboard/profile')
}

const showSearchBox = computed(() => {
  // 管理端路由（/admin/*）不显示顶部搜索
  return !route.path.startsWith('/admin')
})

const logout = async () => {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 打开抽屉时加载告警（避免在登录页面就触发）
watch(drawerVisible, (v) => {
  if (v) {
    try {
      console.info('[AppHeader] drawerVisible opened -> fetching data')
      if (isAdmin.value) alertStore.fetchUnhandled().catch(() => {})
      else messageStore.fetchMessages({ page: 1, size: 20 }).catch(() => {})
    } catch (e) { console.warn('fetch on open failed', e) }
  }
})
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
