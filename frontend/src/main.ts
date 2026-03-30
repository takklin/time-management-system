import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App.vue'
import router from './router'
import '@/assets/styles/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 初始化时尝试加载用户信息
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
if (userStore.token) {
  userStore.fetchUserInfo().catch(() => {
    userStore.logout()
  })
}

app.mount('#app')
