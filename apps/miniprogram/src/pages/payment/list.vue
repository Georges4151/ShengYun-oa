<script setup lang="ts">
import { computed, ref } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import type { AccountStatus, AccountVO, PaymentAccountVO } from '@ceams/types'
import { formatDate } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { accountStatusLabels, accountTypeLabels, allocationStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

type Tab = 'account' | 'paymentAccount'
type TagType = 'pending' | 'approved' | 'rejected' | 'verified' | 'paid' | 'neutral'

/** 流水状态 -> 状态标签语义（仅用于展示配色） */
const accountStatusTagTypes: Record<AccountStatus, TagType> = {
  DRAFT: 'neutral',
  CONFIRMED: 'approved',
  CANCELLED: 'rejected'
}

const activeTab = ref<Tab>('account')
const accounts = ref<AccountVO[]>([])
const paymentAccounts = ref<PaymentAccountVO[]>([])
const page = ref(1)
const total = ref(0)
const size = 20
const loading = ref(false)

/** 已加载流水的收入合计（仅用于展示，不新增请求） */
const incomeTotal = computed<number>(() =>
  accounts.value.reduce(
    (sum, item) => (item.type === 'INCOME' ? sum + (Number(item.amount) || 0) : sum),
    0
  )
)

/** 已加载流水的支出合计（仅用于展示，不新增请求） */
const expenseTotal = computed<number>(() =>
  accounts.value.reduce(
    (sum, item) => (item.type === 'EXPENSE' ? sum + (Number(item.amount) || 0) : sum),
    0
  )
)

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
  <view class="ceams-page payment-list">
    <view v-if="activeTab === 'account'" class="ceams-card payment-list__summary">
      <view class="payment-list__summary-head">
        <view class="ceams-section-title payment-list__summary-title">账目汇总</view>
        <text class="payment-list__summary-hint">已加载 {{ accounts.length }} 条</text>
      </view>
      <view class="payment-list__summary-body">
        <view class="payment-list__summary-cell">
          <text class="payment-list__summary-label">总收入</text>
          <amount-text
            :value="incomeTotal"
            size="md"
            color="var(--ceams-color-success)"
          />
        </view>
        <view class="payment-list__summary-divider" />
        <view class="payment-list__summary-cell">
          <text class="payment-list__summary-label">总支出</text>
          <amount-text
            :value="expenseTotal"
            size="md"
            color="var(--ceams-color-danger)"
          />
        </view>
      </view>
    </view>

    <view class="payment-list__tabs">
      <view
        :class="[
          'payment-list__tab',
          'payment-list__tab--lead',
          activeTab === 'account' ? 'payment-list__tab--active' : ''
        ]"
        @click="switchTab('account')"
      >
        <text>收支流水</text>
      </view>
      <view
        :class="[
          'payment-list__tab',
          activeTab === 'paymentAccount' ? 'payment-list__tab--active' : ''
        ]"
        @click="switchTab('paymentAccount')"
      >
        <text>付款账户</text>
      </view>
    </view>

    <template v-if="activeTab === 'account'">
      <view v-if="accounts.length === 0" class="ceams-card">
        <empty-state title="暂无账目记录" />
      </view>
      <view v-else class="ceams-card payment-list__ledger">
        <view
          v-for="(item, index) in accounts"
          :key="item.id"
          :class="[
            'ceams-list-item',
            'payment-list__row',
            index === accounts.length - 1 ? 'ceams-list-item--last' : ''
          ]"
        >
          <view class="payment-list__row-main">
            <view class="payment-list__row-head">
              <text class="payment-list__date">{{ formatDate(item.transactionDate) }}</text>
              <status-tag
                :label="accountStatusLabels[item.status]"
                :type="accountStatusTagTypes[item.status]"
              />
            </view>
            <text class="payment-list__desc ceams-ellipsis">{{ item.description || '-' }}</text>
            <text class="payment-list__allocation">
              {{ allocationStatusLabels[item.allocationStatus] }}
            </text>
          </view>
          <view class="payment-list__row-amount">
            <amount-text
              :value="item.amount"
              size="md"
              :signed="item.type === 'INCOME'"
              :color="
                item.type === 'INCOME'
                  ? 'var(--ceams-color-success)'
                  : 'var(--ceams-color-danger)'
              "
            />
            <text class="payment-list__type">{{ accountTypeLabels[item.type] }}</text>
          </view>
        </view>
      </view>
    </template>

    <template v-else>
      <view v-if="paymentAccounts.length === 0" class="ceams-card">
        <empty-state title="暂无付款账户" />
      </view>
      <view v-else class="ceams-card payment-list__ledger">
        <view
          v-for="(item, index) in paymentAccounts"
          :key="item.id"
          :class="[
            'ceams-list-item',
            'payment-list__account',
            index === paymentAccounts.length - 1 ? 'ceams-list-item--last' : ''
          ]"
        >
          <view class="payment-list__account-head">
            <text class="payment-list__account-name">{{ item.name }}</text>
            <text class="payment-list__account-type">{{ item.type }}</text>
          </view>
          <view class="payment-list__account-foot">
            <text class="payment-list__account-desc ceams-ellipsis">
              {{ item.description || '-' }}
            </text>
            <amount-text :value="item.balance" size="md" />
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped>
.payment-list__summary {
  padding-bottom: var(--ceams-space-md);
}

.payment-list__summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-md);
}

/* 汇总标题沿用全局 .ceams-section-title */
.payment-list__summary-title {
  flex-shrink: 0;
  margin-right: var(--ceams-space-md);
}

.payment-list__summary-hint {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
}

.payment-list__summary-body {
  display: flex;
  align-items: stretch;
}

.payment-list__summary-cell {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.payment-list__summary-label {
  margin-bottom: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.payment-list__summary-divider {
  width: var(--ceams-border-width);
  margin: 0 var(--ceams-space-lg);
  background-color: var(--ceams-divider);
}

.payment-list__tabs {
  display: flex;
  margin-bottom: var(--ceams-space-md);
}

.payment-list__tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: var(--ceams-control-height-sm);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-body);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-md);
  box-shadow: var(--ceams-shadow-card);
}

.payment-list__tab--active {
  color: var(--ceams-text-inverse);
  font-weight: var(--ceams-font-weight-semibold);
  background-color: var(--ceams-color-primary);
}

.payment-list__tab--lead {
  margin-right: var(--ceams-space-sm);
}

.payment-list__ledger {
  padding-top: 0;
  padding-bottom: 0;
}

.payment-list__row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.payment-list__row-main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
}

.payment-list__row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.payment-list__date {
  margin-right: var(--ceams-space-sm);
  color: var(--ceams-text-primary);
  font-size: 28rpx;
  font-weight: var(--ceams-font-weight-semibold);
  letter-spacing: var(--ceams-letter-spacing-number);
}

.payment-list__desc {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
}

.payment-list__allocation {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-placeholder);
  font-size: 24rpx;
}

.payment-list__row-amount {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.payment-list__type {
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
}

.payment-list__account-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.payment-list__account-name {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-sm);
  overflow: hidden;
  color: var(--ceams-text-primary);
  font-size: 28rpx;
  font-weight: var(--ceams-font-weight-semibold);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.payment-list__account-type {
  flex-shrink: 0;
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
}

.payment-list__account-foot {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: var(--ceams-space-2xs);
}

.payment-list__account-desc {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
}
</style>
