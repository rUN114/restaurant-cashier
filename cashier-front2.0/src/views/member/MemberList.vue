<template>
  <div class="member-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('member.title') }}</span>
          <el-button type="primary" @click="showDialog(null)">{{ $t('member.add') }}</el-button>
        </div>
      </template>

      <el-row :gutter="12" style="margin-bottom: 12px">
        <el-col :span="8">
          <el-input v-model="keyword" :placeholder="$t('member.searchPlaceholder')" clearable @keyup.enter="loadData" />
        </el-col>
        <el-col :span="4">
          <el-button @click="loadData">{{ $t('member.search') }}</el-button>
        </el-col>
      </el-row>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="phone" :label="$t('member.phone')" width="130" />
        <el-table-column prop="name" :label="$t('member.name')" />
        <el-table-column :label="$t('member.level')" width="100">
          <template #default="{ row }">
            <el-tag :color="row.level?.color" effect="dark" v-if="row.level">
              {{ row.level.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalConsumption" :label="$t('member.totalConsumption')" width="100" />
        <el-table-column prop="availablePoints" :label="$t('member.availablePoints')" width="80" />
        <el-table-column prop="totalOrders" :label="$t('member.totalOrders')" width="80" />
        <el-table-column :label="$t('member.status')" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? $t('member.enabled') : $t('member.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operate')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">{{ $t('member.edit') }}</el-button>
            <el-button size="small" type="warning" @click="showPointsDialog(row)">{{ $t('member.points') }}</el-button>
            <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? $t('member.disabled') : $t('member.enabled') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
      />
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? $t('member.edit') : $t('member.add')" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item :label="$t('member.phone')" required>
          <el-input v-model="form.phone" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="$t('member.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('member.gender')">
          <el-select v-model="form.gender">
            <el-option :value="0" :label="$t('member.gender0')" />
            <el-option :value="1" :label="$t('member.gender1')" />
            <el-option :value="2" :label="$t('member.gender2')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('member.birthday')">
          <el-date-picker v-model="form.birthday" type="date" :placeholder="$t('common.pleaseSelect')" />
        </el-form-item>
        <el-form-item :label="$t('member.level')" v-if="isEdit">
          <el-select v-model="form.levelId">
            <el-option v-for="lv in levels" :key="lv.id" :label="lv.name" :value="lv.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('member.remark')">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 积分调整弹窗 -->
    <el-dialog v-model="pointsDialogVisible" :title="$t('member.pointAdjust')" width="360px">
      <p>{{ $t('member.memberInfo') }}{{ pointsForm.name }}（{{ pointsForm.phone }}）</p>
      <p>{{ $t('member.availablePoints') }}：<b>{{ pointsForm.availablePoints }}</b></p>
      <el-form :model="pointsForm" label-width="80px" style="margin-top: 12px">
        <el-form-item :label="$t('member.adjustPoints')">
          <el-input-number v-model="pointsForm.change" :min="-pointsForm.availablePoints" />
          <span style="margin-left: 8px; color: #999">{{ $t('member.adjustPositive') }}</span>
        </el-form-item>
        <el-form-item :label="$t('member.remark')">
          <el-input v-model="pointsForm.remark" :placeholder="$t('member.adjustReason')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="savePoints">{{ $t('member.confirmAdjust') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as memberApi from '@/api/member'

const { t } = useI18n()

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = reactive({ phone: '', name: '', gender: 0, birthday: null, levelId: null, remark: '' })
const levels = ref([])

const pointsDialogVisible = ref(false)
const pointsForm = reactive({ memberId: null, name: '', phone: '', availablePoints: 0, change: 0, remark: '' })

const loadLevels = async () => {
  try { const res = await memberApi.getMemberLevels(); levels.value = res.data || [] } catch { /* */ }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await memberApi.getMembersPage({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    list.value = res.data || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const showDialog = (row) => {
  if (row) {
    isEdit.value = true; editId.value = row.id
    Object.assign(form, { phone: row.phone, name: row.name, gender: row.gender, birthday: row.birthday, levelId: row.levelId, remark: row.remark })
  } else {
    isEdit.value = false; editId.value = null
    Object.assign(form, { phone: '', name: '', gender: 0, birthday: null, levelId: null, remark: '' })
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.phone) { ElMessage.warning(t('member.phoneRequired')); return }
  try {
    isEdit.value ? await memberApi.updateMember(editId.value, form) : await memberApi.createMember(form)
    ElMessage.success(isEdit.value ? t('action.updateSuccess') : t('action.createSuccess'))
    dialogVisible.value = false
    loadData()
  } catch (e) { ElMessage.error(t('common.failed')) }
}

const toggleStatus = async (row) => {
  const action = row.status === 1 ? t('member.disabled') : t('member.enabled')
  try {
    await ElMessageBox.confirm(`${t('member.confirmToggle')}${action}${t('member.title')}「${row.name || row.phone}」？`, t('common.confirm'))
    await memberApi.toggleMemberStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(`${t('common.success')}`)
    loadData()
  } catch { /* cancelled */ }
}

const showPointsDialog = (row) => {
  Object.assign(pointsForm, { memberId: row.id, name: row.name, phone: row.phone, availablePoints: row.availablePoints, change: 0, remark: '' })
  pointsDialogVisible.value = true
}

const savePoints = async () => {
  if (pointsForm.change === 0) { ElMessage.warning(t('member.adjustPoints')); return }
  try {
    await memberApi.adjustPoints({ memberId: pointsForm.memberId, points: pointsForm.change, remark: pointsForm.remark })
    ElMessage.success(t('member.adjustSuccess'))
    pointsDialogVisible.value = false
    loadData()
  } catch (e) { ElMessage.error(t('member.adjustFail')) }
}

onMounted(() => { loadData(); loadLevels() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
