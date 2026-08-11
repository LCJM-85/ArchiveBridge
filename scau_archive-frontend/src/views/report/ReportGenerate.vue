<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 智能报告</div>
        <h2 class="font-display">智能报告生成</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 报告文档 -->
          <g transform="translate(24 14)">
            <rect x="0" y="0" width="40" height="52" rx="7" fill="#0a2e21" stroke="rgba(230,205,149,.4)" stroke-width="1.4"/>
            <path d="M10 0 L10 12 L20 12" fill="none" stroke="#c9a45c" stroke-width="1.6"/>
            <line x1="8" y1="20" x2="32" y2="20" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <line x1="8" y1="28" x2="26" y2="28" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <line x1="8" y1="36" x2="30" y2="36" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <!-- 金印章 -->
            <circle cx="30" cy="44" r="7" fill="rgba(201,164,92,.18)" stroke="#d9b877" stroke-width="1.3"/>
            <path d="M27 44 h6 M30 41 v6" stroke="#d9b877" stroke-width="1.4" stroke-linecap="round"/>
          </g>
          <!-- 分析折线 -->
          <path d="M84 56 C 108 50, 126 40, 148 38 S 184 26, 208 20" fill="none" stroke="#2fb984" stroke-width="2.4" stroke-linecap="round"/>
          <g fill="#2fb984"><circle cx="108" cy="50" r="3"/><circle cx="148" cy="38" r="3"/></g>
          <circle cx="208" cy="20" r="4" fill="#d9b877"/>
          <!-- AI 徽标 -->
          <g transform="translate(150 62)">
            <rect x="0" y="0" width="30" height="18" rx="6" fill="#123d2c" stroke="rgba(201,164,92,.5)" stroke-width="1.2"/>
            <text x="15" y="13" text-anchor="middle" font-size="10" fill="#e6cd95" font-family="inherit" font-weight="bold">AI</text>
          </g>
          <circle cx="66" cy="22" r="2" fill="rgba(230,205,149,.5)"/>
          <circle cx="196" cy="64" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="control-card">
      <div class="control-bar">
        <div class="control-left">
          <span class="control-badge"><el-icon size="17"><Files /></el-icon></span>
          <span class="control-title">智能报告生成</span>
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
        <el-card shadow="never" class="stat-card sc-1">
          <div class="stat-icon"><el-icon :size="18"><DataBoard /></el-icon></div>
          <div class="stat-label">招生总数</div>
          <div class="stat-value">{{ reportData.overview?.total || 0 }}</div>
          <div class="stat-unit">人 · 含硕博</div>
        </el-card>
        <el-card shadow="never" class="stat-card sc-2">
          <div class="stat-icon"><el-icon :size="18"><DataAnalysis /></el-icon></div>
          <div class="stat-label">本科生录取均分</div>
          <div class="stat-value">{{ reportData.score?.avgscore || '-' }}</div>
          <div class="stat-unit">分</div>
        </el-card>
        <el-card shadow="never" class="stat-card sc-3">
          <div class="stat-icon"><el-icon :size="18"><MapLocation /></el-icon></div>
          <div class="stat-label">覆盖省份</div>
          <div class="stat-value">{{ reportData.overview?.provincecount || 0 }}</div>
          <div class="stat-unit">个 · 含硕博</div>
        </el-card>
        <el-card shadow="never" class="stat-card sc-4">
          <div class="stat-icon"><el-icon :size="18"><User /></el-icon></div>
          <div class="stat-label">男女比例</div>
          <div class="stat-value stat-inline">
            <span class="male">{{ reportData.overview?.malePct || 0 }}%</span>
            <span class="sep">:</span>
            <span class="female">{{ reportData.overview?.femalePct || 0 }}%</span>
          </div>
          <div class="stat-unit">男 : 女 · 含硕博</div>
        </el-card>
      </div>

      <div class="chart-grid">
        <el-card shadow="never" class="chart-card">
          <template #header><span>专业分布 <span class="scope-note">含硕博</span></span></template>
          <div ref="majorChartRef" class="chart-body"></div>
        </el-card>
        <el-card shadow="never" class="chart-card">
          <template #header><span>省份排名 <span class="scope-note">含硕博</span></span></template>
          <div ref="provinceChartRef" class="chart-body"></div>
        </el-card>
        <el-card shadow="never" class="chart-card">
          <template #header><span>分数线 <span class="scope-note">仅本科生</span></span></template>
          <div class="score-box">
            <div class="score-item">
              <div class="score-label">本科生最高分</div>
              <div class="score-val max">{{ reportData.score?.maxscore || '-' }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">本科生平均分</div>
              <div class="score-val avg">{{ reportData.score?.avgscore || '-' }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">本科生最低分</div>
              <div class="score-val min">{{ reportData.score?.minscore || '-' }}</div>
            </div>
          </div>
        </el-card>
        <el-card shadow="never" class="chart-card">
          <template #header><span>毕业去向 <span class="scope-note">含硕博</span></span></template>
          <div ref="destChartRef" class="chart-body"></div>
        </el-card>
      </div>

      <el-card shadow="never" class="analysis-card">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span class="analysis-title">AI 智能分析</span>
            <el-button
              size="small"
              class="btn-gold"
              :loading="aiAnalysisLoading"
              :disabled="!reportData"
              @click="fetchAiAnalysis"
            >
              {{ aiAnalysis ? '重新生成' : '生成分析' }}
            </el-button>
          </div>
        </template>
        <div v-if="aiAnalysisLoading" class="analysis-loading">
          <span class="typing-dots"><i></i><i></i><i></i></span> AI 正在分析报告数据…
        </div>
        <div v-else-if="aiAnalysis" class="analysis-content" v-html="renderAnalysis(aiAnalysis)"></div>
        <div v-else class="analysis-loading">点击「生成分析」按钮，AI 将自动分析报告数据</div>
      </el-card>

      <el-card shadow="never" class="print-card">
        <template #header>
          <div class="print-header">
            <span>A3 海报视图</span>
            <div style="display:flex;align-items:center;gap:12px">
              <span style="font-size:12px;color:var(--text-secondary)">打印前请取消勾选「页眉和页脚」</span>
              <el-button size="small" type="primary" class="btn-gold" @click="printPoster">打印 / 导出 PDF</el-button>
            </div>
          </div>
        </template>
        <div class="poster">
          <div class="poster-title">{{ selectedYear }}年招生质量报告</div>
          <div class="poster-subtitle">华南农业大学  ·  档案智能分析平台</div>
          <div class="poster-stats">
            <div class="ps-item"><strong>{{ reportData.overview?.total || 0 }}</strong><span>招生总数（人·含硕博）</span></div>
            <div class="ps-item"><strong>{{ reportData.score?.avgscore || '-' }}</strong><span>本科生录取均分</span></div>
            <div class="ps-item"><strong>{{ reportData.overview?.provincecount || 0 }}</strong><span>生源省份（含硕博）</span></div>
            <div class="ps-item"><strong>{{ reportData.overview?.femalePct || 0 }}%</strong><span>女生比例（含硕博）</span></div>
          </div>
          <div class="poster-table">
            <div class="poster-table-title">专业分布（含硕博）</div>
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
import { ref, watch, onMounted, onActivated, onBeforeUnmount, nextTick } from 'vue'
import { Files, DataBoard, DataAnalysis, MapLocation, User } from '@element-plus/icons-vue'
import { getChartTheme } from '@/utils/chartTheme'
import * as echarts from 'echarts'
import { fetchReportData } from '@/api/modules/admission'
import { analyzeReport } from '@/api/modules/ai'

const reportData = ref(null)
const selectedYear = ref(new Date().getFullYear())
const generateDate = ref('')
const aiAnalysis = ref('')
const aiAnalysisLoading = ref(false)

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
  const t = getChartTheme()
  nextTick(() => {
    if (data.majorDistribution?.length) {
      const c = initChart('major', majorChartRef.value)
      if (c) c.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)', backgroundColor: 'var(--bg-elevated)', borderColor: 'var(--border-color)', textStyle: { color: 'var(--text-primary)' } },
        legend: { type: 'scroll', bottom: 0, textStyle: { color: t.textSecondary, fontSize: 11 }, icon: 'circle', itemWidth: 9, itemHeight: 9 },
        series: [{
          type: 'pie', radius: ['32%', '56%'], center: ['50%', '44%'],
          itemStyle: { borderRadius: 6, borderColor: 'var(--card-bg)', borderWidth: 2 },
          label: { fontSize: 11, color: t.textSecondary },
          color: t.palette,
          data: data.majorDistribution.map(m => ({ name: m.name, value: m.count })),
        }],
      })
    }
    if (data.provinceDistribution?.length) {
      const sorted = [...data.provinceDistribution].sort((a, b) => b.count - a.count).slice(0, 15)
      const c = initChart('province', provinceChartRef.value)
      if (c) c.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'var(--bg-elevated)', borderColor: 'var(--border-color)', textStyle: { color: 'var(--text-primary)' } },
        grid: { left: 90, right: 34, top: 10, bottom: 20 },
        xAxis: { type: 'value', splitLine: { lineStyle: { color: t.borderColor, type: 'dashed' } }, axisLabel: { color: t.textTertiary } },
        yAxis: { type: 'category', data: sorted.map(d => d.name), axisLabel: { fontSize: 10, color: t.textSecondary } },
        series: [{
          type: 'bar',
          barMaxWidth: 14,
          data: sorted.map(d => d.count),
          label: { show: true, position: 'right', fontSize: 10, color: t.textSecondary },
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: t.primary },
              { offset: 1, color: 'rgba(47,185,132,0.3)' },
            ]),
            borderRadius: [0, 7, 7, 0],
          },
        }],
      })
    }
    if (data.destination?.length) {
      const c = initChart('dest', destChartRef.value)
      if (c) c.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)', backgroundColor: 'var(--bg-elevated)', borderColor: 'var(--border-color)', textStyle: { color: 'var(--text-primary)' } },
        legend: { bottom: 0, textStyle: { color: t.textSecondary, fontSize: 11 }, icon: 'circle', itemWidth: 9, itemHeight: 9 },
        series: [{
          type: 'pie', radius: ['32%', '56%'], center: ['50%', '44%'],
          itemStyle: { borderRadius: 6, borderColor: 'var(--card-bg)', borderWidth: 2 },
          label: { fontSize: 11, color: t.textSecondary },
          color: [t.primary, t.gold, '#2fb984', '#57c493', '#8fd3b0', '#b37feb'],
          data: data.destination.map(d => ({ name: d.name, value: d.count })),
        }],
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
  aiAnalysis.value = ''
  aiAnalysisLoading.value = false

  try {
    const res = await fetchReportData(selectedYear.value)
    const data = res.data?.data || null
    reportData.value = data
    generateDate.value = new Date().toLocaleDateString('zh-CN')
    if (data) renderCharts(data)
  } catch (e) {
    console.error('获取报告数据失败:', e)
  }
}

async function fetchAiAnalysis() {
  if (!reportData.value) return
  aiAnalysisLoading.value = true
  try {
    const res = await analyzeReport({ reportData: reportData.value })
    aiAnalysis.value = res.data?.data?.analysis || ''
  } catch {
    // AI 分析失败不影响页面
  } finally {
    aiAnalysisLoading.value = false
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

onActivated(() => {
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

/* ===== 控制栏 ===== */
.control-card { flex-shrink: 0; animation: rise-up 0.7s 0.08s cubic-bezier(0.2, 0.75, 0.3, 1) both; }
.control-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.control-left { display: flex; align-items: center; gap: 10px; }
.control-badge {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(140deg, #14a06f, #0b5c40);
  border: 1px solid rgba(201, 164, 92, 0.4);
  color: #e6cd95;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(6, 40, 28, 0.25);
}
.control-title {
  font-family: var(--font-display);
  font-size: 15.5px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--text-primary);
}

.btn-gold {
  background: linear-gradient(135deg, var(--color-gold), var(--color-gold-dark)) !important;
  border: none !important;
  color: #1d1608 !important;
  font-weight: 600;
}
.btn-gold:hover {
  filter: brightness(1.08);
  box-shadow: 0 6px 16px rgba(201, 164, 92, 0.4);
}

/* ===== 年度概览 ===== */
.section-title {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  margin: 4px 0 12px;
  color: var(--text-primary);
}
.section-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 15px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--color-primary), var(--color-primary-dark));
  margin-right: 9px;
  vertical-align: -2px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  animation: rise-up 0.7s 0.12s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.stat-card {
  text-align: center;
  padding: 20px 0 16px !important;
  position: relative;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease !important;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 34px rgba(7, 39, 28, 0.12) !important;
}
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
.stat-icon {
  width: 38px;
  height: 38px;
  margin: 0 auto 10px;
  border-radius: 11px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--sc1), var(--sc2));
  box-shadow: 0 8px 16px var(--sc-sd);
}
.sc-1 { --sc1: #14a06f; --sc2: #0b5c40; --sc-sd: rgba(20, 160, 111, 0.3); }
.sc-2 { --sc1: #c9a45c; --sc2: #9a7a3c; --sc-sd: rgba(201, 164, 92, 0.3); }
.sc-3 { --sc1: #5b8def; --sc2: #3a63c4; --sc-sd: rgba(91, 141, 239, 0.3); }
.sc-4 { --sc1: #2fb984; --sc2: #0e8a5f; --sc-sd: rgba(47, 185, 132, 0.3); }
.stat-label { font-size: 12.5px; color: var(--text-secondary); margin-bottom: 6px; }
.stat-value { font-size: 32px; font-weight: 700; color: var(--color-primary); font-variant-numeric: tabular-nums; }
.stat-inline { font-size: 24px; }
.stat-inline .male { color: var(--color-primary); }
.stat-inline .sep { color: var(--text-tertiary); margin: 0 4px; font-size: 18px; }
.stat-inline .female { color: #e64545; }
.stat-unit { font-size: 11.5px; color: var(--text-tertiary); margin-top: 2px; }
.scope-note {
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 400;
  margin-left: 4px;
  background: var(--bg-tertiary);
  padding: 1px 8px;
  border-radius: 999px;
  vertical-align: 2px;
}

/* ===== 图表 ===== */
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  animation: rise-up 0.7s 0.18s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.chart-card { min-height: 280px; }
.chart-body { width: 100%; height: 280px; }

.score-box { display: flex; justify-content: space-around; padding: 34px 0; }
.score-item { text-align: center; }
.score-label { font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.score-val { font-size: 36px; font-weight: 700; font-variant-numeric: tabular-nums; }
.score-val.max { color: #e64545; }
.score-val.avg { color: var(--color-primary); }
.score-val.min { color: #67c23a; }

/* ===== AI 分析 ===== */
.print-card { page-break-inside: avoid; }
.analysis-card {
  margin-top: 16px;
  animation: rise-up 0.7s 0.24s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.analysis-title { font-weight: 600; color: var(--text-primary); }
.analysis-content { font-size: 14px; line-height: 1.8; color: var(--text-primary); }
.analysis-content h4 { margin: 8px 0 4px; font-size: 15px; color: var(--color-primary); }
.analysis-content strong { color: var(--color-primary); }
.analysis-loading { font-size: 14px; color: var(--text-tertiary); padding: 10px 0; display: flex; align-items: center; gap: 8px; }
.typing-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-gold);
  display: inline-block;
  margin-right: 3px;
  animation: blink 1.2s ease infinite;
}
.typing-dots i:nth-child(2) { animation-delay: 0.2s; }
.typing-dots i:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}
.print-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }

/* ===== 海报 ===== */
.poster {
  background:
    radial-gradient(600px 260px at 85% -40px, rgba(47, 185, 132, 0.35), transparent 62%),
    radial-gradient(420px 200px at 0% 110%, rgba(201, 164, 92, 0.2), transparent 60%),
    linear-gradient(135deg, #0b5c40, #07271c);
  color: #fff;
  border-radius: 12px;
  padding: 40px;
  min-height: 500px;
}
.poster-title {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 700;
  text-align: center;
  letter-spacing: 3px;
  margin-bottom: 6px;
}
.poster-subtitle { font-size: 14px; text-align: center; opacity: 0.7; margin-bottom: 32px; letter-spacing: 2px; }
.poster-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 32px; }
.ps-item {
  text-align: center;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(230, 205, 149, 0.2);
  border-radius: 12px;
  padding: 20px;
}
.ps-item strong { display: block; font-size: 36px; font-weight: 700; color: var(--color-gold-light); }
.ps-item span { display: block; font-size: 13px; opacity: 0.8; margin-top: 4px; }
.poster-table { background: rgba(255, 255, 255, 0.08); border-radius: 12px; padding: 20px; margin-bottom: 20px; }
.poster-table-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; opacity: 0.8; }
.poster-table table { width: 100%; border-collapse: collapse; font-size: 14px; }
.poster-table th, .poster-table td { padding: 8px 12px; text-align: left; border-bottom: 1px solid rgba(255, 255, 255, 0.15); }
.poster-table th { opacity: 0.7; font-weight: 500; }
.poster-footer { text-align: center; font-size: 12px; opacity: 0.5; }
.poster-analysis { margin-top: 24px; padding: 16px; background: rgba(255, 255, 255, 0.08); border-radius: 12px; }
.poster-analysis-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; opacity: 0.8; }
.poster-analysis-content { font-size: 12px; line-height: 1.7; opacity: 0.85; }

/* ===== 打印 ===== */
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

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .control-card, .overview-grid, .chart-grid, .analysis-card { animation: none !important; }
  .stat-card { transition: none !important; }
}

@media (max-width: 900px) {
  .overview-grid, .chart-grid { grid-template-columns: 1fr; }
  .poster-stats { grid-template-columns: repeat(2, 1fr); }
}
.empty-img { opacity: 0.5; }
</style>
