<script setup lang="ts">
import { ref } from 'vue'
import type { PaymentMethod, ReceiptType } from '@ceams/types'
import { apiClient } from '../../platform/http'
import SchemaForm from '../../components/schema-form/schema-form.vue'
import type { FormModel } from '../../schemas/form-schema'
import { createFormModel, getFormSchema } from '../../schemas/form-schema'
import { apiErrorMessage } from '../../utils/error'

const schema = getFormSchema('EXPENSE')
if (!schema) {
  throw new Error('缺少 EXPENSE 表单 schema')
}

const form = ref<FormModel>({
  ...createFormModel(schema),
  receiptDate: new Date().toISOString().slice(0, 10)
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
  submitting.value = true
  try {
    // 报销域按票据维度建模，一行票据对应一条 receipt
    const created = await apiClient.expense.receiptCreate({
      receiptNumber: text('receiptNumber'),
      amount: text('amount'),
      type: text('type') as ReceiptType,
      receiptDate: `${text('receiptDate')}T00:00:00`,
      reporterName: optional('reporterName'),
      projectId: optional('projectId'),
      description: optional('description'),
      paymentMethod: optional('paymentMethod') as PaymentMethod | undefined,
      filePath: optional('filePath')
    })
    uni.showToast({ title: '已创建，可提交审批', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/expense/detail?id=${created}` })
    }, 600)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '创建失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="ceams-page">
    <SchemaForm ref="formRef" v-model="form" :schema="schema" :disabled="submitting" />

    <view class="submit">
      <button class="ceams-btn ceams-btn--primary" :disabled="submitting" @click="submit">
        保存为草稿
      </button>
      <text class="submit__hint">保存后可在详情页点击"提交审批"发起流程</text>
    </view>
  </view>
</template>

<style scoped>
.submit {
  margin-top: var(--ceams-space-lg);
}

.submit__hint {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}
</style>
