import request from '../request'

export function getDesensitizeStatus() {
  return request.get('/api/desensitize/status')
}

export function toggleDesensitize(enabled) {
  return request.post('/api/desensitize/toggle', { enabled })
}
