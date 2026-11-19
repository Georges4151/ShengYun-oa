<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { VehicleVO } from '@ceams/types'
import { apiClient } from '../../platform/http'
import SchemaForm from '../../components/schema-form/schema-form.vue'
import type { FormModel } from '../../schemas/form-schema'
import { createFormModel, getFormSchema } from '../../schemas/form-schema'
import { apiErrorMessage } from '../../utils/error'
import { vehicleStatusLabels } from '../../utils/labels'

/** 用车申请：车辆选择器留在页面级（需状态过滤），日期/时间/目的地/事由由 schema 渲染 */
const schema = getFormSchema('ADMIN_VEHICLE_APPLY')
if (!schema) {
  throw new Error('缺少 ADMIN_VEHICLE_APPLY 表单 schema')
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

const loading = ref(false)
const submitting = ref(false)
const vehicles = ref<VehicleVO[]>([])
const activeVehicle = ref<VehicleVO | null>(null)
const form = ref<FormModel>(defaultForm())
const formRef = ref<InstanceType<typeof SchemaForm> | null>(null)

function text(key: string): string {
  return String(form.value[key] ?? '').trim()
}

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
  form.value = defaultForm()
}

async function submit(): Promise<void> {
  const vehicle = activeVehicle.value
  if (!vehicle) {
    uni.showToast({ title: '请先选择车辆', icon: 'none' })
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
    await apiClient.admin.vehicleApply({
      vehicleId: vehicle.id,
      destination: text('destination'),
      purpose: text('purpose'),
      startTime: start,
      endTime: end
    })
    uni.showToast({ title: '已提交审批', icon: 'success' })
    activeVehicle.value = null
    form.value = defaultForm()
    setTimeout(() => uni.navigateTo({ url: '/pages/bpm/todo?tab=initiated' }), 500)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '提交失败'), icon: 'none' })
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

    <view v-if="activeVehicle" class="form">
      <text class="form__title">用车申请：{{ activeVehicle.plateNo }}</text>
      <SchemaForm ref="formRef" v-model="form" :schema="schema" :disabled="submitting" />

      <button class="ceams-btn ceams-btn--primary form__submit" :disabled="submitting" @click="submit">
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
