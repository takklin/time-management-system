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
            <div ref="completionChart" style="height:240px;"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-container">
            <h4>专注时长分布</h4>
            <div ref="focusChart" style="height:240px;"></div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="24">
          <div class="chart-container">
            <h4>活跃时段（小时）</h4>
            <div ref="hoursChart" style="height:240px;"></div>
          </div>
        </el-col>
      </el-row>

    <template #footer>
      <el-button @click="$emit('close')">关闭</el-button>
      <el-button type="primary" @click="exportReport">导出报告</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import * as userApi from '@/api/admin/userManage'

const props = defineProps<{ user: { id: number; username: string } }>()
defineEmits<{ close: [] }>()

const completionChart = ref<HTMLDivElement | null>(null)
const focusChart = ref<HTMLDivElement | null>(null)
const hoursChart = ref<HTMLDivElement | null>(null)

let completionChartInstance: any = null
let focusChartInstance: any = null
let hoursChartInstance: any = null

const safeNum = (v: any) => (v == null ? 0 : Number(v))

const renderCompletion = (dates: string[], finished: number[], created: number[]) => {
  if (!completionChartInstance && completionChart.value) completionChartInstance = echarts.init(completionChart.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        if (!params || !params.length) return ''
        const date = params[0].axisValue
        let completed = 0
        let total = 0
        params.forEach((p: any) => {
          if (p.seriesName === '完成任务') completed = safeNum(p.data)
          if (p.seriesName === '创建任务') total = safeNum(p.data)
        })
        const rate = total > 0 ? ((completed / total) * 100).toFixed(1) : '0.0'
        return `${date}<br/>完成任务：${completed}<br/>创建任务：${total}<br/>完成率：${rate}%`
      }
    },
    legend: { data: ['完成任务', '创建任务'] },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '任务数' },
    series: [
      { name: '完成任务', type: 'line', smooth: true, data: finished },
      { name: '创建任务', type: 'line', smooth: true, data: created }
    ]
  }
  completionChartInstance.setOption(option, true)
  nextTick(() => setTimeout(() => completionChartInstance?.resize?.(), 80))
}

const renderFocus = (data: any[]) => {
  if (!focusChartInstance && focusChart.value) focusChartInstance = echarts.init(focusChart.value)
  const pieData = (data || []).map((d: any) => ({ name: d.category || d.name || d.label, value: safeNum(d.minutes || d.value) }))
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        // params.percent 可用
        const percent = params.percent != null ? params.percent.toFixed(1) : ''
        return `${params.name}: ${percent}% (${params.value} 分钟)`
      }
    },
    series: [{ type: 'pie', radius: '60%', data: pieData }]
  }
  focusChartInstance.setOption(option, true)
  nextTick(() => setTimeout(() => focusChartInstance?.resize?.(), 80))
}

const renderHours = (data: any[]) => {
  if (!hoursChartInstance && hoursChart.value) hoursChartInstance = echarts.init(hoursChart.value)
  const hours = Array.from({ length: 24 }).map((_, i) => `${i}:00`)
  const map = new Map<number, number>()
  const arr = Array.isArray(data) ? data : []
  arr.forEach((d: any) => {
    const h = Number(d?.hour ?? d?.HOUR ?? d?.h)
    if (Number.isFinite(h) && h >= 0 && h < 24) {
      map.set(h, safeNum(d?.count ?? d?.value ?? d?.val ?? d?.minutes))
    }
  })
  const values = hours.map((_, idx) => map.get(idx) || 0)
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        if (!params || !params.length) return ''
        const p = params[0]
        return `时段：${p.axisValue}<br/>操作次数：${p.value} 次`
      }
    },
    xAxis: { type: 'category', data: hours },
    yAxis: { type: 'value', name: '操作次数' },
    series: [{ type: 'bar', data: values }]
  }
  try {
    hoursChartInstance.setOption(option, true)
  } catch (err) {
    console.error('hours chart setOption error', err)
  }
  nextTick(() => setTimeout(() => { try { hoursChartInstance && hoursChartInstance.resize && hoursChartInstance.resize() } catch(e){ } }, 80))
}

const analyticsData = ref<any>(null)

const loadAnalytics = async (userId: number) => {
  try {
    const res: any = await userApi.getUserAnalytics(userId)
    const data = (res && res.data) ? res.data : res
    analyticsData.value = data

    // 兼容后端返回不同字段名：weeklyCompletion 或 completionRateTrend
    let dates: string[] = []
    let finished: number[] = []
    let created: number[] = []
    if (data.weeklyCompletion && Array.isArray(data.weeklyCompletion)) {
      dates = data.weeklyCompletion.map((d: any) => d.date)
      finished = data.weeklyCompletion.map((d: any) => safeNum(d.completed))
      created = data.weeklyCompletion.map((d: any) => safeNum(d.created))
    } else if (data.completionRateTrend) {
      dates = data.completionRateTrend.dates || []
      finished = (data.completionRateTrend.finished || []).map((v: any) => safeNum(v))
      created = (data.completionRateTrend.total || []).map((v: any) => safeNum(v))
    }
    renderCompletion(dates, finished, created)

    // 专注时长分布
    let focusRaw = data.focusDistribution || data.focusTimeDistribution || []
    renderFocus(focusRaw)

    // 活跃时段
    let hoursRaw = data.hourlyActivity || data.activeHoursHeatmap || data.activeHours || []
    renderHours(hoursRaw)
  } catch (e) {
    console.error('加载分析数据失败', e)
    ElMessage.error('加载分析数据失败')
  }
}

onMounted(() => {
  if (props.user && props.user.id) loadAnalytics(props.user.id)
  // 响应窗口大小变化
  window.addEventListener('resize', resizeCharts)
})

watch(() => props.user, (u) => { if (u && u.id) loadAnalytics(u.id) })

const resizeCharts = () => {
  try {
    completionChartInstance?.resize?.()
    focusChartInstance?.resize?.()
    hoursChartInstance?.resize?.()
  } catch (e) {
    // ignore
  }
}

onBeforeUnmount(() => {
  try {
    window.removeEventListener('resize', resizeCharts)
    completionChartInstance?.dispose?.()
    focusChartInstance?.dispose?.()
    hoursChartInstance?.dispose?.()
  } catch (e) {
    // ignore
  }
})

const exportReport = async () => {
  try {
    const res: any = await userApi.exportUserAnalytics(props.user.id)
    const blob = (res && res.data) ? res.data : res
    if (!blob) {
      ElMessage.error('导出失败：无数据')
      return
    }
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const name = props.user?.username || props.user?.id || 'user'
    link.setAttribute('download', `analytics_${name}_${new Date().toISOString().slice(0,10)}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('导出失败', e)
    ElMessage.error('导出失败')
  }
}
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
