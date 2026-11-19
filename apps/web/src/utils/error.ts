import { ApiError } from '@ceams/api-client'
import { ERROR_CODES } from '@ceams/types'

/** 后端业务错误码 -> 友好中文提示映射（覆盖常用场景）。 */
const FRIENDLY_MESSAGES: Record<string, string> = {
  [ERROR_CODES.AUTH_INVALID_CREDENTIALS]: '账号或密码错误',
  [ERROR_CODES.AUTH_TOKEN_INVALID]: '登录已失效，请重新登录',
  [ERROR_CODES.AUTH_ACCESS_DENIED]: '无权限执行该操作',
  [ERROR_CODES.VALID_REQUIRED_FIELD]: '存在必填字段未填写',
  [ERROR_CODES.VALID_ILLEGAL_ARGUMENT]: '参数不合法',
  [ERROR_CODES.BIZ_NOT_FOUND]: '数据不存在或已被删除',
  [ERROR_CODES.BIZ_DUPLICATE_CODE]: '编码或名称重复',
  [ERROR_CODES.BIZ_STATE_CONFLICT]: '数据状态冲突，无法执行',
  [ERROR_CODES.BIZ_REFERENCED]: '数据已被引用，无法删除',
  [ERROR_CODES.SYSTEM_RATE_LIMITED]: '请求过于频繁，请稍后再试',
  [ERROR_CODES.SYSTEM_INTERNAL_ERROR]: '系统内部错误，请稍后再试'
}

/** 前端哨兵 / 浏览器原生英文错误 -> 中文提示。 */
const RAW_MESSAGE_ALIASES: Record<string, string> = {
  AUTH_TOKEN_EXPIRED: '登录已超时，请重新登录',
  'Network Error': '网络异常，请检查网络后重试',
  'Failed to fetch': '网络异常，请检查网络后重试'
}

/**
 * 将任意异常归一化为可展示的中文错误消息。
 * 优先匹配业务错误码的友好文案，其次匹配前端哨兵文案，最后回退到原始消息。
 */
export function apiErrorMessage(err: unknown, fallback = '操作失败，请稍后重试'): string {
  if (err instanceof ApiError) {
    return (
      FRIENDLY_MESSAGES[err.message] ?? RAW_MESSAGE_ALIASES[err.message] ?? (err.message || fallback)
    )
  }
  if (err instanceof Error) {
    return RAW_MESSAGE_ALIASES[err.message] ?? (err.message || fallback)
  }
  return fallback
}
