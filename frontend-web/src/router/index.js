import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

// 路由配置
const routes = [
  // ============ 用户端路由 ============
  {
    path: '/',
    component: () => import('@/layouts/UserLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/user/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'drones',
        name: 'DroneList',
        component: () => import('@/views/user/DroneList.vue'),
        meta: { title: '设备浏览' }
      },
      {
        path: 'drones/:id',
        name: 'DroneDetail',
        component: () => import('@/views/user/DroneDetail.vue'),
        meta: { title: '设备详情' }
      },
      {
        path: 'orders',
        name: 'MyOrders',
        component: () => import('@/views/user/MyOrders.vue'),
        meta: { title: '我的订单', requireAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/user/OrderDetail.vue'),
        meta: { title: '订单详情', requireAuth: true }
      },
      {
        path: 'orders/:id/pay',
        name: 'OrderPay',
        component: () => import('@/views/user/OrderPay.vue'),
        meta: { title: '订单支付', requireAuth: true }
      },
      {
        path: 'orders/pending-pay',
        name: 'OrderPendingPay',
        component: () => import('@/views/user/OrderPendingPay.vue'),
        meta: { title: '待支付', requireAuth: true }
      },
      {
        path: 'orders/pending-ship',
        name: 'OrderPendingShip',
        component: () => import('@/views/user/OrderPendingShip.vue'),
        meta: { title: '待发货', requireAuth: true }
      },
      {
        path: 'orders/pending-receive',
        name: 'OrderPendingReceive',
        component: () => import('@/views/user/OrderPendingReceive.vue'),
        meta: { title: '待收货', requireAuth: true }
      },
      {
        path: 'orders/renting',
        name: 'OrderRenting',
        component: () => import('@/views/user/OrderRenting.vue'),
        meta: { title: '租赁中', requireAuth: true }
      },
      {
        path: 'orders/returned',
        name: 'OrderReturned',
        component: () => import('@/views/user/OrderReturned.vue'),
        meta: { title: '已归还', requireAuth: true }
      },
      {
        path: 'orders/canceled',
        name: 'OrderCanceled',
        component: () => import('@/views/user/OrderCanceled.vue'),
        meta: { title: '已取消', requireAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心', requireAuth: true }
      },
      {
        path: 'fault-report',
        name: 'FaultReport',
        component: () => import('@/views/user/FaultReport.vue'),
        meta: { title: '故障报修', requireAuth: true }
      },
      {
        path: 'airspace',
        name: 'AirspaceRecord',
        component: () => import('@/views/user/AirspaceRecord.vue'),
        meta: { title: '空域备案', requireAuth: true }
      }
    ]
  },

  // ============ 登录注册 ============
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', guest: true }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/auth/AdminLogin.vue'),
    meta: { title: '管理员登录', guest: true }
  },

  // ============ 管理端路由 ============
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requireAuth: true, requireAdmin: true },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '控制台' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'audits',
        name: 'AdminAudits',
        component: () => import('@/views/admin/AuditManagement.vue'),
        meta: { title: '资质审核' }
      },
      {
        path: 'drones',
        name: 'AdminDrones',
        component: () => import('@/views/admin/DroneManagement.vue'),
        meta: { title: '无人机管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/OrderManagement.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'fault-audit',
        name: 'AdminFaultAudit',
        component: () => import('@/views/admin/FaultAudit.vue'),
        meta: { title: '故障报修审核' }
      },
      {
        path: 'airspace-audit',
        name: 'AdminAirspaceAudit',
        component: () => import('@/views/admin/AirspaceAudit.vue'),
        meta: { title: '空域备案审核' }
      },
      {
        path: 'maintenance',
        name: 'AdminMaintenance',
        component: () => import('@/views/admin/MaintenanceManagement.vue'),
        meta: { title: '维保管理' }
      },
      {
        path: 'comments',
        name: 'AdminComments',
        component: () => import('@/views/admin/CommentManagement.vue'),
        meta: { title: '评价管理' }
      },
      {
        path: 'ai-chat-audit',
        name: 'AiChatAudit',
        component: () => import('@/views/admin/AiChatAudit.vue'),
        meta: { title: 'AI对话审计' }
      },
      {
        path: 'ai-management',
        name: 'AiManagement',
        component: () => import('@/views/admin/AiManagement.vue'),
        meta: { title: 'AI管理' }
      },
      {
        path: 'notifications',
        name: 'AdminNotifications',
        component: () => import('@/views/admin/NotificationManagement.vue'),
        meta: { title: '系统通知' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/admin/AdminProfile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },

  // ============ 404页面 ============
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  document.title = to.meta.title
    ? `${to.meta.title} - 无人机租赁系统`
    : '无人机租赁系统'

  if (authStore.token && !authStore.user) {
    authStore.fetchUserInfo().catch(() => {})
  }

  if (to.meta.guest && authStore.isLoggedIn) {
    return next(authStore.isAdmin ? '/admin' : '/')
  }

  if (to.meta.requireAuth && !authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  }

  if (to.meta.requireAdmin && !authStore.isAdmin) {
    ElMessage.error('无权访问管理后台')
    return next('/')
  }

  next()
})

export default router
