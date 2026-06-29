<template>
  <div class="kds-container">
    <!-- 顶栏 -->
    <div class="kds-header">
      <span class="kds-title">🔪 后厨看板</span>
      <span class="kds-clock">{{ time }}</span>
      <span class="kds-status">● 在线</span>
    </div>

    <!-- 双列 -->
    <div class="kds-columns">
      <!-- 待制作 -->
      <div class="kds-col waiting-col">
        <div class="col-title">📋 待制作 ({{ list.WAITING?.length || 0 }})</div>
        <div
          v-for="item in list.WAITING"
          :key="item.id"
          :class="cardClass(item)"
          :style="cardStyle(item)"
        >
          <div class="card-badge" v-if="isNew(item)">加菜</div>
          <div class="card-table">{{ item.tableNumber }}</div>
          <div class="card-dish">{{ item.dishName }}</div>
          <div class="card-remark" v-if="item.remark">📝 {{ item.remark }}</div>
          <div class="card-wait">{{ formatWait(item.waitSeconds) }}</div>
          <div style="display:flex;gap:6px">
            <el-button type="primary" size="small" @click="start(item.id)">开始</el-button>
            <el-button type="success" size="small" @click="done(item.id)">完成</el-button>
          </div>
        </div>
      </div>

      <!-- 制作中 -->
      <div class="kds-col cooking-col">
        <div class="col-title">🔥 制作中 ({{ list.COOKING?.length || 0 }})</div>
        <div
          v-for="item in list.COOKING"
          :key="item.id"
          :class="cardClass(item)"
          :style="cardStyle(item)"
        >
          <div class="card-table">{{ item.tableNumber }}</div>
          <div class="card-dish">{{ item.dishName }}</div>
          <div class="card-remark" v-if="item.remark">📝 {{ item.remark }}</div>
          <div class="card-bump" v-if="item.bumpCount">⚡催单 ×{{ item.bumpCount }}</div>
          <div class="card-wait">{{ formatWait(item.waitSeconds) }}</div>
          <el-button type="success" size="small" @click="done(item.id)">完成</el-button>
        </div>
      </div>
    </div>

    <!-- 待上菜 -->
    <div class="done-bar" v-if="list.DONE?.length">
      <span class="done-title">✅ 待上菜 ({{ list.DONE.length }})</span>
      <span v-for="item in list.DONE" :key="item.id" class="done-chip">
        {{ item.tableNumber }}·{{ item.dishName }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { getKitchenBoard, startCooking, doneCooking } from '@/api/kds'
import { useVoice } from '@/composables/useVoice'
const voice = useVoice()
let prevBumpedIds = new Set()

const list = reactive({ WAITING: [], COOKING: [], DONE: [] })
const time = ref('')
const socket = ref(null)
let pollTimer = null
let clockTimer = null
const newItemIds = ref(new Set())

const poll = async () => {
  try {
    const res = await getKitchenBoard()
    if (res.data) {
      // 检测新菜品（闪烁触发）
      const oldIds = new Set([...list.WAITING, ...list.COOKING, ...list.DONE].map(i => i.id))
      const newIds = new Set()
      Object.values(res.data).flat().forEach(i => newIds.add(i.id))
      for (const id of newIds) { if (!oldIds.has(id)) newItemIds.value.add(id) }
      // 检测新催单 → 语音播报
      const bumped = Object.values(res.data).flat().filter(i => i.bumpCount > 0)
      for (const b of bumped) {
        if (!prevBumpedIds.has(b.id)) {
          voice.speak(`${b.tableNumber}桌催单${b.dishName}`)
        }
      }
      prevBumpedIds = new Set(bumped.map(b => b.id))
      list.WAITING = res.data.WAITING || []
      list.COOKING = res.data.COOKING || []
      list.DONE = res.data.DONE || []
    }
  } catch (e) {}
}

const start = async (id) => { await startCooking(id); poll() }
const done = async (id) => { await doneCooking(id); poll() }
const formatWait = (s) => { const m = Math.floor(s / 60); return m > 0 ? `${m}分${s % 60}秒` : `${s}秒` }
const isNew = (item) => newItemIds.value.has(item.id)

const cardClass = (item) => ({
  'kds-card': true,
  'just-added': isNew(item),
  'bumped': item.bumpCount > 0,
  'urgent': item.waitSeconds > 600,
  'warn': item.waitSeconds > 300 && item.waitSeconds <= 600,
})

const cardStyle = (item) => {
  if (item.bumpCount > 0) return { borderColor: '#f00', boxShadow: '0 0 12px rgba(255,0,0,0.5)' }
  if (item.waitSeconds > 600) return { borderColor: '#f56c6c' }
  return {}
}

// WebSocket
const connectWS = () => {
  try {
    const ws = new WebSocket(`ws://${window.location.host}/ws`)
    ws.onmessage = () => poll()
    ws.onclose = () => setTimeout(connectWS, 3000)
    socket.value = ws
  } catch (e) {}
}

onMounted(() => {
  poll()
  pollTimer = setInterval(poll, 5000)
  clockTimer = setInterval(() => {
    const d = new Date()
    time.value = d.toLocaleTimeString('zh-CN', { hour12: false })
    list.WAITING.forEach(i => i.waitSeconds = (Date.now() - new Date(i.createdAt).getTime()) / 1000)
    list.COOKING.forEach(i => i.waitSeconds = (Date.now() - new Date(i.createdAt).getTime()) / 1000)
  }, 1000)
  connectWS()
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  clearInterval(clockTimer)
  socket.value?.close()
})
</script>

<style scoped>
.kds-container { background: #1a1a2e; min-height: 100vh; color: #eee; font-family: 'Microsoft YaHei',sans-serif; padding: 12px; }
.kds-header { display: flex; align-items: center; gap: 24px; padding: 12px 20px; background: #16213e; border-radius: 8px; margin-bottom: 12px; }
.kds-title { font-size: 28px; font-weight: bold; }
.kds-clock { font-size: 22px; margin-left: auto; color: #0f0; }
.kds-status { color: #0f0; font-size: 14px; }

.kds-columns { display: flex; gap: 12px; flex: 1; }
.kds-col { flex: 1; background: #16213e; border-radius: 8px; padding: 12px; min-height: 60vh; }
.col-title { font-size: 22px; font-weight: bold; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px solid #333; }

.kds-card { background: #1f2b47; border-radius: 8px; padding: 16px; margin-bottom: 10px; border-left: 4px solid #4caf50; position: relative; transition: all 0.3s; }
.kds-card.warn { border-left-color: #ff9800; }
.kds-card.urgent { border-left-color: #f44336; }
.kds-card.bumped { border-left-color: #f00; animation: pulse 1s ease-in-out infinite; }
.kds-card.just-added { animation: flash 0.5s ease-in-out 5; }

.card-badge { position: absolute; top: 4px; right: 8px; background: #607d8b; color: #fff; font-size: 11px; padding: 1px 6px; border-radius: 4px; }
.card-table { font-size: 32px; font-weight: bold; }
.card-dish { font-size: 20px; margin: 4px 0; }
.card-remark { color: #ff9800; font-size: 15px; font-weight: bold; margin: 2px 0; padding: 2px 6px; background: rgba(255,152,0,0.1); border-radius: 4px; display: inline-block; }
.card-bump { color: #f00; font-size: 18px; font-weight: bold; margin: 4px 0; animation: pulse 0.5s infinite; }
.card-wait { color: #aaa; font-size: 14px; margin-bottom: 8px; }

.done-bar { background: #16213e; border-radius: 8px; padding: 10px 16px; margin-top: 10px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.done-title { font-size: 16px; color: #4caf50; }
.done-chip { background: #1f2b47; padding: 4px 12px; border-radius: 6px; font-size: 14px; }

@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }
@keyframes flash { 0%,100% { opacity: 1; } 50% { opacity: 0.3; background: #4caf50; } }
</style>
