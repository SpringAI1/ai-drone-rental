import { post, get, del, put } from './request'

/**
 * 翱翔无人机 AI 智能客服 API（v3 真实 RAG + Tool + Memory + MCP + SSE）
 *
 * - POST /ai/v3/chat          普通对话
 * - POST /ai/v3/chat/stream   SSE 流式（打字机）
 * - GET  /ai/v3/history/{id}  历史
 * - DELETE /ai/v3/conversation/{id}  清空
 * - GET  /ai/v3/status        服务状态
 * - GET  /ai/v3/rag/search    知识库语义检索
 * - GET  /ai/v3/tools         工具列表
 */

// ---------- 普通 / 状态 / 历史 / 工具 ----------
export const sendChatMessage = (message, conversationId = '') => {
  return post('/ai/v3/chat', { message, conversationId })
}

export const getChatHistory = (conversationId) => {
  return get(`/ai/v3/history/${conversationId}`)
}

export const clearConversation = (conversationId) => {
  return del(`/ai/v3/conversation/${conversationId}`)
}

export const getAiStatus = (signal) => {
  return get('/ai/v3/status', undefined, { signal })
}

export const updateAiStatus = (data) => {
  return put('/ai/v3/admin/status', data)
}

export const getAiTools = () => {
  return get('/ai/v3/tools')
}

export const searchRag = (query, topK = 3) => {
  return get('/ai/v3/rag/search', { query, topK })
}

// ---------- SSE 流式对话 ----------
// 使用原生 fetch + ReadableStream，绕过 axios + 加密层
// 端点返回 text/event-stream，每条消息形如：
//   id:xxx
//   event:message
//   data:<chunk 文本>
//   （最后一条 event:done  data:[DONE]）

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * 发起 SSE 流式对话
 * @param {{ message: string, conversationId?: string }} params
 * @param {{
 *   onChunk: (chunk: string) => void,
 *   onDone: (full: string) => void,
 *   onError: (err: Error) => void,
 *   signal?: AbortSignal,
 *   token?: string
 * }} callbacks
 */
export async function streamChatMessage(params, callbacks) {
  const { onChunk, onDone, onError, signal, token } = callbacks
  const url = `${API_BASE}/ai/v3/chat/stream`
  const headers = { 'Content-Type': 'application/json', Accept: 'text/event-stream' }
  try {
    const ls = window.localStorage
    if (!token) {
      const t = ls.getItem('token') || ls.getItem('Authorization')
      if (t) headers['Authorization'] = t.startsWith('Bearer ') ? t : `Bearer ${t}`
    } else {
      headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    }
  } catch (_) {}

  let response
  try {
    response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(params),
      signal
    })
  } catch (e) {
    onError?.(e)
    return
  }

  if (!response.ok || !response.body) {
    onError?.(new Error(`SSE 连接失败：HTTP ${response.status}`))
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let full = ''

  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // 按 \n\n 分隔事件
      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const eventBlock = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const dataLines = []
        for (const line of eventBlock.split('\n')) {
          if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
        }
        if (dataLines.length === 0) continue
        const data = dataLines.join('\n')
        if (data === '[DONE]') {
          onDone?.(full)
          return
        }
        full += data
        onChunk?.(data)
      }
    }
    onDone?.(full)
  } catch (e) {
    if (e.name !== 'AbortError') onError?.(e)
  }
}
