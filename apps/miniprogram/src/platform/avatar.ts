/**
 * 头像选择与上传的统一入口。
 * 页面只负责 toast 与本地状态更新，选择图片、取消判定、上传校验等细节收敛在此处。
 */

import { getAccessToken, getUser } from './auth'
import { AVATAR_UPLOAD_URL } from './config'
import { chooseImage, uploadAvatar } from './upload'
import { apiErrorMessage } from '../utils/error'

/**
 * 选择并上传头像：成功返回新的头像地址；用户取消返回 null；未登录或上传失败抛错。
 */
export async function chooseAndUploadAvatar(): Promise<string | null> {
  // 未登录时无法携带 Bearer 令牌，直接给出明确错误，交由页面 toast 提示
  const user = getUser()
  const token = getAccessToken()
  if (!user || !token) {
    throw new Error('请先登录')
  }

  let filePath = ''
  try {
    const paths = await chooseImage(1)
    filePath = paths[0] ?? ''
  } catch (err) {
    // 用户主动取消选择（平台 errMsg 含 cancel）视为正常结束，不抛出
    const message = apiErrorMessage(err, '')
    if (/cancel/i.test(message)) return null
    throw err
  }

  // 选择结果为空（极端情况）同样按取消处理
  if (!filePath) return null

  const uploaded = await uploadAvatar(AVATAR_UPLOAD_URL, filePath, token)
  return uploaded
}
