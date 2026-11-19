import { createCeamsApiClient } from '@ceams/api-client'
import type { CeamsApiClient } from '@ceams/api-client'
import { API_BASE_URL } from './config'
import { uniRequestAdapter } from './request'
import {
  getAccessToken,
  getRefreshToken,
  getSignSecretCache,
  setSession,
  setSignSecretCache
} from './auth'
import { getOrCreateDeviceId } from './storage'

/**
 * 统一请求客户端：业务代码只经这里访问接口，组件内禁止 uni.request / fetch / axios。
 * 复用 @ceams/api-client，仅注入小程序平台适配器（uni.request）。
 */
let apiClient!: CeamsApiClient

/**
 * 刷新令牌轮换：用 refreshToken 换取新的令牌对并写回本地存储。
 * 单飞：同一时刻只允许一个刷新在飞，并发 401 共享同一个 Promise，
 * 避免一次性轮换的 refreshToken 被重复消费导致后续请求全部失败。
 */
let refreshPromise: Promise<string> | null = null

function refreshAccessToken(): Promise<string> {
  if (refreshPromise) {
    return refreshPromise
  }
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return Promise.reject(new Error('AUTH_TOKEN_EXPIRED'))
  }
  const pending = apiClient.iam.refresh({ refreshToken })
    .then((data) => {
      setSession(data)
      return data.tokenPair.accessToken
    })
    .finally(() => {
      // 成功或失败都在此处清空，保证下一次令牌过期能重新发起新一轮刷新。
      refreshPromise = null
    })
  refreshPromise = pending
  return pending
}

/** 获取高危接口 HMAC 签名密钥：命中缓存直接返回，临近过期（提前 60s）时重新下发。 */
async function getSignSecret(): Promise<string | null> {
  const cached = getSignSecretCache()
  const now = Date.now()
  if (cached && cached.expiresAt > now + 60000) {
    return cached.secret
  }
  try {
    const data = await apiClient.iam.signSecret(getOrCreateDeviceId())
    setSignSecretCache(data.secret, data.expiresAt)
    return data.secret
  } catch {
    return null
  }
}

apiClient = createCeamsApiClient({
  baseUrl: API_BASE_URL,
  adapter: uniRequestAdapter,
  timeoutMs: 15000,
  getAccessToken: () => getAccessToken(),
  refreshAccessToken,
  getSignSecret,
  getDeviceId: () => getOrCreateDeviceId()
})

export { apiClient }
