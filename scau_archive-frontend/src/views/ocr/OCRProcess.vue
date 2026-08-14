<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 实时监控</div>
        <h2 class="font-display">OCR 识别进程</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 扫描雷达 -->
          <g transform="translate(46 44)">
            <circle r="30" fill="none" stroke="rgba(47,185,132,.4)" stroke-width="1.4"/>
            <circle r="19" fill="none" stroke="rgba(47,185,132,.3)" stroke-width="1.2"/>
            <circle r="9" fill="none" stroke="rgba(230,205,149,.5)" stroke-width="1.2"/>
            <path d="M0 0 L0 -30 A30 30 0 0 1 21 21 Z" fill="rgba(47,185,132,.25)"/>
            <circle cx="0" cy="0" r="2.6" fill="#d9b877"/>
          </g>
          <!-- 文档扫描线 -->
          <g transform="translate(120 16)">
            <rect x="0" y="0" width="52" height="58" rx="7" fill="#0a2e21" stroke="rgba(230,205,149,.35)" stroke-width="1.4"/>
            <line x1="8" y1="12" x2="44" y2="12" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <line x1="8" y1="22" x2="36" y2="22" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <line x1="8" y1="32" x2="44" y2="32" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <rect x="8" y="42" width="36" height="9" rx="3" fill="rgba(14,138,95,.5)"/>
            <line x1="2" y1="30" x2="50" y2="30" stroke="#d9b877" stroke-width="1.6" opacity=".8"/>
          </g>
          <!-- 完成对勾 -->
          <g transform="translate(186 58)">
            <circle r="14" fill="#0a2e21" stroke="rgba(47,185,132,.5)" stroke-width="1.6"/>
            <path d="M-6 0 L-1.5 5 L7 -4" fill="none" stroke="#2fb984" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
          </g>
          <circle cx="28" cy="14" r="2" fill="rgba(230,205,149,.5)"/>
          <circle cx="106" cy="76" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Monitor /></el-icon>
            <span>OCR 文件监控</span>
          </div>
          <div class="card-header-right">
            <el-button :icon="Refresh" @click="syncAndRefresh" :loading="loading">同步刷新</el-button>
            <el-button :icon="Timer" @click="openHistory">历史记录</el-button>
          </div>
        </div>
      </template>

      <!-- 状态统计卡 -->
      <div class="stat-cards">
        <div class="stat-card sc-processing">
          <div class="sc-icon">
            <el-icon :size="20"><Loading /></el-icon>
          </div>
          <div class="sc-num"><span class="count" :data-to="stats.processing">{{ stats.processing }}</span></div>
          <div class="sc-label">处理中</div>
        </div>
        <div class="stat-card sc-warning">
          <div class="sc-icon">
            <el-icon :size="20"><WarningFilled /></el-icon>
          </div>
          <div class="sc-num"><span class="count" :data-to="stats.warning">{{ stats.warning }}</span></div>
          <div class="sc-label">警告</div>
        </div>
        <div class="stat-card sc-success">
          <div class="sc-icon">
            <el-icon :size="20"><CircleCheckFilled /></el-icon>
          </div>
          <div class="sc-num"><span class="count" :data-to="stats.success">{{ stats.success }}</span></div>
          <div class="sc-label">已完成</div>
        </div>
        <div class="stat-card sc-danger">
          <div class="sc-icon">
            <el-icon :size="20"><CircleCloseFilled /></el-icon>
          </div>
          <div class="sc-num"><span class="count" :data-to="stats.error">{{ stats.error }}</span></div>
          <div class="sc-label">失败</div>
        </div>
      </div>

      <el-table :data="todayLogs" v-loading="loading" stripe border style="width: 100%" empty-text="今日暂无处理记录">
        <el-table-column prop="fileName" label="文件名" min-width="240" />
        <el-table-column prop="fileType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <span class="type-pill">{{ row.fileType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="recognizeTime" label="处理时间" width="170" align="center">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.recognizeTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="recognizeStatus" label="状态" width="110" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="statusClass(row.recognizeStatus)">
              {{ statusText(row.recognizeStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="质量评分" width="90" align="center">
          <template #default="{ row }">
            <span v-if="scoresMap[row.fileId]" class="score-badge" :class="scoreClass(scoresMap[row.fileId].totalScore)">
              {{ scoresMap[row.fileId].totalScore }}
            </span>
            <span v-else style="color:var(--text-tertiary)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="提示信息" min-width="300">
          <template #default="{ row }">
            <span v-if="row.recognizeStatus === 'processing' && row.message" style="color:var(--color-primary)">{{ row.message }}</span>
            <span v-else-if="row.recognizeStatus === 'failed' && row.errorMessage" style="color:#f56c6c">{{ row.errorMessage }}</span>
            <span v-else-if="row.recognizeStatus === 'warning' && row.errorMessage" style="color:#e6a23c">{{ row.errorMessage }}</span>
            <span v-else-if="row.recognizeStatus === 'cancelled'">用户已取消</span>
            <span v-else style="color:var(--text-secondary)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button v-if="row.recognizeStatus === 'processing'" size="small" type="warning" text @click="handleCancelTask(row.logId)">取消</el-button>
            <el-button size="small" class="op-danger-round" :icon="Delete" circle @click="handleDeleteLog(row.logId)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="historyVisible" title="历史记录" width="95%" :close-on-click-modal="false">
      <el-table :data="historyRecords" v-loading="historyLoading" stripe border style="width: 100%" empty-text="暂无历史记录">
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileType" label="类型" width="70" align="center">
          <template #default="{ row }">
            <span class="type-pill">{{ row.fileType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="recognizeTime" label="处理时间" width="160" align="center">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.recognizeTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="recognizeStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="statusClass(row.recognizeStatus)">
              {{ statusText(row.recognizeStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="质量评分" width="80" align="center">
          <template #default="{ row }">
            <span v-if="scoresMap[row.fileId]" class="score-badge" :class="scoreClass(scoresMap[row.fileId].totalScore)">
              {{ scoresMap[row.fileId].totalScore }}
            </span>
            <span v-else style="color:var(--text-tertiary)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="提示信息" min-width="250">
          <template #default="{ row }">
            <span v-if="row.recognizeStatus === 'failed' && row.errorMessage" style="color:#f56c6c">{{ row.errorMessage }}</span>
            <span v-else-if="row.recognizeStatus === 'warning' && row.errorMessage" style="color:#e6a23c">{{ row.errorMessage }}</span>
            <span v-else style="color:var(--text-secondary)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <div style="display:flex;justify-content:center">
              <el-button size="small" class="op-danger-round" :icon="Delete" circle @click="handleDeleteLog(row.logId)" />
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="historyPage"
          v-model:page-size="historyPageSize"
          :total="historyTotal"
          layout="total, prev, pager, next"
          background
          @current-change="fetchHistory"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, onUnmounted } from 'vue'
import { Refresh, Monitor, Timer, Delete, Loading, WarningFilled, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { syncOcrLogs, fetchTodayOcrLogs, fetchOcrLogHistory, deleteOcrLog, fetchQualityScores, fetchProcessingCount, cancelOcrTask } from '@/api/modules/ocr'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const todayLogs = ref([])
const scoresMap = ref({})

const processingCount = ref(0)
let pollTimer = null

const historyVisible = ref(false)
const historyLoading = ref(false)
const historyRecords = ref([])
const historyPage = ref(1)
const historyPageSize = ref(15)
const historyTotal = ref(0)

const stats = computed(() => ({
  processing: processingCount.value,
  warning: todayLogs.value.filter(f => f.recognizeStatus === 'warning').length,
  success: todayLogs.value.filter(f => f.recognizeStatus === 'success').length,
  error: todayLogs.value.filter(f => f.recognizeStatus === 'failed').length,
}))

function statusTag(status) {
  return status === 'processing' ? 'warning'
       : status === 'warning'   ? 'warning'
       : status === 'success'   ? 'success'
       : 'danger'
}

function statusClass(status) {
  return status === 'processing' ? 'sp-processing'
       : status === 'warning'   ? 'sp-warning'
       : status === 'success'   ? 'sp-success'
       : 'sp-danger'
}

function statusText(status) {
  return status === 'processing' ? '处理中'
       : status === 'warning'   ? '有警告'
       : status === 'success'   ? '已完成'
       : status === 'cancelled' ? '已取消'
       : '处理失败'
}

function scoreTag(score) {
  return score >= 80 ? 'success'
       : score >= 60 ? 'warning'
       : 'danger'
}

function scoreClass(score) {
  return score >= 80 ? 'sb-good'
       : score >= 60 ? 'sb-ok'
       : 'sb-bad'
}

async function syncAndRefresh() {
  loading.value = true
  try {
    await syncOcrLogs()
    const res = await fetchTodayOcrLogs()
    todayLogs.value = res.data.data || []
    await fetchQualityScoresForLogs(todayLogs.value)
    ElMessage.success('同步完成')
  } catch {
    ElMessage.error('同步失败')
  } finally {
    loading.value = false
  }
}

async function fetchQualityScoresForLogs(logs) {
  const fileIds = logs.filter(l => l.fileId != null).map(l => l.fileId)
  if (fileIds.length === 0) { scoresMap.value = {}; return }
  try {
    const res = await fetchQualityScores(fileIds)
    scoresMap.value = res.data.data || {}
  } catch {
    scoresMap.value = {}
  }
}

async function pollProcessingCount() {
  try {
    const res = await fetchProcessingCount()
    processingCount.value = res.data.data || 0
  } catch {
    // 忽略
  }
}

async function fetchToday() {
  try {
    const res = await fetchTodayOcrLogs()
    todayLogs.value = res.data.data || []
    await fetchQualityScoresForLogs(todayLogs.value)
  } catch {
    // 轮询失败时保留上一次成功结果，避免统计卡片瞬间归零
  }
}

async function openHistory() {
  historyVisible.value = true
  historyPage.value = 1
  await fetchHistory()
}

async function fetchHistory() {
  historyLoading.value = true
  try {
    const res = await fetchOcrLogHistory({ current: historyPage.value, size: historyPageSize.value })
    const d = res.data.data || {}
    historyRecords.value = d.records || []
    historyTotal.value = d.total || 0
  } catch {
    historyRecords.value = []
  } finally {
    historyLoading.value = false
  }
}

async function handleDeleteLog(logId) {
  try {
    await ElMessageBox.confirm('确定删除该日志吗？', '提示', { type: 'warning' })
    await deleteOcrLog(logId)
    ElMessage.success('删除成功')
    await fetchToday()
    if (historyVisible.value) await fetchHistory()
  } catch {
    // cancelled or error
  }
}

async function handleCancelTask(logId) {
  try {
    await ElMessageBox.confirm('确定取消该任务吗？', '提示', { type: 'warning' })
    await cancelOcrTask(logId)
    ElMessage.success('任务已取消')
    await fetchToday()
  } catch {
    // cancelled or error
  }
}

onMounted(() => {
  fetchToday()
  pollProcessingCount()
  pollTimer = setInterval(() => {
    pollProcessingCount()
    fetchToday()
  }, 3000)
})

onActivated(() => {
  fetchToday()
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
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

.card-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ===== 状态统计卡 ===== */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}
.stat-card {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  padding: 18px 20px 16px;
  border: 1px solid var(--card-border);
  background: var(--card-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 30px rgba(7, 39, 28, 0.12);
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
.sc-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--sc1), var(--sc2));
  box-shadow: 0 8px 16px var(--sc-sd);
}
.sc-num {
  font-size: 30px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.15;
  color: var(--sc-main);
}
.sc-label {
  font-size: 12.5px;
  color: var(--text-secondary);
  letter-spacing: 1px;
}
.sc-processing { --sc1: #c9a45c; --sc2: #9a7a3c; --sc-sd: rgba(201, 164, 92, 0.3); --sc-main: var(--color-gold-dark); }
.sc-warning   { --sc1: #e6a23c; --sc2: #b97e1e; --sc-sd: rgba(230, 162, 60, 0.3); --sc-main: #d68910; }
.sc-success   { --sc1: #14a06f; --sc2: #0b5c40; --sc-sd: rgba(20, 160, 111, 0.3); --sc-main: var(--color-primary); }
.sc-danger    { --sc1: #f56c6c; --sc2: #c94f4f; --sc-sd: rgba(245, 108, 108, 0.3); --sc-main: #e64545; }
@media (max-width: 900px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
}

/* ===== 表格 ===== */
.table-card :deep(.el-table) {
  --el-table-header-text-color: var(--text-secondary);
}
.table-card :deep(.el-table th.el-table__cell) {
  background: linear-gradient(180deg, var(--bg-tertiary), var(--bg-primary)) !important;
  font-weight: 600 !important;
}
.table-card :deep(.el-table__row:hover > td.el-table__cell) {
  background: var(--color-primary-light) !important;
}

.cell-muted { font-size: 12px; color: var(--text-secondary); }

.type-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  border: 1px solid var(--border-light);
}

.status-pill {
  display: inline-block;
  min-width: 56px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
}
.sp-processing { color: var(--color-gold-dark); background: rgba(201, 164, 92, 0.12); border: 1px solid rgba(201, 164, 92, 0.35); }
.sp-warning { color: #d68910; background: rgba(230, 162, 60, 0.12); border: 1px solid rgba(230, 162, 60, 0.35); }
.sp-success { color: #0e8a5f; background: var(--color-primary-light); border: 1px solid rgba(14, 138, 95, 0.25); }
.sp-danger { color: #e64545; background: rgba(245, 108, 108, 0.12); border: 1px solid rgba(245, 108, 108, 0.35); }

.score-badge {
  display: inline-block;
  min-width: 34px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}
.sb-good { color: #0e8a5f; background: var(--color-primary-light); }
.sb-ok { color: #d68910; background: rgba(230, 162, 60, 0.12); }
.sb-bad { color: #e64545; background: rgba(245, 108, 108, 0.12); }

.op-danger-round {
  border-radius: 50% !important;
  color: var(--text-tertiary) !important;
  border: 1px solid var(--border-color) !important;
  transition: all 0.2s ease !important;
}
.op-danger-round:hover {
  color: var(--color-danger) !important;
  border-color: var(--color-danger) !important;
  background: rgba(245, 108, 108, 0.1) !important;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero { animation: none !important; }
  .stat-card { transition: none !important; }
}
</style>
