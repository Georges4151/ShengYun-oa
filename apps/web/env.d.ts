/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_APP_TITLE?: string
  readonly VITE_ROUTER_MODE?: string
}

interface Window {
  /** Electron preload 暴露的桥能力（仅桌面端存在，Web 环境为 undefined） */
  ceamsDesktop?: {
    /** 订阅主进程转发的 ceams:// 深链，返回取消订阅函数 */
    onDeepLink?: (handler: (deepLink: string) => void) => () => void
  }
}