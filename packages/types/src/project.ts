/**
 * 项目合同域（project）类型：项目台账 / 预算 / 变更 / 进度款 / 分包商 / 分包合同 / 结算凭证。
 *
 * 字段建模规则（与 docs/openapi/project.openapi.yaml components/schemas 严格一致）：
 * - openapi `required` 中的字段 → 必填 `field: Type`
 * - 其余字段 → 可选 `field?: Type`
 * - `nullable: true` 且非必填 → 可选可空 `field?: Type | null`
 * - Id = string、金额用 Money（string）、日期用 string（yyyy-MM-dd）、时间用 DateTime（ISO8601）。
 */

import type { DateTime, Id, Money } from './common'

// ---------- 枚举 ----------
export type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'CLOSED'
export type BudgetStatus = 'DRAFT' | 'ACTIVE' | 'CLOSED'
export type ApprovalStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED'
export type ContractStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'TERMINATED'

// ---------- 项目 ----------
export interface ProjectVO {
  id: Id
  projectName: string
  clientName?: string
  projectAddress?: string | null
  projectType: string
  startDate?: string
  endDate?: string | null
  contractAmount?: Money
  budgetAmount?: Money
  actualAmount?: Money
  status: ProjectStatus
  description?: string | null
  managerId?: Id | null
  managerName?: string | null
  createTime?: DateTime
}

export interface ProjectCreateRequest {
  projectName: string
  clientName: string
  projectAddress?: string | null
  projectType: string
  startDate: string
  endDate?: string | null
  contractAmount: Money
  budgetAmount: Money
  status: ProjectStatus
  description?: string | null
  managerId?: Id | null
}

export interface ProjectUpdateRequest {
  projectName?: string
  clientName?: string
  projectAddress?: string
  projectType?: string
  startDate?: string
  endDate?: string
  contractAmount?: Money
  budgetAmount?: Money
  status?: ProjectStatus
  description?: string
  managerId?: Id
}

// ---------- 预算 ----------
export interface BudgetVO {
  id: Id
  budgetName: string
  amount: Money
  projectId?: Id | null
  contractId?: Id | null
  startDate?: string | null
  endDate?: string | null
  status: BudgetStatus
  usedAmount?: Money | null
  remainingAmount?: Money | null
  createTime?: DateTime
}

export interface BudgetCreateRequest {
  budgetName: string
  amount: Money
  projectId?: Id | null
  contractId?: Id | null
  startDate?: string | null
  endDate?: string | null
  status: BudgetStatus
}

export interface BudgetUpdateRequest {
  budgetName?: string
  amount?: Money
  projectId?: Id
  contractId?: Id
  startDate?: string
  endDate?: string
  status?: BudgetStatus
}

// ---------- 变更请求 ----------
export interface ChangeRequestVO {
  id: Id
  projectId: Id
  requestType: string
  description?: string | null
  estimatedCost?: Money
  status: ApprovalStatus
  requestedBy?: string | null
  approvedBy?: string | null
  approvedAt?: DateTime | null
  createTime?: DateTime
}

export interface ChangeRequestCreateRequest {
  projectId: Id
  requestType: string
  description?: string | null
  estimatedCost?: Money
}

export interface ChangeRequestUpdateRequest {
  requestType?: string
  description?: string
  estimatedCost?: Money
}

export interface ApprovalActionRequest {
  comment?: string | null
}

// ---------- 进度款 ----------
export interface ProgressPaymentVO {
  id: Id
  projectId: Id
  paymentPeriod: string
  applicationAmount?: Money
  approvedAmount?: Money | null
  paymentAmount?: Money | null
  applicationDate?: string
  approvalDate?: string | null
  paymentDate?: string | null
  status: ApprovalStatus
  applicationBy?: string | null
  approvedBy?: string | null
  createTime?: DateTime
}

export interface ProgressPaymentCreateRequest {
  projectId: Id
  paymentPeriod: string
  applicationAmount: Money
  applicationDate?: string
}

export interface ProgressPaymentUpdateRequest {
  paymentPeriod?: string
  applicationAmount?: Money
  approvedAmount?: Money
  paymentAmount?: Money
  applicationDate?: string
  approvalDate?: string
  paymentDate?: string
  status?: ApprovalStatus
}

// ---------- 分包商 ----------
export interface SubcontractorVO {
  id: Id
  name: string
  contactPerson?: string | null
  /** 手机号（加密） */
  phone?: string | null
  address?: string | null
  qualificationLevel?: string | null
  creditRating?: string | null
  businessScope?: string | null
  /** 税号（加密） */
  taxIdentificationNumber?: string | null
  /** 银行账号（加密） */
  bankAccount?: string | null
  description?: string | null
  createTime?: DateTime
}

export interface SubcontractorCreateRequest {
  name: string
  contactPerson?: string | null
  phone?: string | null
  address?: string | null
  qualificationLevel?: string | null
  creditRating?: string | null
  businessScope?: string | null
  taxIdentificationNumber?: string | null
  bankAccount?: string | null
  description?: string | null
}

export interface SubcontractorUpdateRequest {
  name?: string
  contactPerson?: string
  phone?: string
  address?: string
  qualificationLevel?: string
  creditRating?: string
  businessScope?: string
  taxIdentificationNumber?: string
  bankAccount?: string
  description?: string
}

// ---------- 分包合同 ----------
export interface SubcontractorContractVO {
  id: Id
  subcontractorId: Id
  projectId?: Id | null
  contractName: string
  contractNumber: string
  contractAmount: Money
  startDate?: string | null
  endDate?: string | null
  status?: ContractStatus
  description?: string | null
  filePath?: string | null
  fileName?: string | null
  paidAmount?: Money | null
  createTime?: DateTime
}

export interface SubcontractorContractCreateRequest {
  subcontractorId: Id
  projectId?: Id | null
  contractName: string
  contractNumber: string
  contractAmount: Money
  startDate?: string | null
  endDate?: string | null
  status?: ContractStatus
  description?: string | null
}

export interface SubcontractorContractUpdateRequest {
  projectId?: Id
  contractName?: string
  contractAmount?: Money
  startDate?: string
  endDate?: string
  status?: ContractStatus
  description?: string
}

// ---------- 结算凭证 ----------
export interface SettlementProofVO {
  id: Id
  contractId: Id
  subcontractorId?: Id | null
  proofName: string
  proofNumber: string
  proofDate?: string
  settlementAmount: Money
  filePath?: string | null
  fileName?: string | null
  description?: string | null
  createTime?: DateTime
}

export interface SettlementProofCreateRequest {
  contractId: Id
  subcontractorId?: Id | null
  proofName: string
  proofNumber: string
  proofDate?: string
  settlementAmount: Money
  description?: string | null
}

export interface SettlementProofUpdateRequest {
  proofName?: string
  proofNumber?: string
  proofDate?: string
  settlementAmount?: Money
  description?: string
}

// ---------- 分页查询入参 ----------
export interface ProjectPageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: ProjectStatus
}

export interface BudgetPageQuery {
  page?: number
  size?: number
  projectId?: Id
}

export interface ChangeRequestPageQuery {
  page?: number
  size?: number
  projectId?: Id
  status?: ApprovalStatus
}

export interface ProgressPaymentPageQuery {
  page?: number
  size?: number
  projectId?: Id
  status?: ApprovalStatus
}

export interface SubcontractorPageQuery {
  page?: number
  size?: number
  keyword?: string
}

export interface SubcontractorContractPageQuery {
  page?: number
  size?: number
  subcontractorId?: Id
  projectId?: Id
}

export interface SettlementProofPageQuery {
  page?: number
  size?: number
  contractId?: Id
}