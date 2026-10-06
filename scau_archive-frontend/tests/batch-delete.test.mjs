import { test } from 'node:test'
import assert from 'node:assert/strict'
import { ref } from 'vue'

const { useBatchDelete } = await import('../src/composables/useBatchDelete.js').catch(() => ({}))

function fixture(overrides = {}) {
  assert.equal(typeof useBatchDelete, 'function', '需要批量删除交互功能')
  const calls = []
  const options = {
    current: ref(3), pageSize: ref(15), total: ref(31),
    confirm: async count => calls.push(['confirm', count]),
    deleteRecords: async ids => { calls.push(['delete', ids]); return 1 },
    reload: async () => calls.push(['reload']),
    notify: (type, message) => calls.push([type, message]),
    ...overrides,
  }
  return { ...useBatchDelete(options), options, calls }
}

test('没有选择时不发删除请求', async () => {
  const f = fixture()
  await f.handleBatchDelete()
  assert.deepEqual(f.calls, [])
})

test('取消确认保留选择且不删除', async () => {
  const f = fixture({ confirm: async () => { throw 'cancel' } })
  f.selectedRows.value = [{ id: 11 }]
  await f.handleBatchDelete()
  assert.equal(f.selectedRows.value.length, 1)
  assert.deepEqual(f.calls, [])
  assert.equal(f.batchDeleting.value, false)
})

test('按选择删除并按实际删除数回退空页', async () => {
  const f = fixture()
  f.selectedRows.value = [{ id: 11 }, { id: 12 }]
  await f.handleBatchDelete()
  assert.deepEqual(f.calls.slice(0, 2), [['confirm', 2], ['delete', [11, 12]]])
  assert.equal(f.options.current.value, 2)
  assert.equal(f.selectedRows.value.length, 0)
  assert.ok(f.calls.some(([type]) => type === 'reload'))
  assert.equal(f.batchDeleting.value, false)
})

test('删除失败保留选择且不刷新', async () => {
  const f = fixture({ deleteRecords: async () => { throw new Error('failed') } })
  f.selectedRows.value = [{ id: 11 }]
  await f.handleBatchDelete()
  assert.equal(f.selectedRows.value.length, 1)
  assert.ok(f.calls.some(([type]) => type === 'error'))
  assert.ok(!f.calls.some(([type]) => type === 'reload'))
})

test('刷新失败不能误报删除失败', async () => {
  const f = fixture({ reload: async () => { throw new Error('reload failed') } })
  f.selectedRows.value = [{ id: 11 }]
  await f.handleBatchDelete()
  assert.ok(f.calls.some(([type]) => type === 'success'))
  assert.ok(f.calls.some(([type]) => type === 'warning'))
  assert.ok(!f.calls.some(([type]) => type === 'error'))
})
