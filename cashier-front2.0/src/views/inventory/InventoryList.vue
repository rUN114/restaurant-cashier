<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.inventoryTitle') }}</h3>
        <el-button type="success" size="small" @click="exportInventory">📥 导出</el-button>
      </div>

      <!-- 门店筛选器 -->
      <div class="filter-group" style="margin-bottom: 16px">
        <el-select
          v-model="selectedStoreId"
          placeholder="请选择门店查看库存"
          style="width: 200px; margin-right: 16px"
          clearable
          :disabled="isLocked"
          @change="loadInventory"
        >
          <el-option
            v-for="store in stores"
            :key="store.id"
            :label="store.name"
            :value="store.id"
          />
        </el-select>
        <span style="color: #909399">
          {{ selectedStoreId ? `已选择: ${getStoreName(selectedStoreId)}` : '请先选择门店' }}
        </span>
        <el-button
          v-if="isAdmin"
          type="warning"
          size="small"
          style="margin-left: 12px"
          :loading="backfillLoading"
          @click="handleBackfill"
        >
          回填成本价
        </el-button>
      </div>

      <el-table
        :data="inventory"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
        :empty-text="selectedStoreId ? '该门店暂无原料库存记录' : '请先选择门店'"
      >
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="storeId" :label="$t('table.store')" width="80" />
        <el-table-column label="原料名称" width="150">
          <template #default="{ row }">
            {{ getMaterialName(row.materialId) }}
          </template>
        </el-table-column>
        <el-table-column label="单位" width="80">
          <template #default="{ row }">
            {{ getMaterialUnit(row.materialId) }}
          </template>
        </el-table-column>
        <el-table-column prop="currentStock" label="当前库存" width="100" />
        <el-table-column label="成本价" width="130">
          <template #default="{ row }">
            <span v-if="row.costPrice">
              ¥{{ row.costPrice }}{{ costPriceUnit(row) }}
            </span>
            <span v-else style="color:#999">-</span>
          </template>
        </el-table-column>
        <el-table-column label="预警值" width="80">
          <template #default="{ row }">
            {{ getMaterialWarnStock(row.materialId) }}
          </template>
        </el-table-column>
        <el-table-column label="库存状态" width="90">
          <template #default="{ row }">
            <el-tag :type="isStockLow(row) ? 'danger' : 'success'" size="small">
              {{ isStockLow(row) ? $t('table.stockLow') : $t('table.stockNormal') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最后更新" width="160" />
        <el-table-column label="快捷进货" width="200" fixed="right">
          <template #default="{ row }">
            <div class="quick-inline">
              <div class="quick-row">
                <el-input
                  v-model.number="quickForms[row.materialId]"
                  type="number"
                  :step="0.001"
                  size="small"
                  :placeholder="getPurchaseUnit(row.materialId) || '数量'"
                  style="width: 72px"
                  :data-mid="row.materialId"
                  @focus="onQuickFocus(row)"
                  @keyup.enter="quickStockIn(row)"
                />
                <el-button
                  type="primary"
                  size="small"
                  :disabled="!quickForms[row.materialId]"
                  @click="quickStockIn(row)"
                >✓</el-button>
                <el-button link type="warning" size="small" @click="handleStockOut(row)">出库</el-button>
              </div>
              <div class="history-hint" v-if="priceHistory[row.materialId]?.length">
                <span
                  v-for="(p, i) in priceHistory[row.materialId]"
                  :key="i"
                  :class="['price-chip', { active: quickPrices[row.materialId] === p.unitPrice }]"
                  :title="p.supplier ? `${p.supplier} · ${p.purchaseTime?.slice(0,10)}` : p.purchaseTime?.slice(0,10)"
                  @click="selectPrice(row, p.unitPrice)"
                >
                  ¥{{ p.unitPrice }}
                </span>
              </div>
              <div class="history-hint muted" v-else-if="priceLoading[row.materialId]">
                加载中...
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 出库弹窗 -->
    <el-dialog :title="stockDialogTitle" v-model="stockDialogVisible" width="450px">
      <el-form :model="stockForm" label-width="80px">
        <el-form-item label="原料">
          <span>{{ stockForm.materialName }}</span>
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input v-model.number="stockForm.quantity" type="number" :step="0.001"
            placeholder="请输入出库数量" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStockAdjust">确认出库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getInventoryByStore,
  getAllMaterials,
  adjustInventory,
  getPurchaseHistory,
  backfillCostPrice,
} from '@/api'
import { useStoreFilter } from '@/composables/useStoreFilter'
import { createLookup } from '@/composables/useLookup'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exportExcel } from '@/composables/useExcel'

const { stores, selectedStoreId, isLocked, loadStores, getStoreName } = useStoreFilter()
const userStore = useUserStore()
const isAdmin = computed(() => {
  const role = userStore.role || ''
  return role === 'ADMIN' || role === 'STORE_ADMIN'
})

const inventory = ref([])
const materials = ref([])
const loading = ref(false)
const backfillLoading = ref(false)

const handleBackfill = async () => {
  try {
    await ElMessageBox.confirm(
      '将从历史采购记录中取最近一次单价，填充所有空缺的成本价。确定继续？',
      '回填成本价',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }
  backfillLoading.value = true
  try {
    const res = await backfillCostPrice()
    const data = res.data || {}
    ElMessage.success(`回填完成：共 ${data.total} 条，更新 ${data.updated} 条，跳过 ${data.skipped} 条`)
    loadInventory()
  } catch (_) {
    ElMessage.error('回填失败')
  } finally {
    backfillLoading.value = false
  }
}

// 快捷进货：每个原料一个数量输入 + 选定单价
const quickForms = reactive({})
const quickPrices = reactive({})
const priceHistory = reactive({})
const priceLoading = reactive({})

const onQuickFocus = async (row) => {
  if (priceHistory[row.materialId] || priceLoading[row.materialId]) return
  if (!selectedStoreId.value) return
  priceLoading[row.materialId] = true
  try {
    const res = await getPurchaseHistory(selectedStoreId.value, row.materialId)
    priceHistory[row.materialId] = res.data || []
  } catch (_) {
    priceHistory[row.materialId] = []
  } finally {
    priceLoading[row.materialId] = false
  }
}

const selectPrice = (row, price) => {
  quickPrices[row.materialId] = price
  // 如果还没填数量，自动聚焦数量输入框
  if (!quickForms[row.materialId]) {
    const input = document.querySelector(`[data-mid="${row.materialId}"] input`)
    if (input) input.focus()
  }
}

const quickStockIn = async (row) => {
  const qty = quickForms[row.materialId]
  if (!qty || qty <= 0) {
    ElMessage.warning('请输入有效数量')
    return
  }
  const price = quickPrices[row.materialId]
    || row.costPrice
    || (priceHistory[row.materialId]?.[0]?.unitPrice)
    || null
  const supplier = priceHistory[row.materialId]?.[0]?.supplier || ''
  try {
    await adjustInventory({
      storeId: row.storeId || selectedStoreId.value,
      materialId: row.materialId,
      quantity: qty,
      unitPrice: price,
      supplier,
    })
    ElMessage.success(`进货成功：${getMaterialName(row.materialId)} +${qty}`)
    quickForms[row.materialId] = null
    quickPrices[row.materialId] = null
    // 刷新历史价
    priceHistory[row.materialId] = null
    loadInventory()
  } catch (_) {
    ElMessage.error('进货失败')
  }
}

// === 出库弹窗 ===
const stockDialogVisible = ref(false)
const stockDialogTitle = ref('')
const stockForm = ref({
  materialId: null,
  materialName: '',
  storeId: null,
  type: 'out',
  quantity: null,
})

const handleStockOut = (row) => {
  stockForm.value = {
    materialId: row.materialId,
    materialName: getMaterialName(row.materialId),
    storeId: row.storeId || selectedStoreId.value,
    type: 'out',
    quantity: null,
  }
  stockDialogTitle.value = '出库 - ' + getMaterialName(row.materialId)
  stockDialogVisible.value = true
}

const submitStockAdjust = async () => {
  const qty = stockForm.value.quantity
  if (!qty || qty <= 0) {
    ElMessage.warning('请输入有效数量')
    return
  }
  try {
    await adjustInventory({
      storeId: stockForm.value.storeId,
      materialId: stockForm.value.materialId,
      quantity: -qty,
    })
    ElMessage.success('出库成功')
    stockDialogVisible.value = false
    loadInventory()
  } catch (_) {
    ElMessage.error('操作失败')
  }
}

// 获取原料名称/单位/预警值
const getMaterialName = createLookup(materials, 'id', 'name')
const getMaterialUnit = createLookup(materials, 'id', 'unit')
const getPurchaseUnit = createLookup(materials, 'id', 'purchaseUnit')
const getMaterialWarnStock = createLookup(materials, 'id', 'warningStock')

const costPriceUnit = (row) => {
  const pu = getPurchaseUnit(row.materialId)
  return (pu && pu !== 'null') ? '/' + pu : ''
}

const isStockLow = (row) => {
  const material = materials.value.find((m) => m.id === row.materialId)
  if (!material || !material.warningStock) return false
  return Number(row.currentStock) < Number(material.warningStock)
}

const loadMaterials = async () => {
  try {
    const res = await getAllMaterials()
    materials.value = res.data || []
  } catch (_) {
    materials.value = []
  }
}

const loadInventory = async () => {
  if (!selectedStoreId.value) {
    inventory.value = []
    return
  }
  loading.value = true
  try {
    const res = await getInventoryByStore(selectedStoreId.value)
    inventory.value = res.data || []
  } catch (_) {
    inventory.value = []
    ElMessage.error('加载库存数据失败')
  } finally {
    loading.value = false
  }
}

const exportInventory = () => {
  const cols = [
    { label: 'ID', key: 'id' },
    { label: '门店', key: 'storeId' },
    { label: '原料', key: 'materialName' },
    { label: '单位', key: 'unit' },
    { label: '当前库存', key: 'currentStock' },
    { label: '成本价', key: 'costPrice' },
    { label: '预警值', key: 'warningStock' },
    { label: '更新时间', key: 'updatedAt' },
  ]
  const rows = inventory.value.map(r => ({
    ...r,
    materialName: getMaterialName(r.materialId) || '',
    unit: getMaterialUnit(r.materialId) || '',
    warningStock: getMaterialWarnStock(r.materialId) || '',
  }))
  exportExcel(rows, cols, `库存报表_${new Date().toISOString().slice(0,10)}`)
}

onMounted(async () => {
  await loadStores()
  loadMaterials()
  loadInventory()
})
</script>

<style scoped>
.quick-inline {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.quick-row {
  display: flex;
  align-items: center;
  gap: 4px;
}
.history-hint {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.price-chip {
  background: #ecf5ff;
  color: #409eff;
  font-size: 11px;
  padding: 0 4px;
  border-radius: 3px;
  cursor: pointer;
}
.price-chip:hover {
  background: #409eff;
  color: #fff;
}
.price-chip.active {
  background: #409eff;
  color: #fff;
  box-shadow: 0 0 0 2px #409eff55;
}
.history-hint.muted {
  font-size: 11px;
  color: #bbb;
}
</style>
