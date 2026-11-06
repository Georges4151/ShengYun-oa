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

/** 图片类型：微信 openDocument 不支持，需走 previewImage */
const IMAGE_TYPES = ['jpg', 'jpeg', 'png', 'webp']

/** openDocument 可打开的文档类型，不在列表内时按 pdf 处理 */
const DOCUMENT_TYPES = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']

/** 从 url 中提取小写扩展名（忽略查询串与哈希） */
function getFileExtension(url: string): string {
  const path = url.split(/[?#]/)[0]
  const index = path.lastIndexOf('.')
  return index === -1 ? '' : path.slice(index + 1).toLowerCase()
}

/**
 * 通用附件预览：按扩展名分流。
 * 图片走 downloadFile + previewImage，其余文档走 previewPdf / openDocument。
 * 需要登录态时自动携带 Authorization 头。
 */
export async function previewFile(url: string): Promise<void> {
  const ext = getFileExtension(url)
  if (IMAGE_TYPES.includes(ext)) {
    const token = getAccessToken()
    const header = token ? { Authorization: `Bearer ${token}` } : undefined
    const filePath = await downloadFile(url, { header })
    await new Promise<void>((resolve, reject) => {
      uni.previewImage({
        urls: [filePath],
        current: filePath,
        success: () => resolve(),
        fail: (err: UniApp.GeneralCallbackResult) => {
          reject(new Error(err.errMsg ?? '预览图片失败'))
        }
      })
    })
    return
  }
  await previewPdf(url, DOCUMENT_TYPES.includes(ext) ? ext : 'pdf')
}
