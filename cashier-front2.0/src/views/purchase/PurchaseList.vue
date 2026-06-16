<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.purchaseTitle') }}</h3>
      </div>

      <!-- 门店筛选器（和库存页面完全一致） -->
      <div class="filter-group" style="margin-bottom: 16px">
        <el-select
          v-model="selectedStoreId"
          placeholder="请选择门店查看采购记录"
          style="width: 200px; margin-right: 16px"
          clearable
          :disabled="isLocked"
          @change="loadPurchases"
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

      <!-- 采购表格（结构排版完全和库存页面一致） -->
      <el-table
        :data="purchases"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
        :empty-text="selectedStoreId ? '该门店暂无采购记录' : '请先选择门店'"
      >
        <el-table-column prop="id" label="采购ID" width="70" />
        <el-table-column prop="storeId" :label="$t('table.store')" width="80" />

        <el-table-column label="原料名称" width="150">
          <template #default="{ row }">
            {{ getMaterialName(row.materialId) }}
          </template>
        </el-table-column>

        <el-table-column prop="quantity" label="采购数量" width="110" />
        <el-table-column prop="unitPrice" label="单价(元)" width="110" />
        <el-table-column prop="totalPrice" label="总价(元)" width="110" />
        <el-table-column prop="supplier" label="供应商" width="160" />
        <el-table-column prop="purchaseTime" label="采购时间" width="180" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
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
import { getPurchasesByStorePage, getAllMaterials } from '@/api'
import { useStoreFilter } from '@/composables/useStoreFilter'
import { usePagination } from '@/composables/usePagination'
import { ElMessage } from 'element-plus'

// 门店筛选
const { stores, selectedStoreId, isLocked, loadStores, getStoreName } = useStoreFilter()

// 服务端分页（复用 usePagination，修复 total 始终等于当前页条数的 bug）
const { pageNum, pageSize, total, loading, data: purchases, load, onPageChange, onSizeChange, resetAndLoad } =
  usePagination(
    (params) => {
      if (!selectedStoreId.value) {
        // 未选门店时返回空 Promise，避免无效请求
        return Promise.resolve({ data: [], total: 0, pages: 0 })
      }
      return getPurchasesByStorePage(selectedStoreId.value, params)
    },
    {},
    10
  )

const materials = ref([])

// 获取原料名称
const getMaterialName = (materialId) => {
  if (!materialId) return '-'
  const material = materials.value.find((m) => m.id === materialId)
  return material ? material.name : '-'
}

// 加载原料列表（用于显示原料名称）
const loadMaterials = async () => {
  try {
    const res = await getAllMaterials()
    materials.value = res.data || []
  } catch (_) {
    materials.value = []
  }
}

// 门店变更时重新加载
const loadPurchases = () => {
  if (!selectedStoreId.value) {
    purchases.value = []
    total.value = 0
    return
  }
  resetAndLoad()
}

onMounted(async () => {
  await loadStores()
  loadMaterials()
  loadPurchases()
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
