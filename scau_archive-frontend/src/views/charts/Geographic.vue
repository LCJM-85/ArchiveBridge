<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 可视化分析</div>
        <h2 class="font-display">录取地理分布</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 简化地图轮廓 -->
          <path d="M96 18 C 112 12, 132 16, 146 26 C 162 38, 172 54, 170 68 C 168 80, 152 82, 140 76 C 124 68, 108 64, 94 68 C 78 72, 62 70, 54 60 C 46 50, 48 36, 60 28 C 70 22, 82 22, 96 18 Z" fill="#0a2e21" stroke="rgba(230,205,149,.4)" stroke-width="1.4"/>
          <!-- 热力点 -->
          <g fill="#2fb984"><circle cx="86" cy="44" r="3.4"/><circle cx="112" cy="38" r="2.8"/><circle cx="138" cy="56" r="3"/></g>
          <g fill="#d9b877"><circle cx="100" cy="56" r="2.4"/><circle cx="64" cy="52" r="2.2"/></g>
          <!-- 定位针 -->
          <g transform="translate(150 22)">
            <path d="M0 0 C 6 -8, 14 -8, 14 -2 C 14 4, 7 12, 7 12 C 7 12, 0 4, 0 -2 Z" fill="#d9b877"/>
            <circle cx="7" cy="-3" r="2.6" fill="#0b2a1e"/>
          </g>
          <!-- 图例条 -->
          <g transform="translate(16 70)">
            <rect x="0" y="0" width="52" height="9" rx="4.5" fill="url(#geoGrad)"/>
            <text x="0" y="22" font-size="9" fill="rgba(230,205,149,.7)" font-family="inherit">低</text>
            <text x="44" y="22" font-size="9" fill="rgba(230,205,149,.7)" font-family="inherit">高</text>
          </g>
          <defs>
            <linearGradient id="geoGrad" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0" stop-color="#e6f3ec"/>
              <stop offset="1" stop-color="#0b5c40"/>
            </linearGradient>
          </defs>
          <circle cx="186" cy="72" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><MapLocation /></el-icon>
            <span>录取地理分布 <span class="scope-note">含硕博</span></span>
          </div>
        </div>
      </template>

      <div class="geo-container">
        <div class="map-section">
          <div ref="mapChartRef" class="map-chart"></div>
        </div>

        <div class="rank-section">
          <div class="rank-title">
            <span class="rank-dot"></span>
            省份录取排名
          </div>
          <div ref="rankChartRef" class="rank-chart"></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { MapLocation } from '@element-plus/icons-vue'
import { getChartTheme } from '@/utils/chartTheme'
import * as echarts from 'echarts'
import { fetchProvinceStats } from '@/api/modules/admission'
import chinaGeo from '@/assets/geo/china.json'

// 注册中国地图（构建时打包，运行时不额外请求）
echarts.registerMap('china', chinaGeo)

const mapChartRef = ref(null)
const rankChartRef = ref(null)
let mapChart = null
let rankChart = null

function renderMap(data) {
  const t = getChartTheme()
  if (!mapChartRef.value) return
  if (!mapChart) mapChart = echarts.init(mapChartRef.value)

  if (!data.length) {
    mapChart.clear()
    mapChart.setOption({
      graphic: { type: 'text', left: 'center', top: 'center', style: { text: '暂无数据', fill: t.emptyText, fontSize: 14 } },
    })
    return
  }

  const maxVal = Math.max(...data.map(d => d.count), 1)
  mapChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'var(--bg-elevated)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
      formatter: p => `${p.name}<br/>录取人数: ${p.value || 0}`,
    },
    visualMap: {
      min: 0,
      max: maxVal,
      left: 'left',
      bottom: 20,
      text: ['高', '低'],
      textStyle: { color: t.textSecondary },
      inRange: { color: ['#e6f3ec', '#8fd3b0', '#2fb984', '#0b5c40'] },
      calculable: true,
    },
    series: [{
      type: 'map',
      map: 'china',
      roam: true,
      label: { show: true, fontSize: 10, color: t.textSecondary },
      itemStyle: {
        borderColor: 'var(--card-bg)',
        borderWidth: 1,
        areaColor: 'var(--bg-tertiary)',
      },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' }, itemStyle: { shadowBlur: 12, shadowColor: 'rgba(14,138,95,.4)' } },
      data: data.map(d => ({ name: d.provincename, value: d.count })),
    }],
  })
}

function renderRank(data) {
  const t = getChartTheme()
  if (!rankChartRef.value) return
  if (!rankChart) rankChart = echarts.init(rankChartRef.value)

  if (!data.length) {
    rankChart.clear()
    rankChart.setOption({
      graphic: { type: 'text', left: 'center', top: 'center', style: { text: '暂无数据', fill: t.emptyText, fontSize: 14 } },
    })
    return
  }

  const sorted = [...data].sort((a, b) => b.count - a.count)
  const names = sorted.map(d => d.provincename)
  const values = sorted.map(d => d.count)

  rankChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'var(--bg-elevated)',
      borderColor: 'var(--border-color)',
      textStyle: { color: 'var(--text-primary)' },
    },
    grid: { left: 90, right: 34, top: 10, bottom: 20 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: t.borderColor, type: 'dashed' } }, axisLabel: { color: t.textTertiary } },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 11, color: t.textSecondary },
      axisLine: { lineStyle: { color: t.axisLine } },
    },
    series: [{
      type: 'bar',
      barMaxWidth: 14,
      data: values.map((v, i) => ({
        value: v,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: i === 0 ? t.gold : t.primary },
            { offset: 1, color: 'rgba(47,185,132,0.3)' },
          ]),
          borderRadius: [0, 7, 7, 0],
        },
      })),
      label: { show: true, position: 'right', fontSize: 11, color: t.textSecondary },
      animationDuration: 1000,
    }],
  })
}

async function fetchData() {
  try {
    const res = await fetchProvinceStats()
    const data = res.data?.data || []
    renderMap(data)
    renderRank(data)
  } catch (e) {
    console.error('获取地理分布数据失败:', e)
  }
}

function handleResize() {
  mapChart?.resize()
  rankChart?.resize()
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
  mapChart?.dispose()
  rankChart?.dispose()
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
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
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

/* ===== 地理容器 ===== */
.geo-container {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 16px;
  min-height: 500px;
}

.map-section {
  min-height: 500px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: var(--card-bg);
  overflow: hidden;
  animation: rise-up 0.7s 0.14s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}

.map-chart {
  width: 100%;
  height: 500px;
}

.rank-section {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: var(--card-bg);
  padding: 14px 12px 6px;
  overflow: hidden;
  animation: rise-up 0.7s 0.2s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}

.rank-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-display);
  font-size: 14.5px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--text-primary);
  margin: 0 6px 10px;
}

.rank-dot {
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--color-primary), var(--color-primary-dark));
}

.rank-chart {
  flex: 1;
  min-height: 400px;
}

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .map-section, .rank-section { animation: none !important; }
}

@media (max-width: 1000px) {
  .geo-container {
    grid-template-columns: 1fr;
  }
  .map-chart {
    height: 400px;
  }
}

@media (max-width: 768px) {
  .geo-container {
    min-height: auto;
  }
  .map-section {
    min-height: auto;
  }
  .map-chart {
    height: 300px;
  }
  .rank-chart {
    min-height: 300px;
  }
}
</style>
