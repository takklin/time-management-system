<template>
  <div class="schedules-container">
    <div class="schedules-header">
      <h1>日程安排</h1>
      <div class="header-actions">
        <el-button :type="viewMode === 'month' ? 'primary' : 'default'" @click="viewMode = 'month'">月视图</el-button>
        <el-button type="primary" @click="showCreateDialog">+ 新增日程</el-button>
        <el-button @click="exportICal">导出 iCal</el-button>
      </div>
    </div>

    <div class="schedules-content">
      <div class="calendar-sidebar">
          <el-calendar v-model="selectedDate" @select="onDateSelect">
            <template #date-cell="{ data }">
                <div class="date-cell-custom" @click.stop="onDateSelect(new Date(data.date))">
                  <span v-if="ensureMonthLoaded(data.date)" style="display:none"></span>
                    <div class="solar-day">{{ dayjs(data.date).date() }}</div>
                    <div class="date-events">
                      <template v-for="sch in eventsForDate(data.date).slice(0, maxEventLines)" :key="sch.id">
                        <span class="event-dot" :style="dotStyle(sch)"></span>
                      </template>
                      <span v-if="eventsForDate(data.date).length > maxEventLines" class="event-more">+{{ eventsForDate(data.date).length - maxEventLines }}</span>
                    </div>
                    <div class="lunar-day">
                      <span class="holiday" v-if="holidayMap[dayjs(data.date).format('YYYY-MM-DD')] && holidayMap[dayjs(data.date).format('YYYY-MM-DD')].holidayName">
                        {{ holidayMap[dayjs(data.date).format('YYYY-MM-DD')].holidayName }}
                      </span>
                      <span v-else>
                        <span v-if="getSolarTermForDate(new Date(data.date))">
                          <span class="solar-term">{{ getSolarTermForDate(new Date(data.date)) }}</span>
                          <span v-if="getLocalLunarText(new Date(data.date))"> · {{ getLocalLunarText(new Date(data.date)) }}</span>
                        </span>
                        <span v-else>
                          {{ getDisplayLunarText(new Date(data.date)) }}
                        </span>
                      </span>
                    </div>
                  </div>
              </template>
          </el-calendar>
        </div>

      <div class="schedules-main">
        <div class="main-header">
          <h3>{{ formatSelectedDate }}</h3>
          <div class="quick-create">
            <el-input v-model="quickTitle" placeholder="快速创建日程：输入标题后回车" @keyup.enter="quickCreate" />
          </div>
        </div>

        <div v-if="viewMode === 'month'">
          <el-empty v-if="selectedDateSchedules.length === 0" description="该日期暂无日程" />
          <div v-for="schedule in selectedDateSchedules" :key="schedule.id" class="schedule-item" :class="{ upcoming: isUpcoming(schedule) }">
            <div class="schedule-time">{{ formatFullRange(schedule) }}</div>
            <div class="schedule-title" :class="{ done: linkedTaskDone(schedule) }">
              <span v-if="linkedTaskDone(schedule)" class="schedule-done-icon">✔</span>
              {{ schedule.title }}
            </div>
            <div class="schedule-actions">
              <el-button type="primary" link size="small" @click="editSchedule(schedule)">编辑</el-button>
              <el-button type="warning" link size="small" @click="linkTaskToSchedule(schedule)">关联任务</el-button>
              <el-button type="danger" link size="small" @click="deleteSchedule(schedule.id)">删除</el-button>
            </div>
          </div>
        </div>

        <!-- 周视图已移除（仅保留月视图） -->
      </div>
    </div>

    <!-- 编辑/创建对话框 -->
    <el-dialog v-model="showDialog" :title="editingScheduleId ? '编辑日程' : '新增日程'" width="600px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="formData.title" placeholder="输入日程标题" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="formData.startTime" type="datetime" placeholder="选择开始时间" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="formData.endTime" type="datetime" placeholder="选择结束时间" />
        </el-form-item>
        
        <el-form-item label="提醒时间">
          <el-input-number v-model="formData.reminderTime" :min="0" placeholder="提醒提前时间（分钟）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="4" placeholder="输入日程描述" />
        </el-form-item>
        <el-form-item label="关联任务">
          <el-select v-model="formData.taskId" placeholder="选择任务（可选）" clearable filterable>
            <el-option v-for="t in taskStore.tasks" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSchedule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, onActivated } from 'vue'
import { useScheduleStore } from '@/store/schedule'
import { useTaskStore } from '@/store/task'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getHolidayForDate, getHolidaysForMonth } from '@/api/holiday'
import { getSchedule } from '@/api/schedules'
import { solar2lunar, getSolarTermName } from '@/utils/lunar'

const scheduleStore = useScheduleStore()
const taskStore = useTaskStore()
const userStore = useUserStore()

const selectedDate = ref(new Date())
const viewMode = ref<'month'|'week'>('month')
const quickTitle = ref('')

// 节假日 / 农历数据缓存，键名 YYYY-MM-DD
const holidayMap = reactive<Record<string, { date: string; isHoliday?: boolean; holidayName?: string | null; lunarText?: string | null }>>({})
const loadedMonths = new Set<string>()

function ensureMonthLoaded(dateValue: Date | string) {
  try {
    const key = dayjs(dateValue).format('YYYY-MM')
    console.debug('[Schedules] ensureMonthLoaded', key)
    if (!loadedMonths.has(key)) {
      console.debug('[Schedules] loading month', key)
      loadedMonths.add(key)
      loadHolidaysForMonth(new Date(dateValue))
      // 异步加载该月份的日程（不阻塞渲染），避免切换月份后日程为空
      try {
        const firstDay = dayjs(dateValue).startOf('month').format('YYYY-MM-DD')
        const lastDay = dayjs(dateValue).endOf('month').format('YYYY-MM-DD')
        scheduleStore.fetchSchedules(firstDay, lastDay).catch((err:any) => console.warn('fetchSchedules month failed', key, err))
      } catch (err) { console.warn('ensureMonthLoaded fetchSchedules failed', err) }
    }
  } catch (e) { /* ignore */ }
  return true
}


async function loadHolidaysForMonth(dateObj: Date) {
  const year = dateObj.getFullYear()
  const month = dateObj.getMonth() + 1
  try {
    const monthMap = await getHolidaysForMonth(year, month)
    Object.keys(monthMap).forEach(k => { holidayMap[k] = monthMap[k] })
    console.debug('[Schedules] loadHolidaysForMonth populated', year, month, Object.keys(monthMap).length)
  } catch (e) {
    // 若批量请求失败，回退为按日请求（但这一般不会执行，因为 getHolidaysForMonth 内部已处理限流）
    const days = new Date(year, month, 0).getDate()
    for (let day = 1; day <= days; day++) {
      const dateStr = `${year}-${String(month).padStart(2,'0')}-${String(day).padStart(2,'0')}`
      if (!holidayMap[dateStr]) {
        try {
          const res = await getHolidayForDate(dateStr)
          holidayMap[dateStr] = res
        } catch (err) {
          holidayMap[dateStr] = { date: dateStr }
        }
      }
    }
  }
}

function getLocalLunarText(dateObj: Date) {
  try {
    const y = dateObj.getFullYear()
    const m = dateObj.getMonth() + 1
    const d = dateObj.getDate()
    const lunar = solar2lunar(y, m, d)
    const txt = lunar ? lunar.lunar : ''
    console.debug('[Schedules] local lunar', `${y}-${m}-${d}`, txt)
    return txt
  } catch (e) { return '' }
}

function getCombinedLunarText(dateObj: Date) {
  try {
    const y = dateObj.getFullYear()
    const m = dateObj.getMonth() + 1
    const d = dateObj.getDate()
    const term = getSolarTermName(y, m, d)
    let lunarStr = ''
    try { lunarStr = (solar2lunar(y, m, d) || {}).lunar || '' } catch (e) { lunarStr = '' }
    console.debug('[Schedules] combined', `${y}-${m}-${d}`, { term, lunarStr })
    if (term && lunarStr) return `${term} · ${lunarStr}`
    if (term) return term
    if (lunarStr) return lunarStr
    return ''
  } catch (e) { return '' }
}

function getDisplayLunarText(dateObj: Date) {
  try {
    const dateStr = dayjs(dateObj).format('YYYY-MM-DD')
    const rec = holidayMap[dateStr]
    const y = dateObj.getFullYear()
    const m = dateObj.getMonth() + 1
    const d = dateObj.getDate()
    const term = getSolarTermName(y, m, d)
    if (term) {
      console.debug('[Schedules] solar term override', dateStr, term)
      return term
    }
    if (rec && rec.lunarText) {
      console.debug('[Schedules] using cached lunarText', dateStr, rec.lunarText)
      return rec.lunarText
    }
    const combined = getCombinedLunarText(dateObj)
    console.debug('[Schedules] computed lunarText', dateStr, combined)
    return combined
  } catch (e) { return '' }
}

function getSolarTermForDate(dateObj: Date) {
  try {
    const y = dateObj.getFullYear()
    const m = dateObj.getMonth() + 1
    const d = dateObj.getDate()
    const term = getSolarTermName(y, m, d)
    if (term) console.debug('[Schedules] getSolarTermForDate', dayjs(dateObj).format('YYYY-MM-DD'), term)
    return term
  } catch (e) { return null }
}

watch(selectedDate, (val: Date) => { if (val) loadHolidaysForMonth(val) }, { immediate: true })

const weekDays = computed(() => {
  const base = dayjs(selectedDate.value).startOf('week')
  const arr: dayjs.Dayjs[] = []
  for (let i = 0; i < 7; i++) arr.push(base.add(i, 'day'))
  return arr
})
const showDialog = ref(false)
const editingScheduleId = ref<number | string | null>(null)

const formData = reactive({
  title: '',
  startTime: null as any,
  endTime: null as any,
  reminderTime: 15,
  description: '',
  taskId: null as number | string | null,
})

const formatSelectedDate = computed(() => {
  const date = selectedDate.value
  return dayjs(date).format('YYYY年MM月DD日')
})

// 判断某个日期是否包含在日程区间内（含首尾）
// 使用 YYYY-MM-DD 字符串比较，避免时区解析引起的偏移
const includesDate = (dateValue: Date | string, sch: any) => {
  try {
    const dateStr = dayjs(dateValue).format('YYYY-MM-DD')
    const startStr = dayjs(sch.startTime).format('YYYY-MM-DD')
    const endStr = dayjs(sch.endTime).format('YYYY-MM-DD')
    // 日期字符串按字典顺序可比较（YYYY-MM-DD 格式）
    return dateStr >= startStr && dateStr <= endStr
  } catch (e) { return false }
}

const eventsForDate = (dateValue: any) => {
  try {
    const arr = (scheduleStore.schedules || []).filter(s => includesDate(dateValue, s))
    try { console.debug('[Schedules] eventsForDate', dayjs(dateValue).format('YYYY-MM-DD'), arr.length, arr.map((x:any)=>x.title)) } catch(e) {}
    return arr.sort((a:any,b:any) => dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf())
  } catch (e) { return [] }
}

const selectedDateSchedules = computed(() => {
  const date = selectedDate.value
  return eventsForDate(date)
})

const schedulesOfDay = (d: dayjs.Dayjs) => {
  return eventsForDate(d.toDate())
}

// 复古浅色系（用于日历标记与周视图块）
const retroColors = ['#F6D6C3','#F6E7C3','#F0EAD6','#DDEBE6','#D9EAF2','#E7DFF4','#F3E6D8','#E8F0D9','#F2E7F0']

const colorForSchedule = (id: any) => {
  try {
    const s = String(id || '')
    let h = 0
    for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
    return retroColors[h % retroColors.length]
  } catch (e) { return retroColors[0] }
}

const maxEventLines = 4

const hexToRgb = (hex: string) => {
  try {
    let h = hex.replace('#', '')
    if (h.length === 3) h = h.split('').map(c => c + c).join('')
    const num = parseInt(h, 16)
    return { r: (num >> 16) & 255, g: (num >> 8) & 255, b: num & 255 }
  } catch (e) {
    return { r: 0, g: 0, b: 0 }
  }
}

const getContrastColor = (hex: string) => {
  const { r, g, b } = hexToRgb(hex)
  const luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255
  return luminance > 0.6 ? '#000' : '#fff'
}

const dotStyle = (sch: any) => {
  const bg = linkedTaskDone(sch) ? '#d8d8d8' : colorForSchedule(sch.id)
  const { r, g, b } = hexToRgb(bg)
  return ({
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    background: bg,
    boxShadow: linkedTaskDone(sch) ? 'none' : `0 0 6px rgba(${r},${g},${b},0.24)`,
    marginRight: '4px',
    flexShrink: '0'
  } as any)
}

// 计算某事件在某天的渲染位置与高度（处理跨天情况）
const blockStyle = (sch: any, d: dayjs.Dayjs) => {
  const dayStart = dayjs(d).startOf('day')
  const dayEnd = dayjs(d).endOf('day')
  const st = dayjs(sch.startTime)
  const en = dayjs(sch.endTime)
  const segStart = st.isBefore(dayStart) ? dayStart : st
  const segEnd = en.isAfter(dayEnd) ? dayEnd : en
  const minutesFromMidnight = (segStart.hour() * 60 + segStart.minute())
  const minutesLen = Math.max(15, segEnd.diff(segStart, 'minute'))
  const top = (minutesFromMidnight / (24 * 60)) * 100
  const height = (minutesLen / (24 * 60)) * 100
  const bg = linkedTaskDone(sch) ? '#d8d8d8' : colorForSchedule(sch.id)
  const textColor = linkedTaskDone(sch) ? '#444' : getContrastColor(bg)
  return ({ position: 'absolute', top: top + '%', height: height + '%', left: '6px', right: '6px', background: bg, color: textColor, padding: '6px', borderRadius: '6px', boxSizing: 'border-box', overflow: 'hidden' } as any)
}

const isUpcoming = (sch: any) => {
  const now = dayjs()
  const start = dayjs(sch.startTime)
  const diff = start.diff(now,'minute')
  return diff >= 0 && diff <= 15
}

// 格式化为：MM月DD日 HH:mm - MM月DD日 HH:mm
const formatFullRange = (sch: any) => {
  try {
    const s = dayjs(sch.startTime).format('MM月DD日 HH:mm')
    const e = dayjs(sch.endTime).format('MM月DD日 HH:mm')
    return `${s} - ${e}`
  } catch (e) { return `${formatTime(sch.startTime)} - ${formatTime(sch.endTime)}` }
}

const tasksById = computed(() => {
  const m = new Map<string, any>()
  for (const t of taskStore.tasks || []) {
    if (t && t.id != null) m.set(String(t.id), t)
  }
  return m
})

const linkedTaskDone = (sch: any) => {
  try {
    if (!sch || sch.taskId == null) return false
    const t = tasksById.value.get(String(sch.taskId))
    return !!t && !!t.completed
  } catch (e) { return false }
}

// AI 助手创建日程事件处理器（定义在 setup 同步上下文以便正确注册生命周期钩子）
const aiHandler = (e: any) => {
  try {
    const d = e.detail
    if (!d) return
    editingScheduleId.value = null
    formData.title = d.title || ''
    formData.startTime = d.startTime ? new Date(d.startTime) : null
    formData.endTime = d.endTime ? new Date(d.endTime) : null
    formData.reminderTime = d.reminderTime ?? 15
    formData.description = d.description ?? ''
    formData.taskId = d.taskId ?? null
    showDialog.value = true
  } catch (err) {
    console.warn('ai-create-schedule handler error', err)
  }
}

onMounted(async () => {
  try {
    await scheduleStore.fetchSchedules()
    await taskStore.fetchTasks()
    try {
      console.debug('[Schedules] after fetchSchedules count', (scheduleStore.schedules || []).length)
      console.debug('[Schedules] sample schedules', JSON.parse(JSON.stringify((scheduleStore.schedules || []).slice(0,10))))
    } catch (e) { console.debug('[Schedules] debug stringify failed', e) }
  } catch (error) {
    console.error('Schedules load failed', error)
    ElMessage.error('加载日程数据失败，请稍后重试')
  }

  // 在挂载后注册事件监听；onBeforeUnmount 在同步上下文中注册以避免 Vue 警告
  window.addEventListener('ai-create-schedule', aiHandler as EventListener)
  // 如果有 pending 的 sessionStorage（热启动场景），先处理一次
  try {
    const pending = sessionStorage.getItem('ai_pending_create_schedule')
    if (pending) {
      const d = JSON.parse(pending)
      window.dispatchEvent(new CustomEvent('ai-create-schedule', { detail: d }))
      sessionStorage.removeItem('ai_pending_create_schedule')
    }
  } catch (err) { /* ignore */ }
})

// 当组件从 keep-alive 或路由缓存中被激活时，重新拉取数据
onActivated(async () => {
  try {
    // 清理月份缓存以确保 ensureMonthLoaded 会重新触发加载
    loadedMonths.clear()
    Object.keys(holidayMap).forEach(k => delete (holidayMap as any)[k])
    await scheduleStore.fetchSchedules()
    await taskStore.fetchTasks()
  } catch (error) {
    console.warn('[Schedules] onActivated fetch failed', error)
  }
})

// 监听用户切换（登录/登出），当用户变更时刷新数据并清空本地月份缓存
watch(() => userStore.user?.id, async (newId, oldId) => {
  try {
    if (newId !== oldId) {
      loadedMonths.clear()
      Object.keys(holidayMap).forEach(k => delete (holidayMap as any)[k])
      try { await scheduleStore.fetchSchedules() } catch (e) { console.warn('fetchSchedules on user change failed', e) }
      try { await taskStore.fetchTasks() } catch (e) { console.warn('fetchTasks on user change failed', e) }
    }
  } catch (err) { /* ignore */ }
})

// 在 setup 同步阶段注册卸载钩子，确保与 aiHandler 配对移除监听器
onBeforeUnmount(() => { window.removeEventListener('ai-create-schedule', aiHandler as EventListener) })

const formatTime = (time: string) => {
  return dayjs(time).format('HH:mm')
}

const onDateSelect = (date: Date) => {
  selectedDate.value = date
}

const quickCreate = async () => {
  if (!quickTitle.value) return
  const start = dayjs(selectedDate.value).hour(9).minute(0)
  const end = start.add(1,'hour')
  try {
    await scheduleStore.createSchedule({ title: quickTitle.value, startTime: start.toISOString(), endTime: end.toISOString() })
    quickTitle.value = ''
    ElMessage.success('已创建日程')
  } catch (err) { ElMessage.error('创建失败') }
}

const openQuickAt = (d: any) => {
  // 在周视图双击某天快速创建，默认 09:00
  selectedDate.value = d.toDate()
  formData.startTime = dayjs(d).hour(9).minute(0).toDate()
  formData.endTime = dayjs(d).hour(10).minute(0).toDate()
  formData.title = ''
  showDialog.value = true
}

const exportICal = () => {
  const lines: string[] = ['BEGIN:VCALENDAR','VERSION:2.0','PRODID:-//TimeManager//EN']
  scheduleStore.schedules.forEach(s => {
    const uid = `sch-${s.id}@time-manager`
    const dtStart = dayjs(s.startTime).format('YYYYMMDDTHHmm00')
    const dtEnd = dayjs(s.endTime).format('YYYYMMDDTHHmm00')
    lines.push('BEGIN:VEVENT')
    lines.push(`UID:${uid}`)
    lines.push(`DTSTAMP:${dayjs().format('YYYYMMDDTHHmm00')}`)
    lines.push(`DTSTART:${dtStart}`)
    lines.push(`DTEND:${dtEnd}`)
    lines.push(`SUMMARY:${s.title}`)
    if (s.description) lines.push(`DESCRIPTION:${s.description.replace(/\n/g,'\\n')}`)
    lines.push('END:VEVENT')
  })
  lines.push('END:VCALENDAR')
  const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'schedules.ics'
  a.click()
  URL.revokeObjectURL(url)
}

const showCreateDialog = () => {
  editingScheduleId.value = null
  formData.title = ''
  formData.startTime = null
  formData.endTime = null
  formData.reminderTime = 15
  formData.description = ''
  formData.taskId = null
  showDialog.value = true
}

const saveSchedule = async () => {
  if (!formData.title) {
    ElMessage.error('请输入日程标题')
    return
  }

  // 校验开始/结束时间
  if (!formData.startTime || !formData.endTime) {
    ElMessage.error('请选择开始和结束时间')
    return
  }
  const st = dayjs(formData.startTime)
  const et = dayjs(formData.endTime)
  if (!st.isValid() || !et.isValid() || st.isAfter(et)) {
    ElMessage.error('开始时间必须早于结束时间')
    return
  }

  try {
    const payload: any = {
      title: formData.title,
      startTime: dayjs(formData.startTime).toISOString(),
      endTime: dayjs(formData.endTime).toISOString(),
      reminderTime: formData.reminderTime,
      description: formData.description,
    }
    if (formData.taskId != null) payload.taskId = formData.taskId
    if (editingScheduleId.value) {
      await scheduleStore.updateSchedule(editingScheduleId.value, payload)
      ElMessage.success('日程更新成功')
    } else {
      await scheduleStore.createSchedule(payload)
      ElMessage.success('日程创建成功')
    }
    showDialog.value = false
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteSchedule = async (id: number | string | undefined) => {
  if (id == null) return
  try {
    await scheduleStore.deleteSchedule(id)
    ElMessage.success('日程删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const editSchedule = async (schedule: any) => {
  // 如果没有有效的远程 id（例如本地临时记录或未同步的项），不要调用后端，直接使用本地数据
  const rawId = schedule && (schedule.id ?? schedule._id)
  const idVal = rawId != null ? rawId : null
  if (idVal == null) {
    editingScheduleId.value = null
    var detail = schedule
  } else {
    // 保持原始 id 类型（string 或 number），避免对大整数进行 Number() 转换导致精度丢失
    editingScheduleId.value = idVal
    var detail = schedule
    try {
      if (!schedule.startTime || !schedule.endTime || schedule.description == null) {
        const res = await getSchedule(idVal as any, { silent: true })
        detail = res?.data || res
      }
    } catch (e) {
      console.warn('getSchedule failed, using provided schedule', e)
      detail = schedule
    }
  }
  formData.title = detail.title || ''
  formData.startTime = detail.startTime ? new Date(detail.startTime) : null
  formData.endTime = detail.endTime ? new Date(detail.endTime) : null
  formData.reminderTime = detail.reminderTime ?? 15
  formData.description = detail.description ?? ''
  formData.taskId = detail.taskId ?? null
  showDialog.value = true
}

const linkTaskToSchedule = (schedule: any) => {
  // 打开编辑弹窗，用户可在其中选择或更改关联任务
  editSchedule(schedule)
}
</script>

<style scoped>
.schedules-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.schedules-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.schedules-header h1 {
  font-size: 24px;
  margin: 0;
}

.schedules-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  flex: 1;
}

.calendar-sidebar {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  height: fit-content;
  position: sticky;
  top: 0;
}

.date-cell-custom { display:flex; flex-direction:column; align-items:flex-start }
.date-cell-custom .solar-day { font-size:14px; font-weight:600 }
.date-cell-custom .lunar-day { font-size:11px; color:#999; margin-top:4px }
.date-cell-custom .holiday { color:#f4955f; font-size:11px; font-weight:700 }

.date-events { display:flex; flex-direction:row; align-items:center; gap:4px; margin-top:6px; flex-wrap:wrap }
.event-dot { display:inline-block; width:8px; height:8px; border-radius:50%; flex-shrink:0 }
.event-more { font-size:11px; color:#666; background:#f5f7fa; padding:2px 6px; border-radius:8px; margin-left:auto }

.date-cell-custom .solar-term { color:#7fcf7f; font-size:11px; font-weight:600 }

.schedules-main {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
}

.main-header { display:flex; justify-content:space-between; align-items:center; gap:12px }
.quick-create { width:320px }
.schedule-item.upcoming { border-color: #f56c6c }
.schedule-title.done { text-decoration: line-through; opacity:0.6 }

.schedule-done-icon { color: #67c23a; font-weight: 700; margin-right: 6px; font-size: 12px }
.block-title.done { text-decoration: line-through; opacity: 0.6 }

.week-view .week-grid { display:flex; gap:8px }
.week-day { flex:1; border:1px dashed #eee; border-radius:6px; padding:6px; position:relative; min-height:520px; background:#fafafa }
.day-title { text-align:center; font-weight:600; margin-bottom:6px }
.day-body { position:relative; height:100%; }
.block.upcoming { animation: blink 1s infinite }
.block .block-title { white-space:nowrap; text-overflow:ellipsis; overflow:hidden }
@keyframes blink { 0% { box-shadow:0 0 0 0 rgba(245,108,108,0.6) } 50% { box-shadow:0 0 12px 6px rgba(245,108,108,0.12) } 100% { box-shadow:0 0 0 0 rgba(245,108,108,0) } }

.schedules-main h3 {
  font-size: 18px;
  margin-bottom: 20px;
}

.schedule-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 10px;
}

.schedule-time {
  min-width: 120px;
  color: #409eff;
  font-weight: 600;
}

.schedule-title {
  flex: 1;
}

.schedule-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .schedules-content {
    grid-template-columns: 1fr;
  }

  .calendar-sidebar {
    position: static;
  }
}
</style>
