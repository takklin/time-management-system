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
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import { useTaskStore } from '@/store/task'
import ChartCard from '@/components/ChartCard.vue'
import { ElMessage } from 'element-plus'
import { getTimeDistribution, getCompletionTrend, getDailyFocus, getEstimateVsActual, getTopTasks } from '@/api/stats'

const taskStore = useTaskStore()
const dateRange = ref([new Date(new Date().getTime() - 7 * 24 * 60 * 60 * 1000), new Date()])
const topTasks = ref<any[]>([])

let chartTimeDistribution: any = null
let chartCompletionTrend: any = null
let chartDailyFocus: any = null
let chartEstimateVsActual: any = null

onMounted(async () => {
  await taskStore.fetchCategories()
  await taskStore.fetchTasks()
  initCharts()
  updateAllCharts()
})

watch(dateRange, () => {
  updateAllCharts()
})

const initCharts = () => {
  chartTimeDistribution = echarts.init(document.getElementById('timeDistribution') as HTMLElement)
  chartCompletionTrend = echarts.init(document.getElementById('completionTrend') as HTMLElement)
  chartDailyFocus = echarts.init(document.getElementById('dailyFocus') as HTMLElement)
  chartEstimateVsActual = echarts.init(document.getElementById('estimateVsActual') as HTMLElement)
}

const getRangeDays = (range: any[]) => {
  const start = new Date(range[0])
  const end = new Date(range[1])
  const days: string[] = []
  const cur = new Date(start)
  while (cur <= end) {
    days.push(cur.toISOString().slice(0, 10))
    cur.setDate(cur.getDate() + 1)
  }
  return days
}

const updateAllCharts = async () => {
  const categories = taskStore.categories || []
  const [start, end] = dateRange.value
  const startStr = new Date(start).toISOString().slice(0,10)
  const endStr = new Date(end).toISOString().slice(0,10)

  try {
    const resTD: any = await getTimeDistribution(startStr, endStr)
    const td = (resTD && (resTD.data || resTD)) || []
    const pieData = (td || []).map((r: any) => ({ name: r.name || r.key || r["name"] || '未分类', value: Number(r.value || r.count || 0) }))
    const pieColors = (pieData || []).map((p: any) => {
      const cat = categories.find((c: any) => String(c.name) === String(p.name) || String(c.id) === String(p.name))
      return (cat && cat.color) || undefined
    })
    chartTimeDistribution.setOption({ tooltip: { trigger: 'item' }, legend: { orient: 'vertical', left: 'left' }, series: [{ name: '时间分配', type: 'pie', radius: '50%', data: pieData, color: pieColors }] })
  } catch (e) {
    console.error('time distribution load failed', e)
  }

  try {
    const resCT: any = await getCompletionTrend(startStr, endStr)
    const payload = (resCT && (resCT.data || resCT)) || { dates: [], completed: [], total: [] }
    const dates = payload.dates || []
    const completed = payload.completed || []
    const total = payload.total || []
    chartCompletionTrend.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const idx = params && params[0] && params[0].dataIndex ? params[0].dataIndex : 0
          const d = dates[idx] || ''
          const comp = completed[idx] || 0
          const tot = total[idx] || 0
          return `${d}<br/>完成：${comp}<br/>创建：${tot}`
        }
      },
      xAxis: { type: 'category', data: dates.map((d: string) => d.slice(5)) },
      yAxis: { type: 'value' },
      series: [
        { name: '完成', data: completed, type: 'line', smooth: true, itemStyle: { color: '#409eff' }, areaStyle: { color: 'rgba(64, 158, 255, 0.18)' } },
        { name: '创建', data: total, type: 'line', smooth: true, itemStyle: { color: '#67C23A' } }
      ]
    })
  } catch (e) {
    console.error('completion trend load failed', e)
  }

  try {
    const resDF: any = await getDailyFocus(startStr, endStr)
    const payload = (resDF && (resDF.data || resDF)) || { dates: [], minutes: [] }
    const dates = payload.dates || []
    const minutes = payload.minutes || []
    chartDailyFocus.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const idx = params && params[0] && params[0].dataIndex ? params[0].dataIndex : 0
          const d = dates[idx] || ''
          const m = (minutes[idx] || 0)
          return `${d}<br/>专注时长：${m} 分钟`
        }
      },
      xAxis: { type: 'category', data: dates.map((d: string) => d.slice(5)) },
      yAxis: { type: 'value' },
      series: [{ data: (minutes || []).map((m: any) => Math.round((m || 0) * 100) / 100), type: 'bar', itemStyle: { color: '#67C23A' } }]
    })
  } catch (e) {
    console.error('daily focus load failed', e)
  }

  try {
    const resEA: any = await getEstimateVsActual(startStr, endStr)
    const payload = (resEA && (resEA.data || resEA)) || []
    // payload likely contains task rows: { title, estimated, actual }
    const list = (payload || []).map((r: any) => ({ name: r.title || r.name || '未知', est: Number(r.estimated || r.estimated || 0), act: Number(r.actual || r.actual || 0) }))
      .sort((a: any,b: any) => (b.act || 0) - (a.act || 0)).slice(0,5)

    chartEstimateVsActual.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          // params[0] = 预估(hours), params[1] = 实际(hours)
          const name = params && params[0] && params[0].axisValue ? params[0].axisValue : ''
          const estHours = params && params[0] ? params[0].value : 0
          const actHours = params && params[1] ? params[1].value : 0
          const estMin = Math.round(estHours * 60)
          const actMin = Math.round(actHours * 60)
          return `${name}<br/>预估：${estHours} 小时 (${estMin} 分钟)<br/>实际：${actHours} 小时 (${actMin} 分钟)`
        }
      },
      xAxis: { type: 'category', data: list.map((c: any) => c.name) },
      yAxis: { type: 'value' },
      series: [ { name: '预估', type: 'bar', data: list.map((c: any) => Math.round((c.est/60)*100)/100), itemStyle: { color: '#409eff' } }, { name: '实际', type: 'bar', data: list.map((c: any) => Math.round((c.act/60)*100)/100), itemStyle: { color: '#f56c6c' } } ],
      legend: { data: ['预估','实际'] }
    })

    // 使用后端按 time_record 聚合的 Top Tasks，优先调用专用接口
    try {
      const resTop: any = await getTopTasks(startStr, endStr, 10)
      const topPayload = (resTop && (resTop.data || resTop)) || []
      topTasks.value = (topPayload || []).map((r: any) => ({ id: r.taskId || r.id || r.title, title: r.title || r.name || '未知', duration: Math.round(((Number(r.totalMinutes || r.total || 0) || 0)/60)*100)/100 }))
    } catch (err) {
      console.error('load top tasks failed', err)
      // fallback: use estimate-vs-actual list
      topTasks.value = (list || []).map((t: any) => ({ id: t.name, title: t.name, duration: Math.round(((t.act||0)/60)*100)/100 }))
    }
  } catch (e) {
    console.error('estimate vs actual load failed', e)
  }
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
