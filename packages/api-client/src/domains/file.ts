/**
 * 文件上传域（file）类型化 API 工厂。
 * 上传接口为 multipart（字段名 file），返回统一包裹 data: { name, url, size, contentType }。
 * 业务记录只保存返回的 name，读取二进制需另行携带登录态请求 /api/v1/files/{name}。
 */

import type { HttpClient } from '../http-client'
import type { FileStorageInfo, UploadedFileVO } from '@ceams/types'

export interface FileApi {
  upload(file: File | Blob): Promise<UploadedFileVO>
  remove(name: string): Promise<void>
  /** 文件存储信息（仅 SYS_ADMIN 可访问，只读） */
  storageInfo(): Promise<FileStorageInfo>
}

export function createFileApi(client: HttpClient): FileApi {
  return {
    upload(file) {
      const form = new FormData()
      if (file instanceof File) {
        form.append('file', file, file.name)
      } else {
        form.append('file', file, 'upload')
      }
      return client.post<UploadedFileVO>('/api/v1/files/upload', form)
    },
    remove(name) {
      return client.delete<void>(`/api/v1/files/${encodeURIComponent(name)}`)
    },
    storageInfo() {
      return client.get<FileStorageInfo>('/api/v1/files/storage-info')
    }
  }
}
