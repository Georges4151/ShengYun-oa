<script setup lang="ts">
import { ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import type { ReceiptApprovalStatus, ReceiptVO } from '@ceams/types'
import { formatDate } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { receiptApprovalStatusLabels, receiptTypeLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

/** 审批状态 → 状态标签语义（仅用于展示配色） */
type TagType = 'pending' | 'approved' | 'rejected' | 'neutral'

const approvalTagTypes: Record<ReceiptApprovalStatus, TagType> = {
  DRAFT: 'neutral',
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected'
}

const list = ref<ReceiptVO[]>([])
const page = ref(1)
const total = ref(0)
const size = 20
const loading = ref(false)

async function load(reset = false): Promise<void> {
  if (reset) page.value = 1
  if (loading.value) return
  loading.value = true
  try {
    const res = await apiClient.expense.receiptPage({
      page: page.value,
      size,
      approvalStatus: 'PENDING'
    })
    list.value = reset ? res.records : list.value.concat(res.records)
    total.value = res.total
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载待办失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function openDetail(id: string): void {
  uni.navigateTo({ url: `/pages/expense/detail?id=${id}` })
}

onShow(() => {
  void load(true)
})

onReachBottom(() => {
  if (loading.value || list.value.length >= total.value) return
  page.value += 1
  void load()
})
</script>

<template>
  <view class="ceams-page">
    <view v-if="list.length > 0" class="ceams-card summary">
      <view class="summary__main">
        <text class="summary__title">待办审批</text>
        <text class="summary__hint">审批完成后会移出待办列表</text>
      </view>
      <view class="summary__count">
        <text class="summary__number">{{ list.length }}</text>
        <text class="summary__unit">条</text>
      </view>
    </view>

    <view v-if="loading && list.length === 0" class="state">加载中...</view>
    <empty-state
      v-else-if="list.length === 0"
      title="暂无待办"
      description="审批完成后会在此显示"
    />

    <view v-for="item in list" :key="item.id" class="item" @click="openDetail(item.id)">
      <list-card>
        <template #left>
          <view class="item__main">
            <text class="item__title ceams-ellipsis">{{ item.receiptNumber }}</text>
            <text class="item__subtitle ceams-ellipsis">{{ item.reporterName || '-' }} · {{ receiptTypeLabels[item.type] }}</text>
            <text class="item__date">{{ formatDate(item.receiptDate) }}</text>
          </view>
        </template>
        <template #right>
          <view class="item__extra">
            <status-tag
              :label="receiptApprovalStatusLabels[item.approvalStatus]"
              :type="approvalTagTypes[item.approvalStatus]"
            />
            <view class="item__amount">
              <amount-text :value="item.amount" size="md" color="var(--ceams-color-primary)" />
            </view>
          </view>
        </template>
      </list-card>
    </view>

    <view v-if="list.length > 0 && list.length < total" class="loadmore">上拉加载更多</view>
  </view>
</template>

<style scoped>
.summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary__main {
  flex: 1;
  min-width: 0;
}

.summary__title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.summary__hint {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.summary__count {
  display: flex;
  align-items: flex-end;
  flex-shrink: 0;
  margin-left: var(--ceams-space-md);
}

.summary__number {
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-display);
  font-weight: var(--ceams-font-weight-bold);
  line-height: var(--ceams-line-height-tight);
  letter-spacing: var(--ceams-letter-spacing-number);
}

.summary__unit {
  margin-bottom: var(--ceams-space-2xs);
  margin-left: var(--ceams-space-2xs);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-tight);
}

.item__main {
  display: flex;
  flex-direction: column;
}

.item__title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.item__subtitle {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.item__date {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.item__extra {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.item__amount {
  margin-top: var(--ceams-space-xs);
}

.state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}

.loadmore {
  padding: var(--ceams-space-md) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}
</style>
