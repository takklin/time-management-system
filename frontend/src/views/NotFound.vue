<template>
  <div class="not-found">
    <div class="not-found-content">
      <h1>404</h1>
      <p>页面未找到</p>
      <router-link to="/dashboard" class="back-link">
        <el-button type="primary">返回首页</el-button>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.not-found-content {
  text-align: center;
  color: white;
}

.not-found-content h1 {
  font-size: 120px;
  margin: 0;
  font-weight: bold;
}

.not-found-content p {
  font-size: 24px;
  margin: 20px 0 40px 0;
}

.back-link {
  text-decoration: none;
}
</style>
