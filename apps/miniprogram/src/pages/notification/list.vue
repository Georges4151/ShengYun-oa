<script setup lang="ts">
import { ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import type { PushChannel, UserNotificationVO } from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { pushChannelLabels, pushStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

/** 渠道 -> 几何字符标识（不使用 emoji / 图标库） */
const channelMarks: Record<PushChannel, string> = {
  SYSTEM: '◈',
  EMAIL: '@',
  SMS: '≡'
}

const records = ref<UserNotificationVO[]>([])
const page = ref(1)
const total = ref(0)
const unreadCount = ref(0)
const size = 20
const loading = ref(false)

async function load(reset = false): Promise<void> {
  if (reset) page.value = 1
  if (loading.value) return
  loading.value = true
  try {
    const res = await apiClient.notification.userNotificationMine({ page: page.value, size })
    records.value = reset ? res.records : records.value.concat(res.records)
    total.value = res.total
    unreadCount.value = res.unreadCount
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载通知失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
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

/** 标题：优先使用通知内容快照，旧数据回退到渠道通知名 */
function displayTitle(item: UserNotificationVO): string {
  return item.title || `${pushChannelLabels[item.channel ?? 'SYSTEM']}通知`
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
    <view class="notification-list__toolbar">
      <view class="notification-list__toolbar-info">
        <text class="notification-list__toolbar-label">未读</text>
        <text class="notification-list__toolbar-count">{{ unreadCount }}</text>
        <text class="notification-list__toolbar-unit">条</text>
      </view>
      <button class="notification-list__readall" @click="markAllRead">全部已读</button>
    </view>

    <view v-if="records.length === 0" class="ceams-card">
      <empty-state title="暂无通知" description="新的通知会出现在这里" icon="◈" />
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
            @click="markRead(item.id)"
          >
            标记已读
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
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
}

.notification-list__item {
  position: relative;
  display: flex;
  align-items: flex-start;
  padding-left: var(--ceams-space-lg);
}

.notification-list__bar {
  position: absolute;
  top: var(--ceams-space-md);
  bottom: var(--ceams-space-md);
  left: 0;
  width: var(--ceams-space-2xs);
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
