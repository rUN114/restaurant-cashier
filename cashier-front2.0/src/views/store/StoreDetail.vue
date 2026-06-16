<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <div class="page-header">
        <h3>{{ $t('pages.storeDetailTitle') }}</h3>
        <el-tag v-if="store.status === 1" type="success">{{ $t('table.operating') }}</el-tag>
        <el-tag v-else type="info">{{ $t('table.closed') }}</el-tag>
      </div>
      <el-descriptions :column="2" border v-if="store.id">
        <el-descriptions-item :label="$t('table.id')">{{ store.id }}</el-descriptions-item>
        <el-descriptions-item :label="$t('table.name')">{{ store.name }}</el-descriptions-item>
        <el-descriptions-item :label="$t('table.address')" :span="2">{{ store.address }}</el-descriptions-item>
        <el-descriptions-item :label="$t('table.phone')">{{ store.phone || $t('table.remark') }}</el-descriptions-item>
        <el-descriptions-item :label="$t('common.createTime')">{{ store.createdAt || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-if="!loading && !store.id" :description="$t('common.noData')" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStoreById } from '@/api'
import { useUserStore } from '@/stores/user'
const user = useUserStore()
const store = ref({})
const loading = ref(false)
onMounted(async () => {
  if (!user.storeId) return
  loading.value = true
  try { store.value = (await getStoreById(user.storeId)).data || {} } finally { loading.value = false }
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
