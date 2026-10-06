import { ref } from 'vue'

export function useBatchDelete({ current, pageSize, total, confirm, deleteRecords, reload, notify }) {
  const selectedRows = ref([])
  const batchDeleting = ref(false)

  async function handleBatchDelete() {
    if (batchDeleting.value || !selectedRows.value.length) return
    const ids = selectedRows.value.map(row => row.id)
    batchDeleting.value = true
    try {
      try {
        await confirm(ids.length)
      } catch {
        return
      }
      let deletedCount
      try {
        deletedCount = await deleteRecords(ids)
      } catch (e) {
        notify('error', e.response?.data?.msg || e.message || '批量删除失败')
        return
      }
      selectedRows.value = []
      current.value = Math.min(current.value, Math.max(1, Math.ceil((total.value - deletedCount) / pageSize.value)))
      notify('success', `已删除 ${deletedCount} 条记录`)
      try {
        await reload()
      } catch {
        notify('warning', '删除已完成，但列表刷新失败，请手动刷新')
      }
    } finally {
      batchDeleting.value = false
    }
  }

  return { selectedRows, batchDeleting, handleBatchDelete }
}
