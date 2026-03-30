<template>
  <div class="statistics-container">
    <div class="stats-header">
      <h1>数据统计</h1>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="onDateRangeChange"
      />
    </div>

    <div class="stats-grid">
      <ChartCard title="时间分配">
        <div id="timeDistribution" style="height: 300px"></div>
      </ChartCard>

      <ChartCard title="任务完成率趋势">
        <div id="completionTrend" style="height: 300px"></div>
      </ChartCard>

      <ChartCard title="每日专注时长">
        <div id="dailyFocus" style="height: 300px"></div>
      </ChartCard>

      <ChartCard title="预估 vs 实际耗时">
        <div id="estimateVsActual" style="height: 300px"></div>
      </ChartCard>

      <ChartCard title="任务排行榜">
        <el-table :data="topTasks" stripe>
          <el-table-column label="排名" width="50">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="title" label="任务名称" />
          <el-table-column prop="duration" label="耗时（小时）" width="120" />
        </el-table>
      </ChartCard>
    </div>

    <div class="export-section">
      <el-button type="primary" @click="exportData">导出数据</el-button>
      <el-button @click="exportImage">导出图表</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import ChartCard from '@/components/ChartCard.vue'
import { ElMessage } from 'element-plus'

const dateRange = ref([new Date(new Date().getTime() - 7 * 24 * 60 * 60 * 1000), new Date()])
const topTasks = ref([
  { id: 1, title: '项目报告', duration: 15 },
  { id: 2, title: '代码审查', duration: 12 },
  { id: 3, title: '需求分析', duration: 10 },
  { id: 4, title: '会议', duration: 8 },
  { id: 5, title: '文档编写', duration: 6 },
])

onMounted(() => {
  initCharts()
})

const initCharts = () => {
  // 时间分配饼图
  const timeDistribution = echarts.init(document.getElementById('timeDistribution'))
  timeDistribution.setOption({
    tooltip: { trigger: 'item' },
    legend: {
      orient: 'vertical',
      left: 'left',
    },
    series: [
      {
        name: 'Access From',
        type: 'pie',
        radius: '50%',
        data: [
          { value: 1048, name: '工作' },
          { value: 735, name: '学习' },
          { value: 580, name: '运动' },
          { value: 484, name: '休息' },
          { value: 300, name: '其他' },
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  })

  // 任务完成率趋势
  const completionTrend = echarts.init(document.getElementById('completionTrend'))
  completionTrend.setOption({
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        data: [80, 85, 75, 90, 88, 92, 87],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#409eff' },
        areaStyle: { color: 'rgba(64, 158, 255, 0.3)' },
      },
    ],
  })

  // 每日专注时长
  const dailyFocus = echarts.init(document.getElementById('dailyFocus'))
  dailyFocus.setOption({
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        data: [4, 5, 3, 6, 4, 3, 3],
        type: 'bar',
        itemStyle: { color: '#67C23A' },
      },
    ],
  })

  // 预估 vs 实际耗时
  const estimateVsActual = echarts.init(document.getElementById('estimateVsActual'))
  estimateVsActual.setOption({
    xAxis: {
      type: 'category',
      data: ['项目A', '项目B', '项目C', '项目D', '项目E'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        name: '预估',
        data: [20, 30, 25, 35, 28],
        type: 'bar',
        itemStyle: { color: '#409eff' },
      },
      {
        name: '实际',
        data: [22, 28, 28, 32, 30],
        type: 'bar',
        itemStyle: { color: '#f56c6c' },
      },
    ],
    legend: {
      data: ['预估', '实际'],
    },
  })
}

const onDateRangeChange = () => {
  ElMessage.info('日期已更新，图表刷新中...')
  // TODO: 实现日期范围变化后的数据重载
}

const exportData = () => {
  ElMessage.success('数据导出成功')
  // TODO: 实现数据导出
}

const exportImage = () => {
  ElMessage.success('图表导出成功')
  // TODO: 实现图表导出
}
</script>

<style scoped>
.statistics-container {
  padding: 20px;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.stats-header h1 {
  font-size: 24px;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.export-section {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
