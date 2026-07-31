<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><School /></el-icon>
            <span>学院管理</span>
          </div>
          <div class="card-header-right">
            <el-input v-model="searchText" placeholder="搜索学院名称" clearable style="width:220px" @keyup.enter="fetchList" @clear="fetchList" />
            <el-button :icon="Search" @click="fetchList">查询</el-button>
            <el-button type="primary" :icon="Plus" @click="openAddDialog">新增学院</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="collegeId" label="ID" width="80" align="center" />
        <el-table-column prop="collegeName" label="学院名称" min-width="200" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row.collegeId)">删除</el-button>
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
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
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
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-header-left { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.card-header-right { display: flex; align-items: center; gap: 8px; }
</style>
