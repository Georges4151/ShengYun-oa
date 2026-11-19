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

/** 选择本地文件（PDF 等）；微信环境走会话文件选择，其余环境走系统文件选择。 */
export function chooseFile(): Promise<string> {
  return new Promise((resolve, reject) => {
    const chooser = (uni as unknown as {
      chooseFile?: (opts: Record<string, unknown>) => void
      chooseMessageFile?: (opts: Record<string, unknown>) => void
    }).chooseFile
      ? (uni as unknown as { chooseFile: (opts: Record<string, unknown>) => void }).chooseFile
      : (uni as unknown as { chooseMessageFile: (opts: Record<string, unknown>) => void }).chooseMessageFile
    if (!chooser) {
      reject(new Error('当前环境不支持选择文件'))
      return
    }
    chooser({
      count: 1,
      type: 'all',
      success: (res: { tempFilePaths?: string[]; tempFiles?: { path: string }[] }) => {
        const path = res.tempFilePaths?.[0] ?? res.tempFiles?.[0]?.path ?? ''
        resolve(path)
      },
      fail: (err: UniApp.GeneralCallbackResult) => {
        reject(new Error(err.errMsg ?? '选择文件失败'))
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

/** 头像上传接口的响应结构（成功时 code 为 0，data.avatar 为头像访问路径）。 */
interface AvatarUploadResponse {
  code?: number
  message?: string
  data?: { avatar?: string } | null
}

/**
 * 上传当前登录用户的头像。
 * 对应后端 POST /api/v1/profile/avatar：multipart 字段名 file，需 Bearer 令牌。
 * 成功时返回头像访问路径（如 /api/v1/profile/avatars/xxx.png）。
 */
export function uploadAvatar(url: string, filePath: string, token: string): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url,
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${token}` },
      success: (res: UniApp.UploadFileSuccessCallbackResult) => {
        let parsed: AvatarUploadResponse
        try {
          parsed = JSON.parse(res.data) as AvatarUploadResponse
        } catch {
          reject(new Error('头像上传响应解析失败'))
          return
        }
        const avatar = parsed?.data?.avatar
        if (!avatar) {
          // 后端失败响应形如 { code: <非0>, message: '中文提示' }
          reject(new Error(parsed?.message ?? '头像上传失败'))
          return
        }
        resolve(avatar)
      },
      fail: (err: UniApp.GeneralCallbackResult) => {
        // 网络异常等场景，优先使用平台返回的 errMsg
        reject(new Error(err.errMsg ?? '头像上传失败'))
      }
    })
  })
}
