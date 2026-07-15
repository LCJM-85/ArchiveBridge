import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getCaptchaUrl, getCaptchaUrlWithTimestamp, loginRequest } from '@/api/modules/auth'
import {
  clearAuthInfo,
  clearRememberedUser,
  getRememberedUser,
  getStoredToken,
  getStoredUsername,
  getStoredRole,
  getLoginErrorMessage,
  getSafeRedirectPath,
  saveAuthInfo,
  saveRememberedUser,
} from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getStoredToken())
  const currentUsername = ref(getStoredUsername())
  const role = ref(getStoredRole())
  const username = ref('')
  const password = ref('')
  const rememberMe = ref(false)
  const isLoading = ref(false)
  const errorMessage = ref('')
  const captcha = ref('')
  const captchaUrl = ref(getCaptchaUrl())

  function initLoginState(route) {
    refreshCaptcha()

    const initialMessage = route.query.message
    if (typeof initialMessage === 'string' && initialMessage.trim()) {
      errorMessage.value = initialMessage
    }

    const rememberedUser = getRememberedUser()
    if (rememberedUser.username) {
      username.value = rememberedUser.username
      password.value = rememberedUser.password || ''
      rememberMe.value = true
    }
  }

  function refreshCaptcha() {
    captchaUrl.value = getCaptchaUrlWithTimestamp()
  }

  async function login(route, router) {
    errorMessage.value = ''

    if (!username.value.trim()) {
      errorMessage.value = '请输入用户名'
      return
    }

    if (!password.value) {
      errorMessage.value = '请输入密码'
      return
    }

    if (!captcha.value.trim()) {
      errorMessage.value = '请输入验证码'
      return
    }

    isLoading.value = true

    try {
      const response = await loginRequest({
        username: username.value.trim(),
        password: password.value,
        captcha: captcha.value.trim(),
      })

      if (response.data.success) {
        if (response.data.token) {
          token.value = response.data.token
        }

        currentUsername.value = username.value.trim()
        role.value = response.data.role || 'user'
        saveAuthInfo(token.value, currentUsername.value, role.value)

        if (rememberMe.value) {
          saveRememberedUser(username.value.trim(), password.value)
        } else {
          clearRememberedUser()
        }

        username.value = ''
        password.value = ''

        const redirectPath = getSafeRedirectPath(route.query.redirect)
        router.push(redirectPath)
      }
    } catch (error) {
      refreshCaptcha()
      captcha.value = ''
      errorMessage.value = getLoginErrorMessage(error)
    } finally {
      isLoading.value = false
    }
  }

  function logout() {
    token.value = ''
    currentUsername.value = ''
    role.value = ''
    clearAuthInfo()
  }

  return {
    token,
    currentUsername,
    role,
    username,
    password,
    rememberMe,
    isLoading,
    errorMessage,
    captcha,
    captchaUrl,
    initLoginState,
    refreshCaptcha,
    login,
    logout,
  }
})
