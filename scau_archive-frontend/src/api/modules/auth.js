import request from '../request'

export function getCaptchaUrl() {
  return `${request.defaults.baseURL}/api/captcha`
}

export function getCaptchaUrlWithTimestamp() {
  return `${getCaptchaUrl()}?time=${Date.now()}`
}

export function loginRequest(payload) {
  return request.post('/api/login', payload)
}

export function changePassword(data) {
  return request.post('/api/change-password', data)
}
