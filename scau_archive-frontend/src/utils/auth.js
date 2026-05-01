export function getSafeRedirectPath(rawRedirect) {
  if (typeof rawRedirect !== 'string' || !rawRedirect.trim()) {
    return '/dashboard'
  }

  const redirect = rawRedirect.trim()
  if (!redirect.startsWith('/') || redirect.startsWith('//')) {
    return '/dashboard'
  }

  return redirect
}

export function getLoginErrorMessage(error) {
  if (error.response) {
    const status = error.response.status
    const data = error.response.data || {}

    if (status === 401) {
      return '用户名或密码错误'
    }
    if (status === 400) {
      return data.message || '请求参数错误'
    }
    if (status >= 500) {
      return '服务器错误，请稍后重试'
    }
    return data.message || '登录失败'
  }

  if (error.request) {
    return '网络连接失败'
  }

  return '登录失败'
}

export function saveRememberedUser(username, password) {
  localStorage.setItem('rememberedUsername', username)
  localStorage.setItem('rememberedPassword', password)
}

export function clearRememberedUser() {
  localStorage.removeItem('rememberedUsername')
  localStorage.removeItem('rememberedPassword')
}

export function saveAuthInfo(token, username) {
  localStorage.setItem('token', token)
  localStorage.setItem('username', username)
}

export function clearAuthInfo() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
}

export function getStoredToken() {
  return localStorage.getItem('token') || ''
}

export function getStoredUsername() {
  return localStorage.getItem('username') || ''
}

export function getRememberedUser() {
  return {
    username: localStorage.getItem('rememberedUsername') || '',
    password: localStorage.getItem('rememberedPassword') || '',
  }
}
