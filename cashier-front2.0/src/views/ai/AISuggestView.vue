<template>
  <div class="page-container">
    <el-card class="ai-card">
      <div class="ai-header">
        <h2>🤖 AI 明日预制建议</h2>
        <el-button type="primary" size="large" @click="load" :loading="loading">
          {{ data ? '重新分析' : '开始分析' }}
        </el-button>
      </div>

      <el-empty v-if="!data && !loading" description="点击上方按钮，AI 将分析近30天销售数据生成明日预制建议" />

      <div v-if="data" class="ai-result">
        <template v-for="(item, i) in data" :key="i">
          <!-- 来源标签 -->
          <el-alert v-if="item.source" :title="'数据来源：' + item.source" type="info" :closable="false" style="margin-bottom:16px" />

          <!-- 预计营业额 -->
          <div v-if="item.forecast" class="forecast-box">
            <span class="forecast-label">📊 预计明日营业额</span>
            <span class="forecast-value">{{ item.forecast }}</span>
          </div>

          <!-- 原料预警 -->
          <div v-if="item.alerts && item.alerts.length" class="alerts-box">
            <h4>⚠️ 备货提醒</h4>
            <el-tag v-for="(a, j) in item.alerts" :key="j" type="danger" size="default" style="margin:4px">{{ a }}</el-tag>
          </div>

          <!-- 预制建议表格 -->
          <div v-if="item.suggestions && item.suggestions.length" class="suggestions-box">
            <h4>🍳 明日建议预制</h4>
            <el-table :data="item.suggestions" stripe border>
              <el-table-column type="index" label="#" width="50" />
              <el-table-column prop="dish" label="菜品" width="160" />
              <el-table-column label="建议份数" width="160">
                <template #default="{ row }">
                  <span :class="row.quantity >= 10 ? 'high-qty' : ''" style="font-size:20px;font-weight:bold">
                    {{ row.quantity }} 份
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="分析原因" min-width="200" />
            </el-table>
          </div>

          <!-- 原始输出（调试用） -->
          <el-collapse v-if="item.raw && !item.suggestions" style="margin-top:16px">
            <el-collapse-item title="查看 AI 原始输出">
              <pre style="white-space:pre-wrap;font-size:13px;color:#909399">{{ item.raw }}</pre>
            </el-collapse-item>
          </el-collapse>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const data = ref(null)

const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/ai/suggest')
    data.value = res.data || null
    if (!data.value || data.value.length === 0) {
      ElMessage.warning('暂无足够数据生成建议')
    }
  } catch (_) {
    ElMessage.error('AI 分析失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-card { max-width: 900px; margin: 0 auto; }
.ai-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.ai-header h2 { margin: 0; }
.forecast-box { background: linear-gradient(135deg, #1a1a2e, #16213e); border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 20px; }
.forecast-label { display: block; color: #909399; font-size: 14px; margin-bottom: 8px; }
.forecast-value { font-size: 36px; font-weight: bold; color: #00e676; }
.alerts-box { margin-bottom: 20px; }
.alerts-box h4, .suggestions-box h4 { margin: 0 0 12px; font-size: 16px; }
.high-qty { color: #f56c6c; }
</style>
