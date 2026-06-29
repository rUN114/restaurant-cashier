<template>
  <div class="page-container">
    <el-card>
      <div class="header">
        <h3>{{ $t('pages.reportTitle') }}</h3>
        <el-button type="success" size="small" @click="exportReport">📥 导出</el-button>
        <div style="display: flex; gap: 16px; align-items: center; flex-wrap: wrap;">
          <el-date-picker
            v-model="selectedMonth"
            type="month"
            placeholder="选择月份"
            format="YYYY-MM"
            value-format="YYYY-MM"
            @change="loadAll"
            style="width: 160px"
          />
          <el-select
            v-if="isAdmin || roleCode === 'STORE_ADMIN'"
            v-model="selectedStoreId"
            :placeholder="$t('common.pleaseSelect')"
            :disabled="roleCode === 'STORE_ADMIN'"
            @change="loadAll"
            style="width: 160px"
          >
            <el-option :label="$t('common.pleaseSelect')" :value="null" />
            <el-option
              v-for="store in storeList"
              :key="store.id"
              :label="store.name"
              :value="store.id"
            />
          </el-select>
        </div>
      </div>

      <!-- 营业数据卡片 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :xs="24" :sm="8">
          <div class="stat-card total">
            <div class="label">营业总额</div>
            <div class="value">¥{{ storeTurnover }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="stat-card today">
            <div class="label">今日销售额</div>
            <div class="value">¥{{ todayTurnover }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="stat-card month">
            <div class="label">本月销售额</div>
            <div class="value">¥{{ monthTurnover }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :xs="24" :sm="12">
          <div class="chart-card">
            <div id="chart-day-trend" style="width: 100%; height: 350px;"></div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div class="chart-card">
            <div id="chart-payment-type" style="width: 100%; height: 350px;"></div>
          </div>
        </el-col>
      </el-row>

      <!-- 毛利报表 -->
      <div class="chart-card" style="margin-top:20px">
        <h4 style="margin-bottom:12px">菜品毛利报表</h4>
        <el-table :data="grossProfitData" stripe size="small" v-loading="loading">
          <el-table-column prop="name" label="菜品" />
          <el-table-column prop="quantity" label="销量" width="80" />
          <el-table-column label="销售额" width="120">
            <template #default="{ row }">¥{{ row.sales }}</template>
          </el-table-column>
          <el-table-column label="原料成本" width="120">
            <template #default="{ row }">¥{{ row.cost }}</template>
          </el-table-column>
          <el-table-column label="毛利" width="120">
            <template #default="{ row }">
              <span :style="{ color: row.profit >= 0 ? '#67c23a' : '#f56c6c' }">¥{{ row.profit }}</span>
            </template>
          </el-table-column>
          <el-table-column label="毛利率" width="100">
            <template #default="{ row }">
              <el-tag :type="row.margin >= 50 ? 'success' : row.margin >= 20 ? 'warning' : 'danger'" size="small">
                {{ row.margin }}%
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 员工绩效排行 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <div class="chart-card">
            <h4 style="margin-bottom:12px">🏆 今日收银排行</h4>
            <el-table :data="cashierRankData" stripe size="small" v-loading="rankLoading">
              <el-table-column type="index" label="排名" width="60" />
              <el-table-column prop="name" label="收银员" width="120" />
              <el-table-column prop="orders" label="订单数" width="80" sortable />
              <el-table-column label="营业额" width="120" sortable sort-by="amount">
                <template #default="{ row }">¥{{ Number(row.amount || 0).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column prop="returns" label="退菜" width="80" sortable />
              <el-table-column label="退菜率" width="100">
                <template #default="{ row }">
                  {{ row.orders > 0 ? (row.returns / row.orders * 100).toFixed(1) + '%' : '0%' }}
                </template>
              </el-table-column>
              <el-table-column label="评价" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.returns === 0 ? 'success' : row.returns <= 2 ? 'warning' : 'danger'" size="small">
                    {{ row.returns === 0 ? '🌟 完美' : row.returns <= 2 ? '👍 良好' : '需改进' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>

      <!-- 退菜原因统计 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :xs="24" :sm="12">
          <div class="chart-card">
            <h4 style="margin-bottom:12px">退菜原因分析</h4>
            <div id="chart-return-reasons" style="width: 100%; height: 320px;"></div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div class="chart-card">
            <h4 style="margin-bottom:12px">退菜明细</h4>
            <el-table :data="returnReasonData" stripe size="small" style="width:100%">
              <el-table-column prop="reason" label="原因" />
              <el-table-column prop="count" label="次数" width="80" sortable />
              <el-table-column label="占比" width="100">
                <template #default="{ row }">
                  {{ percent(row.count, totalReturns) }}%
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>

      <el-row v-if="isAdmin" :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <div class="chart-card">
            <div id="chart-store-turnover" style="width: 100%; height: 350px;"></div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useEcharts } from '@/composables/useEcharts'
import { useUserStore } from '@/stores/user'
const { t } = useI18n()
import { getAllStores } from '@/api'

import request from '@/utils/request'
import { getReturnReasons, getCashierRanking } from '@/api/order'
import { ElMessage } from 'element-plus'
import { exportExcel } from '@/composables/useExcel'

const user = useUserStore()
const isAdmin = computed(() => user.roleCode === 'ADMIN')
const roleCode = computed(() => user.roleCode)
const storeId = computed(() => user.storeId)

const { getEcharts } = useEcharts()

// 筛选条件
const now = new Date()
const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
const selectedMonth = ref(currentMonth)
const selectedStoreId = ref(null)
const storeList = ref([])

// 营业额数据
const storeTurnover = ref(0)
const todayTurnover = ref(0)
const monthTurnover = ref(0)
const grossProfitData = ref([])
const returnReasonData = ref([])
const totalReturns = ref(0)
const cashierRankData = ref([])
const rankLoading = ref(false)
const loading = ref(false)

const percent = (v, total) => total > 0 ? (v / total * 100).toFixed(1) : '0.0'

// 图表实例
let chartDayTrend = null
let chartPaymentType = null
let chartStoreTurnover = null
let chartReturnReasons = null

// 加载门店
const loadStores = async () => {
  if (roleCode.value !== 'ADMIN' && roleCode.value !== 'STORE_ADMIN') return
  try {
    const res = await getAllStores()
    storeList.value = res.data || []
    // 店长自动选中自己门店
    if (roleCode.value === 'STORE_ADMIN') {
      selectedStoreId.value = storeId.value
    }
  } catch (e) {}
}

// 统一加载所有数据
const loadAll = async () => {
  const month = selectedMonth.value
  if (!month) return

  const storeParam = isAdmin.value ? selectedStoreId.value : storeId.value
  const params = { month }
  if (storeParam) params.storeId = storeParam

  try {
    // 基础营业额
    const [totalRes, todayRes, monthRes] = await Promise.all([
      request.get('/statistics/store-turnover', { params }),
      request.get('/statistics/today-turnover', { params }),
      request.get('/statistics/month-turnover', { params })
    ])
    storeTurnover.value = totalRes.data || 0
    todayTurnover.value = todayRes.data || 0
    monthTurnover.value = monthRes.data || 0

    // 图表数据
    const [dayTrendRes, payTypeRes] = await Promise.all([
      request.get('/statistics/daily-turnover-trend', { params }),
      request.get('/statistics/payment-type-statistics', { params })
    ])

    renderDayTrendChart(dayTrendRes.data || [])
    renderPaymentTypeChart(payTypeRes.data || [])

    // 毛利报表
    loading.value = true
    try {
      const gpRes = await request.get('/statistics/gross-profit', { params })
      grossProfitData.value = gpRes.data || []
    } finally {
      loading.value = false
    }

    // 退菜原因统计
    try {
      const crRes = await getCashierRanking(storeParam || null)
        cashierRankData.value = crRes.data || []
      } catch (e) { cashierRankData.value = [] }
      try {
        const rrRes = await getReturnReasons(storeParam || null)
      returnReasonData.value = rrRes.data || []
      totalReturns.value = returnReasonData.value.reduce((s, i) => s + i.count, 0)
      renderReturnReasonsChart()
    } catch (e) { /* ignore */ }

    // 管理员看门店排行
    if (isAdmin.value) {
      const storeTurnoverRes = await request.get('/statistics/store-month-turnover', { params: { month } })
      renderStoreTurnoverChart(storeTurnoverRes.data || [])
    }
  } catch (e) {
    ElMessage.error('数据加载失败')
  }
}

// 日营业额趋势
const renderDayTrendChart = async (data) => {
  const echarts = await getEcharts()
  if (!chartDayTrend) {
    chartDayTrend = echarts.init(document.getElementById('chart-day-trend'))
  }
  chartDayTrend.setOption({
    title: { text: '日营业额趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.day) },
    yAxis: { type: 'value' },
    series: [{
      name: '营业额',
      data: data.map(d => d.amount),
      type: 'line',
      smooth: true,
      itemStyle: { color: '#409EFF' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(64,158,255,0.3)' },
        { offset: 1, color: 'rgba(64,158,255,0)' }
      ]) }
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
  })
}

// 支付方式占比
const renderPaymentTypeChart = async (data) => {
  const echarts = await getEcharts()
  if (!chartPaymentType) {
    chartPaymentType = echarts.init(document.getElementById('chart-payment-type'))
  }
  chartPaymentType.setOption({
    title: { text: '支付方式占比' },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      name: '支付方式',
      type: 'pie',
      radius: ['40%', '70%'],
      data: data.map(d => ({ name: d.name, value: d.amount })),
      color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9C27B0']
    }]
  })
}

// 门店营业额排行（管理员）
const renderStoreTurnoverChart = async (data) => {
  const echarts = await getEcharts()
  if (!chartStoreTurnover) {
    chartStoreTurnover = echarts.init(document.getElementById('chart-store-turnover'))
  }
  chartStoreTurnover.setOption({
    title: { text: '各门店本月营业额排行' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.storeName), axisLabel: { rotate: 45 } },
    yAxis: { type: 'value' },
    series: [{
      data: data.map(d => d.amount),
      type: 'bar',
      itemStyle: { color: '#9C27B0' }
    }],
    grid: { bottom: '15%' }
  })
}

const renderReturnReasonsChart = async () => {
  if (returnReasonData.value.length === 0) return
  const echarts = await getEcharts()
  if (!chartReturnReasons) {
    chartReturnReasons = echarts.init(document.getElementById('chart-return-reasons'))
  }
  chartReturnReasons.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      data: returnReasonData.value.map(d => ({ name: d.reason, value: d.count })),
      label: { formatter: '{b}\n{d}%' },
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  })
}

const onResize = () => {
  chartDayTrend?.resize()
  chartPaymentType?.resize()
  chartStoreTurnover?.resize()
  chartReturnReasons?.resize()
}

const exportReport = () => {
  // 导出毛利报表
  const cols = [
    { label: '菜品', key: 'name' },
    { label: '销量', key: 'quantity' },
    { label: '销售额', key: 'sales' },
    { label: '原料成本', key: 'cost' },
    { label: '毛利', key: 'profit' },
    { label: '毛利率', key: 'margin' },
  ]
  const rows = grossProfitData.value.map(r => ({ ...r, margin: r.margin + '%' }))
  exportExcel(rows, cols, `毛利报表_${selectedMonth.value}`)
}

onMounted(async () => {
  await loadStores()
  await loadAll()

  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

/* 美观统计卡片 */
.stat-card {
  padding: 24px;
  border-radius: 8px;
  color: #fff;
  height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.stat-card.total {
  background: linear-gradient(135deg, #409eff 0%, #2563eb 100%);
}
.stat-card.today {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
}
.stat-card.month {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}
/* 图表卡片容器，继承全局 white-card 规范 */
.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}
.label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}
.value {
  font-size: 26px;
  font-weight: bold;
}
</style>
