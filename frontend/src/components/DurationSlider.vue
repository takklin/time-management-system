<template>
  <div class="duration-slider">
    <div class="duration-display" :class="{ invalid: invalid }" @wheel.prevent="onWheel">
      <!-- 使用原生 range 控件，避免 el-slider 内部的 el-slider__runway 类 -->
      <input
        ref="rangeRef"
        class="native-range"
        type="range"
        v-model.number="minutesLocal"
        :min="min"
        :max="max"
        :step="step"
        @input="onChange"
      />

      <span class="formatted">{{ formattedDuration }}</span>
      <div v-if="placeholder && minutesLocal === 0" class="placeholder">{{ placeholder }}</div>
      <el-input-number
        v-model.number="minutesLocal"
        :min="min"
        :max="max"
        :step="step"
        size="small"
        controls-position="right"
        style="width:110px; margin-left:12px"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, toRef, ref } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  min: { type: Number, default: 0 },
  max: { type: Number, default: 8 * 60 },
  step: { type: Number, default: 5 },
  invalid: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
})
const emit = defineEmits<{ (e: 'update:modelValue', value: number): void }>()

const min = toRef(props, 'min')
const max = toRef(props, 'max')
const step = toRef(props, 'step')
const invalid = toRef(props, 'invalid')
const placeholder = toRef(props, 'placeholder')

const minutesLocal = computed<number>({
  get: () => Math.round(props.modelValue || 0),
  set: (v: number) => {
    const clamped = Math.max(min.value, Math.min(max.value, Math.round(v || 0)))
    emit('update:modelValue', clamped)
  },
})

const formattedDuration = computed(() => {
  const mins = minutesLocal.value || 0
  if (mins === 0) return '0分钟'
  const h = Math.floor(mins / 60)
  const r = mins % 60
  if (h === 0) return `${mins}分钟`
  if (r === 0) return `${h}小时`
  return `${h}小时${r}分钟`
})

function onChange(_e: Event) {
  // input event来自原生 range 或 el-input-number，computed setter 已处理 emit
}

function onWheel(e: WheelEvent) {
  // 鼠标滚轮在 duration-display 上改变分钟，向上增加，向下减少
  const delta = e.deltaY < 0 ? step.value : -step.value
  minutesLocal.value = Math.max(min.value, Math.min(max.value, minutesLocal.value + delta))
}

// expose focus helper
const rangeRef = ref<HTMLInputElement | null>(null)
defineExpose({ focusRange: () => { if (rangeRef.value) rangeRef.value.focus() } })
</script>

<style scoped>
.duration-slider { display:flex; align-items:center; gap:12px }
.duration-display { display:flex; align-items:center; gap:8px; white-space:nowrap }
.duration-display.invalid { outline: 2px solid rgba(245,108,108,0.25); border-radius:6px; padding:6px }
.formatted { color:#333; font-size:14px }
.placeholder { font-size:12px; color:#e6a23c; margin-left:8px }
</style>
