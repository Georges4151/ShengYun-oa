<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { VehicleVO } from '@ceams/types'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'
import { vehicleStatusLabels } from '../../utils/labels'

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

const loading = ref(false)
const submitting = ref(false)
const vehicles = ref<VehicleVO[]>([])
const activeVehicle = ref<VehicleVO | null>(null)
const purpose = ref('')
const destination = ref('')
const date = ref(today())
const startTime = ref('09:00')
const endTime = ref('10:00')

async function load(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    const res = await apiClient.admin.vehiclePage({ page: 1, size: 50, status: 'IDLE' })
    vehicles.value = res.records
    if (activeVehicle.value && !vehicles.value.some((item) => item.id === activeVehicle.value?.id)) {
      activeVehicle.value = null
    }
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载可用车辆失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function selectVehicle(vehicle: VehicleVO): void {
  activeVehicle.value = vehicle
}

async function submit(): Promise<void> {
  const vehicle = activeVehicle.value
  if (!vehicle) {
    uni.showToast({ title: '请先选择车辆', icon: 'none' })
    return
  }
  if (!date.value) {
    uni.showToast({ title: '请选择用车日期', icon: 'none' })
    return
  }
  const start = `${date.value}T${startTime.value}:00`
  const end = `${date.value}T${endTime.value}:00`
  if (start >= end) {
    uni.showToast({ title: '结束时间必须晚于开始时间', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await apiClient.admin.vehicleApply({
      vehicleId: vehicle.id,
      purpose: purpose.value.trim() || undefined,
      destination: destination.value.trim() || undefined,
      startTime: start,
      endTime: end
    })
    uni.showToast({ title: '用车申请已提交', icon: 'success' })
    activeVehicle.value = null
    purpose.value = ''
    destination.value = ''
    await load()
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '提交用车申请失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onShow(() => {
  void load()
})
</script>

<template>
  <view class="ceams-page">
    <view v-if="loading && vehicles.length === 0" class="state">加载中...</view>
    <view v-if="!loading && vehicles.length === 0" class="state">暂无可用的空闲车辆</view>

    <view class="section-title">可用车辆</view>
    <view
      v-for="item in vehicles"
      :key="item.id"
      class="ceams-card vehicle"
      :class="{ 'vehicle--active': activeVehicle?.id === item.id }"
      @click="selectVehicle(item)"
    >
      <view class="vehicle__head">
        <text class="vehicle__plate">{{ item.plateNo }}</text>
        <text class="vehicle__status">{{ vehicleStatusLabels[item.status] }}</text>
      </view>
      <text class="vehicle__meta">车型 {{ item.brandModel || '-' }} · 座位 {{ item.seats ?? '-' }}</text>
      <text v-if="item.insuranceExpire" class="vehicle__meta">保险到期 {{ item.insuranceExpire }}</text>
    </view>

    <view v-if="activeVehicle" class="ceams-card form">
      <text class="form__title">用车申请：{{ activeVehicle.plateNo }}</text>

      <view class="field">
        <text class="field__label">用车日期</text>
        <picker mode="date" :value="date" @change="date = eventValue($event)">
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

      <view class="field">
        <text class="field__label">目的地</text>
        <input
          class="field__input"
          :value="destination"
          placeholder="目的地（可选）"
          @input="destination = eventValue($event)"
        />
      </view>

      <view class="field field--column">
        <text class="field__label">用车事由</text>
        <textarea
          class="field__textarea"
          :value="purpose"
          placeholder="用车事由（可选）"
          maxlength="200"
          @input="purpose = eventValue($event)"
        />
      </view>

      <button class="ceams-btn ceams-btn--primary" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中...' : '提交用车申请' }}
      </button>
      <text class="form__hint">提交后进入审批流程，审批通过后可按时用车</text>
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

.vehicle {
  margin-bottom: var(--ceams-space-md);
}

.vehicle--active {
  border: var(--ceams-border-width) solid var(--ceams-color-primary);
}

.vehicle__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.vehicle__plate {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.vehicle__status {
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-mini);
}

.vehicle__meta {
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
  flex: 1;
  margin-left: var(--ceams-space-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  text-align: right;
}

.field__textarea {
  width: 100%;
  min-height: 120px;
  margin-top: var(--ceams-space-xs);
  padding: var(--ceams-space-sm);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  box-sizing: border-box;
}

.form__hint {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}
</style>
