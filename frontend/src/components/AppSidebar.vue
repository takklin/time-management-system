<template>
  <aside class="app-sidebar">
    <nav class="sidebar-nav">
      <router-link
        v-for="menu in menuItems"
        :key="menu.path"
        :to="menu.path"
        class="nav-item"
        :class="{ active: isActive(menu.path) }"
      >
        <el-icon class="nav-icon"><component :is="menu.icon" /></el-icon>
        <span class="nav-title">{{ menu.title }}</span>
      </router-link>
    </nav>

    <div class="sidebar-footer">
      <div v-if="isAdminRoute">
        <div class="footer-section">
          <p class="section-title">快捷操作</p>
          <el-button type="primary" size="small" block @click="goBackup">数据库备份</el-button>
          <el-button type="info" size="small" block @click="goConfig" style="margin-top:8px">系统配置</el-button>
          <el-button type="warning" size="small" block @click="goLogs" style="margin-top:8px">操作日志</el-button>
        </div>
      </div>
      <div v-else>
        <div class="footer-section">
          <p class="section-title">快速操作</p>
          <el-button type="primary" size="small" block @click="quickAddTask">+ 新建任务</el-button>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Grid,
  DocumentCopy,
  Calendar,
  List,
  Clock,
  TrendCharts,
  User,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

import { computed } from 'vue'
import { useUserStore } from '@/store/user'
import { adminMenu } from '@/router/adminMenu'

const userStore = useUserStore()

const userMenu = [
  { path: '/dashboard', title: '仪表盘', icon: Grid },
  { path: '/dashboard/tasks', title: '任务管理', icon: DocumentCopy },
  { path: '/dashboard/schedules', title: '日程安排', icon: Calendar },
  { path: '/dashboard/todos', title: '待办事项', icon: List },
  { path: '/dashboard/time-records', title: '时间记录', icon: Clock },
  { path: '/dashboard/statistics', title: '数据统计', icon: TrendCharts },
  { path: '/dashboard/profile', title: '个人中心', icon: User },
]

const menuItems = computed(() => {
  const role = userStore.user?.role?.toLowerCase()
  if (role === 'admin') {
    // 兼容 element-plus 图标
    return adminMenu.map(item => ({
      ...item,
      icon: resolveIcon(item.icon)
    }))
  }
  return userMenu
})

function resolveIcon(iconName: string): any {
  // 兼容 element-plus 图标组件
  const icons: Record<string, any> = { Grid, DocumentCopy, Calendar, List, Clock, TrendCharts, User }
  if (iconName && iconName.startsWith('el-icon-')) {
    // 仅支持部分常用图标
    if (iconName === 'el-icon-s-data') return TrendCharts
    if (iconName === 'el-icon-user-solid') return User
    if (iconName === 'el-icon-data-analysis') return TrendCharts
    if (iconName === 'el-icon-document') return DocumentCopy
    if (iconName === 'el-icon-download') return Clock
    if (iconName === 'el-icon-setting') return Grid
    if (iconName === 'el-icon-magic-stick') return Grid
  }
  return icons[iconName] || Grid
}

const isActive = (path: string) => {
  // 只高亮精确匹配的菜单项
  return route.path === path
}

const quickAddTask = () => {
  ElMessage.info('快速添加任务功能开发中')
  router.push('/dashboard/tasks')
}

const goBackup = () => {
  router.push('/admin/backup')
}

const goConfig = () => {
  router.push('/admin/config')
}

const goLogs = () => {
  router.push('/admin/logs')
}

const isAdminRoute = computed(() => {
  return route.path.startsWith('/admin')
})
</script>

<style scoped>
.app-sidebar {
  width: 220px;
  background-color: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 12px 0 rgba(0, 0, 0, 0.1);
}

.sidebar-nav {
  flex: 1;
  padding: 20px 0;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: #666;
  text-decoration: none;
  transition: all 0.3s;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.nav-item.active {
  background-color: #e6f2ff;
  color: #409eff;
  border-left-color: #409eff;
}

.nav-icon {
  font-size: 18px;
}

.nav-title {
  font-size: 14px;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid #ebeef5;
}

.footer-section {
  margin-bottom: 15px;
}

.section-title {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}
</style>
