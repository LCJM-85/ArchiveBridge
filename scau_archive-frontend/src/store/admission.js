import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchAdmissionPage, addAdmission, updateAdmission, deleteAdmission, fetchProvinces, fetchMajors } from '@/api/modules/admission'

export const useAdmissionStore = defineStore('admission', () => {
  const tableData = ref([])
  const loading = ref(false)
  const current = ref(1)
  const pageSize = ref(15)
  const total = ref(0)
  const pages = ref(0)
  const keyword = ref('')

  const provinces = ref([])
  const majors = ref([])

  async function fetchPage() {
    loading.value = true
    try {
      const params = { current: current.value, size: pageSize.value }
      if (keyword.value) {
        params.keyword = keyword.value
      }
      const res = await fetchAdmissionPage(params)
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

  async function fetchProvincesList() {
    try {
      const res = await fetchProvinces()
      provinces.value = res.data.data || []
    } catch {
      provinces.value = []
    }
  }

  async function fetchMajorsList() {
    try {
      const res = await fetchMajors()
      majors.value = res.data.data || []
    } catch {
      majors.value = []
    }
  }

  async function add(data) {
    const res = await addAdmission(data)
    return res.data
  }

  async function update(data) {
    const res = await updateAdmission(data)
    return res.data
  }

  async function remove(id) {
    const res = await deleteAdmission(id)
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
    provinces, majors,
    fetchPage, fetchProvincesList, fetchMajorsList,
    add, update, remove, setPage, search
  }
})
