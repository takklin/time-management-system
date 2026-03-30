<template>
  <div class="todos-container">
    <div class="todos-input">
      <el-input
        v-model="newTodoTitle"
        placeholder="添加待办事项..."
        @keyup.enter="addTodo"
        size="large"
        clearable
      >
        <template #suffix>
          <el-button type="primary" @click="addTodo">添加</el-button>
        </template>
      </el-input>
    </div>

    <div class="todos-filters">
      <el-button
        v-for="filter in filters"
        :key="filter"
        :type="activeFilter === filter ? 'primary' : 'info'"
        plain
        @click="activeFilter = filter"
      >
        {{ filterLabels[filter] }}
      </el-button>
    </div>

    <div class="todos-content">
      <div v-for="(todos, group) in groupedTodos" :key="group" class="todo-group">
        <div class="group-title">
          {{ groupTitles[group] }}
          <span class="group-count">({{ todos.length }})</span>
        </div>
        <el-collapse>
          <el-collapse-item :title="`${groupTitles[group]} (${todos.length})`" :name="group">
            <el-empty v-if="todos.length === 0" :description="`暂无${groupTitles[group].toLowerCase()}`" />
            <div v-for="todo in todos" :key="todo.id" class="todo-item">
              <el-checkbox v-model="todo.completed" @change="updateTodo" />
              <span class="todo-title" :class="{ completed: todo.completed }">{{ todo.title }}</span>
              <el-tag v-if="todo.priority" :type="getPriorityType(todo.priority)" size="small">
                {{ todo.priority }}
              </el-tag>
              <div class="todo-actions">
                <el-button type="primary" link size="small" @click="editTodo">编辑</el-button>
                <el-button type="danger" link size="small" @click="deleteTodo(todo.id)">删除</el-button>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'

interface Todo {
  id?: number
  title: string
  priority?: 'high' | 'medium' | 'low'
  deadline?: string
  completed: boolean
}

const newTodoTitle = ref('')
const activeFilter = ref('all')
const todos = reactive<Todo[]>([
  { id: 1, title: '完成项目报告', priority: 'high', deadline: dayjs().format('YYYY-MM-DD'), completed: false },
  { id: 2, title: '回复邮件', priority: 'medium', deadline: dayjs().add(1, 'day').format('YYYY-MM-DD'), completed: true },
])

const filters = ['all', 'active', 'completed']
const filterLabels: Record<string, string> = {
  all: '全部',
  active: '未完成',
  completed: '已完成',
}

const groupTitles: Record<string, string> = {
  today: '今天',
  tomorrow: '明天',
  future: '未来',
  completed: '已完成',
}

const filteredTodos = computed(() => {
  let filtered = todos
  if (activeFilter.value === 'active') {
    filtered = filtered.filter(t => !t.completed)
  } else if (activeFilter.value === 'completed') {
    filtered = filtered.filter(t => t.completed)
  }
  return filtered
})

const groupedTodos = computed(() => {
  const grouped: Record<string, Todo[]> = {
    today: [],
    tomorrow: [],
    future: [],
    completed: [],
  }

  const today = dayjs().format('YYYY-MM-DD')
  const tomorrow = dayjs().add(1, 'day').format('YYYY-MM-DD')

  filteredTodos.value.forEach(todo => {
    if (todo.completed) {
      grouped.completed.push(todo)
    } else if (!todo.deadline || todo.deadline === today) {
      grouped.today.push(todo)
    } else if (todo.deadline === tomorrow) {
      grouped.tomorrow.push(todo)
    } else {
      grouped.future.push(todo)
    }
  })

  return grouped
})

const getPriorityType = (priority: string) => {
  const map: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'success',
  }
  return map[priority] || 'info'
}

const addTodo = () => {
  if (!newTodoTitle.value.trim()) {
    ElMessage.error('请输入待办事项')
    return
  }

  const newTodo: Todo = {
    id: Date.now(),
    title: newTodoTitle.value,
    completed: false,
    deadline: dayjs().format('YYYY-MM-DD'),
  }

  todos.push(newTodo)
  newTodoTitle.value = ''
  ElMessage.success('待办事项添加成功')
}

const updateTodo = () => {
  // TODO: 更新待办事项
}

const editTodo = () => {
  ElMessage.info('编辑功能开发中')
}

const deleteTodo = (id: number | undefined) => {
  if (!id) return
  const index = todos.findIndex(t => t.id === id)
  if (index > -1) {
    todos.splice(index, 1)
    ElMessage.success('待办事项删除成功')
  }
}
</script>

<style scoped>
.todos-container {
  max-width: 800px;
  margin: 0 auto;
}

.todos-input {
  margin-bottom: 20px;
}

.todos-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.todos-content {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
}

.todo-group {
  margin-bottom: 20px;
}

.group-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #333;
}

.group-count {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}

.todo-item:last-child {
  border-bottom: none;
}

.todo-title {
  flex: 1;
  font-size: 14px;
}

.todo-title.completed {
  color: #ccc;
  text-decoration: line-through;
}

.todo-actions {
  display: flex;
  gap: 10px;
}
</style>
