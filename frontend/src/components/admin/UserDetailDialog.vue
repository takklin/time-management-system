<template>
  <el-dialog :visible.sync="visible" title="用户详情" width="500px">
    <el-descriptions :column="1" v-if="user">
      <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ user.email }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ user.role }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ user.deleted === 0 ? '启用' : '禁用' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ user.createdAt }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ user.updatedAt }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="close">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
const props = defineProps<{ user: any }>()
const emit = defineEmits<{ (event: 'close'): void }>()
const visible = ref(true)

watch(() => props.user, v => {
  if (v) visible.value = true
})

const close = () => {
  visible.value = false
  emit('close')
}
</script>
