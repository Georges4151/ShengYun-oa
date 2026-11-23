<script setup lang="ts">
/**
 * 系统设置 · AI 设置页。
 *
 * - 仅 SYS_ADMIN 可访问（路由 meta.permission = system:ai:config）。
 * - 顶部总开关控制 AI 是否启用；下方两张卡片分别维护「文本模型」与「票据视觉模型」。
 * - 每套配置支持：本地 Ollama / 互联网厂商（OpenAI 兼容协议）、内置厂商预设 + 自定义、
 *   服务地址、模型名、超时秒数、API Key（仅互联网厂商）与连接测试。
 * - API Key 明文永不下发：查询只返回 apiKeySet / apiKeyMasked；保存时
 *   null 表示保持原值、空串表示清空、非空表示更新。
 */

import { computed, onMounted, reactive, ref } from 'vue'
import type { Ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElRadio,
  ElRadioGroup,
  ElSelect,
  ElSwitch,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { PageContainer } from '@ceams/ui-web'
import type {
  AiPresetCode,
  AiPresetVO,
  AiProvider,
  AiProviderConfigVO,
  AiProviderUpdatePayload,
  AiPurpose,
  AiSettingsTestResultVO,
  AiSettingsUpdatePayload,
  AiSettingsVO
} from '@ceams/types'
import { ceamsApi } from '@/services/http'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'AiSettingsView' })

/** 本地 Ollama 默认服务地址。 */
const OLLAMA_DEFAULT_BASE_URL = 'http://localhost:11434'
/** 本地 Ollama 默认模型（按用途区分）。 */
const LOCAL_DEFAULT_MODELS: Record<AiPurpose, string> = {
  TEXT: 'qwen2.5:7b',
  VISION: 'qwen2.5vl:7b'
}
/** 默认超时秒数。 */
const DEFAULT_TIMEOUT_SECONDS = 120
/** 超时取值范围。 */
const MIN_TIMEOUT_SECONDS = 10
const MAX_TIMEOUT_SECONDS = 600

const providerLabels: Record<AiProvider, string> = {
  OLLAMA: '本地 Ollama',
  OPENAI_COMPAT: '互联网厂商'
}

/** 单套提供商表单状态（apiKey 为编辑器草稿，不来自后端）。 */
interface ProviderForm {
  provider: AiProvider
  preset: AiPresetCode
  baseUrl: string
  model: string
  timeoutSeconds: number
  /** null = 不修改；空串 = 清除；非空 = 更新 */
  apiKey: string | null
  /** 后端返回：是否已配置 Key */
  apiKeySet: boolean
  /** 后端返回：脱敏 Key（仅展示） */
  apiKeyMasked: string | null
  /** 是否处于「更换 Key」输入态 */
  apiKeyEditing: boolean
}

/** 页面卡片分段。 */
interface ProviderSection {
  key: 'text' | 'vision'
  purpose: AiPurpose
  title: string
  description: string
  form: ProviderForm
  formRef: Ref<FormInstance | undefined>
}

function emptyForm(purpose: AiPurpose): ProviderForm {
  return {
    provider: 'OLLAMA',
    preset: 'CUSTOM',
    baseUrl: OLLAMA_DEFAULT_BASE_URL,
    model: LOCAL_DEFAULT_MODELS[purpose],
    timeoutSeconds: DEFAULT_TIMEOUT_SECONDS,
    apiKey: null,
    apiKeySet: false,
    apiKeyMasked: null,
    apiKeyEditing: false
  }
}

const enabled = ref(false)
const presets = ref<AiPresetVO[]>([])
const loading = ref(false)
const loadError = ref('')
const saving = ref(false)

const textForm = reactive<ProviderForm>(emptyForm('TEXT'))
const visionForm = reactive<ProviderForm>(emptyForm('VISION'))
const textFormRef = ref<FormInstance>()
const visionFormRef = ref<FormInstance>()

const sections: ProviderSection[] = [
  {
    key: 'text',
    purpose: 'TEXT',
    title: '文本模型',
    description: '用于 AI 助手文本对话、日报摘要等纯文本场景',
    form: textForm,
    formRef: textFormRef
  },
  {
    key: 'vision',
    purpose: 'VISION',
    title: '票据视觉模型',
    description: '用于票据图片识别（编号 / 金额 / 日期 / 类型 / 支付方式）',
    form: visionForm,
    formRef: visionFormRef
  }
]

const testing = reactive<Record<AiPurpose, boolean>>({ TEXT: false, VISION: false })
const testResults = reactive<Partial<Record<AiPurpose, AiSettingsTestResultVO>>>({})

/** 内置预设的服务地址集合（用于切换提供商时判断当前地址是否来自预设）。 */
const presetBaseUrls = computed(
  () => new Set(presets.value.map((preset) => preset.baseUrl.trim()).filter(Boolean))
)

/** 内置预设的默认模型集合。 */
const presetModels = computed(
  () =>
    new Set(
      presets.value
        .flatMap((preset) => [preset.textModel, preset.visionModel])
        .filter((model): model is string => Boolean(model))
    )
)

// ---------- 表单校验 ----------

/** 构造单套配置的校验规则（互联网厂商额外校验地址协议；超时统一约束 10~600）。 */
function buildRules(provider: AiProvider): FormRules<ProviderForm> {
  const rules: FormRules<ProviderForm> = {
    baseUrl: [
      { required: true, whitespace: true, message: '请输入服务地址', trigger: 'blur' },
      {
        validator: (_rule: unknown, value: unknown, callback: (error?: string | Error) => void) => {
          if (provider !== 'OPENAI_COMPAT') {
            callback()
            return
          }
          const url = typeof value === 'string' ? value.trim() : ''
          if (url && !/^https?:\/\//i.test(url)) {
            callback(new Error('服务地址需以 http:// 或 https:// 开头'))
            return
          }
          callback()
        },
        trigger: 'blur'
      }
    ],
    model: [{ required: true, whitespace: true, message: '请输入模型名', trigger: 'blur' }],
    timeoutSeconds: [
      {
        validator: (_rule: unknown, value: unknown, callback: (error?: string | Error) => void) => {
          if (
            typeof value !== 'number' ||
            Number.isNaN(value) ||
            value < MIN_TIMEOUT_SECONDS ||
            value > MAX_TIMEOUT_SECONDS
          ) {
            callback(new Error(`超时秒数需在 ${MIN_TIMEOUT_SECONDS} ~ ${MAX_TIMEOUT_SECONDS} 之间`))
            return
          }
          callback()
        },
        trigger: 'change'
      }
    ]
  }

  return rules
}

const textRules = computed(() => buildRules(textForm.provider))
const visionRules = computed(() => buildRules(visionForm.provider))

// ---------- 交互：提供商 / 预设 / API Key ----------

/** 切换提供商：本地 Ollama 强制自定义预设，并按需带入本地默认地址与模型。 */
function onProviderChange(section: ProviderSection): void {
  const form = section.form
  if (form.provider === 'OLLAMA') {
    form.preset = 'CUSTOM'
    const url = form.baseUrl.trim()
    if (!url || presetBaseUrls.value.has(url)) form.baseUrl = OLLAMA_DEFAULT_BASE_URL
    const model = form.model.trim()
    if (!model || presetModels.value.has(model)) form.model = LOCAL_DEFAULT_MODELS[section.purpose]
  } else {
    form.apiKeyEditing = false
    if (form.baseUrl.trim() === OLLAMA_DEFAULT_BASE_URL) form.baseUrl = ''
    if (form.model.trim() === LOCAL_DEFAULT_MODELS[section.purpose]) form.model = ''
  }
  section.formRef.value?.clearValidate()
}

/** 选择内置预设：自动填充服务地址与对应用途的默认模型；自定义时保留现值供手填。 */
function onPresetChange(section: ProviderSection): void {
  const form = section.form
  if (form.preset === 'CUSTOM') return
  const preset = presets.value.find((item) => item.code === form.preset)
  if (!preset) return
  form.baseUrl = preset.baseUrl
  const defaultModel = section.purpose === 'TEXT' ? preset.textModel : preset.visionModel
  form.model = defaultModel ?? ''
  section.formRef.value?.clearValidate(['baseUrl', 'model'])
}

/** 进入「更换 Key」输入态。 */
function startEditKey(form: ProviderForm): void {
  form.apiKeyEditing = true
  form.apiKey = ''
}

/** 取消更换：恢复为「不修改已保存 Key」。 */
function cancelEditKey(form: ProviderForm): void {
  form.apiKeyEditing = false
  form.apiKey = null
}

/** 清除已保存的 Key：提交空串，保存后生效。 */
function clearKey(form: ProviderForm): void {
  form.apiKeyEditing = false
  form.apiKey = ''
}

/** 撤销「清除」标记。 */
function undoClearKey(form: ProviderForm): void {
  form.apiKey = null
}

/** 是否为「已标记清除、待保存」状态。 */
function isPendingClear(form: ProviderForm): boolean {
  return form.apiKeySet && form.apiKey === '' && !form.apiKeyEditing
}

/** 是否为「已配置且未编辑」状态（展示脱敏值）。 */
function isSavedKey(form: ProviderForm): boolean {
  return form.apiKeySet && form.apiKey === null && !form.apiKeyEditing
}

/** API Key 输入框占位提示。 */
function apiKeyPlaceholder(form: ProviderForm): string {
  return form.apiKeySet ? '留空表示不修改已保存的 Key，填写则更新' : '请输入 API Key'
}

// ---------- 加载 / 保存 ----------

function applyConfig(target: ProviderForm, source: AiProviderConfigVO): void {
  target.provider = source.provider
  target.preset = source.preset
  target.baseUrl = source.baseUrl ?? ''
  target.model = source.model ?? ''
  target.timeoutSeconds = source.timeoutSeconds ?? DEFAULT_TIMEOUT_SECONDS
  target.apiKey = null
  target.apiKeySet = Boolean(source.apiKeySet)
  target.apiKeyMasked = source.apiKeyMasked ?? null
  target.apiKeyEditing = false
}

function applySettings(data: AiSettingsVO): void {
  enabled.value = data.enabled
  presets.value = data.presets ?? []
  applyConfig(textForm, data.text)
  applyConfig(visionForm, data.vision)
}

async function loadSettings(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    applySettings(await ceamsApi.aiSettings.get())
  } catch (err) {
    loadError.value = apiErrorMessage(err, '加载 AI 设置失败')
  } finally {
    loading.value = false
  }
}

function toUpdatePayload(form: ProviderForm): AiProviderUpdatePayload {
  return {
    provider: form.provider,
    preset: form.preset,
    baseUrl: form.baseUrl.trim(),
    model: form.model.trim(),
    timeoutSeconds: form.timeoutSeconds,
    apiKey: form.apiKey
  }
}

async function onSave(): Promise<void> {
  if (saving.value) return

  const [textValid, visionValid] = await Promise.all([
    textFormRef.value?.validate().catch(() => false) ?? false,
    visionFormRef.value?.validate().catch(() => false) ?? false
  ])
  if (!textValid || !visionValid) {
    ElMessage.error('请先完善表单配置后再保存')
    return
  }

  saving.value = true
  try {
    const payload: AiSettingsUpdatePayload = {
      enabled: enabled.value,
      text: toUpdatePayload(textForm),
      vision: toUpdatePayload(visionForm)
    }
    applySettings(await ceamsApi.aiSettings.update(payload))
    // 配置已变化，清空上一轮测试结果，避免误导
    delete testResults.TEXT
    delete testResults.VISION
    ElMessage.success('AI 设置已保存')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存 AI 设置失败'))
  } finally {
    saving.value = false
  }
}

// ---------- 连接测试 ----------

function testAlertType(purpose: AiPurpose): 'success' | 'error' {
  return testResults[purpose]?.success ? 'success' : 'error'
}

function testAlertTitle(purpose: AiPurpose): string {
  const result = testResults[purpose]
  if (!result) return ''
  return `${result.success ? '连接成功' : '连接失败'}：${result.message}`
}

function testAlertDescription(purpose: AiPurpose): string {
  return `耗时 ${testResults[purpose]?.latencyMs ?? 0} ms`
}

async function onTest(section: ProviderSection): Promise<void> {
  if (testing[section.purpose]) return

  const formRef = section.formRef.value
  const valid = formRef ? await formRef.validate().catch(() => false) : false
  if (!valid) return

  const form = section.form
  testing[section.purpose] = true
  try {
    testResults[section.purpose] = await ceamsApi.aiSettings.test({
      purpose: section.purpose,
      provider: form.provider,
      preset: form.preset,
      baseUrl: form.baseUrl.trim(),
      model: form.model.trim(),
      timeoutSeconds: form.timeoutSeconds,
      // null 时不传，后端使用已保存的 Key
      apiKey: form.apiKey ?? undefined
    })
  } catch (err) {
    testResults[section.purpose] = {
      success: false,
      message: apiErrorMessage(err, '测试连接失败'),
      latencyMs: 0
    }
  } finally {
    testing[section.purpose] = false
  }
}

onMounted(() => {
  void loadSettings()
})
</script>

<template>
  <PageContainer
    title="AI 设置"
    subtitle="配置文本对话与票据视觉识别的 AI 提供商，仅系统管理员可维护"
  >
    <template #extra>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="loading"
        @click="onSave"
      >
        保存设置
      </el-button>
    </template>

    <div
      v-loading="loading"
      class="ai-settings"
    >
      <el-alert
        v-if="loadError"
        type="error"
        :closable="false"
        show-icon
        :title="loadError"
        class="load-error"
      >
        <el-button
          link
          type="primary"
          size="small"
          @click="loadSettings"
        >
          重新加载
        </el-button>
      </el-alert>

      <!-- 总开关 -->
      <div class="data-card enable-card">
        <div class="enable-text">
          <span class="card-title">启用 AI 能力</span>
          <span class="enable-desc">
            关闭后系统内的 AI 文本对话、票据识别与智能摘要将不可用；配置仍然保留。
          </span>
        </div>
        <el-switch v-model="enabled" />
      </div>

      <!-- 文本 / 视觉两套配置 -->
      <div
        v-for="section in sections"
        :key="section.key"
        class="data-card provider-card"
      >
        <div class="card-head">
          <div class="card-heading">
            <span class="card-title">{{ section.title }}</span>
            <span class="card-sub">{{ section.description }}</span>
          </div>
          <el-tag
            size="small"
            :type="section.form.provider === 'OLLAMA' ? 'info' : 'primary'"
          >
            {{ providerLabels[section.form.provider] }}
          </el-tag>
        </div>

        <el-form
          :ref="section.formRef"
          :model="section.form"
          :rules="section.purpose === 'TEXT' ? textRules : visionRules"
          label-width="112px"
          class="provider-form"
        >
          <el-form-item label="提供商">
            <el-radio-group
              v-model="section.form.provider"
              @change="onProviderChange(section)"
            >
              <el-radio value="OLLAMA">
                本地 Ollama
              </el-radio>
              <el-radio value="OPENAI_COMPAT">
                互联网厂商
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="厂商预设">
            <el-select
              v-model="section.form.preset"
              :disabled="section.form.provider === 'OLLAMA'"
              style="width: 100%"
              @change="onPresetChange(section)"
            >
              <el-option
                v-for="preset in presets"
                :key="preset.code"
                :label="preset.name"
                :value="preset.code"
              />
            </el-select>
            <div
              v-if="section.form.provider === 'OLLAMA'"
              class="field-tip"
            >
              本地 Ollama 使用自定义配置，厂商预设仅适用于互联网厂商。
            </div>
          </el-form-item>

          <el-form-item
            label="服务地址"
            prop="baseUrl"
          >
            <el-input
              v-model="section.form.baseUrl"
              :placeholder="section.form.provider === 'OLLAMA' ? 'http://localhost:11434' : 'https://api.example.com/v1'"
              clearable
            />
          </el-form-item>

          <el-form-item
            label="模型名"
            prop="model"
          >
            <el-input
              v-model="section.form.model"
              :placeholder="section.purpose === 'TEXT' ? '如 qwen2.5:7b' : '如 qwen2.5vl:7b'"
              clearable
            />
          </el-form-item>

          <el-form-item
            label="超时秒数"
            prop="timeoutSeconds"
          >
            <el-input-number
              v-model="section.form.timeoutSeconds"
              :min="MIN_TIMEOUT_SECONDS"
              :max="MAX_TIMEOUT_SECONDS"
              :step="10"
              style="width: 180px"
            />
            <div class="field-tip">
              单位：秒，范围 {{ MIN_TIMEOUT_SECONDS }} ~ {{ MAX_TIMEOUT_SECONDS }}
            </div>
          </el-form-item>

          <el-form-item
            v-if="section.form.provider === 'OPENAI_COMPAT'"
            label="API Key"
          >
            <div class="api-key-field">
              <!-- 已标记清除，等待保存 -->
              <template v-if="isPendingClear(section.form)">
                <span class="api-key-status">保存后将清除已保存的 Key</span>
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="undoClearKey(section.form)"
                >
                  撤销
                </el-button>
              </template>

              <!-- 已配置且未编辑：只展示脱敏值 -->
              <template v-else-if="isSavedKey(section.form)">
                <span class="api-key-status">{{ section.form.apiKeyMasked || '已配置' }}</span>
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="startEditKey(section.form)"
                >
                  更换
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="clearKey(section.form)"
                >
                  清除
                </el-button>
              </template>

              <!-- 输入态 -->
              <template v-else>
                <el-input
                  v-model="section.form.apiKey"
                  type="password"
                  show-password
                  clearable
                  autocomplete="new-password"
                  :placeholder="apiKeyPlaceholder(section.form)"
                  style="width: 100%"
                />
                <el-button
                  v-if="section.form.apiKeySet"
                  link
                  type="primary"
                  size="small"
                  @click="cancelEditKey(section.form)"
                >
                  取消
                </el-button>
              </template>
            </div>
            <div class="field-tip">
              Key 仅保存在服务端，接口只返回脱敏值；留空表示不修改。
            </div>
          </el-form-item>
        </el-form>

        <div class="card-footer">
          <el-button
            :loading="testing[section.purpose]"
            :disabled="testing[section.purpose]"
            @click="onTest(section)"
          >
            测试连接
          </el-button>
          <span class="footer-tip">使用当前表单值测试，不会保存配置。</span>
        </div>

        <el-alert
          v-if="testResults[section.purpose]"
          class="test-alert"
          :type="testAlertType(section.purpose)"
          :closable="false"
          show-icon
          :title="testAlertTitle(section.purpose)"
          :description="testAlertDescription(section.purpose)"
        />
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
/* 页面纵向卡片间距 16px */
.ai-settings {
  display: flex;
  flex-direction: column;
  gap: var(--ceams-space-4);
}

.load-error {
  margin-bottom: 0;
}

/* 总开关卡片：左侧说明、右侧开关 */
.enable-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-4);
  padding: 18px 20px;
}

.enable-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.enable-desc {
  font-size: var(--ceams-font-size-sm);
  line-height: 1.6;
  color: var(--ceams-text-secondary);
}

/* 配置卡片 */
.provider-card {
  padding: 20px;
}

.card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-4);
}

.card-heading {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.card-title {
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-sub {
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
}

/* 表单限制阅读宽度，避免超宽输入框 */
.provider-form {
  max-width: 720px;
}

/* 字段说明：小号灰字 */
.field-tip {
  width: 100%;
  margin-top: 4px;
  font-size: var(--ceams-font-size-xs);
  line-height: 1.5;
  color: var(--ceams-text-secondary);
}

/* API Key 行：控件与操作按钮同行，允许换行 */
.api-key-field {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ceams-space-2);
  width: 100%;
}

.api-key-status {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-primary);
  word-break: break-all;
}

/* 测试按钮行 */
.card-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ceams-space-3);
  padding-top: var(--ceams-space-2);
}

.footer-tip {
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
}

.test-alert {
  margin-top: var(--ceams-space-3);
}

/* 窄屏：总开关卡片纵向排列 */
@media (max-width: 640px) {
  .enable-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
