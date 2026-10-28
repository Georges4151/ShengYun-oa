import { app, safeStorage } from 'electron'
import * as fs from 'node:fs'
import * as path from 'node:path'

/**
 * refresh token 的系统级安全存储。
 *
 * 选型：使用 Electron 内置 safeStorage（替代原生模块 keytar）。
 * - Windows 底层走 DPAPI，macOS 底层走 Keychain，均为系统级加密，语义等价 keytar；
 * - keytar 是原生模块，依赖 node-gyp 编译 / prebuild 下载，跨平台与 CI 易失败；
 * - safeStorage 随 Electron 内置、零额外依赖，是官方推荐的敏感凭证落盘方案；
 * - 密文落盘路径：app.getPath('userData')/ceams-refresh-token.bin。
 */

const TOKEN_FILE_NAME = 'ceams-refresh-token.bin'

function tokenFilePath(): string {
  return path.join(app.getPath('userData'), TOKEN_FILE_NAME)
}

/** 读取 refresh token；未存储或系统钥匙串不可用时返回 null */
export function getRefreshToken(): string | null {
  if (!safeStorage.isEncryptionAvailable()) return null
  const filePath = tokenFilePath()
  if (!fs.existsSync(filePath)) return null
  try {
    const encrypted = fs.readFileSync(filePath)
    return safeStorage.decryptString(encrypted)
  } catch {
    // 密文损坏或系统密钥变更时按未存储处理
    return null
  }
}

/** 写入 refresh token；系统钥匙串不可用时抛错（拒绝明文落盘） */
export function setRefreshToken(token: string): void {
  if (!safeStorage.isEncryptionAvailable()) {
    throw new Error('系统钥匙串不可用，无法安全存储 refresh token')
  }
  const encrypted = safeStorage.encryptString(token)
  fs.mkdirSync(app.getPath('userData'), { recursive: true })
  fs.writeFileSync(tokenFilePath(), encrypted)
}

/** 删除 refresh token */
export function deleteRefreshToken(): void {
  const filePath = tokenFilePath()
  if (fs.existsSync(filePath)) fs.unlinkSync(filePath)
}
