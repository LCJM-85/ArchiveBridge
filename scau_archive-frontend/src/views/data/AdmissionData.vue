<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 数据管理</div>
        <h2 class="font-display">招生数据管理</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <div class="ph-count">
          <span class="ph-count-num">{{ total }}</span>
          <span class="ph-count-label">条记录</span>
        </div>
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <path d="M6 66 C 40 58, 66 44, 100 40 S 160 22, 214 12" fill="none" stroke="#d9b877" stroke-width="2.4" stroke-linecap="round"/>
          <g fill="#2fb984"><circle cx="40" cy="58" r="3"/><circle cx="100" cy="40" r="3"/><circle cx="160" cy="22" r="3"/></g>
          <circle cx="214" cy="12" r="4" fill="#d9b877"/>
          <rect x="10" y="70" width="34" height="14" rx="4" fill="#0a2e21" stroke="rgba(230,205,149,.3)" stroke-width="1.2"/>
          <rect x="14" y="75" width="26" height="3" rx="1.5" fill="#c9a45c" opacity=".8"/>
          <circle cx="52" cy="18" r="2" fill="rgba(230,205,149,.5)"/>
          <circle cx="182" cy="62" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <!-- 筛选工具栏 -->
    <div class="filter-bar">
      <el-select v-model="degreeFilter" placeholder="培养层次" clearable style="width:132px" @change="handleSearch">
        <el-option v-for="d in degrees" :key="d.degreeId" :label="levelMap[d.degreeName] || d.degreeName" :value="d.degreeId" />
      </el-select>
      <el-input v-model="searchText" placeholder="搜索所有字段" clearable style="width:170px" @keyup.enter="handleSearch" @clear="handleClear">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-date-picker
        v-model="createTimeRange"
        type="daterange"
        range-separator="至"
        start-placeholder="录入开始"
        end-placeholder="录入结束"
        value-format="YYYY-MM-DD"
        style="width:210px"
      />
      <el-date-picker
        v-model="updateTimeRange"
        type="daterange"
        range-separator="至"
        start-placeholder="更新开始"
        end-placeholder="更新结束"
        value-format="YYYY-MM-DD"
        style="width:210px"
      />
      <div class="filter-divider"></div>
      <el-button :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
      <el-button @click="resetFilters">重置</el-button>
      <el-button type="primary" class="btn-gold" :icon="Plus" @click="openAddDialog">新增</el-button>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="70" align="center">
          <template #default="{ row }">
            <span class="gender-tag" :class="row.gender === '男' ? 'gender-male' : 'gender-female'">{{ row.gender }}</span>
          </template>
        </el-table-column>
        <el-table-column label="培养层次" width="105" align="center">
          <template #default="{ row }">
            <span class="degree-pill">{{ levelMap[row.degreeName] || row.degreeName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="idCard" label="身份证号" width="200" />
        <el-table-column prop="examNo" label="考生号" width="150" />
        <el-table-column prop="provinceName" label="生源省份" width="140" />
        <el-table-column prop="majorName" label="录取专业" width="180" />
        <el-table-column prop="admissionDate" label="录取日期" width="110" align="center" />
        <el-table-column prop="admissionScore" label="录取分数" width="100" align="center">
          <template #default="{ row }">
            <span class="score-num">{{ row.admissionScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源文件" width="160">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.fileName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="录入时间" width="175" align="center">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.createTime ? row.createTime.replace('T', ' ').split('.')[0] : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="175" align="center">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.updateTime ? row.updateTime.replace('T', ' ').split('.')[0] : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" class="op-btn" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" class="op-btn op-danger" :icon="Delete" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[15, 30, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑招生记录' : '新增招生记录'" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="form.studentNo" placeholder="请输入学号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择" style="width:100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="培养层次" prop="degreeId">
              <el-select v-model="form.degreeId" placeholder="请选择" clearable filterable style="width:100%">
                <el-option v-for="d in degrees" :key="d.degreeId" :label="d.degreeName" :value="d.degreeId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考生号" prop="examNo">
              <el-input v-model="form.examNo" placeholder="请输入考生号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="录取日期" prop="admissionDate">
              <el-date-picker v-model="form.admissionDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生源省份" prop="provinceId">
              <el-select v-model="form.provinceId" placeholder="请选择" style="width:100%" filterable clearable>
                <el-option v-for="p in provinces" :key="p.provinceId" :label="p.provinceName" :value="p.provinceId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="录取专业" prop="majorId">
              <el-select v-model="form.majorId" placeholder="请选择" style="width:100%" filterable clearable>
                <el-option v-for="m in majors" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="录取分数" prop="admissionScore">
              <el-input-number v-model="form.admissionScore" :min="0" :max="750" placeholder="0-750" style="width:100%" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAdmissionPage, addAdmission, updateAdmission, deleteAdmission, fetchProvinces, fetchMajors, fetchDegrees } from '@/api/modules/admission'

const tableData = ref([])
const loading = ref(false)
const current = ref(1)
const pageSize = ref(15)
const total = ref(0)
const pages = ref(0)
const keyword = ref('')
const degreeFilter = ref(null)
const createTimeRange = ref([])
const updateTimeRange = ref([])
const levelMap = { 学士: '本科生', 硕士: '硕士研究生', 博士: '博士研究生' }

const provinces = ref([])
const degrees = ref([])
const majors = ref([])

async function fetchPage() {
  loading.value = true
  try {
    const params = { current: current.value, size: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    if (degreeFilter.value) params.degreeId = degreeFilter.value
    if (createTimeRange.value && createTimeRange.value.length === 2) {
      params.createTimeStart = createTimeRange.value[0]
      params.createTimeEnd = createTimeRange.value[1]
    }
    if (updateTimeRange.value && updateTimeRange.value.length === 2) {
      params.updateTimeStart = updateTimeRange.value[0]
      params.updateTimeEnd = updateTimeRange.value[1]
    }
    const res = await fetchAdmissionPage(params)
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

async function fetchProvincesList() {
  try {
    const res = await fetchProvinces()
    provinces.value = res.data.data || []
  } catch { provinces.value = [] }
}

async function fetchDegreesList() {
  try {
    const res = await fetchDegrees()
    degrees.value = res.data.data || []
  } catch { degrees.value = [] }
}

async function fetchMajorsList() {
  try {
    const res = await fetchMajors()
    majors.value = res.data.data || []
  } catch { majors.value = [] }
}

async function add(data) {
  const res = await addAdmission(data)
  return res.data
}

async function update(data) {
  const res = await updateAdmission(data)
  return res.data
}

async function remove(id) {
  const res = await deleteAdmission(id)
  return res.data
}

function setPage(p) { current.value = p }

function clearTimeRanges() {
  createTimeRange.value = []
  updateTimeRange.value = []
}

const searchText = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = {
  studentNo: '',
  name: '',
  gender: '',
  degreeId: null,
  idCard: '',
  examNo: '',
  admissionDate: '',
  provinceId: null,
  majorId: null,
  admissionScore: null,
}
const form = ref({ ...defaultForm })

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

function handleSearch() {
  keyword.value = searchText.value
  current.value = 1
  fetchPage()
}

function handleClear() {
  searchText.value = ''
  keyword.value = ''
  current.value = 1
  fetchPage()
}

function resetFilters() {
  searchText.value = ''
  keyword.value = ''
  degreeFilter.value = null
  clearTimeRanges()
  current.value = 1
  fetchPage()
}

function openAddDialog() {
  isEdit.value = false
  form.value = { ...defaultForm }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  form.value = {
    id: row.id,
    studentNo: row.studentNo,
    name: row.name,
    gender: row.gender || '',
    degreeId: row.degreeId ?? null,
    idCard: row.idCard || '',
    examNo: row.examNo || '',
    admissionDate: row.admissionDate || '',
    provinceId: row.provinceId ?? null,
    majorId: row.majorId ?? null,
    admissionScore: row.admissionScore ?? null,
  }
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

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该招生记录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await remove(id)
    ElMessage.success('删除成功')
    await fetchPage()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

function handlePageChange(p) {
  setPage(p)
  fetchPage()
}

function handleSizeChange(size) {
  pageSize.value = size
  current.value = 1
  fetchPage()
}

onMounted(() => {
  fetchPage()
  fetchProvincesList()
  fetchDegreesList()
  fetchMajorsList()
})

onActivated(() => {
  fetchPage()
  fetchProvincesList()
  fetchDegreesList()
  fetchMajorsList()
})
</script>

<style scoped>
:deep(.el-table .cell) {
  white-space: nowrap;
}

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

.ph-right {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 26px;
}
.ph-count { text-align: right; }
.ph-count-num {
  display: block;
  font-size: 34px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-gold-light);
  line-height: 1.1;
  text-shadow: 0 4px 18px rgba(0, 0, 0, 0.3);
}
.ph-count-label { font-size: 12px; color: rgba(255, 255, 255, 0.6); letter-spacing: 1px; }
.ph-art { width: 210px; height: 84px; flex-shrink: 0; }
@media (max-width: 900px) {
  .ph-art { display: none; }
  .page-hero { padding: 14px 20px; }
}

/* ===== 筛选工具栏 ===== */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-radius: 14px;
  border: 1px solid var(--card-border);
  background: linear-gradient(135deg, var(--card-bg), var(--bg-primary));
  box-shadow: 0 4px 16px rgba(7, 39, 28, 0.05);
  animation: rise-up 0.7s 0.08s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.filter-divider {
  width: 1px;
  height: 24px;
  margin: 0 4px;
  background: var(--border-color);
}
.filter-bar :deep(.el-input__wrapper),
.filter-bar :deep(.el-select__wrapper) {
  border-radius: 9px !important;
}
.filter-bar :deep(.el-date-editor .el-input__wrapper) {
  border-radius: 9px !important;
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

/* ===== 数据表格 ===== */
.table-card {
  border-radius: 16px !important;
  overflow: hidden;
  animation: rise-up 0.7s 0.16s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.table-card :deep(.el-table) {
  --el-table-header-text-color: var(--text-secondary);
}
.table-card :deep(.el-table th.el-table__cell) {
  background: linear-gradient(180deg, var(--bg-tertiary), var(--bg-primary)) !important;
  font-weight: 600 !important;
  letter-spacing: 0.5px;
}
.table-card :deep(.el-table__row:hover > td.el-table__cell) {
  background: var(--color-primary-light) !important;
}
.table-card :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: var(--bg-tertiary);
}

.gender-tag {
  display: inline-block;
  min-width: 40px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
}
.gender-male { color: #0e8a5f; background: var(--color-primary-light); }
.gender-female { color: #d6458d; background: rgba(214, 69, 141, 0.1); }

.degree-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  background: rgba(201, 164, 92, 0.12);
  color: var(--color-gold-dark);
  border: 1px solid rgba(201, 164, 92, 0.3);
}
.score-num { font-weight: 600; color: var(--color-primary); font-variant-numeric: tabular-nums; }
.cell-muted { font-size: 12px; color: var(--text-secondary); }

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

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .filter-bar, .table-card { animation: none !important; }
}
</style>
