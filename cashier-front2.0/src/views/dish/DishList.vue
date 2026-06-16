<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.dishTitle') }}</h3>
        <div style="display: flex; gap: 16px; align-items: center">
          <!-- 管理员门店筛选下拉框 -->
          <el-select
            v-if="roleCode === 'ADMIN'"
            v-model="selectedStoreId"
            :placeholder="$t('common.pleaseSelect')"
            style="width: 200px"
            @change="onStoreChange"
          >
            <el-option :label="$t('common.pleaseSelect')" value="all" />
            <el-option
              v-for="store in storeList"
              :key="store.id"
              :label="store.name"
              :value="store.id"
            />
          </el-select>
          <el-button type="primary" @click="handleAdd">新增菜品</el-button>
        </div>
      </div>

      <!-- 菜品卡片网格 -->
      <div v-if="dishes.length > 0" class="dish-card-grid">
        <div v-for="dish in dishes" :key="dish.id" class="dish-card">
          <div class="card-img">
            <el-image
              :src="getImageUrl(dish.imageUrl)"
              fit="cover"
              lazy
              style="width: 100%; height: 140px"
            />
          </div>
          <div class="card-body">
            <h4>{{ dish.name }}</h4>
            <p class="price">¥{{ dish.price }} / {{ dish.unit }}</p>
            <!-- 管理员才显示门店名称 -->
            <p class="store" v-if="roleCode === 'ADMIN'">
              {{ getStoreName(dish.storeId) }}
            </p>
            <p class="category">{{ getCategoryName(dish.categoryId) }}</p>
            <p class="remark" v-if="dish.remark">{{ dish.remark }}</p>
            <el-tag :type="dish.isAvailable === 1 ? 'success' : 'info'" size="small">
              {{ dish.isAvailable === 1 ? '可售' : '停售' }}
            </el-tag>
          </div>
          <div class="card-footer">
            <el-button link type="primary" @click="handleEdit(dish)">{{ $t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(dish.id)">{{ $t('common.delete') }}</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无菜品" />
    </el-card>

    <!-- 新增 / 编辑菜品弹窗 -->
    <el-dialog :title="isEdit ? '编辑菜品' : '新增菜品'" v-model="dialogVisible" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="菜品图片">
          <div class="upload-area">
            <el-image
              v-if="form.imageUrl"
              :src="getImageUrl(form.imageUrl)"
              fit="cover"
              lazy
              style="width: 200px; height: 140px; border-radius: 8px"
            />
            <el-upload
              :show-file-list="false"
              :before-upload="beforeImageUpload"
              :http-request="uploadImage"
              action="#"
            >
              <el-button type="primary" :disabled="uploading">
                {{ uploading ? '上传中...' : '选择图片' }}
              </el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item :label="$t('table.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="例如：份、碗、瓶" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('table.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-switch
            v-model="form.isAvailable"
            :active-value="1"
            :inactive-value="0"
            active-text="可售"
            inactive-text="停售"
          />
        </el-form-item>
        <!-- 管理员新增时可以手动选择所属门店 -->
        <el-form-item v-if="roleCode === 'ADMIN' && !isEdit" :label="$t('table.store')">
          <el-select v-model="form.storeId" :placeholder="$t('common.pleaseSelect')">
            <el-option
              v-for="store in storeList"
              :key="store.id"
              :label="store.name"
              :value="store.id"
            />
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
  getDishesByStore,
  getAllDishes,
  createDish,
  updateDish,
  deleteDish,
  getCategoriesByStore,
  getAllCategories,
  uploadDishImage,
  getAllStores,
} from '@/api'

import { ElMessage, ElMessageBox } from 'element-plus'

const user = useUserStore()
const storeId = computed(() => user.storeId)
const roleCode = computed(() => user.roleCode) // 已映射：ADMIN / STORE_ADMIN / CASHIER / WAITER

const selectedStoreId = ref('all') // 管理员默认“全部门店”
const storeList = ref([]) // 管理员可选择的门店列表

const dishes = ref([])
const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const uploading = ref(false)

const form = ref({
  id: null,
  name: '',
  price: 0,
  unit: '份',
  categoryId: null,
  remark: '',
  imageUrl: '',
  isAvailable: 1,
  storeId: null,
})

// 图片路径处理（已配置代理，直接使用）
const getImageUrl = (url) => url || ''

// 根据门店ID获取门店名称
const getStoreName = (sid) => {
  if (!sid) return '总部'
  const store = storeList.value.find((s) => s.id === sid)
  return store ? store.name : '未知门店'
}

// 根据分类ID获取分类名称
const getCategoryName = (categoryId) => {
  if (!categoryId) return '未分类'
  const cat = categories.value.find((c) => c.id === categoryId)
  return cat ? cat.name : t('common.noData')
}

// 加载门店列表（管理员专用）
const loadStoreList = async () => {
  if (roleCode.value !== 'ADMIN') return
  try {
    const res = await getAllStores()
    storeList.value = res.data || []
  } catch (_) {
    storeList.value = []
  }
}

// 主数据加载：菜品 + 分类
const loadData = async () => {
  loading.value = true
  try {
    let dishPromise, catPromise

    if (roleCode.value === 'ADMIN') {
      // 管理员：根据下拉框选择的全部门店或具体门店
      if (selectedStoreId.value === 'all') {
        dishPromise = getAllDishes()
        catPromise = getAllCategories()
      } else {
        dishPromise = getDishesByStore(selectedStoreId.value)
        catPromise = getCategoriesByStore(selectedStoreId.value)
      }
    } else {
      // 非管理员：使用绑定的门店ID
      if (!storeId.value) {
        ElMessage.warning('您未绑定门店，无法查看菜品')
        dishes.value = []
        categories.value = [{ id: null, name: '未绑定门店' }]
        loading.value = false
        return
      }
      dishPromise = getDishesByStore(storeId.value)
      catPromise = getCategoriesByStore(storeId.value)
    }

    const [dRes, cRes] = await Promise.all([dishPromise, catPromise])
    dishes.value = dRes.data || []
    categories.value = cRes.data || []

    if (categories.value.length === 0) {
      categories.value = [{ id: null, name: '未分类' }]
    }
  } catch (_) {
    dishes.value = []
    categories.value = [{ id: null, name: '加载失败' }]
  } finally {
    loading.value = false
  }
}

// 切换门店时重新加载
const onStoreChange = () => {
  loadData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    name: '',
    price: 0,
    unit: '份',
    categoryId: categories.value[0]?.id || null,
    remark: '',
    imageUrl: '',
    isAvailable: 1,
    storeId:
      roleCode.value === 'ADMIN'
        ? selectedStoreId.value === 'all'
          ? null
          : selectedStoreId.value
        : storeId.value,
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

// 上传图片前校验
const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

// 自定义上传
const uploadImage = async (options) => {
  uploading.value = true
  try {
    const res = await uploadDishImage(options.file)
    form.value.imageUrl = res.data
    ElMessage.success('上传成功')
  } catch (_) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

// 提交表单（新增/编辑）
const submitForm = async () => {
  try {
    const payload = {
      name: form.value.name,
      price: form.value.price,
      unit: form.value.unit,
      categoryId: form.value.categoryId,
      remark: form.value.remark,
      imageUrl: form.value.imageUrl,
      isAvailable: form.value.isAvailable,
      storeId: form.value.storeId,
    }
    if (isEdit.value) {
      await updateDish(form.value.id, payload)
    } else {
      await createDish(payload)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } catch (_) {
    ElMessage.error(t('common.failed'))
  }
}

// 删除菜品
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该菜品吗？', '提示', { type: 'warning' })
    await deleteDish(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (_) {
    // 取消或错误忽略
  }
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
.dish-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.dish-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  transition: box-shadow 0.3s;
}
.dish-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
.card-img {
  height: 140px;
  background: #f5f7fa;
}
.card-body {
  padding: 12px;
}
.card-body h4 {
  margin: 0 0 6px;
  font-size: 16px;
}
.card-body .price {
  color: #f56c6c;
  font-weight: bold;
  margin: 4px 0;
}
.card-body .store,
.card-body .category,
.card-body .remark {
  color: #909399;
  font-size: 12px;
  margin: 2px 0;
}
.card-footer {
  padding: 8px 12px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.upload-area {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
