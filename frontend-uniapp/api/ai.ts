import { post, get, del, put, BASE_URL } from '../utils/request'

/**
 * 翱翔无人机 AI 智能客服 API（v3 真实 RAG + Tool + Memory + MCP + SSE）
 */

export interface ChatParams {
  message: string
  conversationId?: string
}

export interface ChatResult {
  reply: string
  conversationId: string
  elapsedMs?: string
  tools?: any[]
}

export function chat(data: ChatParams) {
  return post<ChatResult>('/ai/v3/chat', data)
}

export function getChatHistory(conversationId: string) {
  return get(`/ai/v3/history/${conversationId}`)
}

export function clearConversation(conversationId: string) {
  return del(`/ai/v3/conversation/${conversationId}`)
}

export function getAiStatus() {
  return get('/ai/v3/status')
}

export function getAiTools() {
  return get('/ai/v3/tools')
}

export function searchRag(query: string, topK = 3) {
  return get('/ai/v3/rag/search', { query, topK })
}

/**
 * SSE 流式对话（uni-app 版）
 *
 * 走 uni.request + enableChunked + onChunkReceived，逐 chunk 拼 SSE 事件。
 * 每条事件以 \n\n 结尾，data 行以 "data:" 开头。
 * 收到 data: [DONE] 视为流结束。
 */
export interface StreamCallbacks {
  onChunk: (chunk: string) => void
  onDone: (full: string) => void
  onError: (err: any) => void
  signal?: { aborted: boolean }
}

export function streamChatMessage(params: ChatParams, cb: StreamCallbacks) {
  const token = uni.getStorageSync('token') || ''
  const authHeader = token ? { Authorization: `Bearer ${token}` } : {}

  let full = ''
  let buffer = ''
  let aborted = false

  const requestTask = uni.request({
    url: `${BASE_URL}/ai/v3/chat/stream`,
    method: 'POST',
    data: { message: params.message, conversationId: params.conversationId || '' },
    enableChunked: true,
    // #ifdef MP-WEIXIN
    timeout: 120000,
    // #endif
    header: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...authHeader
    },
    success: () => {
      // 完整响应（兼容旧版）
      flushDone()
    },
    fail: (err) => {
      if (aborted) return
      cb.onError?.(err)
    },
    complete: () => {
      flushDone()
    }
  } as any)

  // H5 / App 端：监听 onChunkReceived
  const task: any = requestTask
  if (task && typeof task.onChunkReceived === 'function') {
    task.onChunkReceived((res: any) => {
      try {
        const arr = res.data
        // 微信小程序：res.data 是 ArrayBuffer；H5/APP：string
        let chunk = ''
        if (arr instanceof ArrayBuffer) {
          chunk = new TextDecoder('utf-8').decode(new Uint8Array(arr))
        } else if (typeof arr === 'string') {
          chunk = arr
        } else if (arr && arr.__data__) {
          chunk = String(arr.__data__)
        } else {
          chunk = String(arr || '')
        }
        handleChunk(chunk)
      } catch (e) {
        // ignore decode error
      }
    })
  }

  function handleChunk(chunk: string) {
    buffer += chunk
    let idx
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      const dataLines: string[] = []
      for (const line of block.split('\n')) {
        if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
      }
      if (dataLines.length === 0) continue
      const data = dataLines.join('\n')
      if (data === '[DONE]') {
        flushDone()
        return
      }
      full += data
      cb.onChunk?.(data)
    }
  }

  function flushDone() {
    if (aborted) return
    aborted = true
    cb.onDone?.(full)
  }

  return {
    abort: () => {
      aborted = true
      try { task?.abort?.() } catch (_) {}
      try { (requestTask as any)?.abort?.() } catch (_) {}
    }
  }
}
