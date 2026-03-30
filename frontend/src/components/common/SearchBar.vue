<template>
  <div class="search-bar">
    <el-input
      v-model="innerValue"
      :placeholder="placeholder"
      clearable
      @keyup.enter="onSearch"
      style="width: 220px"
    >
      <template #append>
        <el-button type="primary" @click="onSearch">搜索</el-button>
      </template>
    </el-input>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: String,
  placeholder: { type: String, default: '请输入关键词' }
})
const emit = defineEmits(['update:modelValue', 'search'])

const innerValue = ref(props.modelValue || '')

watch(() => props.modelValue, v => { innerValue.value = v || '' })

const onSearch = () => {
  emit('update:modelValue', innerValue.value)
  emit('search')
}
</script>
