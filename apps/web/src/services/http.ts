import { HttpClient, fetchAdapter } from '@ceams/api-client'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'

/**
 * 统一请求客户端：业务代码只经这里访问接口，组件内禁止直接 axios/fetch。
 * 三端复用同一套 @ceams/api-client，仅注入不同平台适配器。
 */
export const httpClient = new HttpClient({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'https://api.example.com',
  adapter: fetchAdapter,
  timeoutMs: 15000,
  getAccessToken: () => useAuthStore(pinia).token,
  // 高风险接口签名密钥（客户端不落 AppSecret，需后端安全下发）
  getSignSecret: () => undefined,
  headers: {
    'X-Requested-With': 'XMLHttpRequest'
  }
})