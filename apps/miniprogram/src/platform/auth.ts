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

/** 保存登录 / 刷新返回的完整会话（令牌对 + 用户）。 */
export function setSession(data: LoginData): void {
  setStoredAccessToken(data.tokenPair.accessToken)
  setStoredRefreshToken(data.tokenPair.refreshToken)
  setStoredUser(data.user)
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
