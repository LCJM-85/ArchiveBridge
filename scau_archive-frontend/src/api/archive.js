import request from '../utils/request'

export function uploadFiles(files, type, archiveType) {
  const formData = new FormData()
  files.forEach((f) => formData.append('files', f))
  formData.append('type', type)
  formData.append('archiveType', archiveType)
  return request.post('/api/upload', formData, {
    timeout: 120000,
  })
}
