<template>
  <el-drawer
    :title="`用户详情 - ${detail?.user?.username || detail?.username}`"
    :model-value="true"
    @close="$emit('close')"
    size="60%"
  >
    <el-tabs>
      <!-- 基本信息 Tab -->
      <el-tab-pane label="基本信息">
        <el-form label-width="120px">
          <el-form-item label="用户名">{{ detail?.user?.username || detail?.username }}</el-form-item>
          <el-form-item label="邮箱">{{ detail?.user?.email || detail?.email }}</el-form-item>
          <el-form-item label="昵称">{{ detail?.user?.nickname || '-' }}</el-form-item>
          <el-form-item label="角色">{{ (detail?.user?.role || detail?.role || '')?.toUpperCase() }}</el-form-item>
          <el-form-item label="注册天数">{{ detail?.user?.registrationDays ?? detail?.registrationDays ?? '-' }}</el-form-item>
          <el-form-item label="完成率">{{ detail?.user?.completionRate ?? detail?.completionRate ?? 0 }}%</el-form-item>
          <el-form-item label="最后活跃">{{ detail?.user?.lastActiveTime ?? detail?.lastActiveTime ?? '-' }}</el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 任务概览 Tab -->
      <el-tab-pane label="任务概览">
        <el-table :data="tasksRows" style="width: 100%" size="small" v-loading="tasksLoading">
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="categoryName" label="分类" width="120" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '完成' : '进行中' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="completedAt" label="完成时间" width="160">
            <template #default="{ row }">{{ formatDate(row.completedAt) }}</template>
          </el-table-column>
        </el-table>
        <div style="margin-top: 12px; text-align: right;">
          <el-pagination
            :current-page="tasksPage"
            :page-size="tasksSize"
            :total="tasksTotal"
            @current-change="loadTasks"
            layout="prev, pager, next, jumper"
          />
        </div>
      </el-tab-pane>

      <!-- 时间记录 Tab -->
      <el-tab-pane label="时间记录">
        <el-table :data="timeRows" style="width: 100%" size="small" v-loading="timeLoading">
          <el-table-column prop="recordDate" label="日期" width="120" />
          <el-table-column prop="startTime" label="开始" width="160">
            <template #default="{ row }">{{ formatDate(row.startTime) }}</template>
          </el-table-column>
          <el-table-column prop="endTime" label="结束" width="160">
            <template #default="{ row }">{{ formatDate(row.endTime) }}</template>
          </el-table-column>
          <el-table-column prop="durationMinutes" label="时长(分钟)" width="120" />
          <el-table-column prop="note" label="备注" />
        </el-table>
        <div style="margin-top: 12px; text-align: right;">
          <el-pagination
            :current-page="timePage"
            :page-size="timeSize"
            :total="timeTotal"
            @current-change="loadTimeRecords"
            layout="prev, pager, next, jumper"
          />
        </div>
      </el-tab-pane>

      <!-- 行为轨迹 Tab -->
      <el-tab-pane label="行为轨迹">
        <el-table :data="logs" v-loading="logsLoading" style="width: 100%" size="small">
          <el-table-column prop="action" label="操作" />
          <el-table-column prop="detail" label="详情">
              <template #default="{ row }">{{ (row.detail || row.result) ? ((row.detail || row.result).length > 120 ? (row.detail || row.result).slice(0,120) + '...' : (row.detail || row.result)) : '-' }}</template>
            </el-table-column>
          <el-table-column prop="ip" label="IP" width="140">
            <template #default="{ row }">{{ row.ip || row.ipAddress || '-' }}</template>
          </el-table-column>
          <el-table-column prop="userAgent" label="设备/UA" width="220" />
          <el-table-column prop="createdAt" label="时间" width="180">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>

        <div style="margin-top: 12px; text-align: right;">
          <el-pagination
            :current-page="logsPage"
            :page-size="logsSize"
            :total="logsTotal"
            @current-change="loadLogs"
            layout="prev, pager, next, jumper"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div style="flex: auto">
        <el-button @click="$emit('close')">关闭</el-button>
        <el-button type="primary" @click="handleResetPassword">重置密码</el-button>
        <el-button type="danger" @click="handleDisable">切换禁用</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as userApi from '@/api/admin/userManage'

const props = defineProps<{
  detail: any
}>()

const emits = defineEmits<{
  close: []
}>()

const formatDate = (date: string | null) => {
  if (!date) return '-'
  try {
    return new Date(date).toLocaleString('zh-CN')
  } catch (e) {
    return String(date)
  }
}

// 任务（分页）
const tasksRows = ref<any[]>([])
const tasksTotal = ref(0)
const tasksPage = ref(1)
const tasksSize = ref(10)
const tasksLoading = ref(false)

const loadTasks = async (page = 1) => {
  const id = props.detail?.user?.id || props.detail?.id
  if (!id) return
  tasksLoading.value = true
  try {
    const res: any = await userApi.getUserTasks(id, page, tasksSize.value)
    const data = res && res.data ? res.data : res
    tasksRows.value = data.rows || []
    tasksTotal.value = data.total || 0
    tasksPage.value = page
  } catch (e) {
    console.error('加载任务失败', e)
  } finally {
    tasksLoading.value = false
  }
}

// 时间记录（分页）
const timeRows = ref<any[]>([])
const timeTotal = ref(0)
const timePage = ref(1)
const timeSize = ref(10)
const timeLoading = ref(false)

const loadTimeRecords = async (page = 1) => {
  const id = props.detail?.user?.id || props.detail?.id
  if (!id) return
  timeLoading.value = true
  try {
    const res: any = await userApi.getUserTimeRecords(id, page, timeSize.value)
    const data = res && res.data ? res.data : res
    timeRows.value = data.rows || []
    timeTotal.value = data.total || 0
    timePage.value = page
  } catch (e) {
    console.error('加载时间记录失败', e)
  } finally {
    timeLoading.value = false
  }
}

// 行为轨迹（操作日志）状态
const logs = ref<any[]>([])
const logsTotal = ref(0)
const logsPage = ref(1)
const logsSize = ref(10)
const logsLoading = ref(false)

const loadLogs = async (page = 1) => {
  const id = props.detail?.user?.id || props.detail?.id
  if (!id) return
  logsLoading.value = true
  try {
    const res: any = await userApi.getUserLogs(id, page, logsSize.value)
    const data = res && res.data ? res.data : res
    logs.value = data.rows || []
    logsTotal.value = data.total || 0
    logsPage.value = page
  } catch (e) {
    console.error('加载行为轨迹失败', e)
  } finally {
    logsLoading.value = false
  }
}

onMounted(() => {
  if (props.detail) {
    loadLogs(1)
    loadTasks(1)
    loadTimeRecords(1)
  }
})

watch(() => props.detail, (d) => {
  if (d) {
    loadLogs(1)
    loadTasks(1)
    loadTimeRecords(1)
  }
})

const handleResetPassword = async () => {
  try {
    const id = props.detail?.user?.id || props.detail?.id
    if (!id) return ElMessage.error('无法识别用户')
    await userApi.resetUserPassword(id, '123456')
    ElMessage.success('密码已重置为 123456')
  } catch (e) {
    console.error(e)
    ElMessage.error('重置密码失败')
  }
}

const handleDisable = async () => {
  try {
    const id = props.detail?.user?.id || props.detail?.id
    const currentDeleted = props.detail?.user?.deleted ?? props.detail?.status ?? 0
    const newStatus = currentDeleted === 0 ? 1 : 0
    await userApi.updateUserStatus(id, newStatus)
    ElMessage.success(newStatus === 1 ? '用户已禁用' : '用户已启用')
    // reload detail and logs
    if (props.detail) loadLogs(1)
  } catch (e) {
    console.error(e)
    ElMessage.error('操作失败')
  }
}
</script>
