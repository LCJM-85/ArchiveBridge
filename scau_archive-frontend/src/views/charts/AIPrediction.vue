<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · AI 预测引擎</div>
        <h2 class="font-display">智能预测</h2>
        <div class="ph-rule"></div>
        <p class="ph-sub">ARIMA + XGBoost 集成模型 · 未来三年招生规模预测与置信区间</p>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 历史实线 -->
          <path d="M8 62 C 34 56, 52 44, 78 42 S 118 30, 142 24" fill="none" stroke="#2fb984" stroke-width="2.6" stroke-linecap="round"/>
          <!-- 未来虚线 -->
          <path d="M142 24 C 162 18, 180 12, 212 6" fill="none" stroke="#d9b877" stroke-width="2.6" stroke-dasharray="7 6" stroke-linecap="round"/>
          <!-- 置信区间 -->
          <path d="M142 24 C 162 30, 180 28, 212 22" fill="none" stroke="rgba(47,185,132,.4)" stroke-width="1.6" stroke-dasharray="3 5"/>
          <path d="M142 24 C 162 8, 180 2, 212 -2" fill="none" stroke="rgba(47,185,132,.4)" stroke-width="1.6" stroke-dasharray="3 5"/>
          <!-- 数据点 -->
          <g fill="#2fb984"><circle cx="34" cy="56" r="3.2"/><circle cx="78" cy="42" r="3.2"/><circle cx="118" cy="30" r="3.2"/></g>
          <circle cx="142" cy="24" r="4" fill="#d9b877"/>
          <!-- 未来之星 -->
          <g transform="translate(196 8)">
            <path d="M0 -5 L1.4 -1.4 L5 0 L1.4 1.4 L0 5 L-1.4 1.4 L-5 0 L-1.4 -1.4 Z" fill="#e6cd95"/>
          </g>
          <!-- 年份标签 -->
          <text x="138" y="52" font-size="8" fill="rgba(230,205,149,.6)" font-family="inherit">现在</text>
          <text x="196" y="52" font-size="8" fill="rgba(230,205,149,.6)" font-family="inherit">未来</text>
          <circle cx="44" cy="70" r="2" fill="rgba(230,205,149,.5)"/>
        </svg>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <span class="filter-badge"><el-icon size="17"><DataAnalysis /></el-icon></span>
          <span class="filter-title">智能预测分析</span>
        </div>
        <div class="filter-right">
          <el-select v-model="degreeName" placeholder="培养层次" clearable style="width:130px;margin-right:8px">
            <el-option v-for="d in degrees" :key="d.degreeId" :label="d.degreeName" :value="d.degreeName" />
          </el-select>
          <el-button type="primary" class="btn-gold" :loading="loading" @click="fetchData">重新预测</el-button>
        </div>
      </div>
    </el-card>

    <!-- 预测概览卡片 -->
    <div v-if="predictions.length" class="overview-grid">
      <el-card v-for="p in predictions" :key="p.year" shadow="never" class="overview-card">
        <div class="overview-top">
          <span class="overview-year">{{ p.year }} 年</span>
          <span class="overview-badge">预测</span>
        </div>
        <div class="overview-value">{{ p.predicted }}</div>
        <div class="overview-range">
          置信区间: {{ p.lowerBound }} ~ {{ p.upperBound }}
        </div>
      </el-card>
    </div>

    <!-- 图表 -->
    <el-card shadow="never" class="chart-card">
      <template #header><span>招生规模趋势预测 <span class="scope-note">未来 3 年</span></span></template>
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
import { ElMessage } from 'element-plus'
import { getChartTheme } from '@/utils/chartTheme'
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
  const t = getChartTheme()
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
      backgroundColor: 'var(--bg-elevated)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
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
    legend: { bottom: 0, fontSize: 12, textStyle: { color: t.textSecondary } },
    grid: { left: 60, right: 30, bottom: 50, top: 20 },
    xAxis: {
      type: 'category',
      data: allYears,
      axisLine: { lineStyle: { color: t.axisLine } },
      axisLabel: { color: t.textTertiary },
    },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: t.borderColor, type: 'dashed' } }, axisLabel: { color: t.textTertiary } },
    series: [
      {
        name: '历史数据',
        type: 'line',
        data: actualData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: t.primary, width: 2.5 },
        itemStyle: { color: t.primary },
        connectNulls: false,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14,138,95,0.18)' },
            { offset: 1, color: 'rgba(14,138,95,0.01)' },
          ]),
        },
      },
      {
        name: '预测趋势',
        type: 'line',
        data: predData,
        smooth: true,
        symbol: 'diamond',
        symbolSize: 9,
        lineStyle: { color: t.gold, width: 2.5, type: 'dashed' },
        itemStyle: { color: t.gold },
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
        areaStyle: { color: 'rgba(201, 164, 92, 0.16)' },
      },
    ],
  })
}

async function fetchData() {
  loading.value = true
  const t = getChartTheme()
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
          graphic: { type: 'text', left: 'center', top: 'center', style: { text: '暂无数据，请先导入招生数据', fill: t.emptyText, fontSize: 14 } },
        })
      }
    }
  } catch (e) {
    console.error('获取预测数据失败:', e)
    ElMessage.error('获取预测数据失败，请稍后重试')
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

/* ===== 页面头部条 ===== */
.page-hero {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(520px 240px at 88% -30px, rgba(47, 185, 132, 0.32), transparent 62%),
    radial-gradient(420px 200px at 100% 130%, rgba(201, 164, 92, 0.2), transparent 60%),
    linear-gradient(120deg, #07271c 0%, #0b5c40 55%, #0e8a5f 100%);
  box-shadow: 0 20px 48px rgba(7, 39, 28, 0.24);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26px 34px;
  min-height: 116px;
  animation: rise-up 0.7s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.page-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.05;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2'/%3E%3C/filter%3E%3Crect width='160' height='160' filter='url(%23n)' opacity='.6'/%3E%3C/svg%3E");
}
.ph-left { position: relative; z-index: 2; }
.ph-kicker {
  font-size: 11px;
  letter-spacing: 3px;
  color: var(--color-gold-light);
  margin-bottom: 8px;
}
.ph-left h2 {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0;
}
.ph-rule {
  width: 48px;
  height: 2px;
  margin: 12px 0;
  border-radius: 2px;
  background: linear-gradient(90deg, #d9b877, rgba(201, 164, 92, 0));
}
.ph-sub { font-size: 12.5px; color: rgba(255, 255, 255, 0.6); margin: 0; }
.ph-right { position: relative; z-index: 2; }
.ph-art { width: 220px; height: 84px; flex-shrink: 0; }
@media (max-width: 900px) {
  .ph-art { display: none; }
  .page-hero { padding: 22px 24px; }
}

/* ===== 筛选栏 ===== */
.filter-card {
  flex-shrink: 0;
  animation: rise-up 0.7s 0.08s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-badge {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(140deg, #14a06f, #0b5c40);
  border: 1px solid rgba(201, 164, 92, 0.4);
  color: #e6cd95;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(6, 40, 28, 0.25);
}

.filter-title {
  font-family: var(--font-display);
  font-size: 15.5px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--text-primary);
}

.filter-right {
  display: flex;
  align-items: center;
}

.btn-gold {
  background: linear-gradient(135deg, var(--color-gold), var(--color-gold-dark)) !important;
  border: none !important;
  color: #1d1608 !important;
  font-weight: 600;
}
.btn-gold:hover {
  filter: brightness(1.08);
  box-shadow: 0 6px 16px rgba(201, 164, 92, 0.4);
}

/* ===== 预测概览 ===== */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.overview-card {
  text-align: center;
  padding: 18px 0 16px !important;
  position: relative;
  overflow: hidden;
  animation: rise-up 0.7s 0.14s cubic-bezier(0.2, 0.75, 0.3, 1) both;
  transition: transform 0.25s ease, box-shadow 0.25s ease !important;
}
.overview-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 34px rgba(7, 39, 28, 0.12) !important;
}
.overview-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 24px;
  right: 24px;
  height: 3px;
  border-radius: 0 0 3px 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light));
}

.overview-top {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 6px;
}

.overview-year {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
}

.overview-badge {
  font-size: 10px;
  letter-spacing: 1px;
  color: var(--color-gold-dark);
  background: rgba(201, 164, 92, 0.14);
  border: 1px solid rgba(201, 164, 92, 0.35);
  padding: 1px 8px;
  border-radius: 999px;
}

.overview-value {
  font-size: 38px;
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
  line-height: 1.25;
  text-shadow: 0 4px 18px rgba(14, 138, 95, 0.15);
}

.overview-range {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* ===== 图表 ===== */
.chart-card {
  animation: rise-up 0.7s 0.2s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}

.chart-container {
  width: 100%;
  height: 420px;
}

.scope-note {
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 400;
  margin-left: 6px;
  background: var(--bg-tertiary);
  padding: 1px 8px;
  border-radius: 999px;
  vertical-align: 2px;
}

/* ===== 精度指标 ===== */
.metrics-card {
  animation: rise-up 0.7s 0.26s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  text-align: center;
}

.metric-item {
  padding: 18px 0 14px;
  border-radius: 12px;
  border: 1px solid var(--border-light);
  background: var(--bg-tertiary);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.metric-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 26px rgba(7, 39, 28, 0.1);
}

.metric-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
  letter-spacing: 1px;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.metric-value.good {
  color: var(--color-primary);
}

.metric-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .filter-card, .overview-card, .chart-card, .metrics-card { animation: none !important; }
  .overview-card, .metric-item { transition: none !important; }
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
