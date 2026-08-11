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
        <div class="ph-count">
          <span class="ph-count-num">{{ coveredCount }}</span>
          <span class="ph-count-label">覆盖省份</span>
        </div>
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 简化地图轮廓 -->
          <path d="M96 18 C 112 12, 132 16, 146 26 C 162 38, 172 54, 170 68 C 168 80, 152 82, 140 76 C 124 68, 108 64, 94 68 C 78 72, 62 70, 54 60 C 46 50, 48 36, 60 28 C 70 22, 82 22, 96 18 Z" fill="#0a2e21" stroke="rgba(230,205,149,.4)" stroke-width="1.4"/>
          <g fill="#2fb984"><circle cx="86" cy="44" r="3.4"/><circle cx="112" cy="38" r="2.8"/><circle cx="138" cy="56" r="3"/></g>
          <g fill="#d9b877"><circle cx="100" cy="56" r="2.4"/><circle cx="64" cy="52" r="2.2"/></g>
          <g transform="translate(150 22)">
            <path d="M0 0 C 6 -8, 14 -8, 14 -2 C 14 4, 7 12, 7 12 C 7 12, 0 4, 0 -2 Z" fill="#d9b877"/>
            <circle cx="7" cy="-3" r="2.6" fill="#0b2a1e"/>
          </g>
          <circle cx="186" cy="72" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><MapLocation /></el-icon>
            <span>录取地理分布 <span class="scope-note">含硕博</span></span>
          </div>
        </div>
      </template>

      <div class="geo-container">
        <!-- 地图主视觉 -->
        <div class="map-section">
          <div ref="mapChartRef" class="map-chart"></div>
        </div>

        <!-- TOP10 排名榜 -->
        <div class="rank-section">
          <div class="rank-title">
            <span class="rank-dot"></span>
            省份录取 TOP10
          </div>
          <div v-if="topProvinces.length" class="rank-list">
            <div v-for="(item, i) in topProvinces" :key="item.provincename" class="rank-row">
              <span class="rank-no" :class="'rank-no--' + (i + 1)">{{ i + 1 }}</span>
              <span class="rank-name">{{ item.provincename }}</span>
              <div class="rank-bar">
                <i :style="{ width: (item.count / maxCount * 100).toFixed(1) + '%', background: barColor(i) }"></i>
              </div>
              <span class="rank-val">{{ item.count }}</span>
            </div>
          </div>
          <div v-else class="rank-empty">暂无数据</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { MapLocation } from '@element-plus/icons-vue'
import { getChartTheme } from '@/utils/chartTheme'
import * as echarts from 'echarts'
import { fetchProvinceStats } from '@/api/modules/admission'
import chinaGeo from '@/assets/geo/china.json'

// 注册中国地图（构建时打包，运行时不额外请求）
// 注：不覆盖内置 'china'（echarts 完整版内置地图含南海诸岛），改用自定义名确保渲染干净的 34 省数据
echarts.registerMap('scau-china', chinaGeo)

const mapChartRef = ref(null)
let mapChart = null
const rawData = ref([])

const coveredCount = computed(() => rawData.value.length)
const topProvinces = computed(() =>
  [...rawData.value].sort((a, b) => b.count - a.count).slice(0, 10)
)
const maxCount = computed(() => rawData.value.reduce((m, d) => Math.max(m, d.count), 0))

function barColor(i) {
  if (i === 0) return 'linear-gradient(90deg, #d9b877, #c9a45c)'
  if (i === 1) return 'linear-gradient(90deg, #b8d4c4, #8fb9a4)'
  if (i === 2) return 'linear-gradient(90deg, #d0a475, #b9825a)'
  return 'linear-gradient(90deg, #14a06f, #0e8a5f)'
}

function renderMap(data) {
  const t = getChartTheme()
  if (!mapChartRef.value) return
  if (!mapChart) mapChart = echarts.init(mapChartRef.value)

  const maxVal = Math.max(...data.map(d => d.count), 1)
  const isDark = document.documentElement.classList.contains('dark')

  if (!data.length) {
    mapChart.clear()
    mapChart.setOption({
      graphic: { type: 'text', left: 'center', top: 'center', style: { text: '暂无数据', fill: t.emptyText, fontSize: 14 } },
    })
    return
  }

  mapChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(7, 39, 28, 0.92)',
      borderColor: 'rgba(201, 164, 92, 0.5)',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#fff', fontSize: 13 },
      formatter: p => {
        const v = p.value || 0
        const rank = [...data].sort((a, b) => b.count - a.count).findIndex(d => d.provincename === p.name) + 1
        const pct = maxVal ? ((v / maxVal) * 100).toFixed(1) : 0
        return `<b style="color:#e6cd95">${p.name}</b><br/>录取人数：<b style="color:#fff">${v}</b> 人<br/>相对占比：${pct}%${rank ? `<br/>省份排名：TOP ${rank}` : ''}`
      },
    },
    visualMap: {
      min: 0,
      max: maxVal,
      left: 18,
      bottom: 14,
      calculable: true,
      itemWidth: 14,
      itemHeight: 84,
      text: ['高', '低'],
      textStyle: { color: t.textSecondary, fontSize: 11 },
      inRange: { color: ['#e3efe8', '#86cba4', '#2ba373', '#0d5c3e'] },
    },
    series: [{
      type: 'map',
      map: 'scau-china',
      roam: false,
      layoutCenter: ['50%', '49%'],
      layoutSize: '118%',
      label: { show: false },
      selectedMode: false,
      itemStyle: {
        borderColor: isDark ? 'rgba(47, 185, 132, 0.8)' : 'rgba(11, 92, 64, 0.7)',
        borderWidth: 1,
        areaColor: isDark ? '#1c2c23' : '#e2ece5',
      },
      emphasis: {
        label: { show: true, fontSize: 15, fontWeight: 'bold', color: '#0b2a1e' },
        itemStyle: {
          areaColor: isDark ? '#2fb984' : '#14a06f',
          shadowBlur: 20,
          shadowColor: 'rgba(14, 138, 95, 0.55)',
          borderColor: isDark ? 'rgba(7, 39, 28, 0.9)' : '#fff',
          borderWidth: 1.5,
        },
      },
      data: data.map(d => ({ name: d.provincename, value: d.count })),
    }],
  })
}

async function fetchData() {
  try {
    const res = await fetchProvinceStats()
    rawData.value = res.data?.data || []
    renderMap(rawData.value)
  } catch (e) {
    console.error('获取地理分布数据失败:', e)
  }
}

function handleResize() {
  mapChart?.resize()
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
})
</script>

<style scoped>
.page-wrapper { display: flex; flex-direction: column; gap: 16px; }

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
  margin-bottom: 6px;
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
  margin: 8px 0 0;
  border-radius: 2px;
  background: linear-gradient(90deg, #d9b877, rgba(201, 164, 92, 0));
}
.ph-right {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 22px;
}
.ph-count { text-align: right; }
.ph-count-num {
  display: block;
  font-size: 28px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-gold-light);
  line-height: 1.1;
  text-shadow: 0 4px 18px rgba(0, 0, 0, 0.3);
}
.ph-count-label { font-size: 11.5px; color: rgba(255, 255, 255, 0.6); letter-spacing: 1px; }
.ph-art { width: 200px; height: 66px; flex-shrink: 0; }
@media (max-width: 900px) {
  .ph-art { display: none; }
  .page-hero { padding: 14px 20px; }
}

.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-header-left { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: var(--text-primary); }
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
  grid-template-columns: 1fr 340px;
  gap: 18px;
}

.map-section {
  min-height: 520px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background:
    radial-gradient(600px 300px at 70% 0%, rgba(14, 138, 95, 0.06), transparent 60%),
    var(--card-bg);
  overflow: hidden;
  position: relative;
  animation: rise-up 0.7s 0.14s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.map-chart {
  width: 100%;
  height: 520px;
}

/* ===== TOP10 排名榜 ===== */
.rank-section {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: var(--card-bg);
  padding: 16px 16px 12px;
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
  margin: 0 4px 14px;
}
.rank-dot {
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--color-primary), var(--color-primary-dark));
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  overflow-y: auto;
}
.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 10px;
  transition: background 0.2s ease;
}
.rank-row:hover {
  background: var(--bg-tertiary);
}
.rank-no {
  width: 22px;
  height: 22px;
  border-radius: 7px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  border: 1px solid var(--border-light);
  font-variant-numeric: tabular-nums;
}
.rank-no--1 {
  background: linear-gradient(135deg, #d9b877, #b8933f);
  color: #fff;
  border: none;
  box-shadow: 0 4px 12px rgba(201, 164, 92, 0.45);
}
.rank-no--2 {
  background: linear-gradient(135deg, #cfd8d2, #9db0a6);
  color: #fff;
  border: none;
}
.rank-no--3 {
  background: linear-gradient(135deg, #d8a878, #b9825a);
  color: #fff;
  border: none;
}
.rank-name {
  width: 74px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rank-bar {
  flex: 1;
  height: 10px;
  border-radius: 99px;
  background: var(--bg-tertiary);
  overflow: hidden;
}
.rank-bar i {
  display: block;
  height: 100%;
  border-radius: 99px;
  transition: width 1s cubic-bezier(0.2, 0.8, 0.3, 1);
  animation: grow-bar 1s 0.3s cubic-bezier(0.2, 0.8, 0.3, 1) both;
}
@keyframes grow-bar {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}
.rank-bar i {
  transform-origin: left;
}
.rank-val {
  width: 34px;
  text-align: right;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}
.rank-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  font-size: 13px;
}

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .map-section, .rank-section { animation: none !important; }
  .rank-bar i { animation: none !important; }
}

@media (max-width: 1000px) {
  .geo-container {
    grid-template-columns: 1fr;
  }
  .map-chart {
    height: 420px;
  }
  .rank-list {
    max-height: 420px;
  }
}

@media (max-width: 768px) {
  .map-section {
    min-height: auto;
  }
  .map-chart {
    height: 300px;
  }
}
</style>
