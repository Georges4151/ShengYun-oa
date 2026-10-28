/**
 * 报销域（expense）类型：票据 / 审批记录 / 财务核验。
 *
 * 字段建模规则（与 docs/openapi/expense.openapi.yaml components/schemas 严格一致）：
 * - openapi `required` 中的字段 → 必填 `field: Type`
 * - 其余字段 → 可选 `field?: Type`
 * - `nullable: true` 且非必填 → 可选可空 `field?: Type | null`
 * - Id = string、金额用 Money（string）、时间用 DateTime（ISO8601）。
 */

import type { DateTime, Id, Money } from './common'
import type { PaymentStatus } from './masterdata'

// ---------- 枚举 ----------
export type ReceiptType = 'TRANSPORTATION' | 'ACCOMMODATION' | 'MEAL' | 'OFFICE' | 'OTHER'
export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED'
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'WECHAT' | 'ALIPAY' | 'OTHER'
export type ReceiptApprovalStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED'
export type ApprovalRecordStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

// ---------- 票据 ----------
export interface ReceiptVO {
  id: Id
  receiptNumber: string
  userId: Id
  reporterId?: Id | null
  projectId?: Id | null
  reporterName?: string | null
  receiptDate?: DateTime | null
  amount: Money
  type: ReceiptType
  description?: string | null
  filePath?: string | null
  fileHash?: string | null
  ocrData?: string | null
  verificationStatus: VerificationStatus
  verifiedBy?: Id | null
  verifiedAt?: DateTime | null
  paymentStatus: PaymentStatus
  paidAmount: Money
  paidAt?: DateTime | null
  paidBy?: Id | null
  paymentMethod?: PaymentMethod | null
  currentApprovalLevel?: number | null
  approvalStatus: ReceiptApprovalStatus
  createTime?: DateTime
}

export interface ReceiptCreateRequest {
  receiptNumber: string
  userId?: Id | null
  reporterId?: Id | null
  projectId?: Id | null
  reporterName?: string | null
  receiptDate?: DateTime | null
  amount: Money
  type: ReceiptType
  description?: string | null
  filePath?: string | null
  fileHash?: string | null
  ocrData?: string | null
  paymentMethod?: PaymentMethod | null
}

export interface ReceiptUpdateRequest {
  reporterId?: Id | null
  projectId?: Id | null
  reporterName?: string | null
  receiptDate?: DateTime | null
  amount?: Money
  type?: ReceiptType
  description?: string | null
  filePath?: string | null
  fileHash?: string | null
  ocrData?: string | null
  paymentMethod?: PaymentMethod | null
}

export interface ReceiptVerifyRequest {
  paymentAccountId: Id
  amount: Money
  paymentMethod?: PaymentMethod | null
  remark?: string | null
}

// ---------- 审批记录 ----------
export interface ApprovalRecordVO {
  id: Id
  receiptId: Id
  approvalLevel: number
  approverRole?: string | null
  approverId?: Id | null
  approverName?: string | null
  status: ApprovalRecordStatus
  approvalComment?: string | null
  approvalTime?: DateTime | null
  createTime?: DateTime
}

// ---------- 财务核验 ----------
export interface ReceiptVerificationVO {
  id: Id
  receiptId: Id
  paymentAccountId: Id
  amount: Money
  paymentMethod?: PaymentMethod | null
  verificationTime?: DateTime | null
  verifiedBy?: Id | null
  remark?: string | null
  createTime?: DateTime
}

// ---------- 分页查询入参 ----------
export interface ReceiptPageQuery {
  page?: number
  size?: number
  receiptNumber?: string
  userId?: Id
  projectId?: Id
  verificationStatus?: VerificationStatus
  paymentStatus?: PaymentStatus
  approvalStatus?: ReceiptApprovalStatus
}
