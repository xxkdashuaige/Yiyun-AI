import request from './request'

export function saveWork(data) {
  return request.post('/works', data)
}

export function getWorks(params) {
  return request.get('/works', { params })
}

export function getWorkDetail(id) {
  return request.get(`/works/${id}`)
}

export function deleteWork(id) {
  return request.delete(`/works/${id}`)
}