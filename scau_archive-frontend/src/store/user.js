import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchCaptcha, loginRequest } from '@/api/modules/auth'
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
  const captchaUuid = ref('')
  const captchaImg = ref('')

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

  async function refreshCaptcha(showError = true) {
    try {
      const response = await fetchCaptcha()
      const data = response.data?.data
      if (!data?.uuid || !data?.imageBase64) throw new Error('验证码响应格式错误')
      captchaUuid.value = data.uuid
      captchaImg.value = data.imageBase64
    } catch (error) {
      captchaUuid.value = ''
      captchaImg.value = ''
      if (showError) {
        errorMessage.value = error.response?.data?.message || '验证码加载失败，请点击重试'
      }
    }
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

    if (!captchaUuid.value) {
      errorMessage.value = '验证码尚未加载，请刷新后重试'
      return
    }

    isLoading.value = true

    try {
      const response = await loginRequest({
        username: username.value.trim(),
        password: password.value,
        captcha: captcha.value.trim(),
        uuid: captchaUuid.value,
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
      await refreshCaptcha(false)
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
    captchaUuid,
    captchaImg,
    initLoginState,
    refreshCaptcha,
    login,
    logout,
  }
})
