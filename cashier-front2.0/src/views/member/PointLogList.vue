<template>
  <div class="point-log-list">
    <el-card>
      <template #header><span>{{ $t('member.pointLog') }}</span></template>

      <el-row :gutter="12" style="margin-bottom: 12px">
        <el-col :span="8">
          <el-input v-model="searchPhone" :placeholder="$t('member.inputPhone')" clearable @keyup.enter="search" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="search" :disabled="!searchPhone">{{ $t('member.search') }}</el-button>
        </el-col>
      </el-row>

      <template v-if="member">
        <el-descriptions :column="4" border style="margin-bottom: 16px">
          <el-descriptions-item :label="$t('member.name')">{{ member.name }}</el-descriptions-item>
          <el-descriptions-item :label="$t('member.phone')">{{ member.phone }}</el-descriptions-item>
          <el-descriptions-item :label="$t('member.availablePoints')">{{ member.availablePoints }}</el-descriptions-item>
          <el-descriptions-item :label="$t('member.totalConsumption')">{{ member.totalConsumption }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="logs" v-loading="loading" border stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column :label="$t('table.status')" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.changeType === 1" type="success">{{ $t('member.consumeEarn') }}</el-tag>
              <el-tag v-else-if="row.changeType === 2" type="warning">{{ $t('member.redeemDeduct') }}</el-tag>
              <el-tag v-else-if="row.changeType === 3" type="danger">{{ $t('member.refundDeduct') }}</el-tag>
              <el-tag v-else-if="row.changeType === 4" type="primary">{{ $t('member.manualAdd') }}</el-tag>
              <el-tag v-else-if="row.changeType === 5" type="danger">{{ $t('member.manualDeduct') }}</el-tag>
              <el-tag v-else>{{ $t('member.other') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="changePoints" :label="$t('member.changePoints')" width="100">
            <template #default="{ row }">
              <span :style="{ color: row.changePoints > 0 ? 'green' : 'red', fontWeight: 'bold' }">
                {{ row.changePoints > 0 ? '+' : '' }}{{ row.changePoints }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="beforePoints" :label="$t('member.beforeChange')" width="80" />
          <el-table-column prop="afterPoints" :label="$t('member.afterChange')" width="80" />
          <el-table-column prop="relatedAmount" :label="$t('member.relatedAmount')" width="100" />
          <el-table-column prop="remark" :label="$t('member.remark')" min-width="150" />
          <el-table-column prop="createdAt" :label="$t('member.time')" width="170" />
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
      <el-empty v-else :description="$t('member.inputPhone')" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import * as memberApi from '@/api/member'

const { t } = useI18n()

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
    if (!res.data) { ElMessage.warning(t('member.notFound')); return }
    loadLogs()
  } catch (e) {
    member.value = null; logs.value = []
    ElMessage.warning(t('member.notFound'))
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
