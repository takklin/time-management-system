<template>
  <div class="admin-user-manage">
    <div class="tools">
      <SearchBar v-model:value="query.keyword" placeholder="用户名/邮箱" @search="loadUsers" />
      <el-button type="primary" @click="loadUsers">刷新</el-button>
    </div>

    <el-table :data="users" stripe border v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="role" label="角色" width="120" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <UserStatusSwitch :value="row.deleted === 0" @change="(status: boolean) => setStatus(row, status)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <el-button size="small" type="warning" @click="resetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination :page="query.page" :page-size="query.size" :total="total" @change="onPageChange" />

    <UserDetailDialog v-if="detailUser" :user="detailUser" @close="detailUser = null" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import SearchBar from '@/components/common/SearchBar.vue'
import Pagination from '@/components/common/Pagination.vue'
import UserStatusSwitch from '@/components/admin/UserStatusSwitch.vue'
import UserDetailDialog from '@/components/admin/UserDetailDialog.vue'
import { useAdminUserManageStore } from '@/store/admin/userManage'

const store = useAdminUserManageStore()
const detailUser = ref(null as any)

const query = reactive({ page: 1, size: 10, keyword: '', status: undefined as number | undefined })

const users = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

const loadUsers = async () => {
  loading.value = true
  try {
    const { rows, total: t } = await store.loadUsers(query)
    users.value = rows
    total.value = t
  } finally {
    loading.value = false
  }
}

const onPageChange = async (page: number, size: number) => {
  query.page = page
  query.size = size
  await loadUsers()
}

const setStatus = async (row: any, active: boolean) => {
  const status = active ? 0 : 1
  await store.updateStatus(row.id, status)
  await loadUsers()
}

const resetPassword = async (row: any) => {
  await store.resetPassword(row.id)
  ElMessage.success('密码已重置为默认密码')
}

const openDetail = async (row: any) => {
  detailUser.value = await store.loadUserDetail(row.id)
}

onMounted(loadUsers)
</script>

<style scoped>
.tools { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
</style>
