<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 系统管理</div>
        <h2 class="font-display">班级管理</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 教室黑板 -->
          <g transform="translate(20 14)">
            <rect x="0" y="0" width="70" height="42" rx="6" fill="#0a2e21" stroke="rgba(230,205,149,.45)" stroke-width="1.4"/>
            <line x1="35" y1="0" x2="35" y2="42" stroke="rgba(230,205,149,.2)" stroke-width="1"/>
            <text x="35" y="26" text-anchor="middle" font-size="11" fill="#d9b877" font-family="inherit">1 班</text>
            <rect x="0" y="46" width="70" height="5" rx="2.5" fill="#123d2c"/>
          </g>
          <!-- 课桌 -->
          <g transform="translate(108 56)">
            <rect x="0" y="0" width="26" height="12" rx="3" fill="#123d2c" stroke="rgba(47,185,132,.4)" stroke-width="1.2"/>
            <rect x="3" y="12" width="3" height="10" fill="#0a2e21"/>
            <rect x="20" y="12" width="3" height="10" fill="#0a2e21"/>
          </g>
          <g transform="translate(148 60)">
            <rect x="0" y="0" width="22" height="10" rx="3" fill="#123d2c" stroke="rgba(47,185,132,.35)" stroke-width="1.1"/>
            <rect x="3" y="10" width="3" height="8" fill="#0a2e21"/>
            <rect x="16" y="10" width="3" height="8" fill="#0a2e21"/>
          </g>
          <g transform="translate(182 56)">
            <rect x="0" y="0" width="26" height="12" rx="3" fill="#123d2c" stroke="rgba(201,164,92,.4)" stroke-width="1.2"/>
            <rect x="3" y="12" width="3" height="10" fill="#0a2e21"/>
            <rect x="20" y="12" width="3" height="10" fill="#0a2e21"/>
          </g>
          <!-- 人 -->
          <g transform="translate(140 26)">
            <circle cx="0" cy="-4" r="5" fill="#14a06f"/>
            <path d="M-8 16 C -8 6, 8 6, 8 16 Z" fill="#14a06f"/>
          </g>
          <circle cx="90" cy="18" r="2" fill="rgba(230,205,149,.5)"/>
          <circle cx="206" cy="30" r="2" fill="rgba(47,185,132,.55)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Files /></el-icon>
            <span>班级管理</span>
          </div>
          <div class="card-header-right">
            <el-input v-model="searchText" placeholder="搜索班级/年级" clearable style="width:220px" @keyup.enter="fetchList" @clear="fetchList">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button :icon="Search" @click="fetchList">查询</el-button>
            <el-button type="primary" class="btn-gold" :icon="Plus" @click="openAddDialog">新增班级</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="classId" label="ID" width="70" align="center" />
        <el-table-column prop="className" label="班级名称" min-width="140">
          <template #default="{ row }">
            <span class="class-name"><span class="name-dot"></span>{{ row.className }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="grade" label="年级" width="90" align="center">
          <template #default="{ row }">
            <span class="grade-pill">{{ row.grade || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="studyLength" label="学制(年)" width="90" align="center">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.studyLength ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="majorName" label="所属专业" min-width="140">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.majorName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" class="op-btn" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" class="op-btn op-danger" :icon="Delete" @click="handleDelete(row.classId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '新增班级'" width="460px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属专业" prop="majorId">
          <el-select v-model="form.majorId" placeholder="请选择专业" style="width:100%" filterable>
            <el-option v-for="m in majors" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="如 2026级软件1班" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="form.grade" placeholder="如 2026" />
        </el-form-item>
        <el-form-item label="学制" prop="studyLength">
          <el-input-number v-model="form.studyLength" :min="1" :max="6" style="width:100%" controls-position="right" />
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
import { Plus, Edit, Delete, Search, Files } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchClasses, addClass, updateClass, deleteClass, fetchMajors } from '@/api/modules/dim'

const tableData = ref([])
const loading = ref(false)
const searchText = ref('')
const majors = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = { classId: null, majorId: null, className: '', grade: '', studyLength: 4 }
const form = ref({ ...defaultForm })

const rules = {
  majorId: [{ required: true, message: '请选择所属专业', trigger: 'change' }],
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
}

async function fetchList() {
  loading.value = true
  try {
    const params = {}
    if (searchText.value.trim()) params.keyword = searchText.value.trim()
    const res = await fetchClasses(params)
    tableData.value = res.data.data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function fetchMajorList() {
  try {
    const res = await fetchMajors()
    majors.value = res.data.data || []
  } catch {
    majors.value = []
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
    classId: row.classId,
    majorId: row.majorId ?? null,
    className: row.className || '',
    grade: row.grade || '',
    studyLength: row.studyLength ?? 4,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateClass(form.value)
      ElMessage.success('更新成功')
    } else {
      await addClass(form.value)
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
    await ElMessageBox.confirm('确定删除该班级吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteClass(id)
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  fetchList()
  fetchMajorList()
})

onActivated(() => {
  fetchList()
  fetchMajorList()
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

.class-name {
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
.grade-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  border: 1px solid rgba(14, 138, 95, 0.25);
  font-variant-numeric: tabular-nums;
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
