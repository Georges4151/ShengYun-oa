/**
 * 文件下载与文档预览平台封装。
 * 页面内禁止直接调用 uni.downloadFile / uni.openDocument。
 */

import { getAccessToken } from './auth'

export interface DownloadOptions {
  header?: Record<string, string>
}

export function downloadFile(url: string, options: DownloadOptions = {}): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url,
      header: options.header,
      success: (res: UniApp.DownloadSuccessData) => {
        if (res.statusCode === 200) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error(`文件下载失败（HTTP ${res.statusCode}）`))
        }
      },
      fail: (err: UniApp.GeneralCallbackResult) => {
        reject(new Error(err.errMsg ?? '文件下载失败'))
      }
    })
  })
}

export function openDocument(filePath: string, fileType?: string): Promise<void> {
  return new Promise((resolve, reject) => {
    uni.openDocument({
      filePath,
      fileType,
      success: () => resolve(),
      fail: (err: UniApp.GeneralCallbackResult) => {
        reject(new Error(err.errMsg ?? '打开文档失败'))
      }
    })
  })
}

/**
 * PDF 预览：服务端生成 -> 下载到本地临时文件 -> openDocument 打开。
 * 需要登录态时自动携带 Authorization 头。
 */
export async function previewPdf(url: string, fileType = 'pdf'): Promise<void> {
  const token = getAccessToken()
  const header = token ? { Authorization: `Bearer ${token}` } : undefined
  const filePath = await downloadFile(url, { header })
  await openDocument(filePath, fileType)
}
