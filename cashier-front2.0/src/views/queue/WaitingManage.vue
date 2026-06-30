<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>🎫 等位管理</h3>
        <div style="display:flex;gap:12px;align-items:center">
          <el-select v-if="isAdmin" v-model="selectedStoreId" placeholder="选择门店" size="small" style="width:160px" @change="load">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-button type="primary" size="small" @click="dialogVisible = true">+ 取号</el-button>
        </div>
      </div>

      <el-row :gutter="20" style="margin-bottom:16px">
        <el-col :xs="24" :sm="12">
          <div class="stat-mini waiting">🕐 等待中 <b>{{ waitingList.length }}</b> 桌</div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div class="stat-mini free">🟢 空闲 <b>{{ freeTables }}</b> 桌</div>
        </el-col>
      </el-row>

      <el-table :data="waitingList" border stripe size="small">
        <el-table-column prop="queueNumber" label="排号" width="90" />
        <el-table-column prop="customerName" label="顾客" />
        <el-table-column prop="partySize" label="人数" width="60" />
        <el-table-column prop="tablePref" label="偏好" width="80" />
        <el-table-column label="等待时间" width="100">
          <template #default="{ row }">{{ fmtElapsed(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="call(row)">叫号入座</el-button>
            <el-button link type="danger" @click="doCancel(row.id)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="取号" width="360px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="顾客姓名">
          <el-input v-model="form.customerName" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="人数">
          <el-input-number v-model="form.partySize" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="桌型偏好">
          <el-select v-model="form.tablePref">
            <el-option label="小桌" value="小桌" />
            <el-option label="中桌" value="中桌" />
            <el-option label="包厢" value="包厢" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确认取号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="callVisible" title="安排入座" width="300px">
      <el-form-item label="桌号">
        <el-input v-model="seatTable" placeholder="如：A5" />
      </el-form-item>
      <template #footer>
        <el-button @click="callVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCall">确认入座</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { getAllStores } from '@/api'
import { ElMessage } from 'element-plus'

const user = useUserStore()
const isAdmin = computed(() => user.roleCode === 'ADMIN')
const stores = ref([])
const selectedStoreId = ref(null)
const waitingList = ref([])
const freeTables = ref(0)
const dialogVisible = ref(false)
const callVisible = ref(false)
const callId = ref(null)
const seatTable = ref('')

const form = ref({ customerName: '', phone: '', partySize: 2, tablePref: '小桌' })

const effectiveStoreId = () => isAdmin.value ? selectedStoreId.value : user.storeId

const load = async () => {
  const sid = effectiveStoreId()
  const [qr, br] = await Promise.all([
    request.get('/queue/list', { params: { storeId: sid } }),
    request.get('/queue/board', { params: { storeId: sid } })
  ])
  waitingList.value = qr.data || []
  freeTables.value = br.data?.freeTables || 0
}

const submit = async () => {
  try {
    await request.post('/queue/take', { ...form.value, storeId: effectiveStoreId() || user.storeId })
    ElMessage.success('取号成功')
    dialogVisible.value = false
    form.value = { customerName: '', phone: '', partySize: 2, tablePref: '小桌' }
    load()
  } catch (_) { ElMessage.error('取号失败') }
}

const call = (row) => {
  callId.value = row.id
  seatTable.value = ''
  callVisible.value = true
}

const confirmCall = async () => {
  try {
    await request.put(`/queue/call/${callId.value}`, null, { params: { table: seatTable.value || undefined } })
    ElMessage.success('已叫号入座')
    callVisible.value = false
    load()
  } catch (_) { ElMessage.error('操作失败') }
}

const doCancel = async (id) => {
  try {
    await request.put(`/queue/cancel/${id}`)
    ElMessage.success('已取消')
    load()
  } catch (_) { ElMessage.error('操作失败') }
}

const fmtElapsed = (t) => {
  if (!t) return ''
  const m = Math.floor((Date.now() - new Date(t).getTime()) / 60000)
  return m < 1 ? '刚刚' : `${m}分钟`
}

onMounted(async () => {
  if (isAdmin.value) {
    try { const res = await getAllStores(); stores.value = res.data || []; selectedStoreId.value = stores.value[0]?.id || null } catch (_) {}
  }
  load()
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.stat-mini { padding: 12px 16px; border-radius: 8px; font-size: 16px; }
.stat-mini.waiting { background: #fff8e1; color: #e6a23c; }
.stat-mini.free { background: #e8f5e9; color: #2e7d32; }
</style>
