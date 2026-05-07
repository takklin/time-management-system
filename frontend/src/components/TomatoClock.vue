<template>
  <div class="tomato-floating">
    <div class="trigger" @click="expanded = !expanded" :title="expanded ? '收起番茄钟' : '打开番茄钟'">
      <div class="trigger-icon">🍅</div>
      <div class="trigger-time" v-if="pomodoro.isRunning">{{ formattedTime }}</div>
    </div>

    <transition name="slide-up">
      <div v-show="expanded" class="panel">
        <div class="panel-header">
          <div class="panel-title">番茄钟</div>
          <el-button icon="Close" link @click="expanded = false"></el-button>
        </div>

        <div class="mode-switch">
          <el-button :type="mode === 'work' ? 'danger' : 'default'" size="small" @click="setMode('work')">专注</el-button>
          <el-button :type="mode === 'break' ? 'success' : 'default'" size="small" @click="setMode('break')">休息</el-button>
        </div>

        <div class="timer-display">{{ formattedTime }}</div>

        <el-select v-model="selectedTaskId" placeholder="关联任务 (可选)" clearable style="width:100%; margin:8px 0">
          <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
        </el-select>

        <div class="controls">
          <el-button v-if="!pomodoro.isRunning && !pomodoro.activeTaskId" type="primary" size="small" @click="onStart">开始</el-button>
          <el-button v-else-if="pomodoro.isRunning" type="warning" size="small" @click="onPause">暂停</el-button>
          <el-button v-else-if="!pomodoro.isRunning && pomodoro.activeTaskId" type="primary" size="small" @click="onResume">继续</el-button>
          <el-button size="small" @click="onComplete" :disabled="!pomodoro.activeTaskId">完成</el-button>
          <el-button size="small" @click="onReset">重置</el-button>
        </div>

        <div class="custom-time" v-if="mode === 'work'">
          <el-input-number v-model="customWorkMinutes" :min="1" :max="240" size="small" />
          <el-button size="small" @click="applyCustom">设置</el-button>
        </div>

        <div class="panel-footer">专注结束会自动创建时间记录（如已关联任务）</div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { usePomodoroStore } from '@/store/pomodoro'
import { useTaskStore } from '@/store/task'
import { ElMessage } from 'element-plus'

const pomodoro = usePomodoroStore()
const taskStore = useTaskStore()
const route = useRoute()

const expanded = ref(false)
const customWorkMinutes = ref(25)
const breakMinutes = ref(5)
const uiMode = ref<'work'|'break'>('work')
const selectedTaskId = ref<number | null>(pomodoro.activeTaskId ?? null)

const tasks = computed(() => {
  // 优先使用来自当前视图的 displayedTasks（只有当 displayedTasksSource === 当前路由 path 时才可信）
  const displayed = (taskStore as any).displayedTasks || []
  const displayedSource = (taskStore as any).displayedTasksSource || null
  const shouldUseDisplayed = displayedSource && String(displayedSource) === String(route.path) && Array.isArray(displayed) && displayed.length > 0
  const source = shouldUseDisplayed ? displayed.slice() : (taskStore.tasks || []).slice()
  const list = (source || []).filter((t: any) => !Boolean((t as any).completed) && !taskStore.isArchived(t.id))
  try { console.debug('[DEBUG] TomatoClock computed tasks -> using=', shouldUseDisplayed ? 'displayedTasks' : 'all', 'displayedSource=', displayedSource, 'count=', list.length, 'sample=', list.slice(0,3).map((x:any)=>({ id: x.id, title: x.title, completed: !!x.completed }))) } catch (e) {}
  return list
})

// 如果已选择的任务被归档或已完成，则清除选择
watch(tasks, (list) => {
  const usedDisplayed = Array.isArray((taskStore as any).displayedTasks) && ((taskStore as any).displayedTasks.length > 0)
  try { console.debug('[DEBUG] TomatoClock tasks length=', Array.isArray(list) ? list.length : 0, 'usedDisplayed=', usedDisplayed, 'displayedCount=', ((taskStore as any).displayedTasks || []).length, 'sample=', (list || []).slice(0,3).map((x: any) => x.title)) } catch (e) { console.debug('[DEBUG] TomatoClock tasks changed') }
  if (selectedTaskId.value != null) {
    const exists = (list || []).some((t: any) => String(t.id) === String(selectedTaskId.value))
    if (!exists) selectedTaskId.value = null
  }
}, { immediate: true })

// keep selectedTaskId in sync
watch(() => pomodoro.activeTaskId, (v) => { selectedTaskId.value = v ?? null })

const mode = computed(() => uiMode.value)

const formattedTime = computed(() => {
  const s = pomodoro.remainingSeconds || pomodoro.durationSeconds || 25 * 60
  const mins = Math.floor(s / 60)
  const secs = s % 60
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
})

const playBeep = () => {
  try {
    const AudioCtx = (window as any).AudioContext || (window as any).webkitAudioContext
    if (!AudioCtx) return
    const ctx = new AudioCtx()
    const o = ctx.createOscillator()
    const g = ctx.createGain()
    o.type = 'sine'
    o.frequency.value = 880
    o.connect(g)
    g.connect(ctx.destination)
    g.gain.setValueAtTime(0.0001, ctx.currentTime)
    g.gain.exponentialRampToValueAtTime(0.1, ctx.currentTime + 0.01)
    o.start()
    g.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 1.0)
    o.stop(ctx.currentTime + 1.0)
  } catch (e) {
    console.warn('playBeep failed', e)
  }
}

const onStart = () => {
  if (uiMode.value === 'work') {
    const minutes = Number(customWorkMinutes.value) || 25
    const taskToUse = (tasks.value || []).find((t: any) => String(t.id) === String(selectedTaskId.value))
    pomodoro.start(taskToUse ? selectedTaskId.value : null, minutes)
  } else {
    pomodoro.start(null, breakMinutes.value)
  }
  ElMessage.success('番茄钟已开始')
}

const onPause = () => {
  // toggle pause/resume
  if (pomodoro.isRunning) {
    pomodoro.pause()
    ElMessage.info('已暂停')
  } else if (pomodoro.activeTaskId) {
    pomodoro.resume()
    ElMessage.success('继续计时')
  }
}

const onReset = () => {
  pomodoro.stop(false)
  ElMessage.info('已重置')
}

const onResume = () => {
  if (!pomodoro.isRunning) {
    pomodoro.resume()
    ElMessage.success('继续计时')
  }
}

const onComplete = async () => {
  try {
    await pomodoro.stop(true)
    ElMessage.success('已完成并记录时间')
  } catch (e) {
    console.error('complete failed', e)
    ElMessage.error('记录时间失败')
  }
}

const applyCustom = () => {
  // apply custom minutes for next start
  ElMessage.success('设置已保存')
}

const setMode = (m: 'work'|'break') => {
  uiMode.value = m
}

onMounted(() => {
  const handleCompleted = (e: any) => {
    playBeep()
    ElMessage.success('番茄钟结束')
  }
  const handleRecordFailed = (e: any) => {
    ElMessage.error('记录时间失败，请检查网络或重试')
  }
  window.addEventListener('pomodoro:completed', handleCompleted)
  window.addEventListener('pomodoro:record-failed', handleRecordFailed)
  onUnmounted(() => {
    try { window.removeEventListener('pomodoro:completed', handleCompleted) } catch (_) {}
    try { window.removeEventListener('pomodoro:record-failed', handleRecordFailed) } catch (_) {}
  })
})
</script>

<style scoped>
.tomato-floating {
  position: fixed;
  right: 22px;
  bottom: 90px;
  z-index: 1200;
}
.trigger {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(180deg,#ff6b4a,#e34d2e);
  display:flex;
  align-items:center;
  justify-content:center;
  flex-direction: column;
  color: #fff;
  box-shadow: 0 8px 26px rgba(0,0,0,0.2);
  cursor: pointer;
}
.trigger:hover { transform: scale(1.04) }
.trigger-icon { font-size: 26px }
.trigger-time { font-size: 11px; font-weight:600 }
.panel {
  width: 300px;
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 14px 40px rgba(0,0,0,0.18);
  margin-bottom: 12px;
}
.panel-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px }
.panel-title { font-weight:700 }
.mode-switch { display:flex; gap:8px; justify-content:center; margin-bottom:8px }
.timer-display { font-size: 2rem; font-weight:700; text-align:center; color:#e34d2e; background:#fff6f3; padding:8px 12px; border-radius:10px }
.controls { display:flex; gap:8px; justify-content:center; margin-top:10px }
.custom-time { display:flex; gap:8px; justify-content:center; align-items:center; margin-top:8px }
.panel-footer { font-size:12px; color:#888; text-align:center; margin-top:8px }
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.18s ease }
.slide-up-enter-from { transform: translateY(8px); opacity: 0 }
.slide-up-enter-to { transform: translateY(0); opacity: 1 }
.slide-up-leave-from { transform: translateY(0); opacity: 1 }
.slide-up-leave-to { transform: translateY(8px); opacity: 0 }
</style>
