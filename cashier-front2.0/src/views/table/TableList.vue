<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.tableTitle') }}</h3>
        <el-button type="primary" @click="handleAdd">{{ $t('common.add') }}</el-button>
      </div>
      <el-table :data="tables" border stripe v-loading="loading">
        <el-table-column prop="tableNumber" :label="$t('table.tableNumber')" width="100" sortable />
        <el-table-column prop="area" :label="$t('table.area')" width="120" />
        <el-table-column prop="capacity" :label="$t('table.capacity')" width="80" />
        <!-- 新增：显示所属门店 -->
        <el-table-column :label="$t('table.store')" width="140">
          <template #default="{ row }">
            {{ getStoreName(row.storeId) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'free' ? 'success' : 'warning'">
              {{ row.status === 'free' ? $t('table.free') : $t('table.occupied') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operate')" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="isEdit ? $t('common.edit') + $t('pages.tableTitle') : $t('common.add') + $t('pages.tableTitle')" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="$t('table.tableNumber')">
          <el-input v-model="form.tableNumber" placeholder="例如 A1、B2" />
        </el-form-item>
        <el-form-item :label="$t('table.area')">
          <el-input v-model="form.area" placeholder="大厅、包间、露天等" />
        </el-form-item>
        <el-form-item :label="$t('table.capacity')">
          <el-input-number v-model="form.capacity" :min="1" :max="20" />
        </el-form-item>
        <!-- 管理员新增时可选择门店，店长不显示此选项 -->
        <el-form-item v-if="roleCode === 'ADMIN' && !isEdit" :label="$t('table.store')">
          <el-select v-model="form.storeId" :placeholder="$t('common.pleaseSelect')">
            <el-option v-for="store in storeList" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-select v-model="form.status">
            <el-option :label="$t('table.free')" value="free" />
            <el-option :label="$t('table.occupied')" value="occupied" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitForm">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
const { t } = useI18n()
import {
  getTablesByStore,
  getAllTables,
  createTable,
  updateTable,
  deleteTable,
  getAllStores
} from '@/api'

import { ElMessage, ElMessageBox } from 'element-plus'

const user = useUserStore()
const storeId = computed(() => user.storeId)
const roleCode = computed(() => user.roleCode)   // 'ADMIN' / 'STORE_ADMIN' 等

const tables = ref([])
const storeList = ref([])      // 管理员可见的所有门店列表
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  tableNumber: '',
  area: '',
  capacity: 4,
  status: 'free',
  storeId: null
})

// 根据 storeId 获取门店名称
const getStoreName = (sid) => {
  if (!sid) return t('common.noData')
  const store = storeList.value.find(s => s.id === sid)
  return store ? store.name : `${t('table.store')}${sid}`
}

// 加载门店列表（管理员用）
const loadStoreList = async () => {
  if (roleCode.value !== 'ADMIN') return
  try {
    const res = await getAllStores()
    storeList.value = res.data || []
  } catch (_) {}
}

// 加载餐桌数据
const loadData = async () => {
  loading.value = true
  try {
    let res
    if (roleCode.value === 'ADMIN') {
      res = await getAllTables()        // 管理员看全部
    } else {
      if (!storeId.value) {
        ElMessage.warning('您未绑定门店，无法查看餐桌')
        tables.value = []
        loading.value = false
        return
      }
      res = await getTablesByStore(storeId.value)  // 店长/服务员只看本店
    }
    tables.value = res.data || []
  } catch (_) {
    tables.value = []
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    tableNumber: '',
    area: '',
    capacity: 4,
    status: 'free',
    storeId: roleCode.value === 'ADMIN' ? (storeList.value[0]?.id || null) : storeId.value
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  form.value = { ...row }       // row 必须包含 storeId，编辑时不修改门店
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    if (isEdit.value) {
      await updateTable(form.value.id, {
        tableNumber: form.value.tableNumber,
        area: form.value.area,
        capacity: form.value.capacity,
        status: form.value.status,
        storeId: form.value.storeId
      })
      ElMessage.success('餐桌已更新')
    } else {
      // 新增餐桌，明确传递 storeId
      await createTable({
        tableNumber: form.value.tableNumber,
        area: form.value.area,
        capacity: form.value.capacity,
        status: form.value.status,
        storeId: form.value.storeId
      })
      ElMessage.success('餐桌已创建')
    }
    dialogVisible.value = false
    loadData()
  } catch (_) {
    ElMessage.error(t('common.failed'))
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该餐桌？', '提示', { type: 'warning' })
    await deleteTable(id)
    ElMessage.success(t('action.deleteSuccess'))
    loadData()
  } catch (_) { /* 取消 */ }
}

onMounted(() => {
  loadStoreList()
  loadData()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
