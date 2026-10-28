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
        if (typeof request.body === 'string') {
          try {
            data = JSON.parse(request.body)
          } catch {
            data = request.body
          }
        } else {
          // uni.request 不支持 multipart FormData 请求体，请改用 platform/upload 的 uploadFile
          reject(new Error('uni.request 不支持 FormData 请求体，请使用 uploadFile'))
          return
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
