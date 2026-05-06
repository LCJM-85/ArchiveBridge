import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchStudentPage, addStudent, updateStudent, deleteStudent, fetchProvinces, fetchMajors, fetchClasses } from '@/api/modules/student'

export const useStudentStore = defineStore('student', () => {
  const tableData = ref([])
  const loading = ref(false)
  const current = ref(1)
  const pageSize = ref(15)
  const total = ref(0)
  const pages = ref(0)
  const keyword = ref('')
  const createTimeRange = ref([])
  const updateTimeRange = ref([])

  const provinces = ref([])
  const majors = ref([])
  const classes = ref([])

  async function fetchPage() {
    loading.value = true
    try {
      const params = { current: current.value, size: pageSize.value }
      if (keyword.value) params.keyword = keyword.value
      if (createTimeRange.value && createTimeRange.value.length === 2) {
        params.createTimeStart = createTimeRange.value[0]
        params.createTimeEnd = createTimeRange.value[1]
      }
      if (updateTimeRange.value && updateTimeRange.value.length === 2) {
        params.updateTimeStart = updateTimeRange.value[0]
        params.updateTimeEnd = updateTimeRange.value[1]
      }
      const res = await fetchStudentPage(params)
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
    } catch { provinces.value = [] }
  }

  async function fetchMajorsList() {
    try {
      const res = await fetchMajors()
      majors.value = res.data.data || []
    } catch { majors.value = [] }
  }

  async function fetchClassesList() {
    try {
      const res = await fetchClasses()
      classes.value = res.data.data || []
    } catch { classes.value = [] }
  }

  async function add(data) {
    const res = await addStudent(data)
    return res.data
  }

  async function update(data) {
    const res = await updateStudent(data)
    return res.data
  }

  async function remove(id) {
    const res = await deleteStudent(id)
    return res.data
  }

  function setPage(p) { current.value = p }

  function search(val) {
    keyword.value = val
    current.value = 1
    fetchPage()
  }

  function clearTimeRanges() {
    createTimeRange.value = []
    updateTimeRange.value = []
  }

  return {
    tableData, loading, current, pageSize, total, pages, keyword,
    createTimeRange, updateTimeRange, provinces, majors, classes,
    fetchPage, fetchProvincesList, fetchMajorsList, fetchClassesList,
    add, update, remove, setPage, search, clearTimeRanges
  }
})
