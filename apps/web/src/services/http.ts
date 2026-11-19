import { createCeamsApiClient } from '@ceams/api-client'
import type { CeamsApiClient } from '@ceams/api-client'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'
import { getOrCreateDeviceId } from '@/utils/device'

/**
 * 统一 API 基础地址：Web 开发态留空走 Vite 代理（/api），生产由 VITE_API_BASE_URL 注入。
 * 导出供需要自建 fetch 的流式请求（如 AI SSE）复用同一地址。
 */
export const apiBaseUrl: string = import.meta.env.VITE_API_BASE_URL ?? 'https://api.example.com'

/**
 * 刷新令牌轮换：用 refreshToken 换取新的令牌对并写回 auth store，
 * 返回新的 access token 供 HttpClient 重试原请求。
 * 单飞：同一时刻只允许一个刷新在飞，并发 401 共享同一个 Promise，
 * 避免一次性轮换的 refreshToken 被重复消费导致后续请求全部失败。
 */
let refreshPromise: Promise<string> | null = null

export function refreshAccessToken(): Promise<string> {
  if (refreshPromise) {
    return refreshPromise
  }
  const auth = useAuthStore(pinia)
  const currentRefreshToken = auth.refreshToken
  if (!currentRefreshToken) {
    return Promise.reject(new Error('AUTH_TOKEN_EXPIRED'))
  }
  const pending = apiClient.iam.refresh({ refreshToken: currentRefreshToken })
    .then((data) => {
      auth.setSession(data)
      return data.tokenPair.accessToken
    })
    .finally(() => {
      // 成功或失败都在此处清空，保证下一次令牌过期能重新发起新一轮刷新。
      refreshPromise = null
    })
  refreshPromise = pending
  return pending
}

// 签名密钥缓存：后端短期下发，带过期刷新；客户端不落 AppSecret
let cachedSignSecret: string | null = null
let cachedSignSecretExpiresAt = 0

/**
 * 获取高危接口 HMAC 签名密钥：命中缓存直接返回，临近过期（提前 60s）时向后端重新获取。
 * 获取失败返回 null，本次请求将不携带签名头（服务端会拒绝高危接口，避免越权执行）。
 */
async function getSignSecret(): Promise<string | null> {
  const now = Date.now()
  if (cachedSignSecret && now < cachedSignSecretExpiresAt - 60000) {
    return cachedSignSecret
  }
  try {
    const data = await apiClient.iam.signSecret(getOrCreateDeviceId())
    cachedSignSecret = data.secret
    cachedSignSecretExpiresAt = data.expiresAt
    return cachedSignSecret
  } catch {
    return null
  }
}

/**
 * 统一请求客户端：业务代码只经这里访问接口，组件内禁止直接 axios/fetch。
 * 三端复用同一套 @ceams/api-client，仅注入不同的平台适配器（默认 fetchAdapter）。
 * 说明：factory 调用位于文件末尾，上方函数体内的引用在模块初始化完成后才会执行。
 */
const apiClient: CeamsApiClient = createCeamsApiClient({
  baseUrl: apiBaseUrl,
  timeoutMs: 15000,
  getAccessToken: () => useAuthStore(pinia).token,
  refreshAccessToken,
  // 高风险接口签名密钥（客户端不落 AppSecret，需后端安全下发）
  getSignSecret,
  // 设备指纹（签名请求携带 X-Device-Id 头）
  getDeviceId: () => getOrCreateDeviceId(),
  headers: {
    'X-Requested-With': 'XMLHttpRequest'
  }
})

/** 统一 CEAMS API 客户端（iam / masterdata / project / expense / payment / bank / seal 六域类型化工厂）。 */
export const ceamsApi = apiClient

/**
 * 认证态二进制拉取：文档预览接口要求 Authorization 头，无法用 `<img src>` / `window.open` 直接访问，
 * 故统一在此处携带令牌取回 Blob，由调用方生成 object URL 后展示。
 */
export async function fetchBlob(path: string): Promise<Blob> {
  const token = useAuthStore(pinia).token
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }
  return response.blob()
}

/** 从统一响应包裹中取后端 message，取不到按 HTTP 状态码兜底。 */
async function readErrorMessage(response: Response): Promise<string> {
  try {
    const text = await response.text()
    const parsed = JSON.parse(text) as { message?: unknown }
    if (typeof parsed.message === 'string' && parsed.message) {
      return parsed.message
    }
  } catch {
    // 非 JSON 响应（如网关错误页）按状态码提示
  }
  return `请求失败（HTTP ${response.status}）`
}
