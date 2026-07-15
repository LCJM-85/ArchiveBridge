import request from '../request'

export function sendChatMessage(data) {
  return request.post('/api/ai/chat', data)
}

export function checkAiStatus() {
  return request.get('/api/ai/status')
}

export function analyzeReport(data) {
  return request.post('/api/ai/analyze-report', data)
}
