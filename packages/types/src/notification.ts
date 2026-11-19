/**
 * 通知域（notification）类型：通知 / 用户通知 / 用户通知偏好。
 *
 * 字段建模规则（与 docs/openapi/notification.openapi.yaml components/schemas 严格一致）：
 * - openapi `required` 中的字段 → 必填 `field: Type`
 * - 其余字段 → 可选 `field?: Type`
 * - `nullable: true` 且非必填 → 可选可空 `field?: Type | null`
 * - Id = string、时间用 DateTime（ISO8601）、免打扰时段用 string（HH:mm:ss）。
 */

import type { DateTime, Id } from './common'

// ---------- 枚举 ----------
export type NotificationStatus = 'DRAFT' | 'PUBLISHED' | 'REVOKED'
export type NotificationPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
/**
 * 通知类型：系统 / 审批 / 公告 / 人力 / 行政 / 文档。
 * M6 新增 HR（工资条发布、合同到期）、ADMIN（资产逾期、报修派单）、DOC（文档共享，预留）。
 */
export type NotificationType = 'SYSTEM' | 'APPROVAL' | 'ANNOUNCEMENT' | 'HR' | 'ADMIN' | 'DOC'
export type ContentType = 'TEXT' | 'LINK'
export type PushChannel = 'SYSTEM' | 'EMAIL' | 'SMS'
export type PushStatus = 'PENDING' | 'SENT' | 'FAILED'

/** 通知类型中文标签（穷举 Record：新增枚举值时必须同步补齐，供 Web / 小程序复用）。 */
export const notificationTypeLabels: Record<NotificationType, string> = {
  SYSTEM: '系统',
  APPROVAL: '审批',
  ANNOUNCEMENT: '公告',
  HR: '人力',
  ADMIN: '行政',
  DOC: '文档'
}

// ---------- 通知 ----------
export interface NotificationVO {
  id: Id
  title: string
  priority: NotificationPriority
  notificationType: NotificationType
  status: NotificationStatus
  contentType?: ContentType
  content?: string | null
  linkUrl?: string | null
  senderId?: Id | null
  senderName?: string | null
  validStartTime?: DateTime | null
  validEndTime?: DateTime | null
  publishTime?: DateTime | null
  createTime?: DateTime
}

export interface NotificationCreateRequest {
  title: string
  notificationType: NotificationType
  contentType?: ContentType
  content?: string | null
  linkUrl?: string | null
  /** 缺省 NORMAL */
  priority?: NotificationPriority
  senderId?: Id | null
  senderName?: string | null
  validStartTime?: DateTime | null
  validEndTime?: DateTime | null
}

export interface NotificationUpdateRequest {
  title?: string | null
  contentType?: ContentType
  content?: string | null
  linkUrl?: string | null
  priority?: NotificationPriority
  notificationType?: NotificationType
  senderId?: Id | null
  senderName?: string | null
  validStartTime?: DateTime | null
  validEndTime?: DateTime | null
}

// ---------- 发布接收范围 ----------
/** 发布接收范围：全员 / 按角色 / 按用户 */
export type NotificationPublishScope = 'ALL' | 'ROLES' | 'USERS'

/** 发布通知请求体：scope 为 ALL 时无需携带 roleIds / userIds */
export interface NotificationPublishRequest {
  scope: NotificationPublishScope
  /** 按角色发布时必填，至少 1 个 */
  roleIds?: Id[]
  /** 按用户发布时必填，至少 1 个 */
  userIds?: Id[]
}

/** 可选接收角色（发布弹窗「按角色」下拉选项） */
export interface NotificationRecipientRoleVO {
  id: Id
  name: string
  code: string
}

/** 可选接收用户（发布弹窗「按用户」远程搜索选项） */
export interface NotificationRecipientUserVO {
  id: Id
  name: string
  username: string
}

// ---------- 用户通知 ----------
export interface UserNotificationVO {
  id: Id
  notificationId: Id
  userId: Id
  /** 0未读 1已读 */
  isRead: number
  pushStatus: PushStatus
  readTime?: DateTime | null
  channel?: PushChannel
  pushTime?: DateTime | null
  createTime?: DateTime
  // ---------- 通知内容快照（后端扩展的可选只读字段，缺失时前端回退旧展示） ----------
  title?: string | null
  content?: string | null
  /** 业务跳转链接，如 ceams://ai/summary?date=yyyy-MM-dd */
  linkUrl?: string | null
  notificationType?: NotificationType
  priority?: NotificationPriority
  publishTime?: DateTime | null
}

export interface UserNotificationMineVO {
  records: UserNotificationVO[]
  total: number
  page: number
  size: number
  unreadCount: number
}

// ---------- 用户通知偏好 ----------
export interface UserNotificationPreferenceVO {
  id: Id
  userId: Id
  notificationType: NotificationType
  /** 0否 1是 */
  receiveSystem: number
  receiveEmail: number
  receiveSms: number
  /** 0关闭 1开启 */
  quietMode: number
  /** HH:mm:ss */
  quietStartTime?: string | null
  quietEndTime?: string | null
  priorityThreshold?: NotificationPriority
}

export interface UserNotificationPreferenceUpsertRequest {
  notificationType: NotificationType
  receiveSystem?: number
  receiveEmail?: number
  receiveSms?: number
  quietMode?: number
  quietStartTime?: string | null
  quietEndTime?: string | null
  priorityThreshold?: NotificationPriority
}

// ---------- 分页查询入参 ----------
export interface NotificationPageQuery {
  page?: number
  size?: number
  type?: NotificationType
  status?: NotificationStatus
  priority?: NotificationPriority
  startTime?: DateTime
  endTime?: DateTime
}

export interface UserNotificationPageQuery {
  page?: number
  size?: number
  /** 通知类型过滤（可选）：不传则返回全部类型，对应后端 `type` 查询参数 */
  type?: NotificationType
}
