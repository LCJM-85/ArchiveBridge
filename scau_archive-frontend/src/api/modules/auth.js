import request from '../request'

export function fetchCaptcha() {
  return request.get('/api/captcha')
}

export function loginRequest(payload) {
  return request.post('/api/login', payload)
}

export function changePassword(data) {
  return request.post('/api/change-password', data)
}
