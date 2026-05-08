<template>
  <div class="admin-system-stat">
    <el-row :gutter="20">
      <el-col :span="6" v-for="(card, idx) in overviewCards" :key="idx">
        <el-card>
          <h4>{{ card.title }}</h4>
          <p class="stat-value">{{ card.value }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <div class="card-header">
            <h3>用户增长</h3>
            <div class="card-actions">
              <el-button link @click="reload">刷新</el-button>
              <el-button type="primary" @click="exportCsv">导出 CSV</el-button>
            </div>
          </div>
          <div ref="userGrowthChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <h3>用户活跃度（DAU/WAU）</h3>
          <div ref="userActivityChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <h3>任务趋势（创建 vs 完成）</h3>
          <div ref="taskTrendChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <h3>分类使用排行</h3>
          <div ref="categoryChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="16">
        <el-card>
          <h3>专注时长热力图（小时 x 星期）</h3>
          <div ref="focusHeatmapChart" style="height:360px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <h3>用户行为排行榜</h3>
          <el-table :data="data.userRanking" style="width:100%">
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="taskCount" label="任务数" />
            <el-table-column prop="focusTime" label="专注时长(分钟)" />
            <el-table-column prop="completionRate" label="完成率(%)" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getFullStatistics } from '@/api/admin/systemStat'

const data = ref<any>({ userGrowth: [], userActivity: [], taskTrend: [], focusHeatmap: [], categoryRanking: [], userRanking: [], overview: {} })

const overviewCards = ref([
  { title: '总用户数', value: 0 },
  { title: '活跃用户(7天)', value: 0 },
  { title: '平均完成率(%)', value: 0 },
  { title: '系统健康', value: '正常' },
])

const userGrowthChart = ref<HTMLDivElement | null>(null)
const userActivityChart = ref<HTMLDivElement | null>(null)
const taskTrendChart = ref<HTMLDivElement | null>(null)
const categoryChart = ref<HTMLDivElement | null>(null)
const focusHeatmapChart = ref<HTMLDivElement | null>(null)

let userGrowthInst: any = null
let userActivityInst: any = null
let taskTrendInst: any = null
let categoryInst: any = null
let focusHeatmapInst: any = null

const load = async () => {
  try {
    const res: any = await getFullStatistics(30)
    const payload = res.data || res
    data.value = payload
    overviewCards.value = [
      { title: '总用户数', value: payload.overview?.totalUsers || 0 },
      { title: '活跃用户(7天)', value: payload.overview?.activeUsersLast7Days || 0 },
      { title: '平均完成率(%)', value: payload.overview?.averageCompletionRate ? Math.round(payload.overview.averageCompletionRate * 100) : 0 },
      { title: '系统健康', value: payload.overview?.systemHealth || '未知' },
    ]

    renderUserGrowth(payload.userGrowth || [])
    renderUserActivity(payload.userActivity || [])
    renderTaskTrend(payload.taskTrend || [])
    renderCategory(payload.categoryRanking || [])
    renderFocusHeatmap(payload.focusHeatmap || [])
  } catch (e) {
    console.error(e)
    ElMessage.error('加载系统统计失败')
  }
}

onMounted(load)

const renderUserGrowth = (arr: any[]) => {
  const dates = arr.map(x => x.date || x.day || '')
  const cumulative = arr.map(x => x.cumulative || x.total || x.count || 0)
  const news = arr.map(x => x.new || x.newUsers || x.add || 0)
  if (!userGrowthInst && userGrowthChart.value) userGrowthInst = echarts.init(userGrowthChart.value)
  userGrowthInst.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['累计用户','新增用户'] },
    xAxis: { type: 'category', data: dates },
    yAxis: [{ type: 'value' }, { type: 'value' }],
    series: [
      { name: '累计用户', type: 'line', data: cumulative, yAxisIndex: 0 },
      { name: '新增用户', type: 'bar', data: news, yAxisIndex: 1 }
    ]
  })
}

const renderUserActivity = (arr: any[]) => {
  const dates = arr.map(x => x.date || '')
  const dau = arr.map(x => x.dau || x.count || 0)
  const wau = arr.map(x => x.wau || x.week || 0)
  if (!userActivityInst && userActivityChart.value) userActivityInst = echarts.init(userActivityChart.value)
  userActivityInst.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['DAU','WAU'] },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [ { name: 'DAU', type: 'bar', data: dau }, { name: 'WAU', type: 'bar', data: wau } ]
  })
}

const renderTaskTrend = (arr: any[]) => {
  const dates = arr.map(x => x.date || '')
  const created = arr.map(x => x.created || x.create || 0)
  const completed = arr.map(x => x.completed || x.finished || 0)
  if (!taskTrendInst && taskTrendChart.value) taskTrendInst = echarts.init(taskTrendChart.value)
  taskTrendInst.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['创建','完成'] },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '创建', type: 'line', stack: 'A', areaStyle: {}, data: created },
      { name: '完成', type: 'line', stack: 'A', areaStyle: {}, data: completed }
    ]
  })
}

const renderCategory = (arr: any[]) => {
  const names = arr.map(x => x.name || x.category || x.key)
  const values = arr.map(x => x.value || x.count || x.taskCount || x.num || 0)
  if (!categoryInst && categoryChart.value) categoryInst = echarts.init(categoryChart.value)
  categoryInst.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{ name: '分类', type: 'pie', radius: '60%', data: names.map((n,i) => ({ name: n, value: values[i] })) }]
  })
}

const renderFocusHeatmap = (arr: any[]) => {
  const days = ['一','二','三','四','五','六','日']
  const hours = Array.from({ length: 24 }, (_, i) => String(i))
  const data3 = arr.map(x => [ (x.hour || 0), ((x.dayOfWeek || 1) - 1), x.value || x.count || 0 ])
  if (!focusHeatmapInst && focusHeatmapChart.value) focusHeatmapInst = echarts.init(focusHeatmapChart.value)
  focusHeatmapInst.setOption({
    tooltip: {},
    xAxis: { type: 'category', data: hours },
    yAxis: { type: 'category', data: days },
    visualMap: { min: 0, max: 100, orient: 'vertical', left: 'right' },
    series: [{ name: '热力', type: 'heatmap', data: data3, label: { show: false } }]
  })
}

const reload = () => { load() }

const exportCsv = () => {
  const s = data.value
  if (!s) return ElMessage.info('暂无数据可导出')
  const lines: string[] = []
  lines.push('系统统计导出')
  lines.push('')
  lines.push('用户增长')
  lines.push(['日期','累计','新增'].join(','))
  ;(s.userGrowth || []).forEach((r: any) => lines.push([r.date || '', r.cumulative || r.total || r.count || 0, r.new || r.newUsers || 0].join(',')))
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', `system_stats_${new Date().toISOString().slice(0,10)}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.stat-value { font-size: 24px; font-weight: bold; margin-top: 8px }
.card-header { display: flex; justify-content: space-between; align-items: center }
.card-actions { display: flex; gap: 8px }
</style>
