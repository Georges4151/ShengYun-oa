export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

/** 查询参数允许的原始值类型 */
export type QueryValue = string | number | boolean | null | undefined

/** 请求头映射 */
export interface HttpHeaders {
  [key: string]: string
}

/**
 * 已准备就绪的请求描述（body 已序列化为字符串）。
 * adapter 只负责传输，不关心签名 / 鉴权等业务逻辑。
 */
export interface HttpRequest {
  method: HttpMethod
  url: string
  headers: HttpHeaders
  query?: Record<string, QueryValue>
  /** 已序列化的请求体（JSON 字符串或 multipart FormData），GET/HEAD 为 undefined */
  body?: string | FormData
  /** 高风险签名接口标记 */
  signed?: boolean
  /** 本次请求超时（毫秒）；省略则沿用 HttpClient 默认超时，供适配器设置底层超时 */
  timeoutMs?: number
}

/** 传输层返回的原始响应 */
export interface HttpResponse {
  status: number
  statusText: string
  headers: Record<string, string>
  body: string
}

/**
 * 平台请求适配器抽象：
 * - Web / Electron 渲染层：fetchAdapter
 * - 微信小程序：uniRequestAdapter（见 apps/miniprogram）
 */
export interface RequestAdapter {
  send(request: HttpRequest): Promise<HttpResponse>
}

/** 请求拦截器：发送前改写请求 */
export type RequestInterceptor = (request: HttpRequest) => HttpRequest | Promise<HttpRequest>

/** 响应拦截器：解包前改写原始响应 */
export type ResponseInterceptor = (
  response: HttpResponse
) => HttpResponse | Promise<HttpResponse>

/** 业务调用入口的请求参数 */
export interface RequestOptions {
  /** 查询参数 */
  query?: Record<string, QueryValue>
  /** 请求体（未序列化，HttpClient 负责 JSON 序列化） */
  body?: unknown
  /** 额外请求头 */
  headers?: HttpHeaders
  /** 标记为高风险签名接口 */
  signed?: boolean
  /** 跳过统一 ApiResponse 解包（适合文件流 / 第三方响应） */
  raw?: boolean
  /** 跳过鉴权头（如登录接口） */
  skipAuth?: boolean
  /** 单次请求超时（毫秒），覆盖客户端默认 timeoutMs；<=0 表示不限制 */
  timeoutMs?: number
}
