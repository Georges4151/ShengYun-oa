import { app, BrowserWindow, ipcMain, session } from 'electron'
import * as path from 'node:path'

/** 渲染层 CSP：渲染进程不接触 Node，仅允许自身资源与公网 HTTPS 接口 */
const CSP = [
  "default-src 'self'",
  "script-src 'self'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: https:",
  "connect-src 'self' https:",
  "font-src 'self' data:"
].join('; ')

const WEB_DEV_URL = 'http://localhost:5173'

function isDev(): boolean {
  return !app.isPackaged && process.env.NODE_ENV !== 'production'
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

  if (isDev()) {
    void win.loadURL(WEB_DEV_URL)
  } else {
    void win.loadFile(path.join(__dirname, '..', 'web', 'dist', 'index.html'))
  }

  return win
}

function registerIpc(): void {
  ipcMain.handle('app:get-version', (): string => app.getVersion())
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