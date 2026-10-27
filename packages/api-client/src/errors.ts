import type { ApiResponse } from '@ceams/types'

/**
 * 统一错误：包装业务错误码与传输层错误。
 */
export class ApiError<T = unknown> extends Error {
  override readonly name = 'ApiError'
  /** 业务码 / HTML 状态码前缀码 */
  readonly code: string
  /** HTTP 状态码；网络层错误为 -1 */
  readonly status: number
  readonly traceId: string | undefined
  readonly data: T | undefined
  readonly timestamp: string | undefined

  constructor(init: {
    code: string
    message: string
    status?: number
    traceId?: string
    data?: T
    timestamp?: string
  }) {
    super(init.message)
    this.code = init.code
    this.status = init.status ?? -1
    this.traceId = init.traceId
    this.timestamp = init.timestamp
    this.data = init.data
  }
}

/** 从后端 ApiResponse 构造业务错误 */
export function toApiError<T>(body: ApiResponse<T>, status: number): ApiError<T> {
  return new ApiError<T>({
    code: body.code,
    message: body.message,
    status,
    traceId: body.traceId,
    data: body.data,
    timestamp: body.timestamp
  })
}