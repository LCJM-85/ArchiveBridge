<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 系统管理</div>
        <h2 class="font-display">元数据标准管理</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 数据库 -->
          <g transform="translate(20 22)">
            <ellipse cx="30" cy="8" rx="28" ry="8" fill="#0a2e21" stroke="rgba(47,185,132,.5)" stroke-width="1.4"/>
            <path d="M2 8 L2 42 C 2 46, 14 50, 30 50 C 46 50, 58 46, 58 42 L58 8" fill="none" stroke="rgba(47,185,132,.5)" stroke-width="1.4"/>
            <path d="M2 25 C 2 29, 14 33, 30 33 C 46 33, 58 29, 58 25" fill="none" stroke="rgba(47,185,132,.35)" stroke-width="1.2"/>
            <circle cx="30" cy="8" r="3" fill="#d9b877"/>
          </g>
          <!-- 字段 -->
          <g transform="translate(90 30)">
            <rect x="0" y="0" width="70" height="34" rx="8" fill="#123d2c" stroke="rgba(201,164,92,.4)" stroke-width="1.3"/>
            <rect x="8" y="8" width="8" height="5" rx="1.5" fill="#2fb984"/>
            <rect x="8" y="20" width="8" height="5" rx="1.5" fill="#c9a45c"/>
            <rect x="22" y="8" width="40" height="5" rx="2.5" fill="rgba(230,205,149,.5)"/>
            <rect x="22" y="20" width="30" height="5" rx="2.5" fill="rgba(230,205,149,.35)"/>
          </g>
          <!-- 映射箭头 -->
          <path d="M168 34 C 178 30, 186 26, 202 22" fill="none" stroke="#d9b877" stroke-width="2" stroke-linecap="round"/>
          <path d="M196 17 L203 22 L195 27" fill="none" stroke="#d9b877" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="182" cy="56" r="2" fill="rgba(47,185,132,.55)"/>
          <circle cx="56" cy="70" r="2" fill="rgba(230,205,149,.5)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Management /></el-icon>
            <span>元数据标准管理</span>
          </div>
          <div class="card-header-right">
            <el-input v-model="searchText" placeholder="字段编码 / 字段名称 / 来源字段" clearable style="width:280px" @keyup.enter="handleSearch" @clear="handleClear">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
            <el-button type="primary" class="btn-gold" :icon="Plus" @click="openAddDialog">新增</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="fieldCode" label="字段编码" width="160">
          <template #default="{ row }">
            <span class="code-pill">{{ row.fieldCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="fieldName" label="字段名称" width="160">
          <template #default="{ row }">
            <span class="field-name"><span class="name-dot"></span>{{ row.fieldName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="fieldType" label="字段类型" width="100" align="center">
          <template #default="{ row }">
            <span class="type-pill">{{ row.fieldType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceField" label="来源字段" width="140" />
        <el-table-column prop="transformType" label="转换类型" width="110" align="center">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.transformType || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="transformRule" label="转换规则" min-width="160" show-overflow-tooltip />
        <el-table-column prop="isRequired" label="是否必填" width="90" align="center">
          <template #default="{ row }">
            <span class="req-pill" :class="row.isRequired ? 'req-yes' : 'req-no'">
              {{ row.isRequired ? '是' : '否' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" class="op-btn" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" class="op-btn op-danger" :icon="Delete" @click="handleDelete(row.metadataId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!keyword" class="pagination-wrap">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑元数据' : '新增元数据'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字段编码" prop="fieldCode">
          <el-input v-model="form.fieldCode" :disabled="isEdit" placeholder="请输入字段编码" />
        </el-form-item>
        <el-form-item label="字段名称" prop="fieldName">
          <el-input v-model="form.fieldName" placeholder="请输入字段名称" />
        </el-form-item>
        <el-form-item label="字段类型" prop="fieldType">
          <el-select v-model="form.fieldType" placeholder="请选择" style="width:100%">
            <el-option label="字符串" value="varchar" />
            <el-option label="整数" value="int" />
            <el-option label="浮点数" value="decimal" />
            <el-option label="日期" value="date" />
            <el-option label="布尔值" value="boolean" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源字段" prop="sourceField">
          <el-input v-model="form.sourceField" placeholder="请输入来源字段" />
        </el-form-item>
        <el-form-item label="转换类型" prop="transformType">
          <el-select v-model="form.transformType" placeholder="请选择" style="width:100%" clearable>
            <el-option label="直接映射" value="direct" />
            <el-option label="类型转换" value="type_cast" />
            <el-option label="格式转换" value="format" />
            <el-option label="字典映射" value="dict_map" />
          </el-select>
        </el-form-item>
        <el-form-item label="转换规则" prop="transformRule">
          <el-input v-model="form.transformRule" placeholder="请输入转换规则" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="是否必填" prop="isRequired">
          <el-switch v-model="form.isRequired" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="btn-gold" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { Plus, Edit, Delete, Search, Refresh, Management } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchMetaDataPage, addMetaData, updateMetaData, deleteMetaData } from '@/api/modules/metadata'

const tableData = ref([])
const loading = ref(false)
const current = ref(1)
const pageSize = ref(15)
const total = ref(0)
const pages = ref(0)
const keyword = ref('')

async function fetchPage() {
  loading.value = true
  try {
    const params = { current: current.value, size: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    const res = await fetchMetaDataPage(params)
    const d = res.data.data || {}
    tableData.value = d.records || []
    total.value = d.total || 0
    current.value = d.current || 1
    pageSize.value = d.size || 15
    pages.value = d.pages || 0
  } finally {
    loading.value = false
  }
}

async function add(data) {
  const res = await addMetaData(data)
  return res.data
}

async function update(data) {
  const res = await updateMetaData(data)
  return res.data
}

async function remove(metadataId) {
  const res = await deleteMetaData(metadataId)
  return res.data
}

function setPage(p) { current.value = p }

function search(val) {
  keyword.value = val
  current.value = 1
  fetchPage()
}

const searchText = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = {
  fieldCode: '',
  fieldName: '',
  fieldType: '',
  sourceField: '',
  transformType: '',
  transformRule: '',
  isRequired: false,
}
const form = ref({ ...defaultForm })

const rules = {
  fieldCode: [{ required: true, message: '请输入字段编码', trigger: 'blur' }],
  fieldName: [{ required: true, message: '请输入字段名称', trigger: 'blur' }],
  fieldType: [{ required: true, message: '请选择字段类型', trigger: 'change' }],
}

function handleSearch() {
  search(searchText.value)
}

function handleClear() {
  searchText.value = ''
  search('')
}

function openAddDialog() {
  isEdit.value = false
  form.value = { ...defaultForm }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await update(form.value)
      ElMessage.success('更新成功')
    } else {
      await add(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await fetchPage()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(metadataId) {
  try {
    await ElMessageBox.confirm('确定删除该元数据吗？', '提示', { type: 'warning' })
    await remove(metadataId)
    ElMessage.success('删除成功')
    await fetchPage()
  } catch {
    // cancelled or error
  }
}

function handlePageChange(p) {
  setPage(p)
  fetchPage()
}

onMounted(() => fetchPage())

onActivated(() => fetchPage())
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

/* ===== 表格 ===== */
.table-card {
  border-radius: 16px !important;
  overflow: hidden;
  animation: rise-up 0.7s 0.12s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.card-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.card-header-left { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.card-header-right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.table-card :deep(.el-table th.el-table__cell) {
  background: linear-gradient(180deg, var(--bg-tertiary), var(--bg-primary)) !important;
  font-weight: 600 !important;
}
.table-card :deep(.el-table__row:hover > td.el-table__cell) {
  background: var(--color-primary-light) !important;
}

.code-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  border: 1px solid var(--border-light);
  font-variant-numeric: tabular-nums;
}
.field-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}
.name-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #14a06f, #0b5c40);
  box-shadow: 0 0 8px rgba(14, 138, 95, 0.5);
  flex-shrink: 0;
}
.type-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  background: rgba(201, 164, 92, 0.12);
  color: var(--color-gold-dark);
  border: 1px solid rgba(201, 164, 92, 0.3);
}
.req-pill {
  display: inline-block;
  min-width: 34px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  text-align: center;
}
.req-yes { color: #e64545; background: rgba(245, 108, 108, 0.12); border: 1px solid rgba(245, 108, 108, 0.35); }
.req-no { color: var(--text-secondary); background: var(--bg-tertiary); border: 1px solid var(--border-light); }
.cell-muted { font-size: 13px; color: var(--text-secondary); }

.op-btn {
  border-radius: 8px !important;
  border: 1px solid var(--border-color) !important;
  color: var(--text-secondary) !important;
  transition: all 0.2s ease !important;
}
.op-btn:hover {
  color: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  background: var(--color-primary-light) !important;
}
.op-danger:hover {
  color: var(--color-danger) !important;
  border-color: var(--color-danger) !important;
  background: rgba(245, 108, 108, 0.1) !important;
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--border-light);
}

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .table-card { animation: none !important; }
}
</style>
