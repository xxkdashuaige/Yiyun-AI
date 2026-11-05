import request from './request'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { ElMessage } from 'element-plus'

export function chat(message, heritageProjectId = null, conversationHistory = []) {
  const body = { message, conversationHistory }
  if (heritageProjectId) body.heritageProjectId = heritageProjectId
  return request.post('/ai/chat', body)
}

export function chatStream(message, heritageProjectId, conversationHistory, callbacks) {
  const userStore = useUserStore()
  const { onDelta, onSources, onDone, onError } = callbacks
  const body = { message, conversationHistory }
  if (heritageProjectId) body.heritageProjectId = heritageProjectId

  fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${userStore.token}`,
    },
    body: JSON.stringify(body),
  })
    .then(async (response) => {
      if (!response.ok) {
        let errMsg = `HTTP ${response.status}`
        try {
          const errData = await response.json()
          if (errData.message) errMsg = errData.message
        } catch (_) {}
        if (response.status === 401) {
          const userStore = useUserStore()
          userStore.logout()
          ElMessage.error('登录已过期，请重新登录')
          router.push({ name: 'Login' })
        }
        throw new Error(errMsg)
      }
      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      const read = () => {
        reader
          .read()
          .then(({ done, value }) => {
            if (done) {
              onDone && onDone()
              return
            }
            buffer += decoder.decode(value, { stream: true })
            const lines = buffer.split('\n')
            buffer = lines.pop()
            for (const line of lines) {
              const trimmed = line.trim()
              if (!trimmed.startsWith('data:')) continue
              const data = trimmed.slice(5).trim()
              if (!data) continue
              if (data === '[DONE]') {
                onDone && onDone()
                return
              }
              try {
                const obj = JSON.parse(data)
                if (obj.delta !== undefined) {
                  onDelta && onDelta(obj.delta)
                } else if (obj.sources !== undefined) {
                  onSources && onSources(obj.sources)
                } else if (obj.error) {
                  onError && onError(new Error(obj.error))
                }
              } catch (e) {
                console.warn('解析 SSE 数据失败:', data, e)
              }
            }
            read()
          })
          .catch((e) => onError && onError(e))
      }
      read()
    })
    .catch((e) => onError && onError(e))
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