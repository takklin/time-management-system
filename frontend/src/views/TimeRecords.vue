<template>
  <div class="time-records-container">
    <!-- timer-section 已移除 -->

    <div class="records-section">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px">
          <div style="flex:1; display:flex; align-items:center; gap:12px">
            <h2 style="margin:0">时间记录历史</h2>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              unlink-panels
              @change="onDateRangeChange"
            />
          </div>
          <div class="stats-cards">
            <el-card class="stat-card">今日: <strong>{{ todayTotal }} 分钟</strong></el-card>
            <el-card class="stat-card">本周: <strong>{{ thisWeekTotal }} 分钟</strong> (<span :style="{color: weekRatio>=0? '#67c23a':'#f56c6c'}">{{ weekRatio }}%</span>)</el-card>
          </div>
        </div>

        <div style="margin-top:16px;display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:16px">
          <div>
            <el-card class="chart-card">
              <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 12px">
                <div style="font-weight:600">近 7 天 专注趋势</div>
                <div style="font-size:12px;color:#888">单位：分钟</div>
              </div>
              <div style="height:240px" id="dailyFocusChart"></div>
            </el-card>
          </div>
          <div>
            <!-- 替换为贡献日历组件（GitHub 风格） -->
            <ContributionHeatmap :records="timeRecordStore.records" />
          </div>
          <div>
            <el-card class="chart-card">
              <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 12px">
                <div style="font-weight:600">任务分类占比</div>
                <div style="font-size:12px;color:#888">Top 8 任务</div>
              </div>
              <div style="height:240px" id="topTasksChart"></div>
            </el-card>
          </div>
        </div>
      <div v-if="timeRecordStore.loading" style="padding:20px;text-align:center">加载中...</div>
      <div v-else-if="timeRecordStore.error" style="padding:20px;text-align:center">
            <div style="margin-bottom:8px;color:#f56c6c">加载时间记录失败：{{ timeRecordStore.error }}</div>
            <el-button type="primary" @click="() => loadRecordsForRange()">重试</el-button>
          </div>
          <div v-else-if="!timeRecordStore.records || timeRecordStore.records.length === 0" style="padding:20px;text-align:center">
            <div style="margin-bottom:8px">当前没有时间记录。</div>
            <el-button type="primary" @click="() => loadRecordsForRange()">刷新</el-button>
          </div>
      <el-table v-else :data="timeRecordStore.records" stripe>
        <el-table-column label="任务" width="200">
          <template #default="{ row }">
            {{ row.taskTitle ? safeTaskTitle(row.taskTitle) : getTaskTitle(row.taskId) }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column prop="duration" label="耗时（分钟）" width="120" />
        <el-table-column prop="note" label="备注" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button :loading="deletingId === row.id" type="danger" link size="small" @click="deleteRecord(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onActivated, watch, nextTick } from 'vue'
import ContributionHeatmap from '@/components/ContributionHeatmap.vue'
import { useTaskStore } from '@/store/task'
import { useTimeRecordStore } from '@/store/time-record'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { ElMessageBox } from 'element-plus'
import { safeTaskTitle } from '@/utils/encoding'
import * as echarts from 'echarts'

const taskStore = useTaskStore()
const timeRecordStore = useTimeRecordStore()

const dateRange = ref<any[]>([dayjs().startOf('month').toDate(), dayjs().endOf('month').toDate()])

let chartDailyFocus: any = null
let chartTaskTrend: any = null
let chartTopTasks: any = null

const isRunning = ref(false)
const elapsedTime = ref(0)
const selectedTaskId = ref<string | number | undefined>()
const deletingId = ref<string | number | undefined>()
let timerInterval: ReturnType<typeof setInterval> | null = null

const manualForm = reactive({
  taskId: undefined,
  duration: 0,
  unit: 'min',
  type: 'work',
  note: '',
})

const formattedTime = computed(() => {
  const hours = Math.floor(elapsedTime.value / 3600)
  const minutes = Math.floor((elapsedTime.value % 3600) / 60)
  const seconds = elapsedTime.value % 60

  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

// 估算剩余时间（分钟）
const estimatedRemaining = computed(() => {
  if (selectedTaskId.value == null) return null
  const task = taskStore.tasks.find(t => String(t.id) === String(selectedTaskId.value))
  if (!task || !task.estimatedTime) return null
  const totalMinutes = (task.estimatedTime || 0) * 60
  const used = timeRecordStore.records.filter(r => String(r.taskId) === String(selectedTaskId.value)).reduce((s, r) => s + (r.duration||0), 0)
  return Math.max(0, totalMinutes - used)
})

// 统计：今日与本周
const todayTotal = computed(() => {
  const today = dayjs().format('YYYY-MM-DD')
  return timeRecordStore.records.filter(r => dayjs(r.startTime).format('YYYY-MM-DD') === today).reduce((s, r) => s + (r.duration||0), 0)
})
const thisWeekTotal = computed(() => {
  const start = dayjs().startOf('week')
  const end = dayjs().endOf('week')
  return timeRecordStore.records.filter(r => {
    const d = dayjs(r.startTime)
    return d.isSame(start, 'day') || d.isSame(end, 'day') || (d.isAfter(start, 'day') && d.isBefore(end, 'day'))
  }).reduce((s, r) => s + (r.duration||0), 0)
})
const lastWeekTotal = computed(() => {
  const start = dayjs().subtract(1,'week').startOf('week')
  const end = dayjs().subtract(1,'week').endOf('week')
  return timeRecordStore.records.filter(r => {
    const d = dayjs(r.startTime)
    return d.isSame(start, 'day') || d.isSame(end, 'day') || (d.isAfter(start, 'day') && d.isBefore(end, 'day'))
  }).reduce((s, r) => s + (r.duration||0), 0)
})
const weekRatio = computed(() => {
  const prev = lastWeekTotal.value || 1
  return Math.round(((thisWeekTotal.value - lastWeekTotal.value) / prev) * 100)
})

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

const initCharts = () => {
  const el1 = document.getElementById('dailyFocusChart')
  const el3 = document.getElementById('topTasksChart')
  if (el1) chartDailyFocus = echarts.init(el1)
  if (el3) chartTopTasks = echarts.init(el3)
}

const updateAllCharts = () => {
  const records = (timeRecordStore.records || []).filter((r: any) => r && r.startTime && r.duration && Number(r.duration) > 0)
  const days = getRangeDays(dateRange.value)

  // 每日专注时长
  const daily: number[] = days.map(_ => 0)
  for (const r of records) {
    const day = dayjs(r.startTime).format('YYYY-MM-DD')
    const idx = days.indexOf(day)
    if (idx !== -1) daily[idx] += Number(r.duration || 0)
  }
  if (chartDailyFocus) chartDailyFocus.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: days.map(d => d.slice(5)) },
    yAxis: { type: 'value' },
    series: [{ data: daily, type: 'bar', name: '专注（分钟）', itemStyle: { color: '#67C23A' } }]
  })

  // Top tasks
  const byTask: Record<string, number> = {}
  for (const r of records) {
    const id = String(r.taskId || '未指定')
    byTask[id] = (byTask[id] || 0) + Number(r.duration || 0)
  }
  const top = Object.keys(byTask).map(k => ({ id: k, val: byTask[k], title: getTaskTitle(k) })).sort((a,b) => b.val - a.val).slice(0,8)
  if (chartTopTasks) chartTopTasks.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{ name: '任务占比', type: 'pie', radius: '50%', data: top.map(t => ({ name: t.title, value: Math.round(t.val) })) }]
  })

  // 任务趋势（对 top N 任务按天堆叠线）
  const topIds = top.map(t => t.id)
  const series = topIds.map(id => {
    const data = days.map(d => {
      return records.filter(r => String(r.taskId) === String(id) && dayjs(r.startTime).format('YYYY-MM-DD') === d).reduce((s, r) => s + Number(r.duration || 0), 0)
    })
    return { name: getTaskTitle(id), type: 'line', stack: 'total', data }
  })
  if (chartTaskTrend) chartTaskTrend.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: series.map(s => s.name) },
    xAxis: { type: 'category', data: days.map(d => d.slice(5)) },
    yAxis: { type: 'value' },
    series
  })
}

const loadRecordsForRange = async () => {
  try {
    const start = dayjs(dateRange.value[0]).format('YYYY-MM-DD')
    const end = dayjs(dateRange.value[1]).format('YYYY-MM-DD')
    await timeRecordStore.fetchRecords(start, end)
    // 确保在加载记录后触发任务列表的后台加载（若尚未加载），以便随后更新图表的任务名称映射
    try { taskStore.fetchTasks().catch((e:any)=>{ console.warn('fetchTasks (background) failed', e) }) } catch (e) { /* ignore */ }
    // ensure DOM ready
    await nextTick()
    initCharts()
    updateAllCharts()
  } catch (err: any) {
    console.error('loadRecordsForRange failed', err)
    ElMessage.error('加载时间记录失败')
  }
}

// 处理 el-date-picker 的 change 事件（v-model 会自动同步 dateRange）
const onDateRangeChange = (val: any) => {
  if (Array.isArray(val) && val.length === 2) dateRange.value = val
  else if (val) dateRange.value = val
  // 触发加载，watch 也会触发，这里主动调用以确保及时响应
  loadRecordsForRange().catch(() => {})
}

onMounted(async () => {
  // 尝试并行加载任务与时间记录，避免任务加载失败阻塞记录显示
  // 把 fetchTasks 放在后台执行，保证记录能尽早渲染
  taskStore.fetchTasks().catch((e: any) => {
    console.warn('fetchTasks failed (non-blocking):', e)
  })

  try {
    await loadRecordsForRange()
  } catch (error: any) {
    console.error('TimeRecords load failed', error)
    ElMessage.error(error && error.message ? error.message : '加载时间记录失败，请稍后重试')
  }
})

// 当页面被 <keep-alive> 缓存后再次激活时，onMounted 不会触发，使用 onActivated 重新加载数据
onActivated(() => {
  // 忽略错误，UI 会展示重试按钮
  loadRecordsForRange().catch(() => {})
})

watch(dateRange, () => {
  loadRecordsForRange()
})

// also update charts when records change
watch(() => timeRecordStore.records, () => {
  nextTick(() => updateAllCharts())
}, { deep: true })

// 当任务列表变化时（例如刚刚从后端加载完成），重新渲染图表以补全任务名映射
watch(() => taskStore.tasks, () => {
  nextTick(() => updateAllCharts())
}, { deep: true })

const startTimer = () => {
  if (!selectedTaskId.value) {
    ElMessage.error('请先选择任务')
    return
  }
  isRunning.value = true
  timerInterval = setInterval(() => {
    elapsedTime.value++
  }, 1000)
}

const pauseTimer = () => {
  isRunning.value = false
  if (timerInterval) {
    clearInterval(timerInterval)
  }
}

const stopTimer = async () => {
  isRunning.value = false
  if (timerInterval) {
    clearInterval(timerInterval)
  }

  if (elapsedTime.value === 0 || !selectedTaskId.value) {
    return
  }

  try {
    const now = new Date()
    const startTime = new Date(now.getTime() - elapsedTime.value * 1000)

    await timeRecordStore.createRecord({
      taskId: selectedTaskId.value,
      startTime: startTime.toISOString(),
      endTime: now.toISOString(),
      duration: Math.round(elapsedTime.value / 60),
      note: '手动记录',
    })

    ElMessage.success('时间记录保存成功')
    elapsedTime.value = 0
    selectedTaskId.value = undefined
    await loadRecordsForRange()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const saveManualRecord = async () => {
  if (!manualForm.taskId || manualForm.duration <= 0) {
    ElMessage.error('请填写完整信息')
    return
  }

  try {
    const now = new Date()
    let durationMinutes = manualForm.duration
    if (manualForm.unit === 'sec') durationMinutes = Math.round(manualForm.duration / 60)
    const startTime = new Date(now.getTime() - durationMinutes * 60 * 1000)

    await timeRecordStore.createRecord({
      taskId: manualForm.taskId,
      startTime: startTime.toISOString(),
      endTime: now.toISOString(),
      duration: durationMinutes,
      note: manualForm.note,
    })

    ElMessage.success('记录保存成功')
    manualForm.taskId = undefined
    manualForm.duration = 0
    manualForm.note = ''
    await loadRecordsForRange()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 切换 selectedTask 时如正在计时需确认是否保存当前计时
watch(selectedTaskId, async (newVal, oldVal) => {
  if (isRunning.value && oldVal && newVal !== oldVal) {
    try {
      const res = await ElMessageBox.confirm('当前计时正在进行，切换任务将提示是否保存当前计时，是否保存并继续？', '切换任务', { confirmButtonText: '保存并切换', cancelButtonText: '放弃并切换', type: 'warning' })
      // 保存当前
      await stopTimer()
      // 继续并选择新任务
    } catch (e) {
      // 放弃保存，仅停止并切换
      await stopTimer()
    }
  }
})

const exportCsv = () => {
  const rows = [['任务ID','开始时间','结束时间','耗时(分钟)','备注']]
  for (const r of timeRecordStore.records) rows.push([String(r.taskTitle ? safeTaskTitle(r.taskTitle) : getTaskTitle(r.taskId)), r.startTime, r.endTime, String(r.duration), (r.note||'')])
  const csv = rows.map(r => r.map(c => '"'+String(c).replace(/"/g,'""')+'"').join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `time-records-${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

const getTaskTitle = (taskId?: string | number) => {
  if (taskId == null) return '-'
  const t = taskStore.tasks.find(t => String(t.id) === String(taskId))
  const title = t && (t as any).title ? String((t as any).title).trim() : ''
  if (title) return safeTaskTitle(title)
  return `任务${taskId}`
}

const deleteRecord = async (id: string | number | undefined) => {
  if (id == null) return
  try {
    deletingId.value = id
    await timeRecordStore.deleteRecord(id)
    // 确保从后端重新拉取以校验删除是否真正生效（使用当前 dateRange）
    try { await loadRecordsForRange() } catch (e) { /* ignore */ }
    ElMessage.success('记录删除成功')
  } catch (error: any) {
    ElMessage.error((error && error.message) ? error.message : '删除失败')
    try { await loadRecordsForRange() } catch (e) { /* ignore */ }
  } finally {
    deletingId.value = undefined
  }
}
</script>

<style scoped>
.time-records-container {
  max-width: 1000px;
  margin: 0 auto;
}

.timer-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 30px;
  text-align: center;
}

.timer-display {
  margin-bottom: 30px;
}

.timer-value {
  font-size: 64px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 20px;
  font-family: 'Courier New', monospace;
}

.timer-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.timer-options {
  margin-bottom: 20px;
}

.manual-record {
  text-align: left;
  margin-top: 20px;
}

.manual-record h3 {
  font-size: 16px;
  margin-bottom: 15px;
}

.records-section {
  margin-top: 30px;
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
}

.records-section h2 {
  font-size: 18px;
  margin-bottom: 20px;
}
</style>
