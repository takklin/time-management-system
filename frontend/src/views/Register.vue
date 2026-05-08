<template>
  <div class="register-container auth-bg">
    <div class="register-box">
      <div class="register-header">
        <h1>创建账号</h1>
        <p>加入时间管理系统</p>
      </div>

      <!-- Stepper -->
      <div class="steps-row">
        <div :class="['step', step === 1 ? 'active' : '']">1. 账号信息</div>
        <div :class="['step', step === 2 ? 'active' : '']">2. 选择兴趣</div>
      </div>

      <el-form
        v-if="step === 1"
        ref="formRef"
        :model="form"
        :rules="rules"
        @submit.prevent="handleNextStep"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱地址"
            type="email"
            :prefix-icon="Message"
            size="large"
          />
        </el-form-item>

        <div class="password-grid">
          <el-form-item prop="password" class="password-field">
            <el-input
              v-model="form.password"
              placeholder="密码"
              type="password"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <div class="pw-strength-box" v-if="form.password">
            <div class="strength-text">{{ pwText }}</div>
            <div class="strength-track-inline">
              <div class="strength-fill" :class="pwLevelClass" :style="{ width: pwPercent + '%' }"></div>
            </div>
          </div>

          <el-form-item prop="confirmPassword" class="confirm-field">
            <el-input
              v-model="form.confirmPassword"
              placeholder="确认密码"
              type="password"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
        </div>

        <el-form-item>
          <div class="action-row">
            <el-button class="primary-action" @click="handleNextStep" :loading="loading">
              下一步
            </el-button>
          </div>
        </el-form-item>

        <div class="form-footer">
          <router-link to="/login" class="login-link">已有账号？登录</router-link>
        </div>
      </el-form>

      <!-- Interests selection -->
      <div v-if="step === 2" class="interests-panel">
        <p>选择你的兴趣（用于初始化分类）</p>
        <div class="interest-list">
          <div
            v-for="cat in interestOptions"
            :key="cat.name"
            class="interest-item"
            :class="{ selected: selectedInterests.includes(cat.name) }"
            :style="selectedInterests.includes(cat.name) ? { backgroundColor: cat.color, color: '#fff', borderColor: cat.color } : { borderColor: cat.color + '33' }"
            @click="toggleInterest(cat.name)"
          >
            {{ cat.name }}
          </div>
        </div>

        <div class="selected-summary" v-if="selectedInterests.length > 0">
          已选分类：
          <span
            v-for="name in selectedInterests"
            :key="name"
            class="selected-badge"
            :style="{ backgroundColor: (interestOptions.find(i => i.name === name) || {}).color || '#ddd', color: '#fff' }"
          >
            {{ name }}
          </span>
        </div>

        <div class="actions">
          <el-button @click="step = 1">上一步</el-button>
          <el-button link @click="handleSkip">跳过此步</el-button>
          <el-button type="primary" @click="handleRegister()" :loading="loading">完成并注册</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { useTaskStore } from '@/store/task'

const router = useRouter()
const userStore = useUserStore()
const taskStore = useTaskStore()

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const step = ref(1)
const interestOptions = [
  { name: '学习', color: '#5B8FF9' },
  { name: '工作', color: '#FF7A45' },
  { name: '生活', color: '#73D13D' },
  { name: '健身', color: '#13C2C2' },
  { name: '阅读', color: '#9254DE' },
  { name: '娱乐', color: '#FFBB96' },
  { name: '健康', color: '#F5222D' },
  { name: '社交', color: '#FF85C0' },
]
const selectedInterests = ref<string[]>([])

const loading = ref(false)
const formRef = ref()

const validatePassword = (_rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码长度至少6位'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (_rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请确认密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
}

const passwordScore = (pw: string) => {
  let score = 0
  if (pw.length >= 8) score++
  if (pw.length >= 12) score++
  if (/[A-Z]/.test(pw)) score++
  if (/[0-9]/.test(pw)) score++
  if (/[^A-Za-z0-9]/.test(pw)) score++
  return Math.min(score, 5)
}

const pwScore = computed(() => passwordScore(form.password))
const pwPercent = computed(() => (pwScore.value / 5) * 100)
const pwText = computed(() => {
  if (!form.password) return ''
  if (pwScore.value <= 1) return '弱'
  if (pwScore.value <= 3) return '中'
  return '强'
})
const pwLevelClass = computed(() => {
  if (pwScore.value <= 1) return 'weak'
  if (pwScore.value <= 3) return 'medium'
  return 'strong'
})

// 不再显示分项建议，相关检测移除以减少未使用代码

const handleNextStep = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid: boolean) => {
    if (valid) {
      // 要求密码至少中等
      if (pwScore.value <= 1) {
        ElMessage.error('请设置更强的密码（至少中等强度）')
        return
      }
      step.value = 2
    }
  })
}

const toggleInterest = (catName: string) => {
  const idx = selectedInterests.value.indexOf(catName)
  if (idx === -1) selectedInterests.value.push(catName)
  else selectedInterests.value.splice(idx, 1)
}

const handleRegister = async (skip = false) => {
  loading.value = true
  try {
    // 准备要提交给后端的分类（包含颜色）
    const categories = selectedInterests.value.map(name => {
      const opt = interestOptions.find(i => i.name === name)
      return { name, color: opt?.color }
    })

    if (skip) {
      await userStore.registerUser(form.username, form.email, form.password)
    } else {
      await userStore.registerUser(form.username, form.email, form.password, categories)
    }

    // 刷新本地分类缓存，确保个人中心/任务页能立即看到分类
    try { await taskStore.fetchCategories() } catch (e) { console.warn('refresh categories failed', e) }

    ElMessage.success('注册成功，正在跳转到仪表盘')
    router.push('/dashboard')
  } catch (error: any) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}

const handleSkip = () => {
  handleRegister(true)
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
}

.auth-bg {
  background: linear-gradient(90deg, #F7D6C1 0%, #E0AFA0 20%, #D6B77A 40%, #9BB7D4 60%, #C39BD4 80%, #F6D6A9 100%);
  background-size: 200% 100%;
  animation: gradientShift 12s ease infinite;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.register-box {
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

/* Ensure header and action buttons share the same centerline */
.register-box { display:flex; flex-direction:column; align-items:center }
.register-box form, .register-box .el-form { width:100% }
.register-header, .action-row, .form-footer { width:100%; display:flex; justify-content:center; align-items:center }

.steps-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.step {
  flex: 1;
  padding: 8px 12px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #C39BD4; /* 非激活时也使用复古色调 */
  text-align: center;
}
.step.active {
  background: linear-gradient(90deg, #C39BD4 0%, #9BB7D4 60%);
  color: white;
}

.password-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  grid-template-rows: auto auto;
  gap: 10px 12px;
  align-items: center;
  margin-top: 8px;
}
.password-field { grid-column: 1; grid-row: 1; }
.pw-strength-box { grid-column: 2; grid-row: 1; display:flex; flex-direction:column; align-items:center; }
.confirm-field { grid-column: 1; grid-row: 2; }
.password-grid .el-input { width: 100%; }
.pw-strength-inline, .pw-strength-box { width: 100%; display: flex; flex-direction: column; align-items: center; gap: 6px; }
.strength-track-inline, .strength-track {
  width: 100%;
  height: 12px;
  background: #f1f4f9;
  border-radius: 8px;
  overflow: hidden;
}
.strength-fill {
  height: 100%;
  width: 0;
  transition: width 0.25s ease;
  border-radius: 8px;
}
.strength-fill.weak { background: linear-gradient(90deg,#f56c6c,#ff8a80); }
.strength-fill.medium { background: linear-gradient(90deg,#e6a23c,#ffbf69); }
.strength-fill.strong { background: linear-gradient(90deg,#67c23a,#9be564); }
.strength-text { font-size: 13px; color: #444; font-weight: 600; }

.interests-panel { padding-top: 8px; }
.interest-list { display: flex; gap: 8px; flex-wrap: wrap; margin: 12px 0; }
.interest-item {
  padding: 8px 14px;
  border-radius: 20px;
  border: 1px solid #eee;
  cursor: pointer;
  user-select: none;
  transition: all 0.12s ease;
  font-size: 14px;
  background: #fff;
}
.interest-item.selected { box-shadow: 0 6px 18px rgba(0,0,0,0.08); transform: translateY(-2px); }
.selected-summary { margin-top: 8px; }
.selected-badge { display: inline-block; padding: 4px 8px; border-radius: 12px; margin-right: 6px; font-size: 12px; }

.actions { display:flex; justify-content:center; gap:12px; margin-top: 12px }

.register-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 14px; /* 减小间距，使副标题紧贴标题下方 */
}

.register-header h1 {
  font-size: 30px;
  color: #C39BD4;
  margin: 0;
}

.register-header p {
  color: #9BB7D4;
  font-size: 14px;
  margin: 4px 0 0 0; /* 直接在标题正下方 */
}

.form-footer {
  text-align: center;
  font-size: 12px;
}

.login-link {
  color: #D6B77A;
  text-decoration: none;
  cursor: pointer;
}

.login-link:hover {
  text-decoration: underline;
}

.action-row { display:flex; justify-content:center; }
.primary-action {
  width: 260px;
  height: 56px;
  font-size: 18px;
  border-radius: 10px;
  color: #fff !important;
  background: linear-gradient(90deg, #C39BD4 0%, #D6B77A 60%) !important;
  box-shadow: 0 10px 30px rgba(195,155,212,0.18);
  border: none !important;
}
.primary-action:active { transform: translateY(1px); }
</style>
