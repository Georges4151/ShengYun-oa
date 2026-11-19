<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { AiChatSyncResult, AiChatSyncToolCall, AiTodoSummaryVO } from '@ceams/types'
import { apiClient } from '../../platform/http'
import { isLoggedIn } from '../../platform/auth'
import { apiErrorMessage } from '../../utils/error'

/** 页面内的对话气泡（只读问答，不落库，纯前端会话态） */
interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  model?: string
  latencyMs?: number
  toolCalls?: AiChatSyncToolCall[]
}

const messages = ref<ChatMessage[]>([])
const input = ref('')
const conversationId = ref<string | null>(null)
const sending = ref(false)
/** AI 服务可用（enabled 且 reachable）才允许提问 */
const aiReady = ref(false)
const statusChecked = ref(false)
/** 工具调用明细展开状态（按消息 id 记录） */
const toolsOpen = ref<Record<string, boolean>>({})

// ---------- 待办摘要（只读，登录即可加载，失败不影响对话） ----------
const todo = ref<AiTodoSummaryVO | null>(null)
const todoLoading = ref(false)
const todoError = ref('')
const todoExpanded = ref(false)
let todoRequested = false

let localSeq = 0
let statusRequested = false

/** 加载待办摘要（force=true 用于加载失败后重试） */
async function loadTodoSummary(force = false): Promise<void> {
  if (todoLoading.value) return
  if (!force && todoRequested && todo.value) return
  todoLoading.value = true
  todoError.value = ''
  try {
    todo.value = await apiClient.ai.todoSummary()
    todoRequested = true
  } catch (err) {
    todoError.value = apiErrorMessage(err, '待办摘要加载失败')
  } finally {
    todoLoading.value = false
  }
}

function toggleTodoExpanded(): void {
  todoExpanded.value = !todoExpanded.value
}

/** 到期时间展示（MM-dd HH:mm，解析失败原样返回） */
function formatTodoDue(value?: string | null): string {
  if (!value) return '未设置'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const inputPlaceholder = computed(() => {
  if (!statusChecked.value) return '正在检测 AI 服务…'
  return aiReady.value ? '用自然语言查账，例如：本月报销合计是多少？' : 'AI 服务暂不可用'
})

/** 首次进页面探活一次：禁用输入并提示的条件是未启用或不可达。 */
async function loadStatus(): Promise<void> {
  try {
    const status = await apiClient.ai.status()
    aiReady.value = status.enabled && status.reachable
  } catch {
    aiReady.value = false
  } finally {
    statusChecked.value = true
  }
}

function toggleTools(id: string): void {
  toolsOpen.value = { ...toolsOpen.value, [id]: !toolsOpen.value[id] }
}

function toolSummary(calls: AiChatSyncToolCall[]): string {
  return calls.map((call) => `${call.name}（${call.rows} 行）`).join('、')
}

function toolArgsText(args: Record<string, unknown>): string {
  return JSON.stringify(args)
}

function metaText(item: ChatMessage): string {
  const parts: string[] = []
  if (item.model) parts.push(item.model)
  if (item.latencyMs !== undefined) parts.push(`${item.latencyMs}ms`)
  return parts.join(' · ')
}

function scrollToBottom(): void {
  void nextTick(() => {
    uni.pageScrollTo({ scrollTop: 100000, duration: 0 })
  })
}

function startNewConversation(): void {
  if (sending.value) return
  conversationId.value = null
  messages.value = []
  toolsOpen.value = {}
  input.value = ''
}

async function send(): Promise<void> {
  const question = input.value.trim()
  if (!question || sending.value) return
  if (!aiReady.value) {
    uni.showToast({ title: 'AI 服务暂不可用', icon: 'none' })
    return
  }

  sending.value = true
  localSeq += 1
  const localId = `local-${localSeq}`
  messages.value.push({ id: localId, role: 'user', content: question })
  input.value = ''
  scrollToBottom()

  try {
    const result: AiChatSyncResult = await apiClient.ai.chatSync({
      conversationId: conversationId.value,
      message: question,
      context: { page: 'ai-assistant' }
    })
    conversationId.value = result.conversationId
    messages.value.push({
      id: result.messageId,
      role: 'assistant',
      content: result.answer,
      model: result.model,
      latencyMs: result.latencyMs,
      toolCalls: result.toolCalls
    })
    scrollToBottom()
  } catch (err) {
    // 失败时移除乐观插入的用户气泡并把问题回填，避免重发后出现重复消息
    messages.value = messages.value.filter((item) => item.id !== localId)
    input.value = question
    uni.showToast({ title: apiErrorMessage(err, 'AI 查询失败'), icon: 'none' })
  } finally {
    sending.value = false
  }
}

onShow(() => {
  if (!isLoggedIn()) {
    uni.redirectTo({ url: '/pages/login/login' })
    return
  }
  if (!statusRequested) {
    statusRequested = true
    void loadStatus()
  }
  void loadTodoSummary()
})
</script>

<template>
  <view class="ai-page">
    <!-- 顶部提示条：固定免责说明 -->
    <view class="ai-page__notice">
      <text class="ai-page__notice-text">AI 建议，请核实；仅支持只读查询</text>
    </view>

    <!-- 待办摘要卡片：只读汇总，失败不影响下方对话 -->
    <view class="ai-page__todo">
      <view class="ai-page__todo-head" @click="toggleTodoExpanded">
        <text class="ai-page__todo-title">我的待办摘要</text>
        <text class="ai-page__todo-toggle">{{ todoExpanded ? '收起' : '展开' }}</text>
      </view>

      <view v-if="todoLoading" class="ai-page__todo-hint">正在加载…</view>
      <view v-else-if="todoError" class="ai-page__todo-hint ai-page__todo-hint--error">
        <text>{{ todoError }}</text>
        <text class="ai-page__todo-retry" @click.stop="loadTodoSummary(true)">重试</text>
      </view>
      <view v-else-if="!todo" class="ai-page__todo-hint">暂无待办摘要</view>
      <template v-else>
        <view class="ai-page__todo-stats">
          <view class="ai-page__todo-stat">
            <text class="ai-page__todo-num">{{ todo.totalPending }}</text>
            <text class="ai-page__todo-label">待办</text>
          </view>
          <view class="ai-page__todo-stat">
            <text class="ai-page__todo-num ai-page__todo-num--danger">{{ todo.overdue }}</text>
            <text class="ai-page__todo-label">已超时</text>
          </view>
          <view class="ai-page__todo-stat">
            <text class="ai-page__todo-num">{{ todo.dueToday }}</text>
            <text class="ai-page__todo-label">今日到期</text>
          </view>
        </view>

        <view v-if="todo.byBizType.length > 0" class="ai-page__todo-tags">
          <text
            v-for="item in todo.byBizType"
            :key="item.bizType"
            class="ai-page__todo-tag"
          >
            {{ item.label }} {{ item.count }}
          </text>
        </view>

        <view v-if="todoExpanded" class="ai-page__todo-detail">
          <view v-if="todo.upcoming.length === 0" class="ai-page__todo-hint">暂无最近到期任务</view>
          <view
            v-for="item in todo.upcoming"
            :key="item.taskId"
            class="ai-page__todo-item"
          >
            <text class="ai-page__todo-item-title">
              {{ item.label }}（任务#{{ item.taskId }}）
            </text>
            <text
              :class="[
                'ai-page__todo-item-meta',
                item.overdue ? 'ai-page__todo-item-meta--danger' : ''
              ]"
            >
              {{ item.initiatorName ? '发起人 ' + item.initiatorName + ' · ' : ''
              }}{{ formatTodoDue(item.dueAt) }}{{ item.overdue ? '（已超时）' : '' }}
            </text>
          </view>
          <text class="ai-page__todo-content">{{ todo.content }}</text>
          <text class="ai-page__todo-disclaimer">{{ todo.disclaimer }}</text>
        </view>
      </template>
    </view>

    <!-- 会话工具栏：新会话入口 -->
    <view class="ai-page__toolbar">
      <text class="ai-page__session">
        {{ conversationId ? '当前会话追问中' : '新会话' }}
      </text>
      <button class="ai-page__new" :disabled="sending" @click="startNewConversation">
        新会话
      </button>
    </view>

    <!-- 服务不可用提示 -->
    <view v-if="statusChecked && !aiReady" class="ai-page__unavailable">
      <text class="ai-page__unavailable-text">AI 服务暂不可用</text>
    </view>

    <!-- 对话区 -->
    <view class="ai-page__chat">
      <view v-if="messages.length === 0" class="ai-page__empty">
        <text class="ai-page__empty-title">用自然语言查账</text>
        <text class="ai-page__empty-desc">
          可查询账目明细，也可让助手解释待办事项；仅支持只读查询。
        </text>
      </view>

      <view
        v-for="item in messages"
        :key="item.id"
        :class="[
          'ai-page__row',
          item.role === 'user' ? 'ai-page__row--user' : 'ai-page__row--assistant'
        ]"
      >
        <view
          :class="[
            'ai-page__bubble',
            item.role === 'user' ? 'ai-page__bubble--user' : 'ai-page__bubble--assistant'
          ]"
        >
          <text class="ai-page__content">{{ item.content }}</text>

          <template v-if="item.role === 'assistant'">
            <!-- 工具调用：折叠式提示 -->
            <view v-if="item.toolCalls && item.toolCalls.length > 0" class="ai-page__tools">
              <view class="ai-page__tools-head" @click="toggleTools(item.id)">
                <text class="ai-page__tools-summary">
                  已查询：{{ toolSummary(item.toolCalls) }}
                </text>
                <text class="ai-page__tools-toggle">
                  {{ toolsOpen[item.id] ? '收起' : '展开' }}
                </text>
              </view>
              <view v-if="toolsOpen[item.id]" class="ai-page__tools-body">
                <view
                  v-for="(tool, index) in item.toolCalls || []"
                  :key="index"
                  class="ai-page__tool"
                >
                  <text class="ai-page__tool-name">{{ tool.name }}（{{ tool.rows }} 行）</text>
                  <text class="ai-page__tool-args">{{ toolArgsText(tool.args) }}</text>
                </view>
              </view>
            </view>

            <text v-if="metaText(item)" class="ai-page__meta">{{ metaText(item) }}</text>
          </template>
        </view>
      </view>
    </view>

    <!-- 输入区 -->
    <view class="ai-page__input">
      <textarea
        v-model="input"
        class="ai-page__textarea"
        :placeholder="inputPlaceholder"
        :disabled="!aiReady || sending"
        :maxlength="200"
        :auto-height="true"
      />
      <button
        class="ai-page__send"
        :disabled="!aiReady || sending || input.trim().length === 0"
        @click="send"
      >
        {{ sending ? '查询中…' : '发送' }}
      </button>
    </view>
  </view>
</template>

<style scoped>
.ai-page {
  min-height: 100vh;
  padding: var(--ceams-space-lg) var(--ceams-space-xl) var(--ceams-space-2xl);
  background-color: var(--ceams-bg-page);
  box-sizing: border-box;
}

/* ---------------- 顶部提示条 ---------------- */
.ai-page__notice {
  padding: var(--ceams-space-sm) var(--ceams-space-md);
  background-color: var(--ceams-color-warning-tint);
  border-radius: var(--ceams-radius-sm);
  box-sizing: border-box;
}

.ai-page__notice-text {
  color: var(--ceams-color-warning-deep);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

/* ---------------- 待办摘要卡片 ---------------- */
.ai-page__todo {
  margin-top: var(--ceams-space-md);
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-md);
  box-shadow: var(--ceams-shadow-card);
  box-sizing: border-box;
}

.ai-page__todo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-page__todo-title {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.ai-page__todo-toggle {
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.ai-page__todo-hint {
  display: block;
  margin-top: var(--ceams-space-sm);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.ai-page__todo-hint--error {
  color: var(--ceams-color-danger-deep);
}

.ai-page__todo-retry {
  margin-left: var(--ceams-space-sm);
  color: var(--ceams-color-primary);
}

.ai-page__todo-stats {
  display: flex;
  margin-top: var(--ceams-space-sm);
}

.ai-page__todo-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.ai-page__todo-num {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.ai-page__todo-num--danger {
  color: var(--ceams-color-danger-deep);
}

.ai-page__todo-label {
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.ai-page__todo-tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: var(--ceams-space-sm);
}

.ai-page__todo-tag {
  margin: 0 var(--ceams-space-xs) var(--ceams-space-xs) 0;
  padding: var(--ceams-space-2xs) var(--ceams-space-sm);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-pill);
}

.ai-page__todo-detail {
  margin-top: var(--ceams-space-sm);
  padding-top: var(--ceams-space-sm);
  border-top: var(--ceams-border-width) solid var(--ceams-border);
}

.ai-page__todo-item {
  display: flex;
  flex-direction: column;
  padding: var(--ceams-space-xs) 0;
}

.ai-page__todo-item-title {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.ai-page__todo-item-meta {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.ai-page__todo-item-meta--danger {
  color: var(--ceams-color-danger-deep);
}

.ai-page__todo-content {
  display: block;
  margin-top: var(--ceams-space-sm);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-loose);
  white-space: pre-wrap;
  word-break: break-all;
}

.ai-page__todo-disclaimer {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

/* ---------------- 会话工具栏 ---------------- */
.ai-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--ceams-space-md);
  margin-bottom: var(--ceams-space-md);
}

.ai-page__session {
  flex: 1;
  min-width: 0;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.ai-page__new {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  height: var(--ceams-control-height-sm);
  margin: 0 0 0 var(--ceams-space-md);
  padding: 0 var(--ceams-space-md);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-caption);
  font-weight: var(--ceams-font-weight-medium);
  line-height: var(--ceams-line-height-tight);
  background-color: var(--ceams-bg-card);
  border: var(--ceams-border-width) solid var(--ceams-color-primary);
  border-radius: var(--ceams-radius-pill);
  box-sizing: border-box;
}

.ai-page__new[disabled] {
  opacity: 0.6;
}

/* ---------------- 服务不可用 ---------------- */
.ai-page__unavailable {
  margin-bottom: var(--ceams-space-md);
  padding: var(--ceams-space-sm) var(--ceams-space-md);
  background-color: var(--ceams-color-danger-tint);
  border-radius: var(--ceams-radius-sm);
  box-sizing: border-box;
}

.ai-page__unavailable-text {
  color: var(--ceams-color-danger-deep);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

/* ---------------- 对话区 ---------------- */
.ai-page__chat {
  display: flex;
  flex-direction: column;
}

.ai-page__empty {
  padding: var(--ceams-space-3xl) var(--ceams-space-md);
  text-align: center;
}

.ai-page__empty-title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.ai-page__empty-desc {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.ai-page__row {
  display: flex;
  margin-bottom: var(--ceams-space-md);
}

.ai-page__row--user {
  justify-content: flex-end;
}

.ai-page__row--assistant {
  justify-content: flex-start;
}

.ai-page__bubble {
  max-width: 78%;
  padding: var(--ceams-space-sm) var(--ceams-space-md);
  border-radius: var(--ceams-radius-md);
  box-sizing: border-box;
}

.ai-page__bubble--assistant {
  background-color: var(--ceams-bg-card);
  box-shadow: var(--ceams-shadow-card);
}

.ai-page__bubble--user {
  background-color: var(--ceams-color-primary);
}

.ai-page__content {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  line-height: var(--ceams-line-height-loose);
  word-break: break-all;
  white-space: pre-wrap;
}

.ai-page__bubble--user .ai-page__content {
  color: var(--ceams-text-inverse);
}

/* ---------------- 工具调用提示 ---------------- */
.ai-page__tools {
  margin-top: var(--ceams-space-sm);
  padding: var(--ceams-space-xs) var(--ceams-space-sm);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-sm);
}

.ai-page__tools-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-page__tools-summary {
  flex: 1;
  min-width: 0;
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
  word-break: break-all;
}

.ai-page__tools-toggle {
  flex-shrink: 0;
  margin-left: var(--ceams-space-sm);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.ai-page__tools-body {
  margin-top: var(--ceams-space-xs);
}

.ai-page__tool {
  padding: var(--ceams-space-2xs) 0;
}

.ai-page__tool-name {
  display: block;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.ai-page__tool-args {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
  word-break: break-all;
}

/* ---------------- 元信息 ---------------- */
.ai-page__meta {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

/* ---------------- 输入区 ---------------- */
.ai-page__input {
  display: flex;
  align-items: flex-end;
  margin-top: var(--ceams-space-md);
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-lg);
  box-shadow: var(--ceams-shadow-card);
  box-sizing: border-box;
}

.ai-page__textarea {
  flex: 1;
  min-width: 0;
  min-height: var(--ceams-control-height-sm);
  padding: var(--ceams-space-xs) var(--ceams-space-sm);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  line-height: var(--ceams-line-height-normal);
  background-color: var(--ceams-bg-subtle);
  border: var(--ceams-border-width) solid var(--ceams-border);
  border-radius: var(--ceams-radius-md);
  box-sizing: border-box;
}

.ai-page__send {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  height: var(--ceams-control-height-sm);
  margin: 0 0 0 var(--ceams-space-md);
  padding: 0 var(--ceams-space-lg);
  color: var(--ceams-text-inverse);
  font-size: var(--ceams-font-size-caption);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
  background-color: var(--ceams-color-primary);
  border-radius: var(--ceams-radius-md);
  box-sizing: border-box;
}

.ai-page__send[disabled] {
  opacity: 0.6;
}
</style>
