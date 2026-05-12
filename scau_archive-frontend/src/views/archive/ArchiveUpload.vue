<template>
  <div class="page-wrapper">
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
          <span class="archive-type-label">省份：</span>
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
          <span class="archive-type-label">录取年份：</span>
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
      </div>

      <!-- File Type Selector -->
      <div class="type-selector">
        <div
          v-for="t in fileTypes"
          :key="t.key"
          class="type-card"
          :class="{ active: activeType === t.key, 'type-card--disabled': !archiveType }"
          @click="switchType(t.key)"
        >
          <div class="type-card-icon" :style="{ background: t.bg }">
            <el-icon :size="20" :color="t.color">
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
        <el-icon :size="36" class="drop-zone-icon" :color="files.length > 0 ? 'var(--color-primary)' : '#bbb'">
          <UploadFilled />
        </el-icon>
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
            <div class="file-item-icon">
              <el-icon :size="18" :color="fileIconColor(f)">
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { defineOptions } from 'vue'

defineOptions({ name: 'ArchiveUpload' })
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import { uploadFiles } from '@/api/modules/archive'
import { fetchProvinces } from '@/api/modules/admission'
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
} from '@element-plus/icons-vue'

const router = useRouter()

const fileTypes = [
  { key: 'image', label: '图片文件', hint: '.jpg / .png / .tiff', icon: Picture, accept: '.jpg,.jpeg,.png,.tif,.tiff,.bmp', acceptLabel: '.jpg / .png / .tiff 等图片文件', bg: 'rgba(64, 158, 255, 0.1)', color: '#409eff' },
  { key: 'pdf', label: 'PDF文件', hint: '.pdf', icon: DocumentCopy, accept: '.pdf', acceptLabel: '.pdf 文件', bg: 'rgba(230, 162, 60, 0.1)', color: '#e6a23c' },
  { key: 'excel', label: 'Excel', hint: '.xls / .xlsx', icon: Grid, accept: '.xls,.xlsx', acceptLabel: '.xls / .xlsx 文件', bg: 'rgba(26, 122, 78, 0.1)', color: '#1a7a4e' },
  { key: 'csv', label: 'CSV', hint: '.csv', icon: List, accept: '.csv', acceptLabel: '.csv 文件', bg: 'rgba(64, 158, 255, 0.1)', color: '#7232dd' },
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
  }
})

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
  if (ext === 'pdf') return '#e6a23c'
  if (imgExts.includes(ext)) return '#409eff'
  if (['xls', 'xlsx'].includes(ext)) return '#1a7a4e'
  if (ext === 'csv') return '#7232dd'
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
      admissionDate
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
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: var(--bg-tertiary);
  border-radius: 8px;
  border: 1px solid var(--border-light);
}

.archive-type-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
}

.archive-type-selector--required {
  border-color: #e6a23c;
  background: rgba(230, 162, 60, 0.04);
}

.archive-type-hint {
  font-size: 12px;
  color: #e6a23c;
  margin-left: 4px;
}

.archive-type-divider {
  width: 1px;
  height: 20px;
  background: var(--border-light);
  margin: 0 12px;
}

/* Type Selector */
.type-selector {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 4px;
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 8px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid var(--border-light);
  background: var(--bg-tertiary);
  transition: all 0.2s;
}

.type-card:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.type-card.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.type-card--disabled {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

.type-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
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
  border: 2px dashed var(--border-light);
  border-radius: 12px;
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.drop-zone:hover,
.drop-zone--dragover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.drop-zone--has-files {
  padding: 24px 20px;
}

.drop-zone--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.drop-zone-icon {
  pointer-events: none;
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
  font-weight: 500;
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
  border-radius: 10px;
  overflow: hidden;
}

.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-light);
}

.file-list-body {
  max-height: 260px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-light);
  transition: background 0.15s;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item--error {
  background: rgba(245, 108, 108, 0.04);
}

.file-item--done {
  background: rgba(103, 194, 58, 0.04);
}

.file-item-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border-radius: 6px;
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
}
</style>
