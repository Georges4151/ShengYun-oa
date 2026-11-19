/**
 * 企业微信集成域（integration）类型：组织架构同步批次、应用消息发件箱、扫码登录。
 *
 * 字段与后端 `com.ceams.module.integration.vo` / `AuthController` 严格一致：
 * - 主键与关联 ID（id / notificationId / targetUserId）在后端为 Long（雪花 ID），
 *   JSON 层统一序列化为字符串，故此处使用 `Id`（= string）而非 number
 * - 时间字段为后端格式化的 `yyyy-MM-dd HH:mm:ss` 字符串，允许为空
 * - 状态与触发方式在后端为枚举，JSON 层为字符串字面量
 */

import type { DateTime, Id } from './common'

// ---------- 状态枚举 ----------

/** 同步批次状态：进行中 / 全部成功 / 部分成功 / 失败。 */
export type WecomSyncStatus = 'RUNNING' | 'SUCCESS' | 'PARTIAL' | 'FAILED'

/** 同步触发方式：手动同步 / 定时任务。 */
export type WecomSyncType = 'MANUAL' | 'SCHEDULED'

/** 应用消息状态：待发送 / 已发送 / 失败。 */
export type WecomMessageStatus = 'PENDING' | 'SENT' | 'FAILED'

// ---------- 组织架构同步 ----------

/** 同步批次日志（GET /integration/wecom/sync/logs 列表项）。 */
export interface WecomSyncLogVO {
  id: Id
  /** 同步批次号（唯一） */
  batchNo: string
  /** 触发方式 */
  syncType: WecomSyncType
  /** 同步对象总数（部门 + 成员） */
  total: number
  successCount: number
  failedCount: number
  status: WecomSyncStatus
  /** 失败原因（超长由前端省略展示） */
  errorMsg?: string | null
  /** 开始时间 */
  startedAt: DateTime
  /** 结束时间（RUNNING 时为空） */
  finishedAt?: DateTime | null
}

/** 手动同步结果（POST /integration/wecom/sync）。 */
export interface WecomSyncResultVO {
  batchNo: string
  status: WecomSyncStatus
  total: number
  successCount: number
  failedCount: number
  errorMsg?: string | null
}

// ---------- 应用消息发件箱 ----------

/** 企业微信应用消息记录（GET /integration/wecom/messages 列表项）。 */
export interface WecomMessageVO {
  id: Id
  /** 站内通知 ID */
  notificationId: Id
  /** 目标用户 ID（CEAMS 用户主键） */
  targetUserId: Id
  /** 企业微信成员账号（入队时快照） */
  wecomUserid: string
  /** 消息类型：当前仅 TEXT */
  msgType: string
  status: WecomMessageStatus
  /** 已重试次数 */
  retryCount: number
  /** 下次重试时间（PENDING 且待重试时有值） */
  nextRetryAt?: DateTime | null
  /** 发送成功时间 */
  sentAt?: DateTime | null
  /** 最近一次失败原因 */
  errorMsg?: string | null
  createTime: DateTime
}

// ---------- 扫码登录 ----------

/** 扫码登录地址与一次性 state（state 5 分钟有效，读到即删）。 */
export interface WecomQrCodeVO {
  /** 企业微信授权页地址，前端直接整页跳转 */
  loginUrl: string
  /** 一次性随机 state，回调时原样回传 */
  state: string
}

/** 企业微信扫码登录入参（deviceId 由服务端按 state 还原，无需前端传递）。 */
export interface WecomLoginBody {
  /** 企业微信授权码 */
  code: string
  /** 发起扫码时下发的一次性 state */
  state: string
}

// ---------- 分页查询 ----------

/** 集成域通用分页查询；status 为空表示不按状态过滤。 */
export interface WecomPageQuery {
  page: number
  size: number
  status?: WecomSyncStatus | WecomMessageStatus
}
