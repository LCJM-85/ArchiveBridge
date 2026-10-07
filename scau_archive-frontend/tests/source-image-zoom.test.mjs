import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

let geometry
try { geometry = await import('../src/utils/sourceImageZoom.mjs') } catch {}
const viewport={width:500,height:400}, image={width:1000,height:800}
function ready(){assert.equal(typeof geometry?.zoomAtPoint,'function','需要图片缩放计算逻辑')}

test('放大以鼠标所在位置为中心',()=>{
  ready()
  const result=geometry.zoomAtPoint({scale:1,x:0,y:0},2,{x:100,y:50},viewport,image)
  assert.deepEqual(result,{scale:2,x:-100,y:-50})
  assert.equal(100*result.scale+result.x,100)
})
test('缩放有上限，缩回默认时恢复居中全图',()=>{
  ready()
  assert.equal(geometry.zoomAtPoint({scale:4,x:0,y:0},2,{x:0,y:0},viewport,image).scale,4)
  assert.deepEqual(geometry.zoomAtPoint({scale:2,x:100,y:50},0.1,{x:0,y:0},viewport,image),{scale:1,x:0,y:0})
})
test('拖动不能把放大的图片移出可视区域',()=>{
  ready()
  assert.deepEqual(geometry.clampImagePan({scale:2,x:999,y:-999},viewport,image),{scale:2,x:250,y:-200})
  assert.deepEqual(geometry.clampImagePan({scale:2,x:100,y:100},viewport,{width:1000,height:2000}),{scale:2,x:0,y:100})
})
test('审查页使用独立图片缩放组件，不影响PDF和右侧表格',()=>{
  const page=readFileSync(new URL('../src/views/review/ArchiveReview.vue',import.meta.url),'utf8')
  assert.ok(page.includes('<SourceImagePreview'))
  assert.ok(page.includes('class="source-pdf"'))
})
