<template>
  <div class="user-manage">
    <!-- 工具栏 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索用户名、姓名、手机号、邮箱" clearable style="width: 300px" @clear="search" @keyup.enter="search" />
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button type="primary" @click="openAddDialog">新增用户</el-button>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="role" label="角色" width="80">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.role === 'admin' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              v-if="row.role !== 'admin'"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              v-if="row.role !== 'admin'"
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              @click="handleResetPassword(row)"
            >
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 15, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑用户' : '新增用户'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEditing" />
        </el-form-item>
        <el-form-item v-if="!isEditing" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchUserList, createUser, updateUser, setUserStatus, deleteUser, resetUserPassword } from '@/api/modules/user'

const keyword = ref('')
const current = ref(1)
const size = ref(15)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)
const submitting = ref(false)

const dialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref(null)
const form = ref({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  remark: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function fetchData() {
  loading.value = true
  try {
    const res = await fetchUserList({ current: current.value, size: size.value, keyword: keyword.value })
    const page = res.data?.data || {}
    tableData.value = page.records || []
    total.value = page.total || 0
  } catch {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  current.value = 1
  fetchData()
}

function openAddDialog() {
  isEditing.value = false
  form.value = { id: null, username: '', password: '', realName: '', phone: '', email: '', remark: '' }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEditing.value = true
  form.value = {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    remark: row.remark,
  }
  // 编辑时密码非必填
  rules.password = []
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEditing.value) {
      await updateUser({
        id: form.value.id,
        realName: form.value.realName,
        phone: form.value.phone,
        email: form.value.email,
        remark: form.value.remark,
      })
      ElMessage.success('修改成功')
    } else {
      await createUser({
        username: form.value.username,
        password: form.value.password,
        realName: form.value.realName,
        phone: form.value.phone,
        email: form.value.email,
        remark: form.value.remark,
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
    // 恢复密码必填规则（下一次新增时用）
    rules.password = [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${actionText}账号「${row.username}」吗？`, '提示')
    await setUserStatus({ id: row.id, status: newStatus })
    ElMessage.success(`${actionText}成功`)
    fetchData()
  } catch {
    // 取消或失败都不处理
  }
}

async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(`确定将账号「${row.username}」的密码重置为 123456 吗？`, '提示')
    await resetUserPassword({ id: row.id, password: '123456' })
    ElMessage.success('密码已重置为 123456')
  } catch {
    // 取消不处理
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除账号「${row.username}」吗？此操作不可恢复。`, '警告', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 取消或失败不处理
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.user-manage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar-card {
  flex-shrink: 0;
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
