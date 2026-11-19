/**
 * 企业微信集成域（integration）类型化 API 工厂。
 *
 * 路径分两组：
 * - 扫码登录 `/api/v1/auth/wecom`（匿名可访问，需 `skipAuth` 跳过 Bearer 头；
 *   401 时也不会触发刷新 / 清会话逻辑）
 * - 集成管理 `/api/v1/integration/wecom`（普通 Bearer 鉴权，权限码由后端服务层校验）
 *
 * 所有方法返回后端 `Result<T>` 解包后的 `data`（由 HttpClient 统一处理）。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  LoginData,
  PageResult,
  WecomLoginBody,
  WecomMessageStatus,
  WecomMessageVO,
  WecomPageQuery,
  WecomQrCodeVO,
  WecomSyncLogVO,
  WecomSyncResultVO,
  WecomSyncStatus
} from '@ceams/types'

/** 同步批次分页查询参数（status 为空表示不过滤）。 */
export type WecomSyncLogQuery = Omit<WecomPageQuery, 'status'> & { status?: WecomSyncStatus }

/** 应用消息分页查询参数（status 为空表示不过滤）。 */
export type WecomMessageQuery = Omit<WecomPageQuery, 'status'> & { status?: WecomMessageStatus }

export interface IntegrationApi {
  /** 生成企业微信扫码登录地址（未启用时后端返回 503）。 */
  getWecomQrcode(deviceId?: string): Promise<WecomQrCodeVO>
  /** 授权码换令牌（返回结构与普通登录一致，未绑定 400 / 禁用 403 / 未启用 503）。 */
  loginByWecom(body: WecomLoginBody): Promise<LoginData>
  /** 手动触发组织架构同步（未启用 503；企业微信接口异常时仍返回 FAILED 批次）。 */
  triggerWecomSync(): Promise<WecomSyncResultVO>
  /** 同步批次分页（需 integration:wecom:view）。 */
  pageWecomSyncLogs(params?: WecomSyncLogQuery): Promise<PageResult<WecomSyncLogVO>>
  /** 应用消息分页（需 integration:wecom:view）。 */
  pageWecomMessages(params?: WecomMessageQuery): Promise<PageResult<WecomMessageVO>>
  /** 重投失败消息（需 integration:wecom:sync；非 FAILED 返回 400）。 */
  retryWecomMessage(id: Id): Promise<void>
}

/** 扫码登录前缀（匿名）。 */
const AUTH_PREFIX = '/api/v1/auth/wecom'

/** 集成管理前缀（需登录态）。 */
const PREFIX = '/api/v1/integration/wecom'

export function createIntegrationApi(client: HttpClient): IntegrationApi {
  return {
    // ---------- 扫码登录 ----------
    async getWecomQrcode(deviceId) {
      return client.get<WecomQrCodeVO>(`${AUTH_PREFIX}/qrcode`, {
        query: { deviceId },
        skipAuth: true
      })
    },
    async loginByWecom(body) {
      return client.post<LoginData>(`${AUTH_PREFIX}/login`, body, { skipAuth: true })
    },

    // ---------- 组织架构同步 ----------
    async triggerWecomSync() {
      return client.post<WecomSyncResultVO>(`${PREFIX}/sync`)
    },
    async pageWecomSyncLogs(params = { page: 1, size: 10 }) {
      return client.get<PageResult<WecomSyncLogVO>>(`${PREFIX}/sync/logs`, {
        query: {
          page: params.page,
          size: params.size,
          status: params.status
        }
      })
    },

    // ---------- 应用消息发件箱 ----------
    async pageWecomMessages(params = { page: 1, size: 10 }) {
      return client.get<PageResult<WecomMessageVO>>(`${PREFIX}/messages`, {
        query: {
          page: params.page,
          size: params.size,
          status: params.status
        }
      })
    },
    async retryWecomMessage(id) {
      return client.post<void>(`${PREFIX}/messages/${id}/retry`)
    }
  }
}
