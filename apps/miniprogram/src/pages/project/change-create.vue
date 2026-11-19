<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { apiClient } from '../../platform/http'
import SchemaForm from '../../components/schema-form/schema-form.vue'
import type { FormModel } from '../../schemas/form-schema'
import { createFormModel, getFormSchema } from '../../schemas/form-schema'
import { apiErrorMessage } from '../../utils/error'

const schema = getFormSchema('CHANGE_REQUEST')
if (!schema) {
  throw new Error('缺少 CHANGE_REQUEST 表单 schema')
}

const form = ref<FormModel>(createFormModel(schema))
const formRef = ref<InstanceType<typeof SchemaForm> | null>(null)
const submitting = ref(false)

function text(key: string): string {
  return String(form.value[key] ?? '').trim()
}

function optional(key: string): string | undefined {
  const value = text(key)
  return value || undefined
}

/** 从项目详情进入时自动带入项目 ID */
onLoad((query) => {
  const value = typeof query?.projectId === 'string' ? query.projectId : ''
  if (value) form.value.projectId = value
})

/** 创建变更单；submitNow=true 时创建后立即发起 BPM 审批。 */
async function create(submitNow: boolean): Promise<void> {
  const error = formRef.value?.validate()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const id = await apiClient.project.changeCreate({
      projectId: text('projectId'),
      requestType: text('requestType'),
      description: optional('description'),
      estimatedCost: optional('estimatedCost')
    })
    if (submitNow) {
      await apiClient.project.changeSubmit(id)
      uni.showToast({ title: '已提交审批', icon: 'success' })
    } else {
      uni.showToast({ title: '已保存草稿', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 600)
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

    <view class="hint">
      <text class="hint__text">提交后流程为「部门审批 → 结束」，审批人取发起人所属部门主管。</text>
    </view>

    <view class="actions">
      <button class="ceams-btn actions__draft" :disabled="submitting" @click="create(false)">
        保存草稿
      </button>
      <button class="ceams-btn ceams-btn--primary actions__submit" :disabled="submitting" @click="create(true)">
        保存并提交
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

.actions {
  display: flex;
  gap: var(--ceams-space-sm);
  margin-top: var(--ceams-space-lg);
}

.actions__draft {
  flex-shrink: 0;
  width: 240rpx;
  margin: 0;
}

.actions__submit {
  flex: 1;
  margin: 0;
}
</style>
