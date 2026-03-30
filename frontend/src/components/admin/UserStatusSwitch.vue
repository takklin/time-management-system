<template>
  <el-switch
    v-model="checked"
    active-text="启用"
    inactive-text="禁用"
    @change="onChange"
  />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
const props = defineProps<{ value: boolean }>()
const emit = defineEmits<{ (event: 'change', enabled: boolean): void }>()
const checked = ref(props.value)

watch(() => props.value, v => { checked.value = v })

const onChange = (val: boolean) => {
  emit('change', val)
}
</script>
