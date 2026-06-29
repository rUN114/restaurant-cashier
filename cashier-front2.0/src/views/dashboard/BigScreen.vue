<template>
  <div class="bigscreen" @click="toggleFullscreen">
    <!-- 退出全屏按钮 -->
    <div v-if="isFS" class="exit-btn" @click.stop="exitFullscreen" title="退出全屏 (ESC)">✕ 退出全屏</div>
    <!-- 顶栏 -->
    <div class="bs-header">
      <span class="bs-logo">🏪</span>
      <span class="bs-title">{{ storeTitle }}</span>
      <span class="bs-clock">{{ now }}</span>
      <span class="bs-badge">● 实时</span>
      <span class="bs-refresh">刷新 {{ countdown }}s</span>
    </div>

    <!-- 三大指标 -->
    <div class="bs-kpi-row">
      <div class="bs-kpi revenue">
        <div class="kpi-label">今日营业额</div>
        <div class="kpi-value">¥{{ fmt(data.todayRevenue) }}</div>
      </div>
      <div class="bs-kpi orders">
        <div class="kpi-label">今日订单</div>
        <div class="kpi-value">{{ data.todayOrderCount }} <small>单</small></div>
      </div>
      <div class="bs-kpi tables">
        <div class="kpi-label">桌台占用</div>
        <div class="kpi-value">{{ data.occupiedTables }}<small>/{{ data.totalTables }}</small></div>
      </div>
      <div class="bs-kpi trend" v-if="data.revenueChange != null">
        <div class="kpi-label">环比昨日</div>
        <div class="kpi-value" :class="{ up: data.revenueChange >= 0, down: data.revenueChange < 0 }">
          {{ data.revenueChange >= 0 ? '↑' : '↓' }}{{ Math.abs(data.revenueChange) }}%
        </div>
      </div>
    </div>

    <!-- 两列内容 -->
    <div class="bs-body">
      <!-- 左：最近成交 -->
      <div class="bs-panel">
        <div class="panel-head">💰 最近成交</div>
        <div class="panel-list" v-if="data.recentOrders?.length">
          <div class="list-row header-row">
            <span>桌号</span><span>金额</span><span>时间</span>
          </div>
          <div v-for="(o, i) in data.recentOrders" :key="i" class="list-row" :style="{ animationDelay: i * 0.1 + 's' }">
            <span class="tbl">{{ o.tableNumber || '-' }}</span>
            <span class="amt">¥{{ fmt(o.amount) }}</span>
            <span class="tm">{{ o.time }}</span>
          </div>
        </div>
        <div class="panel-empty" v-else>暂无成交</div>
      </div>

      <!-- 右：等餐中 -->
      <div class="bs-panel">
        <div class="panel-head">🍳 等餐中</div>
        <div class="panel-list" v-if="data.diningOrders?.length">
          <div class="list-row header-row">
            <span>桌号</span><span>菜品</span><span>等餐</span>
          </div>
          <div v-for="(o, i) in data.diningOrders" :key="i" class="list-row" :class="{ urgent: o.elapsedMinutes > 20 }">
            <span class="tbl">{{ o.tableNumber || '-' }}</span>
            <span class="cnt">{{ o.dishCount }} 道</span>
            <span class="eta">{{ o.elapsedMinutes }}分</span>
          </div>
        </div>
        <div class="panel-empty" v-else>全部已出餐 🎉</div>
      </div>
    </div>

    <!-- 底部滚动条 -->
    <div class="bs-ticker" v-if="data.diningOrders?.length > 0">
      <span class="ticker-text">
        🔔 {{ data.diningOrders.map(o => o.tableNumber + '等餐' + o.elapsedMinutes + '分').join(' · ') }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { getBigScreen } from '@/api/dashboard'
import { useFullscreen } from '@/composables/useFullscreen'

const { isFullscreen } = useFullscreen()
const user = useUserStore()

const now = ref('')
const countdown = ref(30)
const data = reactive({
  todayRevenue: 0,
  todayOrderCount: 0,
  occupiedTables: 0,
  totalTables: 0,
  revenueChange: null,
  recentOrders: [],
  diningOrders: [],
})

const storeTitle = computed(() => {
  if (user.roleCode === 'ADMIN') return '全部门店'
  return user.storeId ? `门店 #${user.storeId}` : '实时营业'
})

const fmt = (v) => {
  if (v == null) return '0.00'
  return Number(v).toFixed(2)
}

let pollTimer = null
let clockTimer = null
let cdTimer = null

const refresh = async () => {
  try {
    const res = await getBigScreen()
    if (res.data) Object.assign(data, res.data)
  } catch (_) {}
}

const isFS = ref(false)
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
  }
}
const exitFullscreen = () => {
  if (document.fullscreenElement) document.exitFullscreen()
}
const onFSChange = () => {
  isFS.value = !!document.fullscreenElement
  isFullscreen.value = isFS.value
}
onMounted(() => document.addEventListener('fullscreenchange', onFSChange))
onBeforeUnmount(() => { document.removeEventListener('fullscreenchange', onFSChange); isFullscreen.value = false; if (document.fullscreenElement) document.exitFullscreen() })

onMounted(() => {
  refresh()
  // 30秒刷新数据
  pollTimer = setInterval(refresh, 30000)
  // 时钟
  clockTimer = setInterval(() => {
    now.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }, 1000)
  // 倒计时
  cdTimer = setInterval(() => {
    countdown.value = countdown.value <= 1 ? 30 : countdown.value - 1
  }, 1000)
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  clearInterval(clockTimer)
  clearInterval(cdTimer)
})
</script>

<style scoped>
.exit-btn {
  position: fixed; bottom: 24px; right: 24px; z-index: 9999;
  background: rgba(255,0,0,0.85); color: #fff; padding: 12px 24px;
  border-radius: 8px; cursor: pointer; font-size: 16px; font-weight: bold;
}

.bigscreen {
  background: #0a0f1a;
  min-height: 100vh;
  color: #e0e6f0;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  padding: 0;
  cursor: pointer;
  user-select: none;
}

/* 顶栏 */
.bs-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 32px;
  background: linear-gradient(180deg, #111a2e 0%, #0a0f1a 100%);
  border-bottom: 1px solid #1e2d4a;
}
.bs-logo { font-size: 32px; }
.bs-title { font-size: 24px; font-weight: bold; letter-spacing: 4px; }
.bs-clock { font-size: 28px; margin-left: auto; color: #00e676; font-weight: bold; font-variant-numeric: tabular-nums; }
.bs-badge { color: #00e676; font-size: 13px; animation: pulse 2s infinite; }
.bs-refresh { color: #8899aa; font-size: 13px; margin-left: 8px; }

/* KPI */
.bs-kpi-row {
  display: flex;
  gap: 16px;
  padding: 24px 32px;
}
.bs-kpi {
  flex: 1;
  background: linear-gradient(135deg, #131f35 0%, #1a2a48 100%);
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #1e3050;
  text-align: center;
}
.bs-kpi.revenue { border-left: 4px solid #409eff; }
.bs-kpi.orders { border-left: 4px solid #e6a23c; }
.bs-kpi.tables { border-left: 4px solid #67c23a; }
.bs-kpi.trend { border-left: 4px solid #00bcd4; }

.kpi-label { font-size: 15px; color: #8899aa; margin-bottom: 8px; }
.kpi-value { font-size: 42px; font-weight: bold; }
.kpi-value small { font-size: 20px; color: #8899aa; }
.kpi-value.up { color: #f56c6c; }
.kpi-value.down { color: #67c23a; }
.bs-kpi.revenue .kpi-value { color: #409eff; }
.bs-kpi.orders .kpi-value { color: #e6a23c; }
.bs-kpi.tables .kpi-value { color: #67c23a; }

/* 双列 */
.bs-body {
  display: flex;
  gap: 16px;
  padding: 0 32px 24px;
}
.bs-panel {
  flex: 1;
  background: linear-gradient(135deg, #131f35 0%, #1a2a48 100%);
  border-radius: 12px;
  border: 1px solid #1e3050;
  padding: 20px;
  min-height: 360px;
}
.panel-head {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #1e3050;
}
.panel-list { display: flex; flex-direction: column; gap: 0; }
.list-row {
  display: flex;
  align-items: center;
  padding: 12px 8px;
  border-bottom: 1px solid #111a2a;
  animation: fadeIn 0.4s ease;
  font-size: 18px;
}
.list-row:hover { background: rgba(255,255,255,0.03); }
.header-row { font-size: 13px; color: #667788; text-transform: uppercase; letter-spacing: 2px; }
.list-row .tbl { flex: 0 0 80px; font-weight: bold; font-size: 22px; color: #ffd700; }
.list-row .amt { flex: 1; color: #67c23a; font-weight: bold; }
.list-row .tm { color: #8899aa; }
.list-row .cnt { flex: 1; }
.list-row .eta { color: #ff9800; font-weight: bold; }
.list-row.urgent { background: rgba(255,0,0,0.08); border-left: 3px solid #f44336; }
.list-row.urgent .eta { color: #f44336; animation: pulse 1s infinite; }

.panel-empty {
  text-align: center;
  padding: 60px 0;
  color: #555;
  font-size: 18px;
}

/* 底部滚动条 */
.bs-ticker {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #131f35;
  border-top: 1px solid #1e3050;
  padding: 8px 32px;
  overflow: hidden;
}
.ticker-text {
  color: #667788;
  font-size: 14px;
  white-space: nowrap;
  animation: ticker 20s linear infinite;
  display: inline-block;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes ticker {
  0% { transform: translateX(100%); }
  100% { transform: translateX(-100%); }
}
</style>
