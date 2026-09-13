<template>
  <div class="login-container">
    <!-- 校园照片背景 + 绿色遮罩 -->
    <img class="bg-photo" src="/school.jpg" alt="" @error="photoFailed = true" />
    <div class="bg-shade" :class="{ 'no-photo': photoFailed }"></div>

    <div class="login-left">
      <div class="brand">
        <div class="brand-icon">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.6">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
            <line x1="8" y1="7" x2="16" y2="7"/>
            <line x1="8" y1="11" x2="14" y2="11"/>
          </svg>
        </div>
        <h1 class="brand-title">数智档桥 <em>ArchiveBridge</em></h1>
        <div class="brand-en">SOUTH CHINA AGRICULTURAL UNIVERSITY</div>
        <div class="brand-rule"></div>
        <p class="brand-subtitle">华南农业大学 · 招生学籍档案数字化与可视化分析平台</p>
      </div>

      <div class="archive-statement">
        <span class="archive-kicker">校园档案 · 数字典藏</span>
        <h2>留存来时路，<br />读懂成长的轨迹。</h2>
        <p>从一份入学档案开始，连接招生、学籍与毕业数据。<br />让记录有据可查，让分析有所依循。</p>
      </div>
      <div class="brand-foot"><span>ARCHIVE / CONNECT / INSIGHT</span><span>华南农业大学</span></div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <div class="login-eyebrow">ARCHIVE BRIDGE / 工作空间</div>
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-desc">使用你的账号，进入档案管理工作台</p>
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
            <label class="input-label" for="login-username">用户名</label>
            <el-input
              id="login-username"
              autocomplete="username"
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
            <label class="input-label" for="login-password">密码</label>
            <el-input
              id="login-password"
              autocomplete="current-password"
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
            <label class="input-label" for="login-captcha">验证码</label>
            <div class="captcha-box">
              <el-input
                id="login-captcha"
                v-model="captcha"
                placeholder="验证码"
                :disabled="isLoading"
                size="large"
                class="login-input captcha-input"
              />
              <img
                :src="captchaImg"
                class="captcha-img"
                alt="验证码，点击刷新"
                tabindex="0"
                role="button"
                @click="refreshCaptcha"
                @keydown.enter.prevent="refreshCaptcha"
                @keydown.space.prevent="refreshCaptcha"
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
            {{ isLoading ? '登录中...' : '进入工作台' }}
          </el-button>
        </el-form>
        <p class="login-note">招生 · 学籍 · 毕业<br /><span>档案数字化与可视化分析平台</span></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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
  captchaImg,
} = storeToRefs(userStore)
const { initLoginState, refreshCaptcha, login } = userStore

const photoFailed = ref(false)

onMounted(() => {
  initLoginState(route)
})

const handleLogin = () => login(route, router)
</script>

<style scoped>
.login-container {
  --text-primary: #f4f1e8; --text-secondary: #d0d9d1; --text-tertiary: #aebfb2;
  min-height: 100svh; display: grid; grid-template-columns: 1.15fr 1fr;
  position: relative; isolation: isolate; background: #18382e;
}
.bg-photo { position: absolute; inset: 0; z-index: -2; width: 100%; height: 100%; object-fit: cover; object-position: center 45%; }
.bg-shade {
  position: absolute; inset: 0; z-index: -1;
  background: linear-gradient(90deg, rgba(10, 30, 23, .50), rgba(13, 34, 26, .28) 40%, rgba(13, 34, 26, .64) 72%, rgba(13, 34, 26, .78)),
    linear-gradient(180deg, rgba(11, 29, 22, .22), transparent 35%, rgba(11, 29, 22, .62));
}
.bg-shade.no-photo { background: linear-gradient(110deg, #244a3b, #122b23); }
.login-left { position: relative; color: #fff; padding: 52px 9%; display: flex; flex-direction: column; min-height: 100svh; box-sizing: border-box; }
.brand { position: relative; padding-left: 48px; }
.brand-icon { position: absolute; left: 0; top: 2px; color: #dfcca5; }
.brand-title { font-family: var(--font-display); font-size: 25px; font-weight: 600; letter-spacing: 3px; margin: 0; }
.brand-title em { display: block; font-family: var(--font-body); font-size: 11px; font-style: normal; font-weight: 400; letter-spacing: 2px; margin-top: 8px; color: #dfebe4; }
.brand-en { margin-top: 30px; color: #d8e0db; font-size: 10px; letter-spacing: 1.6px; line-height: 1.8; }
.brand-rule { width: 34px; height: 1px; background: #c7b38a; margin: 20px 0; }
.brand-subtitle { margin: 0; font-size: 12px; line-height: 1.9; color: #e1e9e4; max-width: 330px; }
.archive-statement { margin-top: auto; padding: 100px 0 64px; }
.archive-kicker { color: #e0ceaa; font-size: 12px; letter-spacing: 3px; }
.archive-statement h2 { font-family: var(--font-display); font-size: clamp(30px, 3.2vw, 48px); font-weight: 400; line-height: 1.6; letter-spacing: 2px; margin: 20px 0; }
.archive-statement p { color: #e1e9e4; font-size: 13px; line-height: 2; }
.brand-foot { display: flex; justify-content: space-between; gap: 16px; border-top: 1px solid #ffffff30; padding-top: 20px; color: #d8e0db; font-size: 10px; letter-spacing: 1.5px; }
.login-right { display: flex; align-items: center; justify-content: center; padding: 64px 48px; position: relative; }
.login-card { width: 100%; max-width: 352px; }
.login-eyebrow { color: #dfcca5; letter-spacing: 2px; font-size: 10px; margin-bottom: 30px; }
.login-title { font-family: var(--font-display); font-size: 32px; font-weight: 400; letter-spacing: 3px; color: var(--text-primary); margin: 0 0 14px; }
.login-desc { font-size: 13px; color: var(--text-secondary); margin: 0 0 36px; line-height: 1.7; }
.mb-16 { margin-bottom: 20px; }
.input-group { margin-bottom: 22px; }
.input-label { display: block; font-size: 12px; letter-spacing: 1px; font-weight: 400; color: var(--text-secondary); margin-bottom: 10px; }
.login-input :deep(.el-input__wrapper) { background: #ffffff08; box-shadow: 0 0 0 1px #d9e3d52e inset; border-radius: 5px; padding: 3px 13px; }
.login-input :deep(.el-input__wrapper:hover) { box-shadow: 0 0 0 1px #d9e3d561 inset; }
.login-input :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #d7c39b inset; background: #ffffff0c; }
.login-input :deep(.el-input__inner) { height: 40px; color: #f4f1e8; }
.login-input :deep(.el-input__inner::placeholder) { color: #aebfb2; }
.login-input :deep(.el-input__prefix), .login-input :deep(.el-input__suffix) { color: #b6c7bb; }
.captcha-box { display: flex; gap: 12px; }
.captcha-input { flex: 1; min-width: 0; }
.captcha-img { width: 116px; height: 46px; cursor: pointer; border-radius: 5px; border: 1px solid #d9e3d52e; object-fit: contain; background: #f0efe7; }
.login-options { margin: 0 0 24px; }
.login-options :deep(.el-checkbox__label) { color: #d0d9d1 !important; font-size: 12px; }
.login-options :deep(.el-checkbox__inner) { background: transparent; border-color: #8b9e8f; }
.login-options :deep(.is-checked .el-checkbox__inner) { background: #567e64; border-color: #8b9e8f; }
.login-btn { width: 100%; height: 46px; font-size: 14px; font-weight: 500; letter-spacing: 2px; border-radius: 5px; color: #233a2f; background: #d7c39b; border-color: #d7c39b; }
.login-btn:hover { background: #e2d2b1; border-color: #e2d2b1; color: #233a2f; }
.login-btn.is-disabled, .login-btn.is-disabled:hover { background: #9b947f; border-color: #9b947f; color: #233a2f; opacity: .65; }
.login-note { margin: 34px 0 0; padding-top: 20px; border-top: 1px solid #d9e3d526; color: var(--text-secondary); font-size: 12px; line-height: 2; letter-spacing: 1px; }
.login-note span { color: var(--text-tertiary); font-size: 11px; letter-spacing: 0; }
.login-container :focus-visible { outline-color: #dfcca5; }
@media (max-width: 900px) {
  .login-container { grid-template-columns: .9fr 1fr; }
  .login-left { padding: 40px 28px; }
  .brand-title { font-size: 22px; }
  .brand-foot { flex-direction: column; }
  .login-right { padding: 40px 28px; }
}
@media (max-width: 640px) {
  .login-container { grid-template-columns: 1fr; }
  .bg-shade { background: linear-gradient(180deg, #0d221abd, #0d221ae8 40%, #0d221af5); }
  .login-left { min-height: 0; padding: 28px 28px 8px; }
  .brand-en, .brand-rule, .brand-subtitle, .archive-statement, .brand-foot { display: none; }
  .login-right { padding: 32px 28px; }
  .login-eyebrow { margin-bottom: 22px; }
}
</style>
