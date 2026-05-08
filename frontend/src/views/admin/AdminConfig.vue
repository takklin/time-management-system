<template>
  <div class="admin-config">
    <h2>系统配置</h2>

    <el-card>
      <div style="max-width:900px">
        <el-form :model="form" label-width="240px">
          <el-form-item label="是否允许新用户注册">
            <el-switch v-model="form.allow_registration" active-text="允许" inactive-text="禁止" />
          </el-form-item>

          <el-form-item label="任务截止前默认提醒（分钟）">
            <el-input-number v-model="form.default_task_reminder_minutes" :min="0" />
          </el-form-item>

          <el-form-item label="单次计时最大分钟数">
            <el-input-number v-model="form.max_timer_minutes" :min="1" />
          </el-form-item>

          <el-form-item label="操作日志保留天数">
            <el-input-number v-model="form.log_retention_days" :min="1" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
            <el-button @click="onResetDefaults" style="margin-left:12px">恢复为默认值</el-button>
          </el-form-item>
        </el-form>

        <!-- 设计说明已移除，保持界面简洁 -->
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as api from '@/api/admin/config'
import { ElMessage } from 'element-plus'

const saving = ref(false)

const form = ref({
  allow_registration: true,
  default_task_reminder_minutes: 30,
  max_timer_minutes: 480,
  log_retention_days: 90,
})

const DEFAULTS: Record<string, any> = {
  allow_registration: true,
  default_task_reminder_minutes: 30,
  max_timer_minutes: 480,
  log_retention_days: 90,
}

async function loadConfigs() {
  try {
    const res: any = await api.listConfigs()
    if (Array.isArray(res)) {
      res.forEach((c: any) => {
        const k = c.configKey
        const v = c.configValue
        if (k === 'allow_registration') form.value.allow_registration = (String(v) === 'true')
        if (k === 'default_task_reminder_minutes') form.value.default_task_reminder_minutes = Number(v)
        if (k === 'max_timer_minutes') form.value.max_timer_minutes = Number(v)
        if (k === 'log_retention_days') form.value.log_retention_days = Number(v)
      })
    }
  } catch (e) {
    console.error(e)
  }
}

async function onSave() {
  saving.value = true
  try {
    const ops = []
    ops.push(api.updateConfig('allow_registration', String(form.value.allow_registration)))
    ops.push(api.updateConfig('default_task_reminder_minutes', String(form.value.default_task_reminder_minutes)))
    ops.push(api.updateConfig('max_timer_minutes', String(form.value.max_timer_minutes)))
    ops.push(api.updateConfig('log_retention_days', String(form.value.log_retention_days)))

    await Promise.all(ops)
    ElMessage.success('配置已保存')
  } catch (err: any) {
    ElMessage.error('保存失败: ' + (err.message || err))
  } finally { saving.value = false }
}

async function onResetDefaults() {
  try {
    form.value = { ...DEFAULTS }
    await onSave()
    ElMessage.success('已恢复默认值并保存')
  } catch (e) {
    ElMessage.error('恢复默认值失败')
  }
}

onMounted(() => { loadConfigs() })
</script>

<style scoped>
.admin-config { padding: 24px }
</style>

