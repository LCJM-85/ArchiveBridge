<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><School /></el-icon>
            <span>毕业数据管理</span>
          </div>
          <div class="card-header-right">
            <el-select v-model="degreeFilter" placeholder="学历" clearable style="width:130px" @change="handleSearch">
              <el-option v-for="d in degreeOptions" :key="d" :label="d" :value="d" />
            </el-select>
            <el-input v-model="searchText" placeholder="搜索所有字段" clearable style="width:160px" @keyup.enter="handleSearch" @clear="handleClear" />
            <el-date-picker
              v-model="createTimeRange"
              type="daterange"
              range-separator="至"
              start-placeholder="录入开始"
              end-placeholder="录入结束"
              value-format="YYYY-MM-DD"
              style="width:200px"
            />
            <el-date-picker
              v-model="updateTimeRange"
              type="daterange"
              range-separator="至"
              start-placeholder="更新开始"
              end-placeholder="更新结束"
              value-format="YYYY-MM-DD"
              style="width:200px"
            />
            <el-button :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
            <el-button @click="resetFilters">重置</el-button>
            <el-button type="primary" :icon="Plus" @click="openAddDialog">新增</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="80" />
        <el-table-column prop="gender" label="性别" width="60" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.gender === '男' ? 'var(--color-primary)' : '#e84393' }">{{ row.gender }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="idCard" label="身份证号" min-width="190" show-overflow-tooltip />
        <el-table-column prop="degreeName" label="学历" width="90" />
        <el-table-column prop="destName" label="毕业去向" min-width="100" />
        <el-table-column prop="graduationDate" label="毕业日期" width="110" align="center" />
        <el-table-column label="来源文件" min-width="140">
          <template #default="{ row }">
            <span style="font-size:12px;color:var(--text-secondary)">{{ row.fileName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="录入时间" width="155" align="center">
          <template #default="{ row }">
            <span style="font-size:12px;color:var(--text-secondary);white-space:nowrap">{{ row.createTime ? row.createTime.replace('T', ' ').split('.')[0] : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="155" align="center">
          <template #default="{ row }">
            <span style="font-size:12px;color:var(--text-secondary);white-space:nowrap">{{ row.updateTime ? row.updateTime.replace('T', ' ').split('.')[0] : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button size="small" circle :icon="Edit" @click="openEditDialog(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button size="small" circle type="danger" :icon="Delete" @click="handleDelete(row.id)" />
            </el-tooltip>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑毕业记录' : '新增毕业记录'" width="600px" :close-on-click-modal="false">
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
            <el-form-item label="学历" prop="degreeId">
              <el-select v-model="form.degreeId" placeholder="请选择" style="width:100%" filterable clearable>
                <el-option v-for="d in degrees" :key="d.degreeId" :label="d.degreeName" :value="d.degreeId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="毕业去向" prop="destId">
              <el-select v-model="form.destId" placeholder="请选择" style="width:100%" filterable clearable>
                <el-option v-for="d in destinations" :key="d.destId" :label="d.destName" :value="d.destId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="毕业日期" prop="graduationDate">
              <el-date-picker v-model="form.graduationDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { Plus, Edit, Delete, Search, School, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchGraduationPage, addGraduation, updateGraduation, deleteGraduation, fetchDegrees, fetchDestinations } from '@/api/modules/graduation'

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
const degrees = ref([])
const destinations = ref([])
const degreeOptions = ['学士', '硕士', '博士']

async function fetchPage() {
  loading.value = true
  try {
    const params = { current: current.value, size: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    if (degreeFilter.value) params.degreeName = degreeFilter.value
    if (createTimeRange.value && createTimeRange.value.length === 2) {
      params.createTimeStart = createTimeRange.value[0]
      params.createTimeEnd = createTimeRange.value[1]
    }
    if (updateTimeRange.value && updateTimeRange.value.length === 2) {
      params.updateTimeStart = updateTimeRange.value[0]
      params.updateTimeEnd = updateTimeRange.value[1]
    }
    const res = await fetchGraduationPage(params)
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

async function fetchDegreesList() {
  try {
    const res = await fetchDegrees()
    degrees.value = res.data.data || []
  } catch { degrees.value = [] }
}

async function fetchDestinationsList() {
  try {
    const res = await fetchDestinations()
    destinations.value = res.data.data || []
  } catch { destinations.value = [] }
}

async function add(data) {
  const res = await addGraduation(data)
  return res.data
}

async function update(data) {
  const res = await updateGraduation(data)
  return res.data
}

async function remove(id) {
  const res = await deleteGraduation(id)
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
  idCard: '',
  degreeId: null,
  destId: null,
  graduationDate: '',
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
    idCard: row.idCard || '',
    degreeId: row.degreeId ?? null,
    destId: row.destId ?? null,
    graduationDate: row.graduationDate || '',
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
    await ElMessageBox.confirm('确定删除该毕业记录吗？', '提示', { type: 'warning' })
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
  fetchDegreesList()
  fetchDestinationsList()
})

onActivated(() => {
  fetchPage()
  fetchDegreesList()
  fetchDestinationsList()
})
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
