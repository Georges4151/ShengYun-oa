/**
 * CEAMS 跨端统一基础类型。
 *
 * 跨端格式统一约束（与 ARCHITECTURE.md 第 5 节一致）：
 * - ID 一律字符串（后端雪花 ID 在 JSON 层序列化为字符串）
 * - 金额一律字符串（decimal(18,2) 序列化，避免浮点精度）
 * - 时间一律 ISO8601 字符串
 */

/** 全局唯一标识：DB bigint / JSON 字符串 */
export type Id = string

/** 金额：后端 BigDecimal -> JSON 字符串，如 "1280.50" */
export type Money = string

/** 时间：ISO8601 字符串，如 "2026-10-28T10:30:00" */
export type DateTime = string

/** 文件上传返回：name 为服务端保存的文件标识（业务记录中存该值），url 为访问地址 */
export interface UploadedFileVO {
  name: string
  url: string
  size: number
  contentType?: string
}

/** 文件存储子目录统计：name 为目录分类名（业务附件 / avatars / doc / tmp 等） */
export interface StorageDirectoryStat {
  name: string
  /** 该目录下文件数量 */
  fileCount: number
  /** 该目录占用字节数 */
  sizeBytes: number
}

/** 文件存储信息（只读展示）：FILE_UPLOAD_DIR 配置值与磁盘占用快照 */
export interface FileStorageInfo {
  /** 配置中的原始路径，如 "./data/files" */
  configuredPath: string
  /** 解析后的绝对路径 */
  absolutePath: string
  /** 目录是否存在 */
  exists: boolean
  /** 目录是否可写 */
  writable: boolean
  /** 文件总数 */
  fileCount: number
  /** 文件占用总字节数 */
  totalSizeBytes: number
  /** 磁盘可用字节数；无法探测时为 null */
  diskUsableBytes: number | null
  /** 磁盘总容量字节数；无法探测时为 null */
  diskTotalBytes: number | null
  /** 子目录统计（后端已按 sizeBytes 倒序） */
  directories: StorageDirectoryStat[]
}