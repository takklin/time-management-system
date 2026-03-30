<template>
  <div class="admin-system-stat">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <h3>用户总数</h3>
          <p class="stat-value">{{ stat.userCount }}</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <h3>任务总数</h3>
          <p class="stat-value">{{ stat.taskCount }}</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <h3>日程总数</h3>
          <p class="stat-value">{{ stat.scheduleCount }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSystemStat } from '@/api/admin/systemStat'

const stat = ref({ userCount: 0, taskCount: 0, scheduleCount: 0 })

const loadStat = async () => {
  const res: any = await getSystemStat()
  stat.value = res.data || res
}

onMounted(loadStat)
</script>

<style scoped>
.stat-value { font-size: 28px; font-weight: bold; margin-top: 10px; }
</style>
