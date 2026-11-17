<script setup lang="ts">
import { ref } from 'vue'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'

interface ValueEvent {
  detail?: { value?: string }
}

interface ScanResult {
  result?: string
  errMsg?: string
}

function eventValue(event: unknown): string {
  const detail = (event as ValueEvent).detail
  return detail && typeof detail.value === 'string' ? detail.value : ''
}

const token = ref('')
const scanning = ref(false)
const submitting = ref(false)
const lastMessage = ref('')

function scan(): void {
  if (scanning.value) return
  scanning.value = true
  uni.scanCode({
    scanType: ['qrCode'],
    success: (res: ScanResult) => {
      const value = typeof res.result === 'string' ? res.result.trim() : ''
      if (!value) {
        uni.showToast({ title: '未识别到有效二维码', icon: 'none' })
        return
      }
      token.value = value
      void checkin()
    },
    fail: (err: ScanResult) => {
      if (err.errMsg && err.errMsg.includes('cancel')) return
      uni.showToast({ title: '扫码失败，请手动输入令牌', icon: 'none' })
    },
    complete: () => {
      scanning.value = false
    }
  })
}

async function checkin(): Promise<void> {
  const value = token.value.trim()
  if (!value) {
    uni.showToast({ title: '请输入访客核销令牌', icon: 'none' })
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await apiClient.admin.visitorCheckin({ token: value })
    lastMessage.value = '访客核销成功，已放行'
    uni.showModal({
      title: '核销成功',
      content: '访客已入场，请予以放行。',
      showCancel: false
    })
    token.value = ''
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '访客核销失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="ceams-page">
    <view class="ceams-card">
      <text class="title">访客入场核销</text>
      <text class="desc">扫描访客出示的通行二维码完成核销，二维码仅包含一次性核销令牌。</text>
      <button class="ceams-btn ceams-btn--primary scan-btn" :disabled="scanning" @click="scan">
        {{ scanning ? '扫码中...' : '扫描访客二维码' }}
      </button>
    </view>

    <view class="ceams-card manual">
      <text class="manual__title">手动输入令牌</text>
      <textarea
        class="manual__input"
        :value="token"
        placeholder="扫码失败时，可手动粘贴访客二维码中的核销令牌"
        maxlength="500"
        @input="token = eventValue($event)"
      />
      <button class="ceams-btn ceams-btn--primary" :disabled="submitting" @click="checkin">
        {{ submitting ? '核销中...' : '确认核销' }}
      </button>
    </view>

    <view v-if="lastMessage" class="ceams-card result">
      <text class="result__text">{{ lastMessage }}</text>
    </view>
  </view>
</template>

<style scoped>
.title {
  display: block;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-title);
  font-weight: var(--ceams-font-weight-semibold);
}

.desc {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: 1.6;
}

.scan-btn {
  margin-top: var(--ceams-space-lg);
}

.manual {
  margin-top: var(--ceams-space-lg);
}

.manual__title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.manual__input {
  width: 100%;
  min-height: 140px;
  margin-bottom: var(--ceams-space-md);
  padding: var(--ceams-space-sm);
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-md);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  box-sizing: border-box;
}

.result {
  margin-top: var(--ceams-space-lg);
}

.result__text {
  color: #16a34a;
  font-size: var(--ceams-font-size-caption);
}
</style>
