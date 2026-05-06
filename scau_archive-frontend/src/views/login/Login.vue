<template>
  <div class="login-container">
    <div class="login-bg-overlay"></div>

    <div class="login-left">
      <div class="brand">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
            <line x1="8" y1="7" x2="16" y2="7"/>
            <line x1="8" y1="11" x2="14" y2="11"/>
          </svg>
        </div>
        <h1 class="brand-title">SCAU 档案管理系统</h1>
        <p class="brand-subtitle">华南农业大学 · 学生档案智能分析平台</p>
      </div>
      <div class="feature-list">
        <div class="feature-item">
          <span class="feature-dot"></span>
          <span>智能档案采集与OCR识别</span>
        </div>
        <div class="feature-item">
          <span class="feature-dot"></span>
          <span>多维度数据可视化分析</span>
        </div>
        <div class="feature-item">
          <span class="feature-dot"></span>
          <span>AI预测与智能报告生成</span>
        </div>
      </div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <div class="login-header">
          <h2 class="login-title">系统登录</h2>
          <p class="login-desc">欢迎使用档案管理平台</p>
        </div>

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          show-icon
          :closable="false"
          class="mb-16"
        />

        <el-form @submit.prevent="handleLogin" class="login-form">
          <div class="input-group">
            <label class="input-label">用户名</label>
            <el-input
              v-model="username"
              placeholder="请输入用户名"
              :disabled="isLoading"
              clearable
              size="large"
              class="login-input"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </div>

          <div class="input-group">
            <label class="input-label">密码</label>
            <el-input
              v-model="password"
              type="password"
              placeholder="请输入密码"
              show-password
              :disabled="isLoading"
              size="large"
              class="login-input"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </div>

          <div class="input-group">
            <label class="input-label">验证码</label>
            <div class="captcha-box">
              <el-input
                v-model="captcha"
                placeholder="验证码"
                :disabled="isLoading"
                size="large"
                class="login-input captcha-input"
              />
              <img
                :src="captchaUrl"
                class="captcha-img"
                alt="验证码"
                @click="refreshCaptcha"
              />
            </div>
          </div>

          <div class="login-options">
            <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
          </div>

          <el-button
            type="primary"
            class="login-btn"
            native-type="submit"
            :loading="isLoading"
            :disabled="!username.trim() || !password || !captcha.trim()"
            size="large"
          >
            {{ isLoading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const {
  username,
  password,
  rememberMe,
  isLoading,
  errorMessage,
  captcha,
  captchaUrl,
} = storeToRefs(userStore)
const { initLoginState, refreshCaptcha, login } = userStore

onMounted(() => {
  initLoginState(route)
})

const handleLogin = () => login(route, router)
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  overflow: hidden;
  position: relative;
  background: #1a1e24;
}

.login-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url('/school.jpg') center/cover no-repeat;
  opacity: 0.65;
  pointer-events: none;
}

.login-bg-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(2px);
  pointer-events: none;
}

/* Left Panel - Brand */
.login-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 80px;
  position: relative;
  z-index: 1;
}

.brand {
  margin-bottom: 48px;
}

.brand-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #1a7a4e, #2d9d6e);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(26, 122, 78, 0.3);
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px 0;
  letter-spacing: 2px;
}

.brand-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  font-weight: 400;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.feature-dot {
  width: 6px;
  height: 6px;
  background: #2d9d6e;
  border-radius: 50%;
  flex-shrink: 0;
}

/* Right Panel - Login Card */
.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
  z-index: 1;
}

.login-card {
  width: 100%;
  max-width: 380px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 40px 32px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px 0;
}

.login-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.input-group {
  margin-bottom: 20px;
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 6px;
}

.mb-16 {
  margin-bottom: 16px;
}

.captcha-box {
  display: flex;
  gap: 10px;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 110px;
  height: 40px;
  cursor: pointer;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  flex-shrink: 0;
}

.login-options {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #1a7a4e, #2d9d6e);
  border: none;
  letter-spacing: 4px;
}

.login-btn:hover {
  background: linear-gradient(135deg, #14603d, #238a5f);
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(26, 122, 78, 0.35);
}

.login-btn:active {
  transform: translateY(0);
}

/* Input overrides */
.login-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(255, 255, 255, 0.14) !important;
  box-shadow: none !important;
  border-radius: 10px !important;
  padding: 0 12px !important;
}

.login-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.25) !important;
}

.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: #2d9d6e !important;
}

.login-input :deep(.el-input__inner) {
  color: #ffffff !important;
  height: 40px !important;
}

.login-input :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3) !important;
}

.login-input :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* Checkbox override */
.login-options :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* Responsive */
@media (max-width: 900px) {
  .login-left {
    display: none;
  }
  .login-right {
    width: 100%;
  }
}
</style>
