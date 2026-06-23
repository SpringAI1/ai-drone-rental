import { post } from '../utils/request'

export interface ChatParams {
  message: string
  conversationId?: string
}

export interface ChatResult {
  reply: string
  conversationId: string
  tools?: any[]
}

export function chat(data: ChatParams) {
  return post<ChatResult>('/ai/chat', data)
}