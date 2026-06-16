<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.consumptionTitle') }}</h3>
      </div>

      <!-- 筛选条件区（门店+订单） -->
      <div class="filter-group" style="margin-bottom: 16px">
        <el-select
          v-model="selectedStoreId"
          :placeholder="$t('common.pleaseSelect')"
          style="width: 200px; margin-right: 16px"
          clearable
          :disabled="isLocked"
          @change="handleStoreChange"
        >
          <el-option
            v-for="store in stores"
            :key="store.id"
            :label="store.name"
            :value="store.id"
          />
        </el-select>

        <el-select
          v-model="selectedOrderId"
          placeholder="选择订单（可选，查看指定订单消耗）"
          style="width: 300px; margin-right: 16px"
          clearable
          @change="loadData"
        >
          <el-option
            v-for="order in orders"
            :key="order.id"
            :label="`订单#${order.id} - ${order.tableNumber || order.orderNo || '外卖'}`"
            :value="order.id"
          />
        </el-select>

        <el-button type="primary" @click="loadData">{{ $t('common.search') }}</el-button>
        <el-button @click="resetFilter">{{ $t('common.reset') }}</el-button>
      </div>

      <el-table
        :data="consumptions"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
        empty-text="暂无消耗记录"
      >
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="storeId" :label="$t('table.store')" width="80" />
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column prop="dishId" label="菜品ID" width="80" />
        <el-table-column label="原料名称" width="150">
          <template #default="{ row }">
            {{ getMaterialName(row.materialId) }}
          </template>
        </el-table-column>
        <el-table-column prop="usedQuantity" label="消耗数量" width="120" />
        <el-table-column prop="createdAt" label="消耗时间" width="180" />
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getAllConsumptions,
  getConsumptionsByOrder,
  getOrdersByStore,
  getAllMaterials,
} from '@/api'
import { useStoreFilter } from '@/composables/useStoreFilter'
import { usePagination } from '@/composables/usePagination'
import { ElMessage } from 'element-plus'

// 使用组合函数
const { stores, selectedStoreId, isLocked, loadStores } = useStoreFilter()

const orders = ref([])
const materials = ref([])
const selectedOrderId = ref(null)

// 分页（复用 usePagination）
const {
  pageNum, pageSize, total, loading,
  data: consumptions,
  load, onPageChange, onSizeChange, resetAndLoad,
} = usePagination(
  (params) => {
    if (selectedOrderId.value) {
      // 按订单查询不分页，但为兼容 usePagination 格式，包装一下
      return getConsumptionsByOrder(selectedOrderId.value).then(res => ({
        total: res.data?.length || 0,
        pages: 1,
        data: res.data || [],
      }))
    }
    return getAllConsumptions({
      storeId: selectedStoreId.value || undefined,
      ...params,
    })
  },
  {},
  10
)

// 获取原料名称（通过materialId映射）
const getMaterialName = (materialId) => {
  if (!materialId) return '-'
  const material = materials.value.find((m) => m.id === materialId)
  return material ? material.name : '-'
}

// 加载当前门店的订单
const loadOrders = async () => {
  if (!selectedStoreId.value) {
    orders.value = []
    return
  }

  try {
    const res = await getOrdersByStore(selectedStoreId.value)
    orders.value = res.data || []
  } catch (_) {
    orders.value = []
  }
}

// 加载所有原料（用于名称映射）
const loadMaterials = async () => {
  try {
    const res = await getAllMaterials()
    materials.value = res.data || []
  } catch (_) {
    materials.value = []
  }
}

// 加载消耗记录数据
const loadData = () => {
  resetAndLoad()
}

// 门店切换事件
const handleStoreChange = () => {
  selectedOrderId.value = null // 清空已选订单
  loadOrders() // 加载新门店的订单
  loadData()
}

// 重置筛选条件
const resetFilter = () => {
  selectedStoreId.value = null
  selectedOrderId.value = null
  loadData()
}

onMounted(async () => {
  await loadStores()
  loadMaterials()
  if (selectedStoreId.value) {
    handleStoreChange()  // 店长自动填充门店后，加载该门店的订单列表
  }
  loadData()
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
