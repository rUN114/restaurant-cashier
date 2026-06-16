<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.storeTitle') }}</h3>
        <el-button type="primary" @click="handleAdd">{{ $t('action.createStore') }}</el-button>
      </div>
      <el-table :data="stores" border stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="name" :label="$t('table.name')" width="140" />
        <el-table-column prop="address" :label="$t('table.address')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" :label="$t('table.phone')" width="140" />
        <el-table-column :label="$t('common.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? $t('table.operating') : $t('table.closed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="$t('common.createTime')" width="180" />
        <el-table-column prop="updatedAt" :label="$t('common.updateTime')" width="180" />
        <el-table-column :label="$t('common.operate')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗，增加联系电话 -->
    <el-dialog :title="isEdit ? $t('action.editStore') : $t('action.createStore')" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item :label="$t('table.name')"><el-input v-model="form.name" /></el-form-item>
        <el-form-item :label="$t('table.address')"><el-input v-model="form.address" /></el-form-item>
        <el-form-item :label="$t('table.phone')"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-select v-model="form.status">
            <el-option :label="$t('table.available')" :value="1" />
            <el-option :label="$t('table.unavailable')" :value="0" />
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
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getAllStores, createStore, updateStore, deleteStore, getStoreById } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t } = useI18n()

const stores = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  name: '',
  address: '',
  phone: '',
  status: 1
})

// 加载所有门店（不含软删除）
const loadData = async () => {
  loading.value = true
  try {
    const res = await getAllStores()
    stores.value = res.data || []
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  form.value = { id: null, name: '', address: '', phone: '', status: 1 }
  dialogVisible.value = true
}

// 编辑（从接口获取最新数据）
const handleEdit = async (row) => {
  isEdit.value = true
  try {
    const res = await getStoreById(row.id)
    const store = res.data
    form.value = {
      id: store.id,
      name: store.name,
      address: store.address,
      phone: store.phone || '',
      status: store.status
    }
    dialogVisible.value = true
  } catch (_) {
    // 忽略错误
  }
}

// 提交新增/编辑
const submitForm = async () => {
  try {
    const data = {
      name: form.value.name,
      address: form.value.address,
      phone: form.value.phone,
      status: form.value.status
    }
    if (isEdit.value) {
      await updateStore(form.value.id, data)
    } else {
      await createStore(data)
    }
    ElMessage.success(t('action.createSuccess'))
    dialogVisible.value = false
    loadData()
  } catch (_) {
    // 忽略
  }
}

// 软删除
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm(t('action.deleteConfirm'), t('common.confirm'), { type: 'warning' })
    await deleteStore(id)
    ElMessage.success(t('action.deleteSuccess'))
    loadData()
  } catch (_) {
    // 取消或错误忽略
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
