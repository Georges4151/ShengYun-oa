import * as crypto from 'node:crypto'
import * as fs from 'node:fs'
import * as path from 'node:path'
import { list } from 'drivelist'
import type { Drive } from 'drivelist'
import { PDFDocument } from 'pdf-lib'
import type { PDFImage } from 'pdf-lib'
import type {
  ReadSealImageRequest,
  SealImageInfo,
  StampLocalRequest,
  StampLocalResult,
  UsbDriveInfo,
  UsbMountPoint
} from './ipc-types'

/**
 * USB 电子盖章（主进程）。
 *
 * 选型说明：
 * - 枚举 USB 盘用 drivelist（原生模块，prebuild-install 失败时回退 node-gyp 编译）；
 * - 读取印章用 Node 内置 fs 直接读盘（替代 node-usb）——
 *   node-usb 依赖 libusb 工具链与驱动，跨平台编译/安装成本高、失败率高，
 *   而电子印章场景只需「找到盘符 + 读取印章图片文件」，fs 读盘已足够；
 * - 印章完整性用 Node 内置 crypto 计算 SHA-256；
 * - PDF 本地叠加用 pdf-lib（纯 JS，无原生依赖）。
 */

/** 印章图片最大字节数（16MB） */
const SEAL_IMAGE_MAX_BYTES = 16 * 1024 * 1024
/** 源 PDF 最大字节数（64MB） */
const PDF_MAX_BYTES = 64 * 1024 * 1024

function isUsbDrive(drive: Drive): boolean {
  return drive.isUSB === true || drive.isRemovable === true
}

function mapMountpoints(mountpoints: Drive['mountpoints']): UsbMountPoint[] {
  return mountpoints.map((mountpoint) => ({
    path: mountpoint.path,
    label: mountpoint.label
  }))
}

/** 枚举已插入且可读的 USB 存储设备 */
export async function listUsbDrives(): Promise<UsbDriveInfo[]> {
  const drives = await list()
  return drives
    .filter(isUsbDrive)
    .filter((drive) => drive.mountpoints.length > 0)
    .map((drive) => ({
      device: drive.device,
      devicePath: drive.devicePath,
      description: drive.description,
      isRemovable: drive.isRemovable,
      isUSB: drive.isUSB === true,
      isSystem: drive.isSystem,
      size: drive.size,
      mountpoints: mapMountpoints(drive.mountpoints)
    }))
}

function mimeTypeForFileName(fileName: string): string {
  const ext = path.extname(fileName).toLowerCase()
  if (ext === '.png') return 'image/png'
  if (ext === '.jpg' || ext === '.jpeg') return 'image/jpeg'
  throw new Error(`不支持的印章图片格式：${ext || '(无扩展名)'}`)
}

function sha256Hex(data: Buffer): string {
  return crypto.createHash('sha256').update(data).digest('hex')
}

/**
 * 从 USB 盘读取印章图片，返回 base64 与 SHA-256 校验值。
 * 安全边界：仅允许读取「挂载点内部」的文件，拒绝绝对路径与 `..` 越界。
 */
export function readSealImage(request: ReadSealImageRequest): SealImageInfo {
  if (request.driveMountpoint.trim() === '') throw new Error('USB 挂载点不能为空')
  if (request.relativePath.trim() === '') throw new Error('印章图片相对路径不能为空')

  const driveRoot = path.resolve(request.driveMountpoint)
  const fullPath = path.resolve(driveRoot, request.relativePath)
  const relative = path.relative(driveRoot, fullPath)
  if (relative.startsWith('..') || path.isAbsolute(relative)) {
    throw new Error('印章图片路径越界：仅允许读取 USB 盘内文件')
  }

  const stat = fs.statSync(fullPath)
  if (!stat.isFile()) throw new Error('印章图片路径不是文件')
  if (stat.size > SEAL_IMAGE_MAX_BYTES) throw new Error('印章图片超过大小限制（16MB）')

  const data = fs.readFileSync(fullPath)
  return {
    fileName: path.basename(fullPath),
    mimeType: mimeTypeForFileName(fullPath),
    size: data.length,
    sha256: sha256Hex(data),
    data: data.toString('base64')
  }
}

function normalizeMimeType(mimeType: string | undefined): string {
  return (mimeType ?? 'image/png').toLowerCase()
}

async function embedSealImage(
  pdfDoc: PDFDocument,
  sealBytes: Uint8Array,
  mimeType: string
): Promise<PDFImage> {
  if (mimeType === 'image/png') return pdfDoc.embedPng(sealBytes)
  if (mimeType === 'image/jpeg' || mimeType === 'image/jpg') return pdfDoc.embedJpg(sealBytes)
  throw new Error(`不支持的印章图片 MIME 类型：${mimeType}`)
}

/**
 * 在本地对 PDF 叠加印章图片（渲染进程不接触 Node / pdf-lib）。
 * 结果可返回 base64，也可落盘到 outputPath。
 */
export async function stampLocal(request: StampLocalRequest): Promise<StampLocalResult> {
  const pdfBytes = Buffer.from(request.sourcePdfBase64, 'base64')
  if (pdfBytes.length === 0) throw new Error('源 PDF 为空')
  if (pdfBytes.length > PDF_MAX_BYTES) throw new Error('源 PDF 超过大小限制（64MB）')

  const sealBytes = Buffer.from(request.sealImageBase64, 'base64')
  if (sealBytes.length === 0) throw new Error('印章图片为空')

  if (request.sealImageSha256) {
    const actual = sha256Hex(sealBytes)
    if (actual !== request.sealImageSha256.toLowerCase()) {
      throw new Error('印章图片完整性校验失败（SHA-256 不匹配）')
    }
  }

  const pdfDoc = await PDFDocument.load(pdfBytes)
  const pageCount = pdfDoc.getPageCount()
  const pageIndex = request.pageIndex ?? 0
  if (pageIndex < 0 || pageIndex >= pageCount) {
    throw new Error(`目标页索引越界：${pageIndex}（共 ${pageCount} 页）`)
  }

  const mimeType = normalizeMimeType(request.sealImageMimeType)
  const image = await embedSealImage(pdfDoc, sealBytes, mimeType)

  const scale = request.scale ?? 1
  if (scale <= 0) throw new Error('印章缩放比例必须为正数')

  const page = pdfDoc.getPage(pageIndex)
  page.drawImage(image, {
    x: request.position.x,
    y: request.position.y,
    width: image.width * scale,
    height: image.height * scale
  })

  const outputBytes = await pdfDoc.save()
  const outputBase64 = Buffer.from(outputBytes).toString('base64')

  let outputPath: string | undefined
  if (request.outputPath) {
    const resolvedOutput = path.resolve(request.outputPath)
    fs.mkdirSync(path.dirname(resolvedOutput), { recursive: true })
    fs.writeFileSync(resolvedOutput, outputBytes)
    outputPath = resolvedOutput
  }

  return {
    outputBase64,
    outputPath,
    pageCount,
    stampedPageIndex: pageIndex
  }
}
