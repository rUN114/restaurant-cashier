<template>
  <div class="page-container">
    <el-card>
      <h3>{{ $t('pages.loginLogTitle') }}</h3>
      <el-table :data="data" border stripe v-loading="loading">
        <el-table-column prop="id" :label="$t('table.id')" width="60" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="storeId" :label="$t('table.store')" width="80" />
        <el-table-column prop="loginIp" label="{{ $t('login.loginBtn') }}IP" width="140" />
        <el-table-column :label="$t('common.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.loginStatus === 1 ? 'success' : 'danger'">
              {{ row.loginStatus === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="{{ $t('login.loginBtn') }}时间" width="180" />
        <el-table-column prop="loginDevice" label="设备" show-overflow-tooltip />
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
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { getLoginLogsPage } from '@/api'
import { usePagination } from '@/composables/usePagination'

const { pageNum, pageSize, total, loading, data, load, onPageChange, onSizeChange } =
  usePagination((params) => getLoginLogsPage(params), {}, 10)

onMounted(load)
</script>

<style scoped>
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
