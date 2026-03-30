import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('@/views/Tasks.vue'),
        meta: { title: '任务管理' },
      },
      {
        path: 'schedules',
        name: 'Schedules',
        component: () => import('@/views/Schedules.vue'),
        meta: { title: '日程安排' },
      },
      {
        path: 'todos',
        name: 'Todos',
        component: () => import('@/views/Todos.vue'),
        meta: { title: '待办事项' },
      },
      {
        path: 'time-records',
        name: 'TimeRecords',
        component: () => import('@/views/TimeRecords.vue'),
        meta: { title: '时间记录' },
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '数据统计' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' },
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['admin'] },
    children: [
      {
        path: 'users',
        name: 'AdminUserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', roles: ['admin'] },
      },
      {
        path: 'system',
        name: 'AdminSystemStat',
        component: () => import('@/views/admin/SystemStat.vue'),
        meta: { title: '系统统计', roles: ['admin'] },
      },
      {
        path: 'logs',
        name: 'AdminOperationLogs',
        component: () => import('@/views/admin/OperationLogs.vue'),
        meta: { title: '操作日志', roles: ['admin'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局路由守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false)

  console.debug('[ROUTER] to', to.fullPath, 'token', userStore.token, 'user', userStore.user)

  if (requiresAuth) {
    if (!userStore.token) {
      return next('/login')
    }

    if (!userStore.user) {
      try {
        await userStore.fetchUserInfo()
      } catch (err) {
        console.warn('[ROUTER] fetchUserInfo failed', err)
        userStore.logout()
        return next('/login')
      }
    }

    const role = userStore.user?.role
    const requiredRoles = to.meta.roles as string[] | undefined
    if (requiredRoles && !requiredRoles.includes(role || '')) {
      return next('/dashboard')
    }

    return next()
  }

  if ((to.path === '/login' || to.path === '/register') && userStore.token) {
    return next('/dashboard')
  }

  next()
})

export default router
