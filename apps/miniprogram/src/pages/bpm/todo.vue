<script setup lang="ts">
/**
 * BPM 待办中心：我的待办 / 我发起的 / 我已办 / 抄送我的 四类页签。
 * 四类数据分别缓存，切换页签不重复请求；列表内不直接审批，统一跳转流程详情页。
 */
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import type { CcRecordVO, InstanceVO, TaskVO } from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { apiErrorMessage } from '../../utils/error'
import { bpmBizTypeLabels, bpmInstanceStatusLabels, bpmTaskStatusLabels } from '../../utils/labels'

type TodoTab = 'pending' | 'initiated' | 'done' | 'cc'
type TagType = 'pending' | 'approved' | 'rejected' | 'neutral'

interface TabOption {
  key: TodoTab
  label: string
  /** 空态文案 */
  empty: string
}

/** 归一化后的列表行：四类接口字段差异在这一层抹平 */
interface TodoRow {
  key: string
  bizLabel: string
  nodeName: string
  initiatorName: string
  timeLabel: string
  dueLabel: string
  overdue: boolean
  statusLabel: string
  statusType: TagType
  taskId: string
  instanceId: string
}

interface TabCache {
  rows: TodoRow[]
  page: number
  total: number
  /** 是否已完成首次加载（用于切页签时判断是否命中缓存） */
  loaded: boolean
  loading: boolean
  /** 是否已加载完全部数据 */
  finished: boolean
}

const TABS: TabOption[] = [
  { key: 'pending', label: '我的待办', empty: '暂无待办' },
  { key: 'initiated', label: '我发起的', empty: '暂无发起记录' },
  { key: 'done', label: '我已办', empty: '暂无已办记录' },
  { key: 'cc', label: '抄送我的', empty: '暂无抄送' }
]

const TAB_KEYS: TodoTab[] = ['pending', 'initiated', 'done', 'cc']
const SIZE = 20

function emptyCache(): TabCache {
  return { rows: [], page: 0, total: 0, loaded: false, loading: false, finished: false }
}

const activeTab = ref<TodoTab>('pending')
const caches = ref<Record<TodoTab, TabCache>>({
  pending: emptyCache(),
  initiated: emptyCache(),
  done: emptyCache(),
  cc: emptyCache()
})

const current = computed<TabCache>(() => caches.value[activeTab.value])
const rows = computed<TodoRow[]>(() => current.value.rows)
const currentTab = computed<TabOption>(
  () => TABS.find((item) => item.key === activeTab.value) ?? TABS[0]
)
const loading = computed<boolean>(() => current.value.loading)
const isEmpty = computed<boolean>(
  () => current.value.loaded && !current.value.loading && current.value.rows.length === 0
)
const noMore = computed<boolean>(
  () => current.value.rows.length > 0 && current.value.finished
)

function bizLabel(bizType?: string | null): string {
  if (!bizType) return '审批'
  return bpmBizTypeLabels[bizType] ?? bizType
}

function initiatorName(name?: string | null, id?: string | null): string {
  return name || id || '-'
}

function taskTagType(status: TaskVO['status']): TagType {
  if (status === 'APPROVED') return 'approved'
  if (status === 'REJECTED') return 'rejected'
  if (status === 'PENDING' || status === 'PENDING_ASSIGNEE') return 'pending'
  return 'neutral'
}

function instanceTagType(status: InstanceVO['status']): TagType {
  if (status === 'RUNNING') return 'pending'
  if (status === 'APPROVED') return 'approved'
  if (status === 'REJECTED') return 'rejected'
  return 'neutral'
}

/** 待办行：主键 taskId + instanceId，点击与快捷按钮均跳详情页 */
function toPendingRow(item: TaskVO): TodoRow {
  return {
    key: item.id,
    bizLabel: bizLabel(item.bizType),
    nodeName: item.currentNodeName || '审批',
    initiatorName: initiatorName(item.initiatorName, item.initiatorId),
    timeLabel: `发起 ${formatDateTime(item.createTime)}`,
    dueLabel: item.dueAt ? `到期 ${formatDateTime(item.dueAt)}` : '',
    overdue: item.overdue === true,
    statusLabel: bpmTaskStatusLabels[item.status] ?? item.status,
    statusType: taskTagType(item.status),
    taskId: item.id,
    instanceId: item.instanceId
  }
}

function toDoneRow(item: TaskVO): TodoRow {
  return {
    key: item.id,
    bizLabel: bizLabel(item.bizType),
    nodeName: item.currentNodeName || '审批',
    initiatorName: initiatorName(item.initiatorName, item.initiatorId),
    timeLabel: `发起 ${formatDateTime(item.createTime)}`,
    dueLabel: `处理 ${formatDateTime(item.updateTime ?? item.dueAt)}`,
    overdue: item.overdue === true,
    statusLabel: bpmTaskStatusLabels[item.status] ?? item.status,
    statusType: taskTagType(item.status),
    taskId: '',
    instanceId: item.instanceId
  }
}

/** 发起行：只有实例，无任务主键 */
function toInitiatedRow(item: InstanceVO): TodoRow {
  return {
    key: item.id,
    bizLabel: bizLabel(item.bizType),
    nodeName: item.currentNodeKey || '流程',
    initiatorName: '本人',
    timeLabel: `发起 ${formatDateTime(item.startedAt ?? item.createTime)}`,
    dueLabel: item.endedAt ? `结束 ${formatDateTime(item.endedAt)}` : '',
    overdue: false,
    statusLabel: bpmInstanceStatusLabels[item.status] ?? item.status,
    statusType: instanceTagType(item.status),
    taskId: '',
    instanceId: item.id
  }
}

function toCcRow(item: CcRecordVO): TodoRow {
  const unread = item.readStatus === 'UNREAD'
  return {
    key: item.id,
    bizLabel: bizLabel(item.bizType),
    nodeName: item.currentNodeName || '抄送',
    initiatorName: initiatorName(item.initiatorName, item.initiatorId),
    timeLabel: `抄送 ${formatDateTime(item.createTime)}`,
    dueLabel: '',
    overdue: false,
    statusLabel: unread ? '未读' : '已读',
    statusType: unread ? 'pending' : 'neutral',
    taskId: '',
    instanceId: item.instanceId
  }
}

/** 按页签调用既有接口并归一化为列表行 */
async function fetchRows(tab: TodoTab, page: number): Promise<{ rows: TodoRow[]; total: number }> {
  if (tab === 'pending') {
    const res = await apiClient.bpm.pendingTasks({ page, size: SIZE })
    return { rows: res.records.map(toPendingRow), total: res.total }
  }
  if (tab === 'done') {
    const res = await apiClient.bpm.doneTasks({ page, size: SIZE })
    return { rows: res.records.map(toDoneRow), total: res.total }
  }
  if (tab === 'initiated') {
    const res = await apiClient.bpm.initiated({ page, size: SIZE })
    return { rows: res.records.map(toInitiatedRow), total: res.total }
  }
  const res = await apiClient.bpm.ccRecords({ page, size: SIZE })
  return { rows: res.records.map(toCcRow), total: res.total }
}

async function loadTab(tab: TodoTab, reset: boolean): Promise<void> {
  const cache = caches.value[tab]
  if (cache.loading) return
  if (!reset && cache.finished) return
  cache.loading = true
  const nextPage = reset ? 1 : cache.page + 1
  try {
    const res = await fetchRows(tab, nextPage)
    cache.rows = reset ? res.rows : cache.rows.concat(res.rows)
    cache.page = nextPage
    cache.total = res.total
    cache.loaded = true
    cache.finished = cache.rows.length >= res.total || res.rows.length < SIZE
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载失败'), icon: 'none' })
    if (reset) {
      cache.rows = []
      cache.page = 0
      cache.total = 0
      cache.finished = false
    }
  } finally {
    cache.loading = false
  }
}

/** 切换页签：重置当前页签分页；已缓存过的页签直接展示，避免重复请求 */
function switchTab(tab: TodoTab): void {
  if (tab === activeTab.value) return
  activeTab.value = tab
  if (!caches.value[tab].loaded) {
    void loadTab(tab, true)
  }
}

function openDetail(row: TodoRow): void {
  const query = row.taskId
    ? `taskId=${encodeURIComponent(row.taskId)}&instanceId=${encodeURIComponent(row.instanceId)}`
    : `instanceId=${encodeURIComponent(row.instanceId)}`
  uni.navigateTo({ url: `/pages/bpm/detail?${query}` })
}

onLoad((query) => {
  const tab = typeof query?.tab === 'string' ? query.tab : ''
  if (TAB_KEYS.includes(tab as TodoTab)) {
    activeTab.value = tab as TodoTab
  }
  const taskId = typeof query?.taskId === 'string' ? query.taskId : ''
  const instanceId = typeof query?.instanceId === 'string' ? query.instanceId : ''
  // 通知深链携带具体单据时直达详情页，避免多一次列表跳转
  if (taskId || instanceId) {
    const parts = [taskId ? `taskId=${encodeURIComponent(taskId)}` : '', instanceId ? `instanceId=${encodeURIComponent(instanceId)}` : '']
    uni.redirectTo({ url: `/pages/bpm/detail?${parts.filter(Boolean).join('&')}` })
    return
  }
  void loadTab(activeTab.value, true)
})

let shown = false
onShow(() => {
  if (!shown) {
    shown = true
    return
  }
  // 从详情页返回时刷新当前页签，保证审批结果及时反映
  void loadTab(activeTab.value, true)
})

onPullDownRefresh(async () => {
  await loadTab(activeTab.value, true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  const cache = caches.value[activeTab.value]
  if (cache.loading || cache.finished) return
  void loadTab(activeTab.value, false)
})
</script>

<template>
  <view class="ceams-page">
    <view class="tabs">
      <view
        v-for="item in TABS"
        :key="item.key"
        class="tabs__item"
        :class="{ 'tabs__item--active': item.key === activeTab }"
        @click="switchTab(item.key)"
      >
        {{ item.label }}
      </view>
    </view>

    <view v-if="loading && rows.length === 0" class="state">加载中...</view>
    <empty-state
      v-else-if="isEmpty"
      :title="currentTab.empty"
      description="有新记录时会在此显示"
    />

    <view v-for="item in rows" :key="item.key" class="item" @click="openDetail(item)">
      <list-card>
        <template #left>
          <view class="item__main">
            <view class="item__title-row">
              <text class="item__title ceams-ellipsis">
                {{ item.bizLabel }} · {{ item.nodeName }}
              </text>
              <status-tag v-if="item.overdue" label="已超时" type="rejected" />
            </view>
            <text class="item__subtitle ceams-ellipsis">发起人 {{ item.initiatorName }}</text>
            <text class="item__date">{{ item.timeLabel }}</text>
            <text v-if="item.dueLabel" class="item__date">{{ item.dueLabel }}</text>
          </view>
        </template>
        <template #right>
          <view class="item__extra">
            <status-tag :label="item.statusLabel" :type="item.statusType" />
            <view v-if="item.taskId" class="item__actions">
              <text class="item__action item__action--reject" @click.stop="openDetail(item)">驳回</text>
              <text class="item__action item__action--approve" @click.stop="openDetail(item)">通过</text>
            </view>
          </view>
        </template>
      </list-card>
    </view>

    <view v-if="noMore" class="loadmore">没有更多了</view>
    <view v-else-if="rows.length > 0" class="loadmore">上拉加载更多</view>
  </view>
</template>

<style scoped>
.tabs {
  display: flex;
  margin-bottom: var(--ceams-space-md);
  padding: 0 var(--ceams-space-xs);
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-card);
  box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.04);
}

.tabs__item {
  position: relative;
  flex: 1;
  padding: 24rpx 0 22rpx;
  color: var(--ceams-text-secondary);
  font-size: 26rpx;
  line-height: var(--ceams-line-height-tight);
  text-align: center;
}

/* 选中页签：加粗 + 底部 4rpx 主色条 */
.tabs__item--active {
  color: var(--ceams-color-primary);
  font-weight: var(--ceams-font-weight-semibold);
}

.tabs__item--active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 48rpx;
  height: 4rpx;
  margin-left: -24rpx;
  background-color: var(--ceams-color-primary);
  border-radius: 2rpx;
}

.item {
  margin-bottom: var(--ceams-space-md);
}

.item__main {
  display: flex;
  flex-direction: column;
}

.item__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item__title {
  flex: 1;
  min-width: 0;
  color: var(--ceams-text-primary);
  font-size: 28rpx;
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

.item__subtitle {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.item__date {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
  line-height: var(--ceams-line-height-normal);
}

.item__extra {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.item__actions {
  display: flex;
  margin-top: var(--ceams-space-xs);
}

.item__action {
  margin-left: var(--ceams-space-xs);
  padding: var(--ceams-space-2xs) var(--ceams-space-sm);
  border-radius: var(--ceams-radius-pill);
  font-size: var(--ceams-font-size-mini);
}

.item__action--reject {
  color: var(--ceams-color-danger);
  border: var(--ceams-border-width) solid var(--ceams-color-danger);
}

.item__action--approve {
  color: var(--ceams-color-primary);
  border: var(--ceams-border-width) solid var(--ceams-color-primary);
}

.state {
  padding: var(--ceams-space-3xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}

.loadmore {
  padding: var(--ceams-space-md) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}
</style>
