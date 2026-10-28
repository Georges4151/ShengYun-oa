/**
 * 小程序端环境与运行时配置。
 *
 * baseUrl 优先读取 Vite 环境变量 VITE_API_BASE_URL（构建时注入），
 * 未配置时回退到占位地址。部署到真实后端前，请：
 * 1. 在 apps/miniprogram/.env 中配置 VITE_API_BASE_URL=https://<真实后端>；或
 * 2. 直接修改下方默认占位地址。
 */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? 'https://api.example.com'

/**
 * 票据附件上传端点（占位）。
 * 当前后端契约未提供通用文件上传接口，此处预留 multipart 上传路径，
 * 待后端文件服务就绪后替换为真实端点。
 */
export const ATTACHMENT_UPLOAD_URL: string = `${API_BASE_URL}/api/v1/files/upload`
