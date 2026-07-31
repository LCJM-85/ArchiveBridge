<template>
  <div class="page-wrapper">
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

      <div class="stat-cards">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value warning">{{ stats.processing }}</div>
          <div class="stat-label">处理中</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-value warning">{{ stats.warning }}</div>
          <div class="stat-label">警告</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-value success">{{ stats.success }}</div>
          <div class="stat-label">已完成</div>
        </el-card>
        <el-card shadow="never" class="stat-card">
          <div class="stat-value danger">{{ stats.error }}</div>
          <div class="stat-label">失败</div>
        </el-card>
      </div>

      <el-table :data="todayLogs" v-loading="loading" stripe border style="width: 100%" empty-text="今日暂无处理记录">
        <el-table-column prop="fileName" label="文件名" min-width="240" />
        <el-table-column prop="fileType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fileType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recognizeTime" label="处理时间" width="170" align="center" />
        <el-table-column prop="recognizeStatus" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.recognizeStatus)" size="small" effect="dark">
              {{ statusText(row.recognizeStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="质量评分" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="scoresMap[row.fileId]" :type="scoreTag(scoresMap[row.fileId].totalScore)" size="small">
              {{ scoresMap[row.fileId].totalScore }}
            </el-tag>
            <span v-else style="color:var(--text-tertiary)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="提示信息" min-width="300">
          <template #default="{ row }">
            <span v-if="row.recognizeStatus === 'failed' && row.errorMessage" style="color:#f56c6c">{{ row.errorMessage }}</span>
            <span v-else-if="row.recognizeStatus === 'warning' && row.errorMessage" style="color:#e6a23c">{{ row.errorMessage }}</span>
            <span v-else style="color:var(--text-secondary)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ row }">
            <el-button size="small" type="danger" :icon="Delete" circle @click="handleDeleteLog(row.logId)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="historyVisible" title="历史记录" width="95%" :close-on-click-modal="false">
      <el-table :data="historyRecords" v-loading="historyLoading" stripe border style="width: 100%" empty-text="暂无历史记录">
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileType" label="类型" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fileType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recognizeTime" label="处理时间" width="160" align="center" />
        <el-table-column prop="recognizeStatus" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.recognizeStatus)" size="small" effect="dark">
              {{ statusText(row.recognizeStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="质量评分" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="scoresMap[row.fileId]" :type="scoreTag(scoresMap[row.fileId].totalScore)" size="small">
              {{ scoresMap[row.fileId].totalScore }}
            </el-tag>
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
              <el-button size="small" type="danger" :icon="Delete" circle @click="handleDeleteLog(row.logId)" />
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
import { Refresh, Monitor, Timer, Delete } from '@element-plus/icons-vue'
import { syncOcrLogs, fetchTodayOcrLogs, fetchOcrLogHistory, deleteOcrLog, fetchQualityScores, fetchProcessingCount } from '@/api/modules/ocr'
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

function statusText(status) {
  return status === 'processing' ? '处理中'
       : status === 'warning'   ? '有警告'
       : status === 'success'   ? '已完成'
       : '处理失败'
}

function scoreTag(score) {
  return score >= 80 ? 'success'
       : score >= 60 ? 'warning'
       : 'danger'
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
    todayLogs.value = []
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
    todayLogs.value = []
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

onMounted(() => {
  fetchToday()
  pollProcessingCount()
  pollTimer = setInterval(pollProcessingCount, 3000)
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

.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.stat-card {
  flex: 1;
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.stat-value.warning { color: #e6a23c; }
.stat-value.success { color: #67c23a; }
.stat-value.danger  { color: #f56c6c; }

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

</style>
