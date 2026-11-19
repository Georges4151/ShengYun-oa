<script setup lang="ts">
import { ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ApiError } from '@ceams/api-client'
import type { MeetingAvailabilityVO, MeetingRoomVO } from '@ceams/types'
import { apiClient } from '../../platform/http'
import SchemaForm from '../../components/schema-form/schema-form.vue'
import type { FormModel } from '../../schemas/form-schema'
import { createFormModel, getFormSchema } from '../../schemas/form-schema'
import { apiErrorMessage } from '../../utils/error'

/** 会议室预定：会议室选择器与时段占用查询保留在页面级，其余字段由 schema 渲染 */
const schema = getFormSchema('ADMIN_MEETING_APPLY')
if (!schema) {
  throw new Error('缺少 ADMIN_MEETING_APPLY 表单 schema')
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

function defaultForm(): FormModel {
  return {
    ...createFormModel(schema!),
    useDate: today(),
    startClock: '09:00',
    endClock: '10:00'
  }
}

function timeText(value: string): string {
  return value.split('T')[1]?.slice(0, 5) ?? value
}

const loading = ref(false)
const submitting = ref(false)
const rooms = ref<MeetingRoomVO[]>([])
const activeRoom = ref<MeetingRoomVO | null>(null)
const form = ref<FormModel>(defaultForm())
const formRef = ref<InstanceType<typeof SchemaForm> | null>(null)
const availability = ref<MeetingAvailabilityVO | null>(null)

function text(key: string): string {
  return String(form.value[key] ?? '').trim()
}

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
  if (!activeRoom.value || !text('useDate')) {
    availability.value = null
    return
  }
  try {
    availability.value = await apiClient.admin.meetingAvailability(activeRoom.value.id, text('useDate'))
  } catch (err) {
    availability.value = null
    uni.showToast({ title: apiErrorMessage(err, '加载占用情况失败'), icon: 'none' })
  }
}

function selectRoom(room: MeetingRoomVO): void {
  activeRoom.value = room
  void loadAvailability()
}

/** 日期由 SchemaForm 更新，需同步刷新当日占用 */
watch(
  () => form.value.useDate,
  () => {
    void loadAvailability()
  }
)

async function submit(): Promise<void> {
  if (!activeRoom.value) {
    uni.showToast({ title: '请先选择会议室', icon: 'none' })
    return
  }
  const error = formRef.value?.validate()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }
  const start = `${text('useDate')}T${text('startClock')}:00`
  const end = `${text('useDate')}T${text('endClock')}:00`
  if (start >= end) {
    uni.showToast({ title: '结束时间必须晚于开始时间', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await apiClient.admin.meetingApply({
      roomId: activeRoom.value.id,
      subject: text('subject'),
      startTime: start,
      endTime: end
    })
    uni.showToast({ title: '已提交审批', icon: 'success' })
    form.value = defaultForm()
    setTimeout(() => uni.navigateTo({ url: '/pages/bpm/todo?tab=initiated' }), 500)
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
    uni.showToast({ title: apiErrorMessage(err, '提交失败'), icon: 'none' })
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

    <view v-if="activeRoom" class="form">
      <text class="form__title">预定：{{ activeRoom.name }}</text>
      <SchemaForm ref="formRef" v-model="form" :schema="schema" :disabled="submitting" />

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

      <button class="ceams-btn ceams-btn--primary form__submit" :disabled="submitting" @click="submit">
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

.form__submit {
  margin-top: var(--ceams-space-lg);
}

.form__hint {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}
</style>
