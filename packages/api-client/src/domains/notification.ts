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
  publish(id: Id): Promise<void>
  revoke(id: Id): Promise<void>

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
    async publish(id) {
      return client.post<void>(`/api/v1/notifications/${id}/publish`)
    },
    async revoke(id) {
      return client.post<void>(`/api/v1/notifications/${id}/revoke`)
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
