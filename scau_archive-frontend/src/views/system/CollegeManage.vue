<template>
  <div class="page-wrapper">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 系统管理</div>
        <h2 class="font-display">学院管理</h2>
        <div class="ph-rule"></div>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 教学楼 -->
          <g transform="translate(24 18)">
            <rect x="0" y="0" width="54" height="40" rx="4" fill="#0a2e21" stroke="rgba(230,205,149,.4)" stroke-width="1.4"/>
            <path d="M27 -8 L27 0 M10 0 L27 -14 L44 0" fill="none" stroke="#d9b877" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <rect x="8" y="8" width="9" height="9" rx="1.5" fill="rgba(47,185,132,.5)"/>
            <rect x="23" y="8" width="9" height="9" rx="1.5" fill="rgba(47,185,132,.5)"/>
            <rect x="38" y="8" width="9" height="9" rx="1.5" fill="rgba(47,185,132,.5)"/>
            <rect x="8" y="24" width="9" height="9" rx="1.5" fill="rgba(201,164,92,.45)"/>
            <rect x="23" y="24" width="9" height="9" rx="1.5" fill="rgba(201,164,92,.45)"/>
            <rect x="38" y="24" width="9" height="9" rx="1.5" fill="rgba(201,164,92,.45)"/>
          </g>
          <!-- 树 -->
          <g transform="translate(100 46)">
            <rect x="7" y="12" width="5" height="16" rx="2" fill="#0a2e21"/>
            <circle cx="9" cy="8" r="10" fill="#123d2c" stroke="rgba(47,185,132,.5)" stroke-width="1.3"/>
            <circle cx="3" cy="4" r="6" fill="#14a06f" opacity=".7"/>
            <circle cx="16" cy="4" r="5" fill="#2fb984" opacity=".6"/>
          </g>
          <g transform="translate(132 52)">
            <rect x="6" y="10" width="4" height="12" rx="2" fill="#0a2e21"/>
            <circle cx="8" cy="7" r="8" fill="#123d2c" stroke="rgba(47,185,132,.45)" stroke-width="1.2"/>
            <circle cx="3" cy="4" r="5" fill="#2fb984" opacity=".6"/>
          </g>
          <!-- 旗 -->
          <g transform="translate(158 16)">
            <line x1="0" y1="0" x2="0" y2="40" stroke="#c9a45c" stroke-width="2" stroke-linecap="round"/>
            <path d="M0 0 L22 5 L0 12 Z" fill="#d9b877"/>
          </g>
          <circle cx="190" cy="62" r="2" fill="rgba(47,185,132,.55)"/>
          <circle cx="60" cy="72" r="2" fill="rgba(230,205,149,.5)"/>
        </svg>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><School /></el-icon>
            <span>学院管理</span>
          </div>
          <div class="card-header-right">
            <el-input v-model="searchText" placeholder="搜索学院名称" clearable style="width:220px" @keyup.enter="fetchList" @clear="fetchList">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button :icon="Search" @click="fetchList">查询</el-button>
            <el-button type="primary" class="btn-gold" :icon="Plus" @click="openAddDialog">新增学院</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="collegeId" label="ID" width="80" align="center" />
        <el-table-column prop="collegeName" label="学院名称" min-width="200">
          <template #default="{ row }">
            <span class="college-name"><span class="name-dot"></span>{{ row.collegeName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" class="op-btn" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" class="op-btn op-danger" :icon="Delete" @click="handleDelete(row.collegeId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学院' : '新增学院'" width="420px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="学院名称" prop="collegeName">
          <el-input v-model="form.collegeName" placeholder="请输入学院名称" />
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
import { Plus, Edit, Delete, Search, School } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchColleges, addCollege, updateCollege, deleteCollege } from '@/api/modules/dim'

const tableData = ref([])
const loading = ref(false)
const searchText = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = { collegeId: null, collegeName: '' }
const form = ref({ ...defaultForm })

const rules = {
  collegeName: [{ required: true, message: '请输入学院名称', trigger: 'blur' }],
}

async function fetchList() {
  loading.value = true
  try {
    const params = {}
    if (searchText.value.trim()) params.keyword = searchText.value.trim()
    const res = await fetchColleges(params)
    tableData.value = res.data.data || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  form.value = { ...defaultForm }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  form.value = { collegeId: row.collegeId, collegeName: row.collegeName || '' }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCollege(form.value)
      ElMessage.success('更新成功')
    } else {
      await addCollege(form.value)
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
    await ElMessageBox.confirm('确定删除该学院吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteCollege(id)
    ElMessage.success('删除成功')
    await fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(fetchList)
onActivated(fetchList)
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

.college-name {
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
