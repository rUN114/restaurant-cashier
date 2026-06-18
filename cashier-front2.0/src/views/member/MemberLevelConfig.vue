<template>
  <div class="member-level-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('member.levelConfig') }}</span>
          <el-button type="primary" @click="showDialog(null)">{{ $t('member.addLevel') }}</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="sortOrder" :label="$t('member.sortOrder')" width="60" />
        <el-table-column prop="name" :label="$t('member.levelName')" width="120" />
        <el-table-column :label="$t('member.color')" width="80">
          <template #default="{ row }">
            <span :style="{ color: row.color, fontWeight: 'bold' }">● {{ row.color }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="minConsumption" :label="$t('member.upgrade')" width="120" />
        <el-table-column :label="$t('member.discount')" width="80">
          <template #default="{ row }">
            <el-tag>{{ row.discountRate }} {{ $t('member.discountRate') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="$t('member.remark')" />
        <el-table-column :label="$t('common.operate')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">{{ $t('member.edit') }}</el-button>
            <el-popconfirm :title="$t('member.deleteConfirm')" @confirm="del(row.id)">
              <template #reference>
                <el-button size="small" type="danger">{{ $t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? $t('member.editLevel') : $t('member.addLevel')" width="420px">
      <el-form :model="form" label-width="100px">
        <el-form-item :label="$t('member.levelName')" required>
          <el-input v-model="form.name" :placeholder="$t('member.levelPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('member.sortOrder')" required>
          <el-input-number v-model="form.sortOrder" :min="1" />
        </el-form-item>
        <el-form-item :label="$t('member.upgrade')" required>
          <el-input-number v-model="form.minConsumption" :min="0" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item :label="$t('member.discount') + $t('member.discountRate')">
          <el-input-number v-model="form.discountRate" :min="0" :max="10" :precision="2" :step="0.5" />
          <span style="margin-left: 8px; color: #999">{{ $t('member.discountTip') }}</span>
        </el-form-item>
        <el-form-item :label="$t('member.color')">
          <el-color-picker v-model="form.color" />
          <span style="margin-left: 8px">{{ form.color }}</span>
        </el-form-item>
        <el-form-item :label="$t('member.remark')">
          <el-input v-model="form.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import * as memberApi from '@/api/member'

const { t } = useI18n()

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editId = ref(null)
const form = reactive({ name: '', sortOrder: 1, minConsumption: 0, discountRate: 10, color: '#999', remark: '' })

const loadData = async () => {
  loading.value = true
  try { const res = await memberApi.getMemberLevels(); list.value = res.data || [] } finally { loading.value = false }
}

const showDialog = (row) => {
  if (row) { editId.value = row.id; Object.assign(form, row) }
  else { editId.value = null; Object.assign(form, { name: '', sortOrder: 1, minConsumption: 0, discountRate: 10, color: '#999', remark: '' }) }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.name) { ElMessage.warning(t('member.levelRequired')); return }
  try {
    editId.value ? await memberApi.updateMemberLevel(editId.value, form) : await memberApi.createMemberLevel(form)
    ElMessage.success(editId.value ? t('action.updateSuccess') : t('action.createSuccess'))
    dialogVisible.value = false
    loadData()
  } catch (e) { ElMessage.error(t('common.failed')) }
}

const del = async (id) => {
  await memberApi.deleteMemberLevel(id)
  ElMessage.success(t('member.deleteSuccess'))
  loadData()
}

onMounted(loadData)
</script>
<style scoped>.card-header { display: flex; justify-content: space-between; align-items: center; }</style>
