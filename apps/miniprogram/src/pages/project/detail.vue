<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import type { ApprovalStatus, ProgressPaymentVO, ProjectStatus, ProjectVO } from '@ceams/types'
import { formatDate } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { approvalStatusLabels, projectStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

type TagType = 'pending' | 'approved' | 'rejected' | 'verified' | 'paid' | 'neutral'

/** 展示用的状态配色映射，不参与业务判断 */
const projectStatusTagTypes: Record<ProjectStatus, TagType> = {
  PLANNING: 'neutral',
  IN_PROGRESS: 'verified',
  PAUSED: 'pending',
  COMPLETED: 'approved',
  CLOSED: 'neutral'
}

const approvalStatusTagTypes: Record<ApprovalStatus, TagType> = {
  DRAFT: 'neutral',
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
  CANCELLED: 'neutral'
}

const id = ref('')
const project = ref<ProjectVO | null>(null)
const payments = ref<ProgressPaymentVO[]>([])
const page = ref(1)
const total = ref(0)
const size = 20
const loading = ref(false)

async function loadProject(): Promise<void> {
  try {
    project.value = await apiClient.project.projectDetail(id.value)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载项目详情失败'), icon: 'none' })
  }
}

async function loadPayments(reset = false): Promise<void> {
  if (reset) page.value = 1
  if (loading.value) return
  loading.value = true
  try {
    const res = await apiClient.project.paymentPage({ page: page.value, size, projectId: id.value })
    payments.value = reset ? res.records : payments.value.concat(res.records)
    total.value = res.total
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载进度款失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  id.value = typeof query?.id === 'string' ? query.id : ''
  void loadProject()
  void loadPayments(true)
})

/** 携带当前项目 ID 进入变更单创建页 */
function openChangeCreate(): void {
  uni.navigateTo({ url: `/pages/project/change-create?projectId=${id.value}` })
}

onReachBottom(() => {
  if (loading.value || payments.value.length >= total.value) return
  page.value += 1
  void loadPayments()
})
</script>

<template>
  <view class="ceams-page detail">
    <view v-if="project" class="detail__overview">
      <view class="detail__overview-head">
        <text class="detail__name">{{ project.projectName }}</text>
        <status-tag
          :label="projectStatusLabels[project.status]"
          :type="projectStatusTagTypes[project.status]"
        />
      </view>
      <text class="detail__overview-label">合同金额</text>
      <amount-text
        :value="project.contractAmount ?? '--'"
        size="lg"
        color="var(--ceams-color-primary-dark)"
      />
      <text class="detail__overview-meta">
        {{ project.projectType }} · {{ project.clientName || '-' }}
      </text>
      <view class="detail__overview-actions">
        <text class="detail__overview-action" @click="openChangeCreate">发起项目变更 ›</text>
      </view>
    </view>

    <view v-if="project" class="ceams-card detail__info">
      <view class="ceams-section-title detail__section-title">项目信息</view>
      <view class="ceams-list-item detail__kv">
        <text class="detail__kv-label">客户</text>
        <text class="detail__kv-value">{{ project.clientName || '-' }}</text>
      </view>
      <view class="ceams-list-item detail__kv">
        <text class="detail__kv-label">项目类型</text>
        <text class="detail__kv-value">{{ project.projectType }}</text>
      </view>
      <view class="ceams-list-item detail__kv">
        <text class="detail__kv-label">预算金额</text>
        <amount-text :value="project.budgetAmount ?? '--'" size="sm" />
      </view>
      <view class="ceams-list-item detail__kv">
        <text class="detail__kv-label">实际金额</text>
        <amount-text :value="project.actualAmount ?? '--'" size="sm" />
      </view>
      <view class="ceams-list-item detail__kv">
        <text class="detail__kv-label">负责人</text>
        <text class="detail__kv-value">{{ project.managerName || '-' }}</text>
      </view>
      <view class="ceams-list-item detail__kv">
        <text class="detail__kv-label">项目周期</text>
        <text class="detail__kv-value">
          {{ project.startDate ? formatDate(project.startDate) : '--' }} 至
          {{ project.endDate ? formatDate(project.endDate) : '未定' }}
        </text>
      </view>
      <view class="ceams-list-item detail__kv">
        <text class="detail__kv-label">项目地址</text>
        <text class="detail__kv-value">{{ project.projectAddress || '-' }}</text>
      </view>
      <view class="ceams-list-item ceams-list-item--last detail__kv">
        <text class="detail__kv-label">项目描述</text>
        <text class="detail__kv-value">{{ project.description || '-' }}</text>
      </view>
    </view>

    <view class="detail__section-head">
      <view class="ceams-section-title">进度款</view>
      <text class="detail__section-count">共 {{ total }} 笔</text>
    </view>

    <view v-if="payments.length === 0" class="ceams-card">
      <empty-state title="暂无进度款" description="该项目尚未产生进度款记录" />
    </view>

    <view v-else class="ceams-card detail__payments">
      <view
        v-for="(item, index) in payments"
        :key="item.id"
        :class="[
          'ceams-list-item',
          'detail__payment',
          index === payments.length - 1 ? 'ceams-list-item--last' : ''
        ]"
      >
        <view class="detail__payment-main">
          <view class="detail__payment-head">
            <text class="detail__payment-period">{{ item.paymentPeriod }}</text>
            <status-tag
              :label="approvalStatusLabels[item.status]"
              :type="approvalStatusTagTypes[item.status]"
            />
          </view>
          <text class="detail__payment-date">
            申请日期：{{ item.applicationDate ? formatDate(item.applicationDate) : '--' }}
          </text>
          <view class="detail__figs">
            <view class="detail__fig">
              <text class="detail__fig-label">申请</text>
              <amount-text
                :value="item.applicationAmount ?? '--'"
                size="sm"
                color="var(--ceams-text-secondary)"
              />
            </view>
            <view class="detail__fig">
              <text class="detail__fig-label">批准</text>
              <amount-text
                :value="item.approvedAmount ?? '--'"
                size="sm"
                color="var(--ceams-text-secondary)"
              />
            </view>
          </view>
        </view>
        <view class="detail__payment-amount">
          <text class="detail__payment-amount-label">实付金额</text>
          <amount-text
            :value="item.paymentAmount ?? item.approvedAmount ?? item.applicationAmount ?? '--'"
            size="md"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.detail__overview {
  margin-bottom: var(--ceams-space-md);
  padding: var(--ceams-space-lg);
  background-color: var(--ceams-color-primary-tint);
  border-radius: var(--ceams-radius-card);
}

.detail__overview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-md);
}

.detail__name {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
  color: var(--ceams-text-primary);
  font-size: 28rpx;
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.detail__overview-label {
  display: block;
  color: var(--ceams-color-primary-deep);
  font-size: var(--ceams-font-size-caption);
}

.detail__overview-meta {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.detail__overview-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--ceams-space-sm);
}

.detail__overview-action {
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

/* 分组标题沿用全局 .ceams-section-title，仅补下间距 */
.detail__section-title {
  margin-bottom: var(--ceams-space-sm);
}

.detail__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-sm);
}

.detail__section-count {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
}

.detail__info {
  padding-bottom: 0;
}

.detail__kv {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail__kv-label {
  flex-shrink: 0;
  margin-right: var(--ceams-space-lg);
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
}

.detail__kv-value {
  flex: 1;
  min-width: 0;
  color: var(--ceams-text-primary);
  font-size: 24rpx;
  line-height: var(--ceams-line-height-normal);
  text-align: right;
  word-break: break-all;
}

.detail__payments {
  padding-top: 0;
  padding-bottom: 0;
}

.detail__payment {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.detail__payment-main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
}

.detail__payment-head {
  display: flex;
  align-items: center;
}

.detail__payment-period {
  margin-right: var(--ceams-space-sm);
  color: var(--ceams-text-primary);
  font-size: 28rpx;
  font-weight: var(--ceams-font-weight-semibold);
}

.detail__payment-date {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
}

.detail__figs {
  display: flex;
  margin-top: var(--ceams-space-xs);
}

.detail__fig {
  display: flex;
  flex-direction: column;
  margin-right: var(--ceams-space-lg);
}

.detail__fig-label {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
}

.detail__payment-amount {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.detail__payment-amount-label {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
}
</style>
