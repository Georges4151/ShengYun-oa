/**
 * 统一结果码 / 错误码枚举。
 *
 * 规则（与 ARCHITECTURE.md 第 6 节一致）：
 * - 成功码固定为字符串 "0"（契约冻结后以 OpenAPI 为准，请勿在业务代码硬编码其它值）
 * - 错误码采用 RFC7807 风格 + 业务码前缀：AUTH_ / VALID_ / BIZ_ / SIGN_
 * - HTTP 语义严格：错误码只描述"是什么"，HTTP 状态码描述"传输层结果"
 */
export enum ResultCode {
  /** 成功 */
  SUCCESS = '0',

  // ---- AUTH_ / 鉴权域 ----
  /** 未认证：缺少或无效的访问令牌 */
  UNAUTHORIZED = 'AUTH_UNAUTHORIZED',
  /** 无权限访问资源 */
  FORBIDDEN = 'AUTH_FORBIDDEN',
  /** 访问令牌已过期 */
  ACCESS_TOKEN_EXPIRED = 'AUTH_ACCESS_TOKEN_EXPIRED',
  /** 刷新令牌已过期或被吊销 */
  REFRESH_TOKEN_EXPIRED = 'AUTH_REFRESH_TOKEN_EXPIRED',
  /** 设备未绑定或已失效 */
  DEVICE_INVALID = 'AUTH_DEVICE_INVALID',

  // ---- VALID_ / 参数校验域 ----
  /** 参数校验失败 */
  VALIDATION_ERROR = 'VALID_INVALID_PARAMETER',

  // ---- BIZ_ / 业务域 ----
  /** 通用业务错误 */
  BIZ_ERROR = 'BIZ_ERROR',
  /** 资源不存在 */
  NOT_FOUND = 'BIZ_NOT_FOUND',
  /** 资源冲突（唯一键冲突等） */
  CONFLICT = 'BIZ_CONFLICT',

  // ---- SIGN_ / 签名与防重放域 ----
  /** 签名校验失败 */
  SIGN_INVALID = 'SIGN_INVALID',
  /** 签名时间戳超出 ±5min 窗口 */
  SIGN_TIMESTAMP_EXPIRED = 'SIGN_TIMESTAMP_EXPIRED',
  /** nonce 重放 */
  SIGN_NONCE_REPLAYED = 'SIGN_NONCE_REPLAYED'
}

/** 错误码字符串联合类型（兼容后端动态返回的未知码） */
export type ResultCodeValue = `${string}`