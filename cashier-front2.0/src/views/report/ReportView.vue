<template>
  <div class="page-container">
    <el-card>
      <div class="header">
        <h3>{{ $t('pages.reportTitle') }}</h3>
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
            v-if="isAdmin"
            v-model="selectedStoreId"
            :placeholder="$t('common.pleaseSelect')"
            clearable
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
          <div id="chart-day-trend" style="width: 100%; height: 350px;"></div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div id="chart-payment-type" style="width: 100%; height: 350px;"></div>
        </el-col>
      </el-row>

      <el-row v-if="isAdmin" :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <div id="chart-store-turnover" style="width: 100%; height: 350px;"></div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { ref, onMounted, nextTick, computed } from 'vue'
// ECharts 动态导入（仅在用户访问报表页时加载 ~1MB）
let echartsModule = null
const getEcharts = async () => {
  if (!echartsModule) echartsModule = await import('echarts')
  return echartsModule
}
import { useUserStore } from '@/stores/user'
const { t } = useI18n()
import { getAllStores } from '@/api'

import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const user = useUserStore()
const isAdmin = computed(() => user.roleCode === 'ADMIN')
const storeId = computed(() => user.storeId)

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

// 图表实例
let chartDayTrend = null
let chartPaymentType = null
let chartStoreTurnover = null

// 加载门店
const loadStores = async () => {
  if (!isAdmin.value) return
  try {
    const res = await getAllStores()
    storeList.value = res.data || []
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

onMounted(async () => {
  await loadStores()
  await loadAll()

  window.addEventListener('resize', () => {
    chartDayTrend?.resize()
    chartPaymentType?.resize()
    chartStoreTurnover?.resize()
  })
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.stat-card.today {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  color: #333;
}
.stat-card.month {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #333;
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
