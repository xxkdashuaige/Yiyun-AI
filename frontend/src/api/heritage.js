import request from './request'

export function getCategories() {
  return request.get('/heritage/categories')
}

export function getProjects(params) {
  return request.get('/heritage/projects', { params })
}

export function getProjectDetail(id) {
  return request.get(`/heritage/projects/${id}`)
}