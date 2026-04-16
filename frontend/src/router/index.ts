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
    path: '/dashboard',
    component: () => import('@/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
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
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('@/views/admin/AdminDashboard.vue'),
          meta: { title: '管理员仪表盘', roles: ['admin'] },
        },
        {
          path: 'backup',
          name: 'AdminBackup',
          component: () => import('@/views/admin/AdminBackup.vue'),
          meta: { title: '数据备份', roles: ['admin'] },
        },
        {
          path: 'config',
          name: 'AdminConfig',
          component: () => import('@/views/admin/AdminConfig.vue'),
          meta: { title: '系统配置', roles: ['admin'] },
        },
        {
          path: 'ai-assistant',
          name: 'AdminAIAssistant',
          component: () => import('@/views/admin/AIAssistant.vue'),
          meta: { title: 'AI 智能助手', roles: ['admin'] },
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

    const role = userStore.user?.role?.toLowerCase() || ''

    // 角色路径检查：防止用户访问不符合其角色的路由
    // admin 用户不应该访问 /dashboard（用户端）
    if (role === 'admin' && (to.path === '/' || to.path.startsWith('/dashboard'))) {
      console.debug('[ROUTER] Admin user trying to access user dashboard, redirecting to admin dashboard')
      return next('/admin/dashboard')
    }

    // 普通用户不应该访问 /admin（已由权限检查处理，但这里加强）
    if (role !== 'admin' && to.path.startsWith('/admin')) {
      console.debug('[ROUTER] Non-admin user trying to access admin panel, redirecting to dashboard')
      return next('/dashboard')
    }

    // 权限检查：检查路由是否要求特定角色
    const requiredRoles = to.meta.roles as string[] | undefined
    if (requiredRoles && !requiredRoles.includes(role)) {
      console.debug('[ROUTER] User lacks required roles for this route')
      return next(role === 'admin' ? '/admin/dashboard' : '/dashboard')
    }

    return next()
  }

  // 未登录用户访问登录/注册/主页相关处理
  if ((to.path === '/' || to.path === '/login' || to.path === '/register') && userStore.token) {
    // 已登录的用户访问这些路径时，根据角色重定向
    const role = userStore.user?.role?.toLowerCase() || ''
    const redirectPath = role === 'admin' ? '/admin/dashboard' : '/dashboard'
    console.debug('[ROUTER] Logged-in user accessing public path, redirecting to:', redirectPath)
    return next(redirectPath)
  }

  next()
})

export default router
