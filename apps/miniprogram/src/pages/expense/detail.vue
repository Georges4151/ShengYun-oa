<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import type { ReceiptApprovalStatus, ReceiptVO } from '@ceams/types'
import { formatDate } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { chooseImage, uploadFile } from '../../platform/upload'
import { previewFile } from '../../platform/download'
import { API_BASE_URL, ATTACHMENT_UPLOAD_URL } from '../../platform/config'
import { getAccessToken } from '../../platform/auth'
import {
  paymentStatusLabels,
  receiptApprovalStatusLabels,
  receiptTypeLabels,
  verificationStatusLabels
} from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

/** 审批状态 → 状态标签语义（仅用于展示配色） */
type TagType = 'pending' | 'approved' | 'rejected' | 'neutral'

const approvalTagTypes: Record<ReceiptApprovalStatus, TagType> = {
  DRAFT: 'neutral',
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected'
}

const id = ref('')
const detail = ref<ReceiptVO | null>(null)
const comment = ref('')
const submitting = ref(false)

/** 审批中才展示审批意见与底部操作条 */
const hasPendingActions = computed<boolean>(() => detail.value?.approvalStatus === 'PENDING')

/** 附件展示名：取路径最后一段 */
const attachmentName = computed<string>(() => {
  const path = detail.value?.filePath ?? ''
  if (!path) return ''
  const name = path.split('/').pop() ?? ''
  return name.length > 0 ? name : path
})

/** 附件类型：取扩展名，仅用于展示标识 */
const attachmentExt = computed<string>(() => {
  const path = detail.value?.filePath ?? ''
  const dot = path.lastIndexOf('.')
  const slash = path.lastIndexOf('/')
  if (dot < 0 || dot <= slash) return '文件'
  const ext = path.slice(dot + 1).toUpperCase()
  return ext.length > 0 ? ext : '文件'
})

async function load(): Promise<void> {
  try {
    detail.value = await apiClient.expense.receiptDetail(id.value)
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载报销详情失败'), icon: 'none' })
  }
}

async function doApproval(action: 'approve' | 'reject'): Promise<void> {
  if (submitting.value) return
  submitting.value = true
  try {
    const body = { comment: comment.value || null }
    if (action === 'approve') {
      await apiClient.expense.receiptApprove(id.value, body)
      uni.showToast({ title: '审批通过', icon: 'success' })
    } else {
      await apiClient.expense.receiptReject(id.value, body)
      uni.showToast({ title: '已驳回', icon: 'success' })
    }
    await load()
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '审批操作失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

async function takePhoto(): Promise<void> {
  try {
    const paths = await chooseImage(1)
    if (paths.length === 0) return
    const token = getAccessToken()
    const header = token ? { Authorization: `Bearer ${token}` } : undefined
    const uploaded = await uploadFile(ATTACHMENT_UPLOAD_URL, paths[0], { header })
    await apiClient.expense.receiptUpdate(id.value, { filePath: uploaded || null })
    uni.showToast({ title: '票据已上传', icon: 'success' })
    await load()
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '上传失败'), icon: 'none' })
  }
}

function previewAttachment(): void {
  const filePath = detail.value?.filePath
  if (!filePath) {
    uni.showToast({ title: '暂无附件', icon: 'none' })
    return
  }
  previewFile(`${API_BASE_URL}${filePath}`).catch((err: unknown) => {
    uni.showToast({ title: apiErrorMessage(err, '预览失败'), icon: 'none' })
  })
}

onLoad((query) => {
  id.value = typeof query?.id === 'string' ? query.id : ''
  void load()
})
</script>

<template>
  <view class="ceams-page" :class="{ 'page--with-bar': hasPendingActions }">
    <template v-if="detail">
      <view class="bill">
        <view class="bill__head">
          <view class="bill__main">
            <text class="bill__label">票据编号</text>
            <text class="bill__number">{{ detail.receiptNumber }}</text>
          </view>
          <status-tag
            :label="receiptApprovalStatusLabels[detail.approvalStatus]"
            :type="approvalTagTypes[detail.approvalStatus]"
          />
        </view>
        <view class="bill__amount">
          <text class="bill__amount-label">报销金额</text>
          <amount-text :value="detail.amount" size="lg" color="var(--ceams-color-primary-dark)" />
        </view>
        <text class="bill__hint">{{ receiptTypeLabels[detail.type] }}票据 · 报销日期 {{ formatDate(detail.receiptDate) }}</text>
      </view>

      <view class="ceams-card">
        <view class="section">
          <view class="section__bar"></view>
          <text class="section__title">基础信息</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">报销人</text>
          <text class="row__value">{{ detail.reporterName || '-' }}</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">报销日期</text>
          <text class="row__value">{{ formatDate(detail.receiptDate) }}</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">票据类型</text>
          <text class="row__value">{{ receiptTypeLabels[detail.type] }}</text>
        </view>
        <view class="ceams-list-item ceams-list-item--last row">
          <text class="row__label">说明</text>
          <text class="row__value">{{ detail.description || '-' }}</text>
        </view>
      </view>

      <view class="ceams-card">
        <view class="section">
          <view class="section__bar"></view>
          <text class="section__title">金额与核验</text>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">报销金额</text>
          <view class="row__value row__value--amount">
            <amount-text :value="detail.amount" size="sm" />
          </view>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">已支付金额</text>
          <view class="row__value row__value--amount">
            <amount-text :value="detail.paidAmount" size="sm" />
          </view>
        </view>
        <view class="ceams-list-item row">
          <text class="row__label">核验状态</text>
          <text class="row__value">{{ verificationStatusLabels[detail.verificationStatus] }}</text>
        </view>
        <view class="ceams-list-item ceams-list-item--last row">
          <text class="row__label">付款状态</text>
          <text class="row__value">{{ paymentStatusLabels[detail.paymentStatus] }}</text>
        </view>
      </view>

      <view class="ceams-card">
        <view class="attach__head">
          <view class="section attach__section">
            <view class="section__bar"></view>
            <text class="section__title">票据附件</text>
          </view>
          <button class="ceams-btn ceams-btn--ghost attach__btn" @click="takePhoto">拍照上传</button>
        </view>
        <text class="attach__hint">支持 jpg / png / pdf，单文件不超过 20MB</text>

        <view v-if="detail.filePath" class="attach__item" @click="previewAttachment">
          <view class="attach__icon">▤</view>
          <view class="attach__main">
            <text class="attach__name ceams-ellipsis">{{ attachmentName }}</text>
            <text class="attach__meta">{{ attachmentExt }} · 点击预览</text>
          </view>
          <text class="attach__arrow">›</text>
        </view>
        <view v-else class="attach__empty">
          <text class="attach__empty-text">暂无附件，拍照上传后可在此预览</text>
        </view>
      </view>

      <view v-if="hasPendingActions" class="ceams-card">
        <view class="section">
          <view class="section__bar"></view>
          <text class="section__title">审批意见</text>
        </view>
        <textarea
          v-model="comment"
          class="comment"
          placeholder="请输入审批意见（可选）"
          maxlength="500"
        />
      </view>
    </template>

    <view v-else class="state">加载中...</view>

    <view v-if="hasPendingActions" class="action ceams-safe-bottom">
      <button
        class="ceams-btn action__reject"
        :class="{ 'action__reject--disabled': submitting }"
        :disabled="submitting"
        @click="doApproval('reject')"
      >
        驳回
      </button>
      <button
        class="ceams-btn ceams-btn--primary action__approve"
        :class="{ 'action__approve--disabled': submitting }"
        :disabled="submitting"
        @click="doApproval('approve')"
      >
        通过
      </button>
    </view>
  </view>
</template>

<style scoped>
.page--with-bar {
  padding-bottom: calc(var(--ceams-control-height) + var(--ceams-space-2xl));
}

.bill {
  margin-bottom: var(--ceams-space-md);
  padding: var(--ceams-space-lg);
  background-color: var(--ceams-color-primary-tint);
  border: var(--ceams-border-width) solid var(--ceams-color-primary-tint-strong);
  border-radius: var(--ceams-radius-card);
  box-sizing: border-box;
}

.bill__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.bill__main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-md);
}

.bill__label {
  display: block;
  color: var(--ceams-color-primary-deep);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-tight);
  letter-spacing: var(--ceams-letter-spacing-number);
}

.bill__number {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-bold);
  line-height: var(--ceams-line-height-tight);
}

.bill__amount {
  display: flex;
  align-items: flex-end;
  margin-top: var(--ceams-space-lg);
}

.bill__amount-label {
  margin-bottom: var(--ceams-space-2xs);
  margin-right: var(--ceams-space-xs);
  color: var(--ceams-color-primary-deep);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-tight);
}

.bill__hint {
  display: block;
  margin-top: var(--ceams-space-sm);
  color: var(--ceams-color-primary-deep);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.section {
  display: flex;
  align-items: center;
  margin-bottom: var(--ceams-space-xs);
}

.section__bar {
  width: 6rpx;
  height: var(--ceams-space-lg);
  margin-right: var(--ceams-space-xs);
  background-color: var(--ceams-color-primary);
  border-radius: var(--ceams-radius-xs);
}

.section__title {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.row__label {
  flex-shrink: 0;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.row__value {
  flex: 1;
  margin-left: var(--ceams-space-lg);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
  text-align: right;
}

.row__value--amount {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.attach__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.attach__section {
  margin-bottom: 0;
}

.attach__btn {
  flex-shrink: 0;
  width: 200rpx;
  height: var(--ceams-control-height-sm);
  margin: 0;
  padding: 0 var(--ceams-space-md);
  font-size: var(--ceams-font-size-caption);
}

.attach__hint {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.attach__item {
  display: flex;
  align-items: center;
  margin-top: var(--ceams-space-sm);
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-subtle);
  border: var(--ceams-border-width) solid var(--ceams-divider);
  border-radius: var(--ceams-radius-md);
}

.attach__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 64rpx;
  height: 64rpx;
  margin-right: var(--ceams-space-sm);
  color: var(--ceams-color-primary);
  background-color: var(--ceams-color-primary-tint);
  border-radius: var(--ceams-radius-sm);
  font-size: var(--ceams-font-size-heading);
  line-height: var(--ceams-line-height-tight);
}

.attach__main {
  flex: 1;
  min-width: 0;
}

.attach__name {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.attach__meta {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.attach__arrow {
  flex-shrink: 0;
  margin-left: var(--ceams-space-xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-heading);
  line-height: var(--ceams-line-height-tight);
}

.attach__empty {
  margin-top: var(--ceams-space-sm);
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
}

.attach__empty-text {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.comment {
  width: 100%;
  height: 160rpx;
  padding: var(--ceams-space-md);
  background-color: var(--ceams-bg-subtle);
  border: var(--ceams-border-width) solid var(--ceams-border);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
  box-sizing: border-box;
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

.action__reject--disabled,
.action__approve--disabled {
  opacity: 0.6;
}

.state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}
</style>
