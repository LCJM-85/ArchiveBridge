<template>
  <el-header class="header">
    <div class="left">
      <div class="logo">
        <el-icon size="20" class="logo-icon"><HomeFilled /></el-icon>
        <span class="title">招生学籍档案管理系统</span>
      </div>

      <el-breadcrumb class="breadcrumb" separator="/">
        <el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
          {{ item }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="right">
      <el-button text @click="toggleFullScreen">
        <el-icon size="18"><FullScreen /></el-icon>
      </el-button>

      <el-button text @click="toggleTheme">
        <el-icon size="18">
          <Sunny v-if="isDark" />
          <Moon v-else />
        </el-icon>
      </el-button>

      <el-badge :value="3" hidden="0">
        <el-button text>
          <el-icon size="18"><Bell /></el-icon>
        </el-button>
      </el-badge>

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
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useMenuStore } from '@/store/menu'
import { useUserStore } from '@/store/user'
import { useTheme } from '@/composables/useTheme'
import { useFullscreen } from '@/composables/useFullscreen'
import { usePasswordChange } from '@/composables/usePasswordChange'
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

const breadcrumbList = ref(['首页', activeTitle])

const { isDark, toggleTheme } = useTheme()
const { toggleFullScreen } = useFullscreen()
const {
  passwordDialogVisible,
  passwordFormRef,
  loading,
  passwordForm,
  passwordRules,
  showPasswordDialog,
  handleChangePassword
} = usePasswordChange(() => {
  logout()
})

function logout() {
  userStore.logout()
  menuStore.resetMenu()
  ElMessage.success('退出成功')
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
