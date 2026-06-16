<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.recipeTitle') }}</h3>
        <el-button
          type="primary"
          @click="handleAdd"
          :disabled="!selectedDishId || !selectedStoreId"
        >
          新增配方
        </el-button>
      </div>

      <!-- 门店+菜品选择器 -->
      <div class="selector-group" style="margin-bottom: 16px">
        <el-select
          v-model="selectedStoreId"
          placeholder="请先选择门店"
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
          v-model="selectedDishId"
          placeholder="请选择要管理配方的菜品"
          style="width: 300px"
          clearable
          @change="loadRecipes"
          :disabled="!selectedStoreId"
        >
          <el-option v-for="dish in dishes" :key="dish.id" :label="dish.name" :value="dish.id" />
        </el-select>

        <span style="margin-left: 8px; color: #909399">
          {{ selectedDishId ? `已选择: ${getDishName(selectedDishId)}` : '' }}
        </span>
      </div>

      <el-table
        :data="recipes"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
        :empty-text="selectedDishId ? '该菜品暂无配方' : '请先选择门店和菜品'"
      >
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="storeId" :label="$t('table.store')" width="80" />
        <el-table-column prop="dishId" label="菜品ID" width="80" />
        <el-table-column label="原料名称" width="150">
          <template #default="{ row }">
            {{ getMaterialName(row.materialId) }}
          </template>
        </el-table-column>
        <el-table-column prop="usedQuantity" label="用量" width="100" />
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
    <el-dialog :title="isEdit ? '编辑配方' : '新增配方'" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="原料">
          <el-select v-model="form.materialId" placeholder="请选择原料" style="width: 100%">
            <el-option
              v-for="material in materials"
              :key="material.id"
              :label="material.name"
              :value="material.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用量">
          <el-input
            v-model.number="form.usedQuantity"
            placeholder="请输入用量"
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
import { ref, onMounted } from 'vue'
import {
  getRecipesByDish,
  saveRecipe,
  deleteRecipe,
  getDishesByStore,
  getAllMaterials,
} from '@/api'
const { t } = useI18n()
import { useStoreFilter } from '@/composables/useStoreFilter'
import { ElMessage, ElMessageBox } from 'element-plus'

// 使用组合函数
const { stores, selectedStoreId, isLocked, loadStores } = useStoreFilter()

const recipes = ref([])
const dishes = ref([])
const materials = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const selectedDishId = ref(null)

const form = ref({
  id: null,
  storeId: null,
  dishId: null,
  materialId: null,
  usedQuantity: null,
})

// 获取原料名称
const getMaterialName = (materialId) => {
  if (!materialId) return '-'
  const material = materials.value.find((m) => m.id === materialId)
  return material ? material.name : '-'
}

// 获取菜品名称
const getDishName = (dishId) => {
  if (!dishId) return '-'
  const dish = dishes.value.find((d) => d.id === dishId)
  return dish ? dish.name : '-'
}

// 加载当前门店的菜品
const loadDishes = async () => {
  if (!selectedStoreId.value) {
    dishes.value = []
    return
  }

  try {
    const res = await getDishesByStore(selectedStoreId.value)
    dishes.value = res.data || []
  } catch (_) {
    dishes.value = []
  }
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

// 加载当前选中菜品的配方
const loadRecipes = async () => {
  if (!selectedDishId.value) {
    recipes.value = []
    return
  }

  loading.value = true
  try {
    const res = await getRecipesByDish(selectedDishId.value)
    recipes.value = res.data || []
  } catch (_) {
    recipes.value = []
  } finally {
    loading.value = false
  }
}

// 门店切换事件
const handleStoreChange = () => {
  selectedDishId.value = null // 清空已选菜品
  recipes.value = [] // 清空配方列表
  loadDishes() // 加载新门店的菜品
}

onMounted(async () => {
  await loadStores()
  loadMaterials()
  if (selectedStoreId.value) handleStoreChange()
})

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    storeId: selectedStoreId.value, // 自动填充当前选中的门店ID
    dishId: selectedDishId.value, // 自动填充当前选中的菜品ID
    materialId: null,
    usedQuantity: null,
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    storeId: row.storeId,
    dishId: row.dishId,
    materialId: row.materialId,
    usedQuantity: row.usedQuantity,
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  // 表单校验
  if (!form.value.materialId || !form.value.usedQuantity) {
    ElMessage.warning('请选择原料并填写用量')
    return
  }

  if (form.value.usedQuantity <= 0) {
    ElMessage.warning('用量必须大于0')
    return
  }

  try {
    await saveRecipe(form.value)
    ElMessage.success(t('action.createSuccess'))
    dialogVisible.value = false
    loadRecipes() // 重新加载当前菜品的配方
  } catch (_) {
    ElMessage.error(t('common.failed'))
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该配方？', '提示', { type: 'warning' })
    await deleteRecipe(id)
    ElMessage.success(t('action.deleteSuccess'))
    loadRecipes()
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

.selector-group {
  display: flex;
  align-items: center;
}
</style>
