<script setup lang="ts">
import { ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import type { UserNotificationVO } from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { pushChannelLabels, pushStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

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
  <view class="page">
    <view class="toolbar">
      <text class="toolbar__count">未读 {{ unreadCount }} 条</text>
      <button class="toolbar__btn" @click="markAllRead">全部已读</button>
    </view>

    <view v-if="records.length === 0" class="empty">暂无通知</view>
    <view v-for="item in records" :key="item.id" class="card">
      <view class="card__row">
        <text class="card__channel">{{ pushChannelLabels[item.channel ?? 'SYSTEM'] }}</text>
        <text v-if="item.isRead === 0" class="card__badge">未读</text>
      </view>
      <text class="card__line">通知 ID：{{ item.notificationId }}</text>
      <text class="card__line">发送状态：{{ pushStatusLabels[item.pushStatus] }}</text>
      <text class="card__line">时间：{{ formatDateTime(item.pushTime ?? item.createTime) }}</text>
      <view v-if="item.isRead === 0" class="card__action" @click="markRead(item.id)">标记已读</view>
    </view>
  </view>
</template>

<style scoped>
.page {
  padding: 24rpx 32rpx;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}
.toolbar__count {
  font-size: 26rpx;
  color: #6b7280;
}
.toolbar__btn {
  font-size: 26rpx;
  color: #2563eb;
  background-color: transparent;
  padding: 0 8rpx;
  line-height: 1;
}
.toolbar__btn::after {
  border: none;
}
.card {
  padding: 28rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}
.card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.card__channel {
  font-size: 30rpx;
  font-weight: 600;
}
.card__badge {
  font-size: 22rpx;
  color: #dc2626;
  background-color: #fef2f2;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
}
.card__line {
  display: block;
  font-size: 26rpx;
  color: #6b7280;
  margin-top: 6rpx;
}
.card__action {
  margin-top: 16rpx;
  text-align: right;
  font-size: 26rpx;
  color: #2563eb;
}
.empty {
  text-align: center;
  color: #9ca3af;
  padding: 80rpx 0;
}
</style>
