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
export type ReceiptApprovalStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
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
  /** 关联的 BPM 流程实例 ID（审批状态的唯一来源）。 */
  bpmInstanceId?: Id | null
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

// ---------- 报销额度分级规则 ----------
/**
 * 规则状态：与 BPM 模板状态取值一致，但语义独立（仅表示规则是否参与匹配）。
 */
export type AmountRuleStatus = 'ENABLED' | 'DISABLED'

/**
 * 报销额度分级规则。
 *
 * 按报销金额区间 [minAmount, maxAmount) 匹配审批流程模板：
 * - maxAmount 为空表示无上限；
 * - deptId / projectId 为空表示不限部门 / 项目；
 * - priority 越大越优先。
 */
export interface AmountRuleVO {
  id: Id
  name: string
  minAmount: Money
  /** 最高金额：null 表示无上限。 */
  maxAmount?: Money | null
  deptId?: Id | null
  deptName?: string | null
  projectId?: Id | null
  projectName?: string | null
  /** 关联的流程模板编码（仅报销业务且启用的模板）。 */
  templateCode: string
  templateName?: string | null
  priority: number
  status: AmountRuleStatus
  remark?: string | null
  createTime?: DateTime
  updateTime?: DateTime
}

/** 报销额度分级规则保存入参（新增与修改共用）。 */
export interface AmountRuleSaveRequest {
  name: string
  /** 最低金额，必须 >= 0。 */
  minAmount: Money
  /** 最高金额：null/不传表示无上限，填写时必须大于 minAmount。 */
  maxAmount?: Money | null
  /** 适用部门：null 表示不限。 */
  deptId?: Id | null
  /** 适用项目：null 表示不限。 */
  projectId?: Id | null
  /** 流程模板编码，必填且须为启用的报销模板。 */
  templateCode: string
  priority?: number
  status?: AmountRuleStatus
  remark?: string | null
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

/** 报销额度分级规则分页查询入参：keyword 匹配规则名称。 */
export interface AmountRulePageQuery {
  page?: number
  size?: number
  keyword?: string
}
