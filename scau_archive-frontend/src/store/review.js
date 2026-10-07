import { defineStore } from 'pinia'
import { ref } from 'vue'
export const useReviewStore = defineStore('review', () => {
  const logId = ref(null)
  return { logId }
})
