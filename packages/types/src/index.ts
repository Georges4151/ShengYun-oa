export * from './common'
export * from './api'
export * from './codes'
export * from './iam'
export * from './masterdata'
export * from './project'
export * from './hr'
export * from './admin'
export * from './expense'
export * from './bpm'
export * from './payment'
export * from './bank'
export * from './seal'
export * from './notification'
export * from './report'
export * from './ai'
// 企业微信集成：扫码登录、组织架构同步批次、应用消息发件箱
export * from './integration'
// 文档知识库：doc 的 PermissionVO 与 IAM 的权限树 PermissionVO 重名，无法 `export *`，
// 故显式导出 doc 的类型，并以 DocPermissionVO 别名对外提供文档授权条目（PermissionVO 保持 IAM 语义）
export type {
  LibraryType,
  DocFileStatus,
  DocSubjectType,
  DocAction,
  DocVersionSource,
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
} from './doc'