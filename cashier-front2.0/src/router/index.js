import { createRouter, createWebHistory } from 'vue-router'
import { useCookies } from '@vueuse/integrations/useCookies'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/login/LoginView.vue'), // 你自己的登录页
      meta: { noAuth: true },
    },

    // 管理员
    {
      path: '/admin',
      component: () => import('@/views/Admin.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' },
      redirect: '/admin/home',
      children: [
        { path: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
        {
          path: 'pos',
          component: () => import('@/views/pos/PosView.vue'),
          meta: { title: '收银点餐' },
        },
        {
          path: 'orders',
          component: () => import('@/views/order/OrderList.vue'),
          meta: { title: '订单查询' },
        },
        {
          path: 'dishes',
          component: () => import('@/views/dish/DishList.vue'),
          meta: { title: '菜品管理' },
        },
        {
          path: 'categories',
          component: () => import('@/views/dish/CategoryList.vue'),
          meta: { title: '分类管理' },
        },
        {
          path: 'tables',
          component: () => import('@/views/table/TableList.vue'),
          meta: { title: '餐桌管理' },
        },

        {
          path: 'stores',
          component: () => import('@/views/store/StoreList.vue'),
          meta: { title: '门店管理' },
        },
        {
          path: 'users',
          component: () => import('@/views/user/UserList.vue'),
          meta: { title: '员工管理' },
        },
        {
          path: 'roles',
          component: () => import('@/views/role/RoleList.vue'),
          meta: { title: '角色管理' },
        },
        {
          path: 'report',
          component: () => import('@/views/report/ReportView.vue'),
          meta: { title: '营业统计' },
        },
        {
          path: 'ai',
          component: () => import('@/views/ai/AISuggestView.vue'),
          meta: { title: 'AI预制建议' },
        },
        {
          path: 'bigscreen',
          component: () => import('@/views/dashboard/BigScreen.vue'),
          meta: { title: '实时大屏', fullscreen: true },
        },
        {
          path: 'queue',
          component: () => import('@/views/queue/WaitingManage.vue'),
          meta: { title: '等位管理' },
        },
        {
          path: 'queue-board',
          component: () => import('@/views/queue/WaitingBoard.vue'),
          meta: { title: '等位大屏', fullscreen: true },
        },
        {
          path: 'logs/login',
          component: () => import('@/views/log/LoginLogList.vue'),
          meta: { title: '登录日志' },
        },
        {
          path: 'logs/operation',
          component: () => import('@/views/log/OperationLogList.vue'),
          meta: { title: '操作日志' },
        },
        {
          path: 'logs/order-item',
          component: () => import('@/views/log/OrderItemLogList.vue'),
          meta: { title: '菜品流水' },
        },
        {
          path: 'logs/changelog',
          component: () => import('@/views/log/ChangelogList.vue'),
          meta: { title: '更新日志' },
        },

        {
          path: 'materials',
          component: () => import('@/views/material/MaterialList.vue'),
          meta: { title: '原材料管理' },
        },
        {
          path: 'purchases',
          component: () => import('@/views/purchase/PurchaseList.vue'),
          meta: { title: '采购入库' },
        },
        {
          path: 'inventory',
          component: () => import('@/views/inventory/InventoryList.vue'),
          meta: { title: '库存查询' },
        },
        {
          path: 'recipes',
          component: () => import('@/views/recipe/RecipeList.vue'),
          meta: { title: '菜品配方' },
        },
        {
          path: 'consumptions',
          component: () => import('@/views/consumption/ConsumptionList.vue'),
          meta: { title: '原材料消耗' },
        },
        {
          path: 'fixed-costs',
          component: () => import('@/views/fixed-cost/FixedCostList.vue'),
          meta: { title: '固定成本' },
        },
        {
          path: 'members',
          component: () => import('@/views/member/MemberList.vue'),
          meta: { title: '会员管理' },
        },
        {
          path: 'sys-config',
          component: () => import('@/views/config/SysConfigView.vue'),
          meta: { title: '系统配置' },
        },
        {
          path: 'member-levels',
          component: () => import('@/views/member/MemberLevelConfig.vue'),
          meta: { title: '会员等级' },
        },
        {
          path: 'member-points',
          component: () => import('@/views/member/PointLogList.vue'),
          meta: { title: '积分流水' },
        },
      ],
    },

    // 店长
    {
      path: '/manager',
      component: () => import('@/views/Manager.vue'),
      meta: { requiresAuth: true, role: 'STORE_ADMIN' },
      redirect: '/manager/home',
      children: [
        { path: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
        {
          path: 'pos',
          component: () => import('@/views/pos/PosView.vue'),
          meta: { title: '收银点餐' },
        },
        {
          path: 'orders',
          component: () => import('@/views/order/OrderList.vue'),
          meta: { title: '订单查询' },
        },
        {
          path: 'dishes',
          component: () => import('@/views/dish/DishList.vue'),
          meta: { title: '菜品管理' },
        },
        {
          path: 'categories',
          component: () => import('@/views/dish/CategoryList.vue'),
          meta: { title: '分类管理' },
        },
        {
          path: 'tables',
          component: () => import('@/views/table/TableList.vue'),
          meta: { title: '餐桌管理' },
        },
        {
          path: 'stores',
          component: () => import('@/views/store/StoreDetail.vue'),
          meta: { title: '门店信息' },
        },
        {
          path: 'users',
          component: () => import('@/views/user/UserList.vue'),
          meta: { title: '员工管理' },
        },
        {
          path: 'materials',
          component: () => import('@/views/material/MaterialList.vue'),
          meta: { title: '原材料管理' },
        },
        {
          path: 'purchases',
          component: () => import('@/views/purchase/PurchaseList.vue'),
          meta: { title: '采购入库' },
        },
        {
          path: 'inventory',
          component: () => import('@/views/inventory/InventoryList.vue'),
          meta: { title: '库存查询' },
        },
        {
          path: 'recipes',
          component: () => import('@/views/recipe/RecipeList.vue'),
          meta: { title: '菜品配方' },
        },
        {
          path: 'consumptions',
          component: () => import('@/views/consumption/ConsumptionList.vue'),
          meta: { title: '原料消耗' },
        },
        {
          path: 'fixed-costs',
          component: () => import('@/views/fixed-cost/FixedCostList.vue'),
          meta: { title: '固定成本' },
        },
        {
          path: 'report',
          component: () => import('@/views/report/ReportView.vue'),
          meta: { title: '营业统计' },
        },
        {
          path: 'ai',
          component: () => import('@/views/ai/AISuggestView.vue'),
          meta: { title: 'AI预制建议' },
        },
        {
          path: 'bigscreen',
          component: () => import('@/views/dashboard/BigScreen.vue'),
          meta: { title: '实时大屏', fullscreen: true },
        },
        {
          path: 'queue',
          component: () => import('@/views/queue/WaitingManage.vue'),
          meta: { title: '等位管理' },
        },
        {
          path: 'queue-board',
          component: () => import('@/views/queue/WaitingBoard.vue'),
          meta: { title: '等位大屏', fullscreen: true },
        },
        {
          path: 'logs/login',
          component: () => import('@/views/log/LoginLogList.vue'),
          meta: { title: '登录日志' },
        },
        {
          path: 'logs/operation',
          component: () => import('@/views/log/OperationLogList.vue'),
          meta: { title: '操作日志' },
        },
        {
          path: 'logs/order-item',
          component: () => import('@/views/log/OrderItemLogList.vue'),
          meta: { title: '菜品流水' },
        },
        {
          path: 'logs/changelog',
          component: () => import('@/views/log/ChangelogList.vue'),
          meta: { title: '更新日志' },
        },
      ],
    },

    // 收银员
    {
      path: '/cashier',
      component: () => import('@/views/Cashier.vue'),
      meta: { requiresAuth: true, role: 'CASHIER' },
      redirect: '/cashier/home',
      children: [
        { path: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
        {
          path: 'pos',
          component: () => import('@/views/pos/PosView.vue'),
          meta: { title: '收银点餐' },
        },
        {
          path: 'orders',
          component: () => import('@/views/order/OrderList.vue'),
          meta: { title: '订单查询' },
        },
        {
          path: 'bigscreen',
          component: () => import('@/views/dashboard/BigScreen.vue'),
          meta: { title: '实时大屏', fullscreen: true },
        },
        {
          path: 'logs/changelog',
          component: () => import('@/views/log/ChangelogList.vue'),
          meta: { title: '更新日志' },
        },
      ],
    },

    // 服务员
    {
      path: '/waiter',
      component: () => import('@/views/Waiter.vue'),
      meta: { requiresAuth: true, role: 'WAITER' },
      redirect: '/waiter/home',
      children: [
        { path: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
        {
          path: 'pos',
          component: () => import('@/views/pos/PosView.vue'),
          meta: { title: '点餐' },
        },
        {
          path: 'orders',
          component: () => import('@/views/order/OrderList.vue'),
          meta: { title: '订单查询' },
        },
        {
          path: 'kds',
          component: () => import('@/views/kds/WaiterKDS.vue'),
          meta: { title: '服务操控台' },
        },
        {
          path: 'queue',
          component: () => import('@/views/queue/WaitingManage.vue'),
          meta: { title: '等位管理' },
        },
        {
          path: 'queue-board',
          component: () => import('@/views/queue/WaitingBoard.vue'),
          meta: { title: '等位大屏', fullscreen: true },
        },
        {
          path: 'logs/changelog',
          component: () => import('@/views/log/ChangelogList.vue'),
          meta: { title: '更新日志' },
        },
      ],
    },
    // 新增：采购员（BUYER）
    {
      path: '/buyer',
      component: () => import('@/views/Purchaser.vue'), // 需创建该页面
      meta: { requiresAuth: true, role: 'PURCHASER' },
      redirect: '/buyer/home',
      children: [
        { path: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
        {
          path: 'purchases',
          component: () => import('@/views/purchase/PurchaseList.vue'),
          meta: { title: '采购入库' },
        },
        {
          path: 'inventory',
          component: () => import('@/views/inventory/InventoryList.vue'),
          meta: { title: '库存查询' },
        },
        {
          path: 'materials',
          component: () => import('@/views/material/MaterialList.vue'),
          meta: { title: '原材料管理' },
        },
        {
          path: 'logs/changelog',
          component: () => import('@/views/log/ChangelogList.vue'),
          meta: { title: '更新日志' },
        },
      ],
    },

    // 新增：厨师（CHEF）
    {
      path: '/chef',
      component: () => import('@/views/Chef.vue'), // 需创建该页面
      meta: { requiresAuth: true, role: 'CHEF' },
      redirect: '/chef/home',
      children: [
        { path: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
        {
          path: 'kds',
          component: () => import('@/views/kds/KDSView.vue'),
          meta: { title: '后厨看板' },
        },
        {
          path: 'ai',
          component: () => import('@/views/ai/AISuggestView.vue'),
          meta: { title: 'AI预制建议' },
        },
        {
          path: 'orders',
          component: () => import('@/views/order/OrderList.vue'),
          meta: { title: '订单查询' },
        },
        {
          path: 'recipes',
          component: () => import('@/views/recipe/RecipeList.vue'),
          meta: { title: '菜品配方' },
        },
        {
          path: 'consumptions',
          component: () => import('@/views/consumption/ConsumptionList.vue'),
          meta: { title: '原材料消耗' },
        },
        {
          path: 'logs/changelog',
          component: () => import('@/views/log/ChangelogList.vue'),
          meta: { title: '更新日志' },
        },
      ],
    },

    // 未匹配重定向到登录
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

// 全局登录/角色守卫
const ROLE_MAP = { admin_global: 'ADMIN', admin_store: 'STORE_ADMIN', cashier: 'CASHIER', waiter: 'WAITER', chef: 'CHEF', purchaser: 'PURCHASER' }

router.beforeEach((to, from, next) => {
  const cookies = useCookies()
  const token = cookies.get('cashier-token')
  const role = ROLE_MAP[cookies.get('cashier-role')?.trim()] || cookies.get('cashier-role')?.trim()

  // 无需登录的页面
  if (to.meta.noAuth) {
    next()
    return
  }

  // 需要登录但没token
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  // 检查角色权限
  const requiredRole = to.meta.role || to.matched[0]?.meta?.role
  if (requiredRole && role !== requiredRole) {
    // 角色不匹配，跳转到对应角色首页
    const roleHomeMap = {
      ADMIN: '/admin',
      STORE_ADMIN: '/manager',
      CASHIER: '/cashier',
      WAITER: '/waiter',
      PURCHASER: '/buyer',
      CHEF: '/chef',
    }
    next(roleHomeMap[role] || '/login')
    return
  }

  next()
})

export default router
