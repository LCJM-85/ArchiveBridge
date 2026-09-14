<template>
  <div class="ai-assistant">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="chat-header">
          <div class="chat-title">
            <span class="ai-badge">
              <el-icon :size="16"><Reading /></el-icon>
            </span>
            <div>
              <div class="chat-name">AI 分析助手</div>
              <div class="chat-status"><span class="status-dot" :class="{ offline: aiOnline === false, checking: aiOnline === null }"></span>{{ aiOnline === null ? '正在检查服务状态' : aiOnline ? '分析服务可用' : '分析服务暂不可用' }}</div>
            </div>
          </div>
          <el-button text size="small" class="clear-btn" :disabled="loading || !messages.length" @click="clearChat">清空对话</el-button>
        </div>
      </template>

      <div class="message-list" ref="messageListRef">
        <div v-if="!messages.length && !loading" class="welcome">
          <div class="welcome-kicker">档案研究 / ASSISTANT</div>
          <h3 class="welcome-title">从一个问题，读懂档案。</h3>
          <p class="welcome-desc">围绕招生、学籍、毕业数据与已收录文档，辅助查询与分析。</p>
          <div class="welcome-suggests">
            <button v-for="q in suggestQuestions" :key="q" class="suggest-card" @click="askSuggest(q)">
              <span class="suggest-arrow">→</span>
              {{ q }}
            </button>
          </div>
          <div class="welcome-cap">
            <span><el-icon :size="14"><DataAnalysis /></el-icon>数据分析</span><span><el-icon :size="14"><Collection /></el-icon>文档检索</span>
          </div>
        </div>
        <div v-for="(msg, i) in messages" :key="i" class="message-row" :class="msg.role">
          <div class="avatar" :class="msg.role"><el-icon :size="18"><component :is="msg.role === 'user' ? UserFilled : Reading" /></el-icon></div>
          <div class="bubble" v-html="renderMarkdown(msg.content)"></div>
        </div>
        <div v-if="loading" class="message-row assistant">
          <div class="avatar assistant"><el-icon :size="18"><Reading /></el-icon></div>
          <div class="bubble loading">
            <span class="typing-dots"><i></i><i></i><i></i></span>
            {{ statusText || 'AI 正在分析数据…' }}
          </div>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="question"
          type="textarea"
          :rows="2"
          aria-label="档案分析问题"
          placeholder="描述你想了解的数据，建议注明年份、学院或专业…"
          :disabled="loading"
          @keydown.enter="handleEnter"
        />
        <el-button v-if="!loading" :disabled="!question.trim()" type="primary" @click="sendMessage" class="send-btn">
          发送
        </el-button>
        <el-button v-else type="danger" @click="cancelStream" class="send-btn">
          停止
        </el-button>
      </div>
      <div class="input-hint"><span>分析结果请结合原始档案核实</span><span>Enter 发送 · Shift + Enter 换行</span></div>
    </el-card>
  </div>
</template>

<script>
export default { name: 'AIAssistant' }
</script>
<script setup>
import { ref, nextTick, onActivated } from 'vue'
import { UserFilled, Reading, DataAnalysis, Collection } from '@element-plus/icons-vue'
import { sendChatMessageStream, checkAiStatus } from '@/api/modules/ai'

const messages = ref([])
const question = ref('')
const loading = ref(false)
const statusText = ref('')
const aiOnline = ref(null)
const messageListRef = ref(null)
let streamController = null

const suggestQuestions = [
  '今年录取人数最多的专业是哪个？',
  '近三年本科生平均录取分数线是多少？',
  '各学院的毕业生去向分布如何？',
  '帮我分析一下招生趋势的变化',
]

function handleEnter(event) {
  if (event.isComposing || event.keyCode === 229 || event.shiftKey) return
  event.preventDefault()
  sendMessage()
}

function askSuggest(q) {
  question.value = q
  sendMessage()
}

function renderMarkdown(text) {
  if (!text) return ''
  let html = text
    .replace(/### (.+)/g, '<h4>$1</h4>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

  // 先处理表格（保护起来，防止被 <br> 破坏）
  html = html.replace(/(^\|.+\|[\s\S]*?^\|.+\|)/gm, (match) => {
    const rows = match.trim().split('\n').filter(r => r.trim())
    let table = '<table>'
    for (const row of rows) {
      if (/^\|[\s\-:]+\|$/.test(row.trim())) continue
      const cells = row.split('|').filter(c => c.trim())
      if (cells.length) table += '<tr><td>' + cells.join('</td><td>') + '</td></tr>'
    }
    return table + '</table>'
  })

  // 剩下的 \n 转 <br>
  html = html.replace(/\n/g, '<br>')
  return html
}

async function sendMessage() {
  const q = question.value.trim()
  if (!q || loading.value) return

  messages.value.push({ role: 'user', content: q })
  question.value = ''
  loading.value = true
  statusText.value = '正在分析问题...'
  scrollToBottom()

  const history = messages.value.slice(0, -1).map(m => ({ role: m.role, content: m.content }))
  let msgIdx = -1

  streamController = sendChatMessageStream(
    { question: q, history },
    {
      onStatus(text) {
        statusText.value = text
        scrollToBottom()
      },
      onToken(text) {
        if (msgIdx === -1) {
          // 第一个 token，创建 assistant 消息
          msgIdx = messages.value.length
          messages.value.push({ role: 'assistant', content: '' })
        }
        statusText.value = ''
        messages.value[msgIdx].content += text
        scrollToBottom()
      },
      onDone() {
        loading.value = false
        statusText.value = ''
        scrollToBottom()
      },
      onError(msg) {
        if (msgIdx === -1) {
          messages.value.push({ role: 'assistant', content: msg })
        }
        loading.value = false
        statusText.value = ''
        scrollToBottom()
      },
    }
  )
}

function cancelStream() {
  if (streamController) {
    streamController.abort()
    streamController = null
  }
}

function clearChat() {
  messages.value = []
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

onActivated(() => {
  scrollToBottom()
  checkStatus()
})

async function checkStatus() {
  try {
    const res = await checkAiStatus()
    aiOnline.value = res?.data?.data?.available ?? false
  } catch {
    aiOnline.value = false
  }
}
</script>

<style scoped>
.ai-assistant { height: calc(100dvh - 164px); min-height: 520px; max-width: 1200px; margin: 0 auto; }
.chat-card { height: 100%; display: flex; flex-direction: column; }
.chat-card :deep(.el-card__header) { flex-shrink: 0; }
.chat-card :deep(.el-card__body) { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.chat-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.chat-title { display: flex; align-items: center; gap: 12px; }
.ai-badge { display: grid; place-items: center; color: var(--color-primary); background: var(--color-primary-light); width: 36px; height: 36px; border-radius: 6px; }
.chat-name { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.chat-status { display: flex; align-items: center; gap: 6px; color: var(--text-secondary); font-size: 11px; margin-top: 3px; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--color-primary); }
.status-dot.offline { background: var(--color-danger); }
.status-dot.checking { background: var(--text-tertiary); }
.message-list { flex: 1; min-height: 0; overflow-y: auto; padding: 12px 20px; }
.welcome { max-width: 640px; margin: 36px auto; padding: 16px 0; }
.welcome-kicker { font-size: 10px; color: var(--color-primary); letter-spacing: 2px; padding-bottom: 18px; border-bottom: 1px solid var(--border-color); }
.welcome-title { font-family: var(--font-display); font-weight: 400; font-size: 32px; line-height: 1.6; margin: 24px 0 12px; }
.welcome-desc { color: var(--text-secondary); font-size: 13px; line-height: 1.9; margin: 0 0 30px; }
.welcome-suggests { display: grid; grid-template-columns: 1fr 1fr; gap: 0 24px; }
.suggest-card { display: flex; align-items: baseline; gap: 10px; border: none; border-bottom: 1px solid var(--border-color); padding: 18px 0; background: transparent; color: var(--text-secondary); font-size: 13px; line-height: 1.8; text-align: left; cursor: pointer; }
.suggest-card:hover { color: var(--color-primary); background: var(--color-primary-light); }
.suggest-arrow { color: var(--color-primary); }
.welcome-cap { display: flex; gap: 20px; color: var(--text-tertiary); font-size: 12px; margin-top: 24px; }
.welcome-cap span { display: inline-flex; gap: 6px; align-items: center; }
.message-row { display: flex; gap: 12px; max-width: 800px; margin: 0 auto 24px; }
.message-row.user { flex-direction: row-reverse; }
.avatar { width: 30px; height: 30px; border-radius: 5px; display: grid; place-items: center; flex-shrink: 0; background: var(--bg-tertiary); color: var(--text-secondary); }
.avatar.assistant { background: var(--color-primary-light); color: var(--color-primary); }
.bubble { max-width: 85%; min-width: 0; padding: 12px 16px; font-size: 14px; line-height: 1.85; overflow-x: auto; overflow-wrap: anywhere; border-radius: 8px; }
.user .bubble { background: var(--color-primary-light); color: var(--text-primary); }
.assistant .bubble { background: var(--card-bg); border: 1px solid var(--border-light); }
.bubble.loading { color: var(--text-secondary); display: flex; gap: 10px; align-items: center; }
.typing-dots { display: flex; gap: 3px; }
.typing-dots i { width: 4px; height: 4px; background: var(--color-primary); border-radius: 50%; animation: blink 1.2s ease infinite; }
.typing-dots i:nth-child(2) { animation-delay: .2s; }
.typing-dots i:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%, 100% { opacity: .3; } 50% { opacity: 1; } }
.bubble :deep(h4) { margin: 12px 0 6px; font-size: 14px; }
.bubble :deep(table) { border-collapse: collapse; margin: 12px 0; width: 100%; font-size: 13px; }
.bubble :deep(td), .bubble :deep(th) { border: 1px solid var(--border-color); padding: 8px 12px; overflow-wrap: anywhere; }
.input-area { display: flex; gap: 12px; padding: 14px 0 0; border-top: 1px solid var(--border-color); flex-shrink: 0; align-items: flex-end; }
.input-area .el-textarea { flex: 1; }
.input-area :deep(.el-textarea__inner) { border-radius: 6px; background: var(--bg-primary); padding: 12px 14px; resize: none; }
.send-btn { height: 44px; width: 76px; border-radius: 6px; }
.input-hint { display: flex; justify-content: space-between; gap: 12px; color: var(--text-tertiary); font-size: 11px; margin-top: 10px; }
@media (max-width: 768px) {
  .ai-assistant { height: calc(100dvh - 132px); min-height: 440px; }
  .message-list { padding: 8px 0; }
  .welcome { margin: 10px auto; }
  .welcome-title { font-size: 25px; }
  .welcome-suggests { grid-template-columns: 1fr; }
  .suggest-card { padding: 12px 0; }
  .input-hint > span:last-child { display: none; }
  .bubble { max-width: calc(100% - 70px); padding: 10px; }
}
</style>
