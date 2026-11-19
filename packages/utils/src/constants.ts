/** 应用名称（英文/中文） */
export const APP_NAME = 'CEAMS'
export const APP_NAME_CN = '建筑企业账目管理系统'

/** HTTP 约定的请求头名称（与 OpenAPI 契约一致） */
export const HTTP_HEADERS = {
  AUTHORIZATION: 'Authorization',
  SIGN: 'X-Sign',
  TIMESTAMP: 'X-Timestamp',
  NONCE: 'X-Nonce',
  TRACE_ID: 'X-Trace-Id'
} as const

/** 分页默认与上限 */
export const DEFAULT_PAGE_SIZE = 20
export const MAX_PAGE_SIZE = 100

/** 签名时间窗口（毫秒）：±5 分钟，与服务端一致 */
export const SIGN_TIMESTAMP_WINDOW_MS = 5 * 60 * 1000

/** 常用校验正则 */
export const REGEXP = {
  /** 中国大陆手机号 */
  PHONE: /^1[3-9]\d{9}$/,
  /** 身份证号（18 位） */
  ID_CARD: /^\d{17}[\dXx]$/,
  /** 金额字符串（可选负号 + 最多两位小数） */
  AMOUNT: /^-?\d+(\.\d{1,2})?$/
} as const