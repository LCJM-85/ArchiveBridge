<template>
  <div class="page-wrapper">
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
        <span class="legend-item"><span class="dot" style="background:#409eff"></span>专业</span>
        <el-icon><ArrowRight /></el-icon>
        <span class="legend-item"><span class="dot" style="background:#67c23a"></span>学位</span>
        <el-icon><ArrowRight /></el-icon>
        <span class="legend-item"><span class="dot" style="background:#e6a23c"></span>去向</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Connection, ArrowRight } from '@element-plus/icons-vue'
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
    chart.setOption({
      graphic: { type: 'text', left: 'center', top: 'center', style: { text: '暂无数据', fill: '#bbb', fontSize: 14 } },
    })
    return
  }

  const majorNames = data.links
    .filter(l => !data.links.some(ll => ll.target === l.source))
    .map(l => l.source)

  const nodeColors = {}
  data.nodes.forEach(n => {
    if (majorNames.includes(n.name)) {
      nodeColors[n.name] = '#409eff'
    } else if (['就业','升学','毕业','结业'].includes(n.name)) {
      nodeColors[n.name] = '#e6a23c'
    } else {
      nodeColors[n.name] = '#67c23a'
    }
  })

  chart.setOption({
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
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
      lineStyle: { curveness: 0.5, opacity: 0.25 },
      label: { fontSize: 12 },
      data: data.nodes.map(n => ({ ...n, itemStyle: { color: nodeColors[n.name] || '#909399' } })),
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

.chart-container {
  min-height: 550px;
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
  padding: 12px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
  border-top: 1px solid var(--border-color);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
</style>
