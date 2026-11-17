<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ApiError } from '@ceams/api-client'
import type { MeetingAvailabilityVO, MeetingRoomVO } from '@ceams/types'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'

interface ValueEvent {
  detail?: { value?: string }
}

function eventValue(event: unknown): string {
  const detail = (event as ValueEvent).detail
  return detail && typeof detail.value === 'string' ? detail.value : ''
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

function timeText(value: string): string {
  return value.split('T')[1]?.slice(0, 5) ?? value
}

const loading = ref(false)
const submitting = ref(false)
const rooms = ref<MeetingRoomVO[]>([])
const activeRoom = ref<MeetingRoomVO | null>(null)
const subject = ref('')
const date = ref(today())
const startTime = ref('09:00')
const endTime = ref('10:00')
const availability = ref<MeetingAvailabilityVO | null>(null)

async function loadRooms(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    const res = await apiClient.admin.meetingRoomList()
    rooms.value = res.filter((item) => item.status === 'ENABLED')
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载会议室失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function loadAvailability(): Promise<void> {
  if (!activeRoom.value || !date.value) {
    availability.value = null
    return
  }
  try {
    availability.value = await apiClient.admin.meetingAvailability(activeRoom.value.id, date.value)
  } catch (err) {
    availability.value = null
    uni.showToast({ title: apiErrorMessage(err, '加载占用情况失败'), icon: 'none' })
  }
}

function selectRoom(room: MeetingRoomVO): void {
  activeRoom.value = room
  void loadAvailability()
}

function onDateChange(event: unknown): void {
  date.value = eventValue(event)
  void loadAvailability()
}

async function submit(): Promise<void> {
  if (!activeRoom.value) {
    uni.showToast({ title: '请先选择会议室', icon: 'none' })
    return
  }
  if (!subject.value.trim()) {
    uni.showToast({ title: '请输入会议主题', icon: 'none' })
    return
  }
  if (!date.value) {
    uni.showToast({ title: '请选择会议日期', icon: 'none' })
    return
  }
  if (startTime.value >= endTime.value) {
    uni.showToast({ title: '结束时间必须晚于开始时间', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await apiClient.admin.meetingApply({
      roomId: activeRoom.value.id,
      subject: subject.value.trim(),
      startTime: `${date.value}T${startTime.value}:00`,
      endTime: `${date.value}T${endTime.value}:00`
    })
    uni.showToast({ title: '预定申请已提交', icon: 'success' })
    subject.value = ''
    await loadAvailability()
  } catch (err) {
    if (err instanceof ApiError && (err.status === 409 || err.code === 409001)) {
      void loadAvailability()
      uni.showModal({
        title: '时段冲突',
        content: '该会议室在所选时段已被占用，请调整时间后重试。',
        showCancel: false
      })
      return
    }
    uni.showToast({ title: apiErrorMessage(err, '提交预定失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onShow(() => {
  void loadRooms()
})
</script>

<template>
  <view class="ceams-page">
    <view v-if="loading && rooms.length === 0" class="state">加载中...</view>
    <view v-if="!loading && rooms.length === 0" class="state">暂无可预定的会议室</view>

    <view class="section-title">会议室</view>
    <view
      v-for="item in rooms"
      :key="item.id"
      class="ceams-card room"
      :class="{ 'room--active': activeRoom?.id === item.id }"
      @click="selectRoom(item)"
    >
      <view class="room__head">
        <text class="room__name">{{ item.name }}</text>
        <text class="room__capacity">可容纳 {{ item.capacity ?? '-' }} 人</text>
      </view>
      <text class="room__meta">位置 {{ item.location || '-' }}</text>
      <text v-if="item.equipment" class="room__meta">设备 {{ item.equipment }}</text>
    </view>

    <view v-if="activeRoom" class="ceams-card form">
      <text class="form__title">预定：{{ activeRoom.name }}</text>

      <view class="field">
        <text class="field__label">会议日期</text>
        <picker mode="date" :value="date" @change="onDateChange($event)">
          <view class="field__value">{{ date || '请选择' }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="field__label">开始时间</text>
        <picker mode="time" :value="startTime" @change="startTime = eventValue($event)">
          <view class="field__value">{{ startTime }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="field__label">结束时间</text>
        <picker mode="time" :value="endTime" @change="endTime = eventValue($event)">
          <view class="field__value">{{ endTime }}</view>
        </picker>
      </view>

      <view class="field field--column">
        <text class="field__label">会议主题</text>
        <input
          class="field__input"
          :value="subject"
          placeholder="请输入会议主题"
          @input="subject = eventValue($event)"
        />
      </view>

      <view class="occupied">
        <text class="occupied__title">当日占用</text>
        <text v-if="!availability || availability.intervals.length === 0" class="occupied__empty">
          当日暂无占用
        </text>
        <view v-for="(item, index) in availability?.intervals ?? []" :key="index" class="occupied__item">
          <text class="occupied__time">{{ timeText(item.startTime) }} - {{ timeText(item.endTime) }}</text>
          <text class="occupied__subject">{{ item.subject || '已占用' }}</text>
        </view>
      </view>

      <button class="ceams-btn ceams-btn--primary" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中...' : '提交预定申请' }}
      </button>
      <text class="form__hint">提交后进入审批流程，可在待办中心查看进度</text>
    </view>
  </view>
</template>

<style scoped>
.state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}

.section-title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.room {
  margin-bottom: var(--ceams-space-md);
}

.room--active {
  border: var(--ceams-border-width) solid var(--ceams-color-primary);
}

.room__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.room__name {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.room__capacity {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.room__meta {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.form {
  margin-top: var(--ceams-space-lg);
}

.form__title {
  display: block;
  margin-bottom: var(--ceams-space-sm);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ceams-space-sm) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.field--column {
  flex-direction: column;
  align-items: flex-start;
}

.field__label {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.field__value {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

.field__input {
  width: 100%;
  margin-top: var(--ceams-space-xs);
  padding: var(--ceams-space-sm);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  box-sizing: border-box;
}

.occupied {
  padding: var(--ceams-space-md) 0;
}

.occupied__title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.occupied__empty {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
}

.occupied__item {
  display: flex;
  justify-content: space-between;
  padding: var(--ceams-space-2xs) 0;
}

.occupied__time {
  color: #dc2626;
  font-size: var(--ceams-font-size-mini);
}

.occupied__subject {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.form__hint {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}
</style>
