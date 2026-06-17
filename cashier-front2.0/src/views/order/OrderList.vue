<template>
  <div class="page-container">
    <el-card>
      <h3>{{ $t('pages.orderTitle') }}</h3>
      <el-table :data="data" border stripe v-loading="loading">
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="tableNumber" :label="$t('table.tableNumber')" width="80" />
        <el-table-column :label="$t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'settled' ? 'success' : row.status === 'cancelled' ? 'info' : 'warning'"
            >
              {{ row.status === 'settled' ? '已结账' : row.status === 'cancelled' ? '已取消' : '进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column :label="$t('common.operate')" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 服务端分页组件 -->
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

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px" @opened="showContent = true" @closed="showContent = false">
      <template v-if="showContent">
        <div v-if="detailOrder">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单ID">{{ detailOrder.id }}</el-descriptions-item>
            <el-descriptions-item label="订单编号">{{ detailOrder.orderNo }}</el-descriptions-item>
            <el-descriptions-item :label="$t('table.tableNumber')">{{ detailOrder.tableNumber }}</el-descriptions-item>
            <el-descriptions-item :label="$t('common.status')">
              {{ detailOrder.status === 'settled' ? '已结账' : detailOrder.status === 'cancelled' ? '已取消' : '进行中' }}
            </el-descriptions-item>
            <el-descriptions-item label="金额">
              ¥{{ detailOrder.finalAmount || detailOrder.totalAmount || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">
              {{ detailOrder.paymentMethod || '-' }}
            </el-descriptions-item>
          </el-descriptions>
          <h4 style="margin-top:16px">菜品明细</h4>
          <el-table :data="detailItems" stripe size="small">
            <el-table-column prop="dishName" :label="$t('table.dishName')" />
            <el-table-column prop="price" :label="$t('table.price')" width="80" />
            <el-table-column prop="quantity" :label="$t('table.quantity')" width="60" />
            <el-table-column :label="$t('pos.subtotal')" width="80">
              <template #default="{ row }">
                ¥{{ (row.price * row.quantity).toFixed(2) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { getOrderById, getOrderItemsByOrderId, getOrdersPage } from '@/api'
import { usePagination } from '@/composables/usePagination'

const user = useUserStore()
const storeId = computed(() => user.storeId)
const roleCode = computed(() => user.roleCode)

const { pageNum, pageSize, total, loading, data, load, onPageChange, onSizeChange, filters } =
  usePagination(
    (params) => {
      if (roleCode.value === 'ADMIN') {
        // 管理员不传 storeId 则查全部，传了则过滤
        return getOrdersPage(params)
      }
      // 非管理员：固定传入本店 storeId
      return getOrdersPage({ ...params, storeId: storeId.value })
    },
    {},
    10
  )

// 管理员切换门店筛选时重新加载
watch(storeId, () => {
  if (roleCode.value !== 'ADMIN' && storeId.value) {
    load()
  }
})

const detailVisible = ref(false)
const showContent = ref(false)
const detailOrder = ref(null)
const detailItems = ref([])

const showDetail = async (orderId) => {
  try {
    const [orderRes, itemsRes] = await Promise.all([
      getOrderById(orderId),
      getOrderItemsByOrderId(orderId)
    ])
    detailOrder.value = orderRes.data
    detailItems.value = itemsRes.data || []
    detailVisible.value = true
  } catch (_) {
    // 忽略
  }
}

onMounted(load)
</script>

