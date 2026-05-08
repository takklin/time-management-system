<template>
  <div class="login-container auth-bg">
    <div class="login-box" :class="{ shake: shake }">
      <div class="login-header">
        <h1>时间管理系统</h1>
        <p>欢迎登录</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            placeholder="密码"
            type="password"
            :prefix-icon="Lock"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <div class="action-row">
            <el-button class="primary-action" @click="handleLogin" :loading="loading">
              登录
            </el-button>
          </div>
        </el-form-item>

        <div class="form-footer">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <router-link to="/register" class="register-link">注册账号</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
})

const rememberMe = ref(false)
const loading = ref(false)
const formRef = ref()
const shake = ref(false)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        const res = await userStore.loginUser(form.username, form.password)
        ElMessage.success('登录成功')
        const role = userStore.user?.role || res?.user?.role || ''
        if (role === 'admin') {
          router.push('/admin/dashboard')
        } else {
          router.push('/dashboard')
        }
      } catch (error: any) {
        // 触发表单抖动以提示错误
        shake.value = true
        setTimeout(() => { shake.value = false }, 700)
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
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

.login-box {
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

/* Ensure header and action buttons share the same centerline */
.login-box { display: flex; flex-direction: column; align-items: center }
.login-box form, .login-box .el-form { width: 100% }
.login-header, .action-row { width: 100%; display:flex; justify-content:center; align-items:center }

.login-box.shake {
  animation: shake 0.7s cubic-bezier(.36,.07,.19,.97) both;
}

@keyframes shake {
  10%, 90% { transform: translateX(-1px); }
  20%, 80% { transform: translateX(2px); }
  30%, 50%, 70% { transform: translateX(-4px); }
  40%, 60% { transform: translateX(4px); }
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 14px; /* 更紧凑，副标题紧贴标题下方 */
}

.login-header h1 {
  font-size: 30px;
  color: #C39BD4; /* 复古色 - 标题 */
  margin: 0; /* 清除默认 margin，确保副标题在正下方 */
}

.login-header p {
  color: #9BB7D4; /* 副标题复古调 */
  font-size: 14px;
  margin: 4px 0 0 0; /* 直接在标题正下方 */
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.register-link {
  color: #D6B77A; /* 链接使用和谐复古色 */
  text-decoration: none;
  cursor: pointer;
}

.register-link:hover {
  text-decoration: underline;
}

.action-row { display:flex; justify-content:center; }
.primary-action {
  width: 260px;
  height: 56px;
  font-size: 18px;
  border-radius: 10px;
  color: #fff !important;
  background: linear-gradient(90deg, #C39BD4 0%, #9BB7D4 60%) !important;
  box-shadow: 0 10px 30px rgba(195,155,212,0.18);
  border: none !important;
}
.primary-action:active { transform: translateY(1px); }
</style>
