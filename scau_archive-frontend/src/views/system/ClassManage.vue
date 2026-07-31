<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Files /></el-icon>
            <span>班级管理</span>
          </div>
          <div class="card-header-right">
            <el-input v-model="searchText" placeholder="搜索班级/年级" clearable style="width:220px" @keyup.enter="fetchList" @clear="fetchList" />
            <el-button :icon="Search" @click="fetchList">查询</el-button>
            <el-button type="primary" :icon="Plus" @click="openAddDialog">新增班级</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="classId" label="ID" width="70" align="center" />
        <el-table-column prop="className" label="班级名称" min-width="140" />
        <el-table-column prop="grade" label="年级" width="90" align="center" />
        <el-table-column prop="studyLength" label="学制(年)" width="90" align="center" />
        <el-table-column prop="majorName" label="所属专业" min-width="140">
          <template #default="{ row }">
            {{ row.majorName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row.classId)">删除</el-button>
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
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
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
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-header-left { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.card-header-right { display: flex; align-items: center; gap: 8px; }
</style>
