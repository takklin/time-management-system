<template>
  <div class="time-records-container">
    <div class="timer-section">
      <div class="timer-display">
        <div class="timer-value">{{ formattedTime }}</div>
        <div class="timer-actions">
          <el-button
            v-if="!isRunning"
            type="success"
            size="large"
            @click="startTimer"
          >
            开始计时
          </el-button>
          <template v-else>
            <el-button type="warning" size="large" @click="pauseTimer">暂停</el-button>
            <el-button type="danger" size="large" @click="stopTimer">停止</el-button>
          </template>
        </div>
      </div>

      <div class="timer-options">
        <el-select v-model="selectedTaskId" placeholder="选择任务" clearable>
          <el-option v-for="task in taskStore.tasks" :key="task.id" :label="task.title" :value="task.id" />
        </el-select>
      </div>

      <el-divider />

      <div class="manual-record">
        <h3>手动记录</h3>
        <el-form :model="manualForm" label-width="100px">
          <el-form-item label="任务">
            <el-select v-model="manualForm.taskId" placeholder="选择任务">
              <el-option v-for="task in taskStore.tasks" :key="task.id" :label="task.title" :value="task.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="耗时（分钟）">
            <el-input-number v-model="manualForm.duration" :min="1" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="manualForm.notes" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveManualRecord">保存记录</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <el-divider />

    <div class="records-section">
      <h2>时间记录历史</h2>
      <el-table :data="timeRecordStore.records" stripe>
        <el-table-column prop="taskId" label="任务" width="150" />
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column prop="duration" label="耗时（分钟）" width="120" />
        <el-table-column prop="notes" label="备注" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="deleteRecord(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useTaskStore } from '@/store/task'
import { useTimeRecordStore } from '@/store/time-record'
import { ElMessage } from 'element-plus'

const taskStore = useTaskStore()
const timeRecordStore = useTimeRecordStore()

const isRunning = ref(false)
const elapsedTime = ref(0)
const selectedTaskId = ref<number | undefined>()
let timerInterval: ReturnType<typeof setInterval> | null = null

const manualForm = reactive({
  taskId: undefined,
  duration: 0,
  notes: '',
})

const formattedTime = computed(() => {
  const hours = Math.floor(elapsedTime.value / 3600)
  const minutes = Math.floor((elapsedTime.value % 3600) / 60)
  const seconds = elapsedTime.value % 60

  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

onMounted(async () => {
  try {
    await taskStore.fetchTasks()
    await timeRecordStore.fetchRecords()
  } catch (error) {
    console.error('TimeRecords load failed', error)
    ElMessage.error('加载时间记录失败，请稍后重试')
  }
})

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
    })

    ElMessage.success('时间记录保存成功')
    elapsedTime.value = 0
    selectedTaskId.value = undefined
    await timeRecordStore.fetchRecords()
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
    const startTime = new Date(now.getTime() - manualForm.duration * 60 * 1000)

    await timeRecordStore.createRecord({
      taskId: manualForm.taskId,
      startTime: startTime.toISOString(),
      endTime: now.toISOString(),
      duration: manualForm.duration,
      notes: manualForm.notes,
    })

    ElMessage.success('记录保存成功')
    manualForm.taskId = undefined
    manualForm.duration = 0
    manualForm.notes = ''
    await timeRecordStore.fetchRecords()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const deleteRecord = async (id: number | undefined) => {
  if (!id) return
  try {
    await timeRecordStore.deleteRecord(id)
    ElMessage.success('记录删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
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
