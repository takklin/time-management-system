<template>
  <div class="contribution-heatmap">
    <div class="heatmap-header">
        <div class="left">
          <span class="title">📆 专注贡献图</span>
        </div>
              <div class="center">
                <el-dropdown @command="onSelectMonth">
                  <span class="month-label">{{ currentYear }}年 {{ monthNames[selectedMonth] }} <i class="el-icon-arrow-down" /></span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="m in 12" :key="m" :command="m">{{ monthNames[m] }}</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
        <div class="right stats">
          <span>🔥 最长连续专注: <b>{{ maxStreak }}</b> 天</span>
          <span>📅 本月专注天数: <b>{{ totalDays }}</b> 天</span>
        </div>
      </div>
    <div ref="chartRef" class="chart-container"></div>
    <div class="legend">
      <span>少</span>
      <span v-for="(color, i) in gradeColors" :key="i" class="legend-color" :style="{ backgroundColor: color }"></span>
      <span>多</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

type TimeRecord = { startTime?: string; duration?: number }

const props = defineProps<{ records?: TimeRecord[] }>()

const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// GitHub 风格绿色渐变（等级 0..4）
const gradeColors = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39']

// 聚合每天专注分钟（基于传入的 records）
function getDailyMinutes(records?: TimeRecord[]) {
  const map = new Map<string, number>()
  ;(records ?? props.records ?? []).forEach(r => {
    if (!r || !r.startTime) return
    const key = dayjs(r.startTime).format('YYYY-MM-DD')
    const mins = Number(r.duration || 0)
    map.set(key, (map.get(key) || 0) + mins)
  })
  return map
}

// 在给定区间内计算最长连续活跃天数
function computeMaxStreak(dailyMap: Map<string, number>, start: dayjs.Dayjs, end: dayjs.Dayjs) {
  let cur = 0, best = 0
  let d = start.clone()
  while (d.isBefore(end) || d.isSame(end, 'day')) {
    const k = d.format('YYYY-MM-DD')
    if ((dailyMap.get(k) || 0) > 0) { cur++; if (cur > best) best = cur } else { cur = 0 }
    d = d.add(1, 'day')
  }
  return best
}

// 本月总活跃天数
const selectedMonth = ref<number>(dayjs().month() + 1)
const currentYear = dayjs().year()
const monthNames = ['','一月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','十二月']

const totalDays = computed(() => {
  const daily = getDailyMinutes()
  const start = dayjs().year(currentYear).month(selectedMonth.value - 1).startOf('month')
  const end = dayjs().year(currentYear).month(selectedMonth.value - 1).endOf('month')
  let cnt = 0
  let d = start.clone()
  while (d.isBefore(end) || d.isSame(end, 'day')) {
    if ((daily.get(d.format('YYYY-MM-DD')) || 0) > 0) cnt++
    d = d.add(1, 'day')
  }
  return cnt
})

const maxStreak = ref<number>(0)

// 将分钟数映射为等级 0..4（基于本月最大值）
function getColorIndex(minutes: number, maxMinutes: number) {
  if (!minutes || minutes <= 0) return 0
  if (minutes >= maxMinutes) return 4
  if (minutes <= maxMinutes * 0.25) return 1
  if (minutes <= maxMinutes * 0.5) return 2
  return 3
}

function renderChart() {
  if (!chartRef.value) return
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
  chartInstance = echarts.init(chartRef.value)

  const dailyMap = getDailyMinutes()
  const positives = Array.from(dailyMap.values()).filter(v => v > 0)
  const maxMinutes = positives.length ? Math.max(...positives) : 60

  const start = dayjs().year(currentYear).month(selectedMonth.value - 1).startOf('month')
  const end = dayjs().year(currentYear).month(selectedMonth.value - 1).endOf('month')
  const raw: Array<{ date: string; mins: number; idx: number }> = []
  let d = start.clone()
  while (d.isBefore(end) || d.isSame(end, 'day')) {
    const key = d.format('YYYY-MM-DD')
    const mins = dailyMap.get(key) || 0
    raw.push({ date: key, mins, idx: getColorIndex(mins, maxMinutes) })
    d = d.add(1, 'day')
  }

  maxStreak.value = computeMaxStreak(dailyMap, start, end)

  const seriesData = raw.map(i => ({ value: [i.date, i.idx], minutes: i.mins }))

  const option: any = {
    tooltip: {
      formatter: (params: any) => {
        const date = params.value && params.value[0]
        const minutes = params.data && params.data.minutes ? params.data.minutes : 0
        if (!minutes) return `${date}<br/>无专注记录`
        return `${date}<br/>专注时长: ${minutes} 分钟`
      }
    },
    visualMap: { show: false, min: 0, max: 4, inRange: { color: gradeColors } },
    calendar: {
      top: 30, left: 20, right: 20, bottom: 20,
      range: [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')],
      cellSize: ['auto', 20],
      yearLabel: { show: false },
      monthLabel: {
        show: true,
        // ensure month label is Chinese (e.g., 五月)
        formatter: (month: any) => {
          try {
            let txt = ''
            if (month == null) return ''
            if (typeof month === 'object') {
              if (month.name) txt = String(month.name)
              else if (month.start) txt = dayjs(month.start).format('M月')
              else if (month.date) txt = String(month.date)
              else if (month.value) txt = String(month.value)
              else txt = JSON.stringify(month)
            } else {
              txt = String(month)
            }

            const eng = txt.match(/Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec/)
            const monthMap: Record<string,string> = { Jan: '一月', Feb: '二月', Mar: '三月', Apr: '四月', May: '五月', Jun: '六月', Jul: '七月', Aug: '八月', Sep: '九月', Oct: '十月', Nov: '十一月', Dec: '十二月' }
            if (eng && monthMap[eng[0]]) return monthMap[eng[0]]
            const digits = txt.match(/\d{1,2}/)
            if (digits) return `${Number(digits[0])}月`

            const fullEng = txt.match(/January|February|March|April|May|June|July|August|September|October|November|December/i)
            if (fullEng) {
              const short = fullEng[0].slice(0,3)
              if (monthMap[short]) return monthMap[short]
            }

            return monthNames[selectedMonth.value] || txt
          } catch (e) {
            return monthNames[selectedMonth.value] || ''
          }
        },
        color: '#333'
      },
      dayLabel: { show: true, firstDay: 1, nameMap: { Sun: '周日', Mon: '周一', Tue: '周二', Wed: '周三', Thu: '周四', Fri: '周五', Sat: '周六' }, color: '#333' },
      itemStyle: { borderRadius: 4 }
    },
    series: [{
      type: 'heatmap', coordinateSystem: 'calendar', data: seriesData,
      label: { show: false }, itemStyle: { borderWidth: 1, borderColor: '#e6e6e6' }, emphasis: { itemStyle: { borderColor: '#333', borderWidth: 1 } }
    }]
  }

  chartInstance.setOption(option)
}

const resizeHandler = () => chartInstance?.resize()

onMounted(() => { renderChart(); window.addEventListener('resize', resizeHandler) })
onUnmounted(() => { window.removeEventListener('resize', resizeHandler); chartInstance?.dispose() })

watch(() => props.records, () => { renderChart() }, { deep: true })

// 切换月份
const onSelectMonth = (m: number | string) => {
  const mm = Number(m)
  if (mm >= 1 && mm <= 12) {
    selectedMonth.value = mm
    renderChart()
  }
}

watch(() => selectedMonth.value, () => { renderChart() })
</script>

<style scoped>
.contribution-heatmap { background: #fff; border-radius: 12px; padding: 12px; margin-bottom: 20px }
.heatmap-header { display:flex; justify-content:space-between; align-items:baseline; margin-bottom:8px; flex-wrap:wrap }
.title { font-weight:600 }
.stats { display:flex; gap:12px; color:#4b5563 }
.chart-container { width:100%; height:220px }
.legend { display:flex; align-items:center; justify-content:flex-end; gap:6px; margin-top:8px; font-size:12px; color:#6b7280 }
.legend-color { width:12px; height:12px; border-radius:2px }
</style>
