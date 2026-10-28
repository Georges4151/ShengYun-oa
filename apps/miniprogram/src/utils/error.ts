import { ApiError } from '@ceams/api-client'

/** 从任意错误中提取可读消息，供 uni.showToast 使用。 */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof ApiError && err.message) return err.message
  if (err instanceof Error && err.message) return err.message
  return fallback
}
