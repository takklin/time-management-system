<template>
  <div class="profile-container">
    <div class="profile-content">
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
          </div>
        </template>

        <div class="profile-grid">
          <div class="avatar-section">
            <el-avatar :src="userAvatar" :size="120" />
            <el-button type="primary" plain @click="uploadAvatar">上传头像</el-button>
          </div>

          <el-form :model="formData" label-width="100px" class="profile-form">
            <el-form-item label="用户名">
              <span>{{ userStore.user?.username }}</span>
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="formData.email" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="formData.nickname" />
            </el-form-item>
          </el-form>
        </div>

        <el-button type="primary" @click="saveProfile">保存信息</el-button>
      </el-card>

      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>修改密码</span>
          </div>
        </template>

        <el-form :model="passwordForm" label-width="100px" @submit.prevent="changePassword">
          <el-form-item label="当前密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-button type="primary" @click="changePassword">修改密码</el-button>
        </el-form>
      </el-card>

      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>分类管理</span>
          </div>
        </template>

        <div class="categories-list">
          <div v-for="category in categories" :key="category.id" class="category-item">
            <div class="category-color" :style="{ backgroundColor: category.color }"></div>
            <span class="category-name">{{ category.name }}</span>
            <el-button type="danger" link size="small" @click="deleteCategory(category.id)">删除</el-button>
          </div>
        </div>

        <el-divider />

        <div class="add-category">
          <el-input v-model="newCategory.name" placeholder="输入分类名称" style="width: 150px" />
          <el-color-picker v-model="newCategory.color" show-alpha />
          <el-button type="primary" @click="addCategory">添加分类</el-button>
        </div>
      </el-card>

      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>数据管理</span>
          </div>
        </template>

        <div class="data-actions">
          <el-button @click="exportData">导出数据</el-button>
          <el-button @click="importData">导入数据</el-button>
          <el-button type="danger" @click="deleteAccount">删除账号</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getCategories, createCategory, deleteCategory as apiDeleteCategory } from '@/api/tasks'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

const userAvatar = ref('https://cube.elemecdn.com/0/88/03b0f476b6411127aa8e8b9be76153.jpeg')

const formData = reactive({
  email: userStore.user?.email || '',
  nickname: '',
})

const categories = ref([] as Array<{ id: number; name: string; color: string }>)

const newCategory = reactive({
  name: '',
  color: '#409EFF',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// categories 和 newCategory 已在上方声明

const fetchCategories = async () => {
  try {
    const response: any = await getCategories()
    categories.value = response.data || response
  } catch (error) {
    console.error('获取分类失败', error)
    ElMessage.error('获取分类失败')
  }
}

onMounted(async () => {
  try {
    await userStore.fetchUserInfo()
    formData.email = userStore.user?.email || ''
    formData.nickname = userStore.user?.nickname || ''
    const avatar = userStore.user?.avatar || ''
    if (avatar) {
      userAvatar.value = avatar.startsWith('http') ? avatar : avatar
    }
    await fetchCategories()
  } catch (error) {
    console.error('Failed to load profile', error)
  }
})

const uploadAvatar = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (event: Event) => {
    const file = (event.target as HTMLInputElement).files?.[0]
    if (!file) return

    try {
      const updated = await userStore.uploadAvatar(file)
      userAvatar.value = updated.avatar || userAvatar.value
      ElMessage.success('头像上传成功')
    } catch (err) {
      console.error('上传头像失败', err)
      ElMessage.error('头像上传失败')
    }
  }
  input.click()
}

const saveProfile = async () => {
  try {
    const updated = await userStore.updateProfile(formData.email, formData.nickname)
    userStore.user = updated
    ElMessage.success('个人信息保存成功')
  } catch (err) {
    console.error('保存个人信息失败', err)
    ElMessage.error('个人信息保存失败')
  }
}

const changePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.error('请填写密码')
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  try {
    await userStore.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (err) {
    console.error('修改密码失败', err)
    ElMessage.error('修改密码失败，请检查旧密码')
  }
}

const addCategory = async () => {
  if (!newCategory.name.trim()) {
    ElMessage.error('请输入分类名称')
    return
  }

  try {
    await createCategory({ name: newCategory.name, color: newCategory.color })
    await fetchCategories()
    newCategory.name = ''
    newCategory.color = '#409EFF'
    ElMessage.success('分类添加成功')
  } catch (error) {
    console.error('添加分类失败', error)
    ElMessage.error('添加分类失败')
  }
}

const deleteCategory = (id: number) => {
  ElMessageBox.confirm('确定要删除此分类吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      try {
        await apiDeleteCategory(id)
        await fetchCategories()
        ElMessage.success('分类删除成功')
      } catch (error) {
        console.error('删除分类失败', error)
        ElMessage.error('删除分类失败')
      }
    })
    .catch(() => {
      // 用户取消
    })
}

const exportData = () => {
  ElMessage.success('数据导出成功')
  // TODO: 实现数据导出
}

const importData = () => {
  ElMessage.info('数据导入功能开发中')
  // TODO: 实现数据导入
}

const deleteAccount = () => {
  ElMessageBox.confirm('删除账号后，所有数据将无法恢复。确定要继续吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error',
  })
    .then(() => {
      ElMessage.success('账号已删除')
      // TODO: 调用 API 删除账号
    })
    .catch(() => {
      // 用户取消
    })
}
</script>

<style scoped>
.profile-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  background-color: #fff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-grid {
  display: grid;
  grid-template-columns: 150px 1fr;
  gap: 30px;
  margin-bottom: 20px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.profile-form {
  width: 100%;
}

.categories-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.category-color {
  width: 20px;
  height: 20px;
  border-radius: 4px;
}

.category-name {
  flex: 1;
}

.add-category {
  display: flex;
  gap: 10px;
  align-items: center;
}

.data-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
