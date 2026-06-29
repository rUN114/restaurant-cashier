<template>
  <div class="queue-board" @click="toggleFull">
    <div v-if="isFS" class="exit-btn" @click.stop="exitFull" title="退出全屏 (ESC)">✕ 退出全屏</div>
    <header class="qb-head">
      <span class="qb-logo">🏪</span>
      <span>等位大屏</span>
      <span class="qb-clock">{{ now }}</span>
    </header>

    <div class="qb-stats">
      <div class="qb-stat free">🟢 {{ data.freeTables }} 空闲</div>
      <div class="qb-stat occ">🔴 {{ data.occupiedTables }} 用餐</div>
      <div class="qb-stat wait">⏳ 预计{{ data.estimatedWait }}分钟</div>
    </div>

    <div class="qb-body">
      <div class="qb-left">
        <h3>🪑 当前等位 · {{ data.waiting?.length || 0 }}桌</h3>
        <div class="qb-list" v-if="data.waiting?.length">
          <div v-for="(q, i) in data.waiting" :key="q.id" :class="['qb-item', { first: i === 0 }]">
            <span class="qb-num">{{ q.queueNumber }}</span>
            <span class="qb-info">{{ q.customerName || '顾客' }} · {{ q.partySize }}人 · {{ q.tablePref }}</span>
            <span class="qb-elapsed">{{ fmtElapsed(q.createdAt) }}</span>
          </div>
        </div>
        <div class="qb-empty" v-else>🎉 无需等位，欢迎光临！</div>
      </div>
      <div class="qb-right">
        <h3>📢 最近叫号</h3>
        <div class="qb-called" v-if="called.length">
          <div v-for="c in called" :key="c.id" class="called-item">
            <span class="called-num">{{ c.queueNumber }}</span>
            <span>{{ c.customerName || '顾客' }} → {{ c.seatedTable || '已入座' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { useFullscreen } from '@/composables/useFullscreen'

const { isFullscreen } = useFullscreen()
const user = useUserStore()
const now = ref('')
const data = reactive({ waiting: [], freeTables: 0, occupiedTables: 0, estimatedWait: 0 })
const called = ref([])
let timer = null

const isFS = ref(false)
const toggleFull = () => {
  if (!document.fullscreenElement) document.documentElement.requestFullscreen().catch(()=>{})
}
const exitFull = () => {
  if (document.fullscreenElement) document.exitFullscreen()
}
onMounted(() => {
  document.addEventListener('fullscreenchange', () => {
    isFS.value = !!document.fullscreenElement
    isFullscreen.value = isFS.value
  })
})
onBeforeUnmount(() => { isFullscreen.value = false; if (document.fullscreenElement) document.exitFullscreen() })

const load = async () => {
  try {
    const storeId = user.roleCode === 'ADMIN' ? undefined : user.storeId
    const res = await request.get('/queue/board', { params: { storeId } })
    if (res.data) Object.assign(data, res.data)
  } catch (_) {}
  try {
    const storeId2 = user.roleCode === 'ADMIN' ? undefined : user.storeId
    const cres = await request.get('/queue/called', { params: { storeId: storeId2 } })
    if (cres.data) called.value = cres.data
  } catch (_) {}
}

const fmtElapsed = (t) => {
  if (!t) return ''
  const m = Math.floor((Date.now() - new Date(t).getTime()) / 60000)
  return m < 1 ? '刚刚' : `${m}分钟`
}

onMounted(() => {
  load()
  timer = setInterval(load, 10000)
  setInterval(() => { now.value = new Date().toLocaleTimeString('zh-CN', { hour12: false }) }, 1000)
})

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.exit-btn {
  position: fixed; bottom: 24px; right: 24px; z-index: 9999;
  background: rgba(255,0,0,0.85); color: #fff; padding: 12px 24px;
  border-radius: 8px; cursor: pointer; font-size: 16px; font-weight: bold;
}

.queue-board {
  background: #0a1628;
  min-height: 100vh;
  color: #e0e0e0;
  font-family: 'Microsoft YaHei', sans-serif;
  padding: 20px 40px;
  cursor: pointer;
  user-select: none;
}
.qb-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 2px solid #1a3050;
  font-size: 28px;
}
.qb-logo { font-size: 36px; }
.qb-clock { margin-left: auto; font-size: 32px; color: #00e676; font-weight: bold; }

.qb-stats {
  display: flex;
  gap: 20px;
  padding: 24px 0;
  font-size: 32px;
}
.qb-stat {
  flex: 1;
  text-align: center;
  padding: 20px;
  background: #111d35;
  border-radius: 12px;
  font-weight: bold;
}
.qb-stat.free { color: #69f0ae; }
.qb-stat.occ { color: #ff5252; }
.qb-stat.wait { color: #ffd740; }

.qb-body { display: flex; gap: 30px; }
.qb-left, .qb-right { flex: 1; }
.qb-body h3 { font-size: 22px; margin-bottom: 12px; }

.qb-list { display: flex; flex-direction: column; gap: 10px; }
.qb-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #111d35;
  border-radius: 10px;
  font-size: 20px;
  border-left: 4px solid #1a3050;
}
.qb-item.first { border-left-color: #ffd740; background: #1a2235; animation: glow 2s infinite; }
.qb-num { font-size: 28px; font-weight: bold; color: #ffd740; min-width: 80px; }
.qb-info { flex: 1; }
.qb-elapsed { color: #8899aa; }

.qb-empty { text-align: center; padding: 60px; font-size: 24px; color: #69f0ae; }

.qb-called { display: flex; flex-wrap: wrap; gap: 10px; }
.called-item {
  padding: 12px 20px;
  background: #1a2e1a;
  border-radius: 8px;
  font-size: 18px;
}
.called-num { font-size: 24px; font-weight: bold; color: #69f0ae; margin-right: 8px; }

.qb-right {
  background: #111d35;
  border-radius: 12px;
  padding: 20px;
}

@keyframes glow {
  0%, 100% { box-shadow: 0 0 10px rgba(255,215,64,0.3); }
  50% { box-shadow: 0 0 20px rgba(255,215,64,0.6); }
}
</style>
