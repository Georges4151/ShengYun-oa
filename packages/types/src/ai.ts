/**
 * AI 助手域（ai）类型：服务状态 / 对话请求 / SSE 事件 / 会话与消息 / 反馈。
 *
 * 字段建模规则（与 docs/AI_IMPROVEMENT_PLAN.md §5 接口契约一致）：
 * - Id = string、时间用 DateTime（ISO8601）、行数/耗时用 number
 * - SSE 事件建模为判别联合（discriminated union），`event` 为判别键
 */

import type { DateTime, Id } from './common'

// ---------- 服务状态 ----------
export interface AiStatusVO {
  /** AI 总开关（ceams.ai.enabled） */
  enabled: boolean
  /** 当前使用模型，如 qwen2.5:7b */
  model: string
  /** Ollama 是否可达 */
  reachable: boolean
  /** 探活耗时（毫秒） */
  latencyMs: number
}

// ---------- 对话请求 ----------
export interface AiChatContext {
  /** 前端当前页面（路由 name），供后端做提示词裁剪 */
  page?: string
}

export interface AiChatRequest {
  /** 已有会话 ID；null 表示新建会话 */
  conversationId: string | null
  message: string
  context?: AiChatContext
}

// ---------- 同步对话（小程序只读问答） ----------
export interface AiChatSyncPayload {
  /** 已有会话 ID；null 或省略表示新建会话 */
  conversationId?: string | null
  message: string
  context?: AiChatContext
}

export interface AiChatSyncToolCall {
  /** 只读工具名，如 queryExpenseSummary */
  name: string
  /** 工具调用参数 */
  args: Record<string, unknown>
  /** 工具返回行数 */
  rows: number
}

/** `POST /ai/chat/sync` 响应体（非流式，一次性返回完整答案） */
export interface AiChatSyncResult {
  conversationId: string
  messageId: string
  /** 完整回答文本 */
  answer: string
  /** 本次使用的模型，如 qwen2.5:7b */
  model: string
  /** 端到端耗时（毫秒） */
  latencyMs: number
  toolCalls: AiChatSyncToolCall[]
}

// ---------- SSE 事件 ----------
export interface AiChatMetaData {
  conversationId: string
  model: string
}

export interface AiChatDeltaData {
  /** 流式增量文本（逐段追加） */
  text: string
}

export interface AiChatToolData {
  /** 只读工具名，如 queryExpenseSummary */
  name: string
  /** 工具调用参数 */
  args: Record<string, unknown>
  /** 工具返回行数 */
  rows: number
}

export interface AiChatDoneData {
  messageId: string
  latencyMs: number
}

export interface AiChatErrorData {
  code: string
  message: string
}

/**
 * 服务端 SSE 事件（`event:` 字段即判别键）：
 * meta / delta / tool / done / error。
 */
export type AiChatEvent =
  | { event: 'meta'; data: AiChatMetaData }
  | { event: 'delta'; data: AiChatDeltaData }
  | { event: 'tool'; data: AiChatToolData }
  | { event: 'done'; data: AiChatDoneData }
  | { event: 'error'; data: AiChatErrorData }

// ---------- 会话 / 消息 ----------
export type AiMessageRole = 'USER' | 'ASSISTANT' | 'TOOL'

export interface AiConversationVO {
  id: Id
  title?: string | null
  createTime?: DateTime
  updateTime?: DateTime
}

export interface AiMessageVO {
  id: Id
  conversationId?: Id
  role: AiMessageRole
  content: string
  createTime: DateTime
}

// ---------- 反馈 ----------
export interface AiFeedbackPayload {
  messageId: Id
  /** 1 点赞，-1 点踩 */
  score: 1 | -1
  remark?: string
}

// ---------- 分页查询入参 ----------
export interface AiConversationPageQuery {
  page?: number
  size?: number
}

export interface AiMessagePageQuery {
  page?: number
  size?: number
}
