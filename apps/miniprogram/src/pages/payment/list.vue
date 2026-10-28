<script setup lang="ts">
import { ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import type { AccountVO, PaymentAccountVO } from '@ceams/types'
import { formatMoney } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { accountStatusLabels, accountTypeLabels, allocationStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

type Tab = 'account' | 'paymentAccount'

const activeTab = ref<Tab>('account')
const accounts = ref<AccountVO[]>([])
const paymentAccounts = ref<PaymentAccountVO[]>([])
const page = ref(1)
const total = ref(0)
const size = 20
const loading = ref(false)

async function load(reset = false): Promise<void> {
  if (reset) page.value = 1
  if (loading.value) return
  loading.value = true
  try {
    if (activeTab.value === 'account') {
      const res = await apiClient.payment.accountPage({ page: page.value, size })
      accounts.value = reset ? res.records : accounts.value.concat(res.records)
      total.value = res.total
    } else {
      const res = await apiClient.payment.paymentAccountPage({ page: page.value, size })
      paymentAccounts.value = reset ? res.records : paymentAccounts.value.concat(res.records)
      total.value = res.total
    }
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载账目失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function switchTab(tab: Tab): void {
  if (activeTab.value === tab) return
  activeTab.value = tab
  void load(true)
}

onShow(() => {
  void load(true)
})

onReachBottom(() => {
  const current = activeTab.value === 'account' ? accounts.value.length : paymentAccounts.value.length
  if (loading.value || current >= total.value) return
  page.value += 1
  void load()
})
</script>

<template>
  <view class="page">
    <view class="tabs">
      <view :class="['tab', activeTab === 'account' ? 'tab--active' : '']" @click="switchTab('account')">
        <text>收支流水</text>
      </view>
      <view
        :class="['tab', activeTab === 'paymentAccount' ? 'tab--active' : '']"
        @click="switchTab('paymentAccount')"
      >
        <text>付款账户</text>
      </view>
    </view>

    <template v-if="activeTab === 'account'">
      <view v-if="accounts.length === 0" class="empty">暂无收支流水</view>
      <view v-for="item in accounts" :key="item.id" class="card">
        <view class="card__row">
          <text class="card__title">{{ accountTypeLabels[item.type] }}</text>
          <text class="card__tag">{{ accountStatusLabels[item.status] }}</text>
        </view>
        <text class="card__line">金额：{{ formatMoney(item.amount) }}</text>
        <text class="card__line">交易日期：{{ item.transactionDate }}</text>
        <text class="card__line">分摊状态：{{ allocationStatusLabels[item.allocationStatus] }}</text>
        <text class="card__line">描述：{{ item.description || '-' }}</text>
      </view>
    </template>

    <template v-else>
      <view v-if="paymentAccounts.length === 0" class="empty">暂无付款账户</view>
      <view v-for="item in paymentAccounts" :key="item.id" class="card">
        <text class="card__title">{{ item.name }}</text>
        <text class="card__line">类型：{{ item.type }}</text>
        <text class="card__line">余额：{{ formatMoney(item.balance) }}</text>
        <text class="card__line">描述：{{ item.description || '-' }}</text>
      </view>
    </template>
  </view>
</template>

<style scoped>
.page {
  padding: 24rpx 32rpx;
}
.tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  background-color: #ffffff;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #6b7280;
}
.tab--active {
  background-color: #2563eb;
  color: #ffffff;
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
</style>
