<template>
  <div class="member-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>会员管理</span>
          <el-button type="primary" @click="showDialog(null)">新增会员</el-button>
        </div>
      </template>

      <el-row :gutter="12" style="margin-bottom: 12px">
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索手机号/姓名" clearable @keyup.enter="loadData" />
        </el-col>
        <el-col :span="4">
          <el-button @click="loadData">搜索</el-button>
        </el-col>
      </el-row>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column label="等级" width="100">
          <template #default="{ row }">
            <el-tag :color="row.level?.color" effect="dark" v-if="row.level">
              {{ row.level.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalConsumption" label="累计消费" width="100" />
        <el-table-column prop="availablePoints" label="可用积分" width="80" />
        <el-table-column prop="totalOrders" label="消费次数" width="80" />
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="showPointsDialog(row)">积分</el-button>
            <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑会员' : '新增会员'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="手机号" required>
          <el-input v-model="form.phone" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender">
            <el-option :value="0" label="未知" />
            <el-option :value="1" label="男" />
            <el-option :value="2" label="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="form.birthday" type="date" placeholder="选填" />
        </el-form-item>
        <el-form-item label="会员等级" v-if="isEdit">
          <el-select v-model="form.levelId">
            <el-option v-for="lv in levels" :key="lv.id" :label="lv.name" :value="lv.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 积分调整弹窗 -->
    <el-dialog v-model="pointsDialogVisible" title="积分调整" width="360px">
      <p>会员：{{ pointsForm.name }}（{{ pointsForm.phone }}）</p>
      <p>当前可用积分：<b>{{ pointsForm.availablePoints }}</b></p>
      <el-form :model="pointsForm" label-width="80px" style="margin-top: 12px">
        <el-form-item label="变动积分">
          <el-input-number v-model="pointsForm.change" :min="-pointsForm.availablePoints" />
          <span style="margin-left: 8px; color: #999">正数=增加，负数=扣减</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="pointsForm.remark" placeholder="调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePoints">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as memberApi from '@/api/member'

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
  } finally {
    loading.value = false
  }
}

const showDialog = (row) => {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    Object.assign(form, { phone: row.phone, name: row.name, gender: row.gender, birthday: row.birthday, levelId: row.levelId, remark: row.remark })
  } else {
    isEdit.value = false
    editId.value = null
    Object.assign(form, { phone: '', name: '', gender: 0, birthday: null, levelId: null, remark: '' })
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.phone) { ElMessage.warning('请输入手机号'); return }
  try {
    if (isEdit.value) {
      await memberApi.updateMember(editId.value, form)
    } else {
      await memberApi.createMember(form)
    }
    ElMessage.success(isEdit.value ? '已更新' : '已创建')
    dialogVisible.value = false
    loadData()
  } catch (e) { ElMessage.error('操作失败') }
}

const toggleStatus = async (row) => {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}会员「${row.name || row.phone}」？`, '提示')
    await memberApi.toggleMemberStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(`已${action}`)
    loadData()
  } catch { /* cancelled */ }
}

const showPointsDialog = (row) => {
  Object.assign(pointsForm, {
    memberId: row.id, name: row.name, phone: row.phone,
    availablePoints: row.availablePoints, change: 0, remark: '',
  })
  pointsDialogVisible.value = true
}

const savePoints = async () => {
  if (pointsForm.change === 0) { ElMessage.warning('请输入变动积分'); return }
  try {
    await memberApi.adjustPoints({
      memberId: pointsForm.memberId,
      points: pointsForm.change,
      remark: pointsForm.remark,
    })
    ElMessage.success('积分调整成功')
    pointsDialogVisible.value = false
    loadData()
  } catch (e) { ElMessage.error('调整失败') }
}

onMounted(() => { loadData(); loadLevels() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
