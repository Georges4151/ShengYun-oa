<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import type { InstanceDetailVO, RejectTarget, TaskVO } from '@ceams/types'
import { formatDate } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'
import {
  bpmBizTypeLabels,
  bpmInstanceStatusLabels,
  bpmNodeStatusLabels,
  bpmTaskStatusLabels
} from '../../utils/labels'

type TagType = 'pending' | 'approved' | 'rejected' | 'neutral'

const instanceTagTypes: Record<string, TagType> = {
  RUNNING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
  WITHDRAWN: 'neutral',
  TERMINATED: 'neutral',
  DRAFT: 'neutral'
}

const instanceId = ref('')
const taskId = ref('')
const detail = ref<InstanceDetailVO | null>(null)
const comment = ref('')
const rejectTarget = ref<RejectTarget>('INITIATOR')
const submitting = ref(false)

/** 当前待我处理的任务（由待办列表跳转时携带 taskId，并已回传 taskVersion）。 */
const currentTask = computed<TaskVO | null>(() => {
  if (!detail.value || !taskId.value) return null
  for (const node of detail.value.nodes) {
    const found = (node.tasks ?? []).find((task) => task.id === taskId.value)
    if (found) return found
  }
  return null
})

const canOperate = computed<boolean>(() => currentTask.value?.status === 'PENDING')

async function load(): Promise<void> {
  try {
    detail.value = await apiClient.bpm.instanceDetail(instanceId.value)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载流程详情失败'), icon: 'none' })
  }
}

async function approve(): Promise<void> {
  const task = currentTask.value
  if (!task || submitting.value) return
  submitting.value = true
  try {
    await apiClient.bpm.approveTask(task.id, {
      taskVersion: task.taskVersion,
      comment: comment.value || undefined
    })
    uni.showToast({ title: '已通过', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '审批失败，请返回刷新'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function chooseRejectTarget(): void {
  uni.showActionSheet({
    itemList: ['驳回至发起人', '驳回至上一级'],
    success: (res) => {
      rejectTarget.value = res.tapIndex === 1 ? 'PREVIOUS' : 'INITIATOR'
      void reject()
    }
  })
}

async function reject(): Promise<void> {
  const task = currentTask.value
  if (!task || submitting.value) return
  submitting.value = true
  try {
    await apiClient.bpm.rejectTask(task.id, {
      taskVersion: task.taskVersion,
      target: rejectTarget.value,
      reason: comment.value || undefined
    })
    uni.showToast({ title: '已驳回', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '驳回失败，请返回刷新'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad((query) => {
  instanceId.value = typeof query?.instanceId === 'string' ? query.instanceId : ''
  taskId.value = typeof query?.taskId === 'string' ? query.taskId : ''
  void load()
})
</script>

<template>
  <view class="ceams-page" :class="{ 'page--with-bar': canOperate }">
    <template v-if="detail">
      <view class="head">
        <view class="head__main">
          <text class="head__title">
            {{ bpmBizTypeLabels[detail.instance.bizType] ?? detail.instance.bizType }}审批
          </text>
          <text class="head__sub">流程实例 {{ detail.instance.id }} · 单据 {{ detail.instance.bizId ?? '-' }}</text>
        </view>
        <status-tag
          :label="bpmInstanceStatusLabels[detail.instance.status]"
          :type="instanceTagTypes[detail.instance.status] ?? 'neutral'"
        />
      </view>

      <view v-if="currentTask" class="ceams-card task">
        <text class="task__title">当前待办</text>
        <view class="row">
          <text class="row__label">任务状态</text>
          <text class="row__value">{{ bpmTaskStatusLabels[currentTask.status] }}</text>
        </view>
        <view class="row">
          <text class="row__label">到期时间</text>
          <text class="row__value">{{ formatDate(currentTask.dueAt) }}</text>
        </view>
        <view v-if="canOperate" class="task__comment">
          <textarea v-model="comment" class="comment" placeholder="请输入审批意见（可选）" maxlength="500" />
        </view>
      </view>

      <view class="ceams-card">
        <text class="section-title">流程节点</text>
        <view v-for="node in detail.nodes" :key="node.id" class="node">
          <view class="node__head">
            <text class="node__name">#{{ node.seqNo }} {{ node.nodeKey }}</text>
            <status-tag
              :label="bpmNodeStatusLabels[node.status] ?? node.status"
              :type="node.status === 'COMPLETED' ? 'approved' : node.status === 'REJECTED' ? 'rejected' : node.status === 'ACTIVE' ? 'pending' : 'neutral'"
            />
          </view>
          <text class="node__meta">到达 {{ formatDate(node.arrivedAt) }} · 完成 {{ formatDate(node.completedAt) }}</text>
          <view v-if="node.tasks?.length" class="node__tasks">
            <text v-for="task in node.tasks" :key="task.id" class="node__task">
              审批人 {{ task.assigneeId ?? '-' }} · {{ bpmTaskStatusLabels[task.status] }}
            </text>
          </view>
        </view>
      </view>
    </template>

    <view v-else class="state">加载中...</view>

    <view v-if="canOperate" class="action ceams-safe-bottom">
      <button class="ceams-btn action__reject" :disabled="submitting" @click="chooseRejectTarget">驳回</button>
      <button class="ceams-btn ceams-btn--primary action__approve" :disabled="submitting" @click="approve">
        通过
      </button>
    </view>
  </view>
</template>

<style scoped>
.page--with-bar {
  padding-bottom: calc(var(--ceams-control-height) + var(--ceams-space-2xl));
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-md);
  padding: var(--ceams-space-lg);
  background-color: var(--ceams-color-primary-tint);
  border-radius: var(--ceams-radius-card);
  box-sizing: border-box;
}

.head__main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
}

.head__title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-bold);
  line-height: var(--ceams-line-height-tight);
}

.head__sub {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.task__title,
.section-title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ceams-space-xs) 0;
}

.row__label {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.row__value {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

.task__comment {
  margin-top: var(--ceams-space-sm);
}

.comment {
  width: 100%;
  height: 140rpx;
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-subtle);
  border: var(--ceams-border-width) solid var(--ceams-border);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
  box-sizing: border-box;
}

.node {
  padding: var(--ceams-space-sm) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.node:last-child {
  border-bottom: none;
}

.node__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.node__name {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  font-weight: var(--ceams-font-weight-semibold);
}

.node__meta {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.node__tasks {
  margin-top: var(--ceams-space-2xs);
}

.node__task {
  display: block;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.action {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  padding-top: var(--ceams-space-sm);
  padding-left: var(--ceams-space-xl);
  padding-right: var(--ceams-space-xl);
  background-color: var(--ceams-bg-card);
  border-top: var(--ceams-border-width) solid var(--ceams-divider);
  box-shadow: var(--ceams-shadow-raised);
  box-sizing: border-box;
}

.action__reject {
  flex-shrink: 0;
  width: 220rpx;
  margin: 0;
  color: var(--ceams-color-danger);
  background-color: var(--ceams-bg-card);
  border: var(--ceams-border-width) solid var(--ceams-color-danger);
}

.action__approve {
  flex: 1;
  margin: 0;
  margin-left: var(--ceams-space-sm);
}

.state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}
</style>
