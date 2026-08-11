<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 可视化分析</div>
        <h2 class="font-display">招生趋势分析</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <path d="M8 66 C 40 58, 60 44, 92 42 S 148 26, 212 14" fill="none" stroke="#d9b877" stroke-width="2.6" stroke-linecap="round"/>
          <path d="M8 66 C 40 58, 60 44, 92 42 S 148 26, 212 14 L212 90 L8 90 Z" fill="url(#trendFill)" opacity="0"/>
          <defs>
            <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stop-color="#14a06f" stop-opacity=".35"/>
              <stop offset="1" stop-color="#14a06f" stop-opacity="0"/>
            </linearGradient>
          </defs>
          <g fill="#2fb984"><circle cx="40" cy="58" r="3.4"/><circle cx="92" cy="42" r="3.4"/><circle cx="148" cy="26" r="3.4"/></g>
          <circle cx="212" cy="14" r="4.6" fill="#d9b877"/>
          <circle cx="212" cy="14" r="9" fill="none" stroke="#d9b877" opacity=".4"/>
          <g>
            <rect x="20" y="70" width="9" height="12" rx="2.5" fill="#0e8a5f" opacity=".85"/>
            <rect x="34" y="64" width="9" height="18" rx="2.5" fill="#14a06f" opacity=".85"/>
            <rect x="48" y="58" width="9" height="24" rx="2.5" fill="#2fb984" opacity=".85"/>
          </g>
          <circle cx="70" cy="16" r="2" fill="rgba(230,205,149,.5)"/>
          <circle cx="176" cy="62" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <span class="filter-badge"><el-icon size="17"><TrendCharts /></el-icon></span>
          <span class="filter-title">招生趋势分析</span>
        </div>
        <div class="filter-right">
          <el-select v-model="startYear" placeholder="起始年" clearable style="width:120px;margin-right:8px">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
          <el-select v-model="endYear" placeholder="结束年" clearable style="width:120px;margin-right:12px">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
          <el-select v-model="degreeName" placeholder="培养层次" clearable style="width:130px;margin-right:12px">
            <el-option v-for="d in degrees" :key="d.degreeId" :label="d.degreeName" :value="d.degreeName" />
          </el-select>
          <el-button type="primary" class="btn-gold" :loading="loading" @click="fetchData">刷新</el-button>
        </div>
      </div>
    </el-card>

    <!-- 图表网格 -->
    <div class="chart-grid">
      <el-card shadow="never" class="chart-card">
        <template #header><span>年度招生人数趋势 <span class="scope-note">含硕博</span></span></template>
        <div ref="yearlyChartRef" class="chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header><span>各专业录取人数趋势 <span class="scope-note">含硕博</span></span></template>
        <div ref="majorChartRef" class="chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header><span>各省份录取人数趋势 <span class="scope-note">含硕博</span></span></template>
        <div ref="provinceChartRef" class="chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header><span>录取分数线趋势 <span class="scope-note">仅本科生</span></span></template>
        <div ref="scoreChartRef" class="chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card chart-card-full">
        <template #header><span>性别比例趋势 <span class="scope-note">含硕博</span></span></template>
        <div ref="genderChartRef" class="chart-container"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, onBeforeUnmount, watch } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getChartTheme } from '@/utils/chartTheme'
import * as echarts from 'echarts'
import {
  fetchTrendYearly,
  fetchTrendMajor,
  fetchTrendProvince,
  fetchTrendScore,
  fetchTrendGender,
  fetchDegrees,
} from '@/api/modules/admission'

const startYear = ref(null)
const endYear = ref(null)
const degreeName = ref(null)
const degrees = ref([])
const loading = ref(false)

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 20 }, (_, i) => currentYear - i)

const yearlyChartRef = ref(null)
const majorChartRef = ref(null)
const provinceChartRef = ref(null)
const scoreChartRef = ref(null)
const genderChartRef = ref(null)

const chartInstances = {}

function getOrCreateChart(key, dom) {
  if (!dom) return null
  if (!chartInstances[key]) {
    chartInstances[key] = echarts.init(dom)
  }
  return chartInstances[key]
}

function getEmptyOption(msg) {
  const t = getChartTheme()
  return {
    xAxis: { show: false },
    yAxis: { show: false },
    graphic: {
      type: 'text',
      left: 'center',
      top: 'center',
      style: { text: msg, fill: t.emptyText, fontSize: 14 },
    },
  }
}

function updateYearlyChart(data) {
  const t = getChartTheme()
  const chart = getOrCreateChart('yearly', yearlyChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无年度招生数据'))
    return
  }
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'var(--bg-elevated)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
    },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: data.map(d => String(d.year)), axisLabel: { fontSize: 12, color: t.textTertiary }, axisLine: { lineStyle: { color: t.axisLine } } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: t.borderColor, type: 'dashed' } }, axisLabel: { color: t.textTertiary } },
    series: [{
      type: 'bar',
      data: data.map(d => d.count),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: t.primary },
          { offset: 1, color: 'rgba(14,138,95,0.35)' },
        ]),
        borderRadius: [6, 6, 0, 0],
      },
      label: { show: true, position: 'top', fontSize: 11, color: t.textSecondary },
      animationDuration: 1100,
      animationEasing: 'cubicOut',
    }],
  })
}

function updateMajorChart(data) {
  const t = getChartTheme()
  const chart = getOrCreateChart('major', majorChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无专业数据'))
    return
  }

  const years = [...new Set(data.map(d => d.year))].sort()
  const majorTotals = {}
  data.forEach(d => { majorTotals[d.majorname || '未知'] = (majorTotals[d.majorname || '未知'] || 0) + d.count })
  const topMajors = Object.entries(majorTotals)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
    .map(([name]) => name)

  const colors = getChartTheme().palette

  const series = topMajors.map((name, i) => ({
    name,
    type: 'bar',
    stack: 'total',
    itemStyle: { color: colors[i % colors.length], borderRadius: i === topMajors.length - 1 ? [3, 3, 0, 0] : 0 },
    data: years.map(year => {
      const item = data.find(d => d.year === year && (d.majorname || '未知') === name)
      return item ? item.count : 0
    }),
  }))

  const otherData = years.map(year => {
    const yearTotal = data.filter(d => d.year === year).reduce((s, d) => s + d.count, 0)
    const topTotal = topMajors.reduce((s, name) => {
      const item = data.find(d => d.year === year && (d.majorname || '未知') === name)
      return s + (item ? item.count : 0)
    }, 0)
    return Math.max(0, yearTotal - topTotal)
  })
  if (otherData.some(v => v > 0)) {
    series.push({ name: '其他', type: 'bar', stack: 'total', itemStyle: { color: t.borderColor }, data: otherData })
  }

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'var(--bg-elevated)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
    },
    legend: { type: 'scroll', bottom: 0, fontSize: 11, textStyle: { color: t.textSecondary } },
    grid: { left: 60, right: 20, bottom: 50, top: 20 },
    xAxis: { type: 'category', data: years.map(String), axisLabel: { fontSize: 12, color: t.textTertiary }, axisLine: { lineStyle: { color: t.axisLine } } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: t.borderColor, type: 'dashed' } }, axisLabel: { color: t.textTertiary } },
    series,
  })
}

function updateProvinceChart(data) {
  const t = getChartTheme()
  const chart = getOrCreateChart('province', provinceChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无省份数据'))
    return
  }

  const years = [...new Set(data.map(d => d.year))].sort()
  const provinces = [...new Set(data.map(d => d.provincename || '未知'))]
  const heatmapData = data.map(d => [
    years.indexOf(d.year),
    provinces.indexOf(d.provincename || '未知'),
    d.count,
  ])
  const maxVal = Math.max(...data.map(d => d.count), 1)

  chart.setOption({
    tooltip: {
      position: 'top',
      backgroundColor: 'var(--bg-elevated)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
      formatter: p => `年份: ${years[p.data[0]]}<br/>省份: ${provinces[p.data[1]]}<br/>人数: ${p.data[2]}`,
    },
    grid: { left: 80, right: 60, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: years.map(String), splitArea: { show: true }, axisLabel: { color: t.textTertiary } },
    yAxis: { type: 'category', data: provinces, splitArea: { show: true }, axisLabel: { color: t.textTertiary } },
    visualMap: {
      min: 0,
      max: maxVal,
      calculable: true,
      orient: 'vertical',
      right: 0,
      top: 'center',
      inRange: { color: ['#e6f3ec', '#8fd3b0', '#2fb984', '#0b5c40'] },
    },
    series: [{
      type: 'heatmap',
      data: heatmapData,
      label: { show: heatmapData.length < 50, fontSize: 10 },
      emphasis: { itemStyle: { shadowBlur: 10 } },
    }],
  })
}

function updateScoreChart(data) {
  const t = getChartTheme()
  const chart = getOrCreateChart('score', scoreChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无分数线数据'))
    return
  }

  const years = data.map(d => String(d.year))
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'var(--bg-elevated)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
    },
    legend: { bottom: 0, fontSize: 11, textStyle: { color: t.textSecondary } },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: years, axisLabel: { fontSize: 12, color: t.textTertiary }, axisLine: { lineStyle: { color: t.axisLine } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: t.borderColor, type: 'dashed' } }, axisLabel: { color: t.textTertiary } },
    series: [
      { name: '最高分', type: 'line', data: data.map(d => d.maxscore), smooth: true, symbol: 'circle', lineStyle: { width: 2.5, color: t.gold }, itemStyle: { color: t.gold } },
      { name: '平均分', type: 'line', data: data.map(d => d.avgscore), smooth: true, symbol: 'diamond', lineStyle: { width: 2.5, color: t.primary }, itemStyle: { color: t.primary } },
      { name: '最低分', type: 'line', data: data.map(d => d.minscore), smooth: true, symbol: 'triangle', lineStyle: { width: 2, color: '#57c493' }, itemStyle: { color: '#57c493' } },
    ],
  })
}

function updateGenderChart(data) {
  const t = getChartTheme()
  const chart = getOrCreateChart('gender', genderChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无性别数据'))
    return
  }

  const years = [...new Set(data.map(d => d.year))].sort()
  const maleData = years.map(y => { const d = data.find(v => v.year === y && v.gender === '男'); return d ? d.count : 0 })
  const femaleData = years.map(y => { const d = data.find(v => v.year === y && v.gender === '女'); return d ? d.count : 0 })

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'var(--bg-elevated)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
      formatter: p => {
        const yr = p[0].axisValue
        const male = p[0].value
        const female = p[1].value
        const total = male + female
        return `${yr}年<br/>男: ${male} (${total ? ((male/total)*100).toFixed(1) : 0}%)<br/>女: ${female} (${total ? ((female/total)*100).toFixed(1) : 0}%)`
      }
    },
    legend: { bottom: 0, fontSize: 11, textStyle: { color: t.textSecondary } },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: years.map(String), axisLabel: { fontSize: 12, color: t.textTertiary }, axisLine: { lineStyle: { color: t.axisLine } } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: t.borderColor, type: 'dashed' } }, axisLabel: { color: t.textTertiary } },
    series: [
      { name: '男', type: 'bar', stack: 'gender', data: maleData, itemStyle: { color: t.maleColor, borderRadius: [0, 0, 0, 0] } },
      { name: '女', type: 'bar', stack: 'gender', data: femaleData, itemStyle: { color: t.femaleColor, borderRadius: [5, 5, 0, 0] } },
    ],
  })
}

async function fetchDegreesList() {
  try {
    const res = await fetchDegrees()
    degrees.value = res.data.data || []
  } catch { degrees.value = [] }
}

async function fetchData() {
  loading.value = true
  const params = {}
  if (startYear.value) params.startYear = startYear.value
  if (endYear.value) params.endYear = endYear.value
  if (degreeName.value) params.degreeName = degreeName.value

  try {
    const [yearly, major, province, score, gender] = await Promise.all([
      fetchTrendYearly(params),
      fetchTrendMajor(params),
      fetchTrendProvince(params),
      fetchTrendScore(params),
      fetchTrendGender(params),
    ])
    updateYearlyChart(yearly.data?.data || [])
    updateMajorChart(major.data?.data || [])
    updateProvinceChart(province.data?.data || [])
    updateScoreChart(score.data?.data || [])
    updateGenderChart(gender.data?.data || [])
  } catch (e) {
    console.error('获取趋势数据失败:', e)
    ElMessage.error('获取趋势数据失败，请检查后端服务是否正常')
  } finally {
    loading.value = false
  }
}

function handleResize() {
  Object.values(chartInstances).forEach(chart => chart?.resize())
}

watch([startYear, endYear, degreeName], fetchData)

onMounted(() => {
  window.addEventListener('resize', handleResize)
  fetchDegreesList()
  fetchData()
})

onActivated(() => {
  // keep-alive 从缓存恢复时：刷新数据 + 重设图表尺寸（否则切回页面数据是旧的、图表可能错位）
  handleResize()
  fetchData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(chartInstances).forEach(chart => chart?.dispose())
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
  padding: 18px 28px;
  min-height: 84px;
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
  margin: 8px 0;
  border-radius: 2px;
  background: linear-gradient(90deg, #d9b877, rgba(201, 164, 92, 0));
}
.ph-right { position: relative; z-index: 2; }
.ph-art { width: 220px; height: 84px; flex-shrink: 0; }
@media (max-width: 900px) {
  .ph-art { display: none; }
  .page-hero { padding: 14px 20px; }
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
  gap: 12px;
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
  flex-wrap: wrap;
  gap: 4px;
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

/* ===== 图表网格 ===== */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.chart-grid .chart-card {
  animation: rise-up 0.7s 0.16s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.chart-grid .chart-card:nth-child(2) { animation-delay: 0.22s; }
.chart-grid .chart-card:nth-child(3) { animation-delay: 0.28s; }
.chart-grid .chart-card:nth-child(4) { animation-delay: 0.34s; }
.chart-grid .chart-card:nth-child(5) { animation-delay: 0.4s; }

.chart-card-full {
  grid-column: 1 / -1;
}

.chart-container {
  width: 100%;
  height: 360px;
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

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .filter-card, .chart-grid .chart-card { animation: none !important; }
}

@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .chart-container {
    height: 280px;
  }
}
</style>
