<template>
  <div class="login-container">
    <!-- 校园照片背景 + 绿色遮罩 -->
    <img class="bg-photo" src="/school.jpg" alt="" @error="photoFailed = true" />
    <div class="bg-shade" :class="{ 'no-photo': photoFailed }"></div>

    <!-- 光斑粒子 -->
    <div class="bubbles"><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div>
    <!-- 噪点质感 -->
    <div class="grain"></div>

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
        <div class="brand-en">SCAU · ARCHIVE DIGITALIZATION &amp; ANALYTICS</div>
        <div class="brand-rule"></div>
        <p class="brand-subtitle">华南农业大学 · 招生学籍档案数字化与可视化分析平台</p>
      </div>

      <div class="feature-list">
        <div class="feature-item">
          <span class="feature-dot"></span>
          <span>智能档案采集与 OCR 识别</span>
          <span class="feature-tag">AI-OCR</span>
        </div>
        <div class="feature-item">
          <span class="feature-dot"></span>
          <span>多维度数据可视化分析</span>
          <span class="feature-tag">ECharts</span>
        </div>
        <div class="feature-item">
          <span class="feature-dot"></span>
          <span>AI 预测与智能报告生成</span>
          <span class="feature-tag">LLM</span>
        </div>
      </div>

      <div class="brand-foot">SCAU ARCHIVE BRIDGE · 让每份档案都被看见</div>
    </div>

    <div class="login-right">
      <div class="login-card">
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-desc">登录档案管理平台</p>
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
            {{ isLoading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>
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
  captchaUrl,
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
  width: 100%;
  height: 100vh;
  display: flex;
  overflow: hidden;
  position: relative;
  background: #0a2118;
}

/* ---- 校园照片层 ---- */
.bg-photo {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}

/* ---- 翡翠绿遮罩（保证文字可读，暗色模式同款） ---- */
.bg-shade {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    radial-gradient(900px 500px at 16% 10%, rgba(14, 138, 95, 0.34), transparent 62%),
    radial-gradient(720px 480px at 84% 92%, rgba(201, 164, 92, 0.16), transparent 60%),
    linear-gradient(103deg, rgba(6, 28, 19, 0.78) 0%, rgba(8, 32, 22, 0.64) 32%, rgba(11, 38, 26, 0.48) 58%, rgba(8, 31, 23, 0.6) 100%);
  pointer-events: none;
  transition: background 0.4s ease;
}
/* 照片加载失败时回退为纯色氛围 */
.bg-shade.no-photo {
  background:
    radial-gradient(900px 500px at 18% 12%, rgba(14, 138, 95, 0.34), transparent 62%),
    radial-gradient(700px 460px at 82% 88%, rgba(201, 164, 92, 0.14), transparent 60%),
    radial-gradient(560px 420px at 66% 18%, rgba(47, 185, 132, 0.12), transparent 58%),
    linear-gradient(160deg, #0b2a1e 0%, #0a2118 55%, #08221a 100%);
}

/* ---- 噪点 ---- */
.grain {
  position: absolute;
  inset: 0;
  z-index: 2;
  opacity: 0.05;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='2'/%3E%3C/filter%3E%3Crect width='160' height='160' filter='url(%23n)' opacity='.6'/%3E%3C/svg%3E");
}

/* ---- 光斑粒子上升 ---- */
.bubbles {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}
.bubbles i {
  position: absolute;
  bottom: -120px;
  border-radius: 50%;
  background: radial-gradient(circle at 32% 30%, rgba(230, 205, 149, 0.5), rgba(14, 138, 95, 0.12) 70%);
  animation: rise linear infinite;
}
.bubbles i:nth-child(1) { left: 8%; width: 9px; height: 9px; animation-duration: 17s; }
.bubbles i:nth-child(2) { left: 22%; width: 5px; height: 5px; animation-duration: 23s; animation-delay: 3s; }
.bubbles i:nth-child(3) { left: 37%; width: 13px; height: 13px; animation-duration: 29s; animation-delay: 6s; }
.bubbles i:nth-child(4) { left: 52%; width: 6px; height: 6px; animation-duration: 20s; animation-delay: 9s; }
.bubbles i:nth-child(5) { left: 64%; width: 10px; height: 10px; animation-duration: 26s; animation-delay: 2s; }
.bubbles i:nth-child(6) { left: 76%; width: 4px; height: 4px; animation-duration: 31s; animation-delay: 5s; }
.bubbles i:nth-child(7) { left: 88%; width: 8px; height: 8px; animation-duration: 18s; animation-delay: 7s; }
@keyframes rise {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 0.8; }
  100% { transform: translateY(-118vh) translateX(26px); opacity: 0; }
}

/* ---- 左侧品牌区 ---- */
.login-left {
  flex: 1.25;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px 72px;
  position: relative;
  z-index: 4;
  color: #fff;
}

.brand-icon {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  background: linear-gradient(140deg, #14a06f, #0b5c40 78%);
  border: 1px solid rgba(201, 164, 92, 0.55);
  box-shadow: 0 14px 34px rgba(6, 40, 28, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e6cd95;
  margin-bottom: 28px;
  animation: badge-in 1s 0.15s cubic-bezier(0.2, 0.9, 0.3, 1.2) both;
}
@keyframes badge-in {
  from { transform: scale(0.5) rotate(-12deg); opacity: 0; }
  to { transform: scale(1) rotate(0); opacity: 1; }
}

.brand-title {
  font-family: var(--font-display);
  font-size: 42px;
  font-weight: 700;
  letter-spacing: 4px;
  line-height: 1.25;
  margin: 0;
  animation: fade-up 1s 0.3s ease both;
}
.brand-title em {
  font-style: normal;
  color: #e6cd95;
  font-size: 0.62em;
  letter-spacing: 2px;
}
.brand-en {
  font-size: 11px;
  letter-spacing: 4px;
  color: rgba(230, 205, 149, 0.65);
  margin-top: 10px;
  animation: fade-up 1s 0.45s ease both;
}
.brand-rule {
  width: 64px;
  height: 2px;
  margin: 26px 0;
  border-radius: 2px;
  background: linear-gradient(90deg, #d9b877, rgba(201, 164, 92, 0));
  animation: fade-up 1s 0.55s ease both;
}
.brand-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.62);
  margin: 0;
  animation: fade-up 1s 0.6s ease both;
}

.feature-list {
  margin-top: 44px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.78);
  animation: fade-up 0.8s ease both;
}
.feature-item:nth-child(1) { animation-delay: 0.75s; }
.feature-item:nth-child(2) { animation-delay: 0.9s; }
.feature-item:nth-child(3) { animation-delay: 1.05s; }
.feature-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #d9b877;
  box-shadow: 0 0 10px rgba(201, 164, 92, 0.9);
}
.feature-tag {
  margin-left: auto;
  font-size: 10px;
  letter-spacing: 2px;
  color: rgba(230, 205, 149, 0.7);
  border: 1px solid rgba(201, 164, 92, 0.35);
  padding: 3px 10px;
  border-radius: 999px;
  flex-shrink: 0;
}

.brand-foot {
  margin-top: 52px;
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.28);
  animation: fade-up 1s 1.2s ease both;
}
@keyframes fade-up {
  from { transform: translateY(22px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* ---- 右侧登录卡片 ---- */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  position: relative;
  z-index: 4;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 42px 38px 34px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.055);
  /* 无 backdrop-filter：保持与生产构建一致的透明玻璃效果（构建产物不支持 blur） */
  border: 1px solid rgba(255, 255, 255, 0.13);
  box-shadow: 0 40px 90px rgba(2, 18, 12, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.14);
  position: relative;
  animation: card-in 1s 0.35s cubic-bezier(0.18, 0.9, 0.28, 1.15) both, floaty 7s 1.8s ease-in-out infinite;
}
.login-card::before {
  content: '';
  position: absolute;
  top: -1px;
  right: 34px;
  width: 120px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, #e6cd95);
}
@keyframes card-in {
  from { transform: translateY(46px) scale(0.96); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}
@keyframes floaty {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.login-title {
  font-family: var(--font-display);
  font-size: 26px;
  color: #fff;
  letter-spacing: 2px;
  text-align: center;
  font-weight: 600;
  margin: 0 0 8px;
}
.login-desc {
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  margin: 0 0 28px;
  letter-spacing: 1px;
}
.mb-16 { margin-bottom: 16px; }

.input-group { margin-bottom: 18px; }
.input-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.captcha-box { display: flex; gap: 10px; }
.captcha-input { flex: 1; }
.captcha-img {
  width: 110px;
  height: 40px;
  cursor: pointer;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  flex-shrink: 0;
}

.login-options {
  margin: 4px 0 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 8px;
  border-radius: 13px;
  border: none;
  background: linear-gradient(135deg, #14a06f, #0b5c40);
  box-shadow: 0 12px 30px rgba(8, 60, 42, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 38px rgba(8, 60, 42, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}
.login-btn::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  width: 46px;
  left: -70px;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.28), transparent);
  transform: skewX(-18deg);
  animation: sweep 8s ease infinite;
}
@keyframes sweep {
  0% { left: -70px; }
  60% { left: 115%; }
  100% { left: 115%; }
}

.login-tip {
  text-align: center;
  margin-top: 20px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 1px;
}

/* ---- 输入框覆盖 ---- */
.login-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.07) !important;
  border: 1px solid rgba(255, 255, 255, 0.13) !important;
  box-shadow: none !important;
  border-radius: 12px !important;
  padding: 0 12px !important;
  transition: border-color 0.25s ease, background 0.25s ease, box-shadow 0.25s ease;
}
.login-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.25) !important;
}
.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: #d9b877 !important;
  background: rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 0 0 3px rgba(201, 164, 92, 0.16) !important;
}
.login-input :deep(.el-input__inner) {
  color: #ffffff !important;
  height: 40px !important;
}
.login-input :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.28) !important;
}
.login-input :deep(.el-input__prefix) {
  color: rgba(230, 205, 149, 0.75) !important;
}
.login-options :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.55) !important;
}
.login-options :deep(.el-checkbox__inner) {
  background: rgba(255, 255, 255, 0.07);
  border-color: rgba(255, 255, 255, 0.35);
}
.login-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #d9b877;
  border-color: #d9b877;
}

/* ---- 响应式 ---- */
@media (max-width: 900px) {
  .login-left { display: none; }
  .login-right { width: 100%; padding: 24px; }
}
</style>
