import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App.vue'
import router from './router'
import '@/assets/styles/main.css'
import alertListener from '@/plugins/alertListener'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(alertListener)

// 初始化时尝试加载用户信息
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
if (userStore.token) {
  userStore.fetchUserInfo().catch(() => {
    userStore.logout()
  })
}

app.mount('#app')
