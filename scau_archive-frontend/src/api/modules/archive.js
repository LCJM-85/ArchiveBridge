import request from '../request'

export function uploadFiles(files, type, archiveType, provinceName, admissionDate, degreeName, useLlm = false) {
  const formData = new FormData()
  files.forEach((f) => formData.append('files', f))
  formData.append('type', type)
  formData.append('archiveType', archiveType)
  if (provinceName) {
    formData.append('provinceName', provinceName)
  }
  if (admissionDate) {
    formData.append('admissionDate', admissionDate)
  }
  if (degreeName) {
    formData.append('degreeName', degreeName)
  }
  if (useLlm) {
    formData.append('useLlm', 'true')
  }
  return request.post('/api/upload', formData, {
    timeout: 1000000,
  })
}
