<template>
  <div class="admin-user-manage">
    <!-- 工具栏 -->
    <div class="tools-section">
      <el-card>
        <div style="display: flex; gap: 10px; flex-wrap: wrap;">
          <el-input v-model="query.keyword" placeholder="搜索用户名/邮箱" style="width: 200px; " clearable />
          <el-select v-model="query.status" placeholder="账号状态" style="width: 150px;" clearable>
            <el-option label="正常" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
          <el-select v-model="query.orderBy" placeholder="排序字段" style="width: 150px;" clearable>
            <el-option label="注册天数" value="registrationDays" />
            <el-option label="完成率" value="completionRate" />
            <el-option label="最后活跃时间" value="lastActiveTime" />
            <el-option label="使用时长" value="usageMinutes" />
          </el-select>
          <el-select v-model="query.orderType" placeholder="排序方式" style="width: 100px;">
            <el-option label="降序" value="desc" />
            <el-option label="升序" value="asc" />
          </el-select>
          <el-button type="primary" @click="loadUsers">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </el-card>
    </div>

    <!-- 用户列表 -->
    <el-card style="margin-top: 20px;">
      <el-table :data="users" stripe border v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        
        <el-table-column label="注册天数" width="100">
          <template #default="{ row }">
            <span>{{ row.registrationDays }} 天</span>
          </template>
        </el-table-column>
        
        <el-table-column label="完成率" width="100" sortable>
          <template #default="{ row }">
            <el-progress :percentage="safePercentage(row.completionRate)" :color="getCompletionColor(row.completionRate)" />
          </template>
        </el-table-column>
        
        <el-table-column label="任务统计" width="150">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">完成: {{ row.completedTaskCount }}</el-tag>
            <br />
            <el-tag type="warning" effect="plain">未完成: {{ row.uncompletedTaskCount }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="使用时长" width="120">
          <template #default="{ row }">
            <span>{{ row.usageMinutes ?? 0 }} 分钟</span>
          </template>
        </el-table-column>
        
        <el-table-column label="最后活跃时间" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatDate(row.lastActiveTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="账号状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="0"
              :inactive-value="1"
              @change="handleStatusChange(row)"
            />
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="info" @click="openAnalytics(row)">分析</el-button>
            <el-button size="small" type="warning" @click="resetPassword(row)">重置密码</el-button>
            <el-popconfirm title="确定禁用该用户?" confirm-button-text="确定" cancel-button-text="取消" @confirm="setStatus(row, false)">
              <template #reference>
                <el-button size="small" :type="row.status === 0 ? 'danger' : 'success'">
                  {{ row.status === 0 ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > query.size"
        :current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="onPageChange"
        style="margin-top: 20px; text-align: right;"
      />
    </el-card>

    <!-- 用户详情抽屉 -->
    <UserDetailDrawer v-if="selectedUser" :detail="selectedUser" @close="selectedUser = null" />

    <!-- 用户分析弹窗 -->
    <UserAnalyticsDialog v-if="analyticsUser" :user="analyticsUser" @close="analyticsUser = null" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserDetailDrawer from '@/components/admin/UserDetailDrawer.vue'
import UserAnalyticsDialog from '@/components/admin/UserAnalyticsDialog.vue'
import * as userApi from '@/api/admin/userManage'

const route = useRoute()

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  status: undefined as number | undefined,
  // 默认按使用时长降序，使用时长长的排在前面
  orderBy: 'usageMinutes' as string | undefined,
  orderType: 'desc'
})

const users = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const selectedUser = ref(null as any)
const analyticsUser = ref(null as any)

const loadUsers = async () => {
  loading.value = true
  try {
    const response = await userApi.getUserList(query as any)
    // 响应拦截器已提取data，直接访问 rows 和 total
    const resp: any = response as any
    users.value = resp?.rows || []
    total.value = resp?.total || 0
    // 如果后端返回分页元数据，使用后端值同步前端分页状态
    if (resp?.pageNum) query.page = resp.pageNum
    if (resp?.pageSize) query.size = resp.pageSize
  } catch (error: any) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败：' + (error?.message || '请稍后重试'))
    // 保持既往数据，不清空
    if (!users.value.length) {
      users.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

const onPageChange = (page: number) => {
  query.page = page
  loadUsers()
}

const resetFilters = () => {
  query.keyword = ''
  query.status = undefined
  query.orderBy = undefined
  query.orderType = 'desc'
  query.page = 1
  loadUsers()
}

const setStatus = async (row: any, active: boolean) => {
  const status = active ? 0 : 1
  try {
    await userApi.updateUserStatus(row.id, status)
    ElMessage.success(active ? '用户已启用' : '用户已禁用')
    await loadUsers()
  } catch (error) {
    ElMessage.error('更新状态失败')
  }
}

const handleStatusChange = (row: any) => {
  // row.status 已为 0 或 1
  setStatus(row, row.status === 0)
}

const resetPassword = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要重置该用户密码吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userApi.resetUserPassword(row.id, '123456')
    ElMessage.success('密码已重置为 123456')
  } catch (error) {
    console.error(error)
  }
}

const openDetail = async (row: any) => {
  try {
    const res: any = await userApi.getUserDetail(row.id)
    const data = (res && res.data) ? res.data : res
    selectedUser.value = data.user ? data : data // detail object with user, tasks, timeRecords
  } catch (error) {
    console.error('获取用户详情失败', error)
    ElMessage.error('获取用户详情失败')
  }
}

const openAnalytics = async (row: any) => {
  analyticsUser.value = row
}

const formatDate = (date: string | null) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}


const safePercentage = (val: any) => {
  const num = Number(val)
  return isNaN(num) ? 0 : Math.round(num)
}

const getCompletionColor = (rate: number) => {
  if (rate >= 80) return '#67C23A'
  if (rate >= 50) return '#409EFF'
  if (rate >= 20) return '#E6A23C'
  return '#F56C6C'
}

onMounted(() => {
  loadUsers()
})

// 监听路由变化，重新加载数据
watch(() => route.fullPath, () => {
  loadUsers()
})
</script>

<style scoped>
.admin-user-manage {
  padding: 20px;
}

.tools-section {
  margin-bottom: 20px;
}

.el-table {
  margin-top: 20px;
}
</style>
