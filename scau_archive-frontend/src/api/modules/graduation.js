import request from '../request'

export function fetchGraduationPage(params) {
  return request.get('/api/graduation/page', { params })
}

export function addGraduation(data) {
  return request.post('/api/graduation/add', data)
}

export function updateGraduation(data) {
  return request.put('/api/graduation/update', data)
}

export function deleteGraduation(id) {
  return request.delete(`/api/graduation/delete/${id}`)
}

export function fetchDegrees() {
  return request.get('/api/graduation/degrees')
}

export function fetchDestinations() {
  return request.get('/api/graduation/destinations')
}

export async function deleteGraduationBatch(ids) {
  const res = await request.delete('/api/graduation/delete/batch', { data: ids })
  if (res.data.code !== 200) throw new Error(res.data.msg || '批量删除失败')
  return res.data.data.deletedCount
}
