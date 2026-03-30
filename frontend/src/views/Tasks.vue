<template>
  <div class="tasks-container">
    <div class="tasks-header">
      <h1>任务管理</h1>
      <el-button type="primary" @click="showCreateDialog">+ 新增任务</el-button>
    </div>

    <div class="tasks-content">
      <div class="filters-sidebar">
        <div class="filter-group">
          <h3>分类</h3>
          <el-checkbox-group v-model="selectedFilters.categories">
            <el-checkbox v-for="cat in categories" :key="cat" :label="cat">{{ cat }}</el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="filter-group">
          <h3>优先级</h3>
          <el-checkbox-group v-model="selectedFilters.priority">
            <el-checkbox label="high">高</el-checkbox>
            <el-checkbox label="medium">中</el-checkbox>
            <el-checkbox label="low">低</el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="filter-group">
          <h3>状态</h3>
          <el-radio-group v-model="selectedFilters.status">
            <el-radio label="all">全部</el-radio>
            <el-radio label="active">未完成</el-radio>
            <el-radio label="completed">已完成</el-radio>
          </el-radio-group>
        </div>
      </div>

      <div class="tasks-main">
        <el-empty v-if="filteredTasks.length === 0" description="暂无任务" />
        <TaskItem
          v-for="task in filteredTasks"
          :key="task.id"
          :task="task"
          @complete="completeTask"
          @delete="deleteTask"
          @edit="editTask"
        />
      </div>
    </div>

    <!-- 编辑/创建对话框 -->
    <el-dialog v-model="showDialog" :title="editingTaskId ? '编辑任务' : '新增任务'" width="600px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="formData.title" placeholder="输入任务标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="formData.category" placeholder="选择分类">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="formData.priority">
            <el-radio label="high">高</el-radio>
            <el-radio label="medium">中</el-radio>
            <el-radio label="low">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="formData.deadline" type="datetime" placeholder="选择日期时间" />
        </el-form-item>
        <el-form-item label="预估时长">
          <el-input-number v-model="formData.estimatedTime" placeholder="小时" :min="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" rows="4" placeholder="输入任务描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useTaskStore } from '@/store/task'
import { ElMessage } from 'element-plus'
import TaskItem from '@/components/TaskItem.vue'

const taskStore = useTaskStore()

const categories = ref(['工作', '学习', '生活', '运动'])
const showDialog = ref(false)
const editingTaskId = ref<number | null>(null)

const selectedFilters = reactive({
  categories: [] as string[],
  priority: [] as string[],
  status: 'all',
})

const formData = reactive({
  title: '',
  category: '',
  priority: 'medium' as const,
  deadline: null as any,
  estimatedTime: 0,
  description: '',
})

const filteredTasks = computed(() => {
  let filtered = taskStore.tasks

  if (selectedFilters.categories.length > 0) {
    filtered = filtered.filter(t => selectedFilters.categories.includes(t.category))
  }

  if (selectedFilters.priority.length > 0) {
    filtered = filtered.filter(t => selectedFilters.priority.includes(t.priority))
  }

  if (selectedFilters.status === 'active') {
    filtered = filtered.filter(t => !t.completed)
  } else if (selectedFilters.status === 'completed') {
    filtered = filtered.filter(t => t.completed)
  }

  return filtered
})

onMounted(async () => {
  await taskStore.fetchTasks()
})

const showCreateDialog = () => {
  editingTaskId.value = null
  formData.title = ''
  formData.category = ''
  formData.priority = 'medium'
  formData.deadline = null
  formData.estimatedTime = 0
  formData.description = ''
  showDialog.value = true
}

const saveTask = async () => {
  if (!formData.title) {
    ElMessage.error('请输入任务标题')
    return
  }

  try {
    if (editingTaskId.value) {
      await taskStore.updateTask(editingTaskId.value, {
        ...formData,
        completed: false,
      })
      ElMessage.success('任务更新成功')
    } else {
      await taskStore.createTask({
        ...formData,
        completed: false,
      })
      ElMessage.success('任务创建成功')
    }
    showDialog.value = false
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const completeTask = (id: number | undefined) => {
  if (id) {
    taskStore.completeTask(id)
  }
}

const deleteTask = (id: number | undefined) => {
  if (id) {
    taskStore.deleteTask(id)
  }
}

const editTask = (task: any) => {
  editingTaskId.value = task.id
  formData.title = task.title
  formData.category = task.category
  formData.priority = task.priority
  formData.deadline = task.deadline
  formData.estimatedTime = task.estimatedTime
  formData.description = task.description
  showDialog.value = true
}
</script>

<style scoped>
.tasks-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tasks-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tasks-header h1 {
  font-size: 24px;
  margin: 0;
}

.tasks-content {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
  flex: 1;
}

.filters-sidebar {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  height: fit-content;
  position: sticky;
  top: 0;
}

.filter-group {
  margin-bottom: 20px;
}

.filter-group h3 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}

.tasks-main {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
}

@media (max-width: 768px) {
  .tasks-content {
    grid-template-columns: 1fr;
  }

  .filters-sidebar {
    position: static;
  }
}
</style>
