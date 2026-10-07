import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const source=readFileSync(new URL('../src/api/modules/ai.js',import.meta.url),'utf8')
const body=source.slice(source.indexOf('export function sendChatMessageStream'),source.indexOf('export function checkAiStatus')).replace('export function','function')
function harness(response){
  let requests=0
  const send=new Function('fetch','localStorage',`${body};return sendChatMessageStream`)(()=>{requests++;return Promise.resolve(response)}, {getItem(){return 'test'}})
  const events=[]
  let end
  const finished=new Promise(resolve=>end=resolve)
  const controller=send({question:'test'}, {onToken:t=>events.push(['token',t]),onDone:()=>{events.push(['done']);end()},onError:e=>{events.push(['error',e]);end()}})
  return {events,finished,controller,requests:()=>requests}
}
const tick=()=>new Promise(resolve=>setTimeout(resolve,10))
test('正文开始后不再追加独立的等待气泡',()=>{
  const page=readFileSync(new URL('../src/views/ai/AIAssistant.vue',import.meta.url),'utf8')
  assert.ok(page.includes('v-if="loading && !answerStarted"'))
})
test('done事件立即结束，不等待断开的连接，不重复提问',async()=>{
  let cancelled=0, reads=0
  const reader={read(){reads++;return reads===1 ? Promise.resolve({value:new TextEncoder().encode('data: {"type":"token","content":"answer"}\n\ndata: {"type":"done"}\n\n'),done:false}) : new Promise(()=>{})},cancel(){cancelled++;return Promise.resolve()},releaseLock(){}}
  const h=harness({ok:true,body:{getReader:()=>reader}})
  await tick()
  assert.deepEqual(h.events,[['token','answer'],['done']])
  assert.equal(reads,1);assert.equal(cancelled,1);assert.equal(h.requests(),1)
})
test('错误HTTP响应不能作为正常回答处理',async()=>{
  const h=harness({ok:false,status:401,body:{getReader(){throw Error('should not read')}}})
  await h.finished
  assert.match(h.events[0][1],/登录/)
})
test('停止后立即收尾一次，迟到正文不会追加',async()=>{
  let resolveRead
  const reader={read:()=>new Promise(resolve=>resolveRead=resolve),cancel:()=>Promise.resolve(),releaseLock(){}}
  const h=harness({ok:true,body:{getReader:()=>reader}})
  await tick();h.controller.abort();await tick()
  assert.deepEqual(h.events,[['done']])
  resolveRead({value:new TextEncoder().encode('data: {"type":"token","content":"late"}\n\n'),done:false})
  await tick();assert.deepEqual(h.events,[['done']])
})
