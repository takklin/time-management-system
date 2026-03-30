<template>
  <div class="dashboard-container">
    <div class="welcome-card">
      <h2>欢迎回来，{{ userStore.user?.username }}</h2>
      <p>{{ currentDate }} | 今天是个美好的一天</p>
    </div>

    <div class="stats-row">
      <el-card class="stat-card">
        <template #header>
          <div class="card-header">
            <span>任务总数</span>
          </div>
        </template>
        <div class="stat-value">{{ taskStats.total }}</div>
      </el-card>

      <el-card class="stat-card">
        <template #header>
          <div class="card-header">
            <span>完成率</span>
          </div>
        </template>
        <div class="stat-value">{{ taskStats.completionRate }}%</div>
      </el-card>

      <el-card class="stat-card">
        <template #header>
          <div class="card-header">
            <span>本周专注时长</span>
          </div>
        </template>
        <div class="stat-value">{{ taskStats.weeklyFocusHours }}h</div>
      </el-card>
    </div>

    <div class="content-row">
      <ChartCard title="今日待办任务" class="flex-1">
        <el-empty v-if="todayTasks.length === 0" description="暂无今日待办任务" />
        <TaskItem
          v-for="task in todayTasks"
          :key="task.id"
          :task="task"
          @complete="completeTask"
          @delete="deleteTask"
          @edit="editTask"
        />
      </ChartCard>

      <ChartCard title="本周任务完成率" class="flex-1">
        <div id="weeklyChart" style="height: 300px"></div>
      </ChartCard>
    </div>

    <ChartCard title="本周专注时长">
      <div id="focusChart" style="height: 300px"></div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { useTaskStore } from '@/store/task'
import ChartCard from '@/components/ChartCard.vue'
import TaskItem from '@/components/TaskItem.vue'
import * as echarts from 'echarts'

const userStore = useUserStore()
const taskStore = useTaskStore()

const currentDate = ref('')
const taskStats = reactive({
  total: 0,
  completionRate: 0,
  weeklyFocusHours: 0,
})

const todayTasks = computed(() => {
  return taskStore.tasks.filter(t => !t.completed).slice(0, 5)
})

onMounted(async () => {
  // 格式化当前日期
  const now = new Date()
  currentDate.value = now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  })

  // 获取任务列表
  await taskStore.fetchTasks()

  // 计算统计数据
  taskStats.total = taskStore.tasks.length
  taskStats.completionRate = Math.round(
    (taskStore.tasks.filter(t => t.completed).length / taskStats.total) * 100 || 0
  )
  taskStats.weeklyFocusHours = 28

  // 初始化图表
  initCharts()
})

const initCharts = () => {
  // 本周任务完成率
  const weeklyChart = echarts.init(document.getElementById('weeklyChart'))
  weeklyChart.setOption({
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        data: [2, 3, 2, 4, 3, 5, 2],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#409EFF' },
      },
    ],
  })

  // 本周专注时长
  const focusChart = echarts.init(document.getElementById('focusChart'))
  focusChart.setOption({
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        data: [4, 5, 3, 6, 4, 3, 3],
        type: 'bar',
        itemStyle: { color: '#67C23A' },
      },
    ],
  })
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

const editTask = () => {
  // TODO: 打开编辑对话框
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 30px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.welcome-card h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
}

.welcome-card p {
  margin: 0;
  opacity: 0.8;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.content-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.flex-1 {
  flex: 1;
}

@media (max-width: 1024px) {
  .content-row {
    grid-template-columns: 1fr;
  }
}
</style>
