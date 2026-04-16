<template>
  <el-drawer
    :title="`用户详情 - ${user?.username}`"
    :model-value="true"
    @close="$emit('close')"
    size="50%"
  >
    <el-tabs>
      <!-- 基本信息 Tab -->
      <el-tab-pane label="基本信息">
        <el-form label-width="100px">
          <el-form-item label="用户名">{{ user?.username }}</el-form-item>
          <el-form-item label="邮箱">{{ user?.email }}</el-form-item>
          <el-form-item label="昵称">{{ user?.nickname || '-' }}</el-form-item>
          <el-form-item label="角色">{{ user?.role?.toUpperCase() }}</el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 任务概览 Tab -->
      <el-tab-pane label="任务概览">
        <p>任务统计数据将在后续实现</p>
      </el-tab-pane>

      <!-- 时间记录 Tab -->
      <el-tab-pane label="时间记录">
        <p>专注时长数据将在后续实现</p>
      </el-tab-pane>

      <!-- 行为轨迹 Tab -->
      <el-tab-pane label="行为轨迹">
        <p>操作日志将在后续实现</p>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div style="flex: auto">
        <el-button @click="$emit('close')">关闭</el-button>
        <el-button type="primary" @click="handleResetPassword">重置密码</el-button>
        <el-button type="danger" @click="handleDisable">禁用</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'

interface User {
  id: number
  username: string
  email: string
  nickname?: string
  role?: string
}

defineProps<{
  user: User
}>()

defineEmits<{
  close: []
}>()

const handleResetPassword = () => {
  ElMessage.info('重置密码功能将在后续实现')
}

const handleDisable = () => {
  ElMessage.info('禁用用户功能将在后续实现')
}
</script>
