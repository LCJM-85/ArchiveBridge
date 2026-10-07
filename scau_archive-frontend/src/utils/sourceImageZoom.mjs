export function clampImagePan(state, viewport, image) {
  if (state.scale <= 1 || !viewport.width || !viewport.height || !image.width || !image.height) {
    return {...state,x:0,y:0}
  }
  const fit=Math.min(viewport.width/image.width,viewport.height/image.height)
  const maxX=Math.max(0,(image.width*fit*state.scale-viewport.width)/2)
  const maxY=Math.max(0,(image.height*fit*state.scale-viewport.height)/2)
  return {...state,x:Math.max(-maxX,Math.min(maxX,state.x)),y:Math.max(-maxY,Math.min(maxY,state.y))}
}

export function zoomAtPoint(state, factor, anchor, viewport, image) {
  const scale=Math.max(1,Math.min(4,state.scale*factor))
  const ratio=scale/state.scale
  return clampImagePan({scale,x:anchor.x-(anchor.x-state.x)*ratio,y:anchor.y-(anchor.y-state.y)*ratio},viewport,image)
}
