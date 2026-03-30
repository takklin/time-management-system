<template>
  <div class="admin-operation-logs">
    <el-table :data="logs" stripe border v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="operator" label="操作者" />
      <el-table-column prop="action" label="操作" />
      <el-table-column prop="target" label="目标" />
      <el-table-column prop="createdAt" label="时间" />
      <el-table-column prop="result" label="结果" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLogs } from '@/api/admin/log'

const logs = ref<any[]>([])
const loading = ref(false)

const loadLogs = async () => {
  loading.value = true
  try {
    const res: any = await getLogs({ page: 1, size: 20 })
    logs.value = res.data?.rows || res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<style scoped>
</style>
