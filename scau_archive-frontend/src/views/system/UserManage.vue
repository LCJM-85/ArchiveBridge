<template>
  <div class="user-manage">
    <!-- 页面头部条 -->
    <div class="page-hero">
      <div class="ph-left">
        <div class="ph-kicker">SCAU ARCHIVE BRIDGE · 系统管理</div>
        <h2 class="font-display">用户管理</h2>
        <div class="ph-rule"></div>
        <p class="ph-sub">管理系统账号 · 角色权限 · 启用/禁用与密码重置</p>
      </div>
      <div class="ph-right">
        <svg class="ph-art" viewBox="0 0 220 90" preserveAspectRatio="xMidYMax meet">
          <!-- 用户头像组 -->
          <g transform="translate(30 20)">
            <circle cx="0" cy="0" r="16" fill="#123d2c" stroke="rgba(201,164,92,.5)" stroke-width="1.4"/>
            <circle cx="0" cy="-5" r="6" fill="#2fb984"/>
            <path d="M-11 14 C -11 4, 11 4, 11 14 Z" fill="#2fb984"/>
          </g>
          <g transform="translate(74 26)">
            <circle cx="0" cy="0" r="12" fill="#0a2e21" stroke="rgba(47,185,132,.45)" stroke-width="1.3"/>
            <circle cx="0" cy="-4" r="4.5" fill="#57c493"/>
            <path d="M-8 11 C -8 3, 8 3, 8 11 Z" fill="#57c493"/>
          </g>
          <g transform="translate(104 32)">
            <circle cx="0" cy="0" r="9" fill="#0a2e21" stroke="rgba(47,185,132,.4)" stroke-width="1.2"/>
            <circle cx="0" cy="-3" r="3.4" fill="#8fd3b0"/>
            <path d="M-6 8 C -6 2, 6 2, 6 8 Z" fill="#8fd3b0"/>
          </g>
          <!-- 盾牌 -->
          <g transform="translate(156 30)">
            <path d="M14 0 L26 5 L26 16 C 26 26, 20 30, 14 33 C 8 30, 2 26, 2 16 L2 5 Z" fill="#123d2c" stroke="#d9b877" stroke-width="1.5"/>
            <path d="M9 16 L13 20 L20 11" fill="none" stroke="#2fb984" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
          </g>
          <circle cx="182" cy="58" r="2" fill="rgba(47,185,132,.55)"/>
          <circle cx="52" cy="66" r="2" fill="rgba(230,205,149,.5)"/>
        </svg>
      </div>
    </div>

    <!-- 工具栏 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索用户名、姓名、手机号、邮箱" clearable style="width: 300px" @clear="search" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button type="primary" class="btn-gold" @click="openAddDialog">新增用户</el-button>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" min-width="120">
          <template #default="{ row }">
            <span class="user-cell"><span class="avatar-dot">{{ (row.realName || row.username || '?').slice(0, 1) }}</span>{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="role" label="角色" width="90" align="center">
          <template #default="{ row }">
            <span class="role-pill" :class="row.role === 'admin' ? 'role-admin' : 'role-user'">
              {{ row.role === 'admin' ? '管理员' : '用户' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="row.status === 1 ? 'st-active' : 'st-disabled'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <span class="cell-muted">{{ row.createTime ? row.createTime.substring(0, 16).replace('T', ' ') : '-' }}</span>
          </template>
        </el-table-column>
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
        <el-button type="primary" class="btn-gold" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { Search } from '@element-plus/icons-vue'
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

onActivated(() => {
  fetchData()
})
</script>

<style scoped>
.user-manage { display: flex; flex-direction: column; gap: 16px; }

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
  padding: 26px 34px;
  min-height: 116px;
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
  margin: 12px 0;
  border-radius: 2px;
  background: linear-gradient(90deg, #d9b877, rgba(201, 164, 92, 0));
}
.ph-sub { font-size: 12.5px; color: rgba(255, 255, 255, 0.6); margin: 0; }
.ph-right { position: relative; z-index: 2; }
.ph-art { width: 220px; height: 84px; flex-shrink: 0; }
@media (max-width: 900px) {
  .ph-art { display: none; }
  .page-hero { padding: 22px 24px; }
}

/* ===== 工具栏 ===== */
.toolbar-card {
  flex-shrink: 0;
  animation: rise-up 0.7s 0.08s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
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

/* ===== 表格 ===== */
.table-card {
  border-radius: 16px !important;
  overflow: hidden;
  animation: rise-up 0.7s 0.14s cubic-bezier(0.2, 0.75, 0.3, 1) both;
}
.table-card :deep(.el-table th.el-table__cell) {
  background: linear-gradient(180deg, var(--bg-tertiary), var(--bg-primary)) !important;
  font-weight: 600 !important;
}
.table-card :deep(.el-table__row:hover > td.el-table__cell) {
  background: var(--color-primary-light) !important;
}

.user-cell {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-weight: 500;
}
.avatar-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(140deg, #14a06f, #0b5c40);
  color: #e6cd95;
  border: 1px solid rgba(201, 164, 92, 0.4);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.role-pill {
  display: inline-block;
  min-width: 56px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
}
.role-admin { color: #e64545; background: rgba(245, 108, 108, 0.12); border: 1px solid rgba(245, 108, 108, 0.35); }
.role-user { color: var(--color-gold-dark); background: rgba(201, 164, 92, 0.12); border: 1px solid rgba(201, 164, 92, 0.35); }

.status-pill {
  display: inline-block;
  min-width: 46px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
}
.st-active { color: #0e8a5f; background: var(--color-primary-light); border: 1px solid rgba(14, 138, 95, 0.25); }
.st-disabled { color: var(--text-secondary); background: var(--bg-tertiary); border: 1px solid var(--border-light); }

.cell-muted { font-size: 12.5px; color: var(--text-secondary); }

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--border-light);
}

/* ===== 动效 ===== */
@keyframes rise-up {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .page-hero, .toolbar-card, .table-card { animation: none !important; }
}
</style>
