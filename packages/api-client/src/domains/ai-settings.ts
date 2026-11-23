/**
 * 系统设置 · AI 设置域（ai-settings）类型化 API 工厂。
 *
 * 路径前缀 `/api/v1/admin/ai-settings`，仅系统管理员（SYS_ADMIN）可访问；
 * 权限码由后端服务层校验。所有方法返回后端 `Result<T>` 解包后的 `data`。
 *
 * 注意：
 * - 查询接口不会返回 API Key 明文，仅返回 `apiKeySet` / `apiKeyMasked`；
 * - 保存时 `apiKey` 为 null 表示保持原值，空串表示清空，非空表示更新；
 * - 连接测试可能触发真实模型调用，单次放宽超时（默认 15s 太短）。
 */

import type { HttpClient } from '../http-client'
import type {
  AiSettingsTestPayload,
  AiSettingsTestResultVO,
  AiSettingsUpdatePayload,
  AiSettingsVO
} from '@ceams/types'

export interface AiSettingsApi {
  /** 查询 AI 设置（总开关 + 文本/视觉配置 + 内置预设）。 */
  get(): Promise<AiSettingsVO>
  /** 保存 AI 设置，返回保存后的最新配置。 */
  update(body: AiSettingsUpdatePayload): Promise<AiSettingsVO>
  /** 测试文本 / 视觉提供商的连通性（apiKey 为空时后端使用已保存的 Key）。 */
  test(body: AiSettingsTestPayload): Promise<AiSettingsTestResultVO>
}

const PREFIX = '/api/v1/admin/ai-settings'

export function createAiSettingsApi(client: HttpClient): AiSettingsApi {
  return {
    async get() {
      return client.get<AiSettingsVO>(PREFIX)
    },
    async update(body) {
      return client.put<AiSettingsVO>(PREFIX, body)
    },
    async test(body) {
      return client.post<AiSettingsTestResultVO>(`${PREFIX}/test`, body, { timeoutMs: 120000 })
    }
  }
}
