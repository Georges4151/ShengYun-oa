<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElDialog, ElDescriptions, ElDescriptionsItem, ElEmpty, ElMessage, ElTag, ElTimeline, ElTimelineItem } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { InstanceDetailVO } from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'
import { bpmInstanceStatusLabels, bpmNodeStatusLabels, bpmNodeTypeLabels, bpmTaskStatusLabels } from '@/utils/labels'

defineOptions({ name: 'BpmInstanceDrawer' })

const props = defineProps<{ modelValue: boolean; instanceId?: string | null }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const loading = ref(false)
const detail = ref<InstanceDetailVO | null>(null)

// ---------------- 状态标签配色（仅视觉映射，不改动状态值） ----------------
type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 流程实例状态 -> 标签颜色 */
function instanceStatusTagType(status: string): TagType {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'RUNNING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

/** 节点状态 -> 标签颜色 */
function nodeStatusTagType(status: string): TagType {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'ACTIVE':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

/** 任务状态 -> 标签颜色 */
function taskStatusTagType(status: string): TagType {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'CLOSED':
      return 'info'
    default:
      return 'warning'
  }
}

watch(
  () => [props.modelValue, props.instanceId] as const,
  ([visible, id]) => {
    if (visible && id) {
      void load(id)
    }
  },
  { immediate: true }
)

async function load(id: string): Promise<void> {
  loading.value = true
  try {
    detail.value = await ceamsApi.bpm.instanceDetail(id)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载流程详情失败'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="审批流程"
    width="680px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="detail-body">
      <div v-if="!detail" class="detail-empty">
        <el-empty description="暂无流程数据" :image-size="72" />
      </div>
      <template v-else>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="流程实例">{{ detail.instance.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="instanceStatusTagType(detail.instance.status)">
              {{ bpmInstanceStatusLabels[detail.instance.status] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发起人">{{ detail.instance.initiatorId }}</el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ detail.instance.startedAt ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">
            {{ detail.instance.currentNodeKey ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ detail.instance.endedAt ?? '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-timeline class="timeline">
          <el-timeline-item
            v-for="node in detail.nodes"
            :key="node.id"
            :timestamp="node.arrivedAt ?? ''"
            placement="top"
          >
            <div class="node-title">
              <span>#{{ node.seqNo }} {{ node.nodeKey }}</span>
              <el-tag size="small" type="info">{{ bpmNodeTypeLabels[node.nodeType] ?? node.nodeType }}</el-tag>
              <el-tag size="small" :type="nodeStatusTagType(node.status)">
                {{ bpmNodeStatusLabels[node.status] ?? node.status }}
              </el-tag>
            </div>
            <div v-if="node.tasks?.length" class="task-list">
              <div v-for="task in node.tasks" :key="task.id" class="task-item">
                <span>审批人 {{ task.assigneeId ?? '-' }}</span>
                <el-tag size="small" :type="taskStatusTagType(task.status)">
                  {{ bpmTaskStatusLabels[task.status] ?? task.status }}
                </el-tag>
                <span v-if="task.dueAt" class="due">到期 {{ task.dueAt }}</span>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </div>
  </el-dialog>
</template>

<style scoped>
/* 弹窗：标题加粗，内容区 20px 24px */
:deep(.el-dialog__title) {
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

.detail-body {
  min-height: 200px;
}

.detail-empty {
  padding: 40px 0;
}

.timeline {
  margin-top: var(--ceams-space-5);
}

.node-title {
  display: flex;
  gap: var(--ceams-space-2);
  align-items: center;
  font-weight: 500;
  color: var(--ceams-text-primary);
}

.task-list {
  margin-top: var(--ceams-space-2);
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
}

.task-item {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ceams-space-2);
  align-items: center;
  line-height: 1.9;
}

.due {
  color: var(--ceams-color-warning);
}
</style>
