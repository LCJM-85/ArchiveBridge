<template>
  <div class="knowledge-page">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · RAG 知识库</div>
        <h2 class="font-display">知识库管理</h2>
        <div class="ph-rule"></div>
        <p class="ph-sub">上传文档或网页链接，自动分块并生成向量嵌入，供 AI 助手检索引用</p>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 文档 -->
          <g transform="translate(26 22)">
            <rect x="0" y="0" width="30" height="38" rx="5" fill="#0a2e21" stroke="rgba(230,205,149,.35)" stroke-width="1.3"/>
            <line x1="7" y1="12" x2="23" y2="12" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <line x1="7" y1="20" x2="19" y2="20" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <line x1="7" y1="28" x2="21" y2="28" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
          </g>
          <!-- RAG 节点网络 -->
          <g>
            <circle cx="90" cy="34" r="14" fill="#123d2c" stroke="#2fb984" stroke-width="1.6"/>
            <circle cx="90" cy="34" r="5" fill="#d9b877"/>
            <circle cx="132" cy="24" r="9" fill="#123d2c" stroke="rgba(47,185,132,.6)" stroke-width="1.4"/>
            <circle cx="132" cy="24" r="3" fill="#2fb984"/>
            <circle cx="150" cy="52" r="9" fill="#123d2c" stroke="rgba(201,164,92,.6)" stroke-width="1.4"/>
            <circle cx="150" cy="52" r="3" fill="#d9b877"/>
            <circle cx="106" cy="60" r="7" fill="#123d2c" stroke="rgba(47,185,132,.5)" stroke-width="1.3"/>
            <circle cx="106" cy="60" r="2.4" fill="#57c493"/>
          </g>
          <!-- 连接线 -->
          <g stroke="rgba(47,185,132,.45)" stroke-width="1.2" fill="none">
            <line x1="104" y1="34" x2="123" y2="26"/>
            <line x1="90" y1="48" x2="99" y2="58"/>
            <line x1="141" y1="28" x2="147" y2="44"/>
          </g>
          <!-- 检索放大镜 -->
          <g transform="translate(178 18)">
            <circle cx="0" cy="0" r="8" fill="none" stroke="#d9b877" stroke-width="2"/>
            <line x1="6" y1="6" x2="12" y2="12" stroke="#d9b877" stroke-width="2.4" stroke-linecap="round"/>
          </g>
          <circle cx="52" cy="66" r="2" fill="rgba(230,205,149,.5)"/>
          <circle cx="188" cy="60" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>
        <span>知识库操作</span>
      </template>

      <el-tabs v-model="activeTab" class="kb-tabs">
        <el-tab-pane label="上传文件" name="upload">
          <div
            class="drop-zone"
            @click="onDropZoneClick"
            @dragenter.prevent="isDragOver = true"
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop.prevent="onDrop"
            :class="{ 'drag-over': isDragOver }"
          >
            <input
              ref="fileInputRef"
              type="file"
              multiple
              accept=".pdf,.docx,.xlsx,.txt"
              class="file-input-hidden"
              @change="onFileSelect"
            />
            <div class="drop-icon">
              <el-icon :size="42"><UploadFilled /></el-icon>
            </div>
            <p class="drop-text">拖拽文件到此处，或 <em>点击选择</em></p>
            <p class="drop-tip">支持 PDF、DOCX、XLSX、TXT 格式</p>
          </div>

          <div v-if="selectedFiles.length" class="file-list">
            <div v-for="(f, i) in selectedFiles" :key="i" class="file-item">
              <span class="file-item-name">{{ f.name }}</span>
              <el-button text size="small" type="danger" @click="selectedFiles.splice(i, 1)">移除</el-button>
            </div>
          </div>

          <div style="margin-top: 12px;">
            <el-button
              type="primary"
              class="btn-gold"
              :loading="uploading"
              :disabled="selectedFiles.length === 0"
              @click="handleUpload"
            >
              {{ uploading ? '上传中...' : '开始上传' }}
            </el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="添加网页链接" name="url">
          <el-input
            v-model="urlInput"
            placeholder="输入网页链接，例如 https://www.scau.edu.cn/..."
            clearable
          />
          <el-input
            v-model="urlTitle"
            placeholder="标题（选填）"
            clearable
            style="margin-top: 10px;"
          />
          <el-button
            type="primary"
            class="btn-gold"
            @click="handleAddUrl"
            :loading="addingUrl"
            style="margin-top: 12px;"
          >
            添加
          </el-button>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <span>文档列表</span>
      </template>

      <el-table :data="docList" v-loading="loadingList" style="width: 100%;" class="kb-table">
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="file_type" label="类型" width="90" align="center">
          <template #default="{ row }">
            <span class="type-pill">{{ row.file_type }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="chunk_count" label="分块数" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="statusClass(row.status)">
              {{ statusText(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="error_msg" label="失败原因" min-width="200">
          <template #default="{ row }">
            <span style="color:var(--color-danger);font-size:13px;">{{ row.error_msg || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <span class="cell-muted">{{ formatTime(row.create_time) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-popconfirm
              title="确定删除？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button text type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  uploadKnowledgeFiles,
  uploadKnowledge,
  addKnowledgeUrl,
  getKnowledgeList,
  deleteKnowledge,
} from '@/api/modules/knowledge'

const activeTab = ref('upload')
const fileInputRef = ref(null)
const selectedFiles = ref([])
const isDragOver = ref(false)
const uploading = ref(false)
const urlInput = ref('')
const urlTitle = ref('')
const addingUrl = ref(false)
const docList = ref([])
const loadingList = ref(false)

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function statusType(status) {
  const map = { ready: 'success', parsing: 'warning', failed: 'danger' }
  return map[status] || 'info'
}

function statusClass(status) {
  return status === 'ready' ? 'st-ready'
       : status === 'parsing' ? 'st-parsing'
       : status === 'failed' ? 'st-failed'
       : 'st-info'
}

function statusText(status) {
  const map = { ready: '已完成', parsing: '解析中', failed: '失败' }
  return map[status] || status
}

function onDropZoneClick() {
  fileInputRef.value?.click()
}

function onFileSelect(e) {
  addFiles(Array.from(e.target.files))
  e.target.value = ''
}

function onDrop(e) {
  isDragOver.value = false
  addFiles(Array.from(e.dataTransfer.files))
}

function addFiles(newFiles) {
  for (const f of newFiles) {
    const ext = f.name.split('.').pop().toLowerCase()
    if (!['pdf', 'docx', 'xlsx', 'txt'].includes(ext)) continue
    if (!selectedFiles.value.find(ex => ex.name === f.name && ex.size === f.size)) {
      selectedFiles.value.push(f)
    }
  }
}

async function handleUpload() {
  if (selectedFiles.value.length === 0) return

  uploading.value = true
  for (const file of selectedFiles.value) {
    const ext = file.name.split('.').pop().toLowerCase()
    try {
      const formData = new FormData()
      formData.append('files', file)
      const uploadRes = await uploadKnowledgeFiles(formData)
      const savedFile = uploadRes.data?.data?.[0]
      if (!savedFile?.path) throw new Error('文件上传失败')

      const res = await uploadKnowledge({
        filePath: savedFile.path,
        fileName: savedFile.name,
        fileType: ext,
      })
      if (res.data?.code === 200) {
        ElMessage.success(`${file.name} 处理完成`)
      } else {
        ElMessage.error(`${file.name}: ${res.data?.message || '处理失败'}`)
      }
    } catch (e) {
      ElMessage.error(`${file.name}: ${e.message}`)
    }
  }
  uploading.value = false
  selectedFiles.value = []
  fetchList()
}

async function handleAddUrl() {
  if (!urlInput.value.trim()) {
    ElMessage.warning('请输入网页链接')
    return
  }
  addingUrl.value = true
  try {
    const res = await addKnowledgeUrl({
      url: urlInput.value.trim(),
      title: urlTitle.value.trim(),
    })
    if (res.data?.code === 200) {
      ElMessage.success('链接添加成功')
      urlInput.value = ''
      urlTitle.value = ''
      fetchList()
    } else {
      ElMessage.error(res.data?.message || '添加失败')
    }
  } catch (e) {
    ElMessage.error(e.message)
  }
  addingUrl.value = false
}

async function fetchList() {
  loadingList.value = true
  try {
    const res = await getKnowledgeList()
    docList.value = res.data?.data || []
  } catch {
    ElMessage.error('获取文档列表失败')
  }
  loadingList.value = false
}

async function handleDelete(id) {
  try {
    const res = await deleteKnowledge(id)
    if (res.data?.code === 200) {
      ElMessage.success('删除成功')
      fetchList()
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(fetchList)

onActivated(() => fetchList())
</script>

<style scoped>
.knowledge-page { display: flex; flex-direction: column; gap: 16px; }

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
  padding: 26px 34px;
  min-height: 116px;
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
  margin: 12px 0;
  border-radius: 2px;
  background: linear-gradient(90deg, #d9b877, rgba(201, 164, 92, 0));
}
.ph-sub { font-size: 12.5px; color: rgba(255, 255, 255, 0.6); margin: 0; }
.ph-right { position: relative; z-index: 2; }
.ph-art { width: 220px; height: 84px; flex-shrink: 0; }
@media (max-width: 900px) {
  .ph-art { display: none; }
  .page-hero { padding: 22px 24px; }
}

/* ===== 上传区 ===== */
.drop-zone {
  border: 2px dashed var(--border-color);
  border-radius: 16px;
  padding: 42px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
  background: linear-gradient(160deg, var(--card-bg), var(--bg-primary));
}
.drop-zone:hover, .drop-zone.drag-over {
  border-color: var(--color-gold);
  background: linear-gradient(160deg, var(--card-bg), rgba(201, 164, 92, 0.05));
  box-shadow: inset 0 0 0 3px rgba(201, 164, 92, 0.1);
}
.drop-icon {
  display: inline-flex;
  color: var(--color-gold);
  animation: floaty 3.4s ease-in-out infinite;
}
@keyframes floaty {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
.file-input-hidden { display: none; }
.drop-text { margin: 12px 0 4px; font-size: 14px; color: var(--text-secondary); }
.drop-text em { color: var(--color-primary); font-style: normal; font-weight: 600; }
.drop-tip { font-size: 12px; color: var(--text-tertiary); margin: 0; }

.file-list { margin-top: 12px; border: 1px solid var(--border-light); border-radius: 12px; overflow: hidden; }
.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-light);
  font-size: 13.5px;
}
.file-item:last-child { border-bottom: none; }
.file-item-name { color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

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

/* ===== 表格 ===== */
.kb-table :deep(.el-table th.el-table__cell) {
  background: linear-gradient(180deg, var(--bg-tertiary), var(--bg-primary)) !important;
  font-weight: 600 !important;
}
.kb-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: var(--color-primary-light) !important;
}

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
.st-ready { color: #0e8a5f; background: var(--color-primary-light); border: 1px solid rgba(14, 138, 95, 0.25); }
.st-parsing { color: var(--color-gold-dark); background: rgba(201, 164, 92, 0.12); border: 1px solid rgba(201, 164, 92, 0.35); }
.st-failed { color: #e64545; background: rgba(245, 108, 108, 0.12); border: 1px solid rgba(245, 108, 108, 0.35); }
.st-info { color: var(--text-secondary); background: var(--bg-tertiary); border: 1px solid var(--border-light); }

.cell-muted { font-size: 12px; color: var(--text-secondary); }

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .drop-icon { animation: none !important; }
}
</style>
