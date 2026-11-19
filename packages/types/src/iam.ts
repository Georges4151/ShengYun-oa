/**
 * 身份与访问域（iam）类型。
 *
 * 字段建模规则（与 docs/openapi/iam.openapi.yaml components/schemas 严格一致）：
 * - openapi `required` 中的字段 → 必填 `field: Type`
 * - 其余字段 → 可选 `field?: Type`
 * - `nullable: true` 且非必填 → 可选可空 `field?: Type | null`
 * - Id = string、时间用 DateTime（ISO8601）、金额用 Money。
 */

import type { DateTime, Id } from './common'

// ---------- 枚举（string 字面量联合类型） ----------
export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED'
export type RoleStatus = 'ACTIVE' | 'DISABLED'
export type PermissionType = 'MENU' | 'BUTTON' | 'API'
export type Platform = 'WEB' | 'DESKTOP' | 'MINIPROGRAM'
export type DeviceStatus = 'ACTIVE' | 'UNBOUND'
export type OperationType =
  | 'LOGIN'
  | 'LOGOUT'
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'APPROVE'
  | 'EXPORT'
  | 'IMPORT'
  | 'OTHER'

// ---------- 令牌 / 登录 ----------
export interface TokenPair {
  /** 访问令牌 */
  accessToken: string
  /** 刷新令牌（轮换） */
  refreshToken: string
  /** access 有效秒数（900） */
  expiresIn: number
}

export interface LoginRequest {
  username: string
  password: string
  /** 设备指纹（可选；开启设备绑定时必填） */
  deviceId?: string
  deviceName?: string
  platform?: Platform
}

export interface RefreshRequest {
  refreshToken: string
}

/** 登录 / 刷新接口 data 负载 */
export interface LoginData {
  tokenPair: TokenPair
  user: UserVO
}

/** 高危接口 HMAC 签名密钥下发负载 */
export interface SignSecretData {
  /** HMAC 共享密钥 */
  secret: string
  /** 密钥缓存过期时间（epoch 毫秒） */
  expiresAt: number
}

// ---------- 用户 ----------
export interface UserVO {
  id: Id
  username: string
  name: string
  email?: string | null
  /** 手机号（库内加密，接口脱敏） */
  phone?: string | null
  /** 头像访问地址（/api/v1/profile/avatars/xxx），可空 */
  avatar?: string | null
  status: UserStatus
  roles?: RoleVO[]
  createTime?: DateTime
}

export interface UserCreateRequest {
  username: string
  name: string
  password?: string
  email?: string | null
  phone?: string | null
  status?: UserStatus
  /** 初始角色 ID 集合 */
  roleIds?: Id[]
}

export interface UserUpdateRequest {
  name?: string
  email?: string | null
  phone?: string | null
  status?: UserStatus
}

export interface AssignRolesRequest {
  roleIds: Id[]
}

export interface ResetPasswordRequest {
  password: string
}

// ---------- 部门 ----------
export interface DeptVO {
  id: Id
  name: string
  /** 上级部门 ID（根部门为 null） */
  parentId?: Id | null
  leaderId?: Id | null
  leaderName?: string | null
  sortOrder?: number | null
  createTime?: DateTime
  /** 子部门（仅树接口填充） */
  children?: DeptVO[]
}

// ---------- 角色 ----------
export interface RoleVO {
  id: Id
  code: string
  name: string
  description?: string | null
  status: RoleStatus
  createTime?: DateTime
}

export interface RoleCreateRequest {
  code: string
  name: string
  description?: string | null
}

export interface RoleUpdateRequest {
  name?: string
  description?: string
  status?: RoleStatus
}

export interface AssignPermissionsRequest {
  permissionIds: Id[]
}

// ---------- 权限 ----------
export interface PermissionVO {
  id: Id
  /** 权限标识（按钮级控制用） */
  code: string
  name: string
  type: PermissionType
  parentId?: Id | null
  /** 前端路由（MENU 类型） */
  path?: string | null
  icon?: string | null
  sort?: number
  children?: PermissionVO[]
}

export interface PermissionCreatePayload {
  /** 权限码，规则 ^[A-Za-z0-9:_-]{2,100}$ */
  code: string
  /** 权限名称，长度 ≤100 */
  name: string
  type: PermissionType
  /** 父权限 id；null 表示作为根节点 */
  parentId?: Id | null
  /** 前端路由，仅 MENU 类型使用，长度 ≤255 */
  path?: string | null
  /** 图标标识，长度 ≤100 */
  icon?: string | null
  /** 排序值，默认 0 */
  sortOrder?: number
}

/** 权限更新请求体，字段与创建一致 */
export type PermissionUpdatePayload = PermissionCreatePayload

// ---------- 会话 ----------
export interface UserSessionVO {
  sessionId: string
  userId: Id
  platform: Platform
  ipAddress?: string | null
  loginTime: DateTime
  lastActiveTime?: DateTime
}

// ---------- 设备 ----------
export interface UserDeviceVO {
  id: Id
  userId: Id
  deviceId?: string
  deviceName: string
  platform?: Platform
  lastLoginIp?: string | null
  status: DeviceStatus
  bindTime?: DateTime
  lastLoginTime?: DateTime
}

export interface DeviceBindRequest {
  /** 设备指纹 */
  deviceId: string
  deviceName?: string
  platform?: Platform
}

// ---------- 操作日志 ----------
export interface OperationLogVO {
  id: Id
  userId: Id
  username?: string | null
  operationType: OperationType
  operationDescription: string
  targetType?: string | null
  targetId?: Id | null
  ipAddress?: string | null
  createTime: DateTime
}

// ---------- 分页查询入参 ----------
export interface UserPageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: UserStatus
}

export interface RolePageQuery {
  page?: number
  size?: number
  keyword?: string
}

export interface OperationLogPageQuery {
  page?: number
  size?: number
  userId?: Id
  operationType?: OperationType
  startTime?: DateTime
  endTime?: DateTime
}