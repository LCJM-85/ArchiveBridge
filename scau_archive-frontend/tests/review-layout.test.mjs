import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/views/review/ArchiveReview.vue', import.meta.url), 'utf8')
test('审查筛选栏有留白和独立操作分组', () => {
  assert.match(source, /\.filter-bar\s*\{[^}]*padding:\s*16px 20px/)
  assert.ok(source.includes('class="review-filter-actions"'))
  assert.ok(source.includes('placeholder="档案类型"'))
})
test('默认和重置均展示全部状态', () => {
  assert.ok(source.includes("const status=ref(''),archiveType=ref('')"))
  assert.match(source, /function reset\(\)\{[^}]*status\.value='';/)
})
test('审查弹窗限宽限高，表格随窗口高度调整', () => {
  assert.match(source, /class="review-dialog"/)
  assert.match(source, /width="min\(1800px, 96vw\)"/)
  assert.match(source, /max-height:\s*92vh/)
  assert.equal((source.match(/:height="reviewTableHeight"/g)||[]).length, 2)
  assert.match(source, /clamp\(260px, 50vh, 560px\)/)
  assert.match(source, /const rowPageSize=ref\(30\)/)
  assert.match(source, /:page-sizes="\[20,30,50\]"/)
})
test('原文件占半屏，图片完整适应可视区域且没有图片滚动条', () => {
  const imageComponent=readFileSync(new URL('../src/components/SourceImagePreview.vue',import.meta.url),'utf8')
  assert.match(source, /<SourceImagePreview/)
  assert.match(imageComponent, /\.source-image\s*\{[^}]*width:\s*100%/)
  assert.match(imageComponent, /\.source-image\s*\{[^}]*height:\s*100%/)
  assert.match(imageComponent, /\.source-image\s*\{[^}]*object-fit:\s*contain/)
  assert.ok(source.includes('class="review-workspace"'))
  assert.ok(source.includes('class="source-pane"'))
  assert.ok(!source.includes('class="source-dialog"'))
  assert.match(source, /grid-template-columns:minmax\(0, 1fr\) minmax\(0, 1fr\)/)
  assert.ok(!source.includes('sourceZoom'))
  assert.ok(!source.includes('source-zoom-controls'))
  assert.match(source, /\.source-content\s*\{[^}]*overflow:hidden/)
  assert.match(source, /\.source-content\s*\{[^}]*height:clamp\(260px, 50vh, 560px\)/)
  assert.match(source, /@media\(max-width:1100px\)/)
})

test('关闭、切换文件及迟到请求不会残留原文件预览', () => {
  assert.match(source, /@closed="closeSource"/)
  assert.match(source, /async function open\(id\)\{\s*closeSource\(\)/)
  assert.match(source, /requestId!==sourceRequestId/)
  assert.match(source, /draft\.value\?\.draft_id!==draftId/)
  assert.match(source, /URL\.revokeObjectURL\(sourceUrl\.value\)/)
  assert.match(source, /onUnmounted\(closeSource\)/)
})

function previewHarness() {
  const state = {
    draft: { value: { draft_id: 1 } },
    sourceVisible: { value: false }, sourceLoading: { value: false },
    sourceUrl: { value: '' }, sourceType: { value: '' },
  }
  const pending = [], created = [], revoked = []
  const functions = source.slice(source.indexOf('function releaseSource()'), source.indexOf('onActivated(load)'))
  const make = new Function(...Object.keys(state), 'getReviewSource', 'URL', 'ElMessage', 'errorMessage',
    `let sourceRequestId=0;${functions};return {preview,closeSource}`)
  const actions = make(...Object.values(state), () => new Promise(resolve => pending.push(resolve)), {
    createObjectURL(blob) { created.push(blob);return 'blob:test' },
    revokeObjectURL(url) { revoked.push(url) },
  }, {error(){assert.fail('unexpected preview error')}}, e => e.message)
  return {state,pending,created,revoked,...actions}
}

test('收起分屏后迟到的原文件响应不会创建 URL', async () => {
  const h=previewHarness()
  const loading=h.preview()
  assert.equal(h.state.sourceLoading.value,true)
  h.closeSource()
  h.pending[0]({data:{type:'image/png'}})
  await loading
  assert.equal(h.created.length,0)
  assert.equal(h.state.sourceVisible.value,false)
  assert.equal(h.state.sourceLoading.value,false)
})

test('切换草稿忽略旧响应，正常收起会释放已创建 URL', async () => {
  const h=previewHarness()
  const old=h.preview()
  h.state.draft.value={draft_id:2}
  h.pending[0]({data:{type:'image/png'}})
  await old
  assert.equal(h.created.length,0)
  const current=h.preview()
  h.pending[1]({data:{type:'application/pdf'}})
  await current
  assert.equal(h.created.length,1)
  assert.equal(h.state.sourceType.value,'application/pdf')
  h.closeSource()
  assert.deepEqual(h.revoked,['blob:test'])
  assert.equal(h.state.sourceUrl.value,'')
})
