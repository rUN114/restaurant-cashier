<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.categoryTitle') }}</h3>
        <div style="display: flex; gap: 16px; align-items: center">
          <!-- 管理员选择门店 -->
          <el-select
            v-if="isAdmin"
            v-model="selectedStoreId"
            :placeholder="$t('common.pleaseSelect')"
            style="width: 200px"
            @change="loadData"
          >
            <el-option
              v-for="store in storeList"
              :key="store.id"
              :label="store.name"
              :value="store.id"
            />
          </el-select>
          <el-button type="primary" @click="handleAdd">新增分类</el-button>
        </div>
      </div>
      <el-table :data="categories" border stripe v-loading="loading">
        <el-table-column prop="sortOrder" label="排序" width="60" />
        <el-table-column prop="name" :label="$t('table.name')" />
        <el-table-column :label="$t('common.operate')" width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="isEdit ? '编辑分类' : '新增分类'" v-model="dialogVisible" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="$t('table.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" />
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
import { getCategoriesByStore, getAllStores, createCategory, updateCategory, deleteCategory } from '@/api'

import { ElMessage, ElMessageBox } from 'element-plus'

const user = useUserStore()
const isAdmin = computed(() => user.roleCode === 'ADMIN')
const userStoreId = computed(() => user.storeId)  // 店长的门店ID

const selectedStoreId = ref(null)        // 管理员选中的门店ID
const storeList = ref([])                // 门店列表（管理员用）
const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, name: '', sortOrder: 0 })

// 获取实际使用的 storeId
const effectiveStoreId = computed(() => {
  if (isAdmin.value) return selectedStoreId.value
  return userStoreId.value
})

// 加载门店列表（管理员）
const loadStoreList = async () => {
  if (!isAdmin.value) return
  try {
    const res = await getAllStores()
    storeList.value = res.data || []
    if (storeList.value.length > 0 && !selectedStoreId.value) {
      selectedStoreId.value = storeList.value[0].id  // 默认选第一个门店
      await loadData()
    }
  } catch (_) {
    storeList.value = []
  }
}

// 加载分类数据
const loadData = async () => {
  const sid = effectiveStoreId.value
  if (!sid) {
    categories.value = []
    if (isAdmin.value && storeList.value.length === 0) {
      ElMessage.warning('暂无门店，请先添加门店')
    } else if (isAdmin.value && !selectedStoreId.value) {
      // 还未选择门店，不报错，等待用户选择
      categories.value = []
    } else if (!isAdmin.value && !sid) {
      ElMessage.warning('您未绑定门店，请联系管理员')
    }
    return
  }
  loading.value = true
  try {
    const res = await getCategoriesByStore(sid)
    categories.value = res.data || []
  } catch (_) {
    categories.value = []
  } finally {
    loading.value = false
  }
}

// 新增分类（使用 effectiveStoreId）
const handleAdd = () => {
  const sid = effectiveStoreId.value
  if (!sid) {
    ElMessage.warning('请先选择门店')
    return
  }
  isEdit.value = false
  // 计算当前最大 sortOrder + 1
  const maxOrder = categories.value.length > 0
    ? Math.max(...categories.value.map(c => c.sortOrder || 0))
    : 0
  form.value = { id: null, name: '', sortOrder: maxOrder + 1 }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    const data = {
      name: form.value.name,
      sortOrder: form.value.sortOrder,
      storeId: effectiveStoreId.value
    }
    if (isEdit.value) {
      await updateCategory(form.value.id, data)
    } else {
      await createCategory(data)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } catch (_) {
    ElMessage.error(t('common.failed'))
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
    await deleteCategory(id)
    ElMessage.success(t('action.deleteSuccess'))
    loadData()
  } catch (_) {}
}

onMounted(async () => {
  if (isAdmin.value) {
    await loadStoreList()
  } else {
    await loadData()
  }
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
