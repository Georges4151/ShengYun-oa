/**
 * 资金域（payment）类型：付款账户 / 收支流水 / 付款交易 / 付款执行 / 成本分摊。
 *
 * 字段建模规则（与 docs/openapi/payment.openapi.yaml components/schemas 严格一致）：
 * - openapi `required` 中的字段 → 必填 `field: Type`
 * - 其余字段 → 可选 `field?: Type`
 * - `nullable: true` 且非必填 → 可选可空 `field?: Type | null`
 * - Id = string、金额用 Money（string）、时间用 DateTime（ISO8601）。
 */

import type { DateTime, Id, Money } from './common'

// ---------- 枚举 ----------
export type AccountType = 'INCOME' | 'EXPENSE'
export type AccountStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELLED'
export type AllocationStatus = 'UNALLOCATED' | 'PARTIAL' | 'ALLOCATED'
export type TargetType = 'PROJECT' | 'DEPT' | 'SERVICE' | 'CONTRACT'

// ---------- 付款账户 ----------
export interface PaymentAccountVO {
  id: Id
  name: string
  /** 账户类型（代码值，无固定枚举） */
  type: string
  balance: Money
  /** 账号【需加密】（阶段4） */
  accountNumber?: string | null
  description?: string | null
  createTime?: DateTime
}

export interface PaymentAccountCreateRequest {
  name: string
  type: string
  /** 初始余额（缺省 0） */
  balance?: Money | null
  accountNumber?: string | null
  description?: string | null
}

export interface PaymentAccountUpdateRequest {
  name?: string
  type?: string
  accountNumber?: string | null
  description?: string | null
}

// ---------- 收支流水 ----------
export interface AccountVO {
  id: Id
  projectId?: Id | null
  transactionDate: DateTime
  amount: Money
  type: AccountType
  receiptId?: Id | null
  description?: string | null
  status: AccountStatus
  paymentAccountId?: Id | null
  contractId?: Id | null
  paymentType?: string | null
  allocationStatus: AllocationStatus
  categoryId?: Id | null
  contactId?: Id | null
  materialId?: Id | null
  subcontractorId?: Id | null
  /** 关联项目名称（后端补充的展示字段） */
  projectName?: string | null
  /** 付款账户名称（后端补充的展示字段） */
  paymentAccountName?: string | null
  /** 票据编号（后端补充的展示字段） */
  receiptNumber?: string | null
  /** 合同名称（后端补充的展示字段） */
  contractName?: string | null
  createTime?: DateTime
}

export interface AccountCreateRequest {
  projectId?: Id | null
  transactionDate: DateTime
  amount: Money
  type: AccountType
  /** 逻辑引用，不强制校验存在性 */
  receiptId?: Id | null
  description?: string | null
  paymentAccountId?: Id | null
  contractId?: Id | null
  paymentType?: string | null
  categoryId?: Id | null
  contactId?: Id | null
  materialId?: Id | null
  subcontractorId?: Id | null
}

export interface AccountUpdateRequest {
  projectId?: Id | null
  transactionDate?: DateTime | null
  amount?: Money
  type?: AccountType
  receiptId?: Id | null
  description?: string | null
  paymentAccountId?: Id | null
  contractId?: Id | null
  paymentType?: string | null
  categoryId?: Id | null
  contactId?: Id | null
  materialId?: Id | null
  subcontractorId?: Id | null
}

// ---------- 付款交易 ----------
export interface PaymentTransactionVO {
  id: Id
  accountId: Id
  receiptId?: Id | null
  projectId?: Id | null
  amount: Money
  type: AccountType
  transactionDate: DateTime
  description?: string | null
  /** 付款账户名称（后端补充的展示字段） */
  accountName?: string | null
  /** 票据编号（后端补充的展示字段） */
  receiptNumber?: string | null
  /** 关联项目名称（后端补充的展示字段） */
  projectName?: string | null
  createTime?: DateTime
}

export interface PaymentTransactionCreateRequest {
  accountId: Id
  receiptId?: Id | null
  projectId?: Id | null
  amount: Money
  type: AccountType
  transactionDate: DateTime
  description?: string | null
}

export interface PaymentTransactionUpdateRequest {
  accountId?: Id | null
  receiptId?: Id | null
  projectId?: Id | null
  amount?: Money
  type?: AccountType
  transactionDate?: DateTime | null
  description?: string | null
}

// ---------- 付款执行 ----------
export interface PaymentExecuteRequest {
  accountId: Id
  receiptId?: Id | null
  projectId?: Id | null
  amount: Money
  type: AccountType
  transactionDate: DateTime
  description?: string | null
}

// ---------- 成本分摊 ----------
export interface CostAllocationVO {
  id: Id
  accountRecordId: Id
  targetType: TargetType
  targetId: Id
  allocationAmount: Money
  allocationRatio?: string | null
  /** 流水记录名称（后端补充的展示字段） */
  accountRecordName?: string | null
  /** 分摊目标名称（后端补充的展示字段） */
  targetName?: string | null
  createTime?: DateTime
}

export interface CostAllocationCreateRequest {
  accountRecordId: Id
  targetType: TargetType
  targetId: Id
  allocationAmount: Money
  allocationRatio?: string | null
}

export interface CostAllocationUpdateRequest {
  targetType?: TargetType
  targetId?: Id | null
  allocationAmount?: Money
  allocationRatio?: string | null
}

// ---------- 分页查询入参 ----------
export interface PaymentAccountPageQuery {
  page?: number
  size?: number
  name?: string
  type?: string
}

export interface AccountPageQuery {
  page?: number
  size?: number
  projectId?: Id
  type?: AccountType
  status?: AccountStatus
}

export interface PaymentTransactionPageQuery {
  page?: number
  size?: number
  accountId?: Id
  type?: AccountType
}

export interface CostAllocationPageQuery {
  page?: number
  size?: number
  accountRecordId?: Id
}
