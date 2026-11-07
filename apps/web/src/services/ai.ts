import type {
  AiChatDeltaData,
  AiChatDoneData,
  AiChatErrorData,
  AiChatEvent,
  AiChatMetaData,
  AiChatRequest,
  AiChatToolData
} from '@ceams/types'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'
import { apiBaseUrl, refreshAccessToken } from './http'

/**
 * AI 流式对话（SSE）客户端。
 *
 * `POST /api/v1/ai/chat` 是 `text/event-stream` 流式响应，无法使用 `EventSource`
 * （其只支持 GET 且不能自定义 Authorization 头），因此这里用 `fetch` +
 * `ReadableStream` 手动解析事件块：
 * - 事件块以空行分隔，块内逐行解析 `event:` / `data:`（忽略 `:` 注释行）
 * - 一个块可能有多行 `data:`，按 SSE 规范以换行拼接后再 JSON.parse
 * - 支持 AbortController：调用方 abort 后 fetch / reader.read() 抛 AbortError
 * - 401 时复用 http.ts 的单飞 `refreshAccessToken`，刷新成功后重试一次
 */

/** AI 流式事件回调集合（按事件名分发，另提供 onEvent 统一出口便于埋点）。 */
export interface AiStreamHandlers {
  onMeta?: (data: AiChatMetaData) => void
  onDelta?: (data: AiChatDeltaData) => void
  onTool?: (data: AiChatToolData) => void
  onDone?: (data: AiChatDoneData) => void
  onError?: (data: AiChatErrorData) => void
  /** 每个 SSE 事件的统一回调 */
  onEvent?: (event: AiChatEvent) => void
}

/** 单行 SSE 帧：事件名（缺省 message）+ 已拼接的 data 文本。 */
interface SseFrame {
  event: string
  data: string
}

const CHAT_PATH = '/api/v1/ai/chat'

function chatUrl(): string {
  return `${apiBaseUrl.replace(/\/+$/, '')}${CHAT_PATH}`
}

function currentToken(): string | null {
  return useAuthStore(pinia).token
}

async function postChat(req: AiChatRequest, token: string | null, signal?: AbortSignal): Promise<Response> {
  return fetch(chatUrl(), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(req),
    signal
  })
}

/**
 * 提取错误响应体的可展示消息：后端统一包裹为 `{ code, message, ... }`；
 * 非 JSON 响应取原文片段，避免把整段 HTML/堆栈抛给界面。
 */
async function readErrorMessage(res: Response): Promise<string> {
  const fallback = `AI 请求失败（HTTP ${res.status}）`
  try {
    const text = await res.text()
    if (!text) return fallback
    try {
      const parsed = JSON.parse(text) as { message?: unknown }
      if (typeof parsed.message === 'string' && parsed.message) {
        return parsed.message
      }
    } catch {
      // 非 JSON：继续按原文片段返回
    }
    return text.length > 200 ? `${text.slice(0, 200)}…` : text
  } catch {
    return fallback
  }
}

/** 解析单个 SSE 事件块；无 data 行（如心跳注释块）返回 null。 */
function parseSseBlock(rawBlock: string): SseFrame | null {
  const dataLines: string[] = []
  let eventName = ''
  for (const line of rawBlock.split('\n')) {
    if (!line || line.startsWith(':')) continue
    const colon = line.indexOf(':')
    const field = colon === -1 ? line : line.slice(0, colon)
    let value = colon === -1 ? '' : line.slice(colon + 1)
    if (value.startsWith(' ')) value = value.slice(1)
    if (field === 'event') {
      eventName = value.trim()
    } else if (field === 'data') {
      dataLines.push(value)
    }
  }
  if (dataLines.length === 0) return null
  return { event: eventName || 'message', data: dataLines.join('\n') }
}

/**
 * 分发一个事件块，返回是否终止读取：
 * - done / error 视为终止事件（服务端随后关闭流，这里主动 cancel 释放连接）
 * - 未知事件名或非法 JSON 忽略，保证流不因单帧异常而整体失败
 */
function dispatchSseBlock(rawBlock: string, handlers: AiStreamHandlers): boolean {
  const frame = parseSseBlock(rawBlock)
  if (!frame) return false
  let payload: unknown
  try {
    payload = JSON.parse(frame.data)
  } catch {
    return false
  }

  switch (frame.event) {
    case 'meta': {
      const data = payload as AiChatMetaData
      handlers.onMeta?.(data)
      handlers.onEvent?.({ event: 'meta', data })
      return false
    }
    case 'delta': {
      const data = payload as AiChatDeltaData
      handlers.onDelta?.(data)
      handlers.onEvent?.({ event: 'delta', data })
      return false
    }
    case 'tool': {
      const data = payload as AiChatToolData
      handlers.onTool?.(data)
      handlers.onEvent?.({ event: 'tool', data })
      return false
    }
    case 'done': {
      const data = payload as AiChatDoneData
      handlers.onDone?.(data)
      handlers.onEvent?.({ event: 'done', data })
      return true
    }
    case 'error': {
      const data = payload as AiChatErrorData
      handlers.onError?.(data)
      handlers.onEvent?.({ event: 'error', data })
      return true
    }
    default:
      return false
  }
}

/** 判定异常是否为 AbortController 主动中断（停止生成 / 组件卸载）。 */
export function isAbortError(err: unknown): boolean {
  if (err instanceof DOMException) return err.name === 'AbortError'
  return err instanceof Error && err.name === 'AbortError'
}

/**
 * 发起一次 AI 流式对话，直到服务端结束或主动中断。
 *
 * 注意：网络/HTTP 层失败以 Promise reject 抛出；服务端 `error` 事件通过
 * `handlers.onError` 回调，不 reject（便于界面保留已生成内容并在气泡内提示）。
 */
export async function streamChat(
  req: AiChatRequest,
  handlers: AiStreamHandlers = {},
  signal?: AbortSignal
): Promise<void> {
  let res = await postChat(req, currentToken(), signal)
  if (res.status === 401) {
    // 令牌过期：复用 http.ts 的单飞刷新，成功后携带新令牌重试一次
    const token = await refreshAccessToken()
    res = await postChat(req, token, signal)
  }
  if (!res.ok) {
    throw new Error(await readErrorMessage(res))
  }

  // 后端未开启 AI / 降级时可能直接返回 JSON（HTTP 200 + 业务错误码），
  // 此时不能按 SSE 解析，统一转成可展示错误。
  const contentType = res.headers.get('content-type') ?? ''
  if (!contentType.includes('text/event-stream')) {
    throw new Error(await readErrorMessage(res))
  }

  const body = res.body
  if (!body) {
    throw new Error('当前环境不支持流式响应（ReadableStream 不可用）')
  }

  const reader = body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  try {
    for (;;) {
      const chunk = await reader.read()
      if (chunk.done) break
      buffer += decoder.decode(chunk.value, { stream: true })
      // 统一换行后按空行切分事件块（兼容 \r\n 与 Windows 代理改写）
      buffer = buffer.replace(/\r\n?/g, '\n')
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        const rawBlock = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        if (dispatchSseBlock(rawBlock, handlers)) {
          await reader.cancel().catch(() => undefined)
          return
        }
        boundary = buffer.indexOf('\n\n')
      }
    }

    // 流已关闭：冲刷解码器并处理最后一个可能缺少结尾空行的块
    buffer += decoder.decode()
    buffer = buffer.replace(/\r\n?/g, '\n')
    const tail = buffer.trim()
    if (tail) {
      dispatchSseBlock(tail, handlers)
    }
  } finally {
    reader.releaseLock()
  }
}
