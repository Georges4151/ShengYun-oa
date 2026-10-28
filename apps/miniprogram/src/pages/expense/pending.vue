<script setup lang="ts">
import { ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import type { ReceiptVO } from '@ceams/types'
import { formatMoney } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { receiptApprovalStatusLabels, receiptTypeLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

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
  <view class="page">
    <view v-if="loading && list.length === 0" class="empty">加载中...</view>
    <view v-else-if="list.length === 0" class="empty">暂无待办报销</view>

    <view v-for="item in list" :key="item.id" class="card" @click="openDetail(item.id)">
      <view class="card__row">
        <text class="card__title">{{ item.receiptNumber }}</text>
        <text class="card__tag">{{ receiptApprovalStatusLabels[item.approvalStatus] }}</text>
      </view>
      <text class="card__line">报销人：{{ item.reporterName || '-' }}</text>
      <text class="card__line">类型：{{ receiptTypeLabels[item.type] }}</text>
      <text class="card__line">金额：{{ formatMoney(item.amount) }}</text>
    </view>

    <view v-if="list.length > 0 && list.length < total" class="loadmore">上拉加载更多</view>
  </view>
</template>

<style scoped>
.page {
  padding: 24rpx 32rpx;
}
.card {
  padding: 28rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}
.card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.card__title {
  font-size: 32rpx;
  font-weight: 600;
}
.card__tag {
  font-size: 22rpx;
  color: #2563eb;
  background-color: #eff6ff;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
}
.card__line {
  display: block;
  font-size: 26rpx;
  color: #6b7280;
  margin-top: 6rpx;
}
.empty {
  text-align: center;
  color: #9ca3af;
  padding: 80rpx 0;
}
.loadmore {
  text-align: center;
  color: #9ca3af;
  font-size: 24rpx;
  padding: 20rpx 0;
}
</style>
