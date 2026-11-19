/**
 * 高风险接口签名工具（HMAC-SHA256）。
 *
 * 契约：
 * - 原文 = timestamp + nonce + bodyHash
 * - bodyHash = 请求体为空的 SHA-256 hex，空请求体为空串
 * - X-Sign = HMAC-SHA256(secret, 原文) 的 hex，小写
 *
 * 实现分两条路径：
 * - Web / Electron 渲染层 / Node 18+：优先使用 Web Crypto（crypto.subtle），更高效；
 * - 微信小程序：无 crypto.subtle，自动回退到内置的纯 JS SHA-256 + HMAC-SHA256，
 *   输出与 Web Crypto 完全一致（小写 hex）。
 */

// ===== UTF-8 编码（纯 JS，兼容小程序无 TextEncoder 的环境） =====
function utf8Encode(input: string): Uint8Array<ArrayBuffer> {
  if (typeof TextEncoder !== 'undefined') {
    return new TextEncoder().encode(input)
  }
  const bytes: number[] = []
  for (let i = 0; i < input.length; i++) {
    let code = input.charCodeAt(i)
    if (code >= 0xd800 && code <= 0xdbff && i + 1 < input.length) {
      const next = input.charCodeAt(i + 1)
      if (next >= 0xdc00 && next <= 0xdfff) {
        code = 0x10000 + ((code - 0xd800) << 10) + (next - 0xdc00)
        i++
      }
    }
    if (code < 0x80) {
      bytes.push(code)
    } else if (code < 0x800) {
      bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f))
    } else if (code < 0x10000) {
      bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f))
    } else {
      bytes.push(
        0xf0 | (code >> 18),
        0x80 | ((code >> 12) & 0x3f),
        0x80 | ((code >> 6) & 0x3f),
        0x80 | (code & 0x3f)
      )
    }
  }
  return new Uint8Array(bytes)
}

// ===== 纯 JS SHA-256（RFC 6234，无任何运行时依赖） =====
const SHA256_K = new Uint32Array([
  0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4,
  0xab1c5ed5, 0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe,
  0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f,
  0x4a7484aa, 0x5cb0a9dc, 0x76f988da, 0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
  0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138, 0x4d2c6dfc,
  0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, 0xa2bfe8a1, 0xa81a664b,
  0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070, 0x19a4c116,
  0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
  0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7,
  0xc67178f2
])

const SHA256_H = new Uint32Array([
  0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab,
  0x5be0cd19
])

function rotr(x: number, n: number): number {
  return (x >>> n) | (x << (32 - n))
}

/** 纯 JS SHA-256：对 UTF-8 字节计算 32 字节摘要。 */
export function sha256Bytes(data: Uint8Array): Uint8Array {
  const bitLen = data.length * 8
  const paddedLength = Math.ceil((data.length + 9) / 64) * 64
  const padded = new Uint8Array(paddedLength)
  padded.set(data)
  padded[data.length] = 0x80

  const view = new DataView(padded.buffer)
  // 64-bit 消息长度（bit），大端写入填充块末尾
  view.setUint32(paddedLength - 8, Math.floor(bitLen / 0x100000000))
  view.setUint32(paddedLength - 4, bitLen >>> 0)

  const state = new Uint32Array(SHA256_H)
  const w = new Uint32Array(64)

  for (let offset = 0; offset < paddedLength; offset += 64) {
    for (let i = 0; i < 16; i++) {
      w[i] = view.getUint32(offset + i * 4)
    }
    for (let i = 16; i < 64; i++) {
      const s0 = rotr(w[i - 15], 7) ^ rotr(w[i - 15], 18) ^ (w[i - 15] >>> 3)
      const s1 = rotr(w[i - 2], 17) ^ rotr(w[i - 2], 19) ^ (w[i - 2] >>> 10)
      w[i] = (w[i - 16] + s0 + w[i - 7] + s1) >>> 0
    }

    let a = state[0]
    let b = state[1]
    let c = state[2]
    let d = state[3]
    let e = state[4]
    let f = state[5]
    let g = state[6]
    let h = state[7]

    for (let i = 0; i < 64; i++) {
      const s1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25)
      const ch = (e & f) ^ (~e & g)
      const temp1 = (h + s1 + ch + SHA256_K[i] + w[i]) >>> 0
      const s0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22)
      const maj = (a & b) ^ (a & c) ^ (b & c)
      const temp2 = (s0 + maj) >>> 0

      h = g
      g = f
      f = e
      e = (d + temp1) >>> 0
      d = c
      c = b
      b = a
      a = (temp1 + temp2) >>> 0
    }

    state[0] = (state[0] + a) >>> 0
    state[1] = (state[1] + b) >>> 0
    state[2] = (state[2] + c) >>> 0
    state[3] = (state[3] + d) >>> 0
    state[4] = (state[4] + e) >>> 0
    state[5] = (state[5] + f) >>> 0
    state[6] = (state[6] + g) >>> 0
    state[7] = (state[7] + h) >>> 0
  }

  const out = new Uint8Array(32)
  const outView = new DataView(out.buffer)
  for (let i = 0; i < 8; i++) {
    outView.setUint32(i * 4, state[i])
  }
  return out
}

/** 纯 JS HMAC-SHA256（RFC 2104）：返回 32 字节摘要。 */
export function hmacSha256Bytes(key: Uint8Array, message: Uint8Array): Uint8Array {
  const blockSize = 64
  const normalizedKey = key.length > blockSize ? sha256Bytes(key) : key

  const ipad = new Uint8Array(blockSize)
  const opad = new Uint8Array(blockSize)
  for (let i = 0; i < blockSize; i++) {
    const k = i < normalizedKey.length ? normalizedKey[i] : 0
    ipad[i] = k ^ 0x36
    opad[i] = k ^ 0x5c
  }

  const inner = new Uint8Array(blockSize + message.length)
  inner.set(ipad)
  inner.set(message, blockSize)
  const innerHash = sha256Bytes(inner)

  const outer = new Uint8Array(blockSize + innerHash.length)
  outer.set(opad)
  outer.set(innerHash, blockSize)
  return sha256Bytes(outer)
}

function hasSubtleCrypto(): boolean {
  try {
    return typeof crypto !== 'undefined' && crypto.subtle !== undefined
  } catch {
    return false
  }
}

function toHex(bytes: Uint8Array): string {
  let out = ''
  for (let i = 0; i < bytes.length; i++) {
    out += bytes[i].toString(16).padStart(2, '0')
  }
  return out
}

/** 纯 JS SHA-256 hex（小写），不依赖 crypto.subtle。导出供测试与跨端复用。 */
export function sha256HexPure(message: string): string {
  return toHex(sha256Bytes(utf8Encode(message)))
}

/** 纯 JS HMAC-SHA256 hex（小写），不依赖 crypto.subtle。导出供测试与跨端复用。 */
export function hmacSha256HexPure(secret: string, message: string): string {
  return toHex(hmacSha256Bytes(utf8Encode(secret), utf8Encode(message)))
}

/** SHA-256 hex（小写）；优先 Web Crypto，无 crypto.subtle 时回退纯 JS。 */
export async function sha256Hex(message: string): Promise<string> {
  if (hasSubtleCrypto()) {
    const digest = await crypto.subtle.digest('SHA-256', utf8Encode(message))
    return toHex(new Uint8Array(digest))
  }
  return sha256HexPure(message)
}

/** HMAC-SHA256 hex（小写）；优先 Web Crypto，无 crypto.subtle 时回退纯 JS。 */
export async function hmacSha256Hex(secret: string, message: string): Promise<string> {
  if (hasSubtleCrypto()) {
    const key = await crypto.subtle.importKey(
      'raw',
      utf8Encode(secret),
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    )
    const signature = await crypto.subtle.sign('HMAC', key, utf8Encode(message))
    return toHex(new Uint8Array(signature))
  }
  return hmacSha256HexPure(secret, message)
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
