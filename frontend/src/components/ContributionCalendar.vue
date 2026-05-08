<template>
  <el-card class="chart-card">
    <div class="calendar-header">
      <div class="title">
        📆 专注贡献日历
        <span class="year-selector">
          <el-button-group size="small">
            <el-button :disabled="year === currentYear" @click="setYear(year - 1)">-</el-button>
            <el-button disabled>{{ year }}</el-button>
            <el-button :disabled="year === currentYear" @click="setYear(year + 1)">+</el-button>
          </el-button-group>
        </span>
      </div>
      <div class="stats-badge">🔥 最长连续专注：<strong>{{ maxStreak }}</strong> 天</div>
    </div>
    <div ref="calendarChartRef" style="height: 200px; width: 100%"></div>
    <div class="legend">
      <span>专注时长：</span>
      <span>■ 无</span>
      <span>■ 1-30min</span>
      <span>■ 31-60min</span>
      <span>■ 60-120min</span>
      <span>■ 120+min</span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

type TimeRecord = { startTime?: string; duration?: number }

const props = defineProps<{ records?: TimeRecord[] }>()
const calendarChartRef = ref<HTMLDivElement | null>(null)
const year = ref<number>(dayjs().year())
const currentYear = dayjs().year()
const maxStreak = ref<number>(0)
let chartInstance: echarts.ECharts | null = null

const getLevel = (minutes: number) => {
  if (!minutes || minutes <= 0) return 0
  if (minutes <= 30) return 1
  if (minutes <= 60) return 2
  if (minutes <= 120) return 3
  return 4
}

const getDailyMinutes = (records: TimeRecord[] = []) => {
  const map = new Map<string, number>()
  records.forEach(r => {
    const date = dayjs(r.startTime).format('YYYY-MM-DD')
    const mins = Number(r.duration || 0)
    map.set(date, (map.get(date) || 0) + mins)
  })
  return map
}

const computeStreak = (dailyMap: Map<string, number>) => {
  const dates = Array.from(dailyMap.keys()).sort()
  let streak = 0, maxStreakVal = 0
  let prev: dayjs.Dayjs | null = null
  for (const d of dates) {
    const cur = dayjs(d)
    if (prev && cur.diff(prev, 'day') === 1) streak++
    else streak = 1
    maxStreakVal = Math.max(maxStreakVal, streak)
    prev = cur
  }
  return maxStreakVal
}

const render = () => {
  if (!calendarChartRef.value) return
  const records = props.records || []
  const dailyMap = getDailyMinutes(records)
  maxStreak.value = computeStreak(dailyMap)

  const start = dayjs(`${year.value}-01-01`)
  const end = dayjs(`${year.value}-12-31`)
  const data: Array<[string, number]> = []
  for (let d = start.clone(); d.isBefore(end.add(1, 'day')); d = d.add(1, 'day')) {
    const dateStr = d.format('YYYY-MM-DD')
    const minutes = dailyMap.get(dateStr) || 0
    data.push([dateStr, getLevel(minutes)])
  }

  const option = {
    visualMap: {
      min: 0,
      max: 4,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      inRange: { color: ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'] },
      itemWidth: 22,
      itemHeight: 12,
      text: ['高','低'],
      textStyle: { fontSize: 10 }
    },
    calendar: {
      range: [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')],
      cellSize: ['auto', 16],
      yearLabel: { show:false },
      monthLabel: { show:true, nameMap:'cn', color:'#333', fontSize:12 },
      dayLabel: { show:true, nameMap:'cn', color:'#666', fontSize:10 },
      itemStyle: { borderRadius: 3, borderWidth: 0 },
      splitLine: { show:false }
    },
    series: {
      type:'heatmap',
      coordinateSystem:'calendar',
      data,
      tooltip: {
        formatter: (params: any) => {
          const date = params.data[0]
          const level = params.data[1]
          let text = ''
          if (level === 1) text = '专注 1-30 分钟'
          else if (level === 2) text = '专注 31-60 分钟'
          else if (level === 3) text = '专注 60-120 分钟'
          else if (level === 4) text = '专注 >120 分钟'
          else text = '无记录'
          return `${date}<br/>${text}`
        }
      },
      emphasis: { scale:false }
    }
  }

  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(calendarChartRef.value)
  chartInstance.setOption(option)
}

const setYear = (newYear: number) => {
  year.value = newYear
  render()
}

watch(() => props.records, () => {
  render()
}, { deep: true, immediate: true })

onMounted(() => {
  nextTick(() => render())
})
</script>

<style scoped>
.chart-card { margin-bottom:20px; }
.calendar-header { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; margin-bottom:12px; padding:0 8px;}
.title { font-weight:600; color:#2c3e50; display:flex; align-items:center; gap:12px; }
.year-selector { margin-left:8px; }
.stats-badge { background:#f0f9ef; padding:4px 12px; border-radius:30px; font-size:0.85rem; color:#216e39; }
.legend { display:flex; gap:20px; justify-content:flex-end; margin-top:10px; font-size:12px; color:#555; padding-right:12px; }
.legend span { margin-right:4px;}
</style>
