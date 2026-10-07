import request from '@/api/request'

async function unwrap(promise) {
  const response = await promise
  if (response.data.code !== 200) throw new Error(response.data.msg || '审查操作失败')
  return response.data.data
}
export const listReviews = params => unwrap(request.get('/api/review', { params }))
export const getReview = id => unwrap(request.get('/api/review/' + id))
export const saveReview = (id, body) => unwrap(request.put('/api/review/' + id, body))
export const confirmReview = (id, body) => unwrap(request.post('/api/review/' + id + '/confirm', body))
export const discardReview = (id, version) => unwrap(request.post('/api/review/' + id + '/discard', { version }))
export const getReviewSource = id => request.get('/api/review/' + id + '/source', { responseType: 'blob' })
