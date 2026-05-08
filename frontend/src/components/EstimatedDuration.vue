<template>
  <div class="estimated-duration">
    <el-input-number
      v-model="hoursLocal"
      :min="0"
      size="small"
      controls-position="right"
      @change="onHoursChange"
      :step="1"
    />

    <div class="colon">:</div>

    <el-input-number
      v-model="minutesLocal"
      :min="0"
      :max="59"
      size="small"
      controls-position="right"
      @change="onMinutesChange"
      @wheel.prevent="onMinutesWheel"
      :step="1"
    />

    <div class="label">（小时:分钟）</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{ modelValue?: number }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: number): void }>()

const hoursLocal = ref<number>(Math.floor(props.modelValue ?? 0))
const minutesLocal = ref<number>(Math.round(((props.modelValue ?? 0) - hoursLocal.value) * 60))

watch(() => props.modelValue, (v) => {
  const val = Number(v) || 0
  hoursLocal.value = Math.floor(val)
  minutesLocal.value = Math.round((val - hoursLocal.value) * 60)
})

watch([hoursLocal, minutesLocal], () => {
  // normalize minutes into hours when overflow/underflow
  let h = Math.floor(hoursLocal.value || 0)
  let m = Math.round(minutesLocal.value || 0)
  if (m >= 60) {
    const carry = Math.floor(m / 60)
    h += carry
    m = m % 60
  } else if (m < 0) {
    const borrow = Math.ceil(Math.abs(m) / 60)
    h = Math.max(0, h - borrow)
    m = ((m % 60) + 60) % 60
  }
  // reflect normalized values back
  if (h !== hoursLocal.value) hoursLocal.value = h
  if (m !== minutesLocal.value) minutesLocal.value = m

  const newVal = Math.round((hoursLocal.value + minutesLocal.value / 60) * 100) / 100
  emit('update:modelValue', newVal)
})

const onHoursChange = (v: number) => {
  if (v < 0) hoursLocal.value = 0
}

const onMinutesChange = (_v: number) => {
  // normalization handled by watcher
}

const onMinutesWheel = (e: WheelEvent) => {
  // scroll up => increase, scroll down => decrease
  const delta = e.deltaY < 0 ? 1 : -1
  minutesLocal.value = (minutesLocal.value || 0) + delta
}
</script>

<style scoped>
.estimated-duration { display:flex; align-items:center; gap:8px }
.colon { font-size:14px; color:#666 }
.label { font-size:12px; color:#888 }
</style>
