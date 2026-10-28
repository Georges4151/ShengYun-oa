/**
 * Web 端设备指纹工具。
 *
 * 设备指纹用于账号绑定与高风险接口签名（X-Device-Id），
 * 首次生成后持久化到 localStorage，保证同一浏览器会话跨刷新稳定。
 */
export const DEVICE_ID_KEY = 'ceams.device_id'

function randomDeviceId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 12)}`
}

function readStored(): string | null {
  try {
    const value = window.localStorage.getItem(DEVICE_ID_KEY)
    return value && value.length > 0 ? value : null
  } catch {
    return null
  }
}

function writeStored(value: string): void {
  try {
    window.localStorage.setItem(DEVICE_ID_KEY, value)
  } catch {
    // localStorage 不可用时忽略（如隐私模式），指纹仅当前会话有效
  }
}

/** 获取或生成本设备指纹。 */
export function getOrCreateDeviceId(): string {
  const existing = readStored()
  if (existing) return existing
  const id = randomDeviceId()
  writeStored(id)
  return id
}
