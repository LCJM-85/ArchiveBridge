import request from '../request'

export function sendChatMessage(data) {
  return request.post('/api/ai/chat', data, { timeout: 120000 })
}

export function sendChatMessageStream(data, { onStatus, onToken, onDone, onError }) {
  const token = localStorage.getItem('token')
  const controller = new AbortController()
  let finished = false
  let reader
  function finish(error) {
    if (finished) return
    finished = true
    if (error) onError?.(error)
    else onDone?.()
  }
  controller.signal.addEventListener('abort', () => {
    finish()
    reader?.cancel().catch(() => {})
  }, { once: true })

  fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(data),
    signal: controller.signal,
  }).then(async (response) => {
    if (finished) { await response.body?.cancel(); return }
    if (!response.ok) {
      finish(response.status === 401 ? '登录已失效，请重新登录' : `AI 请求失败（${response.status}），请稍后重试`)
      await response.body?.cancel?.()
      return
    }
    if (!response.body) { finish('AI 服务未返回回答流'); return }
    reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    function dispatch(line) {
      const trimmed = line.trim()
      if (!trimmed.startsWith('data:') || finished) return
      let event
      try { event = JSON.parse(trimmed.slice(5)) } catch { return }
      switch (event.type) {
        case 'status': onStatus?.(event.content); break
        case 'token': onToken?.(event.content); break
        case 'error': finish(event.content || 'AI 回答失败'); break
        case 'done': finish(); break
      }
    }
    try {
    while (!finished) {
      const { done, value } = await reader.read()
      if (finished) break
      if (done) {
        buffer += decoder.decode()
        buffer.split('\n').forEach(dispatch)
        finish()
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) dispatch(line)
    }
    } finally {
      await reader.cancel().catch(() => {})
      reader.releaseLock()
    }
  }).catch((err) => {
    finish(err.name === 'AbortError' ? null : 'AI 助手服务暂不可用，请稍后再试')
  })

  return controller
}

export function checkAiStatus() {
  return request.get('/api/ai/status')
}

export function analyzeReport(data) {
  return request.post('/api/ai/analyze-report', data)
}
