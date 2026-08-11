<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 系统管理</div>
        <h2 class="font-display">专业管理</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 学位帽 -->
          <g transform="translate(40 26)">
            <path d="M0 0 L34 -14 L68 0 L34 14 Z" fill="#123d2c" stroke="#d9b877" stroke-width="1.6"/>
            <path d="M68 0 L68 22" stroke="#d9b877" stroke-width="2.2" stroke-linecap="round"/>
            <circle cx="68" cy="27" r="3.6" fill="#2fb984"/>
            <path d="M-6 4 L-6 18" stroke="rgba(230,205,149,.5)" stroke-width="1.6" stroke-linecap="round"/>
          </g>
          <!-- 书本 -->
          <g transform="translate(104 30)">
            <rect x="0" y="0" width="26" height="34" rx="4" fill="#0a2e21" stroke="rgba(47,185,132,.5)" stroke-width="1.4"/>
            <path d="M26 0 L26 34" stroke="#d9b877" stroke-width="1.6"/>
            <line x1="6" y1="10" x2="20" y2="10" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <line x1="6" y1="17" x2="20" y2="17" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
            <line x1="6" y1="24" x2="15" y2="24" stroke="rgba(230,205,149,.45)" stroke-width="2" stroke-linecap="round"/>
          </g>
          <!-- 层级 -->
          <g transform="translate(150 34)">
            <rect x="0" y="0" width="46" height="12" rx="6" fill="#0a2e21" stroke="rgba(201,164,92,.4)" stroke-width="1.2"/>
            <text x="23" y="9" text-anchor="middle" font-size="8" fill="#d9b877" font-family="inherit">学院</text>
          </g>
          <path d="M168 52 C 172 60, 176 62, 182 62 L 182 58" fill="none" stroke="#2fb984" stroke-width="1.6" stroke-linecap="round"/>
          <g transform="translate(182 56)">
            <rect x="0" y="0" width="30" height="12" rx="6" fill="#0a2e21" stroke="rgba(47,185,132,.5)" stroke-width="1.2"/>
            <text x="15" y="9" text-anchor="middle" font-size="8" fill="#2fb984" font-family="inherit">专业</text>
          </g>
          <circle cx="88" cy="68" r="2" fill="rgba(230,205,149,.5)"/>
          <circle cx="202" cy="22" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Collection /></el-icon>
            <span>专业管理</span>
          </div>
          <div class="card-header-right">
            <el-input v-model="searchText" placeholder="搜索专业名称/代码" clearable style="width:220px" @keyup.enter="fetchList" @clear="fetchList">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button :icon="Search" @click="fetchList">查询</el-button>
            <el-button type="primary" class="btn-gold" :icon="Plus" @click="openAddDialog">新增专业</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="majorId" label="ID" width="70" align="center" />
        <el-table-column prop="majorName" label="专业名称" min-width="160">
          <template #default="{ row }">
            <span class="major-name"><span class="name-dot"></span>{{ row.majorName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="majorCode" label="专业代码" width="120">
          <template #default="{ row }">
            <span class="code-pill">{{ row.majorCode || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="collegeName" label="所属学院" min-width="140">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.collegeName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="degreeName" label="培养层次" width="110" align="center">
          <template #default="{ row }">
            <span class="degree-pill">{{ row.degreeName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" class="op-btn" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" class="op-btn op-danger" :icon="Delete" @click="handleDelete(row.majorId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑专业' : '新增专业'" width="460px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="form.majorName" placeholder="请输入专业名称" />
        </el-form-item>
        <el-form-item label="专业代码" prop="majorCode">
          <el-input v-model="form.majorCode" placeholder="如 CS001" />
        </el-form-item>
        <el-form-item label="所属学院" prop="collegeId">
          <el-select v-model="form.collegeId" placeholder="请选择学院" style="width:100%" filterable>
            <el-option v-for="c in colleges" :key="c.collegeId" :label="c.collegeName" :value="c.collegeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="培养层次" prop="degreeId">
          <el-select v-model="form.degreeId" placeholder="请选择" style="width:100%" clearable filterable>
            <el-option v-for="d in degrees" :key="d.degreeId" :label="d.degreeName" :value="d.degreeId" />
          </el-select>
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
import { Plus, Edit, Delete, Search, Collection } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchMajors, addMajor, updateMajor, deleteMajor, fetchColleges } from '@/api/modules/dim'
import { fetchDegrees } from '@/api/modules/admission'

const tableData = ref([])
const loading = ref(false)
const searchText = ref('')
const colleges = ref([])
const degrees = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = { majorId: null, majorName: '', majorCode: '', collegeId: null, degreeId: null }
const form = ref({ ...defaultForm })

const rules = {
  majorName: [{ required: true, message: '请输入专业名称', trigger: 'blur' }],
  collegeId: [{ required: true, message: '请选择所属学院', trigger: 'change' }],
}

async function fetchList() {
  loading.value = true
  try {
    const params = {}
    if (searchText.value.trim()) params.keyword = searchText.value.trim()
    const res = await fetchMajors(params)
    tableData.value = res.data.data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function fetchCollegeList() {
  try {
    const res = await fetchColleges()
    colleges.value = res.data.data || []
  } catch {
    colleges.value = []
  }
}

async function fetchDegreeList() {
  try {
    const res = await fetchDegrees()
    degrees.value = res.data.data || []
  } catch {
    degrees.value = []
  }
}

function openAddDialog() {
  isEdit.value = false
  form.value = { ...defaultForm }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  form.value = {
    majorId: row.majorId,
    majorName: row.majorName || '',
    majorCode: row.majorCode || '',
    collegeId: row.collegeId ?? null,
    degreeId: row.degreeId ?? null,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMajor(form.value)
      ElMessage.success('更新成功')
    } else {
      await addMajor(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || (isEdit.value ? '更新失败' : '添加失败'))
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该专业吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteMajor(id)
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  fetchList()
  fetchCollegeList()
  fetchDegreeList()
})

onActivated(() => {
  fetchList()
  fetchCollegeList()
  fetchDegreeList()
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

.major-name {
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
.degree-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  background: rgba(201, 164, 92, 0.12);
  color: var(--color-gold-dark);
  border: 1px solid rgba(201, 164, 92, 0.3);
}
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

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .table-card { animation: none !important; }
}
</style>
