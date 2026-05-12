import request from '../request'

export function syncOcrLogs() {
  return request.post('/ocr/log/sync')
}

export function fetchTodayOcrLogs() {
  return request.get('/ocr/log/today')
}

export function fetchOcrLogHistory(params) {
  return request.get('/ocr/log/history', { params })
}

export function deleteOcrLog(logId) {
  return request.delete(`/ocr/log/delete/${logId}`)
}

export function fetchQualityScores(fileIds) {
  return request.get('/api/quality-score/list', { params: { fileIds: fileIds.join(',') } })
}

export function fetchProcessingCount() {
  return request.get('/storage/processing-count')
}
