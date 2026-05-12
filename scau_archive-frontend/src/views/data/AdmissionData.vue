<template>
  <div class="page-wrapper">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <el-icon size="18" color="var(--color-primary)"><DataBoard /></el-icon>
            <span>招生数据管理</span>
          </div>
          <div class="card-header-right">
            <el-input v-model="searchText" placeholder="搜索所有字段" clearable style="width:160px" @keyup.enter="handleSearch" @clear="handleClear" />
            <el-date-picker
              v-model="store.createTimeRange"
              type="daterange"
              range-separator="至"
              start-placeholder="录入开始"
              end-placeholder="录入结束"
              value-format="YYYY-MM-DD"
              style="width:200px"
            />
            <el-date-picker
              v-model="store.updateTimeRange"
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

      <el-table :data="store.tableData" v-loading="store.loading" stripe border style="width:100%">
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="80" />
        <el-table-column prop="gender" label="性别" width="60" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.gender === '男' ? 'var(--color-primary)' : '#e84393' }">{{ row.gender }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="idCard" label="身份证号" min-width="190" show-overflow-tooltip />
        <el-table-column prop="examNo" label="考生号" min-width="140" />
        <el-table-column prop="provinceName" label="生源省份" min-width="100" />
        <el-table-column prop="majorName" label="录取专业" min-width="120" />
        <el-table-column prop="admissionDate" label="录取日期" width="110" align="center" />
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
          v-model:current-page="store.current"
          v-model:page-size="store.pageSize"
          :total="store.total"
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
                <el-option v-for="p in store.provinces" :key="p.provinceId" :label="p.provinceName" :value="p.provinceId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="录取专业" prop="majorId">
              <el-select v-model="form.majorId" placeholder="请选择" style="width:100%" filterable clearable>
                <el-option v-for="m in store.majors" :key="m.majorId" :label="m.majorName" :value="m.majorId" />
              </el-select>
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
import { ref, onMounted } from 'vue'
import { Plus, Edit, Delete, Search, DataBoard, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAdmissionStore } from '@/store/admission'

const store = useAdmissionStore()

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
  examNo: '',
  admissionDate: '',
  provinceId: null,
  majorId: null,
}
const form = ref({ ...defaultForm })

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

function handleSearch() {
  store.keyword = searchText.value
  store.current = 1
  store.fetchPage()
}

function handleClear() {
  searchText.value = ''
  store.keyword = ''
  store.current = 1
  store.fetchPage()
}

function resetFilters() {
  searchText.value = ''
  store.keyword = ''
  store.clearTimeRanges()
  store.current = 1
  store.fetchPage()
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
    examNo: row.examNo || '',
    admissionDate: row.admissionDate || '',
    provinceId: row.provinceId ?? null,
    majorId: row.majorId ?? null,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await store.update(form.value)
      ElMessage.success('更新成功')
    } else {
      await store.add(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await store.fetchPage()
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
    await store.remove(id)
    ElMessage.success('删除成功')
    await store.fetchPage()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

function handlePageChange(p) {
  store.setPage(p)
  store.fetchPage()
}

function handleSizeChange(size) {
  store.pageSize = size
  store.current = 1
  store.fetchPage()
}

onMounted(() => {
  store.fetchPage()
  store.fetchProvincesList()
  store.fetchMajorsList()
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
