<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { AssetVO } from '@ceams/types'
import { apiClient } from '../../platform/http'
import SchemaForm from '../../components/schema-form/schema-form.vue'
import type { FormModel } from '../../schemas/form-schema'
import { createFormModel, getFormSchema } from '../../schemas/form-schema'
import { apiErrorMessage } from '../../utils/error'
import { assetCategoryLabels, assetStatusLabels } from '../../utils/labels'

/** 资产领用：资产选择器留在页面级（需状态过滤），其余字段由 schema 渲染 */
const schema = getFormSchema('ADMIN_ASSET_BORROW')
if (!schema) {
  throw new Error('缺少 ADMIN_ASSET_BORROW 表单 schema')
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

/** 每次选择资产后重置表单，领用日期默认今天 */
function defaultForm(): FormModel {
  return { ...createFormModel(schema!), borrowDate: today() }
}

const loading = ref(false)
const submitting = ref(false)
const assets = ref<AssetVO[]>([])
const activeAsset = ref<AssetVO | null>(null)
const form = ref<FormModel>(defaultForm())
const formRef = ref<InstanceType<typeof SchemaForm> | null>(null)

function text(key: string): string {
  return String(form.value[key] ?? '').trim()
}

function optional(key: string): string | undefined {
  const value = text(key)
  return value || undefined
}

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
  form.value = defaultForm()
}

async function submit(): Promise<void> {
  const asset = activeAsset.value
  if (!asset) {
    uni.showToast({ title: '请选择要领用的资产', icon: 'none' })
    return
  }
  const error = formRef.value?.validate()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }
  if (text('expectedReturnDate') && text('expectedReturnDate') < text('borrowDate')) {
    uni.showToast({ title: '预计归还日期不能早于领用日期', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await apiClient.admin.assetBorrow(asset.id, {
      borrowDate: text('borrowDate'),
      expectedReturnDate: optional('expectedReturnDate'),
      remark: optional('remark')
    })
    uni.showToast({ title: '已提交审批', icon: 'success' })
    activeAsset.value = null
    form.value = defaultForm()
    setTimeout(() => uni.navigateTo({ url: '/pages/bpm/todo?tab=initiated' }), 500)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '提交失败'), icon: 'none' })
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
    <view v-if="!loading && assets.length === 0" class="state">暂无可领用的在库资产</view>

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

    <view v-if="activeAsset" class="form">
      <text class="form__title">领用申请：{{ activeAsset.name }}</text>
      <SchemaForm ref="formRef" v-model="form" :schema="schema" :disabled="submitting" />

      <button class="ceams-btn ceams-btn--primary form__submit" :disabled="submitting" @click="submit">
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

.form__submit {
  margin-top: var(--ceams-space-lg);
}

.form__hint {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}
</style>
