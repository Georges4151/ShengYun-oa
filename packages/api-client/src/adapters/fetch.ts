import type { HttpRequest, HttpResponse, RequestAdapter } from '../types'

/**
 * 基于原生 fetch 的默认适配器。
 * 适用于 Web 与 Electron 渲染层（HTTPS）。
 */
export const fetchAdapter: RequestAdapter = {
  async send(request: HttpRequest): Promise<HttpResponse> {
    const init: RequestInit = {
      method: request.method,
      headers: request.headers,
      body: request.body
    }
    const resp = await fetch(request.url, init)
    const text = await resp.text()
    const headers: Record<string, string> = {}
    resp.headers.forEach((value, key) => {
      headers[key] = value
    })
    return {
      status: resp.status,
      statusText: resp.statusText,
      headers,
      body: text
    }
  }
}