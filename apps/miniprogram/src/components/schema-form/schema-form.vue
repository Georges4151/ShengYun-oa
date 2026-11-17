<script setup lang="ts">
import { ref } from 'vue'
import type { FormField, FormModel, FormSchema } from '../../schemas/form-schema'
import { ATTACHMENT_UPLOAD_URL, API_BASE_URL } from '../../platform/config'
import { getAccessToken } from '../../platform/auth'
import { chooseFile, chooseImage, uploadFile } from '../../platform/upload'
import { apiErrorMessage } from '../../utils/error'

/**
 * JSON-Schema 表单渲染引擎。
 * 只负责按 schema 渲染控件与校验，具体提交动作由页面实现。
 */
defineOptions({ name: 'SchemaForm' })

const props = defineProps<{
  schema: FormSchema
  disabled?: boolean
}>()

const model = defineModel<FormModel>({ required: true })

type Widget = 'text' | 'number' | 'textarea' | 'date' | 'select' | 'attachment'

const uploading = ref('')

/** uni-app 小程序事件在 DOM 类型里是 Event，实际携带 detail.value。 */
function eventValue(event: Event): string {
  const detail = (event as unknown as { detail?: { value?: string | number } }).detail
  const value = detail?.value
  return value === undefined || value === null ? '' : String(value)
}

function widgetOf(field: FormField): Widget {
  if (field.enum?.length) return 'select'
  if (field.format === 'date') return 'date'
  if (field.format === 'textarea') return 'textarea'
  if (field.format === 'attachment') return 'attachment'
  return field.type === 'string' ? 'text' : 'number'
}

function isColumn(field: FormField): boolean {
  const widget = widgetOf(field)
  return widget === 'textarea' || widget === 'attachment'
}

function inputType(field: FormField): string {
  if (field.type === 'number') return 'digit'
  if (field.type === 'integer') return 'number'
  return 'text'
}

function valueOf(field: FormField): string {
  return model.value[field.key] ?? ''
}

function setValue(field: FormField, value: string): void {
  model.value = { ...model.value, [field.key]: value }
}

function onInput(field: FormField, event: Event): void {
  setValue(field, eventValue(event))
}

function onDateChange(field: FormField, event: Event): void {
  setValue(field, eventValue(event))
}

// ---------------- 下拉选择 ----------------

function selectRange(field: FormField): string[] {
  if (field.enumNames?.length) return field.enumNames
  return field.enum ?? []
}

function selectIndex(field: FormField): number {
  const value = valueOf(field)
  if (!value) return -1
  return (field.enum ?? []).indexOf(value)
}

function selectText(field: FormField): string {
  const index = selectIndex(field)
  if (index < 0) return field.placeholder ?? '请选择'
  return selectRange(field)[index] ?? valueOf(field)
}

function onSelectChange(field: FormField, event: Event): void {
  const value = (field.enum ?? [])[Number(eventValue(event))]
  if (value !== undefined) setValue(field, value)
}

// ---------------- 附件上传 ----------------

function fileName(field: FormField): string {
  const value = valueOf(field)
  return value.split('/').pop() ?? value
}

async function uploadTo(field: FormField, filePath: string): Promise<void> {
  const token = getAccessToken()
  const header = token ? { Authorization: `Bearer ${token}` } : undefined
  uploading.value = field.key
  try {
    setValue(field, await uploadFile(ATTACHMENT_UPLOAD_URL, filePath, { header }))
    uni.showToast({ title: '附件已上传', icon: 'success' })
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '上传失败'), icon: 'none' })
  } finally {
    uploading.value = ''
  }
}

async function pickAttachment(field: FormField): Promise<void> {
  try {
    const paths = await chooseImage(1)
    if (paths.length === 0) return
    await uploadTo(field, paths[0])
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '选择图片失败'), icon: 'none' })
  }
}

async function pickFile(field: FormField): Promise<void> {
  try {
    const path = await chooseFile()
    if (!path) return
    await uploadTo(field, path)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '选择文件失败'), icon: 'none' })
  }
}

function previewAttachment(field: FormField): void {
  const value = valueOf(field)
  if (!value) return
  const url = value.startsWith('http') ? value : `${API_BASE_URL}${value}`
  uni.setClipboardData({ data: url, success: () => uni.showToast({ title: '地址已复制', icon: 'none' }) })
}

function clearValue(field: FormField): void {
  setValue(field, '')
}

// ---------------- 校验 ----------------

/** 返回第一条校验错误文案；全部通过返回 null。 */
function validate(): string | null {
  for (const section of props.schema.sections) {
    for (const field of section.fields) {
      const value = valueOf(field).trim()
      if (field.required && !value) {
        return `${field.label}不能为空`
      }
      if (!value) continue
      if (field.type === 'string' && !field.enum?.length && field.format !== 'attachment') {
        if (field.maxLength && value.length > field.maxLength) {
          return `${field.label}不能超过 ${field.maxLength} 个字符`
        }
        continue
      }
      if (field.type === 'number' || field.type === 'integer') {
        if (!/^-?\d+(\.\d+)?$/.test(value)) {
          return `${field.label}必须是数字`
        }
        if (field.type === 'integer' && value.includes('.')) {
          return `${field.label}必须是整数`
        }
        const num = Number(value)
        if (field.minimum !== undefined && num < field.minimum) {
          return `${field.label}不能小于 ${field.minimum}`
        }
        if (field.maximum !== undefined && num > field.maximum) {
          return `${field.label}不能大于 ${field.maximum}`
        }
        if (field.multipleOf !== undefined && !Number.isInteger(num / field.multipleOf)) {
          return `${field.label}须为 ${field.multipleOf} 的整数倍`
        }
      }
    }
  }
  return null
}

defineExpose({ validate })
</script>

<template>
  <view class="schema-form">
    <view v-for="section in schema.sections" :key="section.title" class="ceams-card">
      <text class="section-title">{{ section.title }}</text>

      <view
        v-for="field in section.fields"
        :key="field.key"
        class="field"
        :class="{ 'field--column': isColumn(field) }"
      >
        <text class="field__label">
          {{ field.label }}<text v-if="field.required" class="field__required">*</text>
        </text>

        <picker
          v-if="widgetOf(field) === 'select'"
          :value="selectIndex(field)"
          :range="selectRange(field)"
          :disabled="disabled"
          @change="onSelectChange(field, $event)"
        >
          <view class="field__picker" :class="{ 'field__picker--empty': selectIndex(field) < 0 }">
            {{ selectText(field) }}
          </view>
        </picker>

        <picker
          v-else-if="widgetOf(field) === 'date'"
          mode="date"
          :value="valueOf(field)"
          :disabled="disabled"
          @change="onDateChange(field, $event)"
        >
          <view class="field__picker" :class="{ 'field__picker--empty': !valueOf(field) }">
            {{ valueOf(field) || '请选择日期' }}
          </view>
        </picker>

        <textarea
          v-else-if="widgetOf(field) === 'textarea'"
          class="field__textarea"
          :value="valueOf(field)"
          :placeholder="field.placeholder"
          :maxlength="field.maxLength ?? 500"
          :disabled="disabled"
          @input="onInput(field, $event)"
        />

        <view v-else-if="widgetOf(field) === 'attachment'" class="attach">
          <view class="attach__actions">
            <button
              class="ceams-btn ceams-btn--ghost attach__btn"
              :disabled="disabled || uploading === field.key"
              @click="pickAttachment(field)"
            >
              拍照/相册
            </button>
            <button
              class="ceams-btn ceams-btn--ghost attach__btn"
              :disabled="disabled || uploading === field.key"
              @click="pickFile(field)"
            >
              选择文件
            </button>
          </view>
          <text v-if="field.help" class="attach__hint">{{ field.help }}</text>

          <view v-if="valueOf(field)" class="attach__item" @click="previewAttachment(field)">
            <view class="attach__icon">▤</view>
            <view class="attach__main">
              <text class="attach__name ceams-ellipsis">{{ fileName(field) }}</text>
              <text class="attach__meta">已上传 · 点击复制地址</text>
            </view>
            <text class="attach__remove" @click.stop="clearValue(field)">移除</text>
          </view>
          <view v-else class="attach__empty">
            <text class="attach__empty-text">尚未上传附件</text>
          </view>
        </view>

        <input
          v-else
          class="field__input"
          :value="valueOf(field)"
          :type="inputType(field)"
          :placeholder="field.placeholder"
          :maxlength="field.maxLength ?? 140"
          :disabled="disabled"
          @input="onInput(field, $event)"
        />
      </view>
    </view>
  </view>
</template>

<style scoped>
.section-title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.field {
  display: flex;
  align-items: center;
  padding: var(--ceams-space-sm) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.field:last-child {
  border-bottom: none;
}

.field--column {
  flex-direction: column;
  align-items: stretch;
}

.field__label {
  width: 160rpx;
  flex-shrink: 0;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.field__required {
  margin-left: 4rpx;
  color: var(--ceams-color-danger);
}

.field__input {
  flex: 1;
  text-align: right;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

.field__picker {
  flex: 1;
  text-align: right;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

.field__picker--empty {
  color: var(--ceams-text-placeholder);
}

.field__textarea {
  width: 100%;
  height: 160rpx;
  margin-top: var(--ceams-space-xs);
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-subtle);
  border: var(--ceams-border-width) solid var(--ceams-border);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  box-sizing: border-box;
}

.attach {
  width: 100%;
  margin-top: var(--ceams-space-xs);
}

.attach__actions {
  display: flex;
  gap: var(--ceams-space-xs);
}

.attach__btn {
  margin: 0;
  padding: 0 var(--ceams-space-md);
  height: 56rpx;
  font-size: var(--ceams-font-size-mini);
}

.attach__hint {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.attach__item {
  display: flex;
  align-items: center;
  margin-top: var(--ceams-space-sm);
  padding: var(--ceams-space-sm);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
}

.attach__icon {
  width: 64rpx;
  height: 64rpx;
  margin-right: var(--ceams-space-sm);
  color: var(--ceams-color-primary);
  font-size: 32rpx;
  line-height: 64rpx;
  text-align: center;
}

.attach__main {
  flex: 1;
  min-width: 0;
}

.attach__name {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

.attach__meta {
  display: block;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.attach__remove {
  flex-shrink: 0;
  color: var(--ceams-color-danger);
  font-size: var(--ceams-font-size-mini);
}

.attach__empty {
  margin-top: var(--ceams-space-sm);
  padding: var(--ceams-space-lg) 0;
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
  text-align: center;
}

.attach__empty-text {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
}
</style>
