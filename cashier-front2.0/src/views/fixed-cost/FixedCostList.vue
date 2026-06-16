<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.fixedCostTitle') }}</h3>
        <el-button type="primary" @click="handleAdd" :disabled="!selectedStoreId">
          新增成本
        </el-button>
      </div>

      <!-- 门店筛选器 -->
      <div class="filter-group" style="margin-bottom: 16px">
        <el-select
          v-model="selectedStoreId"
          :placeholder="$t('common.pleaseSelect')"
          style="width: 200px; margin-right: 16px"
          clearable
          :disabled="isLocked"
          @change="loadCosts"
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
        :data="costs"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
        :empty-text="selectedStoreId ? '该门店暂无固定成本记录' : '请先选择门店'"
      >
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="storeId" :label="$t('table.store')" width="80" />
        <el-table-column prop="costType" label="成本类型" width="150" />
        <el-table-column prop="amount" label="金额(元)" width="120" />
        <el-table-column prop="costDate" label="成本日期" width="120" />
        <el-table-column prop="remark" :label="$t('table.remark')" min-width="200" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column :label="$t('common.operate')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="isEdit ? '编辑固定成本' : '新增固定成本'"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="成本类型" required>
          <el-input v-model="form.costType" placeholder="如：房租、水电、工资、物业费" />
        </el-form-item>
        <el-form-item label="金额(元)" required>
          <el-input
            v-model.number="form.amount"
            placeholder="请输入金额"
            type="number"
            step="0.01"
          />
        </el-form-item>
        <el-form-item label="成本日期" required>
          <el-date-picker
            v-model="form.costDate"
            type="date"
            placeholder="选择成本发生日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="$t('table.remark')">
          <el-input v-model="form.remark" type="textarea" rows="3" placeholder="可选，补充说明" />
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
import { ref, onMounted } from 'vue'
import {
  getCostsByStore,
  createCost,
  updateCost,
  deleteCost,
} from '@/api'
const { t } = useI18n()
import { useStoreFilter } from '@/composables/useStoreFilter'
import { ElMessage, ElMessageBox } from 'element-plus'

// 使用组合函数
const { stores, selectedStoreId, isLocked, loadStores, getStoreName } = useStoreFilter()

const costs = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = ref({
  id: null,
  storeId: null,
  costType: '',
  amount: null,
  costDate: null,
  remark: '',
})

// 加载当前门店的固定成本记录
const loadCosts = async () => {
  if (!selectedStoreId.value) {
    costs.value = []
    return
  }

  loading.value = true
  try {
    const res = await getCostsByStore(selectedStoreId.value)
    costs.value = res.data || []
  } catch (_) {
    costs.value = []
    ElMessage.error('加载固定成本记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadStores()
  loadCosts()
})

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    storeId: selectedStoreId.value, // 自动填充当前选中的门店ID
    costType: '',
    amount: null,
    costDate: new Date(), // 默认今天
    remark: '',
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    storeId: row.storeId,
    costType: row.costType,
    amount: row.amount,
    costDate: row.costDate,
    remark: row.remark || '',
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  // 表单校验
  if (!form.value.costType || !form.value.amount || !form.value.costDate) {
    ElMessage.warning(t('common.pleaseSelect'))
    return
  }

  if (form.value.amount <= 0) {
    ElMessage.warning(t('common.pleaseSelect'))
    return
  }

  try {
    if (!isEdit.value) {
      await createCost(form.value)
    } else {
      await updateCost(form.value.id, form.value)
    }
    ElMessage.success(t('action.createSuccess'))
    dialogVisible.value = false
    loadCosts() // 重新加载数据
  } catch (_) {
    ElMessage.error(t('common.failed'))
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm(t("action.deleteConfirm"), '提示', { type: 'warning' })
    await deleteCost(id)
    ElMessage.success(t('action.deleteSuccess'))
    loadCosts()
  } catch (_) {
    /* 取消或错误 */
  }
}
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
