import request from '../request'

export function sendChatMessage(data) {
  return request.post('/api/ai/chat', data, { timeout: 120000 })
}

export function sendChatMessageStream(data, { onStatus, onToken, onDone, onError }) {
  const token = localStorage.getItem('token')
  const controller = new AbortController()

  fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(data),
    signal: controller.signal,
  }).then(async (response) => {
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed.startsWith('data:')) continue
        try {
          const event = JSON.parse(trimmed.slice(5))
          switch (event.type) {
            case 'status':
              onStatus?.(event.content)
              break
            case 'token':
              onToken?.(event.content)
              break
            case 'error':
              onError?.(event.content)
              break
            case 'done':
              onDone?.()
              break
          }
        } catch {}
      }
    }
    onDone?.() // 流自然结束时确保回调
  }).catch((err) => {
    if (err.name !== 'AbortError') {
      onError?.('AI 助手服务暂不可用，请稍后再试')
    } else {
      onDone?.() // 用户手动中断，清理 loading 状态
    }
  })

  return controller
}

export function checkAiStatus() {
  return request.get('/api/ai/status')
}

export function analyzeReport(data) {
  return request.post('/api/ai/analyze-report', data)
}
