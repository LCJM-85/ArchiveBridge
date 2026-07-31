<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-icon size="18" color="var(--color-primary)"><DataAnalysis /></el-icon>
          <span class="filter-title">智能预测分析</span>
        </div>
        <div class="filter-right">
          <el-select v-model="degreeName" placeholder="培养层次" clearable style="width:130px;margin-right:8px">
            <el-option v-for="d in degrees" :key="d.degreeId" :label="d.degreeName" :value="d.degreeName" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="fetchData">重新预测</el-button>
        </div>
      </div>
    </el-card>

    <!-- 预测概览卡片 -->
    <div v-if="predictions.length" class="overview-grid">
      <el-card v-for="p in predictions" :key="p.year" shadow="never" class="overview-card">
        <div class="overview-year">{{ p.year }} 年</div>
        <div class="overview-value">{{ p.predicted }}</div>
        <div class="overview-range">
          置信区间: {{ p.lowerBound }} ~ {{ p.upperBound }}
        </div>
      </el-card>
    </div>

    <!-- 图表 -->
    <el-card shadow="never" class="chart-card">
      <template #header><span>招生规模趋势预测</span></template>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>

    <!-- 精度指标 -->
    <el-card v-if="metrics.mape !== undefined" shadow="never" class="metrics-card">
      <template #header><span>模型精度</span></template>
      <div class="metrics-grid">
        <div class="metric-item">
          <div class="metric-label">MAPE</div>
          <div class="metric-value" :class="{ good: metrics.mape <= 5 }">{{ metrics.mape }}%</div>
          <div class="metric-desc">{{ metrics.mape <= 5 ? '优' : '良' }}</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">MAE</div>
          <div class="metric-value">{{ metrics.mae }}</div>
          <div class="metric-desc">平均绝对误差</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">RMSE</div>
          <div class="metric-value">{{ metrics.rmse }}</div>
          <div class="metric-desc">均方根误差</div>
        </div>
        <div class="metric-item" v-if="metrics.arima_order">
          <div class="metric-label">ARIMA</div>
          <div class="metric-value" style="font-size:14px">{{ metrics.arima_order }}</div>
          <div class="metric-desc">最优参数 (p,d,q)</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, onBeforeUnmount, watch } from 'vue'
import { DataAnalysis } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { fetchPrediction, fetchDegrees } from '@/api/modules/admission'

const chartRef = ref(null)
let chart = null
const loading = ref(false)
const degreeName = ref(null)
const degrees = ref([])
const historical = ref([])
const predictions = ref([])
const metrics = ref({})

function renderChart(hist, preds) {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  const allYears = [
    ...hist.map(h => String(h.year)),
    ...preds.map(p => String(p.year)),
  ]

  const actualData = allYears.map(y => {
    const h = hist.find(h => String(h.year) === y)
    return h ? h.actual : null
  })

  const predData = allYears.map(y => {
    const p = preds.find(p => String(p.year) === y)
    return p ? p.predicted : null
  })

  const upperData = allYears.map(y => {
    const p = preds.find(p => String(p.year) === y)
    return p ? p.upperBound : null
  })

  const lowerData = allYears.map(y => {
    const p = preds.find(p => String(p.year) === y)
    return p ? p.lowerBound : null
  })

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: p => {
        const year = p[0].axisValue
        let html = `<b>${year}年</b><br/>`
        p.forEach(item => {
          if (item.value !== null && item.value !== undefined) {
            html += `${item.marker} ${item.seriesName}: ${item.value} 人<br/>`
          }
        })
        return html
      },
    },
    legend: { bottom: 0, fontSize: 12 },
    grid: { left: 60, right: 30, bottom: 50, top: 20 },
    xAxis: {
      type: 'category',
      data: allYears,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '历史数据',
        type: 'line',
        data: actualData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' },
        connectNulls: false,
      },
      {
        name: '预测趋势',
        type: 'line',
        data: predData,
        smooth: true,
        symbol: 'diamond',
        symbolSize: 8,
        lineStyle: { color: '#f56c6c', width: 2, type: 'dashed' },
        itemStyle: { color: '#f56c6c' },
      },
      {
        name: '置信区间',
        type: 'line',
        data: upperData,
        symbol: 'none',
        lineStyle: { opacity: 0 },
        stack: 'confidence',
        showSymbol: false,
        silent: true,
      },
      {
        name: '置信区间',
        type: 'line',
        data: lowerData,
        symbol: 'none',
        lineStyle: { opacity: 0 },
        stack: 'confidence',
        showSymbol: false,
        silent: true,
        areaStyle: { color: 'rgba(245, 108, 108, 0.15)' },
      },
    ],
  })
}

async function fetchData() {
  loading.value = true
  try {
    const res = await fetchPrediction(3, degreeName.value)
    const data = res.data?.data || {}
    historical.value = data.historical || []
    predictions.value = data.predictions || []
    metrics.value = data.metrics || {}

    if (predictions.value.length || historical.value.length) {
      renderChart(historical.value, predictions.value)
    } else {
      if (chart) {
        chart.clear()
        chart.setOption({
          graphic: { type: 'text', left: 'center', top: 'center', style: { text: '暂无数据，请先导入招生数据', fill: '#bbb', fontSize: 14 } },
        })
      }
    }
  } catch (e) {
    console.error('获取预测数据失败:', e)
  } finally {
    loading.value = false
  }
}

function handleResize() {
  chart?.resize()
}

async function fetchDegreesList() {
  try {
    const res = await fetchDegrees()
    degrees.value = res.data.data || []
  } catch { degrees.value = [] }
}

watch(degreeName, fetchData)

onMounted(() => {
  window.addEventListener('resize', handleResize)
  fetchDegreesList()
  fetchData()
})

onActivated(() => {
  fetchData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  flex-shrink: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.overview-card {
  text-align: center;
  padding: 12px 0;
}

.overview-year {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.overview-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-primary);
}

.overview-range {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.chart-container {
  width: 100%;
  height: 420px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  text-align: center;
}

.metric-item {
  padding: 16px 0;
}

.metric-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-value.good {
  color: #67c23a;
}

.metric-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
