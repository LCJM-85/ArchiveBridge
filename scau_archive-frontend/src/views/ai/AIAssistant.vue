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
          <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="bubble" v-html="renderMarkdown(msg.content)"></div>
        </div>
        <div v-if="loading" class="message-row assistant">
          <div class="avatar">🤖</div>
          <div class="bubble loading">AI 正在分析数据…</div>
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
        <el-button type="primary" :loading="loading" @click="sendMessage" class="send-btn">
          发送
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { sendChatMessage } from '@/api/modules/ai'
import { useAiStore } from '@/store/ai'

const aiStore = useAiStore()
const { messages } = storeToRefs(aiStore)
const question = ref('')
const loading = ref(false)
const messageListRef = ref(null)

function renderMarkdown(text) {
  if (!text) return ''
  let html = text
    .replace(/### (.+)/g, '<h4>$1</h4>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
  html = html.replace(/\|(.+)\|/g, (match) => {
    const cells = match.split('|').filter(c => c.trim())
    if (cells.every(c => /^[\s\-]+$/.test(c))) return ''
    return '<tr><td>' + cells.join('</td><td>') + '</td></tr>'
  })
  html = html.replace(/((?:<tr>.*?<\/tr>\s*)+)/g, '<table>$1</table>')
  return html
}

async function sendMessage() {
  const q = question.value.trim()
  if (!q || loading.value) return

  messages.value.push({ role: 'user', content: q })
  question.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await sendChatMessage({
      question: q,
      history: messages.value.slice(0, -1).map(m => ({ role: m.role, content: m.content })),
    })
    const answer = res.data?.data?.answer || '抱歉，暂时无法回答这个问题'
    messages.value.push({ role: 'assistant', content: answer })
  } catch {
    messages.value.push({ role: 'assistant', content: 'AI 助手服务暂不可用，请稍后再试。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function clearChat() {
  aiStore.clearMessages()
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.ai-assistant { height: calc(100vh - 120px); }
.chat-card { height: 100%; display: flex; flex-direction: column; }
.chat-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-header { display: flex; justify-content: space-between; align-items: center; }
.message-list { flex: 1; overflow-y: auto; padding: 12px 0; }
.message-row { display: flex; gap: 10px; margin-bottom: 16px; }
.message-row.user { flex-direction: row-reverse; }
.avatar { width: 32px; height: 32px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; font-size: 18px; }
.bubble { max-width: 70%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; }
.user .bubble { background: var(--color-primary); color: #fff; border-bottom-right-radius: 4px; }
.assistant .bubble { background: var(--el-fill-color-light); border-bottom-left-radius: 4px; }
.bubble.loading { color: var(--el-text-color-secondary); }
.bubble :deep(table) { border-collapse: collapse; margin: 8px 0; font-size: 13px; width: 100%; }
.bubble :deep(td) { border: 1px solid var(--el-border-color); padding: 4px 8px; }
.input-area { display: flex; gap: 12px; align-items: flex-end; padding-top: 12px; border-top: 1px solid var(--el-border-color); }
.input-area .el-textarea { flex: 1; }
.send-btn { height: 48px; width: 80px; }
</style>
