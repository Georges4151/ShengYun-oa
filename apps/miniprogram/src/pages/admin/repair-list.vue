<script setup lang="ts">
/**
 * 我的报修列表：我报修的 / 派给我的 两个切换项。
 * 两个接口均返回完整数组，此处一次性加载，切换时清空并重新请求。
 */
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import type { RepairStatus, RepairVO } from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { repairCategoryLabels, repairStatusLabels } from '../../utils/labels'
import { apiErrorMessage } from '../../utils/error'

type TabKey = 'mine' | 'assigned'
type TagType = 'pending' | 'approved' | 'rejected' | 'verified' | 'paid' | 'neutral'

interface TabOption {
  key: TabKey
  label: string
  /** 空态文案 */
  empty: string
}

const TABS: TabOption[] = [
  { key: 'mine', label: '我报修的', empty: '暂无报修记录' },
  { key: 'assigned', label: '派给我的', empty: '暂无派单任务' }
]

/** 工单状态 -> 状态标签语义（仅用于展示配色） */
const statusTagTypes: Record<RepairStatus, TagType> = {
  SUBMITTED: 'pending',
  ASSIGNED: 'pending',
  PROCESSING: 'verified',
  DONE: 'approved',
  ACCEPTED: 'paid',
  REJECTED: 'rejected'
}

const activeTab = ref<TabKey>('mine')
const records = ref<RepairVO[]>([])
const loading = ref(false)
/** 是否已完成至少一次加载，用于区分「加载中」与「空态」 */
const loaded = ref(false)
/** 加载失败提示，失败时不展示空态，避免误报「暂无数据」 */
const errorText = ref('')
/** 请求序号：快速切换页签时丢弃旧响应，避免列表串数据 */
let requestSeq = 0

const currentTab = computed<TabOption>(
  () => TABS.find((item) => item.key === activeTab.value) ?? TABS[0]
)
const isEmpty = computed<boolean>(() => loaded.value && !loading.value && records.value.length === 0)

/**
 * 报修 VO 未返回提交时间，按已产生的最早业务节点展示；
 * 尚未派单时给出占位文案，避免展示错误时间。
 */
function timeLabel(item: RepairVO): string {
  if (item.assignedAt) return `派单 ${formatDateTime(item.assignedAt)}`
  if (item.processedAt) return `处理 ${formatDateTime(item.processedAt)}`
  if (item.finishedAt) return `完工 ${formatDateTime(item.finishedAt)}`
  if (item.acceptedAt) return `验收 ${formatDateTime(item.acceptedAt)}`
  return '提交时间 --'
}

/** 图片数量提示：现场 x 张 / 完工 y 张 */
function imageSummary(item: RepairVO): string {
  const scene = item.images?.length ?? 0
  const finish = item.finishImages?.length ?? 0
  return `现场 ${scene} 张 / 完工 ${finish} 张`
}

async function load(): Promise<void> {
  const seq = ++requestSeq
  loading.value = true
  errorText.value = ''
  try {
    const list =
      activeTab.value === 'mine'
        ? await apiClient.admin.myRepairs()
        : await apiClient.admin.assignedRepairs()
    if (seq !== requestSeq) return
    records.value = Array.isArray(list) ? list : []
    loaded.value = true
  } catch (err) {
    if (seq !== requestSeq) return
    errorText.value = apiErrorMessage(err, '加载报修列表失败')
    uni.showToast({ title: errorText.value, icon: 'none' })
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

/** 切换页签：清空当前列表并重新加载 */
function switchTab(tab: TabKey): void {
  if (activeTab.value === tab) return
  activeTab.value = tab
  records.value = []
  loaded.value = false
  errorText.value = ''
  void load()
}

function openDetail(item: RepairVO): void {
  uni.navigateTo({ url: `/pages/admin/repair-detail?id=${encodeURIComponent(item.id)}` })
}

onShow(() => {
  // 每次进入或从详情页返回时刷新，保证状态及时反映
  void load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<template>
  <view class="ceams-page repair-list">
    <view class="repair-list__tabs">
      <view
        v-for="item in TABS"
        :key="item.key"
        class="repair-list__tab"
        :class="{ 'repair-list__tab--active': item.key === activeTab }"
        @click="switchTab(item.key)"
      >
        {{ item.label }}
      </view>
    </view>

    <view v-if="loading && !loaded" class="state">加载中...</view>
    <view v-else-if="errorText" class="state">{{ errorText }}</view>
    <empty-state
      v-else-if="isEmpty"
      :title="currentTab.empty"
      description="新的报修工单会在此显示"
    />

    <view
      v-for="item in records"
      :key="item.id"
      class="record"
      @click="openDetail(item)"
    >
      <list-card>
        <template #left>
          <view class="record__main">
            <text class="record__no ceams-ellipsis">{{ item.repairNo }}</text>
            <text class="record__location ceams-ellipsis">{{ item.location || '-' }}</text>
            <text class="record__meta">
              {{ repairCategoryLabels[item.category] }} · {{ timeLabel(item) }}
            </text>
            <text class="record__meta">{{ imageSummary(item) }}</text>
          </view>
        </template>
        <template #right>
          <status-tag
            :label="repairStatusLabels[item.status]"
            :type="statusTagTypes[item.status]"
          />
        </template>
      </list-card>
    </view>
  </view>
</template>

<style scoped>
.repair-list__tabs {
  display: flex;
  margin-bottom: var(--ceams-space-md);
  padding: var(--ceams-space-2xs);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-pill);
  box-shadow: var(--ceams-shadow-card);
}

.repair-list__tab {
  flex: 1;
  padding: var(--ceams-space-xs) 0;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
  border-radius: var(--ceams-radius-pill);
}

.repair-list__tab--active {
  color: var(--ceams-text-inverse);
  font-weight: var(--ceams-font-weight-semibold);
  background-color: var(--ceams-color-primary);
}

.record__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.record__no {
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-heading);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.record__location {
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.record__meta {
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}
</style>
