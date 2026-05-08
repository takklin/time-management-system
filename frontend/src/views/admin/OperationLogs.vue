<template>
  <div class="admin-operation-logs" style="padding: 20px;">
    <!-- 筛选表单 -->
    <el-card class="filter-card" shadow="hover">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="操作人">
          <el-input v-model="queryParams.operator" placeholder="输入用户名" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="操作">
          <el-input v-model="queryParams.action" placeholder="输入操作类型" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="queryParams.result" clearable placeholder="全部" style="width:120px">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker 
            v-model="queryParams.dateRange" 
            type="daterange" 
            range-separator="至" 
            start-placeholder="开始日期" 
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:260px" 
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="onReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
          <el-button type="success" @click="onExport">
            <el-icon><Download /></el-icon> 导出
          </el-button>
          <el-button type="warning" @click="onSeedDemo">
            生成演示数据
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
      <el-table
      ref="tableRef"
      :data="tableData"
      stripe 
      border
      v-loading="loading"
      style="width: 100%; margin-top: 16px;"
      @sort-change="onSortChange"
      @row-click="onRowClick"
      :row-class-name="tableRowClass"
    >
      <el-table-column prop="id" label="ID" width="70" sortable="custom" />
      <el-table-column prop="operator" label="操作人" width="100" />
      <el-table-column prop="action" label="操作类型" width="120" />
      <el-table-column prop="target" label="操作对象" width="120" />
      <el-table-column prop="result" label="结果" width="80">
        <template #default="{ row }">
          <el-tag :type="isResultSuccess(row.result) ? 'success' : 'danger'">
            {{ isResultSuccess(row.result) ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="riskLevel" label="风险等级" width="140">
        <template #default="{ row }">
          <el-tag 
            :type="getRiskLevelType(row.riskLevel)"
            :effect="row.riskLevel === 'critical' ? 'dark' : 'light'"
            :class="{ 'risk-badge-critical': row.riskLevel === 'critical' }"
          >
            {{ getRiskLevelText(row.riskLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP地址" width="130" />
      <el-table-column prop="createdAt" label="操作时间" width="170" sortable="custom" />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="onRowClick(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="paginationData.total > 0"
      class="pagination-wrapper"
      :current-page="paginationData.page"
      :page-size="paginationData.pageSize"
      :total="paginationData.total"
      layout="total, prev, pager, next, jumper"
      @current-change="onPageChange"
    />

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailDrawer" title="操作日志详情" size="45%">
      <div v-if="detailLog" class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="操作ID">{{ detailLog.id }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detailLog.operator }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ detailLog.action }}</el-descriptions-item>
          <el-descriptions-item label="操作对象">{{ detailLog.target }}</el-descriptions-item>
          <el-descriptions-item label="操作结果">
            <el-tag :type="isResultSuccess(detailLog.result) ? 'success' : 'danger'">
              {{ isResultSuccess(detailLog.result) ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ detailLog.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ detailLog.ip || '--' }}</el-descriptions-item>
          <el-descriptions-item label="设备信息">
            <div class="user-agent" v-if="detailLog.userAgent">
              <span>{{ parseUserAgent(detailLog.userAgent) }}</span>
            </div>
            <span v-else>--</span>
          </el-descriptions-item>
          <el-descriptions-item label="错误信息" v-if="detailLog.errorMessage">
            <el-alert :title="detailLog.errorMessage" type="error" :closable="false" />
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import * as logApi from '@/api/admin/log'

const route = useRoute()

const loading = ref(false)
const tableData = ref<any[]>([])

const queryParams = reactive({
  operator: '',
  action: '',
  result: '',
  dateRange: [] as string[]
})

const paginationData = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const sort = reactive<{ prop?: string; order?: string }>({})

const detailDrawer = ref(false)
const detailLog = ref<any>(null)

/**
 * 解析User-Agent字符串，提取关键信息
 */
const parseUserAgent = (ua: string) => {
  if (!ua) return '--'
  if (ua.includes('Mobile')) return '移动设备'
  if (ua.includes('Windows')) return 'Windows桌面'
  if (ua.includes('Mac')) return 'Mac桌面'
  if (ua.includes('Linux')) return 'Linux'
  if (ua.includes('Android')) return 'Android'
  if (ua.includes('iPhone')) return 'iPhone'
  return '其他设备'
}

/**
 * 判断操作结果是否为成功（大小写不敏感）
 */
const isResultSuccess = (result: any): boolean => {
  if (!result && result !== 0) return false
  const s = String(result).toLowerCase()
  return s === 'success' || s === 'succeeded' || s === 'ok' || s === 'true'
}

/**
 * 行样式：超高风险高亮整行
 */
const tableRowClass = ({ row }: { row: any }) => {
  if (!row) return ''
  if (row.riskLevel === 'critical') return 'row-critical'
  if (row.riskLevel === 'high') return 'row-high'
  return ''
}

/**
 * 获取风险等级的标签类型
 */
const getRiskLevelType = (level: string): string => {
  switch (level) {
    case 'critical':
      return 'danger'
    case 'high':
      return 'warning'
    case 'medium':
      return 'warning'
    default:
      return 'info'
  }
}

/**
 * 获取风险等级的显示文本
 */
const getRiskLevelText = (level: string): string => {
  switch (level) {
    case 'critical':
      return '🔴 超高风险'
    case 'high':
      return '🟠 高危'
    case 'medium':
      return '🟡 中危'
    default:
      return '⚪ 低危'
  }
}

/**
 * 加载操作日志
 */
const loadLogs = async () => {
  loading.value = true
  try {
    const params: any = {
      page: paginationData.page,
      size: paginationData.pageSize,
      operator: queryParams.operator || undefined,
      action: queryParams.action || undefined,
      result: queryParams.result || undefined
    }
    
    if (queryParams.dateRange && queryParams.dateRange.length === 2) {
      params.startDate = queryParams.dateRange[0]
      params.endDate = queryParams.dateRange[1]
    }
    
    const res: any = await logApi.getLogs(params)
    tableData.value = res.content || []
    // 使用后端返回的分页元数据（如果有）以保持前后端一致
    if (res.pageNum) paginationData.page = res.pageNum
    if (res.pageSize) paginationData.pageSize = res.pageSize
    paginationData.total = res.total || 0
  } catch (error) {
    console.error('加载日志失败:', error)
    ElMessage.error('加载操作日志失败')
    tableData.value = []
    paginationData.total = 0
  } finally {
    loading.value = false
  }
}

/**
 * 查询日志
 */
const onSearch = () => {
  paginationData.page = 1
  loadLogs()
}

/**
 * 重置查询条件
 */
const onReset = () => {
  queryParams.operator = ''
  queryParams.action = ''
  queryParams.result = ''
  queryParams.dateRange = []
  paginationData.page = 1
  loadLogs()
}

/**
 * 分页变化
 */
const onPageChange = (page: number) => {
  paginationData.page = page
  loadLogs()
}

/**
 * 每页大小变化
 */
const onPageSizeChange = (size: number) => {
  paginationData.pageSize = size
  paginationData.page = 1
  loadLogs()
}

/**
 * 排序变化
 */
const onSortChange = (column: any) => {
  if (!column.prop) {
    sort.prop = undefined
    sort.order = undefined
  } else {
    sort.prop = column.prop
    sort.order = column.order
  }
  // 如果需要，可以重新加载数据
  // loadLogs()
}

/**
 * 导出CSV
 */
const onExport = () => {
  if (tableData.value.length === 0) {
    ElMessage.warning('没有数据可导出')
    return
  }
  
  const header = ['ID', '操作人', '操作类型', '操作对象', '结果', 'IP地址', '操作时间']
    const rows = tableData.value.map(log => [
    log.id,
    log.operator,
    log.action,
    log.target,
    isResultSuccess(log.result) ? '成功' : '失败',
    log.ip || '--',
    log.createdAt
  ])
  
  const csv = [header, ...rows]
    .map(row => row.map(cell => `"${cell}"`).join(','))
    .join('\n')
  
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `operation_logs_${new Date().toISOString().split('T')[0]}.csv`
  a.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('导出成功')
}

/**
 * 生成演示用的高危/超高风险日志（会调用后端 seed 接口）
 */
const onSeedDemo = async () => {
  try {
    loading.value = true
    await logApi.seedDemoLogs()
    ElMessage.success('演示数据已生成，正在刷新列表')
    paginationData.page = 1
    await loadLogs()
  } catch (err) {
    console.error('生成演示数据失败', err)
    ElMessage.error('生成演示数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 查看详情
 */
const onRowClick = (row: any) => {
  detailLog.value = row
  detailDrawer.value = true
}

// 初始化：加载数据
onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.admin-operation-logs {
  min-height: 100vh;
  background: #f5f7fa;
}

.filter-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  margin-top: 16px;
  text-align: right;
}

.detail-content {
  padding: 12px;
}

.user-agent {
  font-size: 12px;
  color: #666;
}

/* 超高风险整行高亮 */
.row-critical {
  background: linear-gradient(90deg, rgba(255,245,245,0.8), rgba(255,250,250,0.6));
  border-left: 4px solid #f56c6c;
}
.row-high {
  background: linear-gradient(90deg, rgba(255,250,240,0.8), rgba(255,252,240,0.6));
  border-left: 4px solid #e6a23c;
}

/* 超高风险标签闪烁效果 */
.risk-badge-critical {
  box-shadow: 0 0 8px rgba(245,108,108,0.8);
  animation: critical-blink 1.2s ease-in-out infinite;
}

@keyframes critical-blink {
  0% { transform: translateY(0); box-shadow: 0 0 6px rgba(245,108,108,0.6); }
  50% { transform: translateY(-1px); box-shadow: 0 0 12px rgba(255,82,82,0.95); }
  100% { transform: translateY(0); box-shadow: 0 0 6px rgba(245,108,108,0.6); }
}
</style>
