<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Collection /></el-icon>
            <span>专业管理</span>
          </div>
          <div class="card-header-right">
            <el-input v-model="searchText" placeholder="搜索专业名称/代码" clearable style="width:220px" @keyup.enter="fetchList" @clear="fetchList" />
            <el-button :icon="Search" @click="fetchList">查询</el-button>
            <el-button type="primary" :icon="Plus" @click="openAddDialog">新增专业</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="majorId" label="ID" width="70" align="center" />
        <el-table-column prop="majorName" label="专业名称" min-width="160" />
        <el-table-column prop="majorCode" label="专业代码" width="120" />
        <el-table-column prop="collegeName" label="所属学院" min-width="140">
          <template #default="{ row }">
            {{ row.collegeName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="degreeName" label="培养层次" width="100" align="center">
          <template #default="{ row }">
            {{ row.degreeName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row.majorId)">删除</el-button>
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
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
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
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-header-left { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.card-header-right { display: flex; align-items: center; gap: 8px; }
</style>
