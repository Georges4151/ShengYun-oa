/**
 * 统一 API 响应与分页结构（与 OpenAPI v1 契约一致）。
 */

/**
 * 统一响应包裹：{ code, message, data, traceId, timestamp }
 * 由后端全局响应包装器产出。
 */
export interface ApiResponse<T = unknown> {
  /** 业务码：整数，0 表示成功，非 0 为错误码（如 401001） */
  code: number
  /** 人类可读的消息 */
  message: string
  /** 业务数据 */
  data: T
  /** 链路追踪 ID，用于日志关联 */
  traceId: string
  /** 响应时间（ISO8601） */
  timestamp: string
}

/**
 * 统一分页结果：{ records, total, page, size }
 * 空列表返回 []（records = []，total = 0）。
 */
export interface PageResult<T> {
  /** 当前页数据列表 */
  records: T[]
  /** 总记录数 */
  total: number
  /** 当前页码（从 1 开始） */
  page: number
  /** 每页大小 */
  size: number
}

/**
 * 分页查询入参：page/size。
 * 默认每页 20，上限 100。
 */
export interface PageQuery {
  page: number
  size: number
}