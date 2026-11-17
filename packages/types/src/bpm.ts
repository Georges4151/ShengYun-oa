/**
 * 流程审批域（bpm）类型：模板 / 实例 / 待办 / 抄送。
 *
 * 统一待办四个视图（我的待办 / 我发起 / 我已办 / 抄送我的）共用同一套枚举与视图结构，
 * Web 与小程序复用本文件，避免两端字段漂移。
 */

import type { DateTime, Id } from './common'

// ---------- 枚举 ----------
export type BpmBizType = 'EXPENSE' | 'CHANGE_REQUEST'
export type BpmInstanceStatus =
  | 'DRAFT'
  | 'RUNNING'
  | 'APPROVED'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'TERMINATED'
export type BpmTaskStatus = 'PENDING' | 'PENDING_ASSIGNEE' | 'APPROVED' | 'REJECTED' | 'CLOSED'
export type BpmNodeStatus = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'REJECTED' | 'CLOSED'
export type BpmNodeType =
  | 'START'
  | 'SINGLE'
  | 'COUNTERSIGN'
  | 'OR_SIGN'
  | 'CC'
  | 'CONDITION'
  | 'AUTO'
  | 'END'
export type BpmAssigneeSource =
  | 'USER'
  | 'ROLE'
  | 'DEPT_LEADER'
  | 'SUPERIOR'
  | 'INITIATOR'
  | 'DELEGATE'
  | 'PROXY'
export type TaskType = 'APPROVE' | 'CC'
export type CcReadStatus = 'UNREAD' | 'READ'
export type TemplateStatus = 'ENABLED' | 'DISABLED'
export type TemplateVersionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type RejectTarget = 'INITIATOR' | 'PREVIOUS'

// ---------- 待办 ----------
export interface TaskVO {
  id: Id
  instanceId: Id
  nodeId: Id
  nodeKey?: string | null
  assigneeId?: Id | null
  /** 被代理人 ID：有值表示当前由代理人处理，本字段为原审批人。 */
  ownerAssigneeId?: Id | null
  /** 被代理人姓名，用于展示“代理人（代 XXX）”。 */
  ownerAssigneeName?: string | null
  sourceType?: BpmAssigneeSource | null
  /** 任务版本号（乐观锁），同意/驳回/改派时必须回传。 */
  taskVersion: number
  taskType?: TaskType
  status: BpmTaskStatus
  dueAt?: DateTime | null
  overdue?: boolean
  createTime?: DateTime
  updateTime?: DateTime
  bizType?: BpmBizType | string
  bizId?: Id | null
  instanceStatus?: BpmInstanceStatus
  initiatorId?: Id | null
  initiatorName?: string | null
  currentNodeName?: string | null
}

export interface CcRecordVO {
  id: Id
  instanceId: Id
  nodeId?: Id | null
  nodeKey?: string | null
  receiverId?: Id | null
  readStatus: CcReadStatus
  createTime?: DateTime
  bizType?: BpmBizType | string
  bizId?: Id | null
  initiatorId?: Id | null
  initiatorName?: string | null
  currentNodeName?: string | null
}

// ---------- 实例 ----------
export interface InstanceVO {
  id: Id
  templateVersionId: Id
  bizType: BpmBizType | string
  bizId?: Id | null
  initiatorId: Id
  status: BpmInstanceStatus
  startedAt?: DateTime | null
  endedAt?: DateTime | null
  version: number
  createTime?: DateTime
  currentNodeKey?: string | null
  currentNodeStatus?: string | null
  currentAssigneeIds?: Id[] | null
}

export interface NodeVO {
  id: Id
  nodeKey: string
  nodeType: BpmNodeType
  seqNo: number
  status: BpmNodeStatus
  arrivedAt?: DateTime | null
  completedAt?: DateTime | null
  tasks?: TaskVO[]
}

export interface InstanceDetailVO {
  instance: InstanceVO
  nodes: NodeVO[]
  variables?: Record<string, string>
  ccRecords?: CcRecordVO[]
}

// ---------- 模板 ----------
export interface TemplateVersionVO {
  id: Id
  templateId: Id
  versionNo: number
  schemaJson: string
  status: TemplateVersionStatus
  effectiveTime?: DateTime | null
  createTime?: DateTime
}

export interface TemplateVO {
  id: Id
  code: string
  name: string
  bizType: BpmBizType | string
  currentVersionId?: Id | null
  status: TemplateStatus
  createTime?: DateTime
  updateTime?: DateTime
  currentVersion?: TemplateVersionVO | null
}

// ---------- 请求入参 ----------
export interface InstanceStartRequest {
  templateCode: string
  bizType?: string
  bizId?: Id
  variables?: Record<string, string>
}

export interface InstanceActionRequest {
  instanceVersion: number
  reason?: string
}

export interface TaskApproveRequest {
  taskVersion: number
  instanceVersion?: number
  comment?: string
}

export interface TaskRejectRequest {
  taskVersion: number
  instanceVersion?: number
  target: RejectTarget
  reason?: string
}

export interface TaskDelegateRequest {
  taskVersion: number
  instanceVersion?: number
  targetUserId: Id
  reason?: string
}

export interface TemplateCreateRequest {
  code: string
  name: string
  bizType: string
}

export interface TemplateUpdateRequest {
  name?: string
  bizType?: string
}

export interface TemplatePublishRequest {
  schemaJson: string
  effectiveTime?: DateTime
}

// ---------- 用户自助代理 ----------
export type DelegateStatus = 'ENABLED' | 'DISABLED'

export interface DelegateVO {
  id: Id
  userId: Id
  userName?: string | null
  delegateId: Id
  delegateName?: string | null
  /** 适用范围：null=全部业务。 */
  bizType?: string | null
  startTime: DateTime
  endTime: DateTime
  status: DelegateStatus
  reason?: string | null
  /** 当前是否处于代理时段（服务端计算）。 */
  effective?: boolean
  createTime?: DateTime
}

export interface DelegateCreateRequest {
  delegateId: Id
  bizType?: string | null
  startTime: DateTime
  endTime: DateTime
  reason?: string
}

export interface UserOptionVO {
  id: Id
  name: string
  username: string
}

// ---------- 查询入参 ----------
export interface TaskPageQuery {
  page?: number
  size?: number
  bizType?: string
  overdue?: boolean
}

export interface InstancePageQuery {
  page?: number
  size?: number
  status?: BpmInstanceStatus
  bizType?: string
}

export interface CcPageQuery {
  page?: number
  size?: number
  bizType?: string
  readStatus?: CcReadStatus
}

export interface TemplatePageQuery {
  page?: number
  size?: number
  bizType?: string
  status?: TemplateStatus
  keyword?: string
}
