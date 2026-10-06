import request from '../request'

export function fetchStudentPage(params) {
  return request.get('/api/student/page', { params })
}

export function addStudent(data) {
  return request.post('/api/student/add', data)
}

export function updateStudent(data) {
  return request.put('/api/student/update', data)
}

export function deleteStudent(id) {
  return request.delete(`/api/student/delete/${id}`)
}

export function fetchProvinces() {
  return request.get('/api/student/provinces')
}

export function fetchMajors() {
  return request.get('/api/student/majors')
}

export function fetchClasses() {
  return request.get('/api/student/classes')
}

export function fetchDegrees() {
  return request.get('/api/student/degrees')
}

export async function deleteStudentBatch(ids) {
  const res = await request.delete('/api/student/delete/batch', { data: ids })
  if (res.data.code !== 200) throw new Error(res.data.msg || '批量删除失败')
  return res.data.data.deletedCount
}
