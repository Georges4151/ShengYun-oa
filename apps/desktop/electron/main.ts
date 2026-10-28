import { app, BrowserWindow, ipcMain, session, shell } from 'electron'
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

/**
 * 渲染层 CSP：渲染进程不接触 Node，仅允许自身资源与公网 HTTPS 接口。
 * - connect-src 收紧为 'self' + https:，禁止 ws:/http: 明文与任意跨域。
 * - script-src 仅 'self'，杜绝内联脚本注入。
 */
const CSP = [
  "default-src 'self'",
  "script-src 'self'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: https:",
  "connect-src 'self' https:",
  "font-src 'self' data:"
].join('; ')

/** Web 开发服务器地址（apps/web 默认 5173） */
const WEB_DEV_URL = 'http://localhost:5173'

/**
 * 生产环境加载 apps/web 的构建产物。
 * __dirname = apps/desktop/dist-electron，向上两级到 apps，再进入 web/dist。
 */
const WEB_DIST_INDEX = path.join(__dirname, '..', '..', 'web', 'dist', 'index.html')

function isDev(): boolean {
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
    '生产模式请先执行 <code>pnpm --filter @ceams/web build</code> 生成 <code>apps/web/dist</code>。</p>',
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
    win.loadFile(WEB_DIST_INDEX).catch((error: Error) => {
      console.error('[ceams-desktop] 生产渲染层加载失败：', error)
      loadFallback(win, `未找到生产构建产物：${WEB_DIST_INDEX}`)
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
  session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
    const headers: Record<string, string[]> = {
      ...(details.responseHeaders ?? {}),
      'Content-Security-Policy': [CSP]
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
