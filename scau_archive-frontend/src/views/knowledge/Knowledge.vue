<template>
  <div class="knowledge-page">
    <el-card shadow="never">
      <template #header>
        <span>知识库管理</span>
      </template>

      <el-tabs v-model="activeTab">
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
            <el-icon :size="48" style="color:var(--el-color-primary)"><UploadFilled /></el-icon>
            <p class="drop-text">拖拽文件到此处，或 <em>点击选择</em></p>
            <p class="drop-tip">支持 PDF、DOCX、XLSX、TXT 格式</p>
          </div>

          <div v-if="selectedFiles.length" class="file-list">
            <div v-for="(f, i) in selectedFiles" :key="i" class="file-item">
              <span>{{ f.name }}</span>
              <el-button text size="small" type="danger" @click="selectedFiles.splice(i, 1)">移除</el-button>
            </div>
          </div>

          <div style="margin-top: 12px;">
            <el-button
              type="primary"
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
            @click="handleAddUrl"
            :loading="addingUrl"
            style="margin-top: 12px;"
          >
            添加
          </el-button>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px;">
      <template #header>
        <span>文档列表</span>
      </template>

      <el-table :data="docList" v-loading="loadingList" style="width: 100%;">
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="file_type" label="类型" width="80" />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column prop="chunk_count" label="分块数" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="error_msg" label="失败原因" min-width="200">
          <template #default="{ row }">
            <span style="color:var(--el-color-danger);font-size:13px;">{{ row.error_msg || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.create_time) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
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
import { ref, onMounted } from 'vue'
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
</script>

<style scoped>
.knowledge-page { padding: 16px; }
.drop-zone {
  border: 2px dashed var(--el-border-color); border-radius: 8px; padding: 40px 24px;
  text-align: center; cursor: pointer; transition: all .2s;
}
.drop-zone:hover, .drag-over { border-color: var(--el-color-primary); background: rgba(64,158,255,0.04); }
.file-input-hidden { display: none; }
.drop-text { margin: 12px 0 4px; font-size: 14px; color: var(--el-text-color-regular); }
.drop-text em { color: var(--el-color-primary); font-style: normal; }
.drop-tip { font-size: 12px; color: var(--el-text-color-secondary); margin: 0; }
.file-list { margin-top: 12px; }
.file-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-bottom: 1px solid var(--el-border-color-light); font-size: 14px; }
</style>
