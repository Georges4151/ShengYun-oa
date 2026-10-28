/**
 * 对账域（bank）类型：银行流水 / 总账 / 对账结果 / 流水导入 / 自动匹配。
 *
 * 字段建模规则（与 docs/openapi/bank.openapi.yaml components/schemas 严格一致）：
 * - openapi `required` 中的字段 → 必填 `field: Type`
 * - 其余字段 → 可选 `field?: Type`
 * - `nullable: true` 且非必填 → 可选可空 `field?: Type | null`
 * - Id = string、金额用 Money（string）、时间用 DateTime（ISO8601）。
 */

import type { DateTime, Id, Money } from './common'

// ---------- 枚举 ----------
export type TransactionType = 'INCOME' | 'EXPENSE'
export type BankStatementStatus = 'UNRECONCILED' | 'RECONCILED' | 'MATCHED'
export type ReconciliationStatus = 'MATCHED' | 'UNMATCHED' | 'PARTIAL_MATCHED'
export type DiscrepancyType =
  | 'MISSING_IN_SYSTEM'
  | 'MISSING_IN_BANK'
  | 'AMOUNT_MISMATCH'
  | 'DATE_MISMATCH'
  | 'DESCRIPTION_MISMATCH'
export type ResolutionStatus = 'UNRESOLVED' | 'RESOLVED'

// ---------- 银行流水 ----------
export interface BankStatementVO {
  id: Id
  accountId: Id
  transactionDate: DateTime
  amount: Money
  type: TransactionType
  status: BankStatementStatus
  description?: string | null
  counterparty?: string | null
  bankReference?: string | null
  balance?: Money | null
  createTime?: DateTime
}

export interface BankStatementCreateRequest {
  accountId: Id
  transactionDate: DateTime
  amount: Money
  type: TransactionType
  description?: string | null
  counterparty?: string | null
  bankReference?: string | null
  balance?: Money | null
}

export interface BankStatementUpdateRequest {
  accountId?: Id | null
  transactionDate?: DateTime | null
  amount?: Money
  type?: TransactionType
  description?: string | null
  counterparty?: string | null
  bankReference?: string | null
  balance?: Money | null
}

// ---------- 流水导入 ----------
export interface StatementImportError {
  row: number
  reason: string
}

export interface StatementImportResultVO {
  total: number
  success: number
  skipped: number
  failed: number
  errors?: StatementImportError[]
}

// ---------- 总账 ----------
export interface GeneralLedgerVO {
  id: Id
  accountName: string
  /** 账号【需加密】（阶段4） */
  accountNumber?: string | null
  bankName?: string | null
  balance?: Money | null
  accountType?: string | null
  summary?: string | null
  transactionTime?: DateTime | null
  transactionType?: TransactionType
  amount?: Money | null
  status?: string | null
  createTime?: DateTime
}

export interface GeneralLedgerCreateRequest {
  accountName: string
  accountNumber?: string | null
  bankName?: string | null
  balance?: Money | null
  accountType?: string | null
  summary?: string | null
  transactionTime?: DateTime | null
  transactionType?: TransactionType
  amount?: Money | null
  status?: string | null
}

export interface GeneralLedgerUpdateRequest {
  accountName?: string | null
  accountNumber?: string | null
  bankName?: string | null
  balance?: Money | null
  accountType?: string | null
  summary?: string | null
  transactionTime?: DateTime | null
  transactionType?: TransactionType
  amount?: Money | null
  status?: string | null
}

// ---------- 对账结果 ----------
export interface ReconciliationResultVO {
  id: Id
  accountId: Id
  status: ReconciliationStatus
  resolutionStatus: ResolutionStatus
  statementId?: Id | null
  transactionId?: Id | null
  discrepancyType?: DiscrepancyType
  bankAmount?: Money | null
  systemAmount?: Money | null
  bankDate?: DateTime | null
  systemDate?: DateTime | null
  bankDescription?: string | null
  systemDescription?: string | null
  statementReference?: string | null
  transactionReference?: string | null
  resolutionNotes?: string | null
  reconciliationDate?: DateTime | null
  createTime?: DateTime
}

export interface ReconciliationMatchRequest {
  accountId: Id
  /** 日期容忍天数（默认 3） */
  daysTolerance?: number
}

export interface ReconciliationResolveRequest {
  resolutionNotes?: string | null
}

export interface ReconciliationMatchResultVO {
  matched: number
  unmatched: number
  partialMatched: number
}

// ---------- 分页查询入参 ----------
export interface BankStatementPageQuery {
  page?: number
  size?: number
  accountId?: Id
  status?: BankStatementStatus
  type?: TransactionType
  startDate?: DateTime
  endDate?: DateTime
}

export interface GeneralLedgerPageQuery {
  page?: number
  size?: number
  accountName?: string
  transactionType?: TransactionType
  status?: string
}

export interface ReconciliationResultPageQuery {
  page?: number
  size?: number
  accountId?: Id
  status?: ReconciliationStatus
  resolutionStatus?: ResolutionStatus
}
