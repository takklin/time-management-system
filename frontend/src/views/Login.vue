<template>
  <div class="login-container">
    <div class="login-box">
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
          <el-button type="primary" size="large" @click="handleLogin" :loading="loading" block>
            登录
          </el-button>
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
        await userStore.loginUser(form.username, form.password)
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch (error: any) {
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  font-size: 28px;
  color: #667eea;
  margin-bottom: 10px;
}

.login-header p {
  color: #999;
  font-size: 14px;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.register-link {
  color: #667eea;
  text-decoration: none;
  cursor: pointer;
}

.register-link:hover {
  text-decoration: underline;
}
</style>
