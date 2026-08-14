import request from '../request'

export function syncOcrLogs() {
  return request.post('/api/ocr/log/sync')
}

export function fetchTodayOcrLogs() {
  return request.get('/api/ocr/log/today')
}

export function fetchOcrLogHistory(params) {
  return request.get('/api/ocr/log/history', { params })
}

export function deleteOcrLog(logId) {
  return request.delete(`/api/ocr/log/delete/${logId}`)
}

export function fetchQualityScores(fileIds) {
  return request.get('/api/quality-score/list', { params: { fileIds: fileIds.join(',') } })
}

export function fetchProcessingCount() {
  return request.get('/api/storage/processing-count')
}

export function cancelOcrTask(logId) {
  return request.post(`/api/ocr/log/${logId}/cancel`)
}
