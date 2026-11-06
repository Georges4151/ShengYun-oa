<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import type { ReceiptVO } from '@ceams/types'
import { formatMoney } from '@ceams/utils'
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

const id = ref('')
const detail = ref<ReceiptVO | null>(null)
const comment = ref('')
const submitting = ref(false)

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
  <view class="page">
    <view v-if="detail" class="panel">
      <view class="panel__title">
        <text class="panel__name">{{ detail.receiptNumber }}</text>
        <text class="panel__tag">{{ receiptApprovalStatusLabels[detail.approvalStatus] }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">报销人</text>
        <text class="kv__value">{{ detail.reporterName || '-' }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">类型</text>
        <text class="kv__value">{{ receiptTypeLabels[detail.type] }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">金额</text>
        <text class="kv__value">{{ formatMoney(detail.amount) }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">已付金额</text>
        <text class="kv__value">{{ formatMoney(detail.paidAmount) }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">核验状态</text>
        <text class="kv__value">{{ verificationStatusLabels[detail.verificationStatus] }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">付款状态</text>
        <text class="kv__value">{{ paymentStatusLabels[detail.paymentStatus] }}</text>
      </view>
      <view class="kv">
        <text class="kv__label">描述</text>
        <text class="kv__value">{{ detail.description || '-' }}</text>
      </view>
    </view>

    <button class="btn btn--ghost" @click="previewAttachment">预览票据附件</button>

    <template v-if="detail && detail.approvalStatus === 'PENDING'">
      <text class="section">审批意见</text>
      <textarea v-model="comment" class="comment" placeholder="请输入审批意见（可选）" maxlength="500" />
      <view class="actions">
        <button class="btn btn--primary" :disabled="submitting" @click="doApproval('approve')">通过</button>
        <button class="btn btn--danger" :disabled="submitting" @click="doApproval('reject')">驳回</button>
      </view>
    </template>

    <button class="btn btn--ghost" @click="takePhoto">拍照上传票据</button>
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
  max-width: 420rpx;
  text-align: right;
}
.section {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  margin: 24rpx 0 16rpx;
}
.comment {
  width: 100%;
  height: 160rpx;
  padding: 20rpx;
  background-color: #ffffff;
  border: 1rpx solid #e5e7eb;
  border-radius: 12rpx;
  box-sizing: border-box;
}
.actions {
  display: flex;
  gap: 20rpx;
  margin: 24rpx 0;
}
.btn {
  border-radius: 12rpx;
  font-size: 30rpx;
}
.btn--primary {
  background-color: #2563eb;
  color: #ffffff;
}
.btn--danger {
  background-color: #dc2626;
  color: #ffffff;
}
.btn--ghost {
  background-color: #ffffff;
  color: #2563eb;
  border: 1rpx solid #2563eb;
  margin-bottom: 20rpx;
}
</style>
