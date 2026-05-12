import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchMetaDataPage, addMetaData, updateMetaData, deleteMetaData } from '@/api/modules/metadata'

export const useMetaDataStore = defineStore('metadata', () => {
  const tableData = ref([])
  const loading = ref(false)
  const current = ref(1)
  const pageSize = ref(15)
  const total = ref(0)
  const pages = ref(0)
  const keyword = ref('')

  async function fetchPage() {
    loading.value = true
    try {
      const params = { current: current.value, size: pageSize.value }
      if (keyword.value) {
        params.keyword = keyword.value
      }
      const res = await fetchMetaDataPage(params)
      const d = res.data.data || {}
      tableData.value = d.records || []
      total.value = d.total || 0
      current.value = d.current || 1
      pageSize.value = d.size || 15
      pages.value = d.pages || 0
    } finally {
      loading.value = false
    }
  }

  async function add(data) {
    const res = await addMetaData(data)
    return res.data
  }

  async function update(data) {
    const res = await updateMetaData(data)
    return res.data
  }

  async function remove(metadataId) {
    const res = await deleteMetaData(metadataId)
    return res.data
  }

  function setPage(p) {
    current.value = p
  }

  function search(val) {
    keyword.value = val
    current.value = 1
    fetchPage()
  }

  return {
    tableData, loading, current, pageSize, total, pages, keyword,
    fetchPage, add, update, remove, setPage, search
  }
})
