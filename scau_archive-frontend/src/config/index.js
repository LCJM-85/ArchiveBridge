export const APP_CONFIG = {
  // 空字符串 = 走同源代理（Nginx / Vite dev proxy），支持局域网访问
  baseURL: '',
  timeout: 10000,
}

export const JWT_CONFIG = {
  tokenPrefix: 'Bearer',
  header: 'Authorization',
}

export const PAGE_CONFIG = {
  defaultPageSize: 15,
}
