<template>
  <div class="alert-panel">
    <div class="panel-actions" style="display:flex;gap:8px;margin-bottom:8px;align-items:center;">
      <el-button size="small" type="primary" @click="markAllAsRead" :loading="loading">全部已读</el-button>
      <el-button size="small" @click="refresh" :loading="loading">刷新</el-button>
      <el-button size="small" type="danger" @click="clearAll">清空</el-button>
      <div style="flex:1"></div>
      <div>未读: <strong>{{ alertStore.unreadCount }}</strong></div>
    </div>

    <!-- 开发/调试时显示原始数据，便于快速定位问题 -->
    <!-- <div v-if="debug" style="margin-bottom:8px;max-height:240px;overflow:auto;border:1px dashed #e6e6e6;padding:8px;background:#fafafa;">
      <pre style="white-space:pre-wrap;word-break:break-word;margin:0">{{ alertStore.alerts }}</pre>
    </div> -->

    <el-table
      v-loading="loading"
      :data="alertStore.alerts"
      style="width: 100%"
      @selection-change="onSelectionChange"
      row-key="id"
    >
      <el-table-column type="selection" width="50"></el-table-column>

      <el-table-column prop="title" label="标题" min-width="180">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:8px;">
            <el-tag v-if="row.severity" :type="row.severity==='critical' || row.severity==='high' ? 'danger' : (row.severity==='medium' ? 'warning' : 'info')">{{ row.severity }}</el-tag>
            <div>
              <div style="font-weight:600">{{ row.title }}</div>
              <div style="font-size:12px;color:#666">{{ row.relatedUsername ? '用户: ' + row.relatedUsername : '' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="message" label="内容" min-width="260">
        <template #default="{ row }">
          <div style="white-space:normal">{{ row.message }}</div>
        </template>
      </el-table-column>

      <el-table-column label="风险分" width="100">
        <template #default="{ row }">
          <el-tag :type="(row.riskScore || 0) >= 80 ? 'danger' : ((row.riskScore || 0) >= 50 ? 'warning' : 'success')">{{ row.riskScore ?? 0 }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="aiSuggestion" label="AI建议" min-width="220">
        <template #default="{ row }">
          <el-tooltip :content="row.aiSuggestion" placement="top" :disabled="!row.aiSuggestion || row.aiSuggestion.length <= 50">
            <span>{{ row.aiSuggestion && row.aiSuggestion.length > 50 ? row.aiSuggestion.slice(0,50) + '...' : (row.aiSuggestion || '-') }}</span>
          </el-tooltip>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">
          <div>{{ formatDate(row.createdAt) }}</div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button v-if="!row.read" type="primary" size="small" @click="markRead(row)">确认</el-button>
          <el-button type="text" size="small" @click="ignore(row)">忽略</el-button>
          <el-button v-if="row.relatedUsername" type="primary" size="small" @click="() => handleNotify(row)" style="margin-left:8px">通知用户</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && (!alertStore.alerts || alertStore.alerts.length === 0)" style="padding:16px;text-align:center;color:#888">暂无告警</div>

    <div style="margin-top:10px;display:flex;gap:8px;align-items:center;">
      <el-button size="small" type="primary" @click="markSelectedAsRead" :disabled="selected.length===0">标记选中为已读</el-button>
      <el-button size="small" type="danger" @click="deleteSelected" :disabled="selected.length===0">删除选中</el-button>
      <div style="flex:1"></div>
      <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :current-page.sync="page"
        :total="total"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useAlertStore } from '@/store/alert'
import { notifyUser, batchDeleteAlerts } from '@/api/admin/alert'
import { useUserStore } from '@/store/user'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const alertStore = useAlertStore()

const loading = ref(false)
const selected = ref<any[]>([])
const page = ref(1)
const pageSize = 10
const total = 0
// 仅在开发环境显示原始数据，便于调试
const debug = !!(import.meta.env && import.meta.env.DEV)

function formatDate(v: any) {
  if (!v) return ''
  try { return new Date(v).toLocaleString() } catch (e) { return v }
}

async function refresh() {
  loading.value = true
  try {
    await alertStore.fetchAlerts({ page: page.value, size: pageSize })
  } finally {
    loading.value = false
  }
}

function onSelectionChange(rows: any[]) {
  // 直接赋值给 ref 的 value，确保响应性正确
  selected.value = Array.isArray(rows) ? rows : []
}

async function markRead(row: any) {
  loading.value = true
  try {
    await alertStore.markAsRead(row.id)
  } finally { loading.value = false }
}

async function ignore(row: any) {
  loading.value = true
  try {
    await alertStore.ignoreAlert(row.id)
  } finally { loading.value = false }
}

async function markSelectedAsRead() {
  loading.value = true
  try {
    await Promise.all(selected.value.map(s => alertStore.markAsRead(s.id)))
    selected.value = []
  } finally { loading.value = false }
}

async function ignoreSelected() {
  loading.value = true
  try {
    // 非管理员用户仍然执行忽略（标记为已读）；管理员用户执行批量删除
    const userStore = useUserStore()
    if (userStore.user && userStore.user.role === 'admin') {
      const ids = selected.value.map(s => (typeof s.id === 'string' ? parseInt(s.id) : s.id)).filter(i => !Number.isNaN(i))
      if (ids.length > 0) {
        await batchDeleteAlerts(ids)
        // 刷新列表以反映删除结果
        await alertStore.fetchAlerts({ page: page.value, size: pageSize })
      }
    } else {
      await Promise.all(selected.value.map(s => alertStore.ignoreAlert(s.id)))
    }
    selected.value = []
  } finally { loading.value = false }
}

async function deleteSelected() {
  // 兼容：deleteSelected 现在与 ignoreSelected 行为类似（管理员删除，普通用户忽略）
  return ignoreSelected()
}

async function handleNotify(row: any) {
  if (!row || !row.relatedUsername) return ElMessage.warning('缺少用户名')
  loading.value = true
  try {
    await notifyUser(row.id, '', row.relatedUsername)
    ElMessage.success(`已通知用户 ${row.relatedUsername}`)
    await alertStore.fetchAlerts({ page: page.value, size: pageSize })
  } catch (e) {
    console.warn('notify failed', e)
    ElMessage.error('通知用户失败')
  } finally { loading.value = false }
}

async function markAllAsRead() {
  loading.value = true
  try { await alertStore.markAllAsRead() } finally { loading.value = false }
}

function clearAll() {
  alertStore.clear()
}

function onPageChange(p: number) {
  page.value = p
  refresh()
}

async function handleFreeze(username: string) {
  if (!username) return ElMessage.warning('缺少用户名');
  try {
    loading.value = true
    await axios.post(`/api/v1/admin/users/${encodeURIComponent(username)}/disable`)
    ElMessage.success(`已禁用用户 ${username}`)
    await alertStore.fetchAlerts({ page: page.value, size: pageSize })
  } catch (e) {
    console.warn('freeze failed', e)
    ElMessage.error('禁用用户失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => { refresh(); if (debug) console.info('[AlertPanel] mounted -> alerts=', alertStore.alerts) })

watch(() => alertStore.alerts, (v) => { if (debug) console.info('[AlertPanel] alerts changed', v) }, { deep: true })
</script>
