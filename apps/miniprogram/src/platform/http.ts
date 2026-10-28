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

/**
 * 统一请求客户端：业务代码只经这里访问接口，组件内禁止 uni.request / fetch / axios。
 * 复用 @ceams/api-client，仅注入小程序平台适配器（uni.request）。
 */
let apiClient!: CeamsApiClient

/** 刷新令牌轮换：用 refreshToken 换取新的令牌对并写回本地存储。 */
async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('AUTH_TOKEN_EXPIRED')
  }
  const data = await apiClient.iam.refresh({ refreshToken })
  setSession(data)
  return data.tokenPair.accessToken
}

/** 获取高危接口 HMAC 签名密钥：命中缓存直接返回，临近过期（提前 60s）时重新下发。 */
async function getSignSecret(): Promise<string | null> {
  const cached = getSignSecretCache()
  const now = Date.now()
  if (cached && cached.expiresAt > now + 60000) {
    return cached.secret
  }
  try {
    const data = await apiClient.iam.signSecret()
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
  getSignSecret
})

export { apiClient }
