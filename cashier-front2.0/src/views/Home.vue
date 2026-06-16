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
        <div class="quick-item quick-warning" @click="goTo('inventory')" v-if="canManageInventory">
          <el-badge :value="lowStockItems.length" :hidden="lowStockItems.length === 0" :max="99">
            <el-icon :size="24" color="#f56c6c"><WarningFilled /></el-icon>
          </el-badge>
          <span>库存预警</span>
        </div>
      </div>
    </el-card>

    <!-- 库存预警卡片 -->
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

    <!-- 更新日志卡片 -->
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
        <!-- 全部版本时间线 -->
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
import { ArrowDown, WarningFilled } from '@element-plus/icons-vue'
import { getTodayTurnover, getLowStock, getLatestChangelog, getChangelogs } from '@/api'

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
const canViewTurnover = computed(() => ['ADMIN','STORE_ADMIN','CASHIER','WAITER'].includes(user.roleCode))

const userAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')

const todayStr = computed(() => {
  const d = new Date()
  const week = t(`home.week${d.getDay()}`)
  return `${d.getFullYear()}${t('home.year')}${d.getMonth()+1}${t('home.month')}${d.getDate()}${t('home.day')} ${week}`
})

const todaySales = ref(0)
const lowStockItems = ref([])
const stockActive = ref(['stock'])
const allChangelogs = ref([])
const latestChangelog = computed(() => allChangelogs.value[0] || null)
const changelogActive = ref(['changelog'])
const latestVersion = ref('v1.0')

onMounted(async () => {
  if (canViewTurnover.value) {
    const paramStoreId = user.roleCode === 'ADMIN' ? undefined : user.storeId
    try {
      const res = await getTodayTurnover(paramStoreId)
      todaySales.value = res.data || 0
    } catch (e) { /* ignore */ }
  }

  if (canManageInventory.value && user.storeId) {
    try {
      const res = await getLowStock(user.storeId)
      lowStockItems.value = res.data || []
    } catch (e) {
      lowStockItems.value = []  // 请求失败也清空，避免展示过期数据
    }
  } else {
    lowStockItems.value = []
  }

  try {
    const res = await getChangelogs()
    allChangelogs.value = res.data || []
    if (allChangelogs.value.length > 0) {
      latestVersion.value = allChangelogs.value[0].version
    }
  } catch (e) { /* ignore */ }
})

const cards = computed(() => [
  { title: t('home.todaySales'), value: `¥${Number(todaySales.value).toFixed(2)}`, icon: 'Money', color: '#fff', bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', image: '/local-images/sales4.jpg' },
  { title: t('home.currentStore'), value: user.roleCode === 'ADMIN' ? t('home.allStore') : (user.storeId ? `ID: ${user.storeId}` : t('home.noStore')), icon: 'Shop', color: '#fff', bg: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)', image: '/local-images/store.png' },
  { title: t('home.role'), value: t(`role.${roleLabelKey()}`), icon: 'UserFilled', color: '#fff', bg: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)', image: '/local-images/role.jpg' },
  { title: t('home.version'), value: latestVersion.value, icon: 'Setting', color: '#fff', bg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', image: '/local-images/system.png' },
])

function roleLabelKey() {
  const map = { ADMIN: 'admin', STORE_ADMIN: 'storeAdmin', WAITER: 'waiter', CASHIER: 'cashier', PURCHASER: 'purchaser', CHEF: 'chef' }
  return map[user.roleCode] || 'admin'
}
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
.quick-title { font-size: 16px; font-weight: bold; }
.quick-buttons { display: flex; flex-wrap: wrap; gap: 20px; }
.quick-item { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 100px; height: 80px; border-radius: 12px; cursor: pointer; transition: background 0.2s, box-shadow 0.2s; background: #f8f9fa; }
.quick-item:hover { background: #e9ecef; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.quick-item span { margin-top: 6px; font-size: 13px; color: #606266; }
.quick-warning { background: #fff5f5; border: 1px solid #fde2e2; }
.quick-warning:hover { background: #ffecec; }
.quick-warning span { color: #f56c6c; font-weight: 600; }

/* 折叠面板通用 */
.collapse-title { display: flex; align-items: center; gap: 10px; width: 100%; }
.panel-icon { font-size: 20px; }
.panel-title { font-size: 16px; font-weight: 700; }

/* 库存预警折叠面板 */
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

/* 更新日志折叠面板 */
.changelog-collapse { border-radius: 12px; overflow: hidden; border: 1px solid #e4e7ed; }
.changelog-collapse :deep(.el-collapse-item__header) { background: #f5f7fa; padding: 14px 20px; font-size: 15px; border: none; }
.changelog-version { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.changelog-date { color: #909399; font-size: 13px; }
.changelog-latest h4 { margin: 8px 0; color: #303133; }
.changelog-content { white-space: pre-wrap; line-height: 1.8; color: #606266; font-size: 14px; }

@media screen and (max-width: 768px) { .stat-card { margin-bottom: 16px; } .stock-item { flex-wrap: wrap; } }
</style>
