<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElDialog, ElEmpty, ElMessage } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { FlowPreviewVO } from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'
import BpmFlowSteps from './BpmFlowSteps.vue'

defineOptions({ name: 'BpmFlowPreviewDialog' })

const props = defineProps<{
  modelValue: boolean
  templateCode?: string | null
  bizType?: string | null
  bizId?: string | null
  subjectUserId?: string | null
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const loading = ref(false)
const preview = ref<FlowPreviewVO | null>(null)

watch(
  () => [props.modelValue, props.templateCode, props.bizId, props.subjectUserId] as const,
  ([visible, code]) => {
    if (visible && code) {
      void load(code)
    }
  },
  { immediate: true }
)

async function load(templateCode: string): Promise<void> {
  loading.value = true
  preview.value = null
  try {
    preview.value = await ceamsApi.bpm.instancePreview({
      templateCode,
      bizType: props.bizType ?? undefined,
      bizId: props.bizId ?? undefined,
      subjectUserId: props.subjectUserId ?? undefined
    })
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载流程预览失败'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="审批流程预览"
    width="620px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="preview-body">
      <div v-if="!preview" class="preview-empty">
        <el-empty description="暂无流程数据" :image-size="72" />
      </div>
      <template v-else>
        <div class="preview-head">
          <span class="preview-title">{{ preview.templateName || preview.templateCode }}</span>
          <span class="preview-hint">
            以下为按当前组织关系估算的审批人，实际以提交后的流程为准
          </span>
        </div>
        <BpmFlowSteps :nodes="preview.nodes" />
      </template>
    </div>
  </el-dialog>
</template>

<style scoped>
:deep(.el-dialog__title) {
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

.preview-body {
  min-height: 180px;
}

.preview-empty {
  padding: 40px 0;
}

.preview-head {
  display: flex;
  flex-direction: column;
  gap: var(--ceams-space-1);
}

.preview-title {
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.preview-hint {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
}
</style>
