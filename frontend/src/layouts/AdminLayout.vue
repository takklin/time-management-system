<template>
  <div class="admin-layout">
    <AppHeader />
    <div class="admin-main">
      <el-menu
        :default-active="activeMenu"
        class="admin-sidebar"
        :collapse="isCollapse"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>管理员仪表板</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/system">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Memo /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item index="/admin/ai-assistant">
          <el-icon><Management /></el-icon>
          <span>AI 智能助手</span>
        </el-menu-item>
        <el-menu-item index="/admin/backup">
          <el-icon><Download /></el-icon>
          <span>数据备份</span>
        </el-menu-item>
        <el-menu-item index="/admin/config">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
      </el-menu>

      <div class="admin-content">
        <router-view v-slot="{ Component }">
          <Transition name="fade" mode="out-in">
            <component :is="Component" :key="$route.fullPath" />
          </Transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { Monitor, User, Setting, Memo, Management, Download } from '@element-plus/icons-vue'

const router = useRouter()
const isCollapse = ref(false)

const activeMenu = computed(() => {
  return router.currentRoute.value.path
})

const handleMenuSelect = (index: string) => {
  router.push(index)
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
}

.admin-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.admin-sidebar {
  width: 200px;
  height: 100%;
  border-right: 1px solid #dcdfe6;
  overflow-y: auto;
}

.admin-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
