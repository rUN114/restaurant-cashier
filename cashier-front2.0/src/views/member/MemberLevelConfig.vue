<template>
  <div class="member-level-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>会员等级配置</span>
          <el-button type="primary" @click="showDialog(null)">新增等级</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="sortOrder" label="排序" width="60" />
        <el-table-column prop="name" label="等级名称" width="120" />
        <el-table-column label="颜色" width="80">
          <template #default="{ row }">
            <span :style="{ color: row.color, fontWeight: 'bold' }">● {{ row.color }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="minConsumption" label="升级门槛(元)" width="120" />
        <el-table-column label="折扣" width="80">
          <template #default="{ row }">
            <el-tag>{{ row.discountRate }} 折</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="del(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑等级' : '新增等级'" width="420px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="等级名称" required>
          <el-input v-model="form.name" placeholder="如：银卡会员" />
        </el-form-item>
        <el-form-item label="排序" required>
          <el-input-number v-model="form.sortOrder" :min="1" />
        </el-form-item>
        <el-form-item label="升级门槛(元)" required>
          <el-input-number v-model="form.minConsumption" :min="0" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item label="折扣率">
          <el-input-number v-model="form.discountRate" :min="0" :max="10" :precision="2" :step="0.5" />
          <span style="margin-left: 8px; color: #999">10.00=不打折，9.50=9.5折</span>
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" />
          <span style="margin-left: 8px">{{ form.color }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as memberApi from '@/api/member'

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
  if (row) {
    editId.value = row.id
    Object.assign(form, row)
  } else {
    editId.value = null
    Object.assign(form, { name: '', sortOrder: 1, minConsumption: 0, discountRate: 10, color: '#999', remark: '' })
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.name) { ElMessage.warning('请输入等级名称'); return }
  try {
    editId.value ? await memberApi.updateMemberLevel(editId.value, form) : await memberApi.createMemberLevel(form)
    ElMessage.success(editId.value ? '已更新' : '已创建')
    dialogVisible.value = false
    loadData()
  } catch (e) { ElMessage.error('操作失败') }
}

const del = async (id) => {
  await memberApi.deleteMemberLevel(id)
  ElMessage.success('已删除')
  loadData()
}

onMounted(loadData)
</script>
<style scoped>.card-header { display: flex; justify-content: space-between; align-items: center; }</style>
