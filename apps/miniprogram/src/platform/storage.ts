import type { UserVO } from '@ceams/types'

const ACCESS_TOKEN_KEY = 'ceams_access_token'
const REFRESH_TOKEN_KEY = 'ceams_refresh_token'
const USER_KEY = 'ceams_user'
const SIGN_SECRET_KEY = 'ceams_sign_secret'
const SIGN_SECRET_EXPIRES_AT_KEY = 'ceams_sign_secret_expires_at'
const DEVICE_ID_KEY = 'ceams_device_id'

function readString(key: string): string | null {
  const value: unknown = uni.getStorageSync(key)
  return typeof value === 'string' && value.length > 0 ? value : null
}

function writeString(key: string, value: string | null): void {
  if (value === null || value === '') {
    uni.removeStorageSync(key)
  } else {
    uni.setStorageSync(key, value)
  }
}

export function getStoredAccessToken(): string | null {
  return readString(ACCESS_TOKEN_KEY)
}

export function setStoredAccessToken(value: string | null): void {
  writeString(ACCESS_TOKEN_KEY, value)
}

export function getStoredRefreshToken(): string | null {
  return readString(REFRESH_TOKEN_KEY)
}

export function setStoredRefreshToken(value: string | null): void {
  writeString(REFRESH_TOKEN_KEY, value)
}

export function getStoredUser(): UserVO | null {
  const raw = readString(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserVO
  } catch {
    return null
  }
}

export function setStoredUser(value: UserVO | null): void {
  writeString(USER_KEY, value === null ? null : JSON.stringify(value))
}

export function getStoredSignSecret(): string | null {
  return readString(SIGN_SECRET_KEY)
}

export function setStoredSignSecret(value: string | null): void {
  writeString(SIGN_SECRET_KEY, value)
}

export function getStoredSignSecretExpiresAt(): number {
  const raw = readString(SIGN_SECRET_EXPIRES_AT_KEY)
  if (!raw) return 0
  const parsed = Number(raw)
  return Number.isNaN(parsed) ? 0 : parsed
}

export function setStoredSignSecretExpiresAt(value: number): void {
  writeString(SIGN_SECRET_EXPIRES_AT_KEY, String(value))
}

function randomDeviceId(): string {
  const rand = Math.random().toString(36).slice(2, 10)
  const ts = Date.now().toString(36)
  return `mp-${rand}-${ts}`
}

/** 获取或生成本设备指纹，用于账号绑定（deviceBind）。 */
export function getOrCreateDeviceId(): string {
  const existing = readString(DEVICE_ID_KEY)
  if (existing) return existing
  const id = randomDeviceId()
  writeString(DEVICE_ID_KEY, id)
  return id
}
