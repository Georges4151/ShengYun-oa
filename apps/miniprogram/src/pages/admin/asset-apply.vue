<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { AssetVO } from '@ceams/types'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'
import { assetCategoryLabels, assetStatusLabels } from '../../utils/labels'

interface ValueEvent {
  detail?: { value?: string }
}

function eventValue(event: unknown): string {
  const detail = (event as ValueEvent).detail
  return detail && typeof detail.value === 'string' ? detail.value : ''
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

const loading = ref(false)
const submitting = ref(false)
const assets = ref<AssetVO[]>([])
const activeAsset = ref<AssetVO | null>(null)
const borrowDate = ref(today())
const expectedReturnDate = ref('')
const remark = ref('')

async function load(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    const res = await apiClient.admin.assetPage({ page: 1, size: 50, status: 'IN_STOCK' })
    assets.value = res.records
    if (activeAsset.value && !assets.value.some((item) => item.id === activeAsset.value?.id)) {
      activeAsset.value = null
    }
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载可领用资产失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function selectAsset(asset: AssetVO): void {
  activeAsset.value = asset
  borrowDate.value = today()
  expectedReturnDate.value = ''
  remark.value = ''
}

async function submit(): Promise<void> {
  const asset = activeAsset.value
  if (!asset) {
    uni.showToast({ title: '请先选择要领用的资产', icon: 'none' })
    return
  }
  if (!borrowDate.value) {
    uni.showToast({ title: '请选择领用日期', icon: 'none' })
    return
  }
  if (expectedReturnDate.value && expectedReturnDate.value < borrowDate.value) {
    uni.showToast({ title: '预计归还日期不能早于领用日期', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await apiClient.admin.assetBorrow(asset.id, {
      borrowDate: borrowDate.value,
      expectedReturnDate: expectedReturnDate.value || undefined,
      remark: remark.value.trim() || undefined
    })
    uni.showToast({ title: '领用申请已提交', icon: 'success' })
    activeAsset.value = null
    await load()
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '提交领用申请失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onShow(() => {
  void load()
})
</script>

<template>
  <view class="ceams-page">
    <view v-if="loading && assets.length === 0" class="state">加载中...</view>

    <view v-if="!loading && assets.length === 0" class="state">
      暂无可领用的在库资产
    </view>

    <view class="section-title">可领用资产</view>
    <view
      v-for="item in assets"
      :key="item.id"
      class="ceams-card asset"
      :class="{ 'asset--active': activeAsset?.id === item.id }"
      @click="selectAsset(item)"
    >
      <view class="asset__head">
        <text class="asset__name">{{ item.name }}</text>
        <text class="asset__status">{{ assetStatusLabels[item.status] }}</text>
      </view>
      <text class="asset__meta">编号 {{ item.assetNo }}</text>
      <text class="asset__meta">
        分类 {{ item.category ? assetCategoryLabels[item.category] : '-' }} · 地点
        {{ item.location || '-' }}
      </text>
      <text v-if="item.specification" class="asset__meta">规格 {{ item.specification }}</text>
    </view>

    <view v-if="activeAsset" class="ceams-card form">
      <text class="form__title">领用申请：{{ activeAsset.name }}</text>

      <view class="field">
        <text class="field__label">领用日期</text>
        <picker mode="date" :value="borrowDate" @change="borrowDate = eventValue($event)">
          <view class="field__value">{{ borrowDate || '请选择' }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="field__label">预计归还</text>
        <picker mode="date" :value="expectedReturnDate" @change="expectedReturnDate = eventValue($event)">
          <view class="field__value">{{ expectedReturnDate || '请选择（可选）' }}</view>
        </picker>
      </view>

      <view class="field field--column">
        <text class="field__label">领用说明</text>
        <textarea
          class="field__textarea"
          :value="remark"
          placeholder="领用说明（可选）"
          maxlength="200"
          @input="remark = eventValue($event)"
        />
      </view>

      <button class="ceams-btn ceams-btn--primary" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中...' : '提交领用申请' }}
      </button>
      <text class="form__hint">提交后进入审批流程，可在待办中心查看进度</text>
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

.section-title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.asset {
  margin-bottom: var(--ceams-space-md);
}

.asset--active {
  border: var(--ceams-border-width) solid var(--ceams-color-primary);
}

.asset__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.asset__name {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.asset__status {
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-mini);
}

.asset__meta {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
}

.form {
  margin-top: var(--ceams-space-lg);
}

.form__title {
  display: block;
  margin-bottom: var(--ceams-space-sm);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ceams-space-sm) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.field--column {
  flex-direction: column;
  align-items: flex-start;
}

.field__label {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.field__value {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

.field__textarea {
  width: 100%;
  min-height: 120px;
  margin-top: var(--ceams-space-xs);
  padding: var(--ceams-space-sm);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  box-sizing: border-box;
}

.form__hint {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}
</style>
