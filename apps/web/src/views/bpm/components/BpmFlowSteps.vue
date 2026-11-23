<script setup lang="ts">
import { ElTag, ElTimeline, ElTimelineItem } from 'element-plus'
import type { NodeVO, PreviewNodeVO, TaskVO } from '@ceams/types'
import {
  bpmAssigneeSourceLabels,
  bpmNodeStatusLabels,
  bpmNodeTypeLabels,
  bpmTaskStatusLabels
} from '@/utils/labels'

defineOptions({ name: 'BpmFlowSteps' })

/**
 * 审批流程竖向步骤条：节点名称、审批人姓名、状态、时间。
 * 同时服务于「发起后查看」（NodeVO）与「发起前预览」（PreviewNodeVO）。
 */
defineProps<{ nodes: Array<NodeVO | PreviewNodeVO> }>()

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

function nodeStatus(node: NodeVO | PreviewNodeVO): string | null {
  return 'status' in node ? node.status : null
}

function nodeStatusTagType(node: NodeVO | PreviewNodeVO): TagType {
  switch (nodeStatus(node)) {
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

function nodeTasks(node: NodeVO | PreviewNodeVO): TaskVO[] {
  return 'tasks' in node ? node.tasks ?? [] : []
}

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
</script>

<template>
  <el-timeline class="flow-steps">
    <el-timeline-item
      v-for="(node, index) in nodes"
      :key="'id' in node ? node.id : node.nodeKey + '-' + index"
      :timestamp="'arrivedAt' in node ? node.arrivedAt ?? '' : ''"
      placement="top"
      :type="nodeStatusTagType(node)"
    >
      <div class="node-title">
        <span>{{ node.seqNo }}. {{ node.nodeName || node.nodeKey }}</span>
        <el-tag size="small" type="info">
          {{ bpmNodeTypeLabels[node.nodeType] ?? node.nodeType }}
        </el-tag>
        <el-tag v-if="nodeStatus(node)" size="small" :type="nodeStatusTagType(node)">
          {{ bpmNodeStatusLabels[nodeStatus(node)!] ?? nodeStatus(node) }}
        </el-tag>
        <el-tag v-if="node.predicted" size="small" type="warning">预计</el-tag>
      </div>

      <div v-if="node.nodeType !== 'START' && node.nodeType !== 'END'" class="assignee-list">
        <template v-if="node.assignees?.length">
          <span class="assignee-label">{{ node.predicted ? '预计审批人' : '审批人' }}</span>
          <el-tag
            v-for="assignee in node.assignees"
            :key="assignee.id"
            size="small"
            effect="plain"
          >
            {{ assignee.name || assignee.id }}
            <span v-if="assignee.sourceType" class="assignee-source">
              （{{ bpmAssigneeSourceLabels[assignee.sourceType] ?? assignee.sourceType }}）
            </span>
          </el-tag>
        </template>
        <span v-else class="assignee-rule">{{ node.assigneeRule || '暂无审批人' }}</span>
      </div>

      <div v-if="nodeTasks(node).length" class="task-list">
        <div v-for="task in nodeTasks(node)" :key="task.id" class="task-item">
          <span>
            {{ task.assigneeName || task.assigneeId || '-' }}
            <template v-if="task.ownerAssigneeName">（代 {{ task.ownerAssigneeName }}）</template>
          </span>
          <el-tag size="small" :type="taskStatusTagType(task.status)">
            {{ bpmTaskStatusLabels[task.status] ?? task.status }}
          </el-tag>
          <span v-if="task.dueAt" class="due">到期 {{ task.dueAt }}</span>
        </div>
      </div>
      <div v-if="'completedAt' in node && node.completedAt" class="node-time">
        完成于 {{ node.completedAt }}
      </div>
    </el-timeline-item>
  </el-timeline>
</template>

<style scoped>
.flow-steps {
  margin-top: var(--ceams-space-5);
}

.node-title {
  display: flex;
  gap: var(--ceams-space-2);
  align-items: center;
  font-weight: 500;
  color: var(--ceams-text-primary);
}

.assignee-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ceams-space-2);
  align-items: center;
  margin-top: var(--ceams-space-2);
  font-size: var(--ceams-font-size-sm);
}

.assignee-label {
  color: var(--ceams-text-secondary);
}

.assignee-source {
  color: var(--ceams-text-secondary);
}

.assignee-rule {
  color: var(--ceams-color-warning);
}

.node-time {
  margin-top: var(--ceams-space-1);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
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
