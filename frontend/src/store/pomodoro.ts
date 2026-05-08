import { defineStore } from 'pinia'
import { ref } from 'vue'
import dayjs from 'dayjs'
import { useTimeRecordStore } from '@/store/time-record'
import { useTaskStore } from '@/store/task'

const SESSION_KEY = 'pomodoro_session_v1'

export const usePomodoroStore = defineStore('pomodoro', () => {
  const activeTaskId = ref<string | number | null>(null)
  const isRunning = ref(false)
  const durationSeconds = ref(25 * 60)
  const remainingSeconds = ref(0)
  const startedAt = ref<string | null>(null)
  let timer: ReturnType<typeof setInterval> | null = null

  const timeRecordStore = useTimeRecordStore()
  const taskStore = useTaskStore()
  const currentRecordId = ref<string | number | null>(null)
  let isRecordingOp = false
  let recordingPromise: Promise<any> | null = null

  const waitForRecordingFinish = async (timeoutMs = 2000) => {
    if (!recordingPromise) return true
    try {
      await Promise.race([
        recordingPromise.catch(() => {}),
        new Promise((_, rej) => setTimeout(() => rej(new Error('timeout')), timeoutMs)),
      ])
      return true
    } catch (_) {
      return false
    }
  }

  function persistSession() {
    const payload = {
      activeTaskId: activeTaskId.value,
      isRunning: isRunning.value,
      durationSeconds: durationSeconds.value,
      remainingSeconds: remainingSeconds.value,
      startedAt: startedAt.value,
      currentRecordId: currentRecordId.value,
    }
    try { localStorage.setItem(SESSION_KEY, JSON.stringify(payload)) } catch (e) { }
  }

  function clearSessionStorage() {
    try { localStorage.removeItem(SESSION_KEY) } catch (e) { }
  }

  async function restoreSession() {
    try {
      const raw = localStorage.getItem(SESSION_KEY)
      if (!raw) return
      const s = JSON.parse(raw)
      if (!s) return
      activeTaskId.value = s.activeTaskId ?? null
      currentRecordId.value = s.currentRecordId ?? null
      durationSeconds.value = s.durationSeconds ?? durationSeconds.value
      // if startedAt exists and isRunning, compute remaining
      if (s.isRunning && s.startedAt) {
        const elapsed = dayjs().diff(dayjs(s.startedAt), 'second')
        const rem = (s.durationSeconds ?? durationSeconds.value) - elapsed
        if (rem <= 0) {
          // expired while offline
          if (activeTaskId.value) {
            try {
                  if (currentRecordId.value) {
                try {
                  await timeRecordStore.updateRecord(currentRecordId.value as any, {
                    endTime: dayjs().toISOString(),
                    actualMinutes: Math.max(1, Math.round((s.durationSeconds ?? durationSeconds.value) / 60)),
                    status: 'completed',
                    note: '自动记录（番茄钟）',
                  })
                } catch (_) {
                  await timeRecordStore.createRecord({
                    taskId: activeTaskId.value,
                    startTime: dayjs().subtract(s.durationSeconds ?? durationSeconds.value, 'second').toISOString(),
                    endTime: dayjs().toISOString(),
                    duration: Math.round((s.durationSeconds ?? durationSeconds.value) / 60),
                    note: '自动记录（番茄钟）',
                  })
                }
              } else {
                await timeRecordStore.createRecord({
                  taskId: activeTaskId.value,
                  startTime: dayjs().subtract(s.durationSeconds ?? durationSeconds.value, 'second').toISOString(),
                  endTime: dayjs().toISOString(),
                  duration: Math.round((s.durationSeconds ?? durationSeconds.value) / 60),
                  note: '自动记录（番茄钟）',
                })
              }
              try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: activeTaskId.value } })) } catch (_) {}
            } catch (err) {
              console.error('pomodoro record failed (restore)', err)
              try { window.dispatchEvent(new CustomEvent('pomodoro:record-failed', { detail: { error: err } })) } catch (_) {}
            }
          }
          try { window.dispatchEvent(new CustomEvent('pomodoro:completed', { detail: { taskId: activeTaskId.value } })) } catch (_) {}
          clearSessionStorage()
          return
        }
        remainingSeconds.value = rem
        startedAt.value = s.startedAt
        isRunning.value = true
        // restart timer
        if (timer) clearInterval(timer)
        timer = setInterval(() => {
          remainingSeconds.value = Math.max(0, remainingSeconds.value - 1)
          persistSession()
          if (remainingSeconds.value <= 0) completePomodoro()
        }, 1000)
      } else {
        remainingSeconds.value = s.remainingSeconds ?? 0
        isRunning.value = false
        startedAt.value = null
      }
    } catch (e) {
      console.warn('restore pomodoro session failed', e)
    }
  }

  function start(taskId: string | number | null, minutes = 25) {
    if (timer) { clearInterval(timer); timer = null }
    activeTaskId.value = taskId
    durationSeconds.value = minutes * 60
    remainingSeconds.value = durationSeconds.value
    startedAt.value = dayjs().toISOString()
    isRunning.value = true
    persistSession()

    // 不在开始时创建没有 endTime 的记录，避免后端出现未结束的占位记录

    timer = setInterval(() => {
      remainingSeconds.value = Math.max(0, remainingSeconds.value - 1)
      persistSession()
      if (remainingSeconds.value <= 0) completePomodoro()
    }, 1000)
  }

  function pause() {
    if (timer) { clearInterval(timer); timer = null }
    if (!isRunning.value) return
    if (startedAt.value) {
      const elapsed = dayjs().diff(dayjs(startedAt.value), 'second')
      remainingSeconds.value = Math.max(0, durationSeconds.value - elapsed)
    }
    isRunning.value = false
    startedAt.value = null
    persistSession()
  }

  function resume() {
    if (isRunning.value) return
    startedAt.value = dayjs().toISOString()
    isRunning.value = true
    persistSession()
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
      remainingSeconds.value = Math.max(0, remainingSeconds.value - 1)
      persistSession()
      if (remainingSeconds.value <= 0) completePomodoro()
    }, 1000)
  }

  async function stop(save = true) {
    if (timer) { clearInterval(timer); timer = null }
    if (!(activeTaskId.value && save)) {
      isRunning.value = false
      activeTaskId.value = null
      currentRecordId.value = null
      remainingSeconds.value = 0
      startedAt.value = null
      clearSessionStorage()
      return
    }

    const taskIdLocal = activeTaskId.value
    const startedAtLocal = startedAt.value
    const durationSecondsLocal = durationSeconds.value

    if (recordingPromise) {
      try { await waitForRecordingFinish(2000) } catch (_) {}
    }

    if (!activeTaskId.value || activeTaskId.value !== taskIdLocal) {
      // session changed; just cleanup below
    } else {
      recordingPromise = (async () => {
        isRecordingOp = true
        try {
          let usedSeconds = Math.max(0, durationSecondsLocal - remainingSeconds.value)
          if (startedAtLocal) {
            usedSeconds = Math.max(0, durationSecondsLocal - remainingSeconds.value)
          }
          const used = Math.max(0, Math.round(usedSeconds / 60))
          if (used > 0) {
            if (currentRecordId.value) {
              try {
                  await timeRecordStore.updateRecord(currentRecordId.value as any, {
                    endTime: dayjs().toISOString(),
                    actualMinutes: used,
                    status: 'stopped',
                    note: '自动记录（番茄钟）',
                })
                try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: used } })) } catch (_) {}
              } catch (err) {
                // fallback: find by startTime, else create
                try {
                  const day = dayjs(startedAtLocal).format('YYYY-MM-DD')
                  await timeRecordStore.fetchRecords(day, day)
                  const found = timeRecordStore.records.find(r => String(r.taskId) === String(taskIdLocal) && r.startTime && dayjs(r.startTime).isSame(dayjs(startedAtLocal), 'minute'))
                  if (found && (found as any).id) {
                    await timeRecordStore.updateRecord((found as any).id, { endTime: dayjs().toISOString(), actualMinutes: used, status: 'stopped', note: '自动记录（番茄钟）' })
                    currentRecordId.value = (found as any).id
                    persistSession()
                    try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: used } })) } catch (_) {}
                  } else {
                    await timeRecordStore.createRecord({
                      taskId: taskIdLocal,
                      startTime: dayjs().subtract(used, 'minute').toISOString(),
                      endTime: dayjs().toISOString(),
                      duration: used,
                      note: '自动记录（番茄钟）',
                    })
                    try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: used } })) } catch (_) {}
                  }
                } catch (err2) {
                  await timeRecordStore.createRecord({
                    taskId: taskIdLocal,
                    startTime: dayjs().subtract(used, 'minute').toISOString(),
                    endTime: dayjs().toISOString(),
                    duration: used,
                    note: '自动记录（番茄钟）',
                  })
                  try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: used } })) } catch (_) {}
                }
              }
            } else {
              try {
                const day = dayjs(startedAtLocal).format('YYYY-MM-DD')
                await timeRecordStore.fetchRecords(day, day)
                const found = timeRecordStore.records.find(r => String(r.taskId) === String(taskIdLocal) && r.startTime && dayjs(r.startTime).isSame(dayjs(startedAtLocal), 'minute'))
                if (found && (found as any).id) {
                  await timeRecordStore.updateRecord((found as any).id, { endTime: dayjs().toISOString(), actualMinutes: used, status: 'stopped', note: '自动记录（番茄钟）' })
                  currentRecordId.value = (found as any).id
                  persistSession()
                  try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: used } })) } catch (_) {}
                } else {
                  await timeRecordStore.createRecord({
                    taskId: taskIdLocal,
                    startTime: dayjs().subtract(used, 'minute').toISOString(),
                    endTime: dayjs().toISOString(),
                    duration: used,
                    note: '自动记录（番茄钟）',
                  })
                  try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: used } })) } catch (_) {}
                }
              } catch (err) {
                await timeRecordStore.createRecord({
                  taskId: taskIdLocal,
                  startTime: dayjs().subtract(used, 'minute').toISOString(),
                  endTime: dayjs().toISOString(),
                  duration: used,
                  note: '自动记录（番茄钟）',
                })
                try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: used } })) } catch (_) {}
              }
            }
          }
        } catch (e) {
          console.error('pomodoro stop record failed', e)
          try { window.dispatchEvent(new CustomEvent('pomodoro:record-failed', { detail: { error: e } })) } catch (_) {}
        } finally {
          isRecordingOp = false
        }
      })()
      try { await recordingPromise } catch (_) { /* ignore */ }
      recordingPromise = null
    }

    // cleanup session state
    isRunning.value = false
    activeTaskId.value = null
    currentRecordId.value = null
    remainingSeconds.value = 0
    startedAt.value = null
    clearSessionStorage()
  }

  async function completePomodoro() {
    if (!activeTaskId.value) return
    const taskIdLocal = activeTaskId.value
    const startedAtLocal = startedAt.value
    const durationSecondsLocal = durationSeconds.value

    if (recordingPromise) {
      try { await waitForRecordingFinish(2000) } catch (_) {}
    }
    if (!activeTaskId.value || activeTaskId.value !== taskIdLocal) return

    recordingPromise = (async () => {
      isRecordingOp = true
      try {
        const minutes = Math.max(1, Math.round(durationSecondsLocal / 60))
        if (currentRecordId.value) {
          try {
                await timeRecordStore.updateRecord(currentRecordId.value as any, {
              endTime: dayjs().toISOString(),
              actualMinutes: minutes,
              status: 'completed',
              note: '自动记录（番茄钟）',
            })
            try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: minutes } })) } catch (_) {}
          } catch (err) {
            try {
              const day = dayjs(startedAtLocal).format('YYYY-MM-DD')
              await timeRecordStore.fetchRecords(day, day)
              const found = timeRecordStore.records.find(r => String(r.taskId) === String(taskIdLocal) && r.startTime && dayjs(r.startTime).isSame(dayjs(startedAtLocal), 'minute'))
              if (found && (found as any).id) {
                await timeRecordStore.updateRecord((found as any).id, { endTime: dayjs().toISOString(), actualMinutes: minutes, status: 'completed', note: '自动记录（番茄钟）' })
                currentRecordId.value = (found as any).id
                persistSession()
                try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: minutes } })) } catch (_) {}
              } else {
                await timeRecordStore.createRecord({
                  taskId: taskIdLocal,
                  startTime: dayjs().subtract(durationSecondsLocal / 60, 'minute').toISOString(),
                  endTime: dayjs().toISOString(),
                  duration: minutes,
                  note: '自动记录（番茄钟）',
                })
                try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: minutes } })) } catch (_) {}
              }
            } catch (err2) {
              await timeRecordStore.createRecord({
                taskId: taskIdLocal,
                startTime: dayjs().subtract(durationSecondsLocal / 60, 'minute').toISOString(),
                endTime: dayjs().toISOString(),
                duration: minutes,
                note: '自动记录（番茄钟）',
              })
              try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: minutes } })) } catch (_) {}
            }
          }
        } else {
          try {
            const day = dayjs(startedAtLocal).format('YYYY-MM-DD')
            await timeRecordStore.fetchRecords(day, day)
            const found = timeRecordStore.records.find(r => String(r.taskId) === String(taskIdLocal) && r.startTime && dayjs(r.startTime).isSame(dayjs(startedAtLocal), 'minute'))
            if (found && (found as any).id) {
              await timeRecordStore.updateRecord((found as any).id, { endTime: dayjs().toISOString(), actualMinutes: minutes, status: 'completed', note: '自动记录（番茄钟）' })
              currentRecordId.value = (found as any).id
              persistSession()
              try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: minutes } })) } catch (_) {}
            } else {
              await timeRecordStore.createRecord({
                taskId: taskIdLocal,
                startTime: dayjs().subtract(durationSecondsLocal / 60, 'minute').toISOString(),
                endTime: dayjs().toISOString(),
                duration: minutes,
                note: '自动记录（番茄钟）',
              })
              try { window.dispatchEvent(new CustomEvent('pomodoro:recorded', { detail: { taskId: taskIdLocal, duration: minutes } })) } catch (_) {}
            }
          } catch (err) {
            console.error('pomodoro record failed', err)
            try { window.dispatchEvent(new CustomEvent('pomodoro:record-failed', { detail: { error: err } })) } catch (_) {}
          }
        }
      } catch (e) {
        console.error('pomodoro record failed', e)
        try { window.dispatchEvent(new CustomEvent('pomodoro:record-failed', { detail: { error: e } })) } catch (_) {}
      } finally {
        isRecordingOp = false
      }
    })()
    try { await recordingPromise } catch (_) { /* ignore */ }
    recordingPromise = null

    try { window.dispatchEvent(new CustomEvent('pomodoro:completed', { detail: { taskId: activeTaskId.value } })) } catch (_) {}
    if (timer) { clearInterval(timer); timer = null }
    isRunning.value = false
    activeTaskId.value = null
    currentRecordId.value = null
    remainingSeconds.value = 0
    startedAt.value = null
    clearSessionStorage()
  }

  function getRemaining() {
    return remainingSeconds.value
  }

  // restore on init
  restoreSession()

  // listen to storage changes to sync across tabs
  try {
    window.addEventListener('storage', (e) => {
      if (e.key === SESSION_KEY) {
        restoreSession()
      }
    })
  } catch (e) {}

  return {
    activeTaskId,
    isRunning,
    durationSeconds,
    remainingSeconds,
    startedAt,
    start,
    pause,
    resume,
    stop,
    completePomodoro,
    getRemaining,
  }
})
