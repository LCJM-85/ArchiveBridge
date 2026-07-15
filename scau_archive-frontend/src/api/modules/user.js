import request from '../request'

export function fetchUserList(params) {
  return request.get('/api/user/list', { params })
}

export function createUser(data) {
  return request.post('/api/user/create', data)
}

export function updateUser(data) {
  return request.put('/api/user/update', data)
}

export function setUserStatus(data) {
  return request.put('/api/user/status', data)
}

export function deleteUser(id) {
  return request.delete(`/api/user/delete/${id}`)
}

export function resetUserPassword(data) {
  return request.put('/api/user/reset-password', data)
}
