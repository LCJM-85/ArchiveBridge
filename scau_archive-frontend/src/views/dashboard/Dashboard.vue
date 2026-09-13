<template>
  <div class="page-wrapper">
    <header class="workspace-heading">
      <div>
        <div class="eyebrow">ARCHIVE BRIDGE / OVERVIEW</div>
        <h1>档案工作台</h1>
        <p>招生、学籍与毕业，让每一段成长有迹可循。</p>
      </div>
      <div class="heading-actions">
        <span>{{ currentDate }}</span>
        <el-button :loading="loading" @click="refresh"><el-icon><Refresh /></el-icon>刷新数据</el-button>
      </div>
    </header>
    <div v-if="loadError" class="data-notice" role="alert">{{ loadError }}</div>
    <div class="overview-label"><span>馆藏数据概览</span><span>{{ updatedAt ? '本次获取于 ' + updatedAt : '等待获取数据' }}</span></div>
    <!-- 指标卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="s in stats" :key="s.key" :class="s.cls">
        <div class="s-top">
          <div class="s-ic">
            <el-icon :size="20"><component :is="s.icon" /></el-icon>
          </div>
          <div class="s-num">
            <span class="count">{{ formatMetric(s.display) }}</span>
            <small v-if="s.unit">{{ s.unit }}</small>
          </div>
        </div>
        <div class="s-label">{{ s.label }}</div>
        <div class="s-sub">{{ s.sub }}</div>
      </div>
    </div>

    <div class="section-heading"><h2>数据观察</h2><span>从历年招生到专业结构</span></div>
    <!-- 图表区 -->
    <div class="chart-grid">
      <el-card shadow="never">
        <template #header>
          <span>招生趋势 <span class="scope-note">含硕博</span></span>
        </template>
        <div class="chart-frame"><div ref="trendChartRef" class="chart-body"></div><div v-if="!dashboardData?.trend?.length" class="chart-empty">{{ loading ? '正在读取数据…' : '暂无招生趋势数据' }}</div></div>
      </el-card>
      <el-card shadow="never">
        <template #header>
          <span>专业招生规模 <span class="scope-note">人数前 8 · 含硕博</span></span>
        </template>
        <div class="chart-frame"><div ref="majorChartRef" class="chart-body"></div><div v-if="!dashboardData?.majorDistribution?.length" class="chart-empty">{{ loading ? '正在读取数据…' : '暂无专业分布数据' }}</div></div>
      </el-card>
      <el-card shadow="never">
        <template #header>
          <span>各层次招生分布</span>
        </template>
        <div class="chart-frame"><div ref="degreeChartRef" class="chart-body"></div><div v-if="!dashboardData?.degreeDistribution?.length" class="chart-empty">{{ loading ? '正在读取数据…' : '暂无招生层次数据' }}</div></div>
      </el-card>
      <el-card shadow="never">
        <template #header>
          <span>快捷入口</span>
        </template>
        <div class="quick-grid">
          <div v-for="q in quickLinks" :key="q.key" class="quick-item" tabindex="0" role="button"
            @click="goTo(q.key)" @keydown.enter.prevent="goTo(q.key)" @keydown.space.prevent="goTo(q.key)">
            <div class="q-ic">
              <el-icon :size="20"><component :is="q.icon" /></el-icon>
            </div>
            <span class="q-name">{{ q.label }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 系统概览 -->
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
            <span class="info-val">{{ dashboardData?.todayUploads ?? '—' }} 份</span>
          </div>
          <div class="info-item">
            <span class="info-key">OCR 识别任务</span>
            <span class="info-val">{{ dashboardData?.ocrCount ?? '—' }} 个</span>
          </div>
        </div>
      </el-card>
      <el-card shadow="never">
        <template #header><span>数据质量</span></template>
        <div class="quality-wrap">
          <div class="quality-head">
            <span class="info-key">综合数据质量评分</span>
            <span class="quality-tag" :class="qualityTagClass">{{ qualityTagText }}</span>
          </div>
          <div class="progress"><i :style="{ width: qualityPercent + '%' }"></i></div>
          <div class="quality-sub">分数：{{ dashboardData?.avgQuality ?? '—' }} / 100</div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onActivated, onBeforeUnmount, computed, watch } from 'vue'
import {
  Refresh, Document, DataAnalysis, TrendCharts, Upload, DataBoard, UserFilled, School, Files, Cpu
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { fetchDashboardStats } from '@/api/modules/admission'
import { getChartTheme, generatePalette } from '@/utils/chartTheme'
import { useMenuStore } from '@/store/menu'
import { useTabStore } from '@/store/tab'
import { useTheme } from '@/composables/useTheme'

const menuStore = useMenuStore()
const tabStore = useTabStore()
const { isDark } = useTheme()
const dashboardData = ref(null)
const trendChartRef = ref(null)
const majorChartRef = ref(null)
const degreeChartRef = ref(null)
let trendChart = null
let majorChart = null
let degreeChart = null

const loading = ref(false)
const loadError = ref('')
const updatedAt = ref('')
const currentDate = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
function formatMetric(value) {
  if (value == null || value === '—') return '—'
  const number = Number(value)
  return Number.isFinite(number) ? number.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : value
}

/* ---- 统计卡 ---- */
const stats = computed(() => {
  const d = dashboardData.value || {}
  const trend = d.trend || []
  // 年份范围动态计算（来自接口趋势数据，避免硬编码）
  const years = trend.map((x) => Number(x.year)).filter((y) => !Number.isNaN(y))
  const yearRange = years.length
    ? `${Math.min(...years)}–${Math.max(...years)} · 含硕博`
    : '含硕博'
  return [
    {
      key: 'admission', cls: 'c1', icon: DataBoard, unit: '人',
      label: '招生总数', sub: yearRange,
      value: d.totalAdmissions, display: d.totalAdmissions ?? '—',
    },
    {
      key: 'graduate', cls: 'c2', icon: School, unit: '人',
      label: '毕业人数', sub: '已归档 · 含硕博',
      value: d.totalGraduates, display: d.totalGraduates ?? '—',
    },
    {
      key: 'major', cls: 'c3', icon: Files, unit: '个',
      label: '开设专业', sub: '本科 · 硕士 · 博士',
      value: d.majorCount, display: d.majorCount ?? '—',
    },
    {
      key: 'score', cls: 'c4', icon: DataAnalysis, unit: '分',
      label: '本科生录取均分', sub: '仅统计学士群体',
      value: d.avgScore, display: d.avgScore ?? '—',
    },
  ]
})

/* ---- 数据质量 ---- */
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
const qualityPercent = computed(() => {
  const q = dashboardData.value?.avgQuality
  if (q == null) return 0
  return Math.min(Math.max(q, 0), 100)
})

/* ---- 快捷入口 ---- */
const quickLinks = [
  { key: 'upload', icon: Upload, label: '数据采集' },
  { key: 'process', icon: Document, label: 'OCR 识别' },
  { key: 'admission', icon: DataBoard, label: '招生数据' },
  { key: 'studentstatus', icon: UserFilled, label: '学籍数据' },
  { key: 'graduation', icon: School, label: '毕业数据' },
  { key: 'report', icon: Files, label: '智能报告' },
  { key: 'trend', icon: TrendCharts, label: '趋势分析' },
  { key: 'prediction', icon: Cpu, label: '智能预测' },
]

/* ---- 图表 ---- */
function renderCharts(data) {
  if (!trendChartRef.value || !majorChartRef.value) return
  const t = getChartTheme()
  const trend = data.trend || []
  if (trend.length) {
    if (!trendChart) trendChart = echarts.init(trendChartRef.value)
    trendChart.resize()
    trendChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: t.surface,
        borderColor: t.borderColor,
        textStyle: { color: t.textPrimary },
      },
      grid: { left: 52, right: 24, bottom: 30, top: 14 },
      xAxis: {
        type: 'category', data: trend.map((d) => String(d.year)),
        axisLine: { lineStyle: { color: t.axisLine } },
        axisLabel: { color: t.textTertiary },
      },
      yAxis: {
        type: 'value', minInterval: 1,
        axisLine: { show: false },
        splitLine: { lineStyle: { color: t.borderColor, type: 'dashed' } },
        axisLabel: { color: t.textTertiary },
      },
      series: [{
        type: 'line', smooth: false, data: trend.map((d) => d.count),
        areaStyle: { color: t.primary, opacity: 0.06 },
        lineStyle: { color: t.primary, width: 2.5 },
        itemStyle: { color: t.primary },
        symbol: 'circle', symbolSize: 6,
        animationDuration: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 300,
        animationEasing: 'cubicOut',
      }],
    })
  }

  const majors = [...(data.majorDistribution || [])].sort((a, b) => b.count - a.count).slice(0, 8)
  if (majors.length) {
    if (!majorChart) majorChart = echarts.init(majorChartRef.value)
    majorChart.resize()
    majorChart.setOption({
      animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
      tooltip: { trigger: 'axis', backgroundColor: t.surface, borderColor: t.borderColor, textStyle: { color: t.textPrimary } },
      grid: { left: 8, right: 46, top: 10, bottom: 8, containLabel: true },
      xAxis: { type: 'value', show: false },
      yAxis: {
        type: 'category', inverse: true, data: majors.map(m => m.name),
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: t.textSecondary, width: 108, overflow: 'truncate', fontSize: 12 },
      },
      series: [{
        type: 'bar', barMaxWidth: 12, data: majors.map(m => m.count),
        itemStyle: { color: t.primary, borderRadius: [0, 2, 2, 0] },
        label: { show: true, position: 'right', color: t.textSecondary, fontSize: 11 },
      }],
    })
  } else { majorChart?.clear() }

  const degreeDist = data.degreeDistribution || []
  if (degreeDist.length) {
    if (!degreeChart) degreeChart = echarts.init(degreeChartRef.value)
    degreeChart.resize()
    degreeChart.setOption({
      tooltip: {
        trigger: 'item', formatter: '{b}: {c}人 ({d}%)',
        backgroundColor: t.surface,
        borderColor: t.borderColor,
        textStyle: { color: t.textPrimary },
      },
      legend: {
        bottom: 0, icon: 'circle',
        textStyle: { color: t.textSecondary, fontSize: 11 },
        itemWidth: 9, itemHeight: 9,
      },
      series: [{
        type: 'pie', radius: ['36%', '58%'], center: ['50%', '44%'],
        label: { fontSize: 11, color: t.textSecondary },
        itemStyle: { borderRadius: 6, borderColor: t.surface, borderWidth: 2 },
        data: degreeDist.map((m) => ({ name: m.name, value: m.count })),
        color: generatePalette(degreeDist.length),
      }],
    })
  }
}

/* ---- 数据加载 ---- */
async function refresh() {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    const res = await fetchDashboardStats()
    dashboardData.value = res.data?.data || {}
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    await nextTick()
    trendChart?.clear()
    degreeChart?.clear()
    renderCharts(dashboardData.value)
  } catch (e) {
    loadError.value = dashboardData.value ? '数据更新失败，当前显示上次获取的结果。请稍后重试。' : '暂时无法获取数据，请检查服务连接后重试。'
  } finally {
    loading.value = false
  }
}

function goTo(key) {
  const item = menuStore.menuItems.find((m) => m.key === key)
  tabStore.addTab(key, item?.title || key)
}

function handleResize() {
  trendChart?.resize()
  majorChart?.resize()
  degreeChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onActivated(() => {
  refresh()
})

watch(isDark, async () => {
  await nextTick()
  if (dashboardData.value) renderCharts(dashboardData.value)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  majorChart?.dispose()
  degreeChart?.dispose()
})
</script>

<style scoped>
.page-wrapper { max-width: 1560px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; padding: 8px 0 16px; }
.workspace-heading { display: flex; justify-content: space-between; align-items: center; gap: 24px; padding: 6px 0 20px; border-bottom: 1px solid var(--border-color); }
.eyebrow { color: var(--color-gold); font-size: 10px; letter-spacing: 2px; margin-bottom: 12px; }
.workspace-heading h1 { font-size: 28px; letter-spacing: 1px; font-weight: 600; margin: 0; }
.workspace-heading p { color: var(--text-secondary); font-size: 13px; margin: 10px 0 0; }
.heading-actions { display: flex; align-items: center; gap: 18px; flex-shrink: 0; }
.heading-actions > span { font-size: 12px; color: var(--text-secondary); }
.heading-actions .el-icon { margin-right: 6px; }
.overview-label { display: flex; justify-content: space-between; color: var(--text-secondary); font-size: 12px; gap: 12px; }
.overview-label > span:first-child { font-weight: 600; color: var(--text-primary); }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border: 1px solid var(--card-border); border-radius: 8px; background: var(--card-bg); }
.stat-card { padding: 24px; min-width: 0; border-right: 1px solid var(--border-light); display: flex; flex-direction: column; }
.stat-card:last-child { border-right: none; }
.s-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; order: 1; margin: 16px 0 8px; }
.s-ic { order: 2; color: var(--text-tertiary); }
.s-num { font-size: clamp(22px, 2.2vw, 32px); font-weight: 600; line-height: 1.2; font-variant-numeric: tabular-nums; letter-spacing: -.8px; }
.s-num small { font-size: 12px; color: var(--text-tertiary); font-weight: 400; margin-left: 6px; letter-spacing: 0; }
.s-label { font-size: 13px; color: var(--text-secondary); }
.s-sub { order: 2; color: var(--text-tertiary); font-size: 11px; }
.section-heading { display: flex; align-items: baseline; gap: 16px; margin-top: 8px; }
.section-heading h2 { font-size: 16px; margin: 0; font-weight: 600; }
.section-heading > span { font-size: 12px; color: var(--text-tertiary); }
.chart-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr); gap: 20px; }
.chart-grid > *, .bottom-grid > * { min-width: 0; }
.chart-frame { position: relative; }
.chart-body { width: 100%; height: 260px; }
.chart-empty { position: absolute; inset: 0; display: grid; place-items: center; color: var(--text-tertiary); font-size: 13px; background: var(--card-bg); }
.scope-note { color: var(--text-tertiary); font-size: 11px; font-weight: 400; margin-left: 10px; }
.quick-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 22px; }
.quick-item { display: flex; align-items: center; gap: 12px; padding: 17px 4px; cursor: pointer; border-bottom: 1px solid var(--border-light); color: var(--text-secondary); }
.quick-item::after { content: '↗'; margin-left: auto; color: var(--text-tertiary); }
.quick-item:hover { color: var(--color-primary); background: var(--color-primary-light); }
.q-ic { display: flex; }
.q-name { font-size: 13px; }
.bottom-grid { display: grid; grid-template-columns: 1.35fr 1fr; gap: 20px; }
.info-list { display: grid; gap: 16px; }
.info-item, .quality-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.info-key { font-size: 13px; color: var(--text-secondary); }
.info-val { font-size: 14px; font-variant-numeric: tabular-nums; }
.quality-tag { font-size: 12px; padding: 3px 9px; border-radius: 4px; background: var(--bg-tertiary); color: var(--text-secondary); }
.tag-good { color: var(--color-primary); background: var(--color-primary-light); }
.tag-ok, .tag-medium { color: var(--text-primary); }
.tag-bad { color: var(--color-danger); }
.progress { height: 6px; background: var(--bg-tertiary); border-radius: 3px; margin-top: 24px; overflow: hidden; }
.progress i { display: block; height: 100%; background: var(--color-primary); }
.quality-sub { color: var(--text-tertiary); font-size: 12px; margin-top: 12px; }
.data-notice { padding: 12px 16px; border-left: 3px solid var(--color-warning); background: var(--card-bg); color: var(--text-secondary); font-size: 13px; }
@media (max-width: 1100px) {
  .heading-actions { flex-direction: column; align-items: flex-end; gap: 10px; }
  .stat-card { padding: 20px 16px; }
}
@media (max-width: 768px) {
  .workspace-heading { align-items: flex-start; }
  .workspace-heading h1 { font-size: 24px; }
  .heading-actions > span { display: none; }
  .chart-grid, .bottom-grid { grid-template-columns: minmax(0, 1fr); }
  .stat-card { border-bottom: 1px solid var(--border-light); }
  .stat-card:nth-child(2n) { border-right: none; }
  .stat-card:nth-last-child(-n+2) { border-bottom: none; }
  .overview-label { font-size: 11px; }
}
@media (max-width: 480px) {
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)) !important; }
  .section-heading > span { display: none; }
  .workspace-heading p { max-width: 215px; line-height: 1.8; }
}
</style>
