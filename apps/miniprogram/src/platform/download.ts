/**
 * 文件下载与文档预览平台封装。
 * 页面内禁止直接调用 uni.downloadFile / uni.openDocument。
 */

import { getAccessToken } from './auth'
import { API_BASE_URL } from './config'

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

/**
 * 打开本地文档。
 * showMenu 为 true 时显示右上角菜单（微信基础库 2.11.0+），便于用户转发/保存附件。
 */
export function openDocument(filePath: string, fileType?: string, showMenu = false): Promise<void> {
  return new Promise((resolve, reject) => {
    uni.openDocument({
      filePath,
      fileType,
      showMenu,
      success: () => resolve(),
      fail: (err: UniApp.GeneralCallbackResult) => {
        reject(new Error(err.errMsg ?? '打开文档失败'))
      }
    })
  })
}

/**
 * 流程实例受控附件下载地址。
 * 对应后端 GET /api/v1/bpm/instances/{id}/files/{name}；
 * name 为服务端存储名，可能包含中文或空格，必须逐段 URI 编码。
 */
export function instanceFileUrl(instanceId: string, name: string): string {
  return `${API_BASE_URL}/api/v1/bpm/instances/${encodeURIComponent(instanceId)}/files/${encodeURIComponent(name)}`
}

/**
 * 下载流程实例附件到本地临时文件，返回 tempFilePath。
 * 受控接口需要登录态：令牌放在 Authorization 请求头（不得拼到 query），
 * 因此不能把附件地址直接交给 <image> 或 uni.previewImage。
 */
export async function downloadInstanceFile(instanceId: string, name: string): Promise<string> {
  if (!instanceId || !name) {
    throw new Error('附件信息不完整')
  }
  const token = getAccessToken()
  if (!token) {
    throw new Error('请先登录')
  }
  return downloadFile(instanceFileUrl(instanceId, name), {
    header: { Authorization: `Bearer ${token}` }
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
