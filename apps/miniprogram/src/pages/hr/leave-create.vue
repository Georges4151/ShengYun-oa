<script setup lang="ts">
import { ref } from 'vue'
import type { HrLeaveType } from '@ceams/types'
import { formatDate } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import SchemaForm from '../../components/schema-form/schema-form.vue'
import type { FormModel } from '../../schemas/form-schema'
import { createFormModel, getFormSchema } from '../../schemas/form-schema'
import { apiErrorMessage } from '../../utils/error'

const schema = getFormSchema('HR_LEAVE')
if (!schema) {
  throw new Error('缺少 HR_LEAVE 表单 schema')
}

const today = formatDate(new Date())
const form = ref<FormModel>({
  ...createFormModel(schema),
  startDate: today,
  endDate: today
})
const formRef = ref<InstanceType<typeof SchemaForm> | null>(null)
const submitting = ref(false)

function text(key: string): string {
  return String(form.value[key] ?? '').trim()
}

function optional(key: string): string | undefined {
  const value = text(key)
  return value || undefined
}

async function submit(): Promise<void> {
  const error = formRef.value?.validate()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }
  if (text('endDate') < text('startDate')) {
    uni.showToast({ title: '结束日期不能早于开始日期', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await apiClient.hr.createLeaveRequest({
      leaveType: text('leaveType') as HrLeaveType,
      startDate: text('startDate'),
      endDate: text('endDate'),
      days: text('days'),
      reason: optional('reason')
    })
    uni.showToast({ title: '已提交审批', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '提交失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="ceams-page">
    <SchemaForm ref="formRef" v-model="form" :schema="schema" :disabled="submitting" />

    <view class="hint">
      <text class="hint__text">提交后进入请假审批流程，审批通过后扣减对应假期额度。</text>
    </view>

    <view class="submit">
      <button class="ceams-btn ceams-btn--primary" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中...' : '提交请假申请' }}
      </button>
    </view>
  </view>
</template>

<style scoped>
.hint {
  margin-top: var(--ceams-space-md);
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
}

.hint__text {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.submit {
  margin-top: var(--ceams-space-lg);
}
</style>
