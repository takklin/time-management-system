<template>
  <div class="admin-layout">
    <AppHeader />
    <div class="admin-main">
      <AppSidebar :menu="adminMenu" />
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { adminMenu } from '@/router/adminMenu'
</script>

<style scoped>
.admin-layout { display: flex; flex-direction: column; height: 100vh; }
.admin-main { display: flex; flex: 1; }
.admin-content { flex: 1; overflow-y: auto; background-color: #f5f7fa; padding: 20px; }
</style>
