<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><Management /></el-icon>
            <span>元数据标准管理</span>
          </div>
          <div class="card-header-right">
            <el-input v-model="searchText" placeholder="字段编码 / 字段名称 / 来源字段" clearable style="width:280px" @keyup.enter="handleSearch" @clear="handleClear" />
            <el-button :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
            <el-button type="primary" :icon="Plus" @click="openAddDialog">新增</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="fieldCode" label="字段编码" width="160" />
        <el-table-column prop="fieldName" label="字段名称" width="160" />
        <el-table-column prop="fieldType" label="字段类型" width="100" />
        <el-table-column prop="sourceField" label="来源字段" width="140" />
        <el-table-column prop="transformType" label="转换类型" width="100" />
        <el-table-column prop="transformRule" label="转换规则" min-width="160" show-overflow-tooltip />
        <el-table-column prop="isRequired" label="是否必填" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRequired ? 'danger' : 'info'" size="small">
              {{ row.isRequired ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row.metadataId)">删除</el-button>
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
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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

.card-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
