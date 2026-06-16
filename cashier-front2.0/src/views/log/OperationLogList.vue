<template>
  <div class="page-container">
    <el-card>
      <h3>{{ $t('pages.operationLogTitle') }}</h3>
      <div style="margin-bottom:16px; display:flex; gap:16px; align-items:center;">
        <el-select
          v-if="isAdmin"
          v-model="filters.storeId"
          :placeholder="$t('common.pleaseSelect')"
          clearable
          @change="onFilterChange"
          style="width: 160px;"
        >
          <el-option :label="$t('common.pleaseSelect')" :value="undefined" />
          <el-option
            v-for="store in storeList"
            :key="store.id"
            :label="store.name"
            :value="store.id"
          />
        </el-select>
        <el-button type="primary" @click="resetAndLoad">刷新</el-button>
      </div>

      <el-table :data="data" border stripe v-loading="loading">
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="operatorId" label="操作人ID" width="80" />
        <el-table-column prop="storeId" :label="$t('table.store')" width="80" />
        <el-table-column prop="targetType" label="对象类型" width="100" />
        <el-table-column prop="targetId" label="对象ID" width="100" />
        <el-table-column prop="action" label="动作" />
        <el-table-column prop="detail" label="详细" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="操作时间" width="180" />
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
import { ref, computed, onMounted } from 'vue'
import { getOperationLogsPage, getAllStores } from '@/api'
import { useUserStore } from '@/stores/user'
import { usePagination } from '@/composables/usePagination'

const user = useUserStore()
const isAdmin = computed(() => user.roleCode === 'ADMIN')

const storeList = ref([])
const selectedStoreId = ref(null)

const { pageNum, pageSize, total, loading, data, filters, load, onPageChange, onSizeChange, resetAndLoad } =
  usePagination(
    (params) => getOperationLogsPage(params),
    {},
    10
  )

const onFilterChange = () => {
  if (filters.storeId === undefined) {
    delete filters.storeId
  }
  resetAndLoad()
}

const loadStores = async () => {
  if (!isAdmin.value) return
  try {
    const res = await getAllStores()
    storeList.value = res.data || []
  } catch (_) {}
}

onMounted(() => {
  loadStores()
  load()
})
</script>

<style scoped>
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
