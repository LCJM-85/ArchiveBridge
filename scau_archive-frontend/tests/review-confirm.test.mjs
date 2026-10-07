import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const source=readFileSync(new URL('../src/views/review/ArchiveReview.vue',import.meta.url),'utf8')
const handler=source.match(/async function confirm\(\)\{[\s\S]*?\n\}/)[0]
function setup(failure, remoteRows) {
  const records=Array.from({length:45},(_,i)=>({_row_id:String(i),name:'记录'+i}))
  const draft={value:{draft_id:9,version:4,records,status:'pending_review'}}
  const dirty={value:true},busy={value:false}
  const calls=[],messages=[]
  const fn=new Function('draft','dirty','busy','ElMessageBox','confirmReview','getReview','ElMessage','load','errorMessage',`return (${handler})`)(
    draft,dirty,busy,{confirm:async()=>{}},
    async(id,body)=>{calls.push(['confirm',id,body]);if(failure)throw failure;return {...draft.value,status:'imported',version:6}},
    async id=>{calls.push(['reload',id]);return {...draft.value,records:remoteRows||draft.value.records,version:5,last_error:'原文件缺失'}},
    {success:message=>messages.push(message),error:message=>messages.push(message)},
    async()=>{calls.push(['list'])},error=>error.message)
  return {fn,draft,dirty,busy,calls,messages,records}
}
test('确认提交全部记录及当前版本，不只当前页',async()=>{
  const run=setup()
  await run.fn()
  assert.equal(run.calls[0][2].records.length,45)
  assert.equal(run.calls[0][2].version,4)
  assert.equal(run.draft.value.status,'imported')
  assert.equal(run.dirty.value,false)
  assert.equal(run.busy.value,false)
})
test('旧页面版本冲突保留本地修改，不用服务端数据覆盖',async()=>{
  const error=new Error('版本冲突')
  error.response={status:409,data:{data:{reason:'version_conflict'}}}
  const run=setup(error)
  await run.fn()
  assert.equal(run.calls.some(c=>c[0]==='reload'),false)
  assert.equal(run.dirty.value,true)
  assert.equal(run.draft.value.version,4)
  assert.equal(run.busy.value,false)
})
test('保存后的入库失败刷新已保存版本，允许继续修正重试',async()=>{
  const error=new Error('原文件缺失')
  error.response={status:400,data:{data:{reason:'review_failed'}}}
  const run=setup(error)
  await run.fn()
  assert.equal(run.calls.some(c=>c[0]==='reload'),true)
  assert.equal(run.draft.value.version,5)
  assert.equal(run.dirty.value,false)
})
test('保存结果未知时刷新元信息也不能覆盖本地修改',async()=>{
  const run=setup(new Error('连接中断'),[{_row_id:'0',name:'服务端旧记录'}])
  await run.fn()
  assert.equal(run.draft.value.records.length,45)
  assert.equal(run.draft.value.records[0].name,'记录0')
  assert.equal(run.dirty.value,true)
  assert.equal(run.draft.value.version,5)
})
