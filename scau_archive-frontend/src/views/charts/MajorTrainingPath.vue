<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 可视化分析</div>
        <h2 class="font-display">学科培养路径</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 专业 -->
          <g transform="translate(20 26)">
            <rect x="0" y="0" width="42" height="38" rx="8" fill="#0a2e21" stroke="rgba(47,185,132,.5)" stroke-width="1.4"/>
            <text x="21" y="24" text-anchor="middle" font-size="10" fill="#2fb984" font-family="inherit">专业</text>
          </g>
          <!-- 学位 -->
          <g transform="translate(88 26)">
            <rect x="0" y="0" width="42" height="38" rx="8" fill="#0a2e21" stroke="rgba(230,205,149,.5)" stroke-width="1.4"/>
            <text x="21" y="24" text-anchor="middle" font-size="10" fill="#d9b877" font-family="inherit">学位</text>
          </g>
          <!-- 去向 -->
          <g transform="translate(156 26)">
            <rect x="0" y="0" width="42" height="38" rx="8" fill="#0a2e21" stroke="rgba(87,196,147,.5)" stroke-width="1.4"/>
            <text x="21" y="24" text-anchor="middle" font-size="10" fill="#57c493" font-family="inherit">去向</text>
          </g>
          <!-- 流向 -->
          <path d="M64 30 C 72 30, 78 30, 86 30" fill="none" stroke="#2fb984" stroke-width="3" stroke-linecap="round"/>
          <path d="M132 30 C 140 30, 146 30, 154 30" fill="none" stroke="#d9b877" stroke-width="3" stroke-linecap="round"/>
          <path d="M66 46 C 74 46, 80 46, 84 46" fill="none" stroke="#2fb984" stroke-width="1.6" stroke-linecap="round" opacity=".5"/>
          <path d="M134 46 C 142 46, 148 46, 152 46" fill="none" stroke="#d9b877" stroke-width="1.6" stroke-linecap="round" opacity=".5"/>
          <circle cx="60" cy="74" r="2" fill="rgba(230,205,149,.5)"/>
          <circle cx="200" cy="66" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Connection /></el-icon>
            <span>学科培养路径</span>
          </div>
          <div class="card-header-right">
            <el-select
              v-model="selectedMajors"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="筛选专业（默认全部）"
              clearable
              style="width:280px"
              @change="applyFilter"
            >
              <el-option v-for="m in allMajors" :key="m" :label="m" :value="m" />
            </el-select>
          </div>
        </div>
      </template>

      <div class="chart-container">
        <div ref="sankeyChartRef" class="sankey-chart"></div>
      </div>

      <div class="legend-bar">
        <span class="legend-item"><span class="dot" style="background:var(--color-primary)"></span>专业</span>
        <el-icon class="legend-arrow"><ArrowRight /></el-icon>
        <span class="legend-item"><span class="dot" style="background:var(--color-accent)"></span>学位</span>
        <el-icon class="legend-arrow"><ArrowRight /></el-icon>
        <span class="legend-item"><span class="dot" style="background:#57c493"></span>去向</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { Connection, ArrowRight } from '@element-plus/icons-vue'
import { getChartTheme } from '@/utils/chartTheme'
import * as echarts from 'echarts'
import { fetchSankeyData } from '@/api/modules/admission'

const sankeyChartRef = ref(null)
let chart = null
let rawData = { nodes: [], links: [] }
const allMajors = ref([])
const selectedMajors = ref([])

function getFilteredData() {
  if (!selectedMajors.value.length) return rawData

  const majorSet = new Set(selectedMajors.value)
  // 只保留选中专业的 major→degree 连接
  const mdLinks = rawData.links.filter(l => majorSet.has(l.source))
  // 找出涉及的学位
  const validTargets = new Set(mdLinks.map(l => l.target))
  // 只保留这些学位的 degree→dest 连接
  const ddLinks = rawData.links.filter(l => validTargets.has(l.source))

  const keepLinks = [...mdLinks, ...ddLinks]
  const keepNames = new Set(keepLinks.flatMap(l => [l.source, l.target]))
  const keepNodes = rawData.nodes.filter(n => keepNames.has(n.name))

  return { nodes: keepNodes, links: keepLinks }
}

function renderChart(data) {
  if (!sankeyChartRef.value) return
  if (!chart) chart = echarts.init(sankeyChartRef.value)

  if (!data.nodes?.length || !data.links?.length) {
    chart.clear()
    const t = getChartTheme()
    chart.setOption({
      graphic: { type: 'text', left: 'center', top: 'center', style: { text: '暂无数据', fill: t.emptyText, fontSize: 14 } },
    })
    return
  }

  const majorNames = data.links
    .filter(l => !data.links.some(ll => ll.target === l.source))
    .map(l => l.source)

  const t = getChartTheme()
  const nodeColors = {}
  data.nodes.forEach(n => {
    if (majorNames.includes(n.name)) {
      nodeColors[n.name] = t.primary
    } else if (['就业','升学','毕业','结业'].includes(n.name)) {
      nodeColors[n.name] = '#57c493'
    } else {
      nodeColors[n.name] = t.accent
    }
  })

  chart.setOption({
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      backgroundColor: 'var(--bg-elevated)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
      formatter: p => `${p.name}<br/>${p.value ? '流向: ' + p.value + ' 人' : ''}`,
    },
    series: [{
      type: 'sankey',
      layout: 'none',
      layoutIterations: 32,
      emphasis: { focus: 'adjacency' },
      nodeAlign: 'left',
      nodeWidth: 18,
      nodeGap: 12,
      lineStyle: { curveness: 0.5, opacity: 0.28, color: 'gradient' },
      label: { fontSize: 12, color: t.textSecondary },
      data: data.nodes.map(n => ({ ...n, itemStyle: { color: nodeColors[n.name] || t.textTertiary, borderColor: 'var(--card-bg)', borderWidth: 1, borderRadius: 4 } })),
      links: data.links,
    }],
  })
}

function applyFilter() {
  renderChart(getFilteredData())
}

async function fetchData() {
  try {
    const res = await fetchSankeyData()
    rawData = res.data?.data || { nodes: [], links: [] }
    // 提取专业列表
    const allNames = Object.fromEntries(rawData.nodes.map(n => [n.name, true]))
    allMajors.value = rawData.links
      .filter(l => allNames[l.source])
      .map(l => l.source)
      .filter((v, i, a) => a.indexOf(v) === i)
    renderChart(rawData)
  } catch (e) {
    console.error('获取培养路径数据失败:', e)
  }
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  fetchData()
})

onActivated(() => {
  handleResize()
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

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-container {
  min-height: 550px;
  animation: rise-up 0.7s 0.12s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}

.sankey-chart {
  width: 100%;
  height: 600px;
}

.legend-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 0 2px;
  font-size: 13px;
  color: var(--text-secondary);
  border-top: 1px dashed var(--border-color);
}

.legend-arrow {
  color: var(--color-gold);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  box-shadow: 0 0 8px rgba(14, 138, 95, 0.35);
}

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .chart-container { animation: none !important; }
}
</style>
