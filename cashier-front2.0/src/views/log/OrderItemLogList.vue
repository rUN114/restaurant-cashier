<template>
  <div class="page-container">
    <el-card>
      <div class="header">
        <h3>菜品销售统计</h3>
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

      <!-- 图表区域 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :xs="24" :sm="12">
          <div id="chart-dish-count" style="width: 100%; height: 350px;"></div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <div id="chart-dish-amount" style="width: 100%; height: 350px;"></div>
        </el-col>
      </el-row>
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <div id="chart-category" style="width: 100%; height: 350px;"></div>
        </el-col>
      </el-row>
      <el-row v-if="isAdmin" :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <div id="chart-store" style="width: 100%; height: 350px;"></div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { ref, onMounted, nextTick, computed } from 'vue'
// ECharts 动态导入（仅在用户访问时加载 ~1MB）
let echartsModule = null
const getEcharts = async () => {
  if (!echartsModule) echartsModule = await import('echarts')
  return echartsModule
}
import { useUserStore } from '@/stores/user'
const { t } = useI18n()
import { getAllStores } from '@/api'

import request from '@/utils/request'

const user = useUserStore()
const isAdmin = computed(() => user.roleCode === 'ADMIN')
const storeId = computed(() => user.storeId)

const selectedMonth = ref('2026-05')  // 默认当月
const selectedStoreId = ref(null)     // 管理员默认全部门店
const storeList = ref([])

let chartDishCount = null
let chartDishAmount = null
let chartCategory = null
let chartStore = null

// 加载门店列表
const loadStores = async () => {
  if (!isAdmin.value) return
  const res = await getAllStores()
  storeList.value = res.data || []
}

// 加载所有图表数据
const loadAll = async () => {
  const month = selectedMonth.value
  if (!month) return

  // 构建参数
  const storeParam = isAdmin.value ? selectedStoreId.value : storeId.value
  const params = { month }
  if (storeParam) params.storeId = storeParam

  // 获取数据
  const [countRes, amountRes, categoryRes] = await Promise.all([
    request.get('/statistics/dish-sales-count', { params }),
    request.get('/statistics/dish-sales-amount', { params }),
    request.get('/statistics/category-sales', { params })
  ])

  // 更新图表
  renderDishCountChart(countRes.data)
  renderDishAmountChart(amountRes.data)
  renderCategoryChart(categoryRes.data)

  // 管理员门店销售
  if (isAdmin.value) {
    const storeRes = await request.get('/statistics/store-sales', { params: { month } })
    renderStoreChart(storeRes.data)
  }
}

// 菜品销量
const renderDishCountChart = async (data) => {
  const echarts = await getEcharts()
  if (!chartDishCount) {
    chartDishCount = echarts.init(document.getElementById('chart-dish-count'))
  }
  chartDishCount.setOption({
    title: { text: '菜品销量 Top 20' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.name), axisLabel: { rotate: 45 } },
    yAxis: { type: 'value' },
    series: [{ data: data.map(d => d.count), type: 'bar', color: '#67C23A' }],
    grid: { bottom: '15%' }
  })
}

// 菜品销售额
const renderDishAmountChart = async (data) => {
  const echarts = await getEcharts()
  if (!chartDishAmount) {
    chartDishAmount = echarts.init(document.getElementById('chart-dish-amount'))
  }
  chartDishAmount.setOption({
    title: { text: '菜品销售额 Top 20' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.name), axisLabel: { rotate: 45 } },
    yAxis: { type: 'value' },
    series: [{ data: data.map(d => d.amount), type: 'bar', color: '#E6A23C' }],
    grid: { bottom: '15%' }
  })
}

// 分类销售（数量+金额）
const renderCategoryChart = async (data) => {
  const echarts = await getEcharts()
  if (!chartCategory) {
    chartCategory = echarts.init(document.getElementById('chart-category'))
  }
  chartCategory.setOption({
    title: { text: '分类销售统计' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售数量', '销售金额'] },
    xAxis: { type: 'category', data: data.map(d => d.name) },
    yAxis: { type: 'value' },
    series: [
      { name: '销售数量', data: data.map(d => d.count), type: 'bar', color: '#409EFF' },
      { name: '销售金额', data: data.map(d => d.amount), type: 'bar', color: '#F56C6C' }
    ]
  })
}

// 门店业绩
const renderStoreChart = async (data) => {
  const echarts = await getEcharts()
  if (!chartStore) {
    chartStore = echarts.init(document.getElementById('chart-store'))
  }
  chartStore.setOption({
    title: { text: '各门店销售额' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.storeName) },
    yAxis: { type: 'value' },
    series: [{ data: data.map(d => d.amount), type: 'bar', color: '#9C27B0' }]
  })
}

onMounted(async () => {
  await loadStores()
  await loadAll()

  // 监听窗口大小变化，图表自适应
  window.addEventListener('resize', () => {
    chartDishCount?.resize()
    chartDishAmount?.resize()
    chartCategory?.resize()
    chartStore?.resize()
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
</style>
