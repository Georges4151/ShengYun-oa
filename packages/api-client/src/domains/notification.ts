/**
 * 通知域（notification）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/notification.openapi.yaml paths 一致。
 * notification 域无高危接口，普通 Bearer 鉴权，无需 HMAC 签名。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  PageResult,
  NotificationVO,
  NotificationCreateRequest,
  NotificationUpdateRequest,
  NotificationPageQuery,
  NotificationPublishRequest,
  NotificationRecipientRoleVO,
  NotificationRecipientUserVO,
  UserNotificationMineVO,
  UserNotificationPageQuery,
  UserNotificationPreferenceVO,
  UserNotificationPreferenceUpsertRequest
} from '@ceams/types'

export interface NotificationApi {
  page(query?: NotificationPageQuery): Promise<PageResult<NotificationVO>>
  detail(id: Id): Promise<NotificationVO>
  create(body: NotificationCreateRequest): Promise<Id>
  update(id: Id, body: NotificationUpdateRequest): Promise<void>
  remove(id: Id): Promise<void>
  /** 发布通知，可指定接收范围；返回实际送达人数 */
  publish(id: Id, body?: NotificationPublishRequest): Promise<number>
  revoke(id: Id): Promise<void>

  /** 可选接收角色列表（发布弹窗「按角色」） */
  recipientRoles(): Promise<NotificationRecipientRoleVO[]>
  /** 可选接收用户远程搜索（发布弹窗「按用户」），最多 50 条 */
  recipientUsers(keyword?: string): Promise<NotificationRecipientUserVO[]>

  userNotificationMine(query?: UserNotificationPageQuery): Promise<UserNotificationMineVO>
  unreadCount(): Promise<number>
  read(id: Id): Promise<void>
  readAll(): Promise<void>

  preferenceMine(): Promise<UserNotificationPreferenceVO[]>
  preferenceUpsert(body: UserNotificationPreferenceUpsertRequest): Promise<Id>
  preferenceRemove(id: Id): Promise<void>
}

export function createNotificationApi(client: HttpClient): NotificationApi {
  return {
    async page(query = {}) {
      return client.get<PageResult<NotificationVO>>('/api/v1/notifications', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          type: query.type,
          status: query.status,
          priority: query.priority,
          startTime: query.startTime,
          endTime: query.endTime
        }
      })
    },
    async detail(id) {
      return client.get<NotificationVO>(`/api/v1/notifications/${id}`)
    },
    async create(body) {
      return client.post<Id>('/api/v1/notifications', body)
    },
    async update(id, body) {
      return client.put<void>(`/api/v1/notifications/${id}`, body)
    },
    async remove(id) {
      return client.delete<void>(`/api/v1/notifications/${id}`)
    },
    async publish(id, body) {
      // 请求体可选：传入 scope 按指定范围发布，不传则沿用旧的按偏好分发逻辑
      return client.post<number>(`/api/v1/notifications/${id}/publish`, body)
    },
    async revoke(id) {
      return client.post<void>(`/api/v1/notifications/${id}/revoke`)
    },
    async recipientRoles() {
      return client.get<NotificationRecipientRoleVO[]>('/api/v1/notifications/recipients/roles')
    },
    async recipientUsers(keyword) {
      return client.get<NotificationRecipientUserVO[]>('/api/v1/notifications/recipients/users', {
        // 关键字为空时由 buildQuery 自动跳过，保持不带 keyword 的请求
        query: { keyword: keyword?.trim() || undefined }
      })
    },

    async userNotificationMine(query = {}) {
      return client.get<UserNotificationMineVO>('/api/v1/notifications/user-notifications/mine', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          // 通知类型过滤：空值由 buildQuery 自动跳过，保持原有「不带 type」的请求行为
          type: query.type
        }
      })
    },
    async unreadCount() {
      return client.get<number>('/api/v1/notifications/user-notifications/unread-count')
    },
    async read(id) {
      return client.post<void>(`/api/v1/notifications/user-notifications/${id}/read`)
    },
    async readAll() {
      return client.post<void>('/api/v1/notifications/user-notifications/read-all')
    },

    async preferenceMine() {
      return client.get<UserNotificationPreferenceVO[]>('/api/v1/notifications/preferences/mine')
    },
    async preferenceUpsert(body) {
      return client.put<Id>('/api/v1/notifications/preferences', body)
    },
    async preferenceRemove(id) {
      return client.delete<void>(`/api/v1/notifications/preferences/${id}`)
    }
  }
}
