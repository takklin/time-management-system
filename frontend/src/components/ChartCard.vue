<template>
  <div class="chart-card">
    <div class="card-header">
      <h3>{{ title }}</h3>
      <slot name="actions"></slot>
    </div>
    <div class="card-content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T">
defineProps<{
  title: string
}>()
</script>

<style scoped>
.chart-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.card-content {
  min-height: 300px;
}

/* 当卡片包含今日待办时，使用淡红色框提示 */
.chart-card.today-has-tasks {
  border: 1px solid rgba(245,108,108,0.12);
}

/* 空状态时让占位内容居中，并适当放大 empty 图像 */
.chart-card.today-empty .card-content {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
}
.chart-card.today-empty .card-content .el-empty {
  width: 100%;
  max-width: 320px;
}
.chart-card.today-empty .card-content .el-empty__image img {
  width: 140px;
  height: 140px;
}
</style>
