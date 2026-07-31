<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><UserFilled /></el-icon>
            <span>学籍数据管理</span>
          </div>
          <div class="card-header-right">
            <el-select v-model="degreeFilter" placeholder="培养层次" clearable style="width:130px" @change="handleSearch">
              <el-option v-for="d in degrees" :key="d.degreeId" :label="levelMap[d.degreeName] || d.degreeName" :value="d.degreeId" />
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
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="60" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.gender === '男' ? 'var(--color-primary)' : '#e84393' }">{{ row.gender }}</span>
          </template>
        </el-table-column>
        <el-table-column label="培养层次" width="100" align="center">
          <template #default="{ row }">
            {{ levelMap[row.degreeName] || row.degreeName }}
          </template>
        </el-table-column>
        <el-table-column prop="idCard" label="身份证号" width="200" />
        <el-table-column prop="provinceName" label="生源省份" width="140" />
        <el-table-column prop="majorName" label="专业" width="180" />
        <el-table-column prop="className" label="班级" width="220" />

        <el-table-column prop="admissionDate" label="入学日期" width="110" align="center" />
        <el-table-column label="毕业状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.graduated ? 'success' : 'primary'" size="small" effect="plain">
              {{ row.graduated ? '已毕业' : '在读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源文件" width="160">
          <template #default="{ row }">
            <span style="font-size:12px;color:var(--text-secondary)">{{ row.fileName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="录入时间" width="175" align="center">
          <template #default="{ row }">
            <span style="font-size:12px;color:var(--text-secondary);white-space:nowrap">{{ row.createTime ? row.createTime.replace('T', ' ').split('.')[0] : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="175" align="center">
          <template #default="{ row }">
            <span style="font-size:12px;color:var(--text-secondary);white-space:nowrap">{{ row.updateTime ? row.updateTime.replace('T', ' ').split('.')[0] : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row.id)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学籍记录' : '新增学籍记录'" width="600px" :close-on-click-modal="false">
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
            <el-form-item label="生源省份" prop="provinceId">
              <el-select v-model="form.provinceId" placeholder="请选择" style="width:100%" filterable clearable>
                <el-option v-for="p in provinces" :key="p.provinceId" :label="p.provinceName" :value="p.provinceId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业" prop="majorId">
              <el-select v-model="form.majorId" placeholder="选择或输入" style="width:100%" filterable clearable allow-create default-first-option @input-value-change="onMajorInputChange" @blur="onMajorBlur" @change="onMajorChange">
                <el-option v-for="m in majors" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="班级" prop="classId">
              <el-select v-model="form.classId" placeholder="选择或输入" style="width:100%" filterable clearable allow-create default-first-option @input-value-change="onClassInputChange" @blur="onClassBlur" @change="onClassChange">
                <el-option v-for="c in classes" :key="c.classId" :label="c.className" :value="c.classId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入学日期" prop="admissionDate">
              <el-date-picker v-model="form.admissionDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="毕业状态" prop="graduated">
              <el-switch v-model="form.graduated" active-text="已毕业" inactive-text="在读" />
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
import { Plus, Edit, Delete, Search, UserFilled, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchStudentPage, addStudent, updateStudent, deleteStudent, fetchProvinces, fetchMajors, fetchClasses, fetchDegrees } from '@/api/modules/student'

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
const classes = ref([])

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
    const res = await fetchStudentPage(params)
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

async function fetchClassesList() {
  try {
    const res = await fetchClasses()
    classes.value = res.data.data || []
  } catch { classes.value = [] }
}

async function add(data) {
  const res = await addStudent(data)
  return res.data
}

async function update(data) {
  const res = await updateStudent(data)
  return res.data
}

async function remove(id) {
  const res = await deleteStudent(id)
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
  provinceId: null,
  majorId: null,
  classId: null,
  admissionDate: '',
  graduated: false,
}
const form = ref({ ...defaultForm })

const pendingMajorText = ref('')
const pendingClassText = ref('')

function onMajorInputChange(val) { pendingMajorText.value = val || '' }
function onClassInputChange(val) { pendingClassText.value = val || '' }
function onMajorChange() { pendingMajorText.value = '' }
function onClassChange() { pendingClassText.value = '' }
function onMajorBlur() {
  const text = pendingMajorText.value?.trim()
  if (text) form.value.majorId = text
}
function onClassBlur() {
  const text = pendingClassText.value?.trim()
  if (text) form.value.classId = text
}

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
  pendingMajorText.value = ''
  pendingClassText.value = ''
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
    provinceId: row.provinceId ?? null,
    majorId: row.majorId ?? null,
    classId: row.classId ?? null,
    admissionDate: row.admissionDate || '',
    graduated: row.graduated ?? false,
  }
  pendingMajorText.value = ''
  pendingClassText.value = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  const payload = { ...form.value }
  if (typeof payload.majorId === 'string') {
    payload.majorName = payload.majorId.trim()
    payload.majorId = null
  }
  if (typeof payload.classId === 'string') {
    payload.className = payload.classId.trim()
    payload.classId = null
  }
  try {
    if (isEdit.value) {
      await update(payload)
      ElMessage.success('更新成功')
    } else {
      await add(payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await fetchPage()
    await fetchMajorsList()
    await fetchClassesList()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该学籍记录吗？', '提示', { type: 'warning' })
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
  fetchClassesList()
})

onActivated(() => {
  fetchPage()
  fetchProvincesList()
  fetchDegreesList()
  fetchMajorsList()
  fetchClassesList()
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
