<template>
  <div class="page-container">
    <el-card>
      <div class="page-header">
        <h3>系统配置</h3>
        <el-button type="danger" size="small" @click="handleBackup">💾 备份数据库</el-button>
      </div>
      <el-table :data="configs" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="configKey" label="配置键" width="220" />
        <el-table-column prop="configValue" label="配置值">
          <template #default="{ row }">
            <template v-if="editingId === row.id">
              <el-input v-model="editValue" size="small" style="width:200px" />
              <el-button size="small" type="primary" @click="save(row)">保存</el-button>
              <el-button size="small" @click="editingId = null">取消</el-button>
            </template>
            <template v-else>
              {{ row.configValue }}
              <el-button link size="small" type="primary" @click="startEdit(row)">编辑</el-button>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllConfigs, updateConfig } from '@/api/cost'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const configs = ref([])
const loading = ref(false)
const editingId = ref(null)
const editValue = ref('')

const load = async () => {
  loading.value = true
  try {
    const res = await getAllConfigs()
    configs.value = res.data || []
  } finally { loading.value = false }
}

const startEdit = (row) => {
  editingId.value = row.id
  editValue.value = row.configValue
}

const save = async (row) => {
  try {
    await updateConfig({ ...row, configValue: editValue.value })
    ElMessage.success('已更新')
    editingId.value = null
    load()
  } catch (_) { ElMessage.error('更新失败') }
}

const handleBackup = async () => {
  try {
    const token = document.cookie.split(';').find(c => c.includes('cashier-token'))?.split('=')[1] || ''
    const resp = await fetch('/api/sys-configs/backup', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!resp.ok) throw new Error()
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `backup_${new Date().toISOString().slice(0,10)}.sql`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('备份下载中...')
  } catch (_) { ElMessage.error('备份失败') }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
