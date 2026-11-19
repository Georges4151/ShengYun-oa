/**
 * 图片预览平台封装。
 * 页面内禁止直接调用 uni.previewImage / uni.downloadFile。
 *
 * 注意：后端文件读取接口 GET /api/v1/files/{name} 需要 Authorization 头，
 * 因此远程图片既不能直接作为 <image src>，也不能直接塞给 previewImage，
 * 必须先携带令牌下载到本地临时文件后再展示 / 预览。
 */

import { getAccessToken } from './auth'
import { API_BASE_URL } from './config'
import { downloadFile } from './download'

/** 拼接文件读取地址：文件名可能包含特殊字符，需 URI 编码 */
function fileReadUrl(name: string): string {
  return `${API_BASE_URL}/api/v1/files/${encodeURIComponent(name)}`
}

/** 调用 uni.previewImage 并转为 Promise，便于页面统一 catch 后 toast */
function openPreview(urls: string[], current: string): Promise<void> {
  return new Promise((resolve, reject) => {
    uni.previewImage({
      urls,
      current,
      success: () => resolve(),
      fail: (err: UniApp.GeneralCallbackResult) => {
        reject(new Error(err.errMsg ?? '预览图片失败'))
      }
    })
  })
}

/** 预览本地图片（选图后的临时路径等）；current 为当前点击项，缺省取第一张 */
export function previewLocalImages(urls: string[], current: string): Promise<void> {
  if (urls.length === 0) return Promise.resolve()
  return openPreview(urls, current || urls[0])
}

/**
 * 携带登录令牌把远程图片下载到本地临时文件，返回可被 <image> 直接加载的路径。
 * 失败统一 throw，由调用方 toast 提示。
 */
export async function resolveRemoteImage(name: string): Promise<string> {
  if (!name) throw new Error('图片不存在')
  const token = getAccessToken()
  if (!token) throw new Error('请先登录')
  return downloadFile(fileReadUrl(name), {
    header: { Authorization: `Bearer ${token}` }
  })
}

/**
 * 预览单张远程图片：先鉴权下载，再用本地临时文件调起预览。
 * 失败统一 throw，由调用方 toast 提示。
 */
export async function previewRemoteImage(name: string): Promise<void> {
  const filePath = await resolveRemoteImage(name)
  await openPreview([filePath], filePath)
}
