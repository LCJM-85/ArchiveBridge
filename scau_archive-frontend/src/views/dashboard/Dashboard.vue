<template>
  <div class="page-wrapper">
    <!-- 欢迎横幅 -->
    <div class="hero">
      <div class="h-left">
        <div class="h-hi">SCAU ARCHIVE BRIDGE · 数据总览</div>
        <h2 class="font-display">{{ greeting }}，管理员</h2>
        <div class="h-date">
          <span>{{ currentDate }}</span>
          <span class="clock">{{ clockText }}</span>
        </div>
        <button class="h-quick" @click="refresh">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </button>
      </div>
      <div class="h-art">
        <div class="ring r1"></div>
        <div class="ring r2"></div>
        <svg viewBox="0 0 360 190" preserveAspectRatio="xMidYMax meet">
          <!-- 档案盒堆 -->
          <g transform="translate(16 96)">
            <rect x="0" y="34" width="76" height="52" rx="7" fill="#0a2e21" stroke="rgba(230,205,149,.35)" stroke-width="1.4"/>
            <rect x="10" y="44" width="56" height="4" rx="2" fill="#c9a45c" opacity=".8"/>
            <rect x="10" y="54" width="38" height="4" rx="2" fill="rgba(230,205,149,.4)"/>
            <rect x="14" y="66" width="34" height="13" rx="3" fill="rgba(14,138,95,.5)"/>
            <rect x="-6" y="18" width="72" height="50" rx="7" fill="#123d2c" stroke="rgba(230,205,149,.28)" stroke-width="1.4"/>
            <rect x="4" y="28" width="52" height="4" rx="2" fill="#d9b877" opacity=".7"/>
            <rect x="4" y="38" width="36" height="4" rx="2" fill="rgba(230,205,149,.35)"/>
          </g>
          <!-- 上升折线 -->
          <path id="heroLine" d="M96 148 C 140 138, 168 122, 206 108 S 272 78, 316 52" fill="none" stroke="#d9b877" stroke-width="2.6" stroke-linecap="round"/>
          <circle cx="316" cy="52" r="4.5" fill="#d9b877"/>
          <circle cx="316" cy="52" r="9" fill="none" stroke="#d9b877" opacity=".4"/>
          <g fill="#2fb984">
            <circle cx="140" cy="136" r="3.4"/><circle cx="206" cy="108" r="3.4"/><circle cx="272" cy="78" r="3.4"/>
          </g>
          <!-- 嫩芽 -->
          <g transform="translate(322 150)">
            <path d="M3 24 V6" stroke="#2fb984" stroke-width="2" stroke-linecap="round"/>
            <path d="M3 12 C 3 4, 12 4, 15 8 C 12 9, 9 10, 6 12" fill="#3cc78f"/>
            <path d="M3 17 C 3 10, 11 9, 13 13 C 10 14, 7 15, 5 17" fill="#2fb984"/>
          </g>
          <circle cx="96" cy="40" r="2.5" fill="rgba(230,205,149,.5)"/>
          <circle cx="252" cy="150" r="2.2" fill="rgba(47,185,132,.55)"/>
          <circle cx="176" cy="30" r="1.8" fill="rgba(255,255,255,.3)"/>
        </svg>
      </div>
    </div>

    <!-- 指标卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="s in stats" :key="s.key" :class="s.cls">
        <div class="s-top">
          <div class="s-ic">
            <el-icon :size="20"><component :is="s.icon" /></el-icon>
          </div>
          <div class="s-num">
            <span class="count" :data-to="s.value">{{ s.display }}</span>
            <small v-if="s.unit">{{ s.unit }}</small>
          </div>
        </div>
        <div class="s-label">{{ s.label }}</div>
        <div class="s-sub">{{ s.sub }}</div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <el-card shadow="never">
        <template #header>
          <span>招生趋势 <span class="scope-note">含硕博</span></span>
        </template>
        <div ref="trendChartRef" class="chart-body"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>
          <span>专业分布 <span class="scope-note">含硕博</span></span>
        </template>
        <div ref="majorChartRef" class="chart-body"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>
          <span>各层次招生分布</span>
        </template>
        <div ref="degreeChartRef" class="chart-body"></div>
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
            <span class="info-val">{{ dashboardData?.todayUploads ?? 0 }} 份</span>
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
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { fetchDashboardStats } from '@/api/modules/admission'
import { getChartTheme } from '@/utils/chartTheme'
import { useMenuStore } from '@/store/menu'
import { useTabStore } from '@/store/tab'

const menuStore = useMenuStore()
const tabStore = useTabStore()
const dashboardData = ref(null)
const trendChartRef = ref(null)
const majorChartRef = ref(null)
const degreeChartRef = ref(null)
let trendChart = null
let majorChart = null
let degreeChart = null

/* ---- 问候 / 时钟 ---- */
const now = new Date()
const currentDate = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const hour = now.getHours()
const greeting = hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'
const clockText = ref('')
function tickClock() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  clockText.value = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
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

/* ---- 数字滚动 ---- */
function animateCount(el) {
  const to = Number(el.dataset.to)
  if (!to || Number.isNaN(to)) return
  const dur = 1300
  const start = performance.now()
  function tick(t) {
    const p = Math.min((t - start) / dur, 1)
    const eased = 1 - Math.pow(1 - p, 3)
    el.textContent = Math.round(to * eased).toLocaleString('zh-CN')
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}
watch(dashboardData, async () => {
  await nextTick()
  document.querySelectorAll('.stat-card .count').forEach(animateCount)
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
        backgroundColor: 'var(--bg-elevated)',
        borderColor: 'var(--border-color)',
        textStyle: { color: 'var(--text-primary)' },
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
        type: 'line', smooth: true, data: trend.map((d) => d.count),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14,138,95,0.32)' },
            { offset: 1, color: 'rgba(14,138,95,0.02)' },
          ]),
        },
        lineStyle: { color: t.primary, width: 2.5 },
        itemStyle: { color: t.primary },
        symbol: 'circle', symbolSize: 6,
        animationDuration: 1200,
        animationEasing: 'cubicOut',
      }],
    })
  }

  const majors = data.majorDistribution || []
  if (majors.length) {
    if (!majorChart) majorChart = echarts.init(majorChartRef.value)
    majorChart.resize()
    majorChart.setOption({
      tooltip: {
        trigger: 'item', formatter: '{b}: {c}人',
        backgroundColor: 'var(--bg-elevated)',
        borderColor: 'var(--border-color)',
        textStyle: { color: 'var(--text-primary)' },
      },
      legend: {
        type: 'scroll', bottom: 0, icon: 'circle',
        textStyle: { color: t.textSecondary, fontSize: 11 },
        itemWidth: 9, itemHeight: 9,
      },
      series: [{
        type: 'pie', radius: ['36%', '58%'], center: ['50%', '44%'],
        label: { fontSize: 11, color: t.textSecondary },
        itemStyle: { borderRadius: 6, borderColor: 'var(--card-bg)', borderWidth: 2 },
        data: majors.map((m) => ({ name: m.name, value: m.count })),
        color: [t.primary, '#2fb984', '#57c493', '#8fd3b0', t.gold || '#c9a45c', '#b37feb'],
      }],
    })
  }

  const degreeDist = data.degreeDistribution || []
  if (degreeDist.length) {
    if (!degreeChart) degreeChart = echarts.init(degreeChartRef.value)
    degreeChart.resize()
    degreeChart.setOption({
      tooltip: {
        trigger: 'item', formatter: '{b}: {c}人 ({d}%)',
        backgroundColor: 'var(--bg-elevated)',
        borderColor: 'var(--border-color)',
        textStyle: { color: 'var(--text-primary)' },
      },
      legend: {
        bottom: 0, icon: 'circle',
        textStyle: { color: t.textSecondary, fontSize: 11 },
        itemWidth: 9, itemHeight: 9,
      },
      series: [{
        type: 'pie', radius: ['36%', '58%'], center: ['50%', '44%'],
        label: { fontSize: 11, color: t.textSecondary },
        itemStyle: { borderRadius: 6, borderColor: 'var(--card-bg)', borderWidth: 2 },
        data: degreeDist.map((m) => ({ name: m.name, value: m.count })),
        color: [t.primary, t.gold || '#d9b877', '#2fb984', '#57c493', '#b37feb'],
      }],
    })
  }
}

/* ---- 数据加载 ---- */
async function refresh() {
  try {
    const res = await fetchDashboardStats()
    dashboardData.value = res.data?.data || {}
    await nextTick()
    renderCharts(dashboardData.value)
  } catch (e) {
    console.error('获取仪表盘数据失败:', e)
    ElMessage.error('获取仪表盘数据失败，请检查后端服务是否正常')
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
  tickClock()
  setInterval(tickClock, 1000)
  refresh()
})

onActivated(() => {
  refresh()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  majorChart?.dispose()
  degreeChart?.dispose()
})
</script>

<style scoped>
.page-wrapper { display: flex; flex-direction: column; gap: 18px; }

/* ===== 欢迎横幅 ===== */
.hero {
  position: relative;
  border-radius: 22px;
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(600px 260px at 82% -40px, rgba(47, 185, 132, 0.32), transparent 62%),
    radial-gradient(480px 220px at 100% 130%, rgba(201, 164, 92, 0.2), transparent 60%),
    linear-gradient(120deg, #07271c 0%, #0b5c40 52%, #0e8a5f 100%);
  box-shadow: 0 26px 60px rgba(7, 39, 28, 0.28);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 34px 42px;
  min-height: 190px;
  animation: rise-up 0.7s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.hero::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.05;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2'/%3E%3C/filter%3E%3Crect width='160' height='160' filter='url(%23n)' opacity='.6'/%3E%3C/svg%3E");
}
.h-left { position: relative; z-index: 2; }
.h-hi { font-size: 12px; letter-spacing: 3px; color: var(--color-gold-light); margin-bottom: 10px; }
.hero h2 { font-size: 26px; font-weight: 700; letter-spacing: 2px; margin: 0; }
.h-date { font-size: 13px; color: rgba(255, 255, 255, 0.6); margin-top: 12px; display: flex; align-items: center; gap: 12px; }
.h-date .clock { color: var(--color-gold-light); font-variant-numeric: tabular-nums; }
.h-quick {
  margin-top: 18px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.85);
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(230, 205, 149, 0.4);
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: all 0.25s ease;
}
.h-quick:hover { background: rgba(201, 164, 92, 0.16); color: #fff; transform: translateY(-1px); }

.h-art { position: relative; z-index: 2; width: 340px; height: 180px; flex-shrink: 0; }
.h-art .ring {
  position: absolute;
  border-radius: 50%;
  border: 1px dashed rgba(230, 205, 149, 0.3);
  animation: spin 40s linear infinite;
}
.h-art .ring.r1 { width: 160px; height: 160px; right: 22px; top: -4px; }
.h-art .ring.r2 { width: 220px; height: 220px; right: -16px; top: -34px; animation-direction: reverse; animation-duration: 60s; border-color: rgba(255, 255, 255, 0.14); }
@keyframes spin { to { transform: rotate(360deg); } }
.h-art svg { position: absolute; inset: 0; width: 100%; height: 100%; }
@media (max-width: 1100px) { .h-art { display: none; } }

/* ===== 统计卡 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  animation: rise-up 0.7s 0.1s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.stat-card {
  background: var(--card-bg);
  border-radius: 18px;
  padding: 22px 22px 18px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--card-border);
  box-shadow: 0 6px 20px rgba(7, 39, 28, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.stat-card:hover { transform: translateY(-5px); box-shadow: 0 18px 40px rgba(7, 39, 28, 0.12); }
.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 22px;
  right: 22px;
  height: 3px;
  border-radius: 0 0 3px 3px;
  background: linear-gradient(90deg, var(--sc1), var(--sc2));
}
.s-top { display: flex; align-items: center; gap: 14px; }
.s-ic {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--sc1), var(--sc2));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 18px var(--sc-sd);
}
.s-num { font-size: 27px; font-weight: 700; font-variant-numeric: tabular-nums; color: var(--text-primary); line-height: 1.05; }
.s-num small { font-size: 12.5px; font-weight: 500; color: var(--text-tertiary); margin-left: 3px; }
.s-label { font-size: 13px; color: var(--text-secondary); margin-top: 9px; }
.s-sub { font-size: 11px; color: var(--text-tertiary); margin-top: 3px; }
.stat-card.c1 { --sc1: #14a06f; --sc2: #0b5c40; --sc-sd: rgba(20, 160, 111, 0.32); }
.stat-card.c2 { --sc1: #c9a45c; --sc2: #9a7a3c; --sc-sd: rgba(201, 164, 92, 0.32); }
.stat-card.c3 { --sc1: #2fb984; --sc2: #0e8a5f; --sc-sd: rgba(47, 185, 132, 0.32); }
.stat-card.c4 { --sc1: #5b8def; --sc2: #3a63c4; --sc-sd: rgba(91, 141, 239, 0.32); }

/* ===== 图表区 ===== */
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.chart-grid :deep(.el-card) { animation: rise-up 0.7s 0.2s cubic-bezier(0.2, 0.75, 0.3, 1) both; }
.chart-grid :deep(.el-card:nth-child(2)) { animation-delay: 0.28s; }
.chart-grid :deep(.el-card:nth-child(3)) { animation-delay: 0.36s; }
.chart-grid :deep(.el-card:nth-child(4)) { animation-delay: 0.44s; }
.chart-body { width: 100%; height: 280px; overflow: hidden; }
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

/* ===== 快捷入口 ===== */
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; padding: 2px 0; }
.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 6px 14px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: var(--bg-tertiary);
  cursor: pointer;
  transition: all 0.25s ease;
}
.quick-item .q-ic {
  width: 44px;
  height: 44px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--card-bg);
  color: var(--color-primary);
  box-shadow: 0 6px 16px rgba(7, 39, 28, 0.08);
  transition: transform 0.3s cubic-bezier(0.3, 1.4, 0.5, 1), box-shadow 0.25s;
}
.quick-item:hover {
  transform: translateY(-4px);
  border-color: rgba(14, 138, 95, 0.35);
  box-shadow: 0 14px 30px rgba(7, 39, 28, 0.1);
  background: var(--card-bg);
}
.quick-item:hover .q-ic { transform: translateY(-3px) scale(1.08); box-shadow: 0 10px 22px rgba(7, 39, 28, 0.14); }
.q-name { font-size: 12.5px; color: var(--text-primary); font-weight: 500; }

/* ===== 系统概览 ===== */
.bottom-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.bottom-grid :deep(.el-card) { animation: rise-up 0.7s 0.5s cubic-bezier(0.2, 0.75, 0.3, 1) both; }
.bottom-grid :deep(.el-card:nth-child(2)) { animation-delay: 0.58s; }
.info-list { display: flex; flex-direction: column; gap: 16px; padding: 6px 0; }
.info-item { display: flex; justify-content: space-between; align-items: center; font-size: 14px; }
.info-key { color: var(--text-secondary); }
.info-val { font-weight: 600; }
.quality-wrap { padding: 6px 0 2px; }
.quality-head { display: flex; justify-content: space-between; align-items: center; font-size: 14px; margin-bottom: 12px; }
.progress { height: 8px; border-radius: 99px; background: var(--bg-tertiary); overflow: hidden; }
.progress i {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light));
  width: 100%;
  transform-origin: left;
  animation: grow-bar 1.4s 0.7s cubic-bezier(0.2, 0.8, 0.3, 1) both;
}
.quality-sub { margin-top: 10px; font-size: 12px; color: var(--text-tertiary); }
.quality-tag { font-size: 12px; padding: 2px 12px; border-radius: 999px; font-weight: 600; }
.tag-good { color: #0e8a5f; background: var(--color-primary-light); }
.tag-ok { color: #238a5f; background: var(--color-primary-light); }
.tag-medium { color: #e6a23c; background: rgba(230, 162, 60, 0.12); }
.tag-bad { color: #f56c6c; background: rgba(245, 108, 108, 0.12); }

/* ===== 入场动画 ===== */
@keyframes rise-up {
  from { transform: translateY(26px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@keyframes grow-bar { from { transform: scaleX(0); } to { transform: scaleX(1); } }

@media (prefers-reduced-motion: reduce) {
  .hero, .stats-grid, .chart-grid :deep(.el-card), .bottom-grid :deep(.el-card) { animation: none !important; }
  .hero::after, .h-art .ring { animation: none !important; }
  .stat-card, .quick-item { transition: none !important; }
  .quick-item:hover { transform: none; box-shadow: none; }
}

@media (max-width: 1100px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-grid, .bottom-grid { grid-template-columns: 1fr; }
  .quick-grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 640px) {
  .stats-grid { grid-template-columns: 1fr; }
  .quick-grid { grid-template-columns: repeat(2, 1fr); }
  .hero { padding: 26px; }
}
</style>
