<template>
  <div class="page-wrapper">
    <!-- 年份筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-icon size="18" color="var(--color-primary)"><TrendCharts /></el-icon>
          <span class="filter-title">招生趋势分析</span>
        </div>
        <div class="filter-right">
          <el-select v-model="startYear" placeholder="起始年" clearable style="width:120px;margin-right:8px">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
          <el-select v-model="endYear" placeholder="结束年" clearable style="width:120px;margin-right:12px">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="fetchData">刷新</el-button>
        </div>
      </div>
    </el-card>

    <!-- 图表网格 -->
    <div class="chart-grid">
      <el-card shadow="never" class="chart-card">
        <template #header><span>年度招生人数趋势</span></template>
        <div ref="yearlyChartRef" class="chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header><span>各专业录取人数趋势</span></template>
        <div ref="majorChartRef" class="chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header><span>各省份录取人数趋势</span></template>
        <div ref="provinceChartRef" class="chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header><span>录取分数线趋势</span></template>
        <div ref="scoreChartRef" class="chart-container"></div>
      </el-card>

      <el-card shadow="never" class="chart-card chart-card-full">
        <template #header><span>性别比例趋势</span></template>
        <div ref="genderChartRef" class="chart-container"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  fetchTrendYearly,
  fetchTrendMajor,
  fetchTrendProvince,
  fetchTrendScore,
  fetchTrendGender,
} from '@/api/modules/admission'

const startYear = ref(null)
const endYear = ref(null)
const loading = ref(false)

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 20 }, (_, i) => currentYear - i)

const yearlyChartRef = ref(null)
const majorChartRef = ref(null)
const provinceChartRef = ref(null)
const scoreChartRef = ref(null)
const genderChartRef = ref(null)

const chartInstances = reactive({})

function getOrCreateChart(key, dom) {
  if (!dom) return null
  if (!chartInstances[key]) {
    chartInstances[key] = echarts.init(dom)
  }
  return chartInstances[key]
}

function getEmptyOption(msg) {
  return {
    xAxis: { show: false },
    yAxis: { show: false },
    graphic: {
      type: 'text',
      left: 'center',
      top: 'center',
      style: { text: msg, fill: '#bbb', fontSize: 14 },
    },
  }
}

function updateYearlyChart(data) {
  const chart = getOrCreateChart('yearly', yearlyChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无年度招生数据'))
    return
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: data.map(d => String(d.year)), axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: data.map(d => d.count),
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', fontSize: 11 },
    }],
  })
}

function updateMajorChart(data) {
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

  const colors = ['#409eff','#67c23a','#e6a23c','#f56c6c','#909399','#b37feb','#36cfc9','#ff85c0','#ffc53d','#5cdbd3']

  const series = topMajors.map((name, i) => ({
    name,
    type: 'bar',
    stack: 'total',
    itemStyle: { color: colors[i % colors.length] },
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
    series.push({ name: '其他', type: 'bar', stack: 'total', itemStyle: { color: '#dcdfe6' }, data: otherData })
  }

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { type: 'scroll', bottom: 0, fontSize: 11 },
    grid: { left: 60, right: 20, bottom: 50, top: 20 },
    xAxis: { type: 'category', data: years.map(String), axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value', minInterval: 1 },
    series,
  })
}

function updateProvinceChart(data) {
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
      formatter: p => `年份: ${years[p.data[0]]}<br/>省份: ${provinces[p.data[1]]}<br/>人数: ${p.data[2]}`,
    },
    grid: { left: 80, right: 60, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: years.map(String), splitArea: { show: true } },
    yAxis: { type: 'category', data: provinces, splitArea: { show: true } },
    visualMap: {
      min: 0,
      max: maxVal,
      calculable: true,
      orient: 'vertical',
      right: 0,
      top: 'center',
      inRange: { color: ['#e8f5e9', '#a5d6a7', '#43a047', '#1b5e20'] },
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
  const chart = getOrCreateChart('score', scoreChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无分数线数据'))
    return
  }

  const years = data.map(d => String(d.year))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, fontSize: 11 },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: years, axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value' },
    series: [
      { name: '最高分', type: 'line', data: data.map(d => d.maxscore), smooth: true, symbol: 'circle', lineStyle: { width: 2 } },
      { name: '平均分', type: 'line', data: data.map(d => d.avgscore), smooth: true, symbol: 'diamond', lineStyle: { width: 2 } },
      { name: '最低分', type: 'line', data: data.map(d => d.minscore), smooth: true, symbol: 'triangle', lineStyle: { width: 2 } },
    ],
  })
}

function updateGenderChart(data) {
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
    tooltip: { trigger: 'axis', formatter: p => {
      const yr = p[0].axisValue
      const male = p[0].value
      const female = p[1].value
      const total = male + female
      return `${yr}年<br/>男: ${male} (${total ? ((male/total)*100).toFixed(1) : 0}%)<br/>女: ${female} (${total ? ((female/total)*100).toFixed(1) : 0}%)`
    }},
    legend: { bottom: 0, fontSize: 11 },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: years.map(String), axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '男', type: 'bar', stack: 'gender', data: maleData, itemStyle: { color: '#409eff' } },
      { name: '女', type: 'bar', stack: 'gender', data: femaleData, itemStyle: { color: '#f56c6c' } },
    ],
  })
}

async function fetchData() {
  loading.value = true
  const params = {}
  if (startYear.value) params.startYear = startYear.value
  if (endYear.value) params.endYear = endYear.value

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
  } finally {
    loading.value = false
  }
}

function handleResize() {
  Object.values(chartInstances).forEach(chart => chart?.resize())
}

watch([startYear, endYear], fetchData)

onMounted(() => {
  window.addEventListener('resize', handleResize)
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

.filter-card {
  flex-shrink: 0;
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
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.filter-right {
  display: flex;
  align-items: center;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card-full {
  grid-column: 1 / -1;
}

.chart-container {
  width: 100%;
  height: 360px;
}

@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
