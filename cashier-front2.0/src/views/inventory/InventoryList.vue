<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.inventoryTitle') }}</h3>
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
        <el-table-column prop="currentStock" label="当前库存" width="120" />
        <el-table-column prop="costPrice" label="成本价(元)" width="120" />
        <el-table-column label="预警值" width="100">
          <template #default="{ row }">
            {{ getMaterialWarnStock(row.materialId) }}
          </template>
        </el-table-column>
        <el-table-column label="库存状态" width="100">
          <template #default="{ row }">
            <el-tag :type="isStockLow(row) ? 'danger' : 'success'" size="small">
              {{ isStockLow(row) ? $t('table.stockLow') : $t('table.stockNormal') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最后更新时间" width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="handleStockIn(row)">进货</el-button>
            <el-button link type="warning" @click="handleStockOut(row)">出库</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 库存调整弹窗 -->
    <el-dialog :title="stockDialogTitle" v-model="stockDialogVisible" width="450px">
      <el-form :model="stockForm" label-width="80px">
        <el-form-item label="原料">
          <span>{{ stockForm.materialName }}</span>
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input v-model.number="stockForm.quantity" type="number" :step="0.001"
            :placeholder="stockForm.type === 'in' ? '请输入进货数量' : '请输入出库数量'" />
        </el-form-item>
        <template v-if="stockForm.type === 'in'">
          <el-form-item label="采购单价(元)" required>
            <el-input v-model.number="stockForm.unitPrice" type="number" :step="0.01" placeholder="默认取当前成本价" />
          </el-form-item>
          <el-form-item label="供应商">
            <el-input v-model="stockForm.supplier" placeholder="供应商名称（选填）" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStockAdjust">{{ stockForm.type === 'in' ? '确认进货' : '确认出库' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getInventoryByStore,
  getAllMaterials,
  adjustInventory,
} from '@/api'
import { useStoreFilter } from '@/composables/useStoreFilter'
import { ElMessage } from 'element-plus'

const { stores, selectedStoreId, isLocked, loadStores, getStoreName } = useStoreFilter()

const inventory = ref([])
const materials = ref([])
const loading = ref(false)

// === 库存调整 ===
const stockDialogVisible = ref(false)
const stockDialogTitle = ref('')
const stockForm = ref({
  materialId: null,
  materialName: '',
  storeId: null,
  type: 'in',
  quantity: null,
  unitPrice: null,
  supplier: '',
})

const handleStockIn = (row) => {
  stockForm.value = {
    materialId: row.materialId,
    materialName: getMaterialName(row.materialId),
    storeId: row.storeId || selectedStoreId.value,
    type: 'in',
    quantity: null,
    unitPrice: row.costPrice || null,   // 默认填入当前成本价
    supplier: '',
  }
  stockDialogTitle.value = '进货 - ' + getMaterialName(row.materialId)
  stockDialogVisible.value = true
}

const handleStockOut = (row) => {
  stockForm.value = {
    materialId: row.materialId,
    materialName: getMaterialName(row.materialId),
    storeId: row.storeId || selectedStoreId.value,
    type: 'out',
    quantity: null,
    unitPrice: null,
    supplier: '',
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
    const payload = {
      storeId: stockForm.value.storeId,
      materialId: stockForm.value.materialId,
      quantity: stockForm.value.type === 'in' ? qty : -qty,
    }
    if (stockForm.value.type === 'in') {
      if (stockForm.value.unitPrice) payload.unitPrice = stockForm.value.unitPrice
      if (stockForm.value.supplier) payload.supplier = stockForm.value.supplier
    }
    await adjustInventory(payload)
    ElMessage.success(stockForm.value.type === 'in' ? '进货成功' : '出库成功')
    stockDialogVisible.value = false
    loadInventory()
  } catch (_) {
    ElMessage.error('操作失败')
  }
}

// 获取原料名称
const getMaterialName = (materialId) => {
  if (!materialId) return '-'
  const material = materials.value.find((m) => m.id === materialId)
  return material ? material.name : '-'
}

// 获取原料单位
const getMaterialUnit = (materialId) => {
  if (!materialId) return '-'
  const material = materials.value.find((m) => m.id === materialId)
  return material ? material.unit || '-' : '-'
}

// 获取原料预警值
const getMaterialWarnStock = (materialId) => {
  if (!materialId) return '-'
  const material = materials.value.find((m) => m.id === materialId)
  return material ? material.warningStock || '-' : '-'
}

// 判断库存是否不足
const isStockLow = (row) => {
  const material = materials.value.find((m) => m.id === row.materialId)
  if (!material || !material.warningStock) return false
  return Number(row.currentStock) < Number(material.warningStock)
}

// 加载所有原料
const loadMaterials = async () => {
  try {
    const res = await getAllMaterials()
    materials.value = res.data || []
  } catch (_) {
    materials.value = []
  }
}

// 加载库存数据
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

onMounted(async () => {
  await loadStores()
  loadMaterials()
  loadInventory()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  align-items: center;
}
</style>
