<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>{{ $t('pages.employeeTitle') }}</h3>
        <el-button type="primary" @click="handleAdd">{{ $t('common.add') }}</el-button>
      </div>
      <el-table :data="data" border stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="username" :label="$t('table.username')" width="120" />
        <el-table-column prop="realName" :label="$t('table.realName')" width="100" />
        <el-table-column prop="phone" :label="$t('table.phone')" width="140" />
        <el-table-column prop="storeId" :label="$t('table.store')" width="80" />
        <el-table-column :label="$t('table.role')" width="120">
          <template #default="{ row }">
            {{ getRoleName(row.roleId) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.status')" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? $t('table.status1') : $t('table.status0') }}
            </el-tag>
          </template>
        </el-table-column>
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
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="isEdit ? '编辑员工' : '新增员工'" v-model="dialogVisible" width="500px" @opened="showContent = true" @closed="showContent = false">
      <template v-if="showContent">
        <el-form :model="form" label-width="80px">
          <el-form-item :label="$t('table.username')"><el-input v-model="form.username" :disabled="isEdit" /></el-form-item>
          <el-form-item v-if="!isEdit" label="密码"><el-input v-model="form.password" type="password" /></el-form-item>
          <el-form-item :label="$t('table.realName')"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item :label="$t('table.phone')"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item :label="$t('table.role')">
            <el-select v-model="form.roleId" placeholder="选择角色">
              <el-option v-for="r in roles" :key="r.id" :label="r.name || r.roleName" :value="r.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="门店">
            <el-select v-model="form.storeId" :placeholder="$t('common.pleaseSelect')" :disabled="isStoreLocked">
              <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('common.status')">
            <el-switch
              v-model="form.status"
              :active-value="1"
              :inactive-value="0"
              active-text="在职"
              inactive-text="离职"
            />
          </el-form-item>
        </el-form>
      </template>
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
import { useUserStore } from '@/stores/user'
import {
  getUsersPage,
  createUser,
  updateUser,
  deleteUser,
  getAllRoles,
  getValidStores,
  getUserWithRole
} from '@/api'
const { t } = useI18n()
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePagination } from '@/composables/usePagination'

const user = useUserStore()
const isStoreLocked = computed(() => user.roleCode === 'STORE_ADMIN')

const roles = ref([])
const stores = ref([])
const dialogVisible = ref(false)
const showContent = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  roleId: null,
  storeId: null,
  status: 1
})

const { pageNum, pageSize, total, loading, data, load, onPageChange, onSizeChange } =
  usePagination((params) => getUsersPage(params), {}, 10)

const getRoleName = (roleId) => {
  if (!roleId) return '-'
  const role = roles.value.find(r => r.id === roleId)
  return role ? (role.name || role.roleName || '-') : '-'
}

const loadRoles = async () => {
  try {
    const res = await getAllRoles()
    roles.value = res.data || []
  } catch (_) {
    roles.value = []
  }
}

const loadStores = async () => {
  try {
    const res = await getValidStores()
    stores.value = res.data || []
  } catch (_) {
    stores.value = []
  }
}

onMounted(() => {
  load()
  loadRoles()
  loadStores()
})

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    username: '',
    password: '',
    realName: '',
    phone: '',
    roleId: null,
    storeId: isStoreLocked.value ? user.storeId : null,
    status: 1
  }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  try {
    const res = await getUserWithRole(row.id)
    const u = res.data
    form.value = {
      id: u.id,
      username: u.username,
      password: '',
      realName: u.realName,
      phone: u.phone || '',
      roleId: u.role?.id || null,
      storeId: u.storeId,
      status: u.status
    }
    dialogVisible.value = true
  } catch (_) { /* ignore */ }
}

const submitForm = async () => {
  try {
    const payload = {
      username: form.value.username,
      realName: form.value.realName,
      phone: form.value.phone,
      roleId: form.value.roleId,
      storeId: form.value.storeId,
      status: form.value.status
    }
    if (!isEdit.value) {
      payload.password = form.value.password
      await createUser(payload)
    } else {
      await updateUser(form.value.id, payload)
    }
    ElMessage.success(t('action.createSuccess'))
    dialogVisible.value = false
    load()
  } catch (_) { /* ignore */ }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该员工？', '提示', { type: 'warning' })
    await deleteUser(id)
    ElMessage.success(t('action.deleteSuccess'))
    load()
  } catch (_) { /* cancel or error */ }
}
</script>
