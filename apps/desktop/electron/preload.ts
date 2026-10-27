import { contextBridge, ipcRenderer } from 'electron'

/**
 * 渲染层唯一可用的白名单桥能力。
 * 原则：最小暴露，只放经审核的 IPC 通道；新增能力须走评审。
 */
const desktopApi = {
  platform: 'desktop' as const,
  getAppVersion: (): Promise<string> => ipcRenderer.invoke('app:get-version') as Promise<string>
}

export type CeamsDesktopApi = typeof desktopApi

contextBridge.exposeInMainWorld('ceamsDesktop', desktopApi)