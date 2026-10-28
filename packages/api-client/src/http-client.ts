import type { ApiResponse } from '@ceams/types'
import { buildSignPayload, hmacSha256Hex, sha256Hex } from '@ceams/utils'
import { fetchAdapter } from './adapters/fetch'
import { ApiError, toApiError } from './errors'
import type {
  HttpHeaders,
  HttpMethod,
  HttpRequest,
  HttpResponse,
  QueryValue,
  RequestAdapter,
  RequestInterceptor,
  RequestOptions,
  ResponseInterceptor
} from './types'

export interface HttpClientOptions {
  /** 服务基础地址，如 https://api.example.com（三端统一公网 HTTPS） */
  baseUrl?: string
  /** 平台请求适配器，默认 fetchAdapter */
  adapter?: RequestAdapter
  /** 默认请求头 */
  headers?: HttpHeaders
  /** 超时（毫秒），默认 15000 */
  timeoutMs?: number
  /** 获取访问令牌（Access Token）的钩子 */
  getAccessToken?: () => string | null | undefined | Promise<string | null | undefined>
  /** 获取签名密钥（Secret）的钩子；设置后可为 signed 请求自动生成签名头 */
  getSignSecret?: () => string | null | undefined | Promise<string | null | undefined>
  /** 获取设备指纹（Device Id）的钩子；签名请求会同时携带 X-Device-Id 头 */
  getDeviceId?: () => string | null | undefined | Promise<string | null | undefined>
  /** 判定请求是否签名，默认读 request.signed */
  shouldSign?: (request: HttpRequest) => boolean
  /** nonce 生成器 */
  generateNonce?: () => string
  /** 刷新访问令牌；未设置则禁用刷新重试 */
  refreshAccessToken?: () => string | Promise<string>
  /** 判定错误是否触发刷新重试，默认 err.message === 'AUTH_TOKEN_EXPIRED' */
  shouldRefresh?: (error: ApiError<unknown>) => boolean
  /** 登出 / 清理回调 */
  onLogout?: () => void
}

const DEFAULT_TIMEOUT_MS = 15000
const SUCCESS_CODE = 0
const ACCESS_TOKEN_EXPIRED = 'AUTH_TOKEN_EXPIRED'
/** 传输层错误哨兵码：网络超时（负数，区分后端正数业务码） */
const NETWORK_TIMEOUT = -1
/** 传输层错误哨兵码：响应体非法（负数，区分后端正数业务码） */
const INVALID_RESPONSE = -2

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isFormData(value: unknown): value is FormData {
  return typeof FormData !== 'undefined' && value instanceof FormData
}

function serializeBody(body: unknown): string | FormData | undefined {
  if (body === undefined || body === null) return undefined
  if (typeof body === 'string') return body
  if (isFormData(body)) return body
  return JSON.stringify(body)
}

function buildQuery(url: string, query?: Record<string, QueryValue>): string {
  if (!query) return url
  const params: string[] = []
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null) continue
    params.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  }
  if (params.length === 0) return url
  const separator = url.includes('?') ? '&' : '?'
  return `${url}${separator}${params.join('&')}`
}

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  if (!isPlainObject(value)) return false
  return typeof (value as { code?: unknown }).code === 'number' && 'message' in value && 'data' in value
}

function defaultNonce(): string {
  const g = globalThis as unknown as { crypto?: { randomUUID?: () => string } }
  const uuid = g.crypto?.randomUUID?.()
  if (uuid) return uuid.replace(/-/g, '')
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 12)}`
}

/**
 * 统一 HTTP 客户端：基于抽象 RequestAdapter，可注入 fetch / axios / wx.request。
 * 内置：请求/响应拦截器、鉴权头、签名头、统一解包、错误归一化、refresh 重试（占位）。
 */
export class HttpClient {
  readonly requestInterceptors: RequestInterceptor[] = []
  readonly responseInterceptors: ResponseInterceptor[] = []

  private readonly options: HttpClientOptions

  constructor(options: HttpClientOptions = {}) {
    this.options = options
  }

  get<T>(path: string, init?: RequestOptions): Promise<T> {
    return this.request<T>('GET', path, init)
  }

  post<T>(path: string, body?: unknown, init?: RequestOptions): Promise<T> {
    return this.request<T>('POST', path, { ...(init ?? {}), body })
  }

  put<T>(path: string, body?: unknown, init?: RequestOptions): Promise<T> {
    return this.request<T>('PUT', path, { ...(init ?? {}), body })
  }

  patch<T>(path: string, body?: unknown, init?: RequestOptions): Promise<T> {
    return this.request<T>('PATCH', path, { ...(init ?? {}), body })
  }

  delete<T>(path: string, init?: RequestOptions): Promise<T> {
    return this.request<T>('DELETE', path, init)
  }

  async request<T>(method: HttpMethod, path: string, init: RequestOptions = {}): Promise<T> {
    let refreshed = false
    for (;;) {
      try {
        return await this.execute<T>(method, path, init)
      } catch (err) {
        const canRefresh =
          !refreshed &&
          this.options.refreshAccessToken !== undefined &&
          err instanceof ApiError &&
          (this.options.shouldRefresh?.(err) ?? err.message === ACCESS_TOKEN_EXPIRED)
        if (!canRefresh) throw err
        refreshed = true
        await this.options.refreshAccessToken?.()
      }
    }
  }

  private async execute<T>(method: HttpMethod, path: string, init: RequestOptions): Promise<T> {
    const baseUrl = (this.options.baseUrl ?? '').replace(/\/+$/, '')
    const hasBody = method === 'POST' || method === 'PUT' || method === 'PATCH'
    const bodyText = hasBody ? serializeBody(init.body) : undefined

    let request: HttpRequest = {
      method,
      url: buildQuery(`${baseUrl}${path}`, init.query),
      headers: { ...(this.options.headers ?? {}) },
      body: bodyText,
      signed: init.signed ?? false
    }

    if (hasBody && bodyText !== undefined && !isFormData(bodyText)) {
      request.headers['Content-Type'] = 'application/json'
    }
    if (init.headers) {
      request.headers = { ...request.headers, ...init.headers }
    }

    for (const interceptor of this.requestInterceptors) {
      request = await interceptor(request)
    }

    if (!init.skipAuth && this.options.getAccessToken) {
      const token = await this.options.getAccessToken()
      if (token) request.headers['Authorization'] = `Bearer ${token}`
    }

    const secret = this.options.getSignSecret ? await this.options.getSignSecret() : null
    const needSign = secret != null && (this.options.shouldSign?.(request) ?? request.signed === true)
    if (needSign && secret) {
      const timestamp = Date.now().toString()
      const nonce = (this.options.generateNonce ?? defaultNonce)()
      const bodyHash = typeof request.body === 'string' && request.body ? await sha256Hex(request.body) : ''
      request.headers['X-Sign'] = await hmacSha256Hex(secret, buildSignPayload(timestamp, nonce, bodyHash))
      request.headers['X-Timestamp'] = timestamp
      request.headers['X-Nonce'] = nonce
      const deviceId = this.options.getDeviceId ? await this.options.getDeviceId() : null
      if (deviceId) request.headers['X-Device-Id'] = deviceId
    }

    let response = await this.transport(request)
    for (const interceptor of this.responseInterceptors) {
      response = await interceptor(response)
    }

    return this.unwrap<T>(response, init)
  }

  private async transport(request: HttpRequest): Promise<HttpResponse> {
    const adapter = this.options.adapter ?? fetchAdapter
    const timeoutMs = this.options.timeoutMs ?? DEFAULT_TIMEOUT_MS
    if (timeoutMs <= 0) return adapter.send(request)

    let timer: ReturnType<typeof setTimeout> | undefined
    const timeout = new Promise<never>((_, reject) => {
      timer = setTimeout(() => {
        reject(new ApiError<unknown>({ code: NETWORK_TIMEOUT, message: `请求超时（${timeoutMs}ms）` }))
      }, timeoutMs)
    })
    try {
      return await Promise.race([adapter.send(request), timeout])
    } finally {
      if (timer) clearTimeout(timer)
    }
  }

  private async unwrap<T>(response: HttpResponse, init: RequestOptions): Promise<T> {
    if (init.raw) {
      return response.body as unknown as T
    }

    let parsed: unknown
    try {
      parsed = response.body ? JSON.parse(response.body) : null
    } catch {
      throw new ApiError<unknown>({
        code: INVALID_RESPONSE,
        message: '响应体不是合法 JSON',
        status: response.status
      })
    }

    if (!isApiResponse(parsed)) {
      throw new ApiError<unknown>({
        code: INVALID_RESPONSE,
        message: '响应体缺少统一包裹结构',
        status: response.status,
        data: parsed
      })
    }

    if (parsed.code !== SUCCESS_CODE) {
      throw toApiError(parsed, response.status)
    }

    return parsed.data as T
  }
}