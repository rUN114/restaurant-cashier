<template>
  <el-popover
    placement="bottom-end"
    :width="320"
    trigger="click"
    @show="load"
  >
    <template #reference>
      <el-badge :value="count" :hidden="count === 0" :max="99" class="bell-badge">
        <el-button link class="bell-btn">🔔</el-button>
      </el-badge>
    </template>

    <div class="notify-panel">
      <h4>通知中心</h4>
      <div v-if="list.length === 0" style="text-align:center;padding:20px;color:#999">
        ✅ 一切正常
      </div>
      <div v-for="n in list" :key="n.id" :class="['notify-item', n.level]" @click="jump(n)">
        <span class="notify-icon">{{ n.icon }}</span>
        <div class="notify-body">
          <div class="notify-text">{{ n.text }}</div>
          <div class="notify-time">{{ n.time }}</div>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getLowStock } from '@/api'
import { getKitchenBoard, getWaiterBoard } from '@/api/kds'

const router = useRouter()
const user = useUserStore()

const list = ref([])
let timer = null

const count = computed(() => list.value.length)

const load = async () => {
  const items = []
  const storeId = user.storeId

  // 库存预警（管理员storeId为null时查全部）
  if (['ADMIN', 'STORE_ADMIN', 'CHEF', 'PURCHASER'].includes(user.roleCode)) {
    try {
      const res = await getLowStock(storeId || undefined)
      const stocks = res.data || []
      for (const s of stocks) {
        items.push({
          id: 'stock-' + s.materialId,
          icon: '📦',
          text: `「${s.name}」库存不足 (${s.currentStock}${s.unit})`,
          time: '',
          level: 'warn',
          path: '/inventory',
        })
      }
    } catch (_) {}
  }

  // 厨房等餐超时 (>25分钟)  仅厨师可见
  if (user.roleCode === 'CHEF') {
    try {
      const res = await getKitchenBoard()
      const data = res.data || {}
      const allItems = [...(data.WAITING || []), ...(data.COOKING || [])]
      for (const item of allItems) {
        if (item.waitSeconds > 1500) { // 25分钟
          const min = Math.floor(item.waitSeconds / 60)
          items.push({
            id: 'longwait-' + item.id,
            icon: '⏰',
            text: `「${item.dishName}」${item.tableNumber}桌 等餐${min}分钟`,
            time: '',
            level: 'danger',
            path: '/kds',
          })
        }
      }
    } catch (_) {}
  }

  // 待上菜堆积 (>3道)  仅服务员可见
  if (user.roleCode === 'WAITER') {
    try {
      const res = await getWaiterBoard()
      const orders = res.data || []
      let doneTotal = 0
      for (const o of orders) {
        doneTotal += o.items.filter(i => i.kitchenStatus === 'DONE').length
      }
      if (doneTotal >= 3) {
        items.push({
          id: 'done-pile',
          icon: '🍽️',
          text: `${doneTotal}道菜待上菜，请及时上菜`,
          time: '',
          level: 'warn',
          path: '/kds',
        })
      }
    } catch (_) {}
  }

  list.value = items
}

const basePath = computed(() => {
  const map = { ADMIN: '/admin', STORE_ADMIN: '/manager', CASHIER: '/cashier', WAITER: '/waiter', CHEF: '/chef', PURCHASER: '/buyer' }
  return map[user.roleCode] || '/'
})

const jump = (n) => {
  if (n.path) router.push(basePath.value + n.path)
}

onMounted(() => {
  load()
  timer = setInterval(load, 30000) // 30秒轮询
})

onBeforeUnmount(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.bell-btn { font-size: 22px; padding: 4px 8px; }
.bell-badge { margin-right: 8px; cursor: pointer; }
.notify-panel h4 { margin: 0 0 12px; font-size: 15px; border-bottom: 1px solid #ebeef5; padding-bottom: 8px; }
.notify-item { display: flex; gap: 10px; padding: 10px 6px; border-bottom: 1px solid #f5f5f5; cursor: pointer; border-radius: 6px; transition: background 0.2s; }
.notify-item:hover { background: #f5f7fa; }
.notify-item.warn { border-left: 3px solid #e6a23c; }
.notify-item.danger { border-left: 3px solid #f56c6c; }
.notify-item.info { border-left: 3px solid #409eff; }
.notify-icon { font-size: 20px; flex-shrink: 0; }
.notify-body { flex: 1; }
.notify-text { font-size: 13px; line-height: 1.5; }
.notify-time { font-size: 11px; color: #999; margin-top: 2px; }
</style>
