/**
 * 身份与访问域（iam）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/iam.openapi.yaml paths 一致。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  LoginData,
  LoginRequest,
  RefreshRequest,
  SignSecretData,
  UserVO,
  UserCreateRequest,
  UserUpdateRequest,
  AssignRolesRequest,
  ResetPasswordRequest,
  RoleVO,
  RoleCreateRequest,
  RoleUpdateRequest,
  AssignPermissionsRequest,
  PermissionVO,
  UserDeviceVO,
  DeviceBindRequest,
  UserSessionVO,
  OperationLogVO,
  UserPageQuery,
  RolePageQuery,
  OperationLogPageQuery,
  PageResult
} from '@ceams/types'

export interface IamApi {
  login(body: LoginRequest): Promise<LoginData>
  refresh(body: RefreshRequest): Promise<LoginData>
  logout(): Promise<void>
  signSecret(): Promise<SignSecretData>
  sessionPage(query?: { page?: number; size?: number }): Promise<PageResult<UserSessionVO>>
  sessionRevoke(sessionId: string): Promise<void>

  userPage(query?: UserPageQuery): Promise<PageResult<UserVO>>
  userDetail(id: Id): Promise<UserVO>
  userCreate(body: UserCreateRequest): Promise<Id>
  userUpdate(id: Id, body: UserUpdateRequest): Promise<void>
  userRemove(id: Id): Promise<void>
  userAssignRoles(id: Id, body: AssignRolesRequest): Promise<void>
  userResetPassword(id: Id, body: ResetPasswordRequest): Promise<void>

  rolePage(query?: RolePageQuery): Promise<PageResult<RoleVO>>
  roleDetail(id: Id): Promise<RoleVO>
  roleCreate(body: RoleCreateRequest): Promise<Id>
  roleUpdate(id: Id, body: RoleUpdateRequest): Promise<void>
  roleRemove(id: Id): Promise<void>
  roleAssignPermissions(id: Id, body: AssignPermissionsRequest): Promise<void>

  permissionTree(): Promise<PermissionVO[]>
  permissionMine(): Promise<string[]>

  devicePage(query?: { page?: number; size?: number }): Promise<PageResult<UserDeviceVO>>
  deviceBind(body: DeviceBindRequest): Promise<Id>
  deviceRemove(id: Id): Promise<void>

  logPage(query?: OperationLogPageQuery): Promise<PageResult<OperationLogVO>>
}

export function createIamApi(client: HttpClient): IamApi {
  return {
    async login(body) {
      return client.post<LoginData>('/api/v1/auth/login', body, { skipAuth: true })
    },
    async refresh(body) {
      return client.post<LoginData>('/api/v1/auth/refresh', body, { skipAuth: true })
    },
    async logout() {
      return client.post<void>('/api/v1/auth/logout')
    },
    async signSecret() {
      return client.get<SignSecretData>('/api/v1/auth/sign-secret')
    },
    async sessionPage(query = {}) {
      return client.get<PageResult<UserSessionVO>>('/api/v1/auth/sessions', {
        query: { page: query.page ?? 1, size: query.size ?? 20 }
      })
    },
    async sessionRevoke(sessionId) {
      return client.delete<void>(`/api/v1/auth/sessions/${sessionId}/revoke`)
    },

    async userPage(query = {}) {
      return client.get<PageResult<UserVO>>('/api/v1/users', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          keyword: query.keyword,
          status: query.status
        }
      })
    },
    async userDetail(id) {
      return client.get<UserVO>(`/api/v1/users/${id}`)
    },
    async userCreate(body) {
      return client.post<Id>('/api/v1/users', body)
    },
    async userUpdate(id, body) {
      return client.put<void>(`/api/v1/users/${id}`, body)
    },
    async userRemove(id) {
      return client.delete<void>(`/api/v1/users/${id}`)
    },
    async userAssignRoles(id, body) {
      return client.put<void>(`/api/v1/users/${id}/roles`, body)
    },
    async userResetPassword(id, body) {
      return client.put<void>(`/api/v1/users/${id}/password`, body)
    },

    async rolePage(query = {}) {
      return client.get<PageResult<RoleVO>>('/api/v1/roles', {
        query: { page: query.page ?? 1, size: query.size ?? 20, keyword: query.keyword }
      })
    },
    async roleDetail(id) {
      return client.get<RoleVO>(`/api/v1/roles/${id}`)
    },
    async roleCreate(body) {
      return client.post<Id>('/api/v1/roles', body)
    },
    async roleUpdate(id, body) {
      return client.put<void>(`/api/v1/roles/${id}`, body)
    },
    async roleRemove(id) {
      return client.delete<void>(`/api/v1/roles/${id}`)
    },
    async roleAssignPermissions(id, body) {
      return client.put<void>(`/api/v1/roles/${id}/permissions`, body)
    },

    async permissionTree() {
      return client.get<PermissionVO[]>('/api/v1/permissions')
    },
    async permissionMine() {
      return client.get<string[]>('/api/v1/permissions/me')
    },

    async devicePage(query = {}) {
      return client.get<PageResult<UserDeviceVO>>('/api/v1/devices', {
        query: { page: query.page ?? 1, size: query.size ?? 20 }
      })
    },
    async deviceBind(body) {
      return client.post<Id>('/api/v1/devices', body)
    },
    async deviceRemove(id) {
      return client.delete<void>(`/api/v1/devices/${id}`)
    },

    async logPage(query = {}) {
      return client.get<PageResult<OperationLogVO>>('/api/v1/operation-logs', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          userId: query.userId,
          operationType: query.operationType,
          startTime: query.startTime,
          endTime: query.endTime
        }
      })
    }
  }
}