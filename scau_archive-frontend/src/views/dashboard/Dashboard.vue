<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="greeting-card">
      <div class="greeting">
        <div>
          <div class="greeting-text">{{ greeting }}，管理员</div>
          <div class="greeting-date">{{ currentDate }}</div>
        </div>
        <el-button text @click="refresh">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </el-card>

    <!-- 指标卡片 -->
    <div class="stats-grid">
      <el-card shadow="never" class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-body">
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </el-card>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <el-card shadow="never">
        <template #header><span>招生趋势</span></template>
        <div ref="trendChartRef" class="chart-body"></div>
      </el-card>
      <el-card shadow="never">
        <template #header><span>专业分布</span></template>
        <div ref="majorChartRef" class="chart-body"></div>
      </el-card>
    </div>

    <!-- 底部 -->
    <div class="bottom-grid">
      <el-card shadow="never">
        <template #header><span>系统概览</span></template>
        <div class="info-list">
          <div class="info-item">
            <span class="info-key">档案总存储</span>
            <span class="info-val">{{ dashboardData?.totalFiles ?? '—' }} 份</span>
          </div>
          <div class="info-item">
            <span class="info-key">今日上传</span>
            <span class="info-val">{{ dashboardData?.todayUploads ?? 0 }} 份</span>
          </div>
          <div class="info-item">
            <span class="info-key">数据质量</span>
            <span class="info-val" :class="qualityTagClass">{{ qualityTagText }}</span>
          </div>
        </div>
      </el-card>
      <el-card shadow="never">
        <template #header><span>快捷入口</span></template>
        <div class="quick-links">
          <div class="quick-item" @click="goTo('upload')">
            <el-icon color="#409eff"><Upload /></el-icon>
            <span>数据采集</span>
          </div>
          <div class="quick-item" @click="goTo('process')">
            <el-icon color="#67c23a"><Document /></el-icon>
            <span>OCR 识别</span>
          </div>
          <div class="quick-item" @click="goTo('trend')">
            <el-icon color="#e6a23c"><TrendCharts /></el-icon>
            <span>趋势分析</span>
          </div>
          <div class="quick-item" @click="goTo('prediction')">
            <el-icon color="#7c3aed"><DataAnalysis /></el-icon>
            <span>智能预测</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount, computed } from 'vue'
import { Refresh, Document, DataAnalysis, TrendCharts, Upload } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { fetchDashboardStats } from '@/api/modules/admission'
import { useMenuStore } from '@/store/menu'

const menuStore = useMenuStore()
const dashboardData = ref(null)
const trendChartRef = ref(null)
const majorChartRef = ref(null)
let trendChart = null
let majorChart = null

const now = new Date()
const currentDate = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
const hour = now.getHours()
const greeting = hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'

const stats = computed(() => [
  { label: '招生总数', value: dashboardData.value?.totalAdmissions ?? '—' },
  { label: '毕业人数', value: dashboardData.value?.totalGraduates ?? '—' },
  { label: '开设专业', value: dashboardData.value?.majorCount ?? '—' },
  { label: '录取平均分', value: dashboardData.value?.avgScore ?? '—' },
])

const qualityTagText = computed(() => {
  const q = dashboardData.value?.avgQuality
  if (q == null) return '—'
  if (q >= 85) return '优'
  if (q >= 70) return '良'
  if (q >= 60) return '中'
  return '待提升'
})
const qualityTagClass = computed(() => {
  const q = dashboardData.value?.avgQuality
  if (q == null) return ''
  if (q >= 85) return 'tag-good'
  if (q >= 70) return 'tag-ok'
  if (q >= 60) return 'tag-medium'
  return 'tag-bad'
})

function renderCharts(data) {
  if (!trendChartRef.value || !majorChartRef.value) return

  // 趋势图
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const trend = data.trend || []
  if (trend.length) {
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, bottom: 30, top: 10 },
      xAxis: { type: 'category', data: trend.map(d => String(d.year)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'line', smooth: true, data: trend.map(d => d.count),
        areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:'rgba(64,158,255,0.3)'},{offset:1,color:'rgba(64,158,255,0.02)'}]) },
        lineStyle: { color: '#409eff', width: 2 },
        symbol: 'circle', symbolSize: 6,
      }],
    })
  }

  // 专业分布
  if (!majorChart) majorChart = echarts.init(majorChartRef.value)
  const majors = data.majorDistribution || []
  if (majors.length) {
    majorChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}人' },
      series: [{
        type: 'pie', radius: ['35%', '55%'],
        label: { fontSize: 11 },
        data: majors.map(m => ({ name: m.name, value: m.count })),
      }],
    })
  }
}

async function refresh() {
  try {
    const res = await fetchDashboardStats()
    dashboardData.value = res.data?.data || {}
    await nextTick()
    renderCharts(dashboardData.value)
  } catch (e) {
    console.error('获取仪表盘数据失败:', e)
  }
}

function goTo(key) {
  menuStore.setActive(key)
}

function handleResize() {
  trendChart?.resize()
  majorChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  refresh()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  majorChart?.dispose()
})
</script>

<style scoped>
.page-wrapper { display: flex; flex-direction: column; gap: 16px; }

.greeting-card { flex-shrink: 0; }
.greeting { display: flex; justify-content: space-between; align-items: center; }
.greeting-text { font-size: 18px; font-weight: 600; }
.greeting-date { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { text-align: center; padding: 16px 0; }
.stat-num { font-size: 28px; font-weight: 700; color: var(--color-primary); }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }

.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-body { width: 100%; height: 280px; }

.bottom-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.info-list { display: flex; flex-direction: column; gap: 14px; padding: 4px 0; }
.info-item { display: flex; justify-content: space-between; align-items: center; font-size: 14px; }
.info-key { color: var(--text-secondary); }
.info-val { font-weight: 600; }
.tag-good { color: #67c23a; }
.tag-ok { color: #409eff; }
.tag-medium { color: #e6a23c; }
.tag-bad { color: #f56c6c; }

.quick-links { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; padding: 4px 0; }
.quick-item {
  display: flex; align-items: center; gap: 8px;
  padding: 14px; border-radius: 8px; cursor: pointer;
  transition: background 0.2s; font-size: 14px;
}
.quick-item:hover { background: var(--bg-tertiary); }

@media (max-width: 900px) {
  .stats-grid, .chart-grid, .bottom-grid { grid-template-columns: 1fr; }
}
</style>
