<template>
  <div class="point-log-list">
    <el-card>
      <template #header><span>积分流水查询</span></template>

      <el-row :gutter="12" style="margin-bottom: 12px">
        <el-col :span="8">
          <el-input v-model="searchPhone" placeholder="输入会员手机号" clearable @keyup.enter="search" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="search" :disabled="!searchPhone">查询</el-button>
        </el-col>
      </el-row>

      <template v-if="member">
        <el-descriptions :column="4" border style="margin-bottom: 16px">
          <el-descriptions-item label="会员">{{ member.name }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ member.phone }}</el-descriptions-item>
          <el-descriptions-item label="可用积分">{{ member.availablePoints }}</el-descriptions-item>
          <el-descriptions-item label="累计消费">{{ member.totalConsumption }} 元</el-descriptions-item>
        </el-descriptions>

        <el-table :data="logs" v-loading="loading" border stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.changeType === 1" type="success">消费加积分</el-tag>
              <el-tag v-else-if="row.changeType === 2" type="warning">抵现扣积分</el-tag>
              <el-tag v-else-if="row.changeType === 3" type="danger">退款扣积分</el-tag>
              <el-tag v-else-if="row.changeType === 4" type="primary">手动增加</el-tag>
              <el-tag v-else-if="row.changeType === 5" type="danger">手动扣减</el-tag>
              <el-tag v-else>其他</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="changePoints" label="变动积分" width="100">
            <template #default="{ row }">
              <span :style="{ color: row.changePoints > 0 ? 'green' : 'red', fontWeight: 'bold' }">
                {{ row.changePoints > 0 ? '+' : '' }}{{ row.changePoints }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="beforePoints" label="变动前" width="80" />
          <el-table-column prop="afterPoints" label="变动后" width="80" />
          <el-table-column prop="relatedAmount" label="关联金额" width="100" />
          <el-table-column prop="remark" label="备注" min-width="150" />
          <el-table-column prop="createdAt" label="时间" width="170" />
        </el-table>

        <el-pagination
          v-if="total > 0"
          style="margin-top: 16px; justify-content: flex-end"
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadLogs"
        />
      </template>
      <el-empty v-else description="输入手机号查询会员积分流水" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as memberApi from '@/api/member'

const searchPhone = ref('')
const member = ref(null)
const logs = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const search = async () => {
  try {
    const res = await memberApi.searchMemberByPhone(searchPhone.value)
    member.value = res.data
    loadLogs()
  } catch (e) {
    member.value = null
    logs.value = []
    ElMessage.warning('未找到会员')
  }
}

const loadLogs = async () => {
  if (!member.value) return
  loading.value = true
  try {
    const res = await memberApi.getPointLogs(member.value.id, { pageNum: pageNum.value, pageSize: pageSize.value })
    logs.value = res.data || []
    total.value = res.total || 0
  } finally { loading.value = false }
}
</script>
