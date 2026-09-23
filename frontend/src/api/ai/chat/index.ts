import request from '@/config/axios'
import { getAccessToken, getTenantId } from '@/utils/auth'

// AI 对话消息类型
export interface ChatMessage {
  role: 'system' | 'user' | 'assistant'
  content?: string
  contentParts?: ContentPart[]
}

export interface ContentPart {
  type: 'text' | 'image_url'
  text?: string
  imageUrl?: { url: string }
}

export interface ChatReqVO {
  messages: ChatMessage[]
  stream?: boolean
  temperature?: number
  maxTokens?: number
}

export interface ChatRespVO {
  content: string
  model: string
  streaming: boolean
  finishReason: string | null
}

/** AI 对话（非流式） */
export function chatCompletions(data: ChatReqVO) {
  return request.post({
    url: '/ai/chat/completions',
    data
  })
}

/** AI 对话（流式 SSE） - 返回原生 fetch，便于手动解析 SSE */
export function chatCompletionsStream(data: ChatReqVO): Promise<Response> {
  const baseURL = import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL
  const url = baseURL + '/ai/chat/completions/stream'
  const token = getAccessToken() || ''
  const tenantId = getTenantId() || ''
  return fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token,
      'tenant-id': tenantId
    },
    body: JSON.stringify(data)
  })
}
