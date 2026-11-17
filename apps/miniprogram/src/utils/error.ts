import { ApiError } from '@ceams/api-client'

/** 从任意错误中提取可读消息，供 uni.showToast 使用。 */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof ApiError && err.message) return err.message
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
