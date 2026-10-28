import type { CeamsDesktopApi } from './preload'

declare global {
  interface Window {
    /**
     * Electron preload 暴露的白名单 API（渲染层访问入口）。
     * 类型由 preload.ts 的 `typeof desktopApi` 自动推导，与主进程 ipcMain.handle
     * 通道一一对应，涵盖：getAppVersion / getRefreshToken / setRefreshToken /
     * deleteRefreshToken / listUsbDrives / readSealImage / stampLocal。
     */
    ceamsDesktop?: CeamsDesktopApi
  }
}

export {}