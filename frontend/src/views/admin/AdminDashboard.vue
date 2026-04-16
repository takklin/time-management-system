<template>
  <div class="admin-dashboard">
    <h2>管理员仪表盘</h2>
    <!-- 第一行：核心指标卡片 -->
    <el-row :gutter="20" class="dashboard-metrics">
      <el-col :span="4"><el-card>
        <div class="metric-title">今日活跃用户</div>
        <div class="metric-value">{{ stat.todayActiveUserCount ?? '--' }}</div>
        <div class="metric-sub">昨日: {{ stat.yesterdayActiveUserCount ?? '--' }}
          <span :class="trendClass(stat.dauChange)">{{ stat.dauChange > 0 ? '+' : '' }}{{ stat.dauChange ?? '--' }}%</span>
        </div>
      </el-card></el-col>
      <el-col :span="4"><el-card>
        <div class="metric-title">总注册用户</div>
        <div class="metric-value">{{ stat.totalUserCount ?? '--' }}</div>
        <div class="metric-sub">近7日新增: {{ stat.last7DaysRegisterCount ?? '--' }}</div>
      </el-card></el-col>
      <el-col :span="4"><el-card>
        <div class="metric-title">API请求数量</div>
        <div class="metric-value">{{ health.totalRequests ?? '0' }}</div>
        <div class="metric-sub">平均响应: {{ health.avgResponseTime?.toFixed(2) ?? '--' }}ms</div>
      </el-card></el-col>
      <el-col :span="4"><el-card>
        <div class="metric-title">近7日任务完成率</div>
        <div class="metric-value">{{ stat.last7DaysTaskFinishRate ?? '--' }}%</div>
        <div class="metric-sub">创建: {{ stat.last7DaysTaskCreated ?? '--' }} 完成: {{ stat.last7DaysTaskFinished ?? '--' }}</div>
      </el-card></el-col>
      <el-col :span="4"><el-card>
        <div class="metric-title">API错误率</div>
        <div class="metric-value" :class="{ 'error-high': (health.errorRate || 0) > 0.05 }">
          {{ ((health.errorRate || 0) * 100)?.toFixed(2) ?? '--' }}%
        </div>
        <div class="metric-sub">错误数: {{ health.errorCount ?? '0' }}</div>
      </el-card></el-col>
    </el-row>

    <!-- 系统性能指标卡片 -->
    <el-row :gutter="20" style="margin-top: 12px;">
      <el-col :span="6"><el-card class="health-card">
        <el-statistic title="慢查询数量" :value="health.slowQueryCount || 0" />
      </el-card></el-col>
      <el-col :span="6"><el-card class="health-card">
        <el-statistic title="成功请求数" :value="health.successCount || 0" />
      </el-card></el-col>
      <el-col :span="6"><el-card class="health-card">
        <el-statistic title="成功率" :value="((health.successRate || 0) * 100)" suffix="%" :precision="2" />
      </el-card></el-col>
      <el-col :span="6"><el-card class="health-card">
        <el-statistic title="统计时间范围" :value="health.timeRange || 60" suffix="分钟" />
      </el-card></el-col>
    </el-row>

    <!-- 第二行：趋势图表 -->
    <el-row :gutter="20" style="margin-top: 24px;">
      <el-col :span="12">
        <el-card>
          <div class="metric-title">近30天活跃用户趋势</div>
          <div id="dauTrendChart" style="height: 260px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="metric-title">近7天任务创建/完成对比</div>
          <div id="taskTrendChart" style="height: 260px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行：异常预警和系统健康 -->
    <el-row :gutter="20" style="margin-top: 24px;">
      <el-col :span="12">
        <el-card>
          <div class="metric-title">
            <el-icon><WarningFilled /></el-icon>
            异常预警（最近5条）
          </div>
          <ul class="alert-list">
            <li v-for="alert in systemAlerts" :key="alert.id" :class="'alert-' + alert.severity">
              <el-tag :type="alert.severity === 'critical' ? 'danger' : 'warning'" effect="dark" size="small">
                {{ alert.severity === 'critical' ? '超高风险' : '高危' }}
              </el-tag>
              <span class="alert-desc">{{ alert.description }}</span>
              <span class="alert-time">{{ formatTime(alert.createdAt) }}</span>
            </li>
            <li v-if="systemAlerts.length === 0" class="empty-state">暂无异常预警</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="metric-title">
            <el-icon><Monitor /></el-icon>
            实时系统信息
          </div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="API平均响应时间">
              <el-progress 
                :percentage="Math.min(health.avgResponseTime / 20, 100)" 
                :color="getHealthColor(health.avgResponseTime, 2000)"
              />
              {{ health.avgResponseTime?.toFixed(2) || '0' }} ms
            </el-descriptions-item>
            <el-descriptions-item label="可用性">
              <el-progress 
                :percentage="(health.successRate || 0) * 100" 
                :color="getHealthColor((health.successRate || 0) * 100, 95)"
              />
              {{ ((health.successRate || 0) * 100)?.toFixed(2) || '0' }}%
            </el-descriptions-item>
            <el-descriptions-item label="最后更新">
              {{ new Date().toLocaleTimeString('zh-CN') }}</el-descriptions-item>
            <el-descriptions-item label="QPS (最后1分钟)">
              {{ health.lastMinute?.toFixed(2) || '0' }} 次/秒
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { getSystemStat } from '@/api/admin/systemStat'
import * as metricsApi from '@/api/admin/metrics'
import * as alertApi from '@/api/admin/alert'
import * as echarts from 'echarts'
import { WarningFilled, Monitor } from '@element-plus/icons-vue'

const stat = ref<any>({})
const health = ref<any>({})
const systemAlerts = ref<any[]>([])
let refreshInterval: any = null

const trendClass = (change: number) => {
  if (change > 0) return 'trend-up'
  if (change < 0) return 'trend-down'
  return ''
}

const formatTime = (time: string | number) => {
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN')
}

const getHealthColor = (value: number, threshold: number) => {
  if (value > threshold * 0.8) return '#f56c6c' // 红色 - 不健康
  if (value > threshold * 0.5) return '#e6a23c' // 黄色 -  警告
  return '#67c23a' // 绿色 - 健康
}

const renderCharts = () => {
  try {
    // 活跃用户趋势
    const dauChartEl = document.getElementById('dauTrendChart')
    if (dauChartEl) {
      const dauChart = echarts.init(dauChartEl)
      dauChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: stat.value.dauTrend?.map((d: any) => d.date) || [] },
        yAxis: { type: 'value' },
        series: [{ name: '活跃用户', type: 'line', data: stat.value.dauTrend?.map((d: any) => d.count) || [], smooth: true }]
      })
    }
    
    // 任务创建/完成对比
    const taskChartEl = document.getElementById('taskTrendChart')
    if (taskChartEl) {
      const taskChart = echarts.init(taskChartEl)
      taskChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['创建', '完成'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: stat.value.taskTrend?.map((d: any) => d.date) || [] },
        yAxis: { type: 'value' },
        series: [
          { name: '创建', type: 'bar', data: stat.value.taskTrend?.map((d: any) => d.created) || [] },
          { name: '完成', type: 'bar', data: stat.value.taskTrend?.map((d: any) => d.finished) || [] }
        ]
      })
    }
  } catch (err) {
    console.error('渲染图表失败:', err)
  }
}

const loadStat = async () => {
  try {
    const res: any = await getSystemStat()
    const data = res.data || res
    
    stat.value = {
      todayActiveUserCount: data.todayActiveUserCount || 0,
      yesterdayActiveUserCount: data.yesterdayActiveUserCount || 0,
      dauChange: data.dauChange || 0,
      totalUserCount: data.totalUserCount || 0,
      last7DaysRegisterCount: data.last7DaysRegisterCount || 0,
      last7DaysTaskFinishRate: data.last7DaysTaskFinishRate || 0,
      last7DaysTaskCreated: data.last7DaysTaskCreated || 0,
      last7DaysTaskFinished: data.last7DaysTaskFinished || 0,
      dauTrend: data.dauTrend || [],
      taskTrend: data.taskTrend || [],
      highRiskLogs: data.highRiskLogs || []
    }
    console.log('[Dashboard] 仪表盘数据加载成功')
  } catch (err) {
    console.error('[Dashboard] 加载仪表盘数据失败:', err)
  }
}

const loadHealthMetrics = async () => {
  try {
    const res: any = await metricsApi.getHealthMetrics(60)
    const data = res.data || res
    health.value = data
    console.log('[Dashboard] 系统健康度指标更新:', data)
  } catch (err) {
    console.error('[Dashboard] 加载健康度指标失败:', err)
  }
}

const loadAlerts = async () => {
  try {
    const res: any = await alertApi.getUnhandledAlerts(5)
    systemAlerts.value = (res.data || res || []).slice(0, 5)
    console.log('[Dashboard] 异常预警加载成功:', systemAlerts.value)
  } catch (err) {
    console.error('[Dashboard] 加载预警失败:', err)
  }
}

const loadDashboardData = async () => {
  await Promise.all([loadStat(), loadHealthMetrics(), loadAlerts()])
  await nextTick()
  renderCharts()
}

onMounted(async () => {
  await loadDashboardData()
  
  // 每30秒刷新一次系统健康度和预警数据
  refreshInterval = setInterval(async () => {
    await Promise.all([loadHealthMetrics(), loadAlerts()])
  }, 30000)
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

h2 {
  margin-bottom: 24px;
  color: #333;
}

.dashboard-metrics .el-card {
  min-height: 110px;
  border-radius: 8px;
}

.health-card {
  border-radius: 8px;
}

.metric-title {
  font-size: 14px;
  color: #888;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 4px;
  transition: color 0.3s;
}

.metric-value.error-high {
  color: #f56c6c;
}

.metric-sub {
  font-size: 12px;
  color: #999;
}

.trend-up {
  color: #67c23a;
  margin-left: 6px;
}

.trend-down {
  color: #f56c6c;
  margin-left: 6px;
}

.alert-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.alert-list li {
  padding: 8px 12px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-left: 4px solid #f56c6c;
  background: #fef0f0;
  border-radius: 4px;
}

.alert-list li.alert-high {
  border-left-color: #e6a23c;
  background: #fdf6ec;
}

.alert-list li.empty-state {
  background: transparent;
  border-left: none;
  color: #999;
  justify-content: center;
}

.alert-desc {
  flex: 1;
  font-size: 12px;
  color: #666;
}

.alert-time {
  font-size: 11px;
  color: #999;
  white-space: nowrap;
}

:deep(.el-progress) {
  margin-right: 8px;
}
</style>
