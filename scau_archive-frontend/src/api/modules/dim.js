import request from '../request'

// ===== 学院 =====
export function fetchColleges(params) {
  return request.get('/api/college/list', { params })
}
export function addCollege(data) {
  return request.post('/api/college/add', data)
}
export function updateCollege(data) {
  return request.put('/api/college/update', data)
}
export function deleteCollege(id) {
  return request.delete(`/api/college/delete/${id}`)
}

// ===== 专业 =====
export function fetchMajors(params) {
  return request.get('/api/major/list', { params })
}
export function addMajor(data) {
  return request.post('/api/major/add', data)
}
export function updateMajor(data) {
  return request.put('/api/major/update', data)
}
export function deleteMajor(id) {
  return request.delete(`/api/major/delete/${id}`)
}

// ===== 班级 =====
export function fetchClasses(params) {
  return request.get('/api/class/list', { params })
}
export function addClass(data) {
  return request.post('/api/class/add', data)
}
export function updateClass(data) {
  return request.put('/api/class/update', data)
}
export function deleteClass(id) {
  return request.delete(`/api/class/delete/${id}`)
}
