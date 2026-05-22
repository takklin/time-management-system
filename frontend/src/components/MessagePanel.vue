<template>
  <div class="message-panel">
    <div style="display:flex;gap:8px;margin-bottom:8px;align-items:center;">
      <el-button size="small" type="primary" @click="markAllAsRead" :loading="loading">全部已读</el-button>
      <el-button size="small" @click="refresh" :loading="loading">刷新</el-button>
      <div style="flex:1"></div>
      <div>未读: <strong>{{ messageStore.unreadCount }}</strong></div>
    </div>

    <el-table v-loading="loading" :data="messageStore.messages" style="width:100%" row-key="id">
      <el-table-column prop="title" label="标题" min-width="180">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:8px;">
            <div>
              <div style="font-weight:600">{{ row.title || '(无标题)' }}</div>
              <div style="font-size:12px;color:#666">{{ row.fromAdminId ? '来自管理员' : '' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="content" label="内容" min-width="260">
        <template #default="{ row }">
          <div style="white-space:normal">{{ row.content }}</div>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">
          <div>{{ formatDate(row.createdAt) }}</div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="!row.isRead" type="primary" size="small" @click="markRead(row)">标记已读</el-button>
          <el-button type="text" size="small" @click="deleteMsg(row)" style="margin-left:8px">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && (!messageStore.messages || messageStore.messages.length === 0)" style="padding:16px;text-align:center;color:#888">暂无消息</div>

    <div style="margin-top:10px;display:flex;gap:8px;align-items:center;">
      <div style="flex:1"></div>
      <el-pagination background layout="prev, pager, next" :page-size="pageSize" :current-page.sync="page" :total="total" @current-change="onPageChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMessageStore } from '@/store/message'

const messageStore = useMessageStore()
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = 0

import dayjs from 'dayjs'

function formatDate(v: any) { if (!v) return ''; try { return dayjs(v).format('YYYY-MM-DD HH:mm:ss') } catch (e) { return v } }

async function refresh() {
  loading.value = true
  try { await messageStore.fetchMessages({ page: page.value, size: pageSize }) } finally { loading.value = false }
}

async function markRead(row: any) {
  loading.value = true
  try { await messageStore.markRead(row.id) } finally { loading.value = false }
}

async function deleteMsg(row: any) {
  loading.value = true
  try { await messageStore.removeMessage(row.id) } finally { loading.value = false }
}

function onPageChange(p: number) { page.value = p; refresh() }

async function markAllAsRead() {
  loading.value = true
  try {
    for (const m of messageStore.messages.filter((x: any) => !x.isRead)) {
      try { await messageStore.markRead(m.id) } catch (e) {}
    }
  } finally { loading.value = false }
}

onMounted(() => { refresh() })
</script>

<style scoped>
.message-panel { }
</style>
