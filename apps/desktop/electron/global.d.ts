import type { CeamsDesktopApi } from './preload'

declare global {
  interface Window {
    /** Electron preload 暴露的白名单 API（渲染层访问入口） */
    ceamsDesktop?: CeamsDesktopApi
  }
}

export {}