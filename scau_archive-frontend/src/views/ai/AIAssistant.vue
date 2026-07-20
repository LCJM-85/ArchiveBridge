<template>
  <div class="ai-assistant">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="chat-header">
          <span>AI 分析助手</span>
          <el-button text size="small" @click="clearChat">清空对话</el-button>
        </div>
      </template>

      <div class="message-list" ref="messageListRef">
        <div v-for="(msg, i) in messages" :key="i" class="message-row" :class="msg.role">
          <div class="avatar"><el-icon :size="20"><component :is="msg.role === 'user' ? UserFilled : Cpu" /></el-icon></div>
          <div class="bubble" v-html="renderMarkdown(msg.content)"></div>
        </div>
        <div v-if="loading" class="message-row assistant">
          <div class="avatar"><el-icon :size="20"><Cpu /></el-icon></div>
          <div class="bubble loading">{{ statusText || 'AI 正在分析数据…' }}</div>
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
.chat-card { height: 100%; display: flex; flex-direction: column; }
.chat-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-header { display: flex; justify-content: space-between; align-items: center; }
.message-list { flex: 1; overflow-y: auto; padding: 12px 0; }
.message-row { display: flex; gap: 10px; margin-bottom: 16px; }
.message-row.user { flex-direction: row-reverse; }
.avatar { width: 32px; height: 32px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.avatar svg { width: 20px; height: 20px; }
.bubble { max-width: 70%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; overflow-x: auto; }
.user .bubble { background: var(--color-primary); color: #fff; border-bottom-right-radius: 4px; }
.assistant .bubble { background: var(--el-fill-color-light); border-bottom-left-radius: 4px; }
.bubble.loading { color: var(--el-text-color-secondary); }
.bubble :deep(table) { border-collapse: collapse; margin: 8px 0; font-size: 13px; width: 100%; table-layout: fixed; }
.bubble :deep(td), .bubble :deep(th) { border: 1px solid var(--el-border-color); padding: 4px 8px; word-break: break-all; }
.input-area { display: flex; gap: 12px; align-items: flex-end; padding-top: 12px; border-top: 1px solid var(--el-border-color); }
.input-area .el-textarea { flex: 1; }
.send-btn { height: 48px; width: 80px; }
</style>
