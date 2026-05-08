<template>
  <div class="admin-layout">
    <AppHeader />
    <div class="admin-main">
      <AppSidebar />

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
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
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
