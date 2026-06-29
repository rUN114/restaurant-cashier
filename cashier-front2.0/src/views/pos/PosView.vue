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

        <!-- 桌台平面图 -->
        <div class="table-grid">
          <div
            v-for="t in tables"
            :key="t.id"
            :class="['table-block', t.status, { selected: t.id === selectedTableId }]"
            @click="onTableChange(t.id)"
          >
            <span class="tbl-num" :title="getTableLabel(t)">{{ t.tableNumber || t.id }}</span>
            <span class="tbl-st">{{ t.status === 'free' ? '空闲' : t.status === 'occupied' ? '用餐中' : t.status || '未知' }}</span>
          </div>
        </div>

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
            v-if="canShift"
            type="info"
            size="small"
            @click="openShift"
          >
            📋 交接班
          </el-button>

          <el-button link size="small" @click="toggleVoice" :type="voiceOn ? 'warning' : 'info'" style="margin-left:4px">
            {{ voiceOn ? '🔊' : '🔇' }}
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
              :class="['dish-card', { soldout: dish.isAvailable === 0 }]"
              @click="dish.isAvailable !== 0 && openDetail(dish)"
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
                <div v-if="dish.isAvailable === 0" class="soldout-badge">沽清</div>
                <div v-if="dish.isCombo === 1" class="combo-badge">套餐</div>
              </div>
              <div class="dish-name">{{ dish.name }}</div>
              <div class="dish-price">¥{{ dish.price }}</div>
              <el-button
                v-if="canToggleDish"
                link
                size="small"
                :type="dish.isAvailable === 1 ? 'danger' : 'success'"
                @click.stop="toggleSoldOut(dish)"
              >{{ dish.isAvailable === 1 ? '沽清' : '上架' }}</el-button>
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
            <el-table-column label="备注" width="110">
              <template #default="{ row }">
                <el-popover trigger="click" :width="220" v-if="canModifyOrder">
                  <template #reference>
                    <el-tag size="small" :type="row.remark ? 'warning' : 'info'" style="cursor:pointer">
                      {{ row.remark || '添加' }}
                    </el-tag>
                  </template>
                  <div class="quick-tags" style="margin-bottom:8px">
                    <el-tag v-for="tag in quickTags" :key="tag" size="small"
                      :type="row.remark === tag ? 'primary' : 'info'"
                      style="cursor:pointer;margin:2px"
                      @click="editRemark(row, tag)">{{ tag }}</el-tag>
                  </div>
                  <el-input v-model="row._remarkTemp" size="small" placeholder="自定义备注"
                    @keyup.enter="editRemark(row, row._remarkTemp)" />
                  <el-button size="small" type="primary" style="margin-top:6px" @click="editRemark(row, row._remarkTemp || '')">确认</el-button>
                </el-popover>
                <el-tag v-else size="small" :type="row.remark ? 'warning' : 'info'">
                  {{ row.remark || '-' }}
                </el-tag>
              </template>
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

    <!-- 交接班对话框 -->
    <el-dialog v-model="shiftVisible" title="📋 交接班" width="420px">
      <div v-if="shift" class="shift-summary">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="收银员ID">{{ shift.cashierId }}</el-descriptions-item>
          <el-descriptions-item label="今日总笔数">{{ shift.totalOrders }} 单</el-descriptions-item>
          <el-descriptions-item label="合计收款"><b style="font-size:20px;color:#409eff">¥{{ fmt(shift.totalAmount) }}</b></el-descriptions-item>
          <el-descriptions-item label="系统营业额">¥{{ fmt(shift.systemTotal) }}</el-descriptions-item>
          <el-descriptions-item label="对账差异">
            <span :style="{ color: diff === 0 ? '#67c23a' : '#f56c6c', fontWeight:'bold' }">
              {{ diff >= 0 ? '+' : '' }}¥{{ fmt(Math.abs(diff)) }}
              {{ diff === 0 ? '✓ 平账' : diff > 0 ? '(多收)' : '(少收)' }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>支付方式明细</el-divider>
        <el-table :data="shift.breakdown" stripe size="small">
          <el-table-column label="支付方式" width="100">
            <template #default="{ row }">{{ payLabel(row.method) }}</template>
          </el-table-column>
          <el-table-column prop="count" label="笔数" width="60" />
          <el-table-column label="金额">
            <template #default="{ row }">¥{{ fmt(row.amount) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else description="加载中..." />
      <template #footer>
        <el-button @click="shiftVisible = false">关闭</el-button>
        <el-button type="primary" @click="printShift">打印</el-button>
      </template>
    </el-dialog>

    <!-- 小票打印对话框 -->
    <el-dialog v-model="receiptVisible" title="🧾 结账小票" width="360px" class="receipt-dialog">
      <div v-if="receipt" class="receipt" id="receipt-print">
        <div style="text-align:center;margin-bottom:12px">
          <h3 style="margin:0">🏪 餐厅结账单</h3>
          <p style="color:#999;margin:4px 0">桌号: {{ receipt.tableNumber }}</p>
          <p style="color:#999;margin:0">{{ receipt.time }}</p>
        </div>
        <el-divider style="margin:8px 0" />
        <table style="width:100%;font-size:14px;border-collapse:collapse">
          <tr v-for="item in receipt.items" :key="item.id" style="border-bottom:1px dashed #ddd">
            <td style="padding:4px 0">{{ item.dishName }}</td>
            <td style="text-align:center;width:40px">×{{ item.quantity }}</td>
            <td style="text-align:right;width:60px">¥{{ (item.price * item.quantity).toFixed(2) }}</td>
          </tr>
        </table>
        <el-divider style="margin:8px 0" />
        <div style="display:flex;justify-content:space-between;font-weight:bold">
          <span>合计</span><span style="font-size:20px;color:#409eff">¥{{ Number(receipt.total).toFixed(2) }}</span>
        </div>
        <div style="display:flex;justify-content:space-between;margin-top:4px">
          <span>支付方式</span><span>{{ receipt.payment }}</span>
        </div>
        <div v-if="receipt.discount > 0" style="display:flex;justify-content:space-between;margin-top:4px">
          <span>优惠</span><span style="color:#f56c6c">-¥{{ Number(receipt.discount).toFixed(2) }}</span>
        </div>
        <p style="text-align:center;color:#999;margin-top:16px">谢谢惠顾！</p>
      </div>
      <template #footer>
        <el-button @click="receiptVisible = false">关闭</el-button>
        <el-button type="primary" @click="printReceipt">🖨️ 打印</el-button>
      </template>
    </el-dialog>

    <!-- 退菜原因对话框 -->
    <el-dialog v-model="returnDialogVisible" title="退菜原因" width="360px">
      <el-radio-group v-model="returnReason">
        <el-radio label="做错了" style="margin-bottom:12px;display:block" />
        <el-radio label="上慢了" style="margin-bottom:12px;display:block" />
        <el-radio label="客人不要了" style="margin-bottom:12px;display:block" />
        <el-radio label="食材问题" style="margin-bottom:12px;display:block" />
        <el-radio label="点多了" style="margin-bottom:12px;display:block" />
        <el-radio label="其他" />
      </el-radio-group>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReturn">确认退菜</el-button>
      </template>
    </el-dialog>

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
        <el-input
          v-model="dishRemark"
          placeholder="备注：如 少辣、不要香菜..."
          size="small"
          style="margin-top: 12px"
          clearable
        />
        <div class="quick-tags">
          <el-tag v-for="tag in quickTags" :key="tag" :type="dishRemark === tag ? 'primary' : 'info'"
            size="small" @click="dishRemark = dishRemark === tag ? '' : tag" style="cursor:pointer">
            {{ tag }}
          </el-tag>
        </div>
        <div style="display: flex; justify-content: center; margin-top: 12px">
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
  toggleDish,
  createOrder,
  getOrderItemsByOrderId,
  addOrderItem,
  updateOrderItem,
  deleteOrderItem,
  settleOrder,
  cancelOrder,
  getShiftSummary,
  updateTableStatus,
  getInProgressOrders,
  getAllStores,
} from '@/api'

import { ElMessage, ElMessageBox } from 'element-plus'
import { checkUnfinished } from '@/api/kds'
import { useVoice } from '@/composables/useVoice'
const voice = useVoice()
const voiceOn = ref(voice.isOn())
const toggleVoice = () => { voiceOn.value = voice.toggle() }
import wechatQrImg from '@/assets/wechat-qr.png'
import alipayQrImg from '@/assets/alipay-qr.png'

const user = useUserStore()
const storeId = computed(() => user.storeId)
const roleCode = computed(() => user.roleCode)

const canCreateOrder = computed(() =>
  ['ADMIN', 'STORE_ADMIN', 'WAITER', 'CHEF'].includes(roleCode.value),
)
const canSettleOrder = computed(() => ['ADMIN', 'STORE_ADMIN', 'CASHIER'].includes(roleCode.value))
const canShift = computed(() => ['ADMIN', 'STORE_ADMIN', 'CASHIER'].includes(roleCode.value))
const canCancelOrder = computed(() =>
  ['ADMIN', 'STORE_ADMIN', 'WAITER'].includes(roleCode.value),
)
const canModifyOrder = computed(() =>
  ['ADMIN', 'STORE_ADMIN', 'WAITER', 'CHEF'].includes(roleCode.value),
)
const canToggleDish = computed(() => ['ADMIN', 'STORE_ADMIN'].includes(roleCode.value))

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
const dishRemark = ref('')
const quickTags = ['少辣', '不要香菜', '不要葱', '打包', '加急', '少盐', '不要蒜', '多煮一会']

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
  // 点击已选中的桌台取消选择
  if (selectedTableId.value === tableId) {
    selectedTableId.value = null
    currentOrder.value = null
    orderItems.value = []
    return
  }
  selectedTableId.value = tableId
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
  dishRemark.value = ''
  detailVisible.value = true
}

const addFromDetail = async (dish) => {
  if (!canModifyOrder.value) return ElMessage.warning('没有点菜权限')
  if (!currentOrder.value?.id) return ElMessage.warning('请先开单')

  // 套餐：拆成多个菜品
  if (dish.isCombo === 1 && dish.comboDishes) {
    const ids = dish.comboDishes.split(',').map(Number).filter(Boolean)
    const members = ids.map(id => dishes.value.find(d => d.id === id)).filter(Boolean)
    if (members.length === 0) return ElMessage.warning('套餐配置错误')
    for (const md of members) {
      await singleAddItem(md)
    }
    detailVisible.value = false
    return ElMessage.success(`已添加套餐「${dish.name}」(${members.length}道)`)
  }
  await singleAddItem(dish)
  ElMessage.success(`已添加 ${dish.name}`)
  detailVisible.value = false
}

const singleAddItem = async (dish) => {
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
      remark: dishRemark.value || undefined,
    })
    await refreshItems()
  }
}

const editRemark = async (row, remark) => {
  row.remark = remark === row.remark ? '' : remark
  row._remarkTemp = ''
  await updateOrderItem(row.id, {
    quantity: row.quantity,
    price: row.price,
    dishId: row.dishId,
    dishName: row.dishName,
    orderId: row.orderId,
    remark: row.remark || null,
  })
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

const toggleSoldOut = async (dish) => {
  try {
    await toggleDish(dish.id)
    dish.isAvailable = dish.isAvailable === 1 ? 0 : 1
  } catch (_) {
    ElMessage.error('操作失败')
  }
}

const returnReason = ref('')
const returnItem = ref(null)
const returnDialogVisible = ref(false)

// 小票
const receiptVisible = ref(false)
const receipt = ref(null)

// 交接班
const shiftVisible = ref(false)
const shift = ref(null)

const openShift = async () => {
  shiftVisible.value = true
  shift.value = null
  try {
    const res = await getShiftSummary()
    shift.value = res.data
  } catch (_) {
    ElMessage.error('加载失败')
    shiftVisible.value = false
  }
}

const diff = computed(() => {
  if (!shift.value) return 0
  return (Number(shift.value.totalAmount) || 0) - (Number(shift.value.systemTotal) || 0)
})

const fmt = (v) => (Number(v) || 0).toFixed(2)

const payLabel = (m) => {
  const map = { CASH: '现金', WECHAT: '微信', ALIPAY: '支付宝', CARD: '银行卡' }
  return map[m] || m
}

const printShift = () => window.print()
const printReceipt = () => {
  const el = document.getElementById('receipt-print')
  if (!el) return
  const w = window.open('', '_blank', 'width=400,height=600')
  w.document.write(`<html><head><title>小票</title><style>
    body{font-family:monospace;padding:20px;max-width:320px;margin:auto}
    h3{margin:0}table{width:100%;border-collapse:collapse}
    td{padding:4px 0}@media print{@page{margin:0}}
  </style></head><body>${el.innerHTML}</body></html>`)
  w.document.close()
  setTimeout(() => w.print(), 300)
}

const removeItem = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定退菜「${row.dishName}」？`,
      '退菜确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  returnItem.value = row
  returnReason.value = '其他'
  returnDialogVisible.value = true
}

const confirmReturn = async () => {
  const row = returnItem.value
  await deleteOrderItem(row.id, returnReason.value || '未指定')
  ElMessage.success('已退菜')
  returnDialogVisible.value = false
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
  // 先查是否已注册
  try {
    const existRes = await memberApi.searchMemberByPhone(memberPhone.value)
    if (existRes.data) {
      ElMessage.warning('该手机号已注册')
      return
    }
  } catch (_) { /* 未找到或其他错误，继续尝试注册 */ }

  try {
    const res = await memberApi.registerMember({ phone: memberPhone.value })
    member.value = res.data
    ElMessage.success('会员注册成功')
    if (res.data) recalcCheckout()
  } catch (e) {
    const msg = e?.response?.data?.msg || '注册失败'
    ElMessage.error(msg)
  }
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

  // 检查厨房是否有未完成的菜品
  try {
    const checkRes = await checkUnfinished(currentOrder.value.id)
    if (checkRes?.data?.hasUnfinished) {
      const items = checkRes.data.items || []
      const dishNames = items.map(i => `「${i.dishName}」(${i.kitchenStatus === 'WAITING' ? '待做' : i.kitchenStatus === 'COOKING' ? '制作中' : '已完成'})`).join('、')
      await ElMessageBox.confirm(
        `以下菜品厨房尚未出餐：\n\n${dishNames}\n\n确定要继续结账吗？结账后厨房看板将不再显示这些菜品。`,
        '厨房菜品未完成',
        { confirmButtonText: '继续结账', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: false }
      )
    }
  } catch (e) {
    // 用户取消或检查失败（网络问题不阻塞结账）
    if (e === 'cancel' || e === 'close') return
  }

  try {
    if (!user.userId) await user.fetchCurrentUser()
  } catch {
    return ElMessage.warning('获取用户信息失败，请刷新重试')
  }
  const cashierId = user.userId
  if (!cashierId) return ElMessage.warning('无法获取收银员信息')

  // 最终实付金额：优先会员结算价，其次手动抹零额，兜底订单合计
  const finalPay = (() => {
    if (memberCheckout.value?.finalPayAmount != null) {
      const v = Number(memberCheckout.value.finalPayAmount)
      if (!isNaN(v) && v >= 0) return v
    }
    const f = Number(finalAmount.value)
    if (!isNaN(f) && f > 0) return f
    // 兜底：会员信息加载失败且未手动输入抹零时，按订单原价结算
    return Math.max(0, Number(total.value) || 0)
  })()

  try {
    // 生成唯一请求 ID，后端 Redis SETNX 防重放
    const requestId = crypto.randomUUID()
    const settleRes = await settleOrder(
      currentOrder.value.id,
      cashierId,
      paymentMethod.value || 'CASH',
      discountAmount.value || 0,
      finalPay,
      member.value?.id || null,
      useMemberDiscount.value || false,
      memberCheckout.value?.redeemPoints || 0,
      requestId,
    )
    if (settleRes && settleRes.code !== 200) {
      ElMessage.error(settleRes.msg || '结账失败')
      return
    }
    ElMessage.success('结账成功')
    // 语音播报
    voice.speak(`${selectedTable?.name || currentOrder.value.tableNumber || '某桌'}已结账`)
    // 显示小票
    receipt.value = {
      tableNumber: selectedTable?.name || currentOrder.value.tableNumber || '-',
      time: new Date().toLocaleString('zh-CN'),
      items: [...orderItems.value],
      total: finalPay,
      payment: payLabel(paymentMethod.value),
      discount: Number(discountAmount.value) || 0,
    }
    receiptVisible.value = true
    if (member.value && memberCheckout.value?.expectedPoints > 0) {
      ElMessage.info(`会员获得 ${memberCheckout.value.expectedPoints} 积分`)
    }
    if (selectedTableId.value) {
      await updateTableStatus(selectedTableId.value, 'free').catch(e => console.error('更新桌台状态失败:', e))
    }
    currentOrder.value = null
    orderItems.value = []
    discountAmount.value = 0
    removeMember()
    await loadData().catch(e => console.error('加载数据失败:', e))
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
    currentOrder.value = null
    orderItems.value = []
    discountAmount.value = 0
    removeMember()
    // 从服务端刷新桌台状态，避免乐观更新与服务端不一致
    await loadData().catch(e => console.error('加载数据失败:', e))
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
  margin-bottom: 16px;
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
  gap: 12px;
  margin-top: 8px;
}
.dish-card {
  cursor: pointer;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px;
  text-align: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: border-color 0.2s, box-shadow 0.2s;
}
/* 桌台平面图 */
.table-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.table-block {
  min-width: 64px;
  height: 56px;
  padding: 4px 6px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}
.table-block.free { background: #e8f5e9; color: #2e7d32; border-color: #a5d6a7; }
.table-block.occupied { background: #ffebee; color: #c62828; border-color: #ef9a9a; }
.table-block.selected { border-color: #409eff !important; box-shadow: 0 0 0 3px rgba(64,158,255,0.3); transform: scale(1.05); }
.table-block .tbl-num { font-size: 16px; font-weight: bold; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }
.table-block .tbl-st { font-size: 9px; margin-top: 2px; white-space: nowrap; }

.dish-card.soldout {
  opacity: 0.5;
  pointer-events: none;
}
.dish-card.soldout .soldout-badge {
  position: absolute; top: 4px; right: 4px;
  background: #f56c6c; color: #fff; font-size: 11px;
  padding: 1px 6px; border-radius: 4px;
}
.combo-badge {
  position: absolute; top: 4px; left: 4px;
  background: #e040fb; color: #fff; font-size: 11px;
  padding: 1px 6px; border-radius: 4px;
}
.dish-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
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
.quick-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.quick-tags .el-tag { cursor: pointer !important; user-select: none; }
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
    padding: 8px;
  }
  .dish-grid {
    grid-template-columns: repeat(3, 1fr) !important;
    gap: 6px;
  }
  .dish-card {
    padding: 8px;
  }
  .dish-img {
    height: 60px;
  }
  .dish-img .el-image {
    height: 60px !important;
  }
  .dish-name {
    font-size: 12px;
  }
  .dish-price {
    font-size: 11px;
  }
  .table-block {
    min-width: 44px;
    height: 44px;
    padding: 2px 3px;
  }
  .table-block .tbl-num {
    font-size: 12px;
  }
  .table-block .tbl-st {
    font-size: 8px;
  }
}
</style>
