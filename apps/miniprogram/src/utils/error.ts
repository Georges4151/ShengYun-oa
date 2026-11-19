import { ApiError } from '@ceams/api-client'
import { ERROR_CODES } from '@ceams/types'

/** 后端业务错误码 -> 中文提示（未命中时回退原始消息）。 */
const FRIENDLY_MESSAGES: Record<string, string> = {
  [ERROR_CODES.AUTH_TOKEN_INVALID]: '登录已失效，请重新登录'
}

/** 从任意错误中提取可读消息，供 uni.showToast 使用。 */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof ApiError && err.message) return FRIENDLY_MESSAGES[err.message] ?? err.message
  if (err instanceof Error && err.message) return err.message
  return fallback
}

/** 判断错误是否为「当前账号未绑定员工档案」（后端 BIZ_NOT_FOUND）。 */
export function isEmployeeUnbound(err: unknown): boolean {
  if (err instanceof ApiError) {
    return err.status === 404 || err.code === 404001 || err.message.includes('未绑定员工档案')
  }
  return err instanceof Error && err.message.includes('未绑定员工档案')
}
