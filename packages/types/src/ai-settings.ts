/**
 * 系统设置 · AI 设置域类型：AI 总开关、文本 / 视觉两套提供商配置、内置厂商预设。
 *
 * 字段与后端 `GET/PUT /api/v1/admin/ai-settings`、`POST /api/v1/admin/ai-settings/test`
 * 契约严格一致：
 * - provider：OLLAMA（本地 Ollama） / OPENAI_COMPAT（OpenAI 兼容协议的互联网厂商）
 * - preset：内置预设编码，CUSTOM 表示手填地址与模型
 * - apiKey 明文永远不下发，查询接口只返回 `apiKeySet` 与 `apiKeyMasked`
 */

/** AI 提供商类型：本地 Ollama / 互联网厂商（OpenAI 兼容协议）。 */
export type AiProvider = 'OLLAMA' | 'OPENAI_COMPAT'

/** 厂商预设编码：内置厂商 + 自定义。 */
export type AiPresetCode = 'DEEPSEEK' | 'DASHSCOPE' | 'SILICONFLOW' | 'OPENAI' | 'CUSTOM'

/** 配置用途：文本对话 / 票据视觉识别。 */
export type AiPurpose = 'TEXT' | 'VISION'

/** 文本或视觉的单套提供商配置（查询响应，不含 Key 明文）。 */
export interface AiProviderConfigVO {
  provider: AiProvider
  preset: AiPresetCode
  /** 服务地址，如 http://localhost:11434 或 https://api.deepseek.com/v1 */
  baseUrl: string
  /** 模型名，如 qwen2.5:7b */
  model: string
  /** 请求超时（秒） */
  timeoutSeconds: number
  /** 是否已配置 API Key（明文不会返回） */
  apiKeySet: boolean
  /** 脱敏后的 API Key，如 sk-****abcd；未配置时为 null */
  apiKeyMasked: string | null
}

/** 内置厂商预设：选中后自动带出服务地址与默认模型。 */
export interface AiPresetVO {
  code: AiPresetCode
  /** 中文名称，如「阿里云百炼」 */
  name: string
  /** 服务地址（CUSTOM 时为空串） */
  baseUrl: string
  /** 默认文本模型；不支持文本的厂商为 null */
  textModel: string | null
  /** 默认视觉模型；不支持视觉的厂商为 null */
  visionModel: string | null
}

/** AI 设置查询响应（GET /admin/ai-settings）。 */
export interface AiSettingsVO {
  /** AI 总开关 */
  enabled: boolean
  text: AiProviderConfigVO
  vision: AiProviderConfigVO
  presets: AiPresetVO[]
}

/** 单套提供商的保存入参。 */
export interface AiProviderUpdatePayload {
  provider: AiProvider
  preset: AiPresetCode
  baseUrl: string
  model: string
  timeoutSeconds: number
  /**
   * API Key 语义：
   * - null / 不填：保持原值
   * - 空串：清空已保存的 Key
   * - 非空：更新 Key
   */
  apiKey: string | null
}

/** AI 设置保存入参（PUT /admin/ai-settings）。 */
export interface AiSettingsUpdatePayload {
  enabled: boolean
  text: AiProviderUpdatePayload
  vision: AiProviderUpdatePayload
}

/** 连接测试入参（POST /admin/ai-settings/test）；后 6 项可选。 */
export interface AiSettingsTestPayload {
  purpose: AiPurpose
  provider?: AiProvider
  preset?: AiPresetCode
  baseUrl?: string
  model?: string
  timeoutSeconds?: number
  /** 为空时后端使用已保存的 Key */
  apiKey?: string | null
}

/** 连接测试结果。 */
export interface AiSettingsTestResultVO {
  success: boolean
  /** 后端返回的中文提示（成功说明或失败原因） */
  message: string
  /** 端到端耗时（毫秒） */
  latencyMs: number
}
