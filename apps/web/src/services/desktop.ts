import type { Router } from 'vue-router'

/** 深链协议前缀（仅桌面端由主进程转发） */
const DEEP_LINK_PREFIX = 'ceams://'
/** 日报摘要深链：不是路由，交给 AI 抽屉处理 */
const SUMMARY_DEEP_LINK = 'ceams://ai/summary'

/**
 * 把 ceams:// 深链转换为应用内路由路径。
 * 例：ceams://todo → /todo；ceams://todo?tab=1 → /todo?tab=1。
 * 非 ceams:// 协议或空路径返回 null。
 */
export function deepLinkToPath(deepLink: string): string | null {
  const link = deepLink.trim()
  if (!link.toLowerCase().startsWith(DEEP_LINK_PREFIX)) return null
  const rest = link.slice(DEEP_LINK_PREFIX.length)
  if (!rest) return null
  return rest.startsWith('/') ? rest : `/${rest}`
}

/** 日报摘要深链 → 全局事件（与通知列表「查看日报摘要」同一入口） */
function openSummary(deepLink: string): void {
  let date = ''
  try {
    date = new URL(deepLink).searchParams.get('date')?.trim() ?? ''
  } catch {
    date = ''
  }
  window.dispatchEvent(new CustomEvent('ceams:open-ai-summary', { detail: date ? { date } : {} }))
}

/**
 * 初始化桌面端深链监听：仅 Electron 渲染层存在 `window.ceamsDesktop` 时生效，
 * Web 环境静默跳过。主进程已做单实例与窗口聚焦，这里只负责改路由。
 */
export function initDesktopDeepLink(router: Router): void {
  const bridge = window.ceamsDesktop
  if (!bridge?.onDeepLink) return

  bridge.onDeepLink((deepLink) => {
    if (deepLink.trim().startsWith(SUMMARY_DEEP_LINK)) {
      openSummary(deepLink.trim())
      return
    }
    const path = deepLinkToPath(deepLink)
    if (path) void router.push(path)
  })
}
