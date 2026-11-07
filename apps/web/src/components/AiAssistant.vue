<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElAlert, ElButton, ElCollapse, ElCollapseItem, ElDrawer, ElInput, ElTag } from 'element-plus'
import type { AiChatRequest, AiChatToolData, AiStatusVO } from '@ceams/types'
import { ceamsApi } from '@/services/http'
import { isAbortError, streamChat } from '@/services/ai'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'AiAssistant' })

const route = useRoute()

/** 工具调用轨迹（仅展示工具名 / 行数 / 参数摘要，不向用户暴露原始 JSON）。 */
interface ToolTrace {
  name: string
  rows: number
  argsText: string[]
}

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  tools: ToolTrace[]
  error: string
  pending: boolean
}

let seq = 0
function nextId(): string {
  seq += 1
  return `${Date.now()}-${seq}`
}

// ---------- 抽屉与状态 ----------
const open = ref(false)
const status = ref<AiStatusVO | null>(null)
const statusLoading = ref(false)
const statusError = ref('')
const statusLoaded = ref(false)
const model = ref('')

/**
 * 首次打开时探测一次 AI 服务状态（不做定时轮询，避免打爆 Ollama 探活）。
 * 探测失败允许手动「重新检测」。
 */
async function ensureStatus(): Promise<void> {
  if (statusLoaded.value || statusLoading.value) return
  statusLoading.value = true
  statusError.value = ''
  try {
    const data = await ceamsApi.ai.status()
    status.value = data
    model.value = data.model
    statusLoaded.value = true
  } catch (err) {
    statusError.value = apiErrorMessage(err, 'AI 状态获取失败')
  } finally {
    statusLoading.value = false
  }
}

function openPanel(): void {
  open.value = true
  void ensureStatus()
}

function minimize(): void {
  // 最小化：仅收起抽屉，保留会话与消息，可随时继续问
  open.value = false
}

function endSession(): void {
  // 关闭：结束当前会话（中断流式输出并清空会话上下文）
  stopGenerating()
  open.value = false
  resetConversation()
}

/** 状态提示：未启用 / 不可达。空字符串表示可用。 */
const blocker = computed(() => {
  const current = status.value
  if (!current) return ''
  if (!current.enabled) return 'AI 功能未启用'
  if (!current.reachable) return 'AI 服务暂不可用（Ollama 未连接）'
  return ''
})

const inputDisabled = computed(() => statusLoading.value || status.value === null || blocker.value !== '')

const inputPlaceholder = computed(() => {
  if (statusLoading.value) return '正在检测 AI 服务…'
  if (!status.value) return 'AI 服务状态未知，请先重新检测'
  if (!status.value.enabled) return 'AI 功能未启用，暂不可提问'
  if (!status.value.reachable) return 'AI 服务暂不可用，请稍后重试'
  return '输入问题，Enter 发送 / Shift+Enter 换行'
})

// ---------- 会话 ----------
const conversationId = ref<string | null>(null)
const messages = ref<ChatMessage[]>([])
const input = ref('')
const streaming = ref(false)
const listRef = ref<HTMLElement | null>(null)
let controller: AbortController | null = null

const canSend = computed(() => !streaming.value && !inputDisabled.value && input.value.trim().length > 0)

function scrollToBottom(): void {
  void nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

/** 通过响应式数组下标改写消息，保证流式增量能触发视图更新。 */
function updateAssistant(index: number, mutate: (message: ChatMessage) => void): void {
  const target = messages.value[index]
  if (!target) return
  mutate(target)
  scrollToBottom()
}

function toToolTrace(data: AiChatToolData): ToolTrace {
  const argsText: string[] = []
  for (const [key, value] of Object.entries(data.args)) {
    if (value === null || value === undefined) continue
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      argsText.push(`${key}：${value}`)
    } else if (Array.isArray(value)) {
      argsText.push(`${key}：${value.length} 项`)
    } else {
      argsText.push(`${key}：（对象）`)
    }
  }
  return { name: data.name, rows: data.rows, argsText }
}

/**
 * 请求失败时回填用户输入，避免重新敲一遍（输入框已有新内容时不覆盖）。
 */
function restoreInput(text: string): void {
  if (!input.value.trim()) input.value = text
}

function onKeydown(event: Event | KeyboardEvent): void {
  if (!(event instanceof KeyboardEvent)) return
  // Enter 发送、Shift+Enter 换行；输入法组合态下的 Enter 不拦截
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    void send()
  }
}

async function send(): Promise<void> {
  if (!canSend.value) return
  const text = input.value.trim()
  const userMessage: ChatMessage = {
    id: nextId(),
    role: 'user',
    content: text,
    tools: [],
    error: '',
    pending: false
  }
  const assistantMessage: ChatMessage = {
    id: nextId(),
    role: 'assistant',
    content: '',
    tools: [],
    error: '',
    pending: true
  }
  messages.value.push(userMessage, assistantMessage)
  const assistantIndex = messages.value.length - 1
  input.value = ''
  scrollToBottom()

  streaming.value = true
  controller = new AbortController()

  const payload: AiChatRequest = {
    conversationId: conversationId.value,
    message: text,
    context: typeof route.name === 'string' ? { page: route.name } : undefined
  }

  try {
    await streamChat(
      payload,
      {
        onMeta(data) {
          conversationId.value = data.conversationId
          model.value = data.model
        },
        onDelta(data) {
          updateAssistant(assistantIndex, (message) => {
            message.content += data.text
          })
        },
        onTool(data) {
          updateAssistant(assistantIndex, (message) => {
            message.tools.push(toToolTrace(data))
          })
        },
        onDone() {
          updateAssistant(assistantIndex, (message) => {
            message.pending = false
          })
        },
        onError(data) {
          updateAssistant(assistantIndex, (message) => {
            message.error = data.message || `AI 返回错误（${data.code}）`
          })
          restoreInput(text)
        }
      },
      controller.signal
    )
  } catch (err) {
    const aborted = isAbortError(err)
    const message = aborted ? '已停止生成' : apiErrorMessage(err, 'AI 请求失败，请稍后重试')
    updateAssistant(assistantIndex, (target) => {
      if (!target.content) target.error = message
      else target.error = `生成中断：${message}`
    })
    // 主动停止不回填；网络/HTTP 失败回填问题，便于直接重试
    if (!aborted) restoreInput(text)
  } finally {
    streaming.value = false
    controller = null
    updateAssistant(assistantIndex, (message) => {
      message.pending = false
    })
  }
}

function stopGenerating(): void {
  controller?.abort()
}

function resetConversation(): void {
  conversationId.value = null
  messages.value = []
  input.value = ''
}

onBeforeUnmount(() => {
  controller?.abort()
})
</script>

<template>
  <div class="ai-assistant">
    <!-- 悬浮入口：抽屉收起时展示 -->
    <button v-show="!open" class="ai-fab" type="button" title="打开 AI 助手" @click="openPanel">
      <svg class="ai-fab__icon" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
        <path
          fill="currentColor"
          d="M12 2C6.48 2 2 5.94 2 10.8c0 2.6 1.4 4.93 3.7 6.5V22l4-2.2c.75.1 1.53.16 2.3.16 5.52 0 10-3.94 10-8.82S17.52 2 12 2Z"
        />
      </svg>
      <span>AI 助手</span>
    </button>

    <el-drawer
      v-model="open"
      class="ai-drawer"
      direction="rtl"
      size="420px"
      :with-header="false"
      :show-close="false"
      :modal="false"
      modal-penetrable
      :lock-scroll="false"
      append-to-body
    >
      <div class="ai-panel">
        <header class="ai-panel__header">
          <div class="ai-panel__title">
            <span class="ai-panel__logo">AI</span>
            <span>AI 助手</span>
            <el-tag v-if="model" size="small" type="success" effect="plain">{{ model }}</el-tag>
          </div>
          <div class="ai-panel__actions">
            <el-button link size="small" title="收起抽屉，保留当前会话" @click="minimize">最小化</el-button>
            <el-button link size="small" title="结束当前会话并收起抽屉" @click="endSession">关闭</el-button>
          </div>
        </header>

        <div v-if="statusLoading || statusError || blocker" class="ai-panel__status">
          <el-alert v-if="statusLoading" type="info" :closable="false" show-icon title="正在检测 AI 服务…" />
          <el-alert v-else-if="statusError" type="error" :closable="false" show-icon :title="statusError">
            <el-button link type="primary" size="small" @click="ensureStatus">重新检测</el-button>
          </el-alert>
          <el-alert v-else-if="blocker" type="warning" :closable="false" show-icon :title="blocker" />
        </div>

        <div ref="listRef" class="ai-panel__messages">
          <div v-if="messages.length === 0" class="ai-panel__empty">
            <p class="ai-panel__empty-title">可以这样问：</p>
            <ul>
              <li>本月报销一共多少钱？</li>
              <li>当前有哪些待审批单据？</li>
              <li>查询项目进度款收款情况</li>
            </ul>
          </div>

          <div v-for="msg in messages" :key="msg.id" class="ai-msg" :class="`ai-msg--${msg.role}`">
            <div class="ai-msg__bubble">
              <div v-if="msg.content" class="ai-msg__content">{{ msg.content }}</div>
              <div v-else-if="msg.pending" class="ai-msg__typing">正在思考…</div>

              <el-collapse v-if="msg.tools.length" class="ai-msg__tools">
                <el-collapse-item
                  v-for="(tool, toolIndex) in msg.tools"
                  :key="`${msg.id}-${toolIndex}`"
                  :title="`已查询：${tool.name}（${tool.rows} 行）`"
                  :name="`${msg.id}-${toolIndex}`"
                >
                  <div class="ai-tool-args">
                    <div v-for="line in tool.argsText" :key="line">{{ line }}</div>
                    <div v-if="tool.argsText.length === 0">无查询参数</div>
                  </div>
                </el-collapse-item>
              </el-collapse>

              <div v-if="msg.error" class="ai-msg__error">{{ msg.error }}</div>
            </div>
          </div>
        </div>

        <footer class="ai-panel__footer">
          <el-input
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            resize="none"
            :placeholder="inputPlaceholder"
            :disabled="inputDisabled"
            @keydown="onKeydown"
          />
          <div class="ai-panel__footer-actions">
            <span class="ai-panel__disclaimer">AI 建议，请核实后再使用</span>
            <div class="ai-panel__buttons">
              <el-button size="small" :disabled="streaming" @click="resetConversation">新会话</el-button>
              <el-button v-if="streaming" size="small" type="danger" plain @click="stopGenerating">停止生成</el-button>
              <el-button size="small" type="primary" :disabled="!canSend" @click="send">发送</el-button>
            </div>
          </div>
        </footer>
      </div>
    </el-drawer>
  </div>
</template>

<style>
/* 组件样式为全局样式（el-drawer 会 teleport 到 body，scoped 无法命中），统一使用 ai- 前缀避免污染 */
.ai-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: none;
  border-radius: 999px;
  background: #409eff;
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.35);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.ai-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.45);
}
.ai-fab__icon {
  display: block;
}

/* 抽屉本体：去内边距，让面板撑满 */
.ai-drawer {
  display: flex;
  flex-direction: column;
}
.ai-drawer .el-drawer__body {
  padding: 0;
  overflow: hidden;
}

.ai-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: #fff;
}
.ai-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: none;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
}
.ai-panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.ai-panel__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}
.ai-panel__actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.ai-panel__status {
  flex: none;
  padding: 8px 16px 0;
}
.ai-panel__messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}
.ai-panel__empty {
  margin-top: 24px;
  padding: 16px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  background: #fff;
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}
.ai-panel__empty-title {
  margin: 0 0 4px;
  font-weight: 600;
  color: #303133;
}
.ai-panel__empty ul {
  margin: 0;
  padding-left: 18px;
}

.ai-msg {
  display: flex;
  margin-bottom: 12px;
}
.ai-msg--user {
  justify-content: flex-end;
}
.ai-msg__bubble {
  max-width: 88%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.ai-msg--user .ai-msg__bubble {
  border-color: #409eff;
  background: #409eff;
  color: #fff;
}
.ai-msg__content {
  white-space: pre-wrap;
}
.ai-msg__typing {
  color: #909399;
  font-size: 13px;
}
.ai-msg__tools {
  margin-top: 8px;
  border-top: 1px solid #ebeef5;
  border-bottom: none;
}
.ai-msg__tools .el-collapse-item__header {
  height: 32px;
  border-bottom: none;
  color: #909399;
  font-size: 12px;
}
.ai-msg__tools .el-collapse-item__wrap {
  border-bottom: none;
  background: transparent;
}
.ai-msg__tools .el-collapse-item__content {
  padding-bottom: 4px;
}
.ai-tool-args {
  color: #606266;
  font-size: 12px;
  line-height: 1.8;
}
.ai-msg__error {
  margin-top: 8px;
  color: #f56c6c;
  font-size: 13px;
}

.ai-panel__footer {
  flex: none;
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
}
.ai-panel__footer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}
.ai-panel__disclaimer {
  color: #909399;
  font-size: 12px;
}
.ai-panel__buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
