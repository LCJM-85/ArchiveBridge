<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 档案管道</div>
        <h2 class="font-display">档案智能采集</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 云上传 -->
          <g transform="translate(24 26)">
            <path d="M30 34 C 22 34, 14 27, 14 20 C 14 12, 22 6, 30 6 C 36 0, 48 0, 54 6 C 62 4, 72 10, 72 18 C 72 26, 64 32, 56 34 Z" fill="#123d2c" stroke="rgba(230,205,149,.4)" stroke-width="1.4"/>
            <path d="M43 26 L43 8 M36 14 L43 7 L50 14" stroke="#d9b877" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>
          </g>
          <!-- 文件卡片 -->
          <g transform="translate(96 30)">
            <rect x="0" y="0" width="34" height="42" rx="6" fill="#0a2e21" stroke="rgba(230,205,149,.35)" stroke-width="1.3"/>
            <path d="M10 0 L10 10 L20 10" fill="none" stroke="#c9a45c" stroke-width="1.6"/>
            <line x1="8" y1="20" x2="26" y2="20" stroke="rgba(230,205,149,.4)" stroke-width="2" stroke-linecap="round"/>
            <line x1="8" y1="27" x2="22" y2="27" stroke="rgba(230,205,149,.4)" stroke-width="2" stroke-linecap="round"/>
            <line x1="8" y1="34" x2="24" y2="34" stroke="rgba(230,205,149,.4)" stroke-width="2" stroke-linecap="round"/>
          </g>
          <!-- 入库箭头 -->
          <path d="M150 52 C 170 48, 186 40, 206 30" fill="none" stroke="#2fb984" stroke-width="2" stroke-linecap="round"/>
          <path d="M198 26 L206 30 L198 34" fill="none" stroke="#2fb984" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <rect x="182" y="52" width="30" height="14" rx="4" fill="#0a2e21" stroke="rgba(230,205,149,.3)" stroke-width="1.2"/>
          <rect x="187" y="57" width="20" height="3" rx="1.5" fill="#c9a45c" opacity=".8"/>
          <circle cx="58" cy="50" r="2" fill="rgba(230,205,149,.5)"/>
          <circle cx="140" cy="18" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Upload /></el-icon>
            <span>档案智能采集</span>
          </div>
        </div>
      </template>

      <!-- Archive Type Selector -->
      <div class="archive-type-selector" :class="{ 'archive-type-selector--required': !archiveType }">
        <span class="archive-type-label">档案类型：</span>
        <el-radio-group v-model="archiveType" :disabled="isUploading">
          <el-radio value="admission">招生档案</el-radio>
          <el-radio value="graduation">毕业档案</el-radio>
        </el-radio-group>
        <span v-if="!archiveType" class="archive-type-hint">请先选择档案类型</span>
        <template v-if="archiveType === 'admission'">
          <span class="archive-type-divider"></span>
          <span class="archive-type-label">
            培养层次
            <span style="display:inline-block;color:#f56c6c;font-size:12px;border:1px solid #f56c6c;border-radius:4px;padding:0 5px;margin-left:6px;line-height:16px">必填</span>：
          </span>
          <el-select
            v-model="selectedDegreeName"
            placeholder="选择培养层次（必选）"
            size="small"
            style="width: 150px"
          >
            <el-option label="学士" value="学士" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
          <span class="archive-type-label">省份<span style="color:#909399;font-size:12px;margin-left:2px">(可选)</span>：</span>
          <el-select
            v-model="selectedProvince"
            placeholder="选择省份（可选）"
            clearable
            size="small"
            style="width: 160px"
          >
            <el-option
              v-for="p in provinces"
              :key="p.provinceId"
              :label="p.provinceName"
              :value="p.provinceName"
            />
          </el-select>
          <span class="archive-type-label">录取年份<span style="color:#909399;font-size:12px;margin-left:2px">(可选)</span>：</span>
          <el-select
            v-model="selectedAdmissionYear"
            placeholder="选择年份（可选）"
            clearable
            size="small"
            style="width: 130px"
          >
            <el-option
              v-for="y in admissionYears"
              :key="y"
              :label="String(y)"
              :value="y"
            />
          </el-select>
        </template>
        <span v-if="activeType === 'image' || activeType === 'pdf'" class="archive-type-divider"></span>
        <el-switch
          v-if="activeType === 'image' || activeType === 'pdf'"
          v-model="useLlm"
          :disabled="!llmConfigured || isUploading"
          size="small"
          style="margin-right: 6px"
        />
        <span
          v-if="activeType === 'image' || activeType === 'pdf'"
          class="archive-type-label"
          :class="{ 'llm-enabled': useLlm }"
        >
          LLM 智能提取
          <el-tooltip content="使用 AI 直接识别图片内容，无需 OCR 匹配规则" placement="top">
            <el-icon size="14" style="margin-left:2px;cursor:pointer;vertical-align:-2px"><QuestionFilled /></el-icon>
          </el-tooltip>
        </span>
      </div>

      <!-- File Type Selector -->
      <div class="type-selector">
        <div
          v-for="t in fileTypes"
          :key="t.key"
          class="type-card"
          :class="{ active: activeType === t.key, 'type-card--disabled': !archiveType }"
          tabindex="0"
          role="button"
          :aria-disabled="!archiveType"
          @click="switchType(t.key)"
          @keydown.enter.prevent="switchType(t.key)"
          @keydown.space.prevent="switchType(t.key)"
        >
          <div class="type-card-icon" :style="{ background: t.bg, color: t.color }">
            <el-icon :size="20">
              <component :is="t.icon" />
            </el-icon>
          </div>
          <span class="type-card-label">{{ t.label }}</span>
          <span class="type-card-hint">{{ t.hint }}</span>
        </div>
      </div>

      <!-- Drop Zone -->
      <div
        class="drop-zone"
        :class="{ 'drop-zone--dragover': isDragOver, 'drop-zone--has-files': files.length > 0, 'drop-zone--disabled': !archiveType }"
        @dragenter.prevent="onDragEnter"
        @dragover.prevent="isDragOver = true"
        @dragleave.prevent="isDragOver = false"
        @drop.prevent="onDrop"
        @click="onDropZoneClick"
      >
        <input
          ref="fileInput"
          type="file"
          multiple
          :accept="acceptStr"
          class="file-input-hidden"
          @change="onFileSelect"
        />
        <div class="drop-zone-icon" :class="{ 'drop-zone-icon--has': files.length > 0 }">
          <el-icon :size="38" :color="files.length > 0 ? 'var(--color-primary)' : 'var(--color-gold)'">
            <UploadFilled />
          </el-icon>
        </div>
        <p class="drop-zone-text" v-if="files.length === 0">
          拖拽文件到此处，或 <em>点击选择</em>
        </p>
        <p class="drop-zone-text" v-else>
          已选择 <strong>{{ files.length }}</strong> 个文件，继续拖拽或点击添加
        </p>
        <p class="drop-zone-hint">支持 {{ currentType.acceptLabel }}</p>
      </div>

      <!-- File List -->
      <div v-if="files.length > 0" class="file-list">
        <div class="file-list-header">
          <span>待上传文件（{{ files.length }}）</span>
          <el-button text size="small" type="danger" @click="clearAll">清空</el-button>
        </div>
        <div class="file-list-body">
          <div
            v-for="(f, i) in files"
            :key="f.uid"
            class="file-item"
            :class="{ 'file-item--uploading': f.status === 'uploading', 'file-item--done': f.status === 'done', 'file-item--error': f.status === 'error' }"
          >
            <div class="file-item-icon" :style="{ color: fileIconColor(f) }">
              <el-icon :size="18">
                <component :is="fileIcon(f)" />
              </el-icon>
            </div>
            <div class="file-item-info">
              <span class="file-item-name">{{ f.name }}</span>
              <span class="file-item-size">{{ formatSize(f.size) }}</span>
            </div>
            <div class="file-item-status">
              <el-progress
                v-if="f.status === 'uploading'"
                :percentage="f.progress"
                :width="36"
                type="circle"
                :stroke-width="3"
                color="#14a06f"
              />
              <el-icon v-else-if="f.status === 'done'" size="18" color="#67c23a"><CircleCheck /></el-icon>
              <el-icon v-else-if="f.status === 'error'" size="18" color="#f56c6c"><CircleClose /></el-icon>
              <el-button
                v-else
                text
                size="small"
                type="danger"
                :icon="Close"
                @click="removeFile(i)"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div v-if="files.length > 0" class="actions">
        <el-button @click="clearAll" :disabled="isUploading">取消</el-button>
        <el-button
          type="primary"
          class="btn-gold"
          :loading="isUploading"
          :disabled="isUploading"
          @click="handleUpload"
        >
          {{ isUploading ? '上传中...' : '确认上传' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { defineOptions } from 'vue'

defineOptions({ name: 'ArchiveUpload' })
import { ElMessage, ElNotification } from 'element-plus'
import { uploadFiles } from '@/api/modules/archive'
import { fetchProvinces, fetchLLMStatus } from '@/api/modules/admission'
import {
  Upload,
  UploadFilled,
  Document,
  DocumentCopy,
  Picture,
  List,
  Grid,
  CircleCheck,
  CircleClose,
  Close,
  QuestionFilled,
} from '@element-plus/icons-vue'

const fileTypes = [
  { key: 'image', label: '图片文件', hint: '.jpg / .png / .tiff', icon: Picture, accept: '.jpg,.jpeg,.png,.tif,.tiff,.bmp', acceptLabel: '.jpg / .png / .tiff 等图片文件', bg: 'rgba(91, 141, 239, 0.12)', color: '#5b8def' },
  { key: 'pdf', label: 'PDF文件', hint: '.pdf', icon: DocumentCopy, accept: '.pdf', acceptLabel: '.pdf 文件', bg: 'rgba(201, 164, 92, 0.14)', color: '#c9a45c' },
  { key: 'excel', label: 'Excel', hint: '.xls / .xlsx', icon: Grid, accept: '.xls,.xlsx', acceptLabel: '.xls / .xlsx 文件', bg: 'rgba(14, 138, 95, 0.12)', color: '#0e8a5f' },
  { key: 'csv', label: 'CSV', hint: '.csv', icon: List, accept: '.csv', acceptLabel: '.csv 文件', bg: 'rgba(138, 99, 210, 0.12)', color: '#8a63d2' },
]

const activeType = ref('image')
const archiveType = ref('')
const fileInput = ref(null)
const isDragOver = ref(false)
const isUploading = ref(false)
const uidCounter = ref(0)

const files = reactive([])

const provinces = ref([])
const selectedProvince = ref('')
const selectedAdmissionYear = ref('')
const selectedDegreeName = ref('')
const useLlm = ref(false)
const llmConfigured = ref(false)
const admissionYears = computed(() => {
  const current = new Date().getFullYear()
  const years = []
  for (let y = 2000; y <= current + 2; y++) {
    years.push(y)
  }
  return years
})

const currentType = computed(() => fileTypes.find((t) => t.key === activeType.value))

const acceptStr = computed(() => currentType.value.accept)

watch(archiveType, (val) => {
  if (val === 'admission' && provinces.value.length === 0) {
    fetchProvinces().then((res) => {
      provinces.value = res.data.data || []
    }).catch(() => {})
  }
  if (val !== 'admission') {
    selectedProvince.value = ''
    selectedAdmissionYear.value = ''
    selectedDegreeName.value = ''
  }
})

// 检查 LLM 配置状态
fetchLLMStatus().then(res => {
  llmConfigured.value = res.data?.data?.configured || false
  useLlm.value = llmConfigured.value
}).catch(() => {})

function switchType(key) {
  if (isUploading.value) return
  if (!archiveType.value) {
    ElMessage.warning('请先选择档案类型')
    return
  }
  activeType.value = key
  clearAll()
}

function onDropZoneClick() {
  if (!archiveType.value) {
    ElMessage.warning('请先选择档案类型')
    return
  }
  fileInput.value?.click()
}

function onDragEnter() {
  if (!archiveType.value) return
  isDragOver.value = true
}

function onFileSelect(e) {
  const selected = Array.from(e.target.files || [])
  addFiles(selected)
  e.target.value = ''
}

function onDrop(e) {
  isDragOver.value = false
  if (!archiveType.value) {
    ElMessage.warning('请先选择档案类型')
    return
  }
  const dropped = Array.from(e.dataTransfer.files || [])
  addFiles(dropped)
}

function addFiles(newFiles) {
  const acceptList = currentType.value.accept.split(',')
  for (const f of newFiles) {
    const ext = '.' + f.name.split('.').pop().toLowerCase()
    if (!acceptList.includes(ext)) continue
    if (files.some((existing) => existing.name === f.name && existing.size === f.size)) continue
    files.push({
      uid: ++uidCounter.value,
      raw: f,
      name: f.name,
      size: f.size,
      status: 'toUpload',
      progress: 0,
    })
  }
}

function removeFile(index) {
  files.splice(index, 1)
}

function clearAll() {
  files.splice(0, files.length)
}

function fileIcon(f) {
  const ext = f.name.split('.').pop().toLowerCase()
  const imgExts = ['jpg', 'jpeg', 'png', 'tif', 'tiff', 'bmp']
  if (['pdf'].includes(ext)) return DocumentCopy
  if (imgExts.includes(ext)) return Picture
  if (['xls', 'xlsx'].includes(ext)) return Grid
  if (['csv'].includes(ext)) return List
  return Document
}

function fileIconColor(f) {
  const ext = f.name.split('.').pop().toLowerCase()
  const imgExts = ['jpg', 'jpeg', 'png', 'tif', 'tiff', 'bmp']
  if (ext === 'pdf') return '#c9a45c'
  if (imgExts.includes(ext)) return '#5b8def'
  if (['xls', 'xlsx'].includes(ext)) return '#0e8a5f'
  if (ext === 'csv') return '#8a63d2'
  return '#67c23a'
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function handleUpload() {
  if (files.length === 0 || isUploading.value) return
  if (!archiveType.value) {
    ElMessage.warning('请先选择档案类型')
    return
  }
  if (archiveType.value === 'admission' && !selectedDegreeName.value) {
    ElMessage.warning('请选择培养层次（招生档案必选）')
    return
  }
  isUploading.value = true

  const toUpload = files.filter((f) => f.status !== 'done')
  if (toUpload.length === 0) { isUploading.value = false; return }
  toUpload.forEach((f) => {
    f.status = 'uploading'
    f.progress = 0
  })

  try {
    const admissionDate = selectedAdmissionYear.value
      ? selectedAdmissionYear.value + '-09-01'
      : ''
    const { data } = await uploadFiles(
      toUpload.map((f) => f.raw),
      activeType.value,
      archiveType.value,
      selectedProvince.value,
      admissionDate,
      selectedDegreeName.value,
      useLlm.value
    )

    toUpload.forEach((f) => {
      f.status = data.success ? 'done' : 'error'
      f.progress = 100
    })

    if (data.success) {
      ElMessage.success(`成功上传 ${data.uploaded.length} 个文件`)
      setTimeout(() => clearAll(), 1500)
    } else {
      ElNotification.warning({
        title: '上传完成（部分失败）',
        message: `<div style="font-size:13px">成功 ${data.uploaded.length} 个${data.errors.length ? `，失败 ${data.errors.length} 个` : ''}${data.errors.length ? '<br><br><b>失败原因：</b><br>' + data.errors.map((e, i) => `${i + 1}. ${e}`).join('<br>') : ''}</div>`,
        dangerouslyUseHTMLString: true,
        duration: 6000,
      })
      // mark each failed file with error detail
    }
  } catch (err) {
    const msg =
      err.response?.data?.message ||
      err.message ||
      '网络异常，请检查服务是否正常运行'

    toUpload.forEach((f) => {
      f.status = 'error'
    })

    ElNotification.error({
      title: '上传失败',
      message: `<div style="font-size:13px">${msg}</div>`,
      dangerouslyUseHTMLString: true,
      duration: 5000,
    })
  } finally {
    isUploading.value = false
  }
}
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

/* Archive Type Selector */
.archive-type-selector {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 14px 18px;
  background: linear-gradient(135deg, var(--card-bg), var(--bg-primary));
  border-radius: 14px;
  border: 1px solid var(--card-border);
  box-shadow: 0 4px 16px rgba(7, 39, 28, 0.05);
  transition: border-color 0.25s ease;
}
.archive-type-selector--required {
  border-color: var(--color-gold);
  background: linear-gradient(135deg, var(--card-bg), rgba(201, 164, 92, 0.06));
}

.archive-type-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
}

.archive-type-selector :deep(.el-radio) {
  margin-right: 8px;
}
.archive-type-selector :deep(.el-radio__label) {
  font-weight: 500;
}

.archive-type-hint {
  font-size: 12px;
  color: var(--color-gold);
  margin-left: 4px;
}

.archive-type-divider {
  width: 1px;
  height: 20px;
  background: var(--border-light);
  margin: 0 8px;
}
.llm-enabled {
  color: var(--color-primary) !important;
  font-weight: 600;
}

/* Type Selector */
.type-selector {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 8px 14px;
  border-radius: 14px;
  cursor: pointer;
  border: 1px solid var(--border-light);
  background: var(--bg-tertiary);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease, background 0.25s ease;
}
.type-card:hover {
  transform: translateY(-4px);
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  box-shadow: 0 12px 26px rgba(7, 39, 28, 0.12);
}
.type-card.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  box-shadow: 0 0 0 3px rgba(14, 138, 95, 0.12), 0 12px 26px rgba(7, 39, 28, 0.1);
}
.type-card.active::after {
  content: '✓';
  position: absolute;
  top: 8px;
  right: 12px;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-primary);
}

.type-card--disabled {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

.type-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s cubic-bezier(0.3, 1.4, 0.5, 1);
}
.type-card:hover .type-card-icon {
  transform: scale(1.1);
}

.type-card-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.type-card-hint {
  font-size: 11px;
  color: var(--text-tertiary);
}

/* Drop Zone */
.drop-zone {
  position: relative;
  border: 2px dashed var(--border-color);
  border-radius: 16px;
  padding: 46px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  background: linear-gradient(160deg, var(--card-bg), var(--bg-primary));
  transition: border-color 0.25s ease, background 0.25s ease, box-shadow 0.25s ease;
}
.drop-zone:hover,
.drop-zone--dragover {
  border-color: var(--color-gold);
  background: linear-gradient(160deg, var(--card-bg), rgba(201, 164, 92, 0.05));
  box-shadow: inset 0 0 0 3px rgba(201, 164, 92, 0.1);
}
.drop-zone--has-files {
  padding: 26px 20px;
}
.drop-zone--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.drop-zone-icon {
  pointer-events: none;
  animation: floaty 3.2s ease-in-out infinite;
}
.drop-zone-icon--has {
  animation: none;
}
@keyframes floaty {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-7px); }
}

.drop-zone-text {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
  pointer-events: none;
}

.drop-zone-text em {
  font-style: normal;
  color: var(--color-primary);
  font-weight: 600;
}

.drop-zone-hint {
  margin: 0;
  font-size: 12px;
  color: var(--text-tertiary);
  pointer-events: none;
}

.file-input-hidden {
  display: none;
}

/* File List */
.file-list {
  border: 1px solid var(--border-light);
  border-radius: 14px;
  overflow: hidden;
  margin-top: 4px;
}

.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-tertiary);
}

.file-list-body {
  max-height: 260px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-light);
  transition: background 0.15s;
}
.file-item:hover {
  background: var(--bg-tertiary);
}
.file-item:last-child {
  border-bottom: none;
}

.file-item--error {
  background: rgba(245, 108, 108, 0.04);
}
.file-item--error:hover { background: rgba(245, 108, 108, 0.08); }
.file-item--done {
  background: rgba(103, 194, 58, 0.04);
}
.file-item--done:hover { background: rgba(103, 194, 58, 0.08); }

.file-item-icon {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--card-bg);
  border: 1px solid var(--border-light);
  border-radius: 9px;
  box-shadow: 0 3px 10px rgba(7, 39, 28, 0.06);
}

.file-item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-item-name {
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-item-size {
  font-size: 12px;
  color: var(--text-tertiary);
}

.file-item-status {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.file-item-status .el-progress__text {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 10px !important;
  line-height: 1;
}

/* Actions */
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
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

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .drop-zone-icon { animation: none !important; }
}
</style>
