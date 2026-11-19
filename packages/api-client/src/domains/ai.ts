/**
 * AI 助手域（ai）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/AI_IMPROVEMENT_PLAN.md §5 接口契约一致。
 * 本域仅普通 Bearer 鉴权（无高危写接口）；流式对话 `POST /ai/chat` 由前端
 * `apps/web/src/services/ai.ts` 直接使用 fetch + ReadableStream 解析 SSE，
 * 不经 HttpClient（HttpClient 会等待完整响应体，无法承载流式事件）。
 * 小程序只读问答走非流式 `POST /ai/chat/sync`，可直接使用 HttpClient。
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
  AiFeedbackPayload,
  AiChatSyncPayload,
  AiChatSyncResult,
  AiDailySummaryVO,
  AiSummaryGeneratePayload,
  AiSummaryListQuery,
  AiTodoSummaryVO,
  PreAuditListQuery,
  ReceiptPreAuditVO,
  ContractRiskHintVO,
  ReceiptOcrRequest,
  ReceiptOcrResult
} from '@ceams/types'

export interface AiApi {
  /** 查询 AI 服务状态（enabled / model / reachable / latencyMs） */
  status(): Promise<AiStatusVO>
  /** 同步对话（非流式）：一次性返回完整回答与工具调用记录 */
  chatSync(body: AiChatSyncPayload): Promise<AiChatSyncResult>
  /** 我的会话列表（分页） */
  conversations(params?: AiConversationPageQuery): Promise<PageResult<AiConversationVO>>
  /** 指定会话的消息列表（分页） */
  messages(id: Id, params?: AiMessagePageQuery): Promise<PageResult<AiMessageVO>>
  /** 提交消息评价（score: 1 点赞 / -1 点踩） */
  feedback(body: AiFeedbackPayload): Promise<void>

  // ---------- 日报摘要（仅 FINANCE / SYS_ADMIN） ----------
  /** 最新一期日报摘要（无数据后端返回 404） */
  summaryLatest(): Promise<AiDailySummaryVO>
  /** 日报摘要列表（按日期倒序） */
  summaries(params?: AiSummaryListQuery): Promise<AiDailySummaryVO[]>
  /** 指定日期（yyyy-MM-dd）的日报摘要（无数据后端返回 404） */
  summaryByDate(date: string): Promise<AiDailySummaryVO>
  /** 手动生成日报摘要（date 省略表示昨日） */
  generateSummary(body?: AiSummaryGeneratePayload): Promise<AiDailySummaryVO>

  // ---------- AI 待办摘要（登录即可访问） ----------
  /** 当前用户数据范围内的待办摘要 */
  todoSummary(): Promise<AiTodoSummaryVO>

  // ---------- 报销预审（仅 FINANCE / SYS_ADMIN，只读提示） ----------
  /** 待审批票据的 AI 预审提示列表 */
  preAuditList(params?: PreAuditListQuery): Promise<ReceiptPreAuditVO[]>
  /** 指定票据的 AI 预审提示 */
  preAudit(receiptId: Id): Promise<ReceiptPreAuditVO>

  // ---------- 合同要点与风险提示（只读提示） ----------
  /** 分包合同的条款要点与风险提示（含免责声明） */
  contractRiskHints(contractId: Id): Promise<ContractRiskHintVO>

  // ---------- 票据图片识别（登录即可访问，只读） ----------
  /** 识别已上传票据图片的编号 / 金额 / 日期 / 类型 / 支付方式（未识别字段为 null） */
  receiptOcr(body: ReceiptOcrRequest): Promise<ReceiptOcrResult>
}

export function createAiApi(client: HttpClient): AiApi {
  return {
    async status() {
      return client.get<AiStatusVO>('/api/v1/ai/status')
    },
    async chatSync(body) {
      return client.post<AiChatSyncResult>('/api/v1/ai/chat/sync', body)
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
    },

    async summaryLatest() {
      return client.get<AiDailySummaryVO>('/api/v1/ai/summaries/latest')
    },
    async summaries(params = {}) {
      return client.get<AiDailySummaryVO[]>('/api/v1/ai/summaries', {
        query: { limit: params.limit ?? 10 }
      })
    },
    async summaryByDate(date) {
      return client.get<AiDailySummaryVO>(`/api/v1/ai/summaries/${encodeURIComponent(date)}`)
    },
    async generateSummary(body) {
      return client.post<AiDailySummaryVO>('/api/v1/ai/summaries/generate', body ?? {})
    },

    async todoSummary() {
      return client.get<AiTodoSummaryVO>('/api/v1/ai/todo-summary')
    },

    async preAuditList(params = {}) {
      return client.get<ReceiptPreAuditVO[]>('/api/v1/ai/preaudit/receipts', {
        query: { limit: params.limit ?? 20 }
      })
    },
    async preAudit(receiptId) {
      return client.get<ReceiptPreAuditVO>(`/api/v1/ai/preaudit/receipts/${receiptId}`)
    },

    async contractRiskHints(contractId) {
      return client.get<ContractRiskHintVO>(`/api/v1/ai/contracts/${contractId}/risk-hints`)
    },

    async receiptOcr(body) {
      // 视觉识别在 CPU 上可能耗时到分钟级，单次放宽超时（默认 15s 太短）
      return client.post<ReceiptOcrResult>('/api/v1/ai/receipt-ocr', body, { timeoutMs: 180000 })
    }
  }
}
