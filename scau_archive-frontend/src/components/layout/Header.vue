<template>
  <el-header class="header">
    <!-- 左侧：Logo + 系统名称 + 面包屑 -->
    <div class="left">
      <div class="logo">
        <el-icon size="20" class="logo-icon"><HomeFilled /></el-icon>
        <span class="title">招生学籍档案管理系统</span>
      </div>

      <!-- 面包屑 -->
      <el-breadcrumb class="breadcrumb" separator="/">
        <el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
          {{ item }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 右侧：工具区 + 用户信息 -->
    <div class="right">
      <!-- 全屏 -->
      <el-button text @click="toggleFullScreen">
        <el-icon size="18"><FullScreen /></el-icon>
      </el-button>

      <!-- 主题切换 -->
      <el-button text @click="toggleTheme">
        <el-icon size="18">
          <Sunny v-if="isDark" />
          <Moon v-else />
        </el-icon>
      </el-button>

      <!-- 通知铃铛 -->
      <el-badge :value="3" hidden="0">
        <el-button text>
          <el-icon size="18"><Bell /></el-icon>
        </el-button>
      </el-badge>

      <!-- 下拉用户菜单 -->
      <el-dropdown>
        <div class="user-info">
          <el-icon size="18" class="user-icon"><User /></el-icon>
          <span>{{ currentUsername || '管理员' }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="showPasswordDialog">修改密码</el-dropdown-item>
            <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请确认新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" :loading="loading">确定</el-button>
      </template>
    </el-dialog>
  </el-header>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useMenuStore } from '../../store/menu'
import { useUserStore } from '../../store/user'
import { changePassword } from '../../api/auth'
import {
  HomeFilled,
  FullScreen,
  Sunny,
  Moon,
  Bell,
  User
} from '@element-plus/icons-vue'

const menuStore = useMenuStore()
const userStore = useUserStore()
const { activeTitle } = storeToRefs(menuStore)
const { currentUsername } = storeToRefs(userStore)

// 模拟面包屑（可从路由动态生成）
const breadcrumbList = ref(['首页', activeTitle])

// 全屏
const toggleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 主题切换
const isDark = ref(localStorage.getItem('theme') === 'dark')
const toggleTheme = () => {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('theme', 'dark')
    ElMessage.success('已切换深色模式')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('theme', 'light')
    ElMessage.success('已切换浅色模式')
  }
}

// 退出
const logout = () => {
  userStore.logout()
  menuStore.resetMenu()
  ElMessage.success('退出成功')
}

// 修改密码
const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)
const loading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const showPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const handleChangePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.data.success) {
      ElMessage.success('密码修改成功，请重新登录')
      passwordDialogVisible.value = false
      logout()
    } else {
      ElMessage.error(res.data.message || '修改失败')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.header {
  height: 60px;
  padding: 0 24px;
  background: var(--header-bg);
  border-bottom: 1px solid var(--header-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--header-shadow);
}

.left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  color: var(--color-primary);
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: var(--header-text);
}

.breadcrumb {
  font-size: 14px;
}

.right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--bg-tertiary);
}

.user-icon {
  color: var(--color-primary);
}
</style>