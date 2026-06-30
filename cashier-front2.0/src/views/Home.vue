<template>
  <div class="home-dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <h2>{{ $t('home.welcome') }}{{ user.realName || user.username }}</h2>
        <p>{{ $t('home.today') }}{{ todayStr }}，{{ $t('home.wish') }}</p>
      </div>
      <div class="welcome-right">
        <el-avatar :size="48" :src="userAvatar" />
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6" v-for="card in cards" :key="card.title">
        <div class="stat-card" :style="{ background: card.bg }">
          <img v-if="card.image" :src="card.image" loading="lazy" style="width:48px;height:48px;object-fit:contain" />
          <el-icon v-else :size="32" :color="card.color"><component :is="card.icon" /></el-icon>
          <div class="stat-info">
            <div class="stat-label">{{ card.title }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 收银员/管理员：进行中订单列表 -->
    <el-card v-if="isCashier || isManager" class="cashier-orders-card" shadow="hover">
      <template #header>
        <div class="cashier-orders-header">
          <span class="co-title">📋 进行中订单</span>
          <el-tag :type="activeOrders.length > 0 ? 'warning' : 'success'" size="small">
            {{ activeOrders.length }} 桌
          </el-tag>
          <el-button link type="primary" size="small" style="margin-left:auto" @click="goTo('pos')">
            全部订单 →
          </el-button>
        </div>
      </template>
      <div v-if="activeOrders.length === 0" class="co-empty">🎉 暂无进行中的订单</div>
      <div class="co-grid" v-else>
        <div
          v-for="order in activeOrders.slice(0, 8)"
          :key="order.id"
          class="co-item"
          @click="goTo('pos')"
        >
          <div class="co-table">{{ order.tableNumber || '-' }}</div>
          <div class="co-info">
            <span class="co-no">#{{ order.orderNo?.slice(-6) || order.id }}</span>
            <span class="co-time">{{ timeAgo(order.createdAt) }}</span>
          </div>
          <el-tag :type="order.status === 'in_progress' ? 'warning' : 'success'" size="small">
            {{ order.status === 'in_progress' ? '用餐中' : '已结账' }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 快捷入口 -->
    <el-card class="quick-actions-card" shadow="hover">
      <template #header><span class="quick-title">{{ $t('home.quick') }}</span></template>
      <div class="quick-buttons">
        <div class="quick-item" @click="goTo('pos')" v-if="showPos">
          <el-icon :size="24" color="#409EFF"><ShoppingCartFull /></el-icon>
          <span>{{ $t('menu.pos') }}</span>
        </div>
        <div class="quick-item" @click="goTo('orders')">
          <el-icon :size="24" color="#67C23A"><Notebook /></el-icon>
          <span>{{ $t('menu.orderQuery') }}</span>
        </div>
        <div class="quick-item" @click="goTo('dishes')" v-if="canManageDish">
          <el-icon :size="24" color="#E6A23C"><KnifeFork /></el-icon>
          <span>{{ $t('menu.dishManage') }}</span>
        </div>
        <div class="quick-item" @click="goTo('stores')" v-if="user.roleCode === 'ADMIN'">
          <el-icon :size="24" color="#909399"><Shop /></el-icon>
          <span>{{ $t('menu.storeManage') }}</span>
        </div>
        <div class="quick-item" @click="goTo('report')" v-if="canReport">
          <el-icon :size="24" color="#F56C6C"><TrendCharts /></el-icon>
          <span>{{ $t('menu.report') }}</span>
        </div>
        <div class="quick-item quick-kds" @click="goTo('kds')" v-if="isChef">
          <el-icon :size="24" color="#e040fb"><KnifeFork /></el-icon>
          <span>后厨看板</span>
        </div>
        <div class="quick-item quick-waiter" @click="goTo('kds')" v-if="isWaiter">
          <el-icon :size="24" color="#ff9800"><Goods /></el-icon>
          <span>服务操控台</span>
        </div>
        <div class="quick-item quick-warning" @click="goTo('inventory')" v-if="canManageInventory">
          <el-badge :value="lowStockCount" :hidden="lowStockCount === 0" :max="99">
            <el-icon :size="24" color="#f56c6c"><WarningFilled /></el-icon>
          </el-badge>
          <span>库存预警</span>
        </div>
      </div>
    </el-card>

    <!-- 库存预警 -->
    <el-collapse v-if="canManageInventory && lowStockItems.length > 0" v-model="stockActive" class="stock-collapse" style="margin-top:20px">
      <el-collapse-item name="stock">
        <template #title>
          <div class="collapse-title">
            <span class="panel-icon">⚠️</span>
            <span class="panel-title">库存不足预警</span>
            <el-tag type="danger" size="small" effect="dark">{{ lowStockItems.length }} 项缺货</el-tag>
          </div>
        </template>
        <div class="stock-item" v-for="item in lowStockItems" :key="item.materialId" @click="goTo('inventory')">
          <span class="stock-name">{{ item.name }}</span>
          <span class="stock-info">当前 <b>{{ item.currentStock }}</b> / 预警 {{ item.warningStock }} {{ item.unit }}</span>
          <el-tag type="danger" size="small" effect="plain">缺 {{ (item.warningStock - item.currentStock).toFixed(1) }}</el-tag>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 更新日志 -->
    <el-collapse v-model="changelogActive" class="changelog-collapse" style="margin-top:16px">
      <el-collapse-item name="changelog">
        <template #title>
          <div class="collapse-title">
            <span class="panel-icon">📋</span>
            <span class="panel-title">版本更新日志</span>
            <span v-if="latestChangelog" style="color:#909399;font-size:13px;margin-left:8px">
              {{ latestChangelog.version }} · {{ latestChangelog.createdAt?.substring(0, 10) }}
            </span>
          </div>
        </template>
        <el-timeline v-if="allChangelogs.length > 0">
          <el-timeline-item
            v-for="(item, idx) in allChangelogs"
            :key="item.id"
            :timestamp="item.createdAt?.substring(0, 10)"
            placement="top"
            :color="idx === 0 ? '#409EFF' : '#c0c4cc'"
          >
            <div style="display:flex;align-items:center;gap:8px">
              <el-tag :type="idx === 0 ? 'primary' : 'info'" size="small">{{ item.version }}</el-tag>
              <strong>{{ item.title }}</strong>
            </div>
            <p v-if="idx === 0" style="white-space:pre-wrap;line-height:1.8;color:#606266;margin-top:8px;font-size:14px">{{ item.content }}</p>
            <p v-else style="color:#909399;font-size:13px;margin-top:4px">{{ item.content?.substring(0, 60) }}{{ item.content?.length > 60 ? '...' : '' }}</p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无更新记录" :image-size="60" />
        <div style="text-align:right;margin-top:8px" v-if="allChangelogs.length > 0">
          <el-button link type="primary" size="small" @click.stop="goTo('logs/changelog')">查看全部 →</el-button>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { ArrowDown, WarningFilled, Goods } from '@element-plus/icons-vue'
import { getTodayTurnover, getLowStock, getChangelogs, getDashboard, getInProgressOrders } from '@/api'
import { getKitchenBoard, getWaiterBoard } from '@/api/kds'

const { t } = useI18n()
const router = useRouter()
const user = useUserStore()

const basePath = computed(() => {
  const map = { ADMIN: '/admin', STORE_ADMIN: '/manager', CASHIER: '/cashier', WAITER: '/waiter', PURCHASER: '/buyer', CHEF: '/chef' }
  return map[user.roleCode] || '/'
})
const goTo = (subPath) => router.push(`${basePath.value}/${subPath}`)

const isChef = computed(() => user.roleCode === 'CHEF')
const isWaiter = computed(() => user.roleCode === 'WAITER')
const isCashier = computed(() => user.roleCode === 'CASHIER')
const isPurchaser = computed(() => user.roleCode === 'PURCHASER')
const isManager = computed(() => ['ADMIN', 'STORE_ADMIN'].includes(user.roleCode))
const isAdmin = computed(() => user.roleCode === 'ADMIN')

const canManageDish = computed(() => isManager.value)
const canReport = computed(() => isManager.value)
const showPos = computed(() => ['ADMIN','STORE_ADMIN','WAITER','CASHIER'].includes(user.roleCode))
const canManageInventory = computed(() => ['ADMIN','STORE_ADMIN','PURCHASER','CHEF'].includes(user.roleCode))

const userAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')

const todayStr = computed(() => {
  const d = new Date()
  const week = t(`home.week${d.getDay()}`)
  return `${d.getFullYear()}${t('home.year')}${d.getMonth()+1}${t('home.month')}${d.getDate()}${t('home.day')} ${week}`
})

// ====== 角色专属数据 ======
const todaySales = ref(0)
const todayProfit = ref(0)
const profitMargin = ref(0)
const todayOrderCount = ref(0)
const avgOrderValue = ref(0)
const yesterdayRevenue = ref(0)
const revenueChange = ref(null)

// 厨师
const kitchenWaiting = ref(0)
const kitchenCooking = ref(0)
const kitchenDone = ref(0)

// 服务员
const occupiedTables = ref(0)
const freeTables = ref(0)
const waiterDoneCount = ref(0)

// 库存
const lowStockItems = ref([])
const lowStockCount = ref(0)
const inventoryKindCount = ref(0)
const activeOrders = ref([])

// 更新日志
const stockActive = ref(['stock'])
const allChangelogs = ref([])
const latestChangelog = computed(() => allChangelogs.value[0] || null)
const changelogActive = ref(['changelog'])
const latestVersion = ref('v1.0')

// ====== 数据加载 ======
onMounted(async () => {
  const storeId = user.storeId

  // --- 管理员 & 店长：营业额看板 ---
  if (isManager.value) {
    const paramStoreId = isAdmin.value ? undefined : storeId
    try {
      const res = await getTodayTurnover(paramStoreId)
      todaySales.value = res.data || 0
    } catch (e) { console.error('加载今日营业额失败:', e) }

    try {
      const dash = await getDashboard()
      if (dash.data) {
        todayProfit.value = dash.data.todayProfit || 0
        profitMargin.value = dash.data.profitMargin || 0
        todayOrderCount.value = dash.data.todayOrderCount || 0
        avgOrderValue.value = dash.data.avgOrderValue || 0
        yesterdayRevenue.value = dash.data.yesterdayRevenue || 0
        if (dash.data.yesterdayRevenue > 0) {
          revenueChange.value = ((dash.data.todayRevenue - dash.data.yesterdayRevenue)
            / dash.data.yesterdayRevenue * 100).toFixed(1)
        }
      }
    } catch (e) { console.error('加载经营看板失败:', e) }
  }

  // --- 收银员 & 管理员：进行中订单 ---
  if ((isCashier.value || isManager.value) && storeId) {
    try {
      const res = await getInProgressOrders(isAdmin.value ? undefined : storeId)
      activeOrders.value = res.data || []
    } catch (e) { activeOrders.value = [] }
  }

  // --- 收银员：营业额 + 订单数 ---
  if (isCashier.value && storeId) {
    try {
      const res = await getTodayTurnover(storeId)
      todaySales.value = res.data || 0
    } catch (e) { console.error('加载营业额失败:', e) }

    try {
      const dash = await getDashboard()
      if (dash.data) {
        todayOrderCount.value = dash.data.todayOrderCount || 0
        avgOrderValue.value = dash.data.avgOrderValue || 0
      }
    } catch (e) { /* ignore */ }

    // 待上菜数
    try {
      const res = await getWaiterBoard()
      if (res.data) {
        let done = 0
        for (const order of res.data) {
          done += order.items.filter(i => i.kitchenStatus === 'DONE').length
        }
        waiterDoneCount.value = done
      }
    } catch (e) { /* ignore */ }
  }

  // --- 服务员：桌台占用 + 待上菜 ---
  if (isWaiter.value && storeId) {
    // 待上菜数
    try {
      const res = await getWaiterBoard()
      if (res.data) {
        let done = 0
        for (const order of res.data) {
          done += order.items.filter(i => i.kitchenStatus === 'DONE').length
        }
        waiterDoneCount.value = done
        todayOrderCount.value = res.data.length
      }
    } catch (e) { /* ignore */ }
  }

  // --- 厨师：厨房看板 ---
  if (isChef.value) {
    try {
      const res = await getKitchenBoard()
      if (res.data) {
        kitchenWaiting.value = (res.data.WAITING || []).length
        kitchenCooking.value = (res.data.COOKING || []).length
        kitchenDone.value = (res.data.DONE || []).length
      }
    } catch (e) { /* ignore */ }
  }

  // --- 采购员 & 厨师 & 管理员：库存预警 ---
  if (canManageInventory.value) {
    try {
      const res = await getLowStock(storeId || undefined)
      lowStockItems.value = res.data || []
      lowStockCount.value = lowStockItems.value.length
    } catch (e) { lowStockItems.value = [] }
  }

  // --- 更新日志（所有人） ---
  try {
    const res = await getChangelogs()
    allChangelogs.value = res.data || []
    if (allChangelogs.value.length > 0) {
      latestVersion.value = allChangelogs.value[0].version
    }
  } catch (e) { console.error('加载更新日志失败:', e) }
})

// ====== 按角色定制卡片 ======
const storeLabel = computed(() => {
  if (isAdmin.value) return '所有门店'
  if (user.storeId) return `门店 #${user.storeId}`
  return '未绑定'
})

const timeAgo = (t) => {
  if (!t) return ''
  const m = Math.floor((Date.now() - new Date(t).getTime()) / 60000)
  return m < 1 ? '刚刚' : m < 60 ? `${m}分钟前` : `${Math.floor(m / 60)}小时前`
}

const roleName = computed(() => {
  const map = { ADMIN: '管理员', STORE_ADMIN: '店长', CASHIER: '收银员', WAITER: '服务员', CHEF: '厨师', PURCHASER: '采购员' }
  return map[user.roleCode] || user.roleCode
})

const cards = computed(() => {
  const role = user.roleCode
  const common = [
    { title: '当前门店', value: storeLabel.value, image: '/local-images/store.png', color: '#fff', bg: 'linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%)' },
    { title: '当前角色', value: roleName.value, image: '/local-images/role.jpg', color: '#fff', bg: 'linear-gradient(135deg, #409eff 0%, #66b1ff 100%)' },
  ]

  switch (role) {
    // ====== 管理员 / 店长 ======
    case 'ADMIN':
    case 'STORE_ADMIN':
      return [
        { title: '今日营业额', value: `¥${Number(todaySales.value).toFixed(2)}`, image: '/local-images/sales4.jpg', color: '#fff', bg: 'linear-gradient(135deg, #409eff 0%, #2563eb 100%)' },
        { title: '今日毛利', value: `¥${Number(todayProfit.value).toFixed(2)}  (${profitMargin.value}%)`, image: '/local-images/profit.jpg', color: '#fff', bg: 'linear-gradient(135deg, #67c23a 0%, #3d8b37 100%)' },
        { title: '今日订单', value: `${todayOrderCount.value} 单`, image: '/local-images/orders.jpg', color: '#fff', bg: 'linear-gradient(135deg, #e6a23c 0%, #cf9236 100%)' },
        { title: '客单价', value: `¥${Number(avgOrderValue.value).toFixed(2)}`, image: '/local-images/avg.jpg', color: '#fff', bg: 'linear-gradient(135deg, #9c27b0 0%, #7b1fa2 100%)' },
        { title: '昨日营业额', value: `¥${Number(yesterdayRevenue.value).toFixed(2)}`, image: '/local-images/yesterday.jpg', color: '#fff', bg: 'linear-gradient(135deg, #607d8b 0%, #455a64 100%)' },
        { title: '环比变化', value: revenueChange.value !== null ? `${revenueChange.value >= 0 ? '+' : ''}${revenueChange.value}%` : '--', image: '/local-images/compare.jpg', color: '#fff', bg: 'linear-gradient(135deg, #00bcd4 0%, #0097a7 100%)' },
        ...common,
      ]

    // ====== 收银员 ======
    case 'CASHIER':
      return [
        { title: '今日营业额', value: `¥${Number(todaySales.value).toFixed(2)}`, icon: 'Money', color: '#fff', bg: 'linear-gradient(135deg, #409eff 0%, #2563eb 100%)' },
        { title: '今日订单', value: `${todayOrderCount.value} 单`, icon: 'Notebook', color: '#fff', bg: 'linear-gradient(135deg, #e6a23c 0%, #cf9236 100%)' },
        { title: '客单价', value: `¥${Number(avgOrderValue.value).toFixed(2)}`, icon: 'ShoppingCartFull', color: '#fff', bg: 'linear-gradient(135deg, #9c27b0 0%, #7b1fa2 100%)' },
        { title: '待上菜', value: `${waiterDoneCount.value} 道`, icon: 'Goods', color: '#fff', bg: 'linear-gradient(135deg, #ff9800 0%, #f57c00 100%)' },
        ...common,
      ]

    // ====== 服务员 ======
    case 'WAITER':
      return [
        { title: '进行中订单', value: `${todayOrderCount.value} 桌`, icon: 'Notebook', color: '#fff', bg: 'linear-gradient(135deg, #409eff 0%, #2563eb 100%)' },
        { title: '待上菜', value: `${waiterDoneCount.value} 道`, icon: 'Goods', color: '#fff', bg: waiterDoneCount.value > 0 ? 'linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%)' : 'linear-gradient(135deg, #67c23a 0%, #3d8b37 100%)' },
        { title: '桌台占用', value: `${todayOrderCount.value} 桌在用`, icon: 'Grid', color: '#fff', bg: 'linear-gradient(135deg, #e6a23c 0%, #cf9236 100%)' },
        { title: '快捷入口', value: 'POS / 操控台', icon: 'ShoppingCartFull', color: '#fff', bg: 'linear-gradient(135deg, #00bcd4 0%, #0097a7 100%)' },
        ...common,
      ]

    // ====== 厨师 ======
    case 'CHEF':
      return [
        { title: '待制作', value: `${kitchenWaiting.value} 道`, icon: 'Notebook', color: '#fff', bg: kitchenWaiting.value > 5 ? 'linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%)' : 'linear-gradient(135deg, #e6a23c 0%, #cf9236 100%)' },
        { title: '制作中', value: `${kitchenCooking.value} 道`, icon: 'KnifeFork', color: '#fff', bg: 'linear-gradient(135deg, #ff9800 0%, #f57c00 100%)' },
        { title: '待上菜', value: `${kitchenDone.value} 道`, icon: 'Goods', color: '#fff', bg: kitchenDone.value > 0 ? 'linear-gradient(135deg, #67c23a 0%, #3d8b37 100%)' : 'linear-gradient(135deg, #607d8b 0%, #455a64 100%)' },
        { title: '库存预警', value: `${lowStockCount.value} 项`, icon: 'WarningFilled', color: '#fff', bg: lowStockCount.value > 0 ? 'linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%)' : 'linear-gradient(135deg, #67c23a 0%, #3d8b37 100%)' },
        ...common,
      ]

    // ====== 采购员 ======
    case 'PURCHASER':
      return [
        { title: '库存预警', value: `${lowStockCount.value} 项`, icon: 'WarningFilled', color: '#fff', bg: lowStockCount.value > 0 ? 'linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%)' : 'linear-gradient(135deg, #67c23a 0%, #3d8b37 100%)' },
        { title: '低库存项', value: `${lowStockItems.value.length} 种`, icon: 'Box', color: '#fff', bg: 'linear-gradient(135deg, #e6a23c 0%, #cf9236 100%)' },
        { title: '快捷入口', value: '采购 / 库存', icon: 'Goods', color: '#fff', bg: 'linear-gradient(135deg, #00bcd4 0%, #0097a7 100%)' },
        { title: '库存状态', value: lowStockCount.value > 0 ? '需补货' : '正常', icon: 'TrendCharts', color: '#fff', bg: lowStockCount.value > 0 ? 'linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%)' : 'linear-gradient(135deg, #67c23a 0%, #3d8b37 100%)' },
        ...common,
      ]

    default:
      return common
  }
})
</script>

<style scoped>
.home-dashboard { padding: 20px; }
.welcome-banner { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; padding: 24px; display: flex; align-items: center; justify-content: space-between; color: #fff; margin-bottom: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.welcome-left h2 { margin: 0 0 8px; font-size: 22px; }
.welcome-left p { margin: 0; opacity: 0.9; }
.stats-row { margin-bottom: 20px; }
.stat-card { padding: 20px; border-radius: 12px; color: #fff; display: flex; align-items: center; gap: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); transition: transform 0.2s; margin-bottom: 16px; }
.stat-card:hover { transform: translateY(-2px); }
.stat-info { flex: 1; }
.stat-label { font-size: 14px; opacity: 0.9; }
.stat-value { font-size: 24px; font-weight: bold; margin-top: 4px; }
.quick-actions-card { border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.cashier-orders-card { border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); margin-bottom: 20px; }
.cashier-orders-header { display: flex; align-items: center; gap: 10px; }
.co-title { font-size: 18px; font-weight: bold; }
.co-empty { text-align: center; padding: 30px; color: #999; font-size: 15px; }
.co-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; }
.co-item { display: flex; align-items: center; gap: 12px; padding: 14px; background: #f8f9fa; border-radius: 10px; cursor: pointer; transition: all 0.2s; border: 1px solid transparent; }
.co-item:hover { background: #e8f4fd; border-color: #409eff; box-shadow: 0 2px 8px rgba(64,158,255,0.15); }
.co-table { font-size: 28px; font-weight: bold; color: #303133; min-width: 50px; }
.co-info { display: flex; flex-direction: column; gap: 2px; flex: 1; }
.co-no { font-size: 13px; color: #909399; }
.co-time { font-size: 12px; color: #bbb; }
.quick-title { font-size: 16px; font-weight: bold; }
.quick-buttons { display: flex; flex-wrap: wrap; gap: 20px; }
.quick-item { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 100px; height: 80px; border-radius: 12px; cursor: pointer; transition: background 0.2s, box-shadow 0.2s; background: #f8f9fa; }
.quick-item:hover { background: #e9ecef; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.quick-item span { margin-top: 6px; font-size: 13px; color: #606266; }
.quick-warning { background: #fff5f5; border: 1px solid #fde2e2; }
.quick-warning:hover { background: #ffecec; }
.quick-warning span { color: #f56c6c; font-weight: 600; }
.quick-kds { background: #f3e5f5; border: 1px solid #e1bee7; }
.quick-kds:hover { background: #f8ecfd; }
.quick-waiter { background: #fff8e1; border: 1px solid #ffecb3; }
.quick-waiter:hover { background: #fffde7; }

.collapse-title { display: flex; align-items: center; gap: 10px; width: 100%; }
.panel-icon { font-size: 20px; }
.panel-title { font-size: 16px; font-weight: 700; }

.stock-collapse { border-radius: 12px; overflow: hidden; border: 1px solid #fde2e2; }
.stock-collapse :deep(.el-collapse-item__header) {
  background: linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%); color: #fff;
  padding: 14px 20px; font-size: 15px; border: none;
}
.stock-collapse :deep(.el-collapse-item__header .el-tag) { margin-left: auto; }
.stock-collapse :deep(.el-collapse-item__wrap) { border: none; background: linear-gradient(135deg, #fff5f0, #fff0f0); }
.stock-collapse :deep(.el-collapse-item__arrow) { color: #fff; }
.stock-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px; margin-top: 8px;
  background: #fff; border-radius: 8px; border: 1px solid #fde2e2;
  cursor: pointer; transition: box-shadow 0.2s;
}
.stock-item:hover { box-shadow: 0 2px 8px rgba(245,108,108,0.15); }
.stock-name { font-weight: 600; color: #303133; min-width: 70px; }
.stock-info { color: #606266; font-size: 13px; flex: 1; }
.stock-info b { color: #f56c6c; }

.changelog-collapse { border-radius: 12px; overflow: hidden; border: 1px solid #e4e7ed; }
.changelog-collapse :deep(.el-collapse-item__header) { background: #f5f7fa; padding: 14px 20px; font-size: 15px; border: none; }

@media screen and (max-width: 768px) { .stat-card { margin-bottom: 16px; } .stock-item { flex-wrap: wrap; } }
</style>
