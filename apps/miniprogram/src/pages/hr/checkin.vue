<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow, onUnload } from '@dcloudio/uni-app'
import type { CheckinVO, HrCheckinStatus, HrCheckinType } from '@ceams/types'
import { formatDate, formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import SchemaForm from '../../components/schema-form/schema-form.vue'
import type { FormModel } from '../../schemas/form-schema'
import { createFormModel, getFormSchema } from '../../schemas/form-schema'
import { apiErrorMessage, isEmployeeUnbound } from '../../utils/error'
import { hrCheckinStatusLabels, hrCheckinTypeLabels } from '../../utils/labels'

type TagType = 'pending' | 'approved' | 'rejected' | 'neutral'

/** 打卡状态 → 状态标签语义（仅用于展示配色） */
const statusTagTypes: Record<HrCheckinStatus, TagType> = {
  NORMAL: 'approved',
  LATE: 'pending',
  EARLY: 'pending',
  ABSENT: 'rejected'
}

/** 补卡面板字段由 schema 渲染，打卡类型仍映射到后端枚举 */
const supplementSchema = getFormSchema('HR_CHECKIN_SUPPLEMENT')
if (!supplementSchema) {
  throw new Error('缺少 HR_CHECKIN_SUPPLEMENT 表单 schema')
}

const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const now = ref(new Date())
const month = ref(monthOf(now.value))
const checkins = ref<CheckinVO[]>([])
const loading = ref(false)
const unbound = ref(false)
const clocking = ref<HrCheckinType | ''>('')

const showSupplement = ref(false)
const supplementSubmitting = ref(false)
const supplementForm = ref<FormModel>(createFormModel(supplementSchema))
const supplementFormRef = ref<InstanceType<typeof SchemaForm> | null>(null)

const timer = setInterval(() => {
  now.value = new Date()
  const current = monthOf(now.value)
  if (current !== month.value) {
    month.value = current
    void loadCheckins()
  }
}, 1000)

const today = computed<string>(() => formatDate(now.value))
const dateTimeText = computed<string>(() => formatDateTime(now.value))
const weekday = computed<string>(() => weekdays[now.value.getDay()] ?? '')
const monthLabel = computed<string>(() => {
  const [year, value] = month.value.split('-')
  return `${year}年${Number(value)}月`
})
const todayCheckins = computed<CheckinVO[]>(() =>
  checkins.value.filter((item) => item.checkinDate === today.value)
)
const checkedIn = computed<boolean>(() => todayCheckins.value.some((item) => item.checkinType === 'IN'))
const checkedOut = computed<boolean>(() =>
  todayCheckins.value.some((item) => item.checkinType === 'OUT')
)

function pad(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

function monthOf(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`
}

function timeOf(date: Date): string {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function timeOnly(value: string): string {
  return formatDateTime(value).slice(11)
}

async function loadCheckins(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    checkins.value = await apiClient.hr.myCheckins(month.value)
    unbound.value = false
  } catch (err) {
    if (isEmployeeUnbound(err)) {
      unbound.value = true
      checkins.value = []
      return
    }
    uni.showToast({ title: apiErrorMessage(err, '加载打卡记录失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function readLocation(): Promise<{ latitude?: string; longitude?: string }> {
  return new Promise((resolve) => {
    uni.getLocation({
      type: 'gcj02',
      success: (res) => resolve({ latitude: String(res.latitude), longitude: String(res.longitude) }),
      fail: () => resolve({})
    })
  })
}

/** 已连接 WiFi：微信小程序需授权，失败或其他端一律静默跳过并传空字符串。 */
function readWifi(): Promise<{ ssid: string; bssid: string }> {
  return new Promise((resolve) => {
    // #ifdef MP-WEIXIN
    uni.getConnectedWifi({
      success: (res) => resolve({ ssid: res.wifi.SSID || '', bssid: res.wifi.BSSID || '' }),
      fail: () => resolve({ ssid: '', bssid: '' })
    })
    // #endif
    // #ifndef MP-WEIXIN
    resolve({ ssid: '', bssid: '' })
    // #endif
  })
}

async function clock(type: HrCheckinType): Promise<void> {
  if (clocking.value) return
  clocking.value = type
  try {
    // 定位、WiFi 均为尽力采集：失败时不上报对应字段，不阻塞打卡。
    const location = await readLocation()
    const wifi = await readWifi()
    await apiClient.hr.checkinClock({
      checkinType: type,
      latitude: location.latitude,
      longitude: location.longitude,
      wifiSsid: wifi.ssid,
      wifiBssid: wifi.bssid,
      source: 'MINI'
    })
    uni.showToast({ title: '打卡成功', icon: 'success' })
    await loadCheckins()
  } catch (err) {
    if (isEmployeeUnbound(err)) {
      unbound.value = true
      uni.showToast({ title: '请联系 HR 绑定员工档案', icon: 'none' })
    } else {
      uni.showToast({ title: apiErrorMessage(err, '打卡失败'), icon: 'none' })
    }
  } finally {
    clocking.value = ''
  }
}

/** 补卡表单重置：日期默认今天、时间默认当前时刻 */
function resetSupplement(): void {
  supplementForm.value = {
    ...createFormModel(supplementSchema!),
    checkinDate: today.value,
    checkinTime: timeOf(now.value)
  }
}

function toggleSupplement(): void {
  showSupplement.value = !showSupplement.value
  if (showSupplement.value) {
    resetSupplement()
  }
}

async function submitSupplement(): Promise<void> {
  if (supplementSubmitting.value) return
  const error = supplementFormRef.value?.validate()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }
  const form = supplementForm.value
  const checkinDate = String(form.checkinDate ?? '').trim()
  const checkinClock = String(form.checkinTime ?? '').trim()
  const checkinType = (String(form.checkinType ?? '').trim() || 'IN') as HrCheckinType
  const reason = String(form.reason ?? '').trim()
  supplementSubmitting.value = true
  try {
    await apiClient.hr.checkinSupplement({
      checkinDate,
      checkinTime: `${checkinDate}T${checkinClock}:00`,
      checkinType,
      reason
    })
    uni.showToast({ title: '补卡申请已提交', icon: 'success' })
    showSupplement.value = false
    await loadCheckins()
  } catch (err) {
    if (isEmployeeUnbound(err)) {
      unbound.value = true
      uni.showToast({ title: '请联系 HR 绑定员工档案', icon: 'none' })
    } else {
      uni.showToast({ title: apiErrorMessage(err, '补卡申请失败'), icon: 'none' })
    }
  } finally {
    supplementSubmitting.value = false
  }
}

onShow(() => {
  now.value = new Date()
  void loadCheckins()
})

onUnload(() => {
  clearInterval(timer)
})
</script>

<template>
  <view class="ceams-page">
    <view class="ceams-card clock">
      <text class="clock__time">{{ dateTimeText }}</text>
      <text class="clock__weekday">{{ weekday }}</text>
      <view v-if="!unbound && todayCheckins.length > 0" class="clock__status">
        <status-tag
          v-for="item in todayCheckins"
          :key="item.id"
          :label="`${hrCheckinTypeLabels[item.checkinType]} · ${hrCheckinStatusLabels[item.status]}`"
          :type="statusTagTypes[item.status]"
        />
      </view>
    </view>

    <empty-state
      v-if="unbound"
      title="未绑定员工档案"
      description="请联系 HR 绑定员工档案后再使用打卡功能"
      icon="!"
    />

    <template v-else>
      <view class="actions">
        <button
          class="ceams-btn ceams-btn--primary actions__btn"
          :disabled="clocking !== '' || checkedIn"
          @click="clock('IN')"
        >
          {{ clocking === 'IN' ? '打卡中...' : checkedIn ? '已打上班卡' : '上班打卡' }}
        </button>
        <button
          class="ceams-btn ceams-btn--ghost actions__btn"
          :disabled="clocking !== '' || checkedOut"
          @click="clock('OUT')"
        >
          {{ clocking === 'OUT' ? '打卡中...' : checkedOut ? '已打下班卡' : '下班打卡' }}
        </button>
      </view>
      <text class="actions__hint">打卡将记录定位与 WiFi 信息，定位失败可确认后继续</text>

      <view class="ceams-card">
        <text class="section-title">今日打卡</text>
        <text v-if="todayCheckins.length === 0" class="empty-text">今日暂无打卡记录</text>
        <view v-for="item in todayCheckins" :key="item.id" class="row">
          <view class="row__main">
            <text class="row__title">{{ hrCheckinTypeLabels[item.checkinType] }}打卡</text>
            <text class="row__sub">{{ timeOnly(item.checkinTime) }}</text>
          </view>
          <status-tag
            :label="hrCheckinStatusLabels[item.status]"
            :type="statusTagTypes[item.status]"
          />
        </view>
      </view>

      <view class="ceams-card">
        <text class="section-title">{{ monthLabel }}打卡记录</text>
        <text v-if="loading && checkins.length === 0" class="empty-text">加载中...</text>
        <text v-else-if="checkins.length === 0" class="empty-text">本月暂无打卡记录</text>
        <view v-for="item in checkins" :key="item.id" class="row">
          <view class="row__main">
            <text class="row__title">
              {{ item.checkinDate }} · {{ hrCheckinTypeLabels[item.checkinType] }}
            </text>
            <text class="row__sub">{{ timeOnly(item.checkinTime) }}</text>
          </view>
          <status-tag
            :label="hrCheckinStatusLabels[item.status]"
            :type="statusTagTypes[item.status]"
          />
        </view>
      </view>

      <view class="ceams-card">
        <view class="section-head">
          <text class="section-title section-title--flush">补卡申请</text>
          <text class="link" @click="toggleSupplement">{{ showSupplement ? '收起' : '去补卡' }}</text>
        </view>

        <text v-if="!showSupplement" class="empty-text">
          忘记打卡可提交补卡申请，审批通过后生效
        </text>

        <view v-else class="supplement">
          <SchemaForm
            ref="supplementFormRef"
            v-model="supplementForm"
            :schema="supplementSchema"
            :disabled="supplementSubmitting"
          />
          <view class="supplement__actions">
            <button
              class="ceams-btn ceams-btn--primary supplement__btn"
              :disabled="supplementSubmitting"
              @click="submitSupplement"
            >
              {{ supplementSubmitting ? '提交中...' : '提交补卡申请' }}
            </button>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped>
.clock {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--ceams-space-xl) var(--ceams-space-lg);
}

.clock__time {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-display);
  font-weight: var(--ceams-font-weight-bold);
  line-height: var(--ceams-line-height-tight);
  letter-spacing: var(--ceams-letter-spacing-number);
}

.clock__weekday {
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.clock__status {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--ceams-space-xs);
  margin-top: var(--ceams-space-md);
}

.actions {
  display: flex;
  gap: var(--ceams-space-sm);
}

.actions__btn {
  flex: 1;
  margin: 0;
}

.actions__hint {
  display: block;
  margin: var(--ceams-space-xs) 0 var(--ceams-space-md);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-xs);
}

.section-title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.section-title--flush {
  margin-bottom: 0;
}

.link {
  color: var(--ceams-text-link);
  font-size: var(--ceams-font-size-caption);
}

.empty-text {
  display: block;
  padding: var(--ceams-space-md) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
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

.row__main {
  flex: 1;
  min-width: 0;
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

.supplement {
  margin-top: var(--ceams-space-xs);
}

.supplement__actions {
  margin-top: var(--ceams-space-md);
}

.supplement__btn {
  margin: 0;
}
</style>
