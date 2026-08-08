<template>
  <el-header class="header">
    <div class="left">
      <el-button text class="hamburger" @click="$emit('toggle-sidebar')">
        <el-icon size="20"><Operation /></el-icon>
      </el-button>
      <div class="logo">
        <el-icon size="20" class="logo-icon"><HomeFilled /></el-icon>
        <span class="title">数智档桥</span>
      </div>

      <el-breadcrumb class="breadcrumb" separator="/">
        <el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
          {{ item }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="right">
      <el-button text class="desktop-only" @click="toggleFullScreen">
        <el-icon size="18"><FullScreen /></el-icon>
      </el-button>

      <el-button text @click="toggleTheme">
        <el-icon size="18">
          <Sunny v-if="isDark" />
          <Moon v-else />
        </el-icon>
      </el-button>

      <el-tooltip content="数据脱敏" placement="bottom">
        <el-switch
          v-model="desensitizeEnabled"
          size="small"
          style="margin: 0 6px"
          @change="handleDesensitizeToggle"
          inline-prompt
          active-text="脱敏"
          inactive-text="原始"
        />
      </el-tooltip>

      <el-button text class="desktop-only">
        <el-icon size="18"><Bell /></el-icon>
      </el-button>

      <el-dropdown>
        <div class="user-info">
          <el-icon size="18" class="user-icon"><User /></el-icon>
          <span class="desktop-only-inline">{{ currentUsername || '管理员' }}</span>
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
import { ref, onMounted, inject } from 'vue'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useMenuStore } from '@/store/menu'
import { useTabStore } from '@/store/tab'
import { useUserStore } from '@/store/user'
import { useTheme } from '@/composables/useTheme'
import { useFullscreen } from '@/composables/useFullscreen'
import { usePasswordChange } from '@/composables/usePasswordChange'
import { getDesensitizeStatus, toggleDesensitize } from '@/api/modules/desensitize'
import {
  HomeFilled,
  FullScreen,
  Sunny,
  Moon,
  Bell,
  User,
  Operation
} from '@element-plus/icons-vue'

const menuStore = useMenuStore()
const tabStore = useTabStore()
const userStore = useUserStore()
const { activeTitle } = storeToRefs(menuStore)
const { currentUsername } = storeToRefs(userStore)

defineEmits(['toggle-sidebar'])

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

const desensitizeEnabled = ref(false)

onMounted(async () => {
  try {
    const res = await getDesensitizeStatus()
    desensitizeEnabled.value = res.data.data === true
  } catch {}
})

async function handleDesensitizeToggle(val) {
  try {
    await toggleDesensitize(val)
    ElMessage.success(val ? '脱敏已开启，刷新数据中...' : '脱敏已关闭，刷新数据中...')
    setTimeout(() => location.reload(), 300)
  } catch {
    desensitizeEnabled.value = !val
    ElMessage.error('操作失败')
  }
}

function logout() {
  userStore.logout()
  tabStore.resetTabs()
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

/* ===== Mobile responsive ===== */
.hamburger {
  display: none;
}

@media (max-width: 768px) {
  .hamburger {
    display: inline-flex;
  }
  .header {
    padding: 0 12px;
  }
  .breadcrumb {
    display: none;
  }
  .desktop-only {
    display: none !important;
  }
  .desktop-only-inline {
    display: none !important;
  }
  .title {
    font-size: 14px !important;
  }
  .right {
    gap: 2px;
  }
}
</style>
