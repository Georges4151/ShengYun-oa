import type { LoginData, UserVO } from '@ceams/types'
import {
  getStoredAccessToken,
  getStoredRefreshToken,
  getStoredSignSecret,
  getStoredSignSecretExpiresAt,
  getStoredUser,
  setStoredAccessToken,
  setStoredRefreshToken,
  setStoredSignSecret,
  setStoredSignSecretExpiresAt,
  setStoredUser
} from './storage'

export interface SignSecretCache {
  secret: string
  expiresAt: number
}

export function getAccessToken(): string | null {
  return getStoredAccessToken()
}

export function getRefreshToken(): string | null {
  return getStoredRefreshToken()
}

export function getUser(): UserVO | null {
  return getStoredUser()
}

export function isLoggedIn(): boolean {
  return getStoredAccessToken() !== null
}

export function getUserName(): string {
  const user = getStoredUser()
  return user?.name || user?.username || ''
}

/**
 * 当前登录用户的角色编码集合（如 SYS_ADMIN / FINANCE / STAFF）。
 * 未登录、无角色时返回空数组，调用方据此做入口显隐。
 */
export function rolesOf(): string[] {
  const user = getStoredUser()
  if (!user?.roles?.length) return []
  return user.roles.map((role) => role.code).filter((code) => Boolean(code))
}

/** 保存登录 / 刷新返回的完整会话（令牌对 + 用户）。 */
export function setSession(data: LoginData): void {
  setStoredAccessToken(data.tokenPair.accessToken)
  setStoredRefreshToken(data.tokenPair.refreshToken)
  setStoredUser(data.user)
}

/** 局部更新本地缓存的当前用户（如头像地址变更），未登录时忽略。 */
export function patchUser(patch: Partial<UserVO>): void {
  const user = getStoredUser()
  if (!user) return
  setStoredUser({ ...user, ...patch })
}

/** 清空本地会话状态。 */
export function clearSession(): void {
  setStoredAccessToken(null)
  setStoredRefreshToken(null)
  setStoredUser(null)
}

export function getSignSecretCache(): SignSecretCache | null {
  const secret = getStoredSignSecret()
  if (!secret) return null
  return { secret, expiresAt: getStoredSignSecretExpiresAt() }
}

export function setSignSecretCache(secret: string, expiresAt: number): void {
  setStoredSignSecret(secret)
  setStoredSignSecretExpiresAt(expiresAt)
}
