<template>
  <div class="pos-container">
    <div class="pos-left">
      <div class="table-bar">
        <!-- 管理员门店筛选 -->
        <el-select
          v-if="roleCode === 'ADMIN'"
          v-model="selectedStoreId"
          :placeholder="$t('common.pleaseSelect')"
          class="store-select"
          @change="onStoreChange"
        >
          <el-option :label="$t('common.pleaseSelect')" value="all" />
          <el-option
            v-for="store in storeList"
            :key="store.id"
            :label="store.name"
            :value="store.id"
          />
        </el-select>

        <!-- 桌台选择 -->
        <el-select
          v-model="selectedTableId"
          :placeholder="$t('common.pleaseSelect')"
          clearable
          class="table-select"
          @change="onTableChange"
        >
          <el-option v-for="t in tables" :key="t.id" :label="getTableLabel(t)" :value="t.id" />
        </el-select>

        <div class="action-buttons">
          <el-button
            v-if="canCreateOrder"
            type="primary"
            @click="handleOpenOrder"
            :disabled="!selectedTableId || selectedTable?.status !== 'free'"
            size="small"
          >
            开单
          </el-button>

          <el-button
            v-if="canSettleOrder && currentOrder"
            type="warning"
            @click="handleSettle"
            :disabled="orderItems.length === 0"
            size="small"
          >
            结账
          </el-button>

          <el-button
            v-if="canCancelOrder && currentOrder"
            type="danger"
            @click="handleCancel"
            size="small"
          >
            退单
          </el-button>
        </div>

        <span v-if="currentOrder" class="current-order-info">
          订单 #{{ currentOrder.id }} 桌号: {{ selectedTable?.tableNumber }}
        </span>
      </div>

      <!-- 分类标签页（含“全部”） -->
      <el-tabs v-model="activeCatId" type="card" class="cat-tabs">
        <el-tab-pane v-for="cat in allCategories" :key="cat.id" :label="cat.name" :name="cat.id">
          <div class="dish-grid">
            <div
              v-for="dish in filteredDishes"
              :key="dish.id"
              class="dish-card"
              @click="openDetail(dish)"
            >
              <div class="dish-img">
                <el-image
                  v-if="dish.imageUrl"
                  :src="dish.imageUrl"
                  fit="cover"
                  lazy
                  style="width: 100%; height: 80px"
                />
                <div v-else class="dish-placeholder">{{ ("common.noData") }}</div>
              </div>
              <div class="dish-name">{{ dish.name }}</div>
              <div class="dish-price">¥{{ dish.price }}</div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="pos-right">
      <div class="order-items-section">
        <h4>{{ ("pos.orderDetail") }}</h4>
        <div class="table-wrapper">
          <el-table :data="orderItems" max-height="400" stripe size="small">
            <el-table-column prop="dishName" :label="$t('table.dishName')" />
            <el-table-column prop="price" :label="$t('table.price')" width="80">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="quantity" :label="$t('table.quantity')" width="100" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  size="small"
                  :disabled="!canModifyOrder"
                  @change="changeQty(row)"
                  controls-position="right"
                />
              </template>
            </el-table-column>
            <el-table-column :label="$t('pos.subtotal')" width="80">
              <template #default="{ row }"> ¥{{ (row.price * row.quantity).toFixed(2) }} </template>
            </el-table-column>
            <!-- 只要订单进行中，任何人都可以删除菜品 -->
            <el-table-column :label="$t('common.operate')" width="60">
              <template #default="{ row }">
                <el-button link type="danger" :disabled="!canModifyOrder" @click="removeItem(row)">{{ $t("common.delete") }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 会员搜索 -->
      <div class="member-search-box" v-if="currentOrder && canSettleOrder">
        <el-input v-model="memberPhone" placeholder="输入会员手机号" size="small" clearable
          @keyup.enter="searchMember" style="width: 160px" />
        <el-button size="small" @click="searchMember" :loading="memberLoading">查询</el-button>
        <el-button size="small" @click="registerMemberQuick" v-if="memberPhone && !member">快速注册</el-button>
      </div>

      <!-- 会员信息展示 -->
      <div v-if="member" class="member-info-box">
        <el-tag :color="member.level?.color" effect="dark" size="small">{{ member.level?.name }}</el-tag>
        <span>{{ member.name }}（{{ member.phone }}）</span>
        <span>可用积分：<b>{{ member.availablePoints }}</b></span>
        <el-button size="small" type="danger" @click="removeMember" link>移除</el-button>

        <div class="member-options">
          <el-switch v-model="useMemberDiscount" active-text="会员折扣" size="small" />
          <template v-if="!useMemberDiscount && memberCheckout">
            <el-switch v-model="usePointsRedeem" active-text="积分抵现" size="small" />
            <template v-if="usePointsRedeem">
              <el-input-number v-model="redeemPoints" :min="0" :max="member.availablePoints" size="small" style="width: 100px" />
              <span style="font-size: 12px; color: #999">抵 ¥{{ redeemAmount }}</span>
            </template>
          </template>
        </div>
      </div>

      <div class="total-box">
        <span
          >{{ ("pos.total") }}：<b>¥{{ total }}</b></span
        >
        <div class="settle-actions" v-if="currentOrder && canSettleOrder">
          <el-select v-model="paymentMethod" :placeholder="$t('table.paymentMethod')" size="small" class="pay-select">
            <el-option :label="$t('table.cash')" value="CASH" />
            <el-option label="WeChat" value="WECHAT" />
            <el-option label="Alipay" value="ALIPAY" />
            <el-option label="银行卡" value="CARD" />
          </el-select>

          <div class="discount-box">
            <span class="discount-label">抹零：</span>
            <el-input-number
              v-model="discountAmount"
              :min="0"
              :max="Number(total)"
              :precision="2"
              size="small"
              class="discount-input"
            />
          </div>

          <div class="final-amount">
            <template v-if="memberPresent && memberCheckout">
              <div v-if="memberCheckout.discountAmount > 0" class="discount-info">
                会员折扣 -¥{{ Number(memberCheckout.discountAmount).toFixed(2) }}
              </div>
              <div v-if="memberCheckout.redeemPoints > 0" class="discount-info">
                积分抵现 -¥{{ Number(memberCheckout.redeemAmount).toFixed(2) }}
              </div>
            </template>
            <div v-if="memberPresent && memberCheckout" style="margin-top: 4px">
              实收：<b>¥{{ Number(memberCheckout.finalPayAmount || 0).toFixed(2) }}</b>
              <span v-if="Number(memberCheckout.finalPayAmount) !== Number(finalAmount)" style="font-size:12px;color:#999;text-decoration:line-through">
                ¥{{ finalAmount }}
              </span>
            </div>
            <div v-else>
              实收：<b>¥{{ finalAmount }}</b>
            </div>
          </div>

          <div v-if="paymentMethod === 'WECHAT' || paymentMethod === 'ALIPAY'" class="qr-code-box">
            <el-image :src="qrCodeUrl" fit="contain" lazy class="qr-img" />
            <p class="qr-tip">{{ paymentMethod === 'WECHAT' ? '微信收款码' : '支付宝收款码' }}</p>
          </div>

          <el-button type="success" @click="handleSettle" :disabled="!canSettle" size="small">
            立即结账
          </el-button>
        </div>
      </div>
    </div>

    <!-- 菜品详情对话框 -->
    <el-dialog v-model="detailVisible" title="菜品详情" width="400px">
      <div v-if="detailDish" class="dish-detail">
        <div class="detail-img">
          <el-image
            v-if="detailDish.imageUrl"
            :src="detailDish.imageUrl"
            fit="cover"
            lazy
            style="width: 100%; max-height: 250px; border-radius: 8px"
          />
          <div
            v-else
            class="dish-placeholder"
            style="
              height: 200px;
              line-height: 200px;
              color: #909399;
              text-align: center;
              background: #f5f7fa;
              border-radius: 8px;
            "
          >
            {{ ("common.noData") }}
          </div>
        </div>
        <h2 class="detail-name">{{ detailDish.name }}</h2>
        <p class="detail-price">¥{{ detailDish.price }} / {{ detailDish.unit }}</p>
        <p class="detail-category">分类：{{ getCategoryName(detailDish.categoryId) }}</p>
        <p v-if="detailDish.remark" class="detail-remark">
          <strong>备注：</strong>{{ detailDish.remark }}
        </p>
        <p v-else class="detail-remark" style="color: #ccc">暂无备注</p>
        <div style="display: flex; justify-content: center; margin-top: 20px">
          <el-button type="primary" @click="addFromDetail(detailDish)">加入订单</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
const { t } = useI18n()
import {
  getTablesByStore,
  getAllTables,
  getCategoriesByStore,
  getAllCategories,
  getAvailableDishes,
  getAllDishes,
  createOrder,
  getOrderItemsByOrderId,
  addOrderItem,
  updateOrderItem,
  deleteOrderItem,
  settleOrder,
  cancelOrder,
  updateTableStatus,
  getInProgressOrders,
  getAllStores,
} from '@/api'

import { ElMessage, ElMessageBox } from 'element-plus'
import wechatQrImg from '@/assets/wechat-qr.png'
import alipayQrImg from '@/assets/alipay-qr.png'

const user = useUserStore()
const storeId = computed(() => user.storeId)
const roleCode = computed(() => user.roleCode)

const canCreateOrder = computed(() =>
  ['ADMIN', 'STORE_ADMIN', 'WAITER', 'CHEF'].includes(roleCode.value),
)
const canSettleOrder = computed(() => ['ADMIN', 'STORE_ADMIN', 'CASHIER'].includes(roleCode.value))
const canCancelOrder = computed(() =>
  ['ADMIN', 'STORE_ADMIN', 'WAITER'].includes(roleCode.value),
)
const canModifyOrder = computed(() =>
  ['ADMIN', 'STORE_ADMIN', 'WAITER', 'CHEF'].includes(roleCode.value),
)

const selectedStoreId = ref('all')
const storeList = ref([])
const tables = ref([])
const categories = ref([])
const dishes = ref([])
const selectedTableId = ref(null)
const selectedTable = computed(() => tables.value.find((t) => t.id === selectedTableId.value))
const activeCatId = ref('all')
const filteredDishes = computed(() => {
  if (activeCatId.value === 'all') return dishes.value
  return dishes.value.filter((d) => d.categoryId == activeCatId.value)
})
const allCategories = computed(() => [{ id: 'all', name: '全部' }, ...categories.value])

const currentOrder = ref(null)
const orderItems = ref([])
const paymentMethod = ref('CASH')
const total = computed(() =>
  orderItems.value.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2),
)
const canSettle = computed(() => currentOrder.value && orderItems.value.length > 0)

const discountAmount = ref(0)
const finalAmount = computed(() => {
  const t = Number(total.value) || 0
  const d = Number(discountAmount.value) || 0
  return Math.max(0, t - d).toFixed(2)
})

const qrCodeUrl = computed(() => {
  if (paymentMethod.value === 'WECHAT') return wechatQrImg
  if (paymentMethod.value === 'ALIPAY') return alipayQrImg
  return ''
})

const detailVisible = ref(false)
const detailDish = ref(null)

const getTableLabel = (table) => {
  const statusText = table.status === 'free' ? t('table.free') : t('table.occupied')
  let label = `${table.tableNumber} (${statusText})`
  if (roleCode.value === 'ADMIN' && selectedStoreId.value === 'all') {
    const store = storeList.value.find((s) => s.id === table.storeId)
    label += ` - ${store?.name || '门店' + table.storeId}`
  }
  return label
}

const getCategoryName = (cid) => {
  if (!cid) return '未分类'
  const cat = categories.value.find((c) => c.id === cid)
  return cat ? cat.name : t('common.noData')
}

const loadStoreList = async () => {
  if (roleCode.value !== 'ADMIN') return
  const res = await getAllStores()
  storeList.value = res.data || []
}

const loadData = async () => {
  try {
    // 桌台加载
    if (roleCode.value === 'ADMIN') {
      tables.value =
        selectedStoreId.value === 'all'
          ? (await getAllTables()).data || []
          : (await getTablesByStore(selectedStoreId.value)).data || []
    } else {
      tables.value = storeId.value ? (await getTablesByStore(storeId.value)).data || [] : []
    }

    // 分类、菜品加载
    const catPromise =
      roleCode.value === 'ADMIN'
        ? selectedStoreId.value === 'all'
          ? getAllCategories()
          : getCategoriesByStore(selectedStoreId.value)
        : storeId.value
          ? getCategoriesByStore(storeId.value)
          : Promise.resolve({ data: [] })
    const dishPromise =
      roleCode.value === 'ADMIN'
        ? selectedStoreId.value === 'all'
          ? getAllDishes()
          : getAvailableDishes(selectedStoreId.value)
        : storeId.value
          ? getAvailableDishes(storeId.value)
          : Promise.resolve({ data: [] })

    const [cRes, dRes] = await Promise.all([catPromise, dishPromise])
    categories.value = cRes?.data || []
    dishes.value = dRes?.data || []
    if (roleCode.value === 'ADMIN' && selectedStoreId.value === 'all') {
      dishes.value = dishes.value.filter((d) => d.isAvailable === 1)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('数据加载失败')
  }
}

const onStoreChange = () => {
  selectedTableId.value = null
  currentOrder.value = null
  orderItems.value = []
  loadData()
}

onMounted(() => {
  loadStoreList()
  loadData()
})

// 核心修复：使用桌台本身的 storeId 加载订单
const onTableChange = async (tableId) => {
  currentOrder.value = null
  orderItems.value = []
  if (!tableId) return

  const table = tables.value.find((t) => t.id === tableId)
  if (!table) return
  const targetStoreId = table.storeId
  if (!targetStoreId) {
    ElMessage.warning('该桌台未绑定门店，无法加载订单')
    return
  }

  console.log('正在加载进行中订单，门店ID：', targetStoreId, '桌台ID：', tableId)

  try {
    const res = await getInProgressOrders(targetStoreId)
    console.log('进行中订单返回：', res)
    const orders = res?.data || []
    const order = orders.find((o) => o.tableId == tableId)
    if (order) {
      currentOrder.value = order
      const itemsRes = await getOrderItemsByOrderId(order.id)
      orderItems.value = (itemsRes?.data || []).map((item) => ({ ...item }))
      console.log('订单明细已加载', orderItems.value)
    } else {
      console.log('未找到该桌台的进行中订单')
    }
  } catch (e) {
    console.error('加载订单失败', e)
  }
}

const handleOpenOrder = async () => {
  if (!selectedTableId.value) return
  const table = tables.value.find((t) => t.id === selectedTableId.value)
  if (!table?.storeId) return ElMessage.warning('桌台数据错误')
  if (table.status !== 'free') return ElMessage.warning('该桌台已占用，不能开单')

  try {
    const res = await createOrder({
      storeId: table.storeId,
      tableId: table.id,
      status: 'IN_PROGRESS',
    })
    currentOrder.value = { id: res.data, tableId: table.id, status: 'IN_PROGRESS' }
    await updateTableStatus(table.id, 'occupied')
    ElMessage.success('开单成功')
    await loadData()
    selectedTableId.value = table.id
  } catch (e) {
    console.error(e)
    ElMessage.error('开单失败')
  }
}

const openDetail = (dish) => {
  detailDish.value = dish
  detailVisible.value = true
}

const addFromDetail = async (dish) => {
  if (!canModifyOrder.value) return ElMessage.warning('没有点菜权限')
  if (!currentOrder.value?.id) return ElMessage.warning('请先开单')
  const exist = orderItems.value.find((i) => i.dishId === dish.id)
  if (exist) {
    exist.quantity++
    await updateOrderItem(exist.id, {
      quantity: exist.quantity,
      price: exist.price,
      dishId: exist.dishId,
      dishName: exist.dishName,
      orderId: exist.orderId,
    })
  } else {
    await addOrderItem({
      orderId: currentOrder.value.id,
      dishId: dish.id,
      dishName: dish.name,
      price: dish.price,
      quantity: 1,
    })
    await refreshItems()
  }
  ElMessage.success(`已添加 ${dish.name}`)
  detailVisible.value = false
}

const changeQty = async (row) => {
  if (!canModifyOrder.value) return
  await updateOrderItem(row.id, {
    quantity: row.quantity,
    dishId: row.dishId,
    price: row.price,
    dishName: row.dishName,
    orderId: row.orderId,
  })
}

const removeItem = async (row) => {
  await deleteOrderItem(row.id)
  ElMessage.success('已退菜')
  refreshItems()
}

const refreshItems = async () => {
  if (!currentOrder.value?.id) return
  const res = await getOrderItemsByOrderId(currentOrder.value.id)
  orderItems.value = (res?.data || []).map((item) => ({ ...item }))
}

// ---- 会员 ----
import * as memberApi from '@/api/member'
const memberPhone = ref('')
const member = ref(null)
const memberLoading = ref(false)
const useMemberDiscount = ref(false)
const usePointsRedeem = ref(false)
const redeemPoints = ref(0)
const memberCheckout = ref(null)

const memberPresent = computed(() => !!member.value)
const redeemAmount = computed(() => {
  const v = memberCheckout.value?.redeemAmount
  if (v == null) return '0.00'
  const n = Number(v)
  return isNaN(n) ? '0.00' : n.toFixed(2)
})
const memberFinalPay = computed(() => {
  if (!memberCheckout.value) return finalAmount.value
  return memberCheckout.value.finalPayAmount
})

const searchMember = async () => {
  if (!memberPhone.value) return
  memberLoading.value = true
  try {
    const res = await memberApi.searchMemberByPhone(memberPhone.value)
    member.value = res.data
    if (res.data) recalcCheckout()
  } catch (e) {
    member.value = null
    // 显示后端返回的具体原因（未找到/已被禁用等）
    const msg = e?.response?.data?.msg || '未找到会员'
    ElMessage.warning(msg)
  } finally { memberLoading.value = false }
}

const registerMemberQuick = async () => {
  if (!memberPhone.value) return
  // 先查一下是否已注册
  try {
    const existRes = await memberApi.searchMemberByPhone(memberPhone.value)
    if (existRes.data) {
      if (existRes.data.status !== 1) {
        ElMessage.warning('该会员已被禁用，无法注册')
      } else {
        ElMessage.warning('该手机号已注册')
      }
      return
    }
  } catch (e) {
    // 未找到，继续注册
    const msg = e?.response?.data?.msg
    if (msg && msg.includes('禁用')) {
      ElMessage.warning(msg)
      return
    }
  }

  try {
    const res = await memberApi.registerMember({ phone: memberPhone.value })
    member.value = res.data
    ElMessage.success('会员注册成功')
    if (res.data) recalcCheckout()
  } catch (e) { ElMessage.error('注册失败') }
}

const removeMember = () => {
  member.value = null
  memberPhone.value = ''
  useMemberDiscount.value = false
  usePointsRedeem.value = false
  redeemPoints.value = 0
  memberCheckout.value = null
}

const recalcCheckout = async () => {
  if (!member.value || !currentOrder.value) return
  const totalVal = Number(total.value)
  if (!totalVal || totalVal <= 0) return  // 订单金额为0不计算
  try {
    const res = await memberApi.calcMemberCheckout({
      memberId: member.value.id,
      orderTotal: totalVal,
      useDiscount: useMemberDiscount.value,
      redeemPoints: usePointsRedeem.value ? redeemPoints.value : 0,
    })
    memberCheckout.value = (res && res.code === 200) ? res.data : null
  } catch {
    memberCheckout.value = null
  }
}

// 监听会员选项变化
import { watch } from 'vue'
watch([useMemberDiscount, usePointsRedeem, redeemPoints], () => {
  if (member.value) recalcCheckout()
})

const handleSettle = async () => {
  if (!currentOrder.value?.id) return
  if (!canSettleOrder.value) return ElMessage.warning('没有结账权限')

  // 校验是否有菜品
  if (!orderItems.value || orderItems.value.length === 0) {
    return ElMessage.warning('订单没有菜品，无法结账')
  }

  try {
    if (!user.userId) await user.fetchCurrentUser()
  } catch {
    return ElMessage.warning('获取用户信息失败，请刷新重试')
  }
  const cashierId = user.userId
  if (!cashierId) return ElMessage.warning('无法获取收银员信息')

  // 最终实付金额
  const finalPay = (() => {
    if (memberCheckout.value?.finalPayAmount != null) {
      const v = Number(memberCheckout.value.finalPayAmount)
      if (!isNaN(v) && v >= 0) return v
    }
    const f = Number(finalAmount.value)
    return isNaN(f) ? 0 : Math.max(0, f)
  })()

  try {
    const settleRes = await settleOrder(
      currentOrder.value.id,
      cashierId,
      paymentMethod.value || 'CASH',
      discountAmount.value || 0,
      finalPay,
      member.value?.id || null,
      useMemberDiscount.value || false,
      memberCheckout.value?.redeemPoints || 0,
    )
    if (settleRes && settleRes.code !== 200) {
      ElMessage.error(settleRes.msg || '结账失败')
      return
    }
    ElMessage.success('结账成功')
    if (member.value && memberCheckout.value?.expectedPoints > 0) {
      ElMessage.info(`会员获得 ${memberCheckout.value.expectedPoints} 积分`)
    }
    if (selectedTableId.value) {
      await updateTableStatus(selectedTableId.value, 'free').catch(() => {})
    }
    currentOrder.value = null
    orderItems.value = []
    discountAmount.value = 0
    removeMember()
    await loadData().catch(() => {})
  } catch (e) {
    console.error('结账异常:', e)
    const msg = e?.response?.data?.msg || e?.message || '结账失败，请重试'
    ElMessage.error(msg)
  }
}

const handleCancel = async () => {
  if (!currentOrder.value?.id) return
  if (!canCancelOrder.value) return ElMessage.warning('没有退单权限')

  try {
    await ElMessageBox.confirm(
      '确定要退单吗？退单后订单将被取消且无法恢复。',
      '退单确认',
      {
        confirmButtonText: '确定退单',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return // 用户取消对话框
  }

  try {
    await cancelOrder(currentOrder.value.id)
    ElMessage.success('退单成功')
    // 乐观更新本地桌台状态
    if (selectedTableId.value) {
      const t = tables.value.find((tbl) => tbl.id === selectedTableId.value)
      if (t) t.status = 'free'
    }
    currentOrder.value = null
    orderItems.value = []
    discountAmount.value = 0
    removeMember()
    await loadData().catch(() => {})
  } catch (e) {
    console.error('退单失败:', e)
    const msg = e?.response?.data?.msg || e?.message || '退单失败，请重试'
    ElMessage.error(msg)
  }
}
</script>

<style scoped>
.pos-container {
  display: flex;
  height: calc(100vh - 100px);
  gap: 16px;
}
.pos-left {
  flex: 1;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.pos-right {
  width: 420px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}
.table-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.store-select {
  width: 180px;
}
.table-select {
  width: 220px;
  flex: 1;
  min-width: 160px;
}
.action-buttons {
  display: flex;
  gap: 8px;
}
.current-order-info {
  color: #e6a23c;
  font-weight: bold;
  font-size: 14px;
}
.cat-tabs {
  flex: 1;
  overflow: hidden;
}
.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
  margin-top: 8px;
}
.dish-card {
  cursor: pointer;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px;
  text-align: center;
}
.dish-card:hover {
  border-color: #409eff;
}
.dish-img {
  height: 80px;
  background: #f5f7fa;
}
.dish-placeholder {
  width: 100%;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f5f7fa;
}
.dish-name {
  font-weight: bold;
  margin: 4px 0;
}
.dish-price {
  color: #f56c6c;
}
.order-items-section {
  flex: 1;
  overflow: hidden;
}
.table-wrapper {
  overflow-x: auto;
}
.total-box {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 16px;
}
.settle-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.pay-select {
  width: 100px;
}
.discount-box {
  display: flex;
  align-items: center;
  gap: 4px;
}
.discount-label {
  font-size: 13px;
}
.discount-input {
  width: 90px;
}
.final-amount {
  font-size: 14px;
  color: #f56c6c;
}
.qr-code-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 8px 0;
}
.qr-img {
  width: 140px;
  height: 140px;
}
.qr-tip {
  font-size: 12px;
  color: #909399;
}
.dish-detail {
  text-align: center;
}
.detail-img {
  margin-bottom: 16px;
}
.detail-name {
  font-size: 20px;
  margin: 10px 0;
}
.detail-price {
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
}
.detail-category {
  font-size: 14px;
  color: #606266;
  margin: 6px 0;
}
.detail-remark {
  font-size: 14px;
  color: #e6a23c;
  margin: 6px 0;
}
@media screen and (max-width: 768px) {
  .pos-container {
    flex-direction: column;
    height: auto;
  }
  .pos-left,
  .pos-right {
    width: 100%;
    padding: 12px;
  }
}
</style>
