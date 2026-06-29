<template>
  <div class="waiter-kds">
    <!-- 顶栏 -->
    <div class="wk-header">
      <span class="wk-title">🍽️ 服务员操控台</span>
      <span class="wk-clock">{{ time }}</span>
      <span class="wk-status">● 在线</span>
    </div>

    <!-- 订单卡片网格 -->
    <div class="wk-grid" v-if="orders.length">
      <div
        v-for="order in orders"
        :key="order.orderId"
        class="wk-order-card"
      >
        <div class="wk-order-head">
          <span class="wk-table">{{ order.tableNumber || '桌号?' }}</span>
          <span class="wk-order-id">#{{ order.orderId }}</span>
          <span class="wk-summary">
            {{ doneCount(order.items) }}/{{ order.items.length }} 已好
          </span>
        </div>

        <div class="wk-items">
          <div
            v-for="item in order.items"
            :key="item.id"
            :class="itemRowClass(item)"
          >
            <div class="wk-item-left">
              <span class="wk-dish">{{ item.dishName }}</span>
              <span class="wk-wait">{{ formatWait(item.waitSeconds) }}</span>
            </div>

            <div class="wk-item-right">
              <span :class="statusBadge(item.kitchenStatus)">
                {{ statusLabel(item.kitchenStatus) }}
              </span>

              <el-button
                v-if="item.kitchenStatus !== 'SERVED' && item.kitchenStatus !== 'DONE'"
                type="warning"
                size="small"
                :icon="Bell"
                @click="bump(item.id)"
              >
                催单
                <span v-if="item.bumpCount > 0" class="bump-n">×{{ item.bumpCount }}</span>
              </el-button>

              <el-button
                v-if="item.kitchenStatus === 'DONE'"
                type="success"
                size="small"
                @click="serve(item.id)"
              >
                上菜
              </el-button>

              <el-tag v-if="item.kitchenStatus === 'SERVED'" type="info" size="small">
                已上菜
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="wk-empty" v-else>
      🎉 当前没有进行中的订单
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { getWaiterBoard, bumpItem, serveItem } from '@/api/kds'
import { ElMessage } from 'element-plus'

const orders = ref([])
const time = ref('')
let pollTimer = null
let clockTimer = null

const poll = async () => {
  try {
    const res = await getWaiterBoard()
    if (res.data) {
      // 更新每个item的waitSeconds（前端计时）
      const now = Date.now()
      for (const order of res.data) {
        for (const item of order.items) {
          if (item.createdAt) {
            item.waitSeconds = Math.floor((now - new Date(item.createdAt).getTime()) / 1000)
          }
        }
      }
      orders.value = res.data
    }
  } catch (e) { /* ignore */ }
}

const bump = async (id) => {
  try {
    await bumpItem(id)
    ElMessage.success('已催单，厨师将优先处理')
    poll()
  } catch (e) {
    ElMessage.error('催单失败')
  }
}

const serve = async (id) => {
  try {
    await serveItem(id)
    ElMessage.success('已上菜')
    poll()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const doneCount = (items) => items.filter(i => i.kitchenStatus === 'DONE' || i.kitchenStatus === 'SERVED').length

const formatWait = (s) => {
  if (!s && s !== 0) return ''
  const m = Math.floor(s / 60)
  return m > 0 ? `${m}分${s % 60}秒` : `${s}秒`
}

const statusLabel = (s) => {
  const map = { WAITING: '待做', COOKING: '制作中', DONE: '已完成', SERVED: '已上菜' }
  return map[s] || s
}

const statusBadge = (s) => {
  return {
    'wk-status-badge': true,
    'st-waiting': s === 'WAITING',
    'st-cooking': s === 'COOKING',
    'st-done': s === 'DONE',
    'st-served': s === 'SERVED',
  }
}

const itemRowClass = (item) => ({
  'wk-item-row': true,
  'bumped': item.bumpCount > 0,
  'urgent': item.waitSeconds > 600,
})

// WebSocket
let ws = null
const connectWS = () => {
  try {
    ws = new WebSocket(`ws://${window.location.host}/ws`)
    ws.onmessage = () => poll()
    ws.onclose = () => setTimeout(connectWS, 3000)
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  poll()
  pollTimer = setInterval(poll, 5000)
  clockTimer = setInterval(() => {
    time.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }, 1000)
  connectWS()
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  clearInterval(clockTimer)
  ws?.close()
})
</script>

<style scoped>
.waiter-kds {
  background: #1a1a2e;
  min-height: 100vh;
  color: #eee;
  font-family: 'Microsoft YaHei', sans-serif;
  padding: 12px;
}

.wk-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 20px;
  background: #16213e;
  border-radius: 8px;
  margin-bottom: 16px;
}
.wk-title { font-size: 28px; font-weight: bold; }
.wk-clock { font-size: 22px; margin-left: auto; color: #0f0; }
.wk-status { color: #0f0; font-size: 14px; }

.wk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 14px;
}

.wk-order-card {
  background: #16213e;
  border-radius: 10px;
  padding: 16px;
  border: 1px solid #2a3a5e;
}

.wk-order-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #2a3a5e;
}
.wk-table {
  font-size: 26px;
  font-weight: bold;
  color: #ffd700;
}
.wk-order-id {
  color: #888;
  font-size: 13px;
}
.wk-summary {
  margin-left: auto;
  color: #4caf50;
  font-size: 14px;
}

.wk-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.wk-item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1f2b47;
  border-radius: 8px;
  padding: 10px 14px;
  border-left: 4px solid #4caf50;
  transition: all 0.3s;
}
.wk-item-row.urgent {
  border-left-color: #f44336;
  background: #2a1f2f;
}
.wk-item-row.bumped {
  border-left-color: #f00;
  box-shadow: 0 0 10px rgba(255,0,0,0.4);
  animation: pulse-border 1s ease-in-out infinite;
}

.wk-item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.wk-dish {
  font-size: 18px;
  font-weight: 600;
}
.wk-wait {
  color: #aaa;
  font-size: 13px;
}

.wk-item-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.wk-status-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.st-waiting { background: #607d8b; color: #fff; }
.st-cooking { background: #ff9800; color: #000; }
.st-done { background: #4caf50; color: #fff; }
.st-served { background: #888; color: #ccc; }

.bump-n {
  font-weight: bold;
  color: #f00;
}

.wk-empty {
  text-align: center;
  padding: 80px 0;
  font-size: 24px;
  color: #666;
}

@keyframes pulse-border {
  0%, 100% { box-shadow: 0 0 6px rgba(255,0,0,0.3); }
  50% { box-shadow: 0 0 16px rgba(255,0,0,0.7); }
}
</style>
