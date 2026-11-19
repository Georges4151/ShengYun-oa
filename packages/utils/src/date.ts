import type { DateTime } from '@ceams/types'

function toDate(value: DateTime | Date | number | null | undefined): Date | null {
  if (value === null || value === undefined) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  if (typeof value === 'number') return new Date(value)
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? null : d
}

function pad(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

/** 当前时间 ISO8601 字符串 */
export function nowIso(): DateTime {
  return new Date().toISOString()
}

/**
 * 格式化为 "YYYY-MM-DD HH:mm:ss"。
 * 跨端统一展示格式，避免依赖 dayjs/moment。
 */
export function formatDateTime(
  value: DateTime | Date | number | null | undefined,
  fallback = '--'
): string {
  const d = toDate(value)
  if (!d) return fallback
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    ` ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}

/** 格式化为 "YYYY-MM-DD" */
export function formatDate(
  value: DateTime | Date | number | null | undefined,
  fallback = '--'
): string {
  const d = toDate(value)
  if (!d) return fallback
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 解析 ISO 字符串为 Date（无效返回 null） */
export function parseIso(value: DateTime | string): Date | null {
  return toDate(value)
}