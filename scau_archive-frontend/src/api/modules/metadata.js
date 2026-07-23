import request from '../request'

export function fetchMetaDataPage(params) {
  return request.get('/api/metadata/page', { params })
}

export function addMetaData(data) {
  return request.post('/api/metadata/add', data)
}

export function updateMetaData(data) {
  return request.put('/api/metadata/update', data)
}

export function deleteMetaData(metadataId) {
  return request.delete('/api/metadata/delete', { params: { metadataId } })
}
