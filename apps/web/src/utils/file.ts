/**
 * 浏览器端文件保存与预览工具：配合 services/http 的 fetchBlob 使用。
 * 附件读取需要登录态（Authorization），故不能直接用 <a href> / <img src>，
 * 统一先取回 Blob，再在本地生成 object URL。
 */

/** 保存 Blob 为本地文件 */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/** 为 PDF/图片生成 objectURL 并在新窗口打开，延迟释放避免内存泄漏 */
export function previewBlob(blob: Blob): void {
  const url = URL.createObjectURL(blob)
  window.open(url, '_blank')
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
