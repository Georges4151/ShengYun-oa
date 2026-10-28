/**
 * 电子印章域（seal）类型：印章 / 用印申请 / 用印审批 / 盖章 / 盖章记录 / 使用审计。
 *
 * 字段建模规则（与 docs/openapi/seal.openapi.yaml components/schemas 严格一致）：
 * - openapi `required` 中的字段 → 必填 `field: Type`
 * - 其余字段 → 可选 `field?: Type`
 * - `nullable: true` 且非必填 → 可选可空 `field?: Type | null`
 * - Id = string、时间用 DateTime（ISO8601）。
 */

import type { DateTime, Id } from './common'

// ---------- 枚举 ----------
export type SealStatus = 'ACTIVE' | 'DISABLED' | 'ARCHIVED'
export type StorageType = 'FILE' | 'USB_DRIVE' | 'CLOUD'
export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

// ---------- 电子印章 ----------
export interface ElectronicSealVO {
  id: Id
  sealName: string
  /** 印章类型（代码值，如 公章/合同章/财务章） */
  sealType: string
  status: SealStatus
  sealCode?: string | null
  description?: string | null
  /** 印章图片路径 */
  sealImage?: string | null
  storageType?: StorageType
  sealFileId?: string | null
  usbDriveId?: string | null
  passwordProtected?: boolean | null
  lastUsedTime?: DateTime | null
  usageCount?: number | null
  createTime?: DateTime
  updateTime?: DateTime
}

export interface ElectronicSealCreateRequest {
  sealName: string
  sealType: string
  sealCode?: string | null
  description?: string | null
  sealImage?: string | null
  storageType?: StorageType
  sealFileId?: string | null
  usbDriveId?: string | null
  passwordProtected?: boolean | null
}

export interface ElectronicSealUpdateRequest {
  sealName?: string | null
  sealType?: string | null
  sealCode?: string | null
  description?: string | null
  status?: SealStatus
  sealImage?: string | null
  storageType?: StorageType
  sealFileId?: string | null
  usbDriveId?: string | null
  passwordProtected?: boolean | null
}

// ---------- 用印申请 ----------
export interface ElectronicSealApplicationVO {
  id: Id
  sealId: Id
  applicantId: Id
  status: ApplicationStatus
  sealName?: string | null
  applicantName?: string | null
  purpose?: string | null
  projectId?: Id | null
  projectName?: string | null
  contractName?: string | null
  contractPath?: string | null
  stampPosition?: string | null
  approverId?: Id | null
  approverName?: string | null
  approvalComment?: string | null
  applyTime?: DateTime | null
  approvalTime?: DateTime | null
  createTime?: DateTime
}

export interface ElectronicSealApplicationCreateRequest {
  sealId: Id
  purpose?: string | null
  projectId?: Id | null
  projectName?: string | null
  contractName?: string | null
  contractPath?: string | null
  stampPosition?: string | null
  applicantName?: string | null
}

export interface ElectronicSealApplicationUpdateRequest {
  purpose?: string | null
  projectId?: Id | null
  projectName?: string | null
  contractName?: string | null
  contractPath?: string | null
  stampPosition?: string | null
}

// ---------- 用印审批 ----------
export interface StampApprovalVO {
  id: Id
  stampId: Id
  status: ApprovalStatus
  sealId?: Id | null
  sealName?: string | null
  sealCode?: string | null
  sealPermission?: string | null
  projectId?: Id | null
  projectName?: string | null
  contractName?: string | null
  contractPath?: string | null
  originalContractPath?: string | null
  stampPositions?: string | null
  applicantId?: Id | null
  applicantName?: string | null
  approverId?: Id | null
  approverName?: string | null
  rejectReason?: string | null
  applyTime?: DateTime | null
  approvalTime?: DateTime | null
  createTime?: DateTime
}

export interface StampApprovalCreateRequest {
  /** 用印申请 ID（逻辑引用 biz_electronic_seal_application.id） */
  stampId: Id
  sealPermission?: string | null
  stampPositions?: string | null
  originalContractPath?: string | null
}

export interface StampApprovalUpdateRequest {
  sealPermission?: string | null
  stampPositions?: string | null
  originalContractPath?: string | null
}

export interface StampApprovalApproveRequest {
  approvalComment?: string | null
}

export interface StampApprovalRejectRequest {
  rejectReason: string
}

// ---------- 盖章 ----------
export interface SealStampRequest {
  sealId: Id
  applicationId: Id
  sourcePdfBase64: string
  sealImageBase64: string
  stampPosition?: string | null
}

export interface SealStampResultVO {
  stampId: Id
  downloadPath: string
}

// ---------- 盖章记录 ----------
export interface ElectronicSealStampVO {
  id: Id
  sealId: Id
  sealName?: string | null
  projectId?: Id | null
  projectName?: string | null
  contractName?: string | null
  contractPath?: string | null
  stampPosition?: string | null
  stampImage?: string | null
  stampTime?: DateTime | null
  userId?: Id | null
  userName?: string | null
  ipAddress?: string | null
  createTime?: DateTime
}

// ---------- 使用审计 ----------
export interface ElectronicSealUsageVO {
  id: Id
  sealId: Id
  sealName?: string | null
  userId?: Id | null
  userName?: string | null
  purpose?: string | null
  documentType?: string | null
  documentPath?: string | null
  usageTime?: DateTime | null
  ipAddress?: string | null
  createTime?: DateTime
}

// ---------- 分页查询入参 ----------
export interface ElectronicSealPageQuery {
  page?: number
  size?: number
  name?: string
  status?: SealStatus
  storageType?: StorageType
}

export interface ElectronicSealApplicationPageQuery {
  page?: number
  size?: number
  sealId?: Id
  status?: ApplicationStatus
  applicantId?: Id
}

export interface StampApprovalPageQuery {
  page?: number
  size?: number
  stampId?: Id
  sealId?: Id
  status?: ApprovalStatus
}

export interface ElectronicSealStampPageQuery {
  page?: number
  size?: number
  sealId?: Id
  projectId?: Id
}

export interface ElectronicSealUsagePageQuery {
  page?: number
  size?: number
  sealId?: Id
  userId?: Id
}
