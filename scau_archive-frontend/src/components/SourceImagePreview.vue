<template>
  <div ref="viewport" class="image-viewport" :class="{zoomed:state.scale>1, dragging:!!drag}"
    @wheel="onWheel" @pointerdown="startDrag" @pointermove="moveDrag"
    @pointerup="endDrag" @pointercancel="endDrag" @lostpointercapture="endDrag" @dblclick="reset">
    <img ref="image" :src="src" :alt="alt" class="source-image" draggable="false" @load="reset"
      :style="{transform:`translate(${state.x}px, ${state.y}px) scale(${state.scale})`}"/>
    <span class="image-hint">滚轮缩放 · 放大后拖动 · 双击恢复全图</span>
  </div>
</template>
<script setup>
import {ref,watch,onMounted,onUnmounted} from 'vue'
import {clampImagePan,zoomAtPoint} from '@/utils/sourceImageZoom.mjs'
const props=defineProps({src:{type:String,required:true},alt:{type:String,default:'原文件预览'}})
const viewport=ref(null),image=ref(null),state=ref({scale:1,x:0,y:0}),drag=ref(null)
let resizeObserver
function sizes(){
  return {box:{width:viewport.value.clientWidth,height:viewport.value.clientHeight},
    image:{width:image.value.naturalWidth,height:image.value.naturalHeight}}
}
function endDrag(){
  const pointerId=drag.value?.pointerId
  drag.value=null
  if(pointerId!==undefined && viewport.value?.hasPointerCapture(pointerId))viewport.value.releasePointerCapture(pointerId)
}
function reset(){endDrag();state.value={scale:1,x:0,y:0}}
function onWheel(event){
  if(!image.value?.naturalWidth || !event.deltaY)return
  // 只拦截图片区域的滚轮，不阻止右侧表格和PDF正常滚动。
  event.preventDefault()
  endDrag()
  const rect=viewport.value.getBoundingClientRect(),size=sizes()
  state.value=zoomAtPoint(state.value,event.deltaY<0 ? 1.15 : 1/1.15,
    {x:event.clientX-rect.left-rect.width/2,y:event.clientY-rect.top-rect.height/2},size.box,size.image)
}
function startDrag(event){
  if(event.button!==0 || state.value.scale<=1)return
  event.preventDefault()
  drag.value={pointerId:event.pointerId,startX:event.clientX,startY:event.clientY,x:state.value.x,y:state.value.y}
  viewport.value.setPointerCapture(event.pointerId)
}
function moveDrag(event){
  if(!drag.value || event.pointerId!==drag.value.pointerId)return
  const size=sizes()
  state.value=clampImagePan({scale:state.value.scale,x:drag.value.x+event.clientX-drag.value.startX,
    y:drag.value.y+event.clientY-drag.value.startY},size.box,size.image)
}
watch(()=>props.src,reset)
onMounted(()=>{resizeObserver=new ResizeObserver(reset);resizeObserver.observe(viewport.value)})
onUnmounted(()=>{endDrag();resizeObserver?.disconnect()})
</script>
<style scoped>
.image-viewport{position:relative;width:100%;height:100%;overflow:hidden;touch-action:none;user-select:none}
.source-image{display:block;width:100%;height:100%;object-fit:contain;transform-origin:center;pointer-events:none}
.zoomed{cursor:grab}.dragging{cursor:grabbing}
.image-hint{position:absolute;bottom:8px;left:50%;transform:translateX(-50%);padding:3px 8px;border-radius:4px;font-size:12px;white-space:nowrap;color:#fff;background:rgba(0,0,0,.55);pointer-events:none;opacity:0}
.image-viewport:hover .image-hint{opacity:1}
</style>
