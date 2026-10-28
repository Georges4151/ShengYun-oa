import { contextBridge, ipcRenderer } from 'electron'
import type {
  ReadSealImageRequest,
  SealImageInfo,
  StampLocalRequest,
  StampLocalResult,
  UsbDriveInfo
} from './ipc-types'

/**
 * 渲染层唯一可用的白名单桥能力。
 * 原则：最小暴露，只放经审核的 IPC 通道；新增能力须走评审。
 *
 * 通道与主进程 ipcMain.handle 一一对应：
 * - app:get-version           → getAppVersion
 * - auth:get-refresh-token    → getRefreshToken
 * - auth:set-refresh-token    → setRefreshToken
 * - auth:delete-refresh-token → deleteRefreshToken
 * - seal:list-usb-drives      → listUsbDrives
 * - seal:read-seal-image      → readSealImage
 * - seal:stamp-local          → stampLocal
 */
const desktopApi = {
  platform: 'desktop' as const,

  getAppVersion: (): Promise<string> => ipcRenderer.invoke('app:get-version') as Promise<string>,

  // refresh token（系统钥匙串加密存储）
  getRefreshToken: (): Promise<string | null> =>
    ipcRenderer.invoke('auth:get-refresh-token') as Promise<string | null>,
  setRefreshToken: (token: string): Promise<void> =>
    ipcRenderer.invoke('auth:set-refresh-token', token) as Promise<void>,
  deleteRefreshToken: (): Promise<void> =>
    ipcRenderer.invoke('auth:delete-refresh-token') as Promise<void>,

  // USB 电子盖章
  listUsbDrives: (): Promise<UsbDriveInfo[]> =>
    ipcRenderer.invoke('seal:list-usb-drives') as Promise<UsbDriveInfo[]>,
  readSealImage: (request: ReadSealImageRequest): Promise<SealImageInfo> =>
    ipcRenderer.invoke('seal:read-seal-image', request) as Promise<SealImageInfo>,
  stampLocal: (request: StampLocalRequest): Promise<StampLocalResult> =>
    ipcRenderer.invoke('seal:stamp-local', request) as Promise<StampLocalResult>
}

export type CeamsDesktopApi = typeof desktopApi

contextBridge.exposeInMainWorld('ceamsDesktop', desktopApi)
