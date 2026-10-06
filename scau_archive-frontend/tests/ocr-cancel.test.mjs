import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

// 隔离网络和弹窗，直接执行组件中实际的取消处理函数。
const source = readFileSync(new URL('../src/views/ocr/OCRProcess.vue', import.meta.url), 'utf8')
const handler = source.match(/async function handleCancelTask\(logId\) \{[\s\S]*?\n\}/)[0]
function setup(response, requestError) {
  const notices = []
  let refreshed = 0
  const fn = new Function('ElMessageBox', 'cancelOcrTask', 'ElMessage', 'fetchToday', `return (${handler})`)(
    { confirm: async () => {} },
    async () => { if (requestError) throw requestError; return response },
    { success: msg => notices.push(['success', msg]), warning: msg => notices.push(['warning', msg]), error: msg => notices.push(['error', msg]) },
    async () => { refreshed++ },
  )
  return { fn, notices, refreshed: () => refreshed }
}

test('入库阶段拒绝取消时不显示成功', async () => {
  const run = setup({ data: { code: 400, msg: '已进入入库阶段，无法取消' } })
  await run.fn(1)
  assert.deepEqual(run.notices, [['warning', '已进入入库阶段，无法取消']])
  assert.equal(run.refreshed(), 1)
})

test('后端确认取消后才显示成功', async () => {
  const run = setup({ data: { code: 200 } })
  await run.fn(1)
  assert.deepEqual(run.notices, [['success', '任务已取消']])
  assert.equal(run.refreshed(), 1)
})

test('网络异常给出错误提示', async () => {
  const run = setup(null, new Error('连接失败'))
  await run.fn(1)
  assert.deepEqual(run.notices, [['error', '连接失败']])
})
