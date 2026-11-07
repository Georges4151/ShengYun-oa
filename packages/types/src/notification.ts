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
export type NotificationType = 'SYSTEM' | 'APPROVAL' | 'ANNOUNCEMENT'
export type ContentType = 'TEXT' | 'LINK'
export type PushChannel = 'SYSTEM' | 'EMAIL' | 'SMS'
export type PushStatus = 'PENDING' | 'SENT' | 'FAILED'

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
}
