import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useReportStore = defineStore('report', () => {
  const reportData = ref(null)
  const aiAnalysis = ref('')
  const aiAnalysisLoading = ref(false)
  const selectedYear = ref(new Date().getFullYear())
  const generateDate = ref('')

  function setReport(data, date) {
    reportData.value = data
    generateDate.value = date
  }

  function setAiAnalysis(text) {
    aiAnalysis.value = text
    aiAnalysisLoading.value = false
  }

  function setAiLoading(loading) {
    aiAnalysisLoading.value = loading
  }

  function setSelectedYear(year) {
    selectedYear.value = year
  }

  function reset() {
    reportData.value = null
    aiAnalysis.value = ''
    aiAnalysisLoading.value = false
    generateDate.value = ''
  }

  return {
    reportData,
    aiAnalysis,
    aiAnalysisLoading,
    selectedYear,
    generateDate,
    setReport,
    setAiAnalysis,
    setAiLoading,
    setSelectedYear,
    reset,
  }
})
