import { createCeamsApiClient } from '@ceams/api-client'
import type { CeamsApiClient } from '@ceams/api-client'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'

/**
 * 统一请求客户端：业务代码只经这里访问接口，组件内禁止直接 axios/fetch。
 * 三端复用同一套 @ceams/api-client，仅注入不同的平台适配器（默认 fetchAdapter）。
 */
let apiClient!: CeamsApiClient

/**
 * 刷新令牌轮换：用 refreshToken 换取新的令牌对并写回 auth store，
 * 返回新的 access token 供 HttpClient 重试原请求。
 */
function refreshAccessToken(): Promise<string> {
  const auth = useAuthStore(pinia)
  const currentRefreshToken = auth.refreshToken
  if (!currentRefreshToken) {
    return Promise.reject(new Error('AUTH_TOKEN_EXPIRED'))
  }
  return apiClient.iam.refresh({ refreshToken: currentRefreshToken }).then((data) => {
    auth.setSession(data)
    return data.tokenPair.accessToken
  })
}

apiClient = createCeamsApiClient({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'https://api.example.com',
  timeoutMs: 15000,
  getAccessToken: () => useAuthStore(pinia).token,
  refreshAccessToken,
  // 高风险接口签名密钥（客户端不落 AppSecret，需后端安全下发）
  getSignSecret: () => undefined,
  headers: {
    'X-Requested-With': 'XMLHttpRequest'
  }
})

/** 统一 CEAMS API 客户端（iam / masterdata / project 三域类型化工厂）。 */
export const ceamsApi = apiClient
