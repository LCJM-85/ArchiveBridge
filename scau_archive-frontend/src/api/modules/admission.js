import request from '../request'

export function fetchAdmissionPage(params) {
  return request.get('/api/admission/page', { params })
}

export function addAdmission(data) {
  return request.post('/api/admission/add', data)
}

export function updateAdmission(data) {
  return request.put('/api/admission/update', data)
}

export function deleteAdmission(id) {
  return request.delete(`/api/admission/delete/${id}`)
}

export function fetchProvinces() {
  return request.get('/api/admission/provinces')
}

export function fetchMajors() {
  return request.get('/api/admission/majors')
}

export function fetchDegrees() {
  return request.get('/api/admission/degrees')
}

export function fetchTrendYearly(params) {
  return request.get('/api/admission/trend/yearly', { params })
}

export function fetchTrendMajor(params) {
  return request.get('/api/admission/trend/major', { params })
}

export function fetchTrendProvince(params) {
  return request.get('/api/admission/trend/province', { params })
}

export function fetchTrendScore(params) {
  return request.get('/api/admission/trend/score', { params })
}

export function fetchTrendGender(params) {
  return request.get('/api/admission/trend/gender', { params })
}

export function fetchProvinceStats() {
  return request.get('/api/admission/geo/province-stats')
}

export function fetchSankeyData() {
  return request.get('/api/admission/training-path/sankey')
}

export function fetchPrediction(years = 3, degreeName = null) {
  const params = { years }
  if (degreeName) params.degreeName = degreeName
  return request.get('/api/admission/predict/next-years', { params })
}

export function fetchReportData(year) {
  return request.get('/api/report/data', { params: { year } })
}

export function fetchDashboardStats() {
  return request.get('/api/dashboard/stats')
}

export function fetchLLMStatus() {
  return request.get('/api/llm/status')
}

export function downloadReportWord(year) {
  return request.get('/api/report/word', { params: { year }, responseType: 'blob' })
}

