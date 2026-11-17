/**
 * AI 助手域（ai）类型：服务状态 / 对话请求 / SSE 事件 / 会话与消息 / 反馈。
 *
 * 字段建模规则（与 docs/AI_IMPROVEMENT_PLAN.md §5 接口契约一致）：
 * - Id = string、时间用 DateTime（ISO8601）、行数/耗时用 number
 * - SSE 事件建模为判别联合（discriminated union），`event` 为判别键
 */

import type { DateTime, Id, Money } from './common'
import type { ContractStatus } from './project'

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

// ---------- 日报摘要（仅 FINANCE / SYS_ADMIN） ----------
/** 摘要触发方式：定时任务 / 手动生成 */
export type AiSummaryTriggerType = 'SCHEDULED' | 'MANUAL'

export interface AiDailySummaryVO {
  id: Id
  /** 摘要日期，yyyy-MM-dd */
  summaryDate: string
  /** 周期类型，当前固定 DAILY */
  periodType: string
  /** 摘要正文（可能包含换行） */
  content: string
  /** TEMPLATE 或模型名 */
  model?: string | null
  triggerType: AiSummaryTriggerType
  /** 关联的通知 ID（生成后推送通知时回填） */
  notificationId?: Id | null
  /** 生成摘要时使用的指标快照（字段随实现演进，保持宽松） */
  metrics?: Record<string, unknown>
  createTime?: DateTime
  updateTime?: DateTime
}

export interface AiSummaryGeneratePayload {
  /** 目标日期 yyyy-MM-dd；省略表示昨日 */
  date?: string
}

export interface AiSummaryListQuery {
  /** 返回条数，缺省沿用后端默认值 */
  limit?: number
}

// ---------- AI 待办摘要（登录即可访问，数据范围与待办列表一致） ----------
export interface AiTodoBizTypeCount {
  /** 业务类型编码，如 EXPENSE */
  bizType: string
  /** 业务类型中文名 */
  label: string
  /** 待办数量 */
  count: number
}

export interface AiTodoUpcomingItem {
  taskId: Id
  instanceId: Id
  bizType: string
  label: string
  initiatorName?: string | null
  dueAt?: DateTime | null
  overdue: boolean
}

export interface AiTodoSummaryVO {
  generatedAt: DateTime
  totalPending: number
  overdue: number
  dueToday: number
  byBizType: AiTodoBizTypeCount[]
  upcoming: AiTodoUpcomingItem[]
  /** 规则模板或 AI 润色后的摘要正文 */
  content: string
  /** TEMPLATE 或模型名 */
  model?: string | null
  aiEnabled: boolean
  disclaimer: string
}

// ---------- 报销预审（仅 FINANCE / SYS_ADMIN，只读提示） ----------
/** 预审综合风险等级 */
export type PreAuditRiskLevel = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE'

/** 预审命中规则 */
export type PreAuditRule =
  | 'AMOUNT_THRESHOLD'
  | 'BUDGET_USAGE'
  | 'DUPLICATE_NUMBER'
  | 'DUPLICATE_AMOUNT'
  | 'MISSING_INVOICE'
  | 'VERIFICATION_PENDING'
  | 'VERIFICATION_REJECTED'

/** 单条规则预警级别 */
export type PreAuditWarningLevel = 'HIGH' | 'MEDIUM'

export interface ReceiptPreAuditWarning {
  rule: PreAuditRule
  level: PreAuditWarningLevel
  /** 可直接展示的中文提示 */
  message: string
}

export interface ReceiptPreAuditVO {
  receiptId: Id
  receiptNumber: string
  reporterName?: string | null
  projectId?: Id | null
  /** 票据金额（后端以数值返回） */
  amount?: number | null
  /** 票据日期，yyyy-MM-dd */
  receiptDate?: DateTime | null
  riskLevel: PreAuditRiskLevel
  warnings: ReceiptPreAuditWarning[]
}

export interface PreAuditListQuery {
  /** 返回条数，缺省沿用后端默认值 */
  limit?: number
}

// ---------- 合同要点与风险提示（只读建议） ----------
/** 合同综合风险等级（NONE 表示未发现明显风险） */
export type ContractRiskLevel = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE'

/** 合同条款要点（已结构化的关键条款） */
export interface ContractClausePointVO {
  /** 要点名称，如「工期」「结算方式」 */
  label: string
  /** 要点内容 */
  value: string
}

/** 单条合同风险提示 */
export interface ContractRiskItemVO {
  level: 'HIGH' | 'MEDIUM' | 'LOW'
  /** 规则编码，如 EXPIRED / PAYMENT_RATIO */
  code: string
  /** 风险标题 */
  title: string
  /** 风险说明 */
  detail: string
}

export interface ContractRiskHintVO {
  contractId: Id
  contractNumber: string
  contractName: string
  subcontractorId: Id
  projectId?: Id | null
  status: ContractStatus
  contractAmount: Money
  paidAmount: Money
  remainingAmount: Money
  /** 已付比例（字符串，如 "0.65"） */
  paidRatio: Money
  /** 合同起止日期，yyyy-MM-dd */
  startDate?: DateTime | null
  endDate?: DateTime | null
  clausePoints: ContractClausePointVO[]
  risks: ContractRiskItemVO[]
  riskLevel: ContractRiskLevel
  /** AI 补充说明（可能为空） */
  aiNote?: string | null
  aiEnabled: boolean
  /** 免责声明（后端提供文案，直接展示） */
  disclaimer: string
}
