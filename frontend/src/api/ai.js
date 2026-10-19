import request from './request'

export function chat(message, heritageProjectId = null, conversationHistory = []) {
  const body = { message, conversationHistory }
  if (heritageProjectId) body.heritageProjectId = heritageProjectId
  return request.post('/ai/chat', body)
}

export function generateImage(data) {
  return request.post('/ai/generate/image', data)
}

export function getAiTasks(params) {
  return request.get('/ai/tasks', { params })
}

export function getAiTaskDetail(id) {
  return request.get(`/ai/tasks/${id}`)
}