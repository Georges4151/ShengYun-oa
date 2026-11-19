<script setup lang="ts">
import { ref } from 'vue'
import type { PaymentMethod, ReceiptOcrResult, ReceiptType } from '@ceams/types'
import { apiClient } from '../../platform/http'
import { getUserName } from '../../platform/auth'
import SchemaForm from '../../components/schema-form/schema-form.vue'
import type { FormModel } from '../../schemas/form-schema'
import { createFormModel, getFormSchema } from '../../schemas/form-schema'
import { apiErrorMessage } from '../../utils/error'

const RECEIPT_TYPES: readonly ReceiptType[] = [
  'TRANSPORTATION',
  'ACCOMMODATION',
  'MEAL',
  'OFFICE',
  'OTHER'
]
const PAYMENT_METHODS: readonly PaymentMethod[] = [
  'CASH',
  'BANK_TRANSFER',
  'WECHAT',
  'ALIPAY',
  'OTHER'
]

const schema = getFormSchema('EXPENSE')
if (!schema) {
  throw new Error('缺少 EXPENSE 表单 schema')
}

const form = ref<FormModel>({
  ...createFormModel(schema),
  receiptDate: new Date().toISOString().slice(0, 10),
  reporterName: getUserName()
})
const formRef = ref<InstanceType<typeof SchemaForm> | null>(null)
const submitting = ref(false)
/** 识别中：用于按钮 loading / 防重复点击 */
const recognizing = ref(false)
/** AI 识别原始输出，随创建请求提交给后端留痕 */
const ocrData = ref('')

function text(key: string): string {
  return String(form.value[key] ?? '').trim()
}

function optional(key: string): string | undefined {
  const value = text(key)
  return value || undefined
}

/** 枚举白名单校验：不在允许范围内返回 undefined，避免回填非法值。 */
function pickEnum<T extends string>(value: string | null | undefined, allowed: readonly T[]): T | undefined {
  const normalized = (value ?? '').trim().toUpperCase()
  return (allowed as readonly string[]).includes(normalized) ? (normalized as T) : undefined
}

/** 统一取 yyyy-MM-dd（后端已返回该格式，兼容带时间的 ISO 字符串）。 */
function dateOnly(value: string | null | undefined): string {
  const date = (value ?? '').trim().slice(0, 10)
  return /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : ''
}

/** 仅回填识别出的非空字段；返回回填字段数。 */
function applyOcrResult(result: ReceiptOcrResult): number {
  const patch: FormModel = {}
  const receiptNumber = (result.receiptNumber ?? '').trim()
  if (receiptNumber) patch.receiptNumber = receiptNumber
  const amount = (result.amount ?? '').trim()
  if (amount) patch.amount = amount
  const receiptDate = dateOnly(result.receiptDate)
  if (receiptDate) patch.receiptDate = receiptDate
  const type = pickEnum(result.type, RECEIPT_TYPES)
  if (type) patch.type = type
  const paymentMethod = pickEnum(result.paymentMethod, PAYMENT_METHODS)
  if (paymentMethod) patch.paymentMethod = paymentMethod
  form.value = { ...form.value, ...patch }
  return Object.keys(patch).length
}

async function recognize(): Promise<void> {
  const filePath = text('filePath')
  if (!filePath) {
    uni.showToast({ title: '请先上传票据图片', icon: 'none' })
    return
  }
  if (recognizing.value) return
  recognizing.value = true
  try {
    const result = await apiClient.ai.receiptOcr({ filePath })
    ocrData.value = result.raw ?? ''
    const filled = applyOcrResult(result)
    uni.showToast({
      title: filled > 0 ? '已回填，请核对后提交' : '未识别到有效信息，请手动填写',
      icon: 'none'
    })
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, 'AI 识别失败'), icon: 'none' })
  } finally {
    recognizing.value = false
  }
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
      filePath: optional('filePath'),
      ocrData: ocrData.value || undefined
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
  <view class="ceams-page page--with-bar">
    <SchemaForm ref="formRef" v-model="form" :schema="schema" :disabled="submitting" />

    <view class="ocr ceams-card">
      <view class="ceams-section-title ocr__title">AI 识别</view>
      <text class="ocr__hint">上传票据图片后，可自动识别编号、金额、日期、类型与支付方式并回填表单</text>
      <button
        class="ceams-btn ceams-btn--ghost ocr__btn"
        :disabled="submitting || recognizing || !text('filePath')"
        :loading="recognizing"
        @click="recognize"
      >
        {{ recognizing ? '识别中…' : 'AI 识别并回填' }}
      </button>
    </view>

    <!-- 底部提交栏：白底 + 安全区，避免被系统手势条遮挡 -->
    <view class="submit ceams-safe-bottom">
      <button class="ceams-btn ceams-btn--primary submit__btn" :disabled="submitting" @click="submit">
        保存为草稿
      </button>
      <text class="submit__hint">保存后可在详情页点击"提交审批"发起流程</text>
    </view>
  </view>
</template>

<style scoped>
/* 为固定提交栏预留滚动空间 */
.page--with-bar {
  padding-bottom: calc(var(--ceams-control-height) + var(--ceams-space-3xl) + var(--ceams-space-lg));
}

.ocr__title {
  margin-bottom: var(--ceams-space-xs);
}

.ocr__hint {
  display: block;
  margin-bottom: var(--ceams-space-md);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.ocr__btn {
  margin: 0;
}

.submit {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  padding-top: var(--ceams-space-sm);
  padding-left: var(--ceams-space-xl);
  padding-right: var(--ceams-space-xl);
  background-color: var(--ceams-bg-card);
  border-top: var(--ceams-border-width) solid var(--ceams-divider);
  box-shadow: var(--ceams-shadow-raised);
  box-sizing: border-box;
}

.submit__btn {
  margin: 0;
}

.submit__hint {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}
</style>
