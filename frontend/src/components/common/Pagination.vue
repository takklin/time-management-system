<template>
  <el-pagination
    background
    layout="prev, pager, next, jumper, ->, total"
    :page-size="pageSize"
    :current-page="page"
    :total="total"
    @current-change="handlePageChange"
    @size-change="handleSizeChange"
  />
</template>

<script setup lang="ts">
const props = defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 }
})
const emit = defineEmits(['change'])

const handlePageChange = (current: number) => {
  emit('change', current, props.pageSize)
}

const handleSizeChange = (size: number) => {
  emit('change', 1, size)
}
</script>
