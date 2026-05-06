import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  withCredentials: true,
})

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

request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || ''
      if (requestUrl.includes('/api/login')) {
        return Promise.reject(error)
      }

      const payload = error.response?.data || {}
      const backendMessage = payload.message || '登录已过期，请重新登录'
      const loginRoute = parsePathToRoute(payload.path)

      const currentPath = window.location.pathname + window.location.search
      const separator = loginRoute.includes('?') ? '&' : '?'
      const targetLoginRoute = `${loginRoute}${separator}redirect=${encodeURIComponent(currentPath)}&message=${encodeURIComponent(backendMessage)}`

      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.href = targetLoginRoute
    }
    return Promise.reject(error)
  }
)

function parsePathToRoute(targetPath) {
  if (!targetPath) return '/login'
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
