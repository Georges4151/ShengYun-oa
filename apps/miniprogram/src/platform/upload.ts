/**
 * 图片选择 + 文件上传平台封装。
 * 页面内禁止直接调用 uni.chooseImage / uni.uploadFile。
 */

export interface UploadOptions {
  /** 文件对应字段名，默认 file */
  name?: string
  /** 随文件一起提交的表单数据 */
  formData?: Record<string, string>
  /** 请求头（如 Authorization） */
  header?: Record<string, string>
}

export function chooseImage(count = 1): Promise<string[]> {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count,
      sourceType: ['album', 'camera'],
      success: (res: UniApp.ChooseImageSuccessCallbackResult) => {
        const paths = typeof res.tempFilePaths === 'string' ? [res.tempFilePaths] : res.tempFilePaths
        resolve(paths)
      },
      fail: (err: UniApp.GeneralCallbackResult) => {
        reject(new Error(err.errMsg ?? '选择图片失败'))
      }
    })
  })
}

export function uploadFile(url: string, filePath: string, options: UploadOptions = {}): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url,
      filePath,
      name: options.name ?? 'file',
      formData: options.formData,
      header: options.header,
      success: (res: UniApp.UploadFileSuccessCallbackResult) => {
        try {
          const parsed = JSON.parse(res.data) as { data?: { url?: string } }
          const url = parsed?.data?.url
          if (!url) {
            reject(new Error('上传响应缺少文件地址'))
            return
          }
          resolve(url)
        } catch {
          reject(new Error('上传响应解析失败'))
        }
      },
      fail: (err: UniApp.GeneralCallbackResult) => {
        reject(new Error(err.errMsg ?? '上传失败'))
      }
    })
  })
}
