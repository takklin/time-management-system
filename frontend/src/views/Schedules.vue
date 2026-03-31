<template>
  <div class="schedules-container">
    <div class="schedules-header">
      <h1>日程安排</h1>
      <el-button type="primary" @click="showCreateDialog">+ 新增日程</el-button>
    </div>

    <div class="schedules-content">
      <div class="calendar-sidebar">
        <el-calendar v-model="selectedDate" @select="onDateSelect" />
      </div>

      <div class="schedules-main">
        <h3>{{ formatSelectedDate }}</h3>
        <el-empty v-if="selectedDateSchedules.length === 0" description="该日期暂无日程" />
        <div v-for="schedule in selectedDateSchedules" :key="schedule.id" class="schedule-item">
          <div class="schedule-time">{{ formatTime(schedule.startTime) }} - {{ formatTime(schedule.endTime) }}</div>
          <div class="schedule-title">{{ schedule.title }}</div>
          <div class="schedule-actions">
            <el-button type="primary" link size="small" @click="editSchedule(schedule)">编辑</el-button>
            <el-button type="danger" link size="small" @click="deleteSchedule(schedule.id)">删除</el-button>
          </div>
        </div>
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
        <el-form-item label="关联任务">
          <el-select v-model="formData.taskId" placeholder="选择任务（可选）">
            <el-option v-for="task in taskStore.tasks" :key="task.id" :label="task.title" :value="task.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="提醒时间">
          <el-input-number v-model="formData.reminderTime" :min="0" placeholder="提醒提前时间（分钟）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="4" placeholder="输入日程描述" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useScheduleStore } from '@/store/schedule'
import { useTaskStore } from '@/store/task'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const scheduleStore = useScheduleStore()
const taskStore = useTaskStore()

const selectedDate = ref(new Date())
const showDialog = ref(false)
const editingScheduleId = ref<number | null>(null)

const formData = reactive({
  title: '',
  startTime: null as any,
  endTime: null as any,
  taskId: undefined as any,
  reminderTime: 15,
  description: '',
})

const formatSelectedDate = computed(() => {
  const date = selectedDate.value
  return dayjs(date).format('YYYY年MM月DD日')
})

const selectedDateSchedules = computed(() => {
  const dateStr = dayjs(selectedDate.value).format('YYYY-MM-DD')
  return scheduleStore.schedules.filter(s => {
    const scheduleDate = dayjs(s.startTime).format('YYYY-MM-DD')
    return scheduleDate === dateStr
  })
})

onMounted(async () => {
  try {
    await scheduleStore.fetchSchedules()
    await taskStore.fetchTasks()
  } catch (error) {
    console.error('Schedules load failed', error)
    ElMessage.error('加载日程数据失败，请稍后重试')
  }
})

const formatTime = (time: string) => {
  return dayjs(time).format('HH:mm')
}

const onDateSelect = (date: Date) => {
  selectedDate.value = date
}

const showCreateDialog = () => {
  editingScheduleId.value = null
  formData.title = ''
  formData.startTime = null
  formData.endTime = null
  formData.taskId = undefined
  formData.reminderTime = 15
  formData.description = ''
  showDialog.value = true
}

const saveSchedule = async () => {
  if (!formData.title) {
    ElMessage.error('请输入日程标题')
    return
  }

  try {
    if (editingScheduleId.value) {
      await scheduleStore.updateSchedule(editingScheduleId.value, formData as any)
      ElMessage.success('日程更新成功')
    } else {
      await scheduleStore.createSchedule(formData as any)
      ElMessage.success('日程创建成功')
    }
    showDialog.value = false
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteSchedule = async (id: number | undefined) => {
  if (!id) return
  try {
    await scheduleStore.deleteSchedule(id)
    ElMessage.success('日程删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const editSchedule = (schedule: any) => {
  editingScheduleId.value = schedule.id
  formData.title = schedule.title
  formData.startTime = schedule.startTime
  formData.endTime = schedule.endTime
  formData.taskId = schedule.taskId
  formData.reminderTime = schedule.reminderTime
  formData.description = schedule.description
  showDialog.value = true
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
  grid-template-columns: 300px 1fr;
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

.schedules-main {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
}

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
