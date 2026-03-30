<template>
  <div class="task-item">
    <div class="item-header">
      <el-checkbox v-model="isCompleted" :disabled="loading" @change="handleComplete" />
      <span class="task-title" :class="{ completed: task.completed }">{{ task.title }}</span>
      <el-tag :type="getPriorityType(task.priority)" size="small">{{ task.priority }}</el-tag>
    </div>
    <div class="item-body">
      <div class="task-info">
        <span class="category">{{ task.category }}</span>
        <span v-if="task.deadline" class="deadline">
          <el-icon><Calendar /></el-icon>
          {{ task.deadline }}
        </span>
      </div>
    </div>
    <div class="item-actions">
      <el-button type="primary" link size="small" @click="handleEdit">编辑</el-button>
      <el-button type="danger" link size="small" @click="handleDelete">删除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Calendar } from '@element-plus/icons-vue'

interface Task {
  id?: number
  title: string
  category: string
  priority: 'high' | 'medium' | 'low'
  deadline?: string
  completed: boolean
}

const props = defineProps<{
  task: Task
}>()

const emit = defineEmits<{
  edit: [task: Task]
  delete: [id: number | undefined]
  complete: [id: number | undefined]
}>()

const loading = ref(false)

const isCompleted = computed({
  get: () => props.task.completed,
  set: () => {
    // This will be handled in handleComplete
  },
})

const getPriorityType = (priority: string) => {
  const map: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'success',
  }
  return map[priority] || 'info'
}

const handleEdit = () => {
  emit('edit', props.task)
}

const handleDelete = () => {
  if (props.task.id) {
    emit('delete', props.task.id)
  }
}

const handleComplete = async () => {
  loading.value = true
  try {
    emit('complete', props.task.id)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.task-item {
  background-color: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  transition: all 0.3s;
}

.task-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.task-title {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.task-title.completed {
  color: #ccc;
  text-decoration: line-through;
}

.item-body {
  margin-bottom: 8px;
}

.task-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

.category {
  background-color: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.deadline {
  display: flex;
  align-items: center;
  gap: 4px;
}

.item-actions {
  display: flex;
  gap: 10px;
}
</style>
