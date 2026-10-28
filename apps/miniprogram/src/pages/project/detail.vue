<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import type { ProgressPaymentVO, ProjectVO } from '@ceams/types'
import { formatMoney } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { approvalStatusLabels, projectStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

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

onReachBottom(() => {
  if (loading.value || payments.value.length >= total.value) return
  page.value += 1
  void loadPayments()
})
</script>

<template>
  <view class="page">
    <view v-if="project" class="panel">
      <view class="panel__title">
        <text class="panel__name">{{ project.projectName }}</text>
        <text class="panel__tag">{{ projectStatusLabels[project.status] }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">客户</text>
        <text class="kv__value">{{ project.clientName || '-' }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">类型</text>
        <text class="kv__value">{{ project.projectType }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">合同金额</text>
        <text class="kv__value">{{ formatMoney(project.contractAmount) }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">预算金额</text>
        <text class="kv__value">{{ formatMoney(project.budgetAmount) }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">负责人</text>
        <text class="kv__value">{{ project.managerName || '-' }}</text>
      </view>
    </view>

    <text class="section">进度款（只读）</text>
    <view v-if="payments.length === 0" class="empty">暂无进度款</view>
    <view v-for="item in payments" :key="item.id" class="card">
      <view class="card__row">
        <text class="card__title">{{ item.paymentPeriod }}</text>
        <text class="card__tag">{{ approvalStatusLabels[item.status] }}</text>
      </view>
      <text class="card__line">申请金额：{{ formatMoney(item.applicationAmount) }}</text>
      <text class="card__line">批准金额：{{ formatMoney(item.approvedAmount) }}</text>
      <text class="card__line">实付金额：{{ formatMoney(item.paymentAmount) }}</text>
    </view>
  </view>
</template>

<style scoped>
.page {
  padding: 24rpx 32rpx;
}
.panel {
  padding: 28rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}
.panel__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.panel__name {
  font-size: 34rpx;
  font-weight: 600;
}
.panel__tag {
  font-size: 22rpx;
  color: #2563eb;
  background-color: #eff6ff;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
}
.kv {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}
.kv__label {
  font-size: 26rpx;
  color: #6b7280;
}
.kv__value {
  font-size: 26rpx;
}
.section {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}
.card {
  padding: 28rpx;
  background-color: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}
.card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.card__title {
  font-size: 30rpx;
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
</style>
