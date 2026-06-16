<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.materialTitle') }}</h3>
        <el-button type="primary" @click="handleAdd" :disabled="!selectedStoreId">
          新增原材料
        </el-button>
      </div>

      <!-- 门店筛选器（和库存页面完全一致） -->
      <div class="filter-group" style="margin-bottom: 16px">
        <el-select
          v-model="selectedStoreId"
          placeholder="请选择门店查看原材料"
          style="width: 200px; margin-right: 16px"
          clearable
          :disabled="isLocked"
          @change="loadMaterials"
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

      <!-- 表格（和库存页面风格一致） -->
      <el-table
        :data="paginatedMaterials"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
        :empty-text="selectedStoreId ? '该门店暂无原材料记录' : '请先选择门店'"
      >
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="storeId" :label="$t('table.store')" width="80" />
        <el-table-column prop="name" label="原料名称" width="150" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="warningStock" label="预警库存" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column :label="$t('common.operate')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="materials.length"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </el-card>

    <!-- 新增/编辑弹窗（简洁版） -->
    <el-dialog :title="isEdit ? '编辑原材料' : '新增原材料'" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="原料名称" required>
          <el-input v-model="form.name" placeholder="请输入原料名称" />
        </el-form-item>
        <el-form-item label="单位" required>
          <el-input v-model="form.unit" placeholder="如：克、千克、个、升" />
        </el-form-item>
        <el-form-item label="预警库存" required>
          <el-input
            v-model.number="form.warningStock"
            placeholder="请输入预警库存值"
            type="number"
            step="0.001"
          />
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
import { ref, computed, onMounted } from 'vue'
import {
  getMaterialsByStore,
  createMaterial,
  updateMaterial,
  deleteMaterial,
} from '@/api'
const { t } = useI18n()
import { useStoreFilter } from '@/composables/useStoreFilter'
import { usePagination } from '@/composables/usePagination'
import { ElMessage, ElMessageBox } from 'element-plus'

// 使用组合函数
const { stores, selectedStoreId, isLocked, loadStores, getStoreName } = useStoreFilter()

const materials = ref([])
const loading = ref(false)

// 客户端分页
const currentPage = ref(1)
const pageSize = ref(10)
const paginatedMaterials = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return materials.value.slice(start, start + pageSize.value)
})

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  storeId: null,
  name: '',
  unit: '',
  warningStock: null,
})

// 加载原材料
const loadMaterials = async () => {
  if (!selectedStoreId.value) {
    materials.value = []
    return
  }

  loading.value = true
  try {
    const res = await getMaterialsByStore(selectedStoreId.value)
    materials.value = res.data || []
  } catch (_) {
    materials.value = []
    ElMessage.error('加载原材料失败')
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    storeId: selectedStoreId.value,
    name: '',
    unit: '',
    warningStock: null,
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

// 提交保存
const submitForm = async () => {
  if (!form.value.name || !form.value.unit || form.value.warningStock == null) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.value.warningStock < 0) {
    ElMessage.warning('预警库存不能为负数')
    return
  }

  try {
    if (isEdit.value) {
      await updateMaterial(form.value.id, form.value)
      ElMessage.success('编辑成功')
    } else {
      await createMaterial(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadMaterials()
  } catch (_) {
    ElMessage.error(t('common.failed'))
  }
}

// 删除
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该原材料？', '提示', { type: 'warning' })
    await deleteMaterial(id)
    ElMessage.success('删除成功')
    loadMaterials()
  } catch (_) {}
}

// 初始化（和库存页面完全一致）
onMounted(async () => {
  await loadStores()
  loadMaterials()
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
