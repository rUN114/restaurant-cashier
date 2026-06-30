<template>
  <div class="page-container">
    <el-card class="ai-card">
      <div class="ai-header">
        <div>
          <h2>🤖 AI 明日预制建议</h2>
          <p style="color:#909399;margin:4px 0 0">DeepSeek 分析近30天数据 · 明日{{ dayName }}</p>
        </div>
        <el-button type="primary" size="large" @click="load" :loading="loading">
          {{ data ? '🔄 重新分析' : '🚀 开始分析' }}
        </el-button>
      </div>

      <el-empty v-if="!data && !loading" description="点击按钮，AI 将分析近30天销售数据生成明日预制建议" />

      <div v-if="data" class="ai-result">
        <template v-for="(item, i) in data" :key="i">
          <el-alert v-if="item.source" :title="'📊 数据来源：' + item.source" type="info" :closable="false" style="margin-bottom:16px" />

          <!-- 三大核心指标 -->
          <el-row :gutter="16" style="margin-bottom:20px">
            <el-col :span="8">
              <div class="kpi-box revenue">
                <div class="kpi-icon">💰</div>
                <div class="kpi-label">预计营业额</div>
                <div class="kpi-value">{{ item.forecast || '--' }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="kpi-box dishes">
                <div class="kpi-icon">🍳</div>
                <div class="kpi-label">建议预制</div>
                <div class="kpi-value">{{ item.totalDishes || (item.suggestions||[]).length }} 份</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="kpi-box peak">
                <div class="kpi-icon">🕐</div>
                <div class="kpi-label">预计高峰</div>
                <div class="kpi-value">{{ item.peakHint || '--' }}</div>
              </div>
            </el-col>
          </el-row>

          <!-- 原料预警 -->
          <div v-if="item.alerts && item.alerts.length" class="alerts-box">
            <div class="alerts-title">⚠️ 备货提醒</div>
            <div class="alerts-list">
              <div v-for="(a, j) in item.alerts" :key="j" class="alert-item">
                <span class="alert-icon">{{ j === 0 ? '🔴' : '🟡' }}</span>
                <span>{{ a }}</span>
              </div>
            </div>
          </div>

          <!-- 预制建议表格 -->
          <div v-if="item.suggestions && item.suggestions.length" class="suggestions-box">
            <div class="suggestions-title">🍳 明日建议预制清单</div>
            <el-table :data="item.suggestions" stripe border :row-class-name="suggestRowClass">
              <el-table-column type="index" label="排名" width="60" />
              <el-table-column prop="dish" label="菜品" width="140">
                <template #default="{ row }">{{ row.dish }} {{ row.quantity >= 3 ? '🔥' : '' }}</template>
              </el-table-column>
              <el-table-column label="建议份数" width="140">
                <template #default="{ row }">
                  <span :class="qtyClass(row.quantity)" class="qty-cell">
                    {{ row.quantity }} 份
                  </span>
                  <el-progress :percentage="Math.min(row.quantity * 25, 100)" :color="qtyColor(row.quantity)" :show-text="false" style="width:60px" />
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="分析原因" min-width="180" />
            </el-table>
          </div>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const data = ref(null)

const dayName = computed(() => {
  const days = ['周日','周一','周二','周三','周四','周五','周六']
  const t = new Date()
  t.setDate(t.getDate() + 1)
  return days[t.getDay()]
})

const qtyClass = (q) => q >= 5 ? 'hot' : q >= 3 ? 'warm' : 'cool'
const qtyColor = (q) => q >= 5 ? '#f56c6c' : q >= 3 ? '#e6a23c' : '#67c23a'
const suggestRowClass = ({ row }) => row.quantity >= 5 ? 'suggest-hot' : ''

const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/ai/suggest')
    data.value = res.data || null
  } catch (_) {
    ElMessage.error('AI 分析失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-card { max-width: 960px; margin: 0 auto; }
.ai-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 2px solid #f0f0f0; }
.ai-header h2 { margin: 0; font-size: 24px; }

.kpi-box { text-align: center; padding: 24px 16px; border-radius: 12px; }
.kpi-box.revenue { background: linear-gradient(135deg, #e3f2fd, #bbdefb); }
.kpi-box.dishes { background: linear-gradient(135deg, #fff3e0, #ffe0b2); }
.kpi-box.peak { background: linear-gradient(135deg, #f3e5f5, #e1bee7); }
.kpi-icon { font-size: 28px; }
.kpi-label { font-size: 13px; color: #666; margin: 8px 0 4px; }
.kpi-value { font-size: 28px; font-weight: bold; color: #303133; }

.alerts-box { background: #fff5f5; border-radius: 12px; padding: 20px; margin-bottom: 20px; border-left: 4px solid #f56c6c; }
.alerts-title { font-size: 16px; font-weight: bold; color: #c62828; margin-bottom: 12px; }
.alerts-list { display: flex; flex-direction: column; gap: 6px; }
.alert-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #606266; }
.alert-icon { font-size: 12px; }

.suggestions-box { margin-top: 8px; }
.suggestions-title { font-size: 18px; font-weight: bold; margin-bottom: 12px; }

.qty-cell { font-size: 18px; font-weight: bold; }
.qty-cell.hot { color: #f56c6c; }
.qty-cell.warm { color: #e6a23c; }
.qty-cell.cool { color: #67c23a; }

:deep(.suggest-hot) { background: #fff5f5 !important; }
</style>
