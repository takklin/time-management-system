<template>
  <div class="admin-backup">
    <h2>数据备份与恢复</h2>
    <el-button type="primary">导出用户数据</el-button>
    <el-button>导出任务数据</el-button>
    <el-button>导出时间记录</el-button>
    <!-- 可扩展导入/恢复功能 -->
  </div>
</template>
<script setup lang="ts">
// TODO: 调用后端接口实现数据导出
</script>
<style scoped>
.admin-backup { padding: 24px; }
</style>
