import type { Money } from '@ceams/types'

/** 金额是否是合法的字符串金额 */
export function isMoney(value: unknown): value is Money {
  return typeof value === 'string' && value !== '' && !Number.isNaN(Number(value))
}

/** 金额转 number（无效返回 0） */
export function toNumber(value: Money | number | null | undefined): number {
  if (typeof value === 'number') return value
  const n = Number(value ?? 0)
  return Number.isNaN(n) ? 0 : n
}

export interface FormatMoneyOptions {
  /** 小数位数，默认 2 */
  decimals?: number
  /** 货币符号；传 false 表示去掉符号，传入字符串表示自定义符号，默认空串 */
  symbol?: string | false
}

/**
 * 金额格式化：千分位 + 固定小数位。
 * 示例：formatMoney('1280.5') => '1,280.50'
 */
export function formatMoney(
  value: Money | number | null | undefined,
  options: FormatMoneyOptions = {}
): string {
  const decimals = options.decimals ?? 2
  const symbol = options.symbol ?? ''
  const num = toNumber(value)
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--'
  }
  const formatted = num.toLocaleString('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
  return symbol === false ? formatted : `${symbol}${formatted}`
}

/** 元转分（整数），用于与后端以"分"交互时使用 */
export function yuanToFen(value: Money | number): number {
  return Math.round(toNumber(value) * 100)
}

/** 分转元（字符串，保留两位小数） */
export function fenToYuan(value: number): Money {
  return (value / 100).toFixed(2)
}