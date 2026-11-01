import { app, BrowserWindow, ipcMain, session, shell } from 'electron'
import * as fs from 'node:fs'
import * as path from 'node:path'
import { deleteRefreshToken, getRefreshToken, setRefreshToken } from './auth-token'
import { listUsbDrives, readSealImage, stampLocal } from './usb-seal'
import type {
  ReadSealImageRequest,
  SealImageInfo,
  StampLocalRequest,
  StampLocalResult,
  UsbDriveInfo
} from './ipc-types'

/** Web 开发服务器地址（apps/web 默认 5173） */
const WEB_DEV_URL = 'http://localhost:5173'

/** dev 模式下 Vite HMR 的 WebSocket 地址（由 WEB_DEV_URL 推导为 ws://） */
const WEB_DEV_WS_URL = WEB_DEV_URL.replace(/^http/, 'ws')

/**
 * 渲染层 CSP：渲染进程不接触 Node，仅允许自身资源与公网 HTTPS 接口。
 * - connect-src 生产环境收紧为 'self' + https:，禁止 ws:/http: 明文与任意跨域。
 * - 仅 dev 模式追加 Vite HMR 的 ws 地址，保证热更新可用；生产环境不放宽。
 * - script-src 仅 'self'，杜绝内联脚本注入。
 */
function buildCsp(dev: boolean): string {
  const connectSrc = dev
    ? `connect-src 'self' https: ${WEB_DEV_WS_URL}`
    : "connect-src 'self' https:"
  return [
    "default-src 'self'",
    "script-src 'self'",
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data: https:",
    connectSrc,
    "font-src 'self' data:"
  ].join('; ')
}

/**
 * 生产环境渲染层入口。
 * - 打包后：electron-builder 通过 files.from 将 apps/web/dist-electron 复制到
 *   app 内的 dist-electron/renderer，故优先加载 __dirname/renderer/index.html。
 * - 未打包 monorepo 场景（CEAMS_ENV=production 跑源码产物）：向上两级到 apps/web/dist-electron。
 * 两者产物均由 `vite build --mode electron`（base='./'）产出，资源相对路径适配 file:// 协议。
 */
const BUNDLED_WEB_INDEX = path.join(__dirname, 'renderer', 'index.html')
const MONOREPO_WEB_INDEX = path.join(__dirname, '..', '..', 'web', 'dist-electron', 'index.html')

function resolveProdIndex(): string {
  return fs.existsSync(BUNDLED_WEB_INDEX) ? BUNDLED_WEB_INDEX : MONOREPO_WEB_INDEX
}

function isDev(): boolean {
  // 显式环境变量优先（打包环境与 .pnpm junction 路径下 app.isPackaged 不可靠）
  if (process.env.CEAMS_ENV === 'production') return false
  if (process.env.CEAMS_ENV === 'development') return true
  return !app.isPackaged && process.env.NODE_ENV !== 'production'
}

/** dev server 未启动 / 生产产物缺失时的兜底提示页（data URL，避免额外文件） */
function loadFallback(win: BrowserWindow, message: string): void {
  const html = [
    '<!doctype html><html lang="zh-CN"><meta charset="utf-8">',
    '<title>CEAMS 桌面端</title>',
    '<body style="font-family:system-ui;padding:40px;max-width:640px;line-height:1.6">',
    '<h2>渲染层加载失败</h2>',
    `<p>${message}</p>`,
    '<p>开发模式请先在 <code>apps/web</code> 执行 <code>pnpm dev</code>（默认端口 5173）；',
    '生产模式请先执行 <code>pnpm --filter @ceams/web build:electron</code> 生成 <code>apps/web/dist-electron</code>。</p>',
    '</body></html>'
  ].join('')
  void win.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`)
}

function createWindow(): BrowserWindow {
  const win = new BrowserWindow({
    width: 1280,
    height: 800,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      sandbox: true,
      nodeIntegration: false,
      webviewTag: false
    }
  })

  win.once('ready-to-show', () => win.show())

  // 安全边界：拦截新窗口，外部链接一律交给系统浏览器
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('https://') || url.startsWith('http://')) {
      void shell.openExternal(url)
    }
    return { action: 'deny' }
  })

  // 安全边界：禁止渲染层导航到本地文件 / 任意地址
  win.webContents.on('will-navigate', (event, url) => {
    const devOriginOk = isDev() && url.startsWith(WEB_DEV_URL)
    const prodFileOk = !isDev() && url.startsWith('file://')
    if (devOriginOk || prodFileOk) return
    event.preventDefault()
    if (url.startsWith('https://') || url.startsWith('http://')) {
      void shell.openExternal(url)
    }
  })

  if (isDev()) {
    win.loadURL(WEB_DEV_URL).catch(() => {
      loadFallback(win, '未检测到 Web 开发服务器（localhost:5173）。')
    })
  } else {
    const prodIndex = resolveProdIndex()
    win.loadFile(prodIndex).catch((error: Error) => {
      console.error('[ceams-desktop] 生产渲染层加载失败：', error)
      loadFallback(win, `未找到生产构建产物：${prodIndex}`)
    })
  }

  return win
}

function registerIpc(): void {
  // —— 应用信息 ——
  ipcMain.handle('app:get-version', (): string => app.getVersion())

  // —— refresh token（safeStorage 系统级加密落盘，渲染层不接触密钥） ——
  ipcMain.handle('auth:get-refresh-token', (): string | null => getRefreshToken())
  ipcMain.handle('auth:set-refresh-token', (_event, token: string): void => setRefreshToken(token))
  ipcMain.handle('auth:delete-refresh-token', (): void => deleteRefreshToken())

  // —— USB 电子盖章 ——
  ipcMain.handle('seal:list-usb-drives', (): Promise<UsbDriveInfo[]> => listUsbDrives())
  ipcMain.handle(
    'seal:read-seal-image',
    (_event, request: ReadSealImageRequest): SealImageInfo => readSealImage(request)
  )
  ipcMain.handle(
    'seal:stamp-local',
    (_event, request: StampLocalRequest): Promise<StampLocalResult> => stampLocal(request)
  )
}

function registerCsp(): void {
  const csp = buildCsp(isDev())
  session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
    const headers: Record<string, string[]> = {
      ...(details.responseHeaders ?? {}),
      'Content-Security-Policy': [csp]
    }
    callback({ responseHeaders: headers })
  })
}

app.whenReady().then(() => {
  registerCsp()
  registerIpc()
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
