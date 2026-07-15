import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useAiStore = defineStore('ai', () => {
  const messages = ref([])

  function addMessage(msg) {
    messages.value.push(msg)
  }

  function clearMessages() {
    messages.value = []
  }

  return {
    messages,
    addMessage,
    clearMessages,
  }
})
