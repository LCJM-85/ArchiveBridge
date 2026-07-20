<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="control-card">
      <div class="control-bar">
        <div class="control-left">
          <el-icon size="18" color="var(--color-primary)"><Files /></el-icon>
          <span>智能报告生成</span>
        </div>
        <div class="control-right">
          <el-select v-model="selectedYear" placeholder="选择年份" style="width:140px">
            <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
          </el-select>
        </div>
      </div>
    </el-card>

    <template v-if="reportData">
      <div class="section-title">年度概览</div>
      <div class="overview-grid">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">招生总数</div>
          <div class="stat-value">{{ reportData.overview?.total || 0 }}</div>
          <div class="stat-unit">人</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">平均分</div>
          <div class="stat-value">{{ reportData.score?.avgscore || '-' }}</div>
          <div class="stat-unit">分</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">覆盖省份</div>
          <div class="stat-value">{{ reportData.overview?.provincecount || 0 }}</div>
          <div class="stat-unit">个</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">男女比例</div>
          <div class="stat-value stat-inline">
            <span class="male">{{ reportData.overview?.malePct || 0 }}%</span>
            <span class="sep">:</span>
            <span class="female">{{ reportData.overview?.femalePct || 0 }}%</span>
          </div>
          <div class="stat-unit">男 : 女</div>
        </el-card>
      </div>

      <div class="chart-grid">
        <el-card shadow="never" class="chart-card">
          <template #header><span>专业分布</span></template>
          <div ref="majorChartRef" class="chart-body"></div>
        </el-card>
        <el-card shadow="never" class="chart-card">
          <template #header><span>省份排名</span></template>
          <div ref="provinceChartRef" class="chart-body"></div>
        </el-card>
        <el-card shadow="never" class="chart-card">
          <template #header><span>分数线</span></template>
          <div class="score-box">
            <div class="score-item">
              <div class="score-label">最高分</div>
              <div class="score-val max">{{ reportData.score?.maxscore || '-' }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">平均分</div>
              <div class="score-val avg">{{ reportData.score?.avgscore || '-' }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">最低分</div>
              <div class="score-val min">{{ reportData.score?.minscore || '-' }}</div>
            </div>
          </div>
        </el-card>
        <el-card shadow="never" class="chart-card">
          <template #header><span>毕业去向</span></template>
          <div ref="destChartRef" class="chart-body"></div>
        </el-card>
      </div>

      <el-card shadow="never" class="analysis-card">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>AI 智能分析</span>
            <el-button
              size="small"
              type="primary"
              :loading="aiAnalysisLoading"
              :disabled="!reportData"
              @click="fetchAiAnalysis"
            >
              {{ aiAnalysis ? '重新生成' : '生成分析' }}
            </el-button>
          </div>
        </template>
        <div v-if="aiAnalysisLoading" class="analysis-loading">AI 正在分析报告数据…</div>
        <div v-else-if="aiAnalysis" class="analysis-content" v-html="renderAnalysis(aiAnalysis)"></div>
        <div v-else class="analysis-loading">点击「生成分析」按钮，AI 将自动分析报告数据</div>
      </el-card>

      <el-card shadow="never" class="print-card">
        <template #header>
          <div class="print-header">
            <span>A3 海报视图</span>
            <div style="display:flex;align-items:center;gap:12px">
              <span style="font-size:12px;color:var(--text-secondary)">打印前请取消勾选「页眉和页脚」</span>
              <el-button size="small" type="primary" @click="printPoster">打印 / 导出 PDF</el-button>
            </div>
          </div>
        </template>
        <div class="poster">
          <div class="poster-title">{{ selectedYear }}年招生质量报告</div>
          <div class="poster-subtitle">华南农业大学  ·  档案智能分析平台</div>
          <div class="poster-stats">
            <div class="ps-item"><strong>{{ reportData.overview?.total || 0 }}</strong><span>招生总数（人）</span></div>
            <div class="ps-item"><strong>{{ reportData.score?.avgscore || '-' }}</strong><span>平均分</span></div>
            <div class="ps-item"><strong>{{ reportData.overview?.provincecount || 0 }}</strong><span>生源省份</span></div>
            <div class="ps-item"><strong>{{ reportData.overview?.femalePct || 0 }}%</strong><span>女生比例</span></div>
          </div>
          <div class="poster-table">
            <table>
              <thead><tr><th>专业</th><th>人数</th><th>占比</th></tr></thead>
              <tbody>
                <tr v-for="m in (reportData.majorDistribution || []).slice(0, 8)" :key="m.name">
                  <td>{{ m.name }}</td><td>{{ m.count }}</td><td>{{ m.pct }}%</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="aiAnalysis" class="poster-analysis">
            <div class="poster-analysis-title">AI 智能分析</div>
            <div class="poster-analysis-content" v-html="renderAnalysis(aiAnalysis)"></div>
          </div>
          <div class="poster-footer">报告生成日期: {{ generateDate }}</div>
        </div>
      </el-card>
    </template>

    <el-card v-else shadow="never">
      <el-empty :image-size="120" description="选择年份查看报告">
        <template #image>
          <div class="empty-img">
            <svg viewBox="0 0 80 80" width="100" height="100" fill="none">
              <path d="M15 10h35l15 15v45a5 5 0 01-5 5H15a5 5 0 01-5-5V15a5 5 0 015-5z" stroke="currentColor" stroke-width="2" opacity="0.3"/>
              <path d="M50 10v15h15" stroke="currentColor" stroke-width="1.5" opacity="0.2"/>
            </svg>
          </div>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { Files } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { fetchReportData } from '@/api/modules/admission'
import { analyzeReport } from '@/api/modules/ai'
import { useReportStore } from '@/store/report'

const reportStore = useReportStore()
const { reportData, selectedYear, generateDate, aiAnalysis, aiAnalysisLoading } = storeToRefs(reportStore)

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: currentYear - 2019 }, (_, i) => 2020 + i)

const majorChartRef = ref(null)
const provinceChartRef = ref(null)
const destChartRef = ref(null)

let charts = {}

function initChart(key, dom) {
  if (!dom) return null
  if (charts[key]) charts[key].dispose()
  charts[key] = echarts.init(dom)
  return charts[key]
}

function renderCharts(data) {
  nextTick(() => {
    if (data.majorDistribution?.length) {
      const c = initChart('major', majorChartRef.value)
      if (c) c.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
        series: [{ type: 'pie', radius: ['30%', '55%'], data: data.majorDistribution.map(m => ({ name: m.name, value: m.count })) }],
      })
    }
    if (data.provinceDistribution?.length) {
      const sorted = [...data.provinceDistribution].sort((a, b) => b.count - a.count).slice(0, 15)
      const c = initChart('province', provinceChartRef.value)
      if (c) c.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 90, right: 20, top: 10, bottom: 20 },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: sorted.map(d => d.name), axisLabel: { fontSize: 10 } },
        series: [{ type: 'bar', data: sorted.map(d => d.count), label: { show: true, position: 'right', fontSize: 10 } }],
      })
    }
    if (data.destination?.length) {
      const c = initChart('dest', destChartRef.value)
      if (c) c.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
        series: [{ type: 'pie', radius: ['30%', '55%'], data: data.destination.map(d => ({ name: d.name, value: d.count })) }],
      })
    }
  })
}

function disposeCharts() {
  Object.values(charts).forEach(c => c?.dispose())
  charts = {}
}

async function generateReport() {
  disposeCharts()
  reportStore.setAiAnalysis('')
  reportStore.setAiLoading(false)

  try {
    const res = await fetchReportData(selectedYear.value)
    const data = res.data?.data || null
    reportStore.setReport(data, new Date().toLocaleDateString('zh-CN'))
    if (data) renderCharts(data)
  } catch (e) {
    console.error('获取报告数据失败:', e)
  }
}

async function fetchAiAnalysis() {
  if (!reportData.value) return
  reportStore.setAiLoading(true)
  try {
    const res = await analyzeReport({ reportData: reportData.value })
    reportStore.setAiAnalysis(res.data?.data?.analysis || '')
  } catch {
    // AI 分析失败不影响页面
  } finally {
    reportStore.setAiLoading(false)
  }
}

function renderAnalysis(text) {
  if (!text) return ''
  return text
    .replace(/### (.+)/g, '<h4>$1</h4>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

function printPoster() {
  // 临时隐藏页头和侧边栏
  const header = document.querySelector('.header')
  const sidebar = document.querySelector('.sidebar')
  if (header) header.style.display = 'none'
  if (sidebar) sidebar.style.display = 'none'

  // 延迟执行打印，等待 DOM 更新
  setTimeout(() => {
    window.print()
    // 打印结束后恢复
    setTimeout(() => {
      if (header) header.style.display = ''
      if (sidebar) sidebar.style.display = ''
    }, 100)
  }, 100)
}

function handleResize() {
  Object.values(charts).forEach(c => c?.resize())
}

watch(selectedYear, generateReport)

onMounted(() => {
  window.addEventListener('resize', handleResize)
  if (reportData.value) {
    nextTick(() => renderCharts(reportData.value))
  } else {
    generateReport()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<style scoped>
.page-wrapper { display: flex; flex-direction: column; gap: 16px; }

.control-card { flex-shrink: 0; }
.control-bar {
  display: flex; align-items: center; justify-content: space-between;
}
.control-left {
  display: flex; align-items: center; gap: 8px;
  font-size: 15px; font-weight: 600; color: var(--text-primary);
}

.section-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }

.overview-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
}
.stat-card { text-align: center; padding: 8px 0; }
.stat-label { font-size: 13px; color: var(--text-secondary); margin-bottom: 6px; }
.stat-value { font-size: 32px; font-weight: 700; color: var(--color-primary); }
.stat-inline { font-size: 24px; }
.stat-inline .male { color: #409eff; }
.stat-inline .sep { color: #dcdfe6; margin: 0 4px; font-size: 18px; }
.stat-inline .female { color: #f56c6c; }
.stat-unit { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-card { min-height: 280px; }
.chart-body { width: 100%; height: 280px; }

.score-box { display: flex; justify-content: space-around; padding: 32px 0; }
.score-item { text-align: center; }
.score-label { font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.score-val { font-size: 36px; font-weight: 700; }
.score-val.max { color: #f56c6c; }
.score-val.avg { color: #409eff; }
.score-val.min { color: #67c23a; }

.print-card { page-break-inside: avoid; }
.analysis-card { margin-top: 16px; }
.analysis-content { font-size: 14px; line-height: 1.8; color: var(--el-text-color-primary); }
.analysis-content h4 { margin: 8px 0 4px; font-size: 15px; }
.analysis-content strong { color: var(--color-primary); }
.analysis-loading { font-size: 14px; color: var(--el-text-color-secondary); padding: 8px 0; }
.print-header { display: flex; justify-content: space-between; align-items: center; }

.poster {
  background: linear-gradient(135deg, #1a7a4e, #0d5c38);
  color: #fff; border-radius: 8px; padding: 40px; min-height: 500px;
}
.poster-title { font-size: 32px; font-weight: 700; text-align: center; margin-bottom: 6px; }
.poster-subtitle { font-size: 14px; text-align: center; opacity: 0.7; margin-bottom: 32px; }
.poster-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }
.ps-item { text-align: center; background: rgba(255,255,255,0.12); border-radius: 10px; padding: 20px; }
.ps-item strong { display: block; font-size: 36px; font-weight: 700; }
.ps-item span { display: block; font-size: 13px; opacity: 0.8; margin-top: 4px; }
.poster-table { background: rgba(255,255,255,0.1); border-radius: 10px; padding: 20px; margin-bottom: 20px; }
.poster-table table { width: 100%; border-collapse: collapse; font-size: 14px; }
.poster-table th, .poster-table td { padding: 8px 12px; text-align: left; border-bottom: 1px solid rgba(255,255,255,0.15); }
.poster-table th { opacity: 0.7; font-weight: 500; }
.poster-footer { text-align: center; font-size: 12px; opacity: 0.5; }
.poster-analysis { margin-top: 24px; padding: 16px; background: rgba(255,255,255,0.08); border-radius: 8px; }
.poster-analysis-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; opacity: 0.8; }
.poster-analysis-content { font-size: 12px; line-height: 1.7; opacity: 0.85; }

@media print {
  body { background: #fff; }
  .control-card, .print-header, .section-title, .overview-grid, .chart-grid, .el-card:not(.print-card) {
    display: none !important;
  }
  .print-card.el-card {
    border: none !important;
    box-shadow: none !important;
    position: fixed;
    top: 0; left: 0;
    width: 100vw !important;
    margin: 0 !important;
    padding: 0 !important;
  }
  .print-card :deep(.el-card__body) { padding: 0 !important; }
  .poster { border-radius: 0; padding: 24px; min-height: 100vh; }
  @page { size: A3 landscape; margin: 0; }
}

@media (max-width: 900px) {
  .overview-grid, .chart-grid { grid-template-columns: 1fr; }
  .poster-stats { grid-template-columns: repeat(2, 1fr); }
}
.empty-img { opacity: 0.5; }
</style>
