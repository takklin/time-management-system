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
      <div class="footer-section">
        <p class="section-title">快速操作</p>
        <el-button type="primary" size="small" block @click="quickAddTask">+ 新建任务</el-button>
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

const menuItems = [
  { path: '/dashboard', title: '仪表盘', icon: Grid },
  { path: '/tasks', title: '任务管理', icon: DocumentCopy },
  { path: '/schedules', title: '日程安排', icon: Calendar },
  { path: '/todos', title: '待办事项', icon: List },
  { path: '/time-records', title: '时间记录', icon: Clock },
  { path: '/statistics', title: '数据统计', icon: TrendCharts },
  { path: '/profile', title: '个人中心', icon: User },
]

const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const quickAddTask = () => {
  ElMessage.info('快速添加任务功能开发中')
  router.push('/tasks')
}
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
