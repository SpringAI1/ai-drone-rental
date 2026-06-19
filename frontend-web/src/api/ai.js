import { post, get, del, put } from './request'

export const sendChatMessage = (message, conversationId = '') => {
  return post('/ai/chat', { message, conversationId })
}

export const getChatHistory = (conversationId) => {
  return get(`/ai/history/${conversationId}`)
}

export const clearConversation = (conversationId) => {
  return del(`/ai/conversation/${conversationId}`)
}

export const checkAiHealth = () => {
  return get('/ai/health')
}

export const getAiStatus = (signal) => {
  return get('/ai/status', undefined, { signal })
}

export const updateAiStatus = (data) => {
  return put('/ai/admin/status', data)
}