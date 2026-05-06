import request from '../request'

export function fetchAdmissionPage(params) {
  return request.get('/api/admission/page', { params })
}

export function addAdmission(data) {
  return request.post('/api/admission/add', data)
}

export function updateAdmission(data) {
  return request.put('/api/admission/update', data)
}

export function deleteAdmission(id) {
  return request.delete(`/api/admission/delete/${id}`)
}

export function fetchProvinces() {
  return request.get('/api/admission/provinces')
}

export function fetchMajors() {
  return request.get('/api/admission/majors')
}
