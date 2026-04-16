<template>
  <div class="admin-config">
    <h2>系统配置管理</h2>
    <el-form label-width="120px">
      <el-form-item label="任务默认提醒时间">
        <el-input placeholder="如 09:00"></el-input>
      </el-form-item>
      <el-form-item label="单次计时最大时长">
        <el-input placeholder="如 120 分钟"></el-input>
      </el-form-item>
      <el-form-item label="是否开放注册">
        <el-switch></el-switch>
      </el-form-item>
      <el-form-item>
        <el-button type="primary">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
<script setup lang="ts">
// TODO: 调用后端接口获取/保存系统配置
</script>
<style scoped>
.admin-config { padding: 24px; }
</style>
