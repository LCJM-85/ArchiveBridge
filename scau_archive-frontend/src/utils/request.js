import axios from 'axios'

const request = axios.create({
  // 所有接口请求统一走后端服务
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  // 验证码依赖后端 session，跨域请求必须携带 cookie
  withCredentials: true,
})

// 请求拦截器：自动携带 JWT Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理未登录/Token 失效
request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || ''
      // 登录接口自身的 401（账号密码错误）不做全局跳转，由登录页自己展示错误
      if (requestUrl.includes('/api/login')) {
        return Promise.reject(error)
      }

      // 这里读取后端 JsonAuthenticationEntryPoint 返回的 message/path
      const payload = error.response?.data || {}
      const backendMessage = payload.message || '登录已过期，请重新登录'
      const loginRoute = parsePathToRoute(payload.path)

      // 记录当前地址，登录成功后可回跳
      const currentPath = window.location.pathname + window.location.search
      const separator = loginRoute.includes('?') ? '&' : '?'
      const targetLoginRoute = `${loginRoute}${separator}redirect=${encodeURIComponent(currentPath)}&message=${encodeURIComponent(backendMessage)}`

      localStorage.removeItem('token')
      localStorage.removeItem('username')
      // 用 location 触发整页跳转，避免在任意组件上下文依赖 router 实例
      window.location.href = targetLoginRoute
    }
    return Promise.reject(error)
  }
)

function parsePathToRoute(targetPath) {
  if (!targetPath) return '/login'
  // 兼容后端返回完整 URL（http://localhost:5173/login）或相对路径（/login）
  if (targetPath.startsWith('http://') || targetPath.startsWith('https://')) {
    try {
      const parsedUrl = new URL(targetPath)
      return parsedUrl.pathname + parsedUrl.search
    } catch (e) {
      return '/login'
    }
  }
  return targetPath
}

export default request
