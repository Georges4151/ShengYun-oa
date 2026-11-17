/**
 * 文档知识库域（doc）类型：目录 / 文件 / 分片上传 / 版本 / 授权 / 编辑锁 / OnlyOffice。
 *
 * 字段与后端 `com.ceams.module.doc.vo` / `dto` 严格一致：
 * - 主键 Id、文件大小 size 均为后端 Long，JSON 层统一序列化为字符串
 * - 时间 ISO8601（yyyy-MM-ddTHH:mm:ss），允许为空
 * - 枚举为后端 Java 枚举，JSON 层为字符串
 */

import type { DateTime, Id } from './common'

// ---------- 枚举 ----------

/** 文档库类型：个人 / 部门 / 项目 / 公开。 */
export type LibraryType = 'PERSONAL' | 'DEPT' | 'PROJECT' | 'PUBLIC'

/** 文件状态：正常 / 编辑中（真实锁以 Redis 为准）。 */
export type DocFileStatus = 'NORMAL' | 'LOCKED'

/** 授权主体类型：用户 / 角色 / 部门。 */
export type DocSubjectType = 'USER' | 'ROLE' | 'DEPT'

/** 授权动作：预览 / 下载 / 编辑 / 删除。 */
export type DocAction = 'PREVIEW' | 'DOWNLOAD' | 'EDIT' | 'DELETE'

/** 版本来源：新上传 / 在线编辑 / 版本回滚。 */
export type DocVersionSource = 'UPLOAD' | 'ONLINE_EDIT' | 'ROLLBACK'

// ---------- 目录 ----------

/** 目录树节点（children 递归）。 */
export interface DirectoryNodeVO {
  id: Id
  name: string
  /** 父目录 ID（null 为库根）。 */
  parentId: Id | null
  libraryType: LibraryType
  /** 目录路径（/id/id/）。 */
  path: string
  sortOrder: number | null
  children: DirectoryNodeVO[]
}

export interface DirectoryCreateRequest {
  name: string
  /** 父目录 ID，为空表示库根。 */
  parentId?: Id | null
  libraryType: LibraryType
  /** 所有者用户 ID（PERSONAL 由服务端取当前用户）。 */
  ownerUserId?: Id | null
  /** 所有者部门 ID（DEPT 为空时取当前用户部门）。 */
  ownerDeptId?: Id | null
  /** 项目 ID（PROJECT 必填）。 */
  projectId?: Id | null
  sortOrder?: number | null
}

export interface DirectoryUpdateRequest {
  /** 目录名（为空表示不修改）。 */
  name?: string
  sortOrder?: number | null
}

export interface DirectoryMoveRequest {
  /** 目标父目录 ID，为空表示移动到库根。 */
  parentId?: Id | null
}

// ---------- 文件 ----------

export interface DocFileVO {
  id: Id
  dirId: Id
  /** 文件名（含扩展名）。 */
  name: string
  /** 小写扩展名。 */
  ext: string | null
  /** 文件字节数（后端 Long 序列化为字符串）。 */
  size: string | null
  currentVersionNo: number | null
  status: DocFileStatus
  uploadedBy: Id | null
  createTime: DateTime | null
  updateTime: DateTime | null
  downloadCount: number | null
}

/** 文件分页查询入参。 */
export interface DocFilePageQuery {
  dirId: Id
  keyword?: string
  page?: number
  size?: number
}

export interface FileRenameRequest {
  /** 新文件名（含扩展名）。 */
  name: string
}

export interface FileMoveRequest {
  /** 目标目录 ID。 */
  dirId: Id
}

// ---------- 分片上传 ----------

/** 分片上传初始化结果。 */
export interface UploadInitVO {
  /** 上传批次 ID（秒传命中时为空）。 */
  uploadId: string | null
  /** 分片大小（字节）。 */
  chunkSize: number
  /** 已上传分片序号（断点续传用，升序）。 */
  uploaded: number[]
  /** 是否秒传命中（true 时 fileId 有效、无需再传分片）。 */
  instant: boolean
  /** 秒传命中时的新文件 ID。 */
  fileId: Id | null
  /** 规范化后的文件名。 */
  fileName: string
}

export interface UploadInitRequest {
  dirId: Id
  /** 文件名（含扩展名，服务端只保留基础名）。 */
  fileName: string
  /** 文件字节数。 */
  size: number
  /** 整文件 SHA-256（64 位 hex，可空；为空则不走秒传、完成时也不校验校验和）。 */
  checksum?: string
}

// ---------- 版本 ----------

export interface DocVersionVO {
  versionNo: number
  /** 文件字节数（后端 Long 序列化为字符串）。 */
  size: string | null
  /** SHA-256 校验和。 */
  checksum: string | null
  uploadedBy: Id | null
  changeNote: string | null
  source: DocVersionSource
  createTime: DateTime | null
}

// ---------- 授权 ----------

/**
 * 文档授权条目（后端 `PermissionVO`）。
 *
 * 注意：IAM 模块已存在同名 `PermissionVO`（权限树节点），为避免 `export *` 重名，
 * 统一导出时以 `DocPermissionVO` 别名对外提供。
 */
export interface PermissionVO {
  id: Id
  subjectType: DocSubjectType
  subjectId: Id
  /** 动作集合：PREVIEW / DOWNLOAD / EDIT / DELETE。 */
  actions: DocAction[]
  grantedBy: Id | null
}

/** 文档授权条目别名（避免与 IAM 的 PermissionVO 重名）。 */
export type DocPermissionVO = PermissionVO

export interface PermissionGrantRequest {
  subjectType: DocSubjectType
  subjectId: Id
  actions: DocAction[]
}

// ---------- 编辑锁 ----------

export interface DocLockVO {
  /** 是否处于编辑锁（Redis 为准，已过期视为未锁）。 */
  locked: boolean
  ownerId: Id | null
  ownerName: string | null
}

// ---------- OnlyOffice ----------

/** OnlyOffice 编辑器配置响应。 */
export interface EditorConfigVO {
  /** Document Server 基地址（容器内地址，服务端探活用）。 */
  documentServerUrl: string
  /** Document Server 的浏览器可达地址（前端据此加载 api.js）。 */
  browserDocumentServerUrl: string
  /** 编辑器 config（document / editorConfig / type），结构由 OnlyOffice 定义，故用 any。 */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  config: Record<string, any>
  /** 整份 config 的 JWT 签名（OnlyOffice 8.1 要求 body.token）。 */
  token: string
  /** 编辑模式：edit / view。 */
  mode: string
  fileId: Id
}
