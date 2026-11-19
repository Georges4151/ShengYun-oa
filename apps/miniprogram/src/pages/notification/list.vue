<script setup lang="ts">
import { ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import type { NotificationType, PushChannel, UserNotificationPageQuery, UserNotificationVO } from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { notificationTypeLabels, pushChannelLabels, pushStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

/** 渠道 -> 几何字符标识（不使用 emoji / 图标库） */
const channelMarks: Record<PushChannel, string> = {
  SYSTEM: '◈',
  EMAIL: '@',
  SMS: '≡'
}

/** 通知类型筛选项的值：空字符串表示「全部」，其余与后端 NotificationType 枚举名一致 */
type NotificationFilterValue = NotificationType | ''

/** 顶部横向 chips：仅暴露业务域（审批 / 人力 / 行政 / 文档），系统与公告归入「全部」 */
const filterOptions: Array<{ value: NotificationFilterValue; label: string }> = [
  { value: '', label: '全部' },
  { value: 'APPROVAL', label: '审批' },
  { value: 'HR', label: '人力' },
  { value: 'ADMIN', label: '行政' },
  { value: 'DOC', label: '文档' }
]

const records = ref<UserNotificationVO[]>([])
const page = ref(1)
const total = ref(0)
const unreadCount = ref(0)
const size = 20
const loading = ref(false)
/** 当前选中的通知类型过滤（空串 = 全部） */
const filterType = ref<NotificationFilterValue>('')
/** 请求序号：切换筛选后丢弃旧请求的响应，避免列表串数据 */
let requestSeq = 0

async function load(reset = false): Promise<void> {
  if (reset) page.value = 1
  const seq = ++requestSeq
  loading.value = true
  try {
    // type 为空时保持原有两参请求行为（不向后端传 type 查询参数）
    const query: UserNotificationPageQuery = { page: page.value, size }
    if (filterType.value) query.type = filterType.value
    const res = await apiClient.notification.userNotificationMine(query)
    if (seq !== requestSeq) return
    records.value = reset ? res.records : records.value.concat(res.records)
    total.value = res.total
    unreadCount.value = res.unreadCount
  } catch (err) {
    if (seq !== requestSeq) return
    uni.showToast({ title: apiErrorMessage(err, '加载通知失败'), icon: 'none' })
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

/** 切换类型筛选：重置分页与列表后按 type 重新查询 */
function onFilterChange(value: NotificationFilterValue): void {
  if (filterType.value === value) return
  filterType.value = value
  records.value = []
  total.value = 0
  void load(true)
}

async function markRead(id: string): Promise<void> {
  try {
    await apiClient.notification.read(id)
    await load(true)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '标记已读失败'), icon: 'none' })
  }
}

async function markAllRead(): Promise<void> {
  try {
    await apiClient.notification.readAll()
    await load(true)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '全部已读失败'), icon: 'none' })
  }
}

// ---------- 深链解析（小程序无现成工具，此处保持简单，不做通用路由框架） ----------

const DEEP_LINK_PREFIX = 'ceams://'

/** decodeURIComponent 的安全版本：非法转义时原样返回，避免解析抛错 */
function safeDecode(value: string): string {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

/** 解析查询串为键值表 */
function parseQuery(query: string): Record<string, string> {
  const result: Record<string, string> = {}
  if (!query) return result
  for (const pair of query.split('&')) {
    if (!pair) continue
    const eqIndex = pair.indexOf('=')
    const key = safeDecode(eqIndex === -1 ? pair : pair.slice(0, eqIndex))
    const value = eqIndex === -1 ? '' : safeDecode(pair.slice(eqIndex + 1))
    if (key) result[key] = value
  }
  return result
}

/**
 * 把 ceams:// 深链解析为小程序页面地址（含查询串）。
 * 支持的深链：
 * - ceams://hr/salary-slip(?month=yyyy-MM) → /pages/hr/salary-slip
 * - ceams://todo?tab=&taskId=&instanceId= → /pages/bpm/todo（页面由 P5 创建）
 * 其余返回 null，由调用方兜底提示「请前往 Web 端查看」。
 */
function resolveDeepLink(linkUrl: string): string | null {
  const link = linkUrl.trim()
  if (!link.toLowerCase().startsWith(DEEP_LINK_PREFIX)) return null
  const rest = link.slice(DEEP_LINK_PREFIX.length)
  if (!rest) return null

  const queryIndex = rest.indexOf('?')
  const rawPath = queryIndex === -1 ? rest : rest.slice(0, queryIndex)
  const query = queryIndex === -1 ? '' : rest.slice(queryIndex + 1)
  const path = rawPath.replace(/^\/+/, '').toLowerCase()

  if (path === 'hr/salary-slip') {
    const month = parseQuery(query).month
    return month
      ? `/pages/hr/salary-slip?month=${encodeURIComponent(month)}`
      : '/pages/hr/salary-slip'
  }

  if (path === 'todo') {
    const params = parseQuery(query)
    const parts = ['tab', 'taskId', 'instanceId']
      .filter((key) => params[key])
      .map((key) => `${key}=${encodeURIComponent(params[key])}`)
    return parts.length > 0 ? `/pages/bpm/todo?${parts.join('&')}` : '/pages/bpm/todo'
  }

  return null
}

/** 点击通知：能识别的深链直接跳转，未识别的仅标记已读并提示前往 Web 端 */
function openItem(item: UserNotificationVO): void {
  const url = resolveDeepLink(item.linkUrl ?? '')
  if (!url) {
    if (item.isRead === 0) void markRead(item.id)
    uni.showToast({ title: '请前往 Web 端查看', icon: 'none' })
    return
  }
  if (item.isRead === 0) void markRead(item.id)
  uni.navigateTo({
    url,
    fail: () => {
      // 目标页面尚未上线（如 P5 的 bpm/todo）时给出兜底提示，不报错中断
      uni.showToast({ title: '请前往 Web 端查看', icon: 'none' })
    }
  })
}

/** 标题：优先使用通知内容快照，旧数据回退到渠道通知名 */
function displayTitle(item: UserNotificationVO): string {
  return item.title || `${pushChannelLabels[item.channel ?? 'SYSTEM']}通知`
}

/** 通知类型标签；旧数据缺少 notificationType 时返回空串（不渲染标签） */
function typeLabel(item: UserNotificationVO): string {
  return item.notificationType ? notificationTypeLabels[item.notificationType] : ''
}

/** 正文：缺失时回退到旧的推送状态 + 通知编号展示，兼容历史数据 */
function displayContent(item: UserNotificationVO): string {
  if (item.content) return item.content
  return `${pushStatusLabels[item.pushStatus]} · 通知编号 ${item.notificationId}`
}

/** 时间：优先发布时间，旧数据回退创建时间 */
function displayTime(item: UserNotificationVO): string {
  return formatDateTime(item.publishTime ?? item.createTime)
}

onShow(() => {
  void load(true)
})

onReachBottom(() => {
  if (loading.value || records.value.length >= total.value) return
  page.value += 1
  void load()
})
</script>

<template>
  <view class="ceams-page notification-list">
    <view class="notification-list__filters">
      <view
        v-for="opt in filterOptions"
        :key="opt.value"
        class="notification-list__chip"
        :class="{ 'notification-list__chip--active': filterType === opt.value }"
        @click="onFilterChange(opt.value)"
      >
        {{ opt.label }}
      </view>
    </view>

    <view class="notification-list__toolbar">
      <view class="notification-list__toolbar-info">
        <text class="notification-list__toolbar-label">未读</text>
        <text class="notification-list__toolbar-count">{{ unreadCount }}</text>
        <text class="notification-list__toolbar-unit">条</text>
      </view>
      <button class="notification-list__readall" @click="markAllRead">全部已读</button>
    </view>

    <view v-if="records.length === 0" class="ceams-card">
      <empty-state title="暂无通知" description="新的通知会出现在这里" />
    </view>

    <view v-else class="ceams-card notification-list__panel">
      <view
        v-for="(item, index) in records"
        :key="item.id"
        :class="[
          'ceams-list-item',
          'notification-list__item',
          item.isRead === 0 ? 'notification-list__item--unread' : 'notification-list__item--read',
          index === records.length - 1 ? 'ceams-list-item--last' : ''
        ]"
        hover-class="notification-list__item--pressed"
        @click="openItem(item)"
      >
        <view v-if="item.isRead === 0" class="notification-list__bar" />
        <view class="notification-list__mark">
          {{ channelMarks[item.channel ?? 'SYSTEM'] }}
        </view>
        <view class="notification-list__body">
          <view class="notification-list__head">
            <text class="notification-list__title">
              {{ displayTitle(item) }}
            </text>
            <text v-if="typeLabel(item)" class="ceams-badge notification-list__type">
              {{ typeLabel(item) }}
            </text>
            <text class="notification-list__time">
              {{ displayTime(item) }}
            </text>
          </view>
          <text class="notification-list__summary">
            {{ displayContent(item) }}
          </text>
          <text
            v-if="item.isRead === 0"
            class="notification-list__action"
            @click.stop="markRead(item.id)"
          >
            标记已读
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.notification-list__filters {
  display: flex;
  align-items: center;
  margin-bottom: var(--ceams-space-md);
  overflow-x: auto;
  white-space: nowrap;
}

.notification-list__chip {
  flex-shrink: 0;
  margin-right: var(--ceams-space-xs);
  padding: var(--ceams-space-2xs) var(--ceams-space-md);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-tight);
  background-color: var(--ceams-bg-card);
  border: var(--ceams-border-width) solid var(--ceams-border);
  border-radius: var(--ceams-radius-pill);
  box-sizing: border-box;
}

.notification-list__chip--active {
  color: var(--ceams-color-primary);
  background-color: var(--ceams-color-primary-tint);
  border-color: var(--ceams-color-primary);
}

.notification-list__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-md);
}

.notification-list__toolbar-info {
  display: flex;
  align-items: center;
}

.notification-list__toolbar-label {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.notification-list__toolbar-count {
  margin: 0 var(--ceams-space-2xs);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  letter-spacing: var(--ceams-letter-spacing-number);
}

.notification-list__toolbar-unit {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.notification-list__readall {
  display: flex;
  align-items: center;
  justify-content: center;
  height: var(--ceams-control-height-sm);
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

.notification-list__panel {
  padding-top: 0;
  padding-bottom: 0;
  overflow: hidden;
}

.notification-list__item {
  position: relative;
  display: flex;
  align-items: flex-start;
  padding-left: var(--ceams-space-lg);
}

.notification-list__item--pressed {
  background-color: var(--ceams-bg-subtle);
}

/* 未读：浅蓝底 + 左侧 6rpx 主色竖条 */
.notification-list__item--unread {
  background-color: var(--ceams-color-primary-tint);
}

/* 已读：整体降低不透明度，弱化视觉层级 */
.notification-list__item--read {
  opacity: 0.62;
}

.notification-list__bar {
  position: absolute;
  top: var(--ceams-space-md);
  bottom: var(--ceams-space-md);
  left: 0;
  width: 6rpx;
  background-color: var(--ceams-color-primary);
  border-radius: var(--ceams-radius-pill);
}

.notification-list__mark {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--ceams-control-height-sm);
  height: var(--ceams-control-height-sm);
  margin-right: var(--ceams-space-md);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  background-color: var(--ceams-color-primary-tint);
  border-radius: var(--ceams-radius-pill);
}

.notification-list__item--unread .notification-list__mark {
  color: var(--ceams-text-inverse);
  background-color: var(--ceams-color-primary);
}

.notification-list__item--read .notification-list__mark {
  color: var(--ceams-color-neutral);
  background-color: var(--ceams-color-neutral-tint);
}

.notification-list__body {
  flex: 1;
  min-width: 0;
}

.notification-list__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notification-list__title {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-sm);
  overflow: hidden;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notification-list__time {
  flex-shrink: 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-tight);
}

/* 类型徽标沿用全局 .ceams-badge，仅补右间距 */
.notification-list__type {
  flex-shrink: 0;
  margin-right: var(--ceams-space-sm);
}

.notification-list__summary {
  display: -webkit-box;
  margin-top: var(--ceams-space-2xs);
  overflow: hidden;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
  white-space: pre-wrap;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notification-list__item--read .notification-list__title {
  color: var(--ceams-text-secondary);
  font-weight: var(--ceams-font-weight-regular);
}

.notification-list__item--read .notification-list__summary {
  color: var(--ceams-text-placeholder);
}

.notification-list__action {
  display: inline-block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-mini);
}
</style>
