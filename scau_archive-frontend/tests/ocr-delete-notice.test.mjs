import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const source = readFileSync(new URL('../src/views/ocr/OCRProcess.vue', import.meta.url), 'utf8')
test('删除提示明确包含原文件和审查记录，保留已入库业务数据', () => {
  assert.ok(source.includes('识别日志、关联审查记录和原文件'))
  assert.ok(source.includes('已入库的业务数据不会删除'))
  assert.ok(source.includes('无法恢复'))
})
