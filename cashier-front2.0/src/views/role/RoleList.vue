<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.roleTitle') }}</h3>
        <el-button type="primary" @click="handleAdd">新增角色</el-button>
      </div>
      <el-table :data="roles" border stripe v-loading="loading">
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="roleCode" label="编码" width="120" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column :label="$t('common.operate')" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="isEdit ? '编辑角色' : '新增角色'" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="$t('table.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.roleCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
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
import { getAllRoles, createRole, updateRole, deleteRole } from '@/api'
const { t } = useI18n()
import { ElMessage, ElMessageBox } from 'element-plus'

const roles = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, name: '', roleCode: '', description: '' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAllRoles()
    roles.value = res.data || []
  } catch (e) {
    ElMessage.error('加载角色列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: null, name: '', roleCode: '', description: '' }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    name: row.name || '',
    roleCode: row.roleCode || '',
    description: row.description || ''
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    const data = {
      name: form.value.name,
      roleCode: form.value.roleCode,
      description: form.value.description
    }
    if (isEdit.value) {
      await updateRole(form.value.id, data)
    } else {
      await createRole(data)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(t('common.failed'))
    console.error(e)
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该角色？', '提示', { type: 'warning' })
    await deleteRole(id)
    ElMessage.success(t('action.deleteSuccess'))
    loadData()
  } catch (e) {
    console.error(e)
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
