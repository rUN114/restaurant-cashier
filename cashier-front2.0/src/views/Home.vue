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

    <!-- 核心经营指标 -->
    <el-row :gutter="16" class="stats-row">
      <!-- 营业额 -->
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="kpi-card revenue">
          <div class="kpi-label">今日营业额</div>
          <div class="kpi-value">¥{{ fmt(dash.todayRevenue) }}</div>
          <div class="kpi-compare" v-if="revenueChange !== null">
            <span :class="revenueUp ? 'up' : 'down'">
              {{ revenueUp ? '↑' : '↓' }} {{ Math.abs(revenueChange) }}%
            </span>
            <span class="vs">vs 昨日</span>
          </div>
        </div>
      </el-col>

      <!-- 毛利 -->
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="kpi-card profit">
          <div class="kpi-label">今日毛利</div>
          <div class="kpi-value">¥{{ fmt(dash.todayProfit) }}</div>
          <div class="kpi-compare">
            <span class="tag">毛利率 {{ dash.profitMargin || 0 }}%</span>
          </div>
        </div>
      </el-col>

      <!-- 订单数 -->
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="kpi-card orders">
          <div class="kpi-label">订单数</div>
          <div class="kpi-value">{{ dash.todayOrderCount }} <small>单</small></div>
        </div>
      </el-col>

      <!-- 客单价 -->
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="kpi-card avg">
          <div class="kpi-label">客单价</div>
          <div class="kpi-value">¥{{ fmt(dash.avgOrderValue) }}</div>
        </div>
      </el-col>

      <!-- 昨日营业额 -->
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="kpi-card yesterday">
          <div class="kpi-label">昨日营业额</div>
          <div class="kpi-value">¥{{ fmt(dash.yesterdayRevenue) }}</div>
        </div>
      </el-col>

      <!-- 门店 / 角色 -->
      <el-col :xs="12" :sm="8" :lg="4">
        <div class="kpi-card store">
          <div class="kpi-label">{{ user.roleCode === 'ADMIN' ? '全部门店' : '当前门店' }}</div>
          <div class="kpi-value small">{{ storeLabel }}</div>
        </div>
      </el-col>
    </el-row>

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
        <div class="quick-item" @click="goTo('report')" v-if="canReport">
          <el-icon :size="24" color="#F56C6C"><TrendCharts /></el-icon>
          <span>{{ $t('menu.report') }}</span>
        </div>
        <div class="quick-item quick-warning" @click="goTo('inventory')" v-if="canManageInventory && lowStockCount > 0">
          <el-badge :value="lowStockCount" :max="99">
            <el-icon :size="24" color="#f56c6c"><WarningFilled /></el-icon>
          </el-badge>
          <span>库存预警</span>
        </div>
      </div>
    </el-card>

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
        <el-timeline v-if="allChangelogs.length">
          <el-timeline-item
            v-for="(item, idx) in allChangelogs"
            :key="item.id"
            :timestamp="item.createdAt?.substring(0, 10)"
            placement="top"
            :color="idx === 0 ? '#409EFF' : '#c0c4cc'">
            <div>
              <el-tag :type="idx === 0 ? 'primary' : 'info'" size="small">{{ item.version }}</el-tag>
              <strong>{{ item.title }}</strong>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无更新记录" :image-size="60" />
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { getDashboard, getLowStock, getChangelogs } from '@/api'

const { t } = useI18n()
const router = useRouter()
const user = useUserStore()

const basePath = computed(() => {
  const map = { ADMIN: '/admin', STORE_ADMIN: '/manager', CASHIER: '/cashier', WAITER: '/waiter', PURCHASER: '/buyer', CHEF: '/chef' }
  return map[user.roleCode] || '/'
})
const goTo = (subPath) => router.push(`${basePath.value}/${subPath}`)

const canManageDish = computed(() => ['ADMIN','STORE_ADMIN'].includes(user.roleCode))
const canReport = computed(() => ['ADMIN','STORE_ADMIN'].includes(user.roleCode))
const showPos = computed(() => ['ADMIN','STORE_ADMIN','WAITER','CASHIER'].includes(user.roleCode))
const canManageInventory = computed(() => ['ADMIN','STORE_ADMIN','PURCHASER','CHEF'].includes(user.roleCode))

const userAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')

const todayStr = computed(() => {
  const d = new Date()
  const week = t(`home.week${d.getDay()}`)
  return `${d.getFullYear()}${t('home.year')}${d.getMonth()+1}${t('home.month')}${d.getDate()}${t('home.day')} ${week}`
})

const storeLabel = computed(() => {
  if (user.roleCode === 'ADMIN') return '全部门店'
  return user.storeId ? `ID: ${user.storeId}` : '暂无'
})

// 看板数据
const dash = reactive({
  todayRevenue: 0, yesterdayRevenue: 0,
  todayOrderCount: 0, avgOrderValue: 0,
  todayProfit: 0, profitMargin: 0,
})
const revenueChange = ref(null)
const revenueUp = computed(() => revenueChange.value !== null && revenueChange.value >= 0)
const lowStockCount = ref(0)
const allChangelogs = ref([])
const latestChangelog = computed(() => allChangelogs.value[0] || null)
const changelogActive = ref(['changelog'])

const fmt = (v) => (Number(v) || 0).toFixed(2)

onMounted(async () => {
  // 老板看板
  try {
    const res = await getDashboard()
    if (res.data) {
      Object.assign(dash, res.data)
      // 环比 = (今天 - 昨天) / 昨天 * 100
      if (dash.yesterdayRevenue > 0) {
        revenueChange.value = ((dash.todayRevenue - dash.yesterdayRevenue) / dash.yesterdayRevenue * 100).toFixed(1)
      }
    }
  } catch (e) { console.error('加载看板失败:', e) }

  // 库存预警
  if (canManageInventory.value && user.storeId) {
    try {
      const res = await getLowStock(user.storeId)
      lowStockCount.value = (res.data || []).length
    } catch (e) { lowStockCount.value = 0 }
  }

  // 更新日志
  try {
    const res = await getChangelogs()
    allChangelogs.value = res.data || []
  } catch (e) { console.error('加载更新日志失败:', e) }
})

function roleLabelKey() {
  const map = { ADMIN: 'admin', STORE_ADMIN: 'storeAdmin', WAITER: 'waiter', CASHIER: 'cashier', PURCHASER: 'purchaser', CHEF: 'chef' }
  return map[user.roleCode] || 'admin'
}
</script>

<style scoped>
.home-dashboard { padding: 20px; }
.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px; padding: 24px; display: flex;
  align-items: center; justify-content: space-between;
  color: #fff; margin-bottom: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.welcome-left h2 { margin: 0 0 8px; font-size: 22px; }
.welcome-left p { margin: 0; opacity: 0.9; }

/* KPI 卡片 */
.stats-row { margin-bottom: 20px; }
.kpi-card {
  border-radius: 12px; padding: 20px 16px; color: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transition: transform 0.2s; margin-bottom: 16px; min-height: 120px;
  display: flex; flex-direction: column; justify-content: center;
}
.kpi-card:hover { transform: translateY(-2px); }
.kpi-label { font-size: 13px; opacity: 0.85; margin-bottom: 6px; }
.kpi-value { font-size: 26px; font-weight: bold; }
.kpi-value small { font-size: 14px; font-weight: 400; }
.kpi-value.small { font-size: 18px; }
.kpi-compare { margin-top: 6px; font-size: 13px; }
.kpi-compare .up { color: #b9f6ca; }
.kpi-compare .down { color: #ffcdd2; }
.kpi-compare .vs { opacity: 0.6; margin-left: 4px; }
.kpi-compare .tag { background: rgba(255,255,255,0.2); padding: 1px 8px; border-radius: 10px; font-size: 12px; }

.revenue { background: linear-gradient(135deg, #409eff 0%, #2563eb 100%); }
.profit  { background: linear-gradient(135deg, #67c23a 0%, #3d8b37 100%); }
.orders  { background: linear-gradient(135deg, #e6a23c 0%, #cf9236 100%); }
.avg     { background: linear-gradient(135deg, #9c27b0 0%, #7b1fa2 100%); }
.yesterday { background: linear-gradient(135deg, #607d8b 0%, #455a64 100%); }
.store   { background: linear-gradient(135deg, #00bcd4 0%, #0097a7 100%); }

/* 快捷操作 */
.quick-actions-card { border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.quick-title { font-size: 16px; font-weight: bold; }
.quick-buttons { display: flex; flex-wrap: wrap; gap: 20px; }
.quick-item {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  width: 100px; height: 80px; border-radius: 12px; cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s; background: #f8f9fa;
}
.quick-item:hover { background: #e9ecef; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.quick-item span { margin-top: 6px; font-size: 13px; color: #606266; }
.quick-warning { background: #fff5f5; border: 1px solid #fde2e2; }
.quick-warning:hover { background: #ffecec; }
.quick-warning span { color: #f56c6c; font-weight: 600; }

/* 更新日志 */
.collapse-title { display: flex; align-items: center; gap: 10px; width: 100%; }
.panel-icon { font-size: 20px; }
.panel-title { font-size: 16px; font-weight: 700; }
.changelog-collapse { border-radius: 12px; overflow: hidden; border: 1px solid #e4e7ed; }
.changelog-collapse :deep(.el-collapse-item__header) {
  background: #f5f7fa; padding: 14px 20px; font-size: 15px; border: none;
}

@media screen and (max-width: 768px) {
  .kpi-card { min-height: 100px; }
  .kpi-value { font-size: 22px; }
}
</style>
