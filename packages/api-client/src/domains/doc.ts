/**
 * 文档知识库域（doc）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1/doc`，与后端 DocDirectoryController / DocFileController /
 * DocUploadController / DocVersionController / OnlyOfficeController 保持一致。
 *
 * 方法按资源分组（directories / files / upload / versions / permissions / locks），
 * 避免 `move`、`remove` 等同名方法冲突。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  PageResult,
  LibraryType,
  DirectoryNodeVO,
  DirectoryCreateRequest,
  DirectoryUpdateRequest,
  DirectoryMoveRequest,
  DocFileVO,
  DocFilePageQuery,
  FileRenameRequest,
  FileMoveRequest,
  UploadInitVO,
  UploadInitRequest,
  DocVersionVO,
  DocPermissionVO,
  PermissionGrantRequest,
  DocLockVO,
  EditorConfigVO
} from '@ceams/types'

/** 分片上传进度回调：loaded / total 均为字节数（fetch 无上传进度事件，按分片完成粒度回调）。 */
export type UploadProgressHandler = (loaded: number, total: number) => void

export interface DocDirectoryApi {
  /** 目录树（按可见性过滤；PROJECT 库必须传 ownerId = 项目 ID）。 */
  tree(params?: { libraryType?: LibraryType; ownerId?: Id }): Promise<DirectoryNodeVO[]>
  create(body: DirectoryCreateRequest): Promise<Id>
  update(id: Id, body: DirectoryUpdateRequest): Promise<void>
  move(id: Id, body: DirectoryMoveRequest): Promise<void>
  remove(id: Id): Promise<void>
}

export interface DocFileApi {
  list(query: DocFilePageQuery): Promise<PageResult<DocFileVO>>
  rename(id: Id, body: FileRenameRequest): Promise<void>
  move(id: Id, body: FileMoveRequest): Promise<void>
  remove(id: Id): Promise<void>
  /** 浏览器内联预览地址（需登录态 Authorization，调用方需自行携带令牌拉取）。 */
  previewUrl(id: Id): string
  /** 签发一次性下载 token（300s）。 */
  issueDownloadToken(id: Id): Promise<string>
  /** 一次性下载地址（匿名可用）：返回相对路径，调用方按需拼接 API 基地址。 */
  downloadUrl(id: Id, token: string): string
}

export interface DocUploadApi {
  initUpload(body: UploadInitRequest): Promise<UploadInitVO>
  /**
   * 上传单个分片（multipart，字段名 file）。
   * 切片由调用方按 init 返回的 chunkSize 负责。
   */
  uploadChunk(
    uploadId: string,
    index: number,
    blob: Blob,
    onProgress?: UploadProgressHandler
  ): Promise<number[]>
  completeUpload(uploadId: string): Promise<DocFileVO>
}

export interface DocVersionApi {
  list(id: Id): Promise<DocVersionVO[]>
  rollback(id: Id, versionNo: number): Promise<DocFileVO>
}

export interface DocPermissionApi {
  list(id: Id): Promise<DocPermissionVO[]>
  grant(id: Id, body: PermissionGrantRequest): Promise<Id>
  revoke(id: Id, permId: Id): Promise<void>
}

export interface DocLockApi {
  lock(id: Id): Promise<void>
  unlock(id: Id): Promise<void>
  renew(id: Id): Promise<void>
  status(id: Id): Promise<DocLockVO>
}

export interface DocApi {
  directories: DocDirectoryApi
  files: DocFileApi
  upload: DocUploadApi
  versions: DocVersionApi
  permissions: DocPermissionApi
  locks: DocLockApi
  /** OnlyOffice 编辑器配置；不可用时后端返回 503，由调用方 catch 后降级为下载。 */
  editorConfig(id: Id): Promise<EditorConfigVO>
}

const PREFIX = '/api/v1/doc'

export function createDocApi(client: HttpClient): DocApi {
  return {
    // ---------- 目录 ----------
    directories: {
      async tree(params = {}) {
        return client.get<DirectoryNodeVO[]>(`${PREFIX}/directories/tree`, {
          query: { libraryType: params.libraryType, ownerId: params.ownerId }
        })
      },
      async create(body) {
        return client.post<Id>(`${PREFIX}/directories`, body)
      },
      async update(id, body) {
        return client.put<void>(`${PREFIX}/directories/${id}`, body)
      },
      async move(id, body) {
        return client.put<void>(`${PREFIX}/directories/${id}/move`, body)
      },
      async remove(id) {
        return client.delete<void>(`${PREFIX}/directories/${id}`)
      }
    },

    // ---------- 文件 ----------
    files: {
      async list(query) {
        return client.get<PageResult<DocFileVO>>(`${PREFIX}/files`, {
          query: {
            dirId: query.dirId,
            keyword: query.keyword,
            page: query.page ?? 1,
            size: query.size ?? 20
          }
        })
      },
      async rename(id, body) {
        return client.put<void>(`${PREFIX}/files/${id}/rename`, body)
      },
      async move(id, body) {
        return client.put<void>(`${PREFIX}/files/${id}/move`, body)
      },
      async remove(id) {
        return client.delete<void>(`${PREFIX}/files/${id}`)
      },
      previewUrl(id) {
        return `${PREFIX}/files/${id}/preview`
      },
      async issueDownloadToken(id) {
        return client.post<string>(`${PREFIX}/files/${id}/download-token`)
      },
      downloadUrl(id, token) {
        return `${PREFIX}/files/${id}/download?token=${encodeURIComponent(token)}`
      }
    },

    // ---------- 分片上传 ----------
    upload: {
      async initUpload(body) {
        return client.post<UploadInitVO>(`${PREFIX}/files/upload/init`, body)
      },
      async uploadChunk(uploadId, index, blob, onProgress) {
        const form = new FormData()
        // 文件名仅用于 multipart 元信息，服务端按 uploadId 的 meta 落盘
        form.append('file', blob, `chunk-${index}`)
        const uploaded = await client.put<number[]>(`${PREFIX}/files/upload/chunk`, form, {
          query: { uploadId, index }
        })
        // fetch 无上传进度事件，按「分片上传完成」粒度回调，页面整体进度以已完成分片数计算
        onProgress?.(blob.size, blob.size)
        return uploaded
      },
      async completeUpload(uploadId) {
        return client.post<DocFileVO>(`${PREFIX}/files/upload/complete`, undefined, {
          query: { uploadId }
        })
      }
    },

    // ---------- 版本 ----------
    versions: {
      async list(id) {
        return client.get<DocVersionVO[]>(`${PREFIX}/files/${id}/versions`)
      },
      async rollback(id, versionNo) {
        return client.post<DocFileVO>(`${PREFIX}/files/${id}/versions/${versionNo}/rollback`)
      }
    },

    // ---------- 授权 ----------
    permissions: {
      async list(id) {
        return client.get<DocPermissionVO[]>(`${PREFIX}/files/${id}/permissions`)
      },
      async grant(id, body) {
        return client.post<Id>(`${PREFIX}/files/${id}/permissions`, body)
      },
      async revoke(id, permId) {
        return client.delete<void>(`${PREFIX}/files/${id}/permissions/${permId}`)
      }
    },

    // ---------- 编辑锁 ----------
    locks: {
      async lock(id) {
        return client.post<void>(`${PREFIX}/files/${id}/lock`)
      },
      async unlock(id) {
        return client.post<void>(`${PREFIX}/files/${id}/unlock`)
      },
      async renew(id) {
        return client.post<void>(`${PREFIX}/files/${id}/lock/renew`)
      },
      async status(id) {
        return client.get<DocLockVO>(`${PREFIX}/files/${id}/lock`)
      }
    },

    // ---------- OnlyOffice ----------
    async editorConfig(id) {
      return client.get<EditorConfigVO>(`${PREFIX}/files/${id}/editor-config`)
    }
  }
}
