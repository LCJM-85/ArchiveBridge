<template>
  <div class="ai-assistant">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="chat-header">
          <div class="chat-title">
            <span class="ai-badge">
              <el-icon :size="16"><Cpu /></el-icon>
            </span>
            <div>
              <div class="chat-name">AI 分析助手</div>
              <div class="chat-status"><span class="status-dot"></span>在线 · RAG 知识库已接入</div>
            </div>
          </div>
          <el-button text size="small" class="clear-btn" @click="clearChat">清空对话</el-button>
        </div>
      </template>

      <div class="message-list" ref="messageListRef">
        <div v-for="(msg, i) in messages" :key="i" class="message-row" :class="msg.role">
          <div class="avatar" :class="msg.role"><el-icon :size="18"><component :is="msg.role === 'user' ? UserFilled : Cpu" /></el-icon></div>
          <div class="bubble" v-html="renderMarkdown(msg.content)"></div>
        </div>
        <div v-if="loading" class="message-row assistant">
          <div class="avatar assistant"><el-icon :size="18"><Cpu /></el-icon></div>
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
          placeholder="输入你的问题，例如：今年录取人数最多的专业是哪个？"
          :disabled="loading"
          @keydown.enter.prevent="sendMessage"
        />
        <el-button v-if="!loading" type="primary" @click="sendMessage" class="send-btn">
          发送
        </el-button>
        <el-button v-else type="danger" @click="cancelStream" class="send-btn">
          停止
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
export default { name: 'AIAssistant' }
</script>
<script setup>
import { ref, nextTick, onActivated } from 'vue'
import { UserFilled, Cpu } from '@element-plus/icons-vue'
import { sendChatMessageStream } from '@/api/modules/ai'

const messages = ref([])
const question = ref('')
const loading = ref(false)
const statusText = ref('')
const messageListRef = ref(null)
let streamController = null

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
})
</script>

<style scoped>
.ai-assistant { height: calc(100vh - 120px); }
.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 20px !important;
}
.chat-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

/* ===== 聊天头部 ===== */
.chat-header { display: flex; justify-content: space-between; align-items: center; }
.chat-title { display: flex; align-items: center; gap: 12px; }
.ai-badge {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(140deg, #14a06f, #0b5c40);
  border: 1px solid rgba(201, 164, 92, 0.5);
  color: #e6cd95;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(6, 40, 28, 0.3);
}
.chat-name {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--text-primary);
}
.chat-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2fb984;
  box-shadow: 0 0 8px rgba(47, 185, 132, 0.8);
  animation: pulse 2s ease infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}
.clear-btn { color: var(--text-tertiary) !important; }
.clear-btn:hover { color: var(--color-danger) !important; }

/* ===== 消息区 ===== */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 4px;
  background:
    radial-gradient(500px 220px at 90% 0%, rgba(14, 138, 95, 0.045), transparent 60%),
    var(--card-bg);
}
.message-row { display: flex; gap: 10px; margin-bottom: 18px; animation: msg-in 0.35s ease both; }
.message-row.user { flex-direction: row-reverse; }
@keyframes msg-in {
  from { transform: translateY(10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}
.avatar.user {
  background: linear-gradient(140deg, var(--color-gold), var(--color-gold-dark));
  color: #1d1608;
  border: 1px solid rgba(201, 164, 92, 0.5);
}
.avatar.assistant {
  background: linear-gradient(140deg, #14a06f, #0b5c40);
  color: #e6cd95;
  border: 1px solid rgba(201, 164, 92, 0.4);
  box-shadow: 0 4px 10px rgba(6, 40, 28, 0.25);
}

.bubble {
  max-width: 70%;
  padding: 11px 15px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  overflow-x: auto;
  box-shadow: 0 3px 10px rgba(7, 39, 28, 0.05);
}
.user .bubble {
  background: linear-gradient(135deg, #14a06f, #0b5c40);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.assistant .bubble {
  background: var(--bg-tertiary);
  border-bottom-left-radius: 4px;
  border: 1px solid var(--border-light);
}
.bubble.loading {
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 8px;
}
.typing-dots i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-gold);
  display: inline-block;
  margin-right: 3px;
  animation: blink 1.2s ease infinite;
}
.typing-dots i:nth-child(2) { animation-delay: 0.2s; }
.typing-dots i:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
}

.bubble :deep(h4) {
  margin: 8px 0 4px;
  font-size: 14px;
  color: var(--color-primary);
}
.bubble :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  font-size: 13px;
  width: 100%;
  table-layout: fixed;
}
.bubble :deep(td), .bubble :deep(th) {
  border: 1px solid var(--border-color);
  padding: 4px 8px;
  word-break: break-all;
}
.bubble :deep(th) { background: var(--bg-tertiary); }

/* ===== 输入区 ===== */
.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 14px 4px 2px;
  border-top: 1px dashed var(--border-color);
}
.input-area .el-textarea { flex: 1; }
.input-area :deep(.el-textarea__inner) {
  border-radius: 12px !important;
  background: var(--bg-tertiary) !important;
  border-color: var(--border-light) !important;
  box-shadow: none !important;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}
.input-area :deep(.el-textarea__inner:focus) {
  border-color: var(--color-gold) !important;
  box-shadow: 0 0 0 3px rgba(201, 164, 92, 0.12) !important;
}
.send-btn {
  height: 48px;
  width: 84px;
  border-radius: 12px !important;
  background: linear-gradient(135deg, var(--color-gold), var(--color-gold-dark)) !important;
  border: none !important;
  color: #1d1608 !important;
  font-weight: 600;
}
.send-btn:hover {
  filter: brightness(1.08);
  box-shadow: 0 6px 16px rgba(201, 164, 92, 0.4);
}
.send-btn.el-button--danger {
  background: linear-gradient(135deg, #f56c6c, #c94f4f) !important;
  color: #fff !important;
}
</style>
