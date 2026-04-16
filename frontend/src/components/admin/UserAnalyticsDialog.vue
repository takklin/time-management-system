<template>
  <el-dialog
    title="用户行为分析"
    :model-value="true"
    @close="$emit('close')"
    width="70%"
  >
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="chart-container">
          <h4>任务完成率周趋势</h4>
          <p>图表数据将在后续实现</p>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-container">
          <h4>专注时长分布</h4>
          <p>图表数据将在后续实现</p>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <div class="chart-container">
          <h4>活跃时段热力图</h4>
          <p>热力图数据将在后续实现</p>
        </div>
      </el-col>
    </el-row>

    <template #footer>
      <el-button @click="$emit('close')">关闭</el-button>
      <el-button type="primary" @click="$emit('close')">导出报告</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'

interface User {
  id: number
  username: string
}

defineProps<{
  user: User
}>()

defineEmits<{
  close: []
}>()
</script>

<style scoped>
.chart-container {
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #f5f7fa;
  text-align: center;
}
</style>
