import request from '../request'

export function fetchMetaDataPage(params) {
  return request.get('/metadata/page', { params })
}

export function addMetaData(data) {
  return request.post('/metadata/add', data)
}

export function updateMetaData(data) {
  return request.put('/metadata/update', data)
}

export function deleteMetaData(metadataId) {
  return request.delete('/metadata/delete', { params: { metadataId } })
}
