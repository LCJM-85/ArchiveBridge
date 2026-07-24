<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><MapLocation /></el-icon>
            <span>录取地理分布</span>
          </div>
        </div>
      </template>

      <div class="geo-container">
        <div class="map-section">
          <div ref="mapChartRef" class="map-chart"></div>
        </div>

        <div class="rank-section">
          <div class="rank-title">省份录取排名</div>
          <div ref="rankChartRef" class="rank-chart"></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { MapLocation } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { fetchProvinceStats } from '@/api/modules/admission'
import chinaGeo from '@/assets/geo/china.json'

// 注册中国地图（构建时打包，运行时不额外请求）
echarts.registerMap('china', chinaGeo)

const mapChartRef = ref(null)
const rankChartRef = ref(null)
let mapChart = null
let rankChart = null

const COLORS = ['#409eff','#67c23a','#e6a23c','#f56c6c','#909399','#b37feb','#36cfc9','#ff85c0','#ffc53d','#5cdbd3']

function renderMap(data) {
  if (!mapChartRef.value) return
  if (!mapChart) mapChart = echarts.init(mapChartRef.value)

  if (!data.length) {
    mapChart.clear()
    mapChart.setOption({
      graphic: { type: 'text', left: 'center', top: 'center', style: { text: '暂无数据', fill: '#bbb', fontSize: 14 } },
    })
    return
  }

  const maxVal = Math.max(...data.map(d => d.count), 1)
  mapChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: p => `${p.name}<br/>录取人数: ${p.value || 0}`,
    },
    visualMap: {
      min: 0,
      max: maxVal,
      left: 'left',
      bottom: 20,
      text: ['高', '低'],
      inRange: { color: ['#e8f5e9', '#a5d6a7', '#43a047', '#1b5e20'] },
      calculable: true,
    },
    series: [{
      type: 'map',
      map: 'china',
      roam: true,
      label: { show: true, fontSize: 10 },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' } },
      data: data.map(d => ({ name: d.provincename, value: d.count })),
    }],
  })
}

function renderRank(data) {
  if (!rankChartRef.value) return
  if (!rankChart) rankChart = echarts.init(rankChartRef.value)

  if (!data.length) {
    rankChart.clear()
    rankChart.setOption({
      graphic: { type: 'text', left: 'center', top: 'center', style: { text: '暂无数据', fill: '#bbb', fontSize: 14 } },
    })
    return
  }

  const sorted = [...data].sort((a, b) => b.count - a.count)
  const names = sorted.map(d => d.provincename)
  const values = sorted.map(d => d.count)

  rankChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 90, right: 30, top: 10, bottom: 20 },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 11 },
    },
    series: [{
      type: 'bar',
      data: values.map((v, i) => ({
        value: v,
        itemStyle: { color: COLORS[i % COLORS.length] },
      })),
      label: { show: true, position: 'right', fontSize: 11 },
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

.geo-container {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 16px;
  min-height: 500px;
}

.map-section {
  min-height: 500px;
}

.map-chart {
  width: 100%;
  height: 500px;
}

.rank-section {
  display: flex;
  flex-direction: column;
}

.rank-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.rank-chart {
  flex: 1;
  min-height: 400px;
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
