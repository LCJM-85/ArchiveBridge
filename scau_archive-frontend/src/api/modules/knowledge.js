import request from '../request'

export function uploadKnowledgeFiles(formData) {
  return request.post('/api/knowledge/upload/file', formData, { timeout: 180000 })
}

export function uploadKnowledge(data) {
  return request.post('/api/knowledge/upload', data, { timeout: 180000 })
}

export function addKnowledgeUrl(data) {
  return request.post('/api/knowledge/url', data, { timeout: 180000 })
}

export function getKnowledgeList() {
  return request.get('/api/knowledge/list')
}

export function deleteKnowledge(id) {
  return request.delete(`/api/knowledge/${id}`)
}
