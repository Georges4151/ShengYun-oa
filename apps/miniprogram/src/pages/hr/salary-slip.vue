<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { SalarySlipVO } from '@ceams/types'
import { formatDateTime, formatMoney } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'
import { hrSalarySlipReadStatusLabels } from '../../utils/labels'

const slips = ref<SalarySlipVO[]>([])
const loading = ref(false)
const selected = ref<SalarySlipVO | null>(null)

/** 工资条按月倒序展示（后端已排序，这里再做一次兜底）。 */
function sortByMonthDesc(list: SalarySlipVO[]): SalarySlipVO[] {
  return [...list].sort((a, b) => b.calcMonth.localeCompare(a.calcMonth))
}

/** 进入列表即调用 slips/my，后端会在该接口内自动把未读工资条置为已读。 */
async function load(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    slips.value = sortByMonthDesc(await apiClient.hr.mySalarySlips())
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载工资条失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function openDetail(item: SalarySlipVO): void {
  selected.value = item
  uni.pageScrollTo({ scrollTop: 0, duration: 0 })
}

function backToList(): void {
  selected.value = null
}

onShow(() => {
  selected.value = null
  void load()
})
</script>

<template>
  <view class="ceams-page">
    <view v-if="loading && slips.length === 0" class="state">加载中...</view>

    <empty-state
      v-if="!loading && slips.length === 0"
      title="暂无工资条"
      description="HR 发布工资条后可在此查看每月工资明细"
      icon="¥"
    />

    <!-- 工资条明细 -->
    <template v-if="selected">
      <view class="detail-bar">
        <text class="detail-bar__back" @click="backToList">‹ 返回列表</text>
        <text class="detail-bar__month">{{ selected.calcMonth }} 工资条</text>
      </view>

      <view class="ceams-card">
        <view class="summary">
          <text class="summary__label">实发工资</text>
          <amount-text :value="selected.netPay" size="lg" />
          <text class="summary__sub">
            应发 {{ formatMoney(selected.grossPay) }} · 个税 {{ formatMoney(selected.tax) }}
          </text>
        </view>

        <view class="ceams-list-item kv">
          <text class="kv__label">基本工资</text>
          <amount-text :value="selected.baseSalary" size="sm" />
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">岗位工资</text>
          <amount-text :value="selected.postSalary" size="sm" />
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">绩效工资（系数 {{ selected.performanceCoefficient }}）</text>
          <amount-text :value="selected.performanceAmount" size="sm" />
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">固定补贴</text>
          <amount-text :value="selected.allowance" size="sm" />
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">考勤扣款</text>
          <amount-text :value="selected.attendanceDeduction" size="sm" />
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">社保</text>
          <amount-text :value="selected.socialInsurance" size="sm" />
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">公积金</text>
          <amount-text :value="selected.housingFund" size="sm" />
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">个人所得税</text>
          <amount-text :value="selected.tax" size="sm" />
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">考勤（出勤 / 缺勤 / 迟到）</text>
          <text class="kv__value">
            {{ selected.attendanceDays }} / {{ selected.absentDays }} / {{ selected.lateCount ?? 0 }}
          </text>
        </view>
        <view class="ceams-list-item kv">
          <text class="kv__label">发布时间</text>
          <text class="kv__value">{{ formatDateTime(selected.publishedAt) }}</text>
        </view>
        <view class="ceams-list-item kv kv--last">
          <text class="kv__label">阅读状态</text>
          <text class="kv__value">{{ hrSalarySlipReadStatusLabels[selected.readStatus] }}</text>
        </view>
      </view>
    </template>

    <!-- 工资条列表 -->
    <view v-else-if="slips.length > 0" class="ceams-card">
      <text class="section-title">我的工资条</text>
      <view
        v-for="item in slips"
        :key="item.id"
        class="row"
        hover-class="row--pressed"
        @click="openDetail(item)"
      >
        <view class="row__main">
          <text class="row__title">{{ item.calcMonth }} 工资条</text>
          <text class="row__sub">
            应发 {{ formatMoney(item.grossPay) }} · {{ formatDateTime(item.publishedAt) }}
          </text>
        </view>
        <view class="row__right">
          <amount-text :value="item.netPay" size="md" />
          <status-tag
            :label="hrSalarySlipReadStatusLabels[item.readStatus]"
            :type="item.readStatus === 'READ' ? 'approved' : 'pending'"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}

.detail-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-md);
}

.detail-bar__back {
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-caption);
}

.detail-bar__month {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--ceams-space-lg) 0 var(--ceams-space-md);
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.summary__label {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.summary__sub {
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
}

.kv {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kv--last {
  border-bottom: none;
}

.kv__label {
  flex: 1;
  min-width: 0;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.kv__value {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  text-align: right;
}

.section-title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ceams-space-sm) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.row:last-child {
  border-bottom: none;
}

.row--pressed {
  background-color: var(--ceams-bg-subtle);
}

.row__main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
}

.row__title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

.row__sub {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.row__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
</style>
