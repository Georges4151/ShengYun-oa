/**
 * 桌面端 IPC 契约类型（主进程 / preload / 渲染层共享）。
 * 仅包含纯类型定义，无运行时依赖，可被 sandbox 化的 preload 安全地 type-only 引用。
 */

/** USB 存储设备信息（由 drivelist 枚举结果映射而来） */
export interface UsbDriveInfo {
  /** 设备标识（如 \\.\PhysicalDrive1） */
  device: string
  /** 设备路径（可能为空） */
  devicePath: string | null
  /** 设备描述 */
  description: string
  /** 是否可移动介质 */
  isRemovable: boolean
  /** 是否 USB 接口 */
  isUSB: boolean
  /** 是否系统盘 */
  isSystem: boolean
  /** 总容量（字节，未知时为 null） */
  size: number | null
  /** 挂载点列表 */
  mountpoints: UsbMountPoint[]
}

/** 存储设备挂载点 */
export interface UsbMountPoint {
  /** 文件系统路径（如 E:\ 或 /media/usb） */
  path: string
  /** 卷标（可能为空） */
  label: string | null
}

/** 读取印章图片入参 */
export interface ReadSealImageRequest {
  /** 目标 USB 盘挂载点路径 */
  driveMountpoint: string
  /** 印章图片相对路径（如 seals/gongzhang.png） */
  relativePath: string
}

/** 读取到的印章图片信息 */
export interface SealImageInfo {
  /** 文件名 */
  fileName: string
  /** MIME 类型 */
  mimeType: string
  /** 字节数 */
  size: number
  /** 内容 SHA-256（crypto 完整性校验值） */
  sha256: string
  /** 图片内容 base64（PNG/JPEG） */
  data: string
}

/** 盖章位置（PDF 页面坐标，原点为左下角，单位 pt） */
export interface StampPosition {
  x: number
  y: number
}

/** 本地盖章入参 */
export interface StampLocalRequest {
  /** 源 PDF 的 base64 */
  sourcePdfBase64: string
  /** 印章图片（PNG/JPEG）的 base64 */
  sealImageBase64: string
  /** 印章图片 MIME 类型（决定嵌入方式，默认 image/png） */
  sealImageMimeType?: string
  /** 印章图片期望 SHA-256（可选，用于盖章前完整性校验） */
  sealImageSha256?: string
  /** 盖章位置 */
  position: StampPosition
  /** 目标页索引（从 0 开始，默认 0） */
  pageIndex?: number
  /** 印章缩放比例（默认 1） */
  scale?: number
  /** 可选：将结果落盘到本地文件 */
  outputPath?: string
}

/** 本地盖章结果 */
export interface StampLocalResult {
  /** 盖章后 PDF 的 base64 */
  outputBase64: string
  /** 落盘路径（若指定 outputPath 则返回，否则为 undefined） */
  outputPath?: string
  /** 文档总页数 */
  pageCount: number
  /** 实际盖章页索引 */
  stampedPageIndex: number
}
