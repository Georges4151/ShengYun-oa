import { HttpClient } from '@ceams/api-client'
import { uniRequestAdapter } from './request'

/**
 * 小程序统一请求客户端：注入 uni.request 适配器。
 * 业务代码只经这里访问接口，组件内禁止 uni.request / wx.request。
 */
export const httpClient = new HttpClient({
  baseUrl: 'https://api.example.com', // 占位：后端公网 HTTPS（待契约冻结后替换）
  adapter: uniRequestAdapter,
  timeoutMs: 15000,
  getAccessToken: () => (uni.getStorageSync('ceams_access_token') as string) || null,
  getSignSecret: () => undefined
})

/** 登录占位：待后端 iam 契约冻结后接入 */
export async function loginByPhone(_phone: string): Promise<void> {
  // 待接入：httpClient.post<LoginResult>('/auth/login', { phone })
}