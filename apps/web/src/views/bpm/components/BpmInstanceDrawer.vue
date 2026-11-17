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
    <div v-loading="loading">
      <el-empty v-if="!detail" description="暂无流程数据" />
      <template v-else>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="流程实例">{{ detail.instance.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small">{{ bpmInstanceStatusLabels[detail.instance.status] }}</el-tag>
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
              <el-tag size="small">{{ bpmNodeStatusLabels[node.status] ?? node.status }}</el-tag>
            </div>
            <div v-if="node.tasks?.length" class="task-list">
              <div v-for="task in node.tasks" :key="task.id" class="task-item">
                审批人 {{ task.assigneeId ?? '-' }} · {{ bpmTaskStatusLabels[task.status] ?? task.status }}
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
.timeline {
  margin-top: 16px;
}
.node-title {
  display: flex;
  gap: 8px;
  align-items: center;
  font-weight: 500;
}
.task-list {
  margin-top: 6px;
  color: #4b5563;
  font-size: 13px;
}
.task-item {
  line-height: 1.8;
}
.due {
  margin-left: 8px;
  color: #b45309;
}
</style>
