<template>
  <div class="global-timer" v-if="show">
    <div class="timer-circle" @click="toggle">
      <div class="time-text">{{ display }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { usePomodoroStore } from '@/store/pomodoro'
const store = usePomodoroStore()
const show = ref(true)

const display = computed(() => {
  if (!store.isRunning) return '⏱'
  const s = store.remainingSeconds || 0
  const m = Math.floor(s/60)
  const sec = s % 60
  return `${String(m).padStart(2,'0')}:${String(sec).padStart(2,'0')}`
})

const toggle = () => {
  if (store.isRunning) store.stop(true)
  else if (store.activeTaskId) store.start(store.activeTaskId)
}
</script>

<style scoped>
.global-timer { position: fixed; right: 22px; bottom: 90px; z-index: 1200 }
.timer-circle { width: 64px; height: 64px; border-radius: 50%; background: linear-gradient(135deg, #C39BD4 0%, #9BB7D4 45%, #D6B77A 90%); display:flex; align-items:center; justify-content:center; box-shadow:0 8px 24px rgba(196,155,212,0.18); cursor:pointer }
.time-text { color: white; font-weight:700 }
</style>
