import type { HttpRequest, HttpResponse, RequestAdapter } from '@ceams/api-client'

/**
 * 小程序平台适配层：把 uni.request（wx.request）包装成 api-client 的 RequestAdapter。
 * 业务代码不得直接调用 uni.request / wx.request。
 */
export const uniRequestAdapter: RequestAdapter = {
  send(request: HttpRequest): Promise<HttpResponse> {
    return new Promise((resolve, reject) => {
      let data: unknown
      if (request.body) {
        try {
          data = JSON.parse(request.body)
        } catch {
          data = request.body
        }
      }

      uni.request({
        url: request.url,
        method: request.method as UniApp.RequestOptions['method'],
        data: data as UniApp.RequestOptions['data'],
        header: request.headers,
        success: (res: UniApp.RequestSuccessCallbackResult) => {
          resolve({
            status: res.statusCode,
            statusText: '',
            headers: (res.header ?? {}) as Record<string, string>,
            body: typeof res.data === 'string' ? res.data : JSON.stringify(res.data)
          })
        },
        fail: (err: UniApp.GeneralCallbackResult) => {
          reject(new Error(err.errMsg ?? '网络请求失败'))
        }
      })
    })
  }
}

/** 上传附件（票据拍照等）；占位实现 */
export function uploadFile(url: string, filePath: string): Promise<unknown> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url,
      filePath,
      name: 'file',
      success: (res) => resolve(res.data),
      fail: (err) => reject(new Error(err.errMsg ?? '上传失败'))
    })
  })
}

/** PDF 预览：服务端生成 -> 下载 -> openDocument */
export function previewPdf(url: string): void {
  uni.downloadFile({
    url,
    success: (res) => {
      if (res.statusCode === 200) {
        uni.openDocument({ filePath: res.tempFilePath, showMenu: true })
      }
    }
  })
}