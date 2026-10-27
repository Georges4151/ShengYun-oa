/**
 * 高风险接口签名工具（HMAC-SHA256）。
 *
 * 契约：
 * - 原文 = timestamp + nonce + bodyHash
 * - bodyHash = 请求体为空的 SHA-256 hex，空请求体为空串
 * - X-Sign = HMAC-SHA256(secret, 原文) 的 hex，小写
 *
 * 实现基于 Web Crypto API（浏览器 / Node 18+ 均可使用）。
 * 微信小程序无 crypto.subtle，需由平台适配层注入等价实现（见 api-client 签名钩子）。
 */

const encoder = new TextEncoder()

function toHex(buffer: ArrayBuffer): string {
  return Array.from(new Uint8Array(buffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

/** SHA-256 hex */
export async function sha256Hex(message: string): Promise<string> {
  const data = encoder.encode(message)
  const digest = await crypto.subtle.digest('SHA-256', data)
  return toHex(digest)
}

/** HMAC-SHA256 hex（小写） */
export async function hmacSha256Hex(secret: string, message: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(message))
  return toHex(signature)
}

/** 拼接签名原文：timestamp + nonce + bodyHash */
export function buildSignPayload(timestamp: string, nonce: string, bodyHash: string): string {
  return `${timestamp}${nonce}${bodyHash}`
}

/**
 * 一站式签名：对请求体字符串计算 X-Sign。
 * @param secret 客户端签名密钥（勿在前端硬编码，需后端安全下发）
 * @param timestamp 毫秒时间戳字符串
 * @param nonce 一次性随机串
 * @param body 已序列化的请求体字符串（可为空串）
 */
export async function sign(
  secret: string,
  timestamp: string,
  nonce: string,
  body: string
): Promise<string> {
  const bodyHash = body ? await sha256Hex(body) : ''
  return hmacSha256Hex(secret, buildSignPayload(timestamp, nonce, bodyHash))
}