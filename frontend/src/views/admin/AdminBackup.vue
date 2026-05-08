<template>
  <div class="admin-backup">
    <h2>数据备份与恢复</h2>
    <el-card>
      <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
        <el-select v-model="format" placeholder="格式" style="width:120px">
          <el-option label="JSON" value="json" />
          <el-option label="SQL" value="sql" />
        </el-select>

        <el-checkbox-group v-model="selectedTables">
          <el-checkbox value="user">用户</el-checkbox>
          <el-checkbox value="task">任务</el-checkbox>
          <el-checkbox value="time_record">时间记录</el-checkbox>
          <el-checkbox value="operation_log">操作日志</el-checkbox>
        </el-checkbox-group>

        <el-button type="primary" :loading="creating" @click="onCreateBackup">创建备份</el-button>

        <el-upload ref="uploader" :before-upload="onBeforeUpload" :show-file-list="false">
          <el-button>选择恢复文件</el-button>
        </el-upload>

        <el-button type="danger" :disabled="!selectedFile || restoring" @click="onUploadRestore">上传并恢复</el-button>
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <el-table :data="backups" style="width:100%">
        <el-table-column prop="backupName" label="文件名" />
        <el-table-column prop="fileSize" label="大小" :formatter="formatSize" />
        <el-table-column prop="backupTime" label="时间" :formatter="formatTime" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="mini" @click="download(row)">下载</el-button>
            <el-button size="mini" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="confirmDialogVisible" title="确认恢复" width="480px">
      <div>恢复将覆盖现有数据，风险较高。请输入 <strong>CONFIRM</strong> 以继续：</div>
      <el-input v-model="confirmText" placeholder="输入 CONFIRM" style="margin-top:12px" />
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="confirmText !== 'CONFIRM'" @click="confirmRestore">确认恢复</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as api from '@/api/admin/backup'
import { ElMessage } from 'element-plus'

const format = ref<'json'|'sql'>('json')
const selectedTables = ref<string[]>(['user','task','time_record','operation_log'])
const creating = ref(false)
const backups = ref<any[]>([])
const selectedFile = ref<File|null>(null)
const restoring = ref(false)

const confirmDialogVisible = ref(false)
const confirmText = ref('')

function formatSize(row: any, column: any, cellValue: any) {
  const size = (typeof cellValue === 'number' ? cellValue : (row && (row.fileSize ?? row.file_size))) || 0
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(2) + ' MB'
}

function formatTime(row: any, column: any, cellValue: any) {
  const t = cellValue ?? (row && (row.backupTime ?? row.backup_time))
  if (!t) return ''
  try { return new Date(t).toLocaleString() } catch { return String(t) }
}

async function loadBackups() {
  try {
    const res: any = await api.listBackups(1, 50)
    // handle different shapes
    backups.value = res.records || res.rows || res.content || res || []
  } catch (e) {
    console.error(e)
  }
}

async function onCreateBackup() {
  creating.value = true
  try {
    const payload = { format: format.value, tables: selectedTables.value }
    const res = await api.createBackup(payload)
    ElMessage.success('备份已创建: ' + (res.backupName || ''))
    await loadBackups()
  } catch (err: any) {
    ElMessage.error('创建备份失败: ' + (err.message || err))
  } finally { creating.value = false }
}

function onBeforeUpload(file: File) {
  selectedFile.value = file
  return false // prevent auto upload
}

async function onUploadRestore() {
  if (!selectedFile.value) return ElMessage.warning('请先选择文件')
  try {
    restoring.value = true
    // first call without confirm to get potential confirmation requirement
    const resp: any = await api.restoreUpload(selectedFile.value, false)
    if (resp && resp.status === 'need_confirm') {
      confirmDialogVisible.value = true
    } else {
      ElMessage.success('恢复成功')
      await loadBackups()
    }
  } catch (e: any) {
    ElMessage.error('恢复失败: ' + (e.message || e))
  } finally { restoring.value = false }
}

async function confirmRestore() {
  confirmDialogVisible.value = false
  if (confirmText.value !== 'CONFIRM') return
  try {
    restoring.value = true
    const resp: any = await api.restoreUpload(selectedFile.value as File, true)
    if (resp && resp.status === 'success') {
      ElMessage.success('恢复完成')
      selectedFile.value = null
      confirmText.value = ''
      await loadBackups()
    } else {
      ElMessage.warning('恢复响应: ' + JSON.stringify(resp))
    }
  } catch (e: any) {
    ElMessage.error('恢复失败: ' + (e.message || e))
  } finally { restoring.value = false }
}

async function download(row: any) {
  try {
    const resp: any = await api.downloadBackup(row.id)
    const blob = new Blob([resp.data || resp], { type: 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.backupName || 'backup'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error('下载失败')
  }
}

async function remove(row: any) {
  try {
    await api.deleteBackup(row.id)
    ElMessage.success('已删除')
    await loadBackups()
  } catch (e: any) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => { loadBackups() })
</script>
<style scoped>
.admin-backup { padding: 24px; }
</style>
