/**
 * AI 助手域（ai）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/AI_IMPROVEMENT_PLAN.md §5 接口契约一致。
 * 本域仅普通 Bearer 鉴权（无高危写接口）；流式对话 `POST /ai/chat` 由前端
 * `apps/web/src/services/ai.ts` 直接使用 fetch + ReadableStream 解析 SSE，
 * 不经 HttpClient（HttpClient 会等待完整响应体，无法承载流式事件）。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  PageResult,
  AiStatusVO,
  AiConversationVO,
  AiConversationPageQuery,
  AiMessageVO,
  AiMessagePageQuery,
  AiFeedbackPayload
} from '@ceams/types'

export interface AiApi {
  /** 查询 AI 服务状态（enabled / model / reachable / latencyMs） */
  status(): Promise<AiStatusVO>
  /** 我的会话列表（分页） */
  conversations(params?: AiConversationPageQuery): Promise<PageResult<AiConversationVO>>
  /** 指定会话的消息列表（分页） */
  messages(id: Id, params?: AiMessagePageQuery): Promise<PageResult<AiMessageVO>>
  /** 提交消息评价（score: 1 点赞 / -1 点踩） */
  feedback(body: AiFeedbackPayload): Promise<void>
}

export function createAiApi(client: HttpClient): AiApi {
  return {
    async status() {
      return client.get<AiStatusVO>('/api/v1/ai/status')
    },
    async conversations(params = {}) {
      return client.get<PageResult<AiConversationVO>>('/api/v1/ai/conversations', {
        query: { page: params.page ?? 1, size: params.size ?? 20 }
      })
    },
    async messages(id, params = {}) {
      return client.get<PageResult<AiMessageVO>>(`/api/v1/ai/conversations/${id}/messages`, {
        query: { page: params.page ?? 1, size: params.size ?? 20 }
      })
    },
    async feedback(body) {
      return client.post<void>('/api/v1/ai/feedback', body)
    }
  }
}
