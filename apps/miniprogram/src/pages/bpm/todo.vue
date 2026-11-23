<script setup lang="ts">
/**
 * BPM 待办中心：我的待办 / 我发起的 / 我已办 / 抄送我的 四类页签。
 * 四类数据分别缓存，切换页签不重复请求；列表内不直接审批，统一跳转流程详情页。
 */
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import type {
  BizAttachmentGroupVO,
  BizAttachmentVO,
  BizDetailVO,
  CcRecordVO,
  InstanceVO,
  TaskVO
} from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { downloadInstanceFile, openDocument } from '../../platform/download'
import { previewLocalImages } from '../../platform/image'
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

// ---------------- 业务单据详情 ----------------
const bizDetailVisible = ref(false)
const bizDetailLoading = ref(false)
const bizDetail = ref<BizDetailVO | null>(null)
const bizDetailTitle = computed(() => bizDetail.value?.title || '业务单据')
/** 当前单据所属流程实例：附件下载按实例校验归属 */
const bizDetailInstanceId = ref('')

// ---------------- 单据附件 ----------------
/** 附件图片本地加载状态：loading 下载中 / ready 已就绪 / error 下载失败 */
type AttachmentImageStatus = 'loading' | 'ready' | 'error'

/** 图片状态与本地临时路径均以 `组下标-项下标` 为键，弹层关闭时整体清空 */
const imageStates = ref<Record<string, AttachmentImageStatus>>({})
const imagePaths = ref<Record<string, string>>({})
/** 非图片附件的下载中状态（避免重复点击） */
const fileLoading = ref<Record<string, boolean>>({})
/** 附件加载批次号：弹层关闭或切换单据后，旧批次迟到的下载结果直接丢弃 */
let attachmentGeneration = 0

const attachmentGroups = computed<BizAttachmentGroupVO[]>(() => bizDetail.value?.attachmentGroups ?? [])

/** 渲染用的附件项：index 为原分组内下标，图片下载状态按该下标索引 */
interface AttachmentItemView {
  item: BizAttachmentVO
  index: number
}

/** 渲染用的附件分组：预先按 kind 拆成图片 / 文件两类，避免模板内 v-for 与 v-if 混用 */
interface AttachmentGroupView {
  groupName: string
  groupIndex: number
  images: AttachmentItemView[]
  files: AttachmentItemView[]
}

const attachmentViewGroups = computed<AttachmentGroupView[]>(() =>
  attachmentGroups.value.map((group, groupIndex) => {
    const images: AttachmentItemView[] = []
    const files: AttachmentItemView[] = []
    group.items.forEach((item, index) => {
      const entry: AttachmentItemView = { item, index }
      if (item.kind === 'IMAGE') {
        images.push(entry)
      } else {
        files.push(entry)
      }
    })
    return { groupName: group.groupName, groupIndex, images, files }
  })
)

/** openDocument 支持的文件类型；其余扩展名提示暂不支持 */
const PREVIEW_FILE_TYPES: Record<string, string> = {
  pdf: 'pdf',
  doc: 'doc',
  docx: 'docx',
  xls: 'xls',
  xlsx: 'xlsx',
  ppt: 'ppt',
  pptx: 'pptx'
}

function attachmentKey(groupIndex: number, itemIndex: number): string {
  return `${groupIndex}-${itemIndex}`
}

function imageStatus(groupIndex: number, itemIndex: number): AttachmentImageStatus {
  return imageStates.value[attachmentKey(groupIndex, itemIndex)] ?? 'loading'
}

function imageSrc(groupIndex: number, itemIndex: number): string {
  return imagePaths.value[attachmentKey(groupIndex, itemIndex)] ?? ''
}

function isFileLoading(groupIndex: number, itemIndex: number): boolean {
  return fileLoading.value[attachmentKey(groupIndex, itemIndex)] === true
}

/** 清空附件下载状态（无需删除临时文件），避免下次打开弹层串图 */
function resetAttachments(): void {
  attachmentGeneration += 1
  imageStates.value = {}
  imagePaths.value = {}
  fileLoading.value = {}
}

/** 逐张下载分组内图片；单张失败只标记该图，不影响同组其它附件 */
function loadGroupImages(instanceId: string, groups: BizAttachmentGroupVO[]): void {
  const generation = attachmentGeneration
  groups.forEach((group, groupIndex) => {
    group.items.forEach((item, itemIndex) => {
      if (item.kind !== 'IMAGE') return
      const key = attachmentKey(groupIndex, itemIndex)
      imageStates.value[key] = 'loading'
      void downloadInstanceFile(instanceId, item.name)
        .then((path) => {
          if (generation !== attachmentGeneration) return
          imagePaths.value[key] = path
          imageStates.value[key] = 'ready'
        })
        .catch(() => {
          if (generation !== attachmentGeneration) return
          imageStates.value[key] = 'error'
        })
    })
  })
}

/** 点击缩略图：把同组已下载成功的本地路径交给 previewImage，支持左右滑动切换 */
function previewGroupImage(groupIndex: number, itemIndex: number): void {
  const group = attachmentGroups.value[groupIndex]
  if (!group) return
  const urls: string[] = []
  let currentPath = ''
  group.items.forEach((item, index) => {
    if (item.kind !== 'IMAGE' || imageStatus(groupIndex, index) !== 'ready') return
    const path = imageSrc(groupIndex, index)
    if (!path) return
    if (index === itemIndex) currentPath = path
    urls.push(path)
  })
  if (!currentPath || urls.length === 0) return
  previewLocalImages(urls, currentPath).catch(() => {
    uni.showToast({ title: '预览图片失败', icon: 'none' })
  })
}

/** 从展示名中提取小写扩展名（忽略查询串与哈希） */
function fileExtension(name: string): string {
  const path = name.split(/[?#]/)[0]
  const index = path.lastIndexOf('.')
  return index === -1 ? '' : path.slice(index + 1).toLowerCase()
}

/** 解析可预览的文件类型：优先按扩展名，PDF 兜底按 kind 判定 */
function resolveFileType(item: BizAttachmentVO): string {
  const ext = PREVIEW_FILE_TYPES[fileExtension(item.fileName || item.name)]
  if (ext) return ext
  return item.kind === 'PDF' ? 'pdf' : ''
}

/** 打开非图片附件：先鉴权下载到本地临时文件，再交给 openDocument 预览 */
async function openAttachmentFile(
  groupIndex: number,
  itemIndex: number,
  item: BizAttachmentVO
): Promise<void> {
  const key = attachmentKey(groupIndex, itemIndex)
  if (fileLoading.value[key]) return
  const fileType = resolveFileType(item)
  if (!fileType) {
    uni.showToast({ title: '该文件暂不支持预览', icon: 'none' })
    return
  }
  // 记录批次号：下载期间弹层被关闭时，不再打开文档也不再提示
  const generation = attachmentGeneration
  fileLoading.value[key] = true
  try {
    const filePath = await downloadInstanceFile(bizDetailInstanceId.value, item.name)
    if (generation !== attachmentGeneration) return
    await openDocument(filePath, fileType, true)
  } catch {
    if (generation !== attachmentGeneration) return
    uni.showToast({ title: '无法预览该文件', icon: 'none' })
  } finally {
    if (generation === attachmentGeneration) {
      fileLoading.value[key] = false
    }
  }
}

/** 打开业务单据详情：按流程实例反查被审批的业务单据字段 */
async function openBizDetail(row: TodoRow): Promise<void> {
  // 先清空上一次的附件状态，防止旧单据图片串入新单据
  resetAttachments()
  bizDetail.value = null
  bizDetailInstanceId.value = row.instanceId
  bizDetailVisible.value = true
  bizDetailLoading.value = true
  try {
    const detail = await apiClient.bpm.instanceBizDetail(row.instanceId)
    bizDetail.value = detail
    // 字段就绪后异步加载图片缩略图，失败只影响单张
    loadGroupImages(row.instanceId, detail.attachmentGroups ?? [])
  } catch (err) {
    bizDetailVisible.value = false
    resetAttachments()
    uni.showToast({ title: apiErrorMessage(err, '加载单据详情失败'), icon: 'none' })
  } finally {
    bizDetailLoading.value = false
  }
}

function closeBizDetail(): void {
  bizDetailVisible.value = false
  bizDetailInstanceId.value = ''
  resetAttachments()
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
              <text class="item__action item__action--detail" @click.stop="openBizDetail(item)">查看详情</text>
              <text class="item__action item__action--reject" @click.stop="openDetail(item)">驳回</text>
              <text class="item__action item__action--approve" @click.stop="openDetail(item)">通过</text>
            </view>
          </view>
        </template>
      </list-card>
    </view>

    <view v-if="noMore" class="loadmore">没有更多了</view>
    <view v-else-if="rows.length > 0" class="loadmore">上拉加载更多</view>

    <!-- 业务单据详情弹层：按流程实例展示被审批单据的业务字段 -->
    <view v-if="bizDetailVisible" class="detail-mask" @click="closeBizDetail">
      <view class="detail-card" @click.stop>
        <view class="detail-card__head">
          <text class="detail-card__title">{{ bizDetailTitle }}</text>
          <text class="detail-card__close" @click="closeBizDetail">关闭</text>
        </view>
        <view v-if="bizDetailLoading" class="detail-card__state">加载中...</view>
        <template v-else-if="bizDetail">
          <view class="detail-card__status">
            <text class="detail-row__label">业务状态</text>
            <text class="detail-row__value">{{ bizDetail.statusText || '-' }}</text>
          </view>
          <view v-for="(item, index) in bizDetail.items" :key="index" class="detail-row">
            <text class="detail-row__label">{{ item.label }}</text>
            <text class="detail-row__value">{{ item.value }}</text>
          </view>

          <!-- 附件按分组展示；无附件时不渲染区域 -->
          <view v-if="attachmentViewGroups.length" class="attachment-area">
            <view
              v-for="view in attachmentViewGroups"
              :key="view.groupIndex"
              class="attachment-group"
            >
              <text class="attachment-group__title">{{ view.groupName }}</text>

              <!-- 图片：横向缩略图，点击查看大图并支持左右滑动 -->
              <view v-if="view.images.length" class="attachment-images">
                <view
                  v-for="entry in view.images"
                  :key="entry.index"
                  class="attachment-thumb"
                >
                  <image
                    v-if="imageStatus(view.groupIndex, entry.index) === 'ready'"
                    class="attachment-thumb__image"
                    :src="imageSrc(view.groupIndex, entry.index)"
                    mode="aspectFill"
                    @click="previewGroupImage(view.groupIndex, entry.index)"
                  />
                  <view v-else class="attachment-thumb__placeholder">
                    {{ imageStatus(view.groupIndex, entry.index) === 'error' ? '加载失败' : '加载中…' }}
                  </view>
                </view>
              </view>

              <!-- PDF / OFFICE / OTHER：文件名 + 查看 -->
              <view v-if="view.files.length" class="attachment-files">
                <view
                  v-for="entry in view.files"
                  :key="entry.index"
                  class="attachment-file"
                >
                  <text class="attachment-file__name">{{ entry.item.fileName || entry.item.name }}</text>
                  <text
                    class="attachment-file__action"
                    @click="openAttachmentFile(view.groupIndex, entry.index, entry.item)"
                  >
                    {{ isFileLoading(view.groupIndex, entry.index) ? '加载中…' : '查看' }}
                  </text>
                </view>
              </view>
            </view>
          </view>
        </template>
        <view v-else class="detail-card__state">暂无单据详情</view>
      </view>
    </view>
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

.item__action--detail {
  color: var(--ceams-text-secondary);
  border: var(--ceams-border-width) solid var(--ceams-border);
}

/* 单据详情弹层：遮罩 + 居中卡片，内容超高时内部滚动 */
.detail-mask {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ceams-space-xl);
  background-color: rgba(15, 23, 42, 0.45);
  box-sizing: border-box;
}

.detail-card {
  width: 100%;
  max-height: 80vh;
  padding: var(--ceams-space-lg);
  overflow-y: auto;
  background-color: var(--ceams-bg-card);
  border-radius: var(--ceams-radius-card);
  box-shadow: var(--ceams-shadow-raised);
  box-sizing: border-box;
}

.detail-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: var(--ceams-space-sm);
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.detail-card__title {
  flex: 1;
  min-width: 0;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-body);
  font-weight: var(--ceams-font-weight-semibold);
}

.detail-card__close {
  padding-left: var(--ceams-space-sm);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-caption);
}

.detail-card__state {
  padding: var(--ceams-space-2xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}

.detail-card__status,
.detail-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: var(--ceams-space-xs) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.detail-card__status {
  margin-top: var(--ceams-space-sm);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-row__label {
  flex-shrink: 0;
  width: 220rpx;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
}

.detail-row__value {
  flex: 1;
  min-width: 0;
  margin-left: var(--ceams-space-sm);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
  text-align: right;
  word-break: break-all;
}

/* ---------------- 单据附件 ---------------- */
.attachment-area {
  margin-top: var(--ceams-space-md);
}

.attachment-group {
  margin-bottom: var(--ceams-space-md);
}

.attachment-group:last-child {
  margin-bottom: 0;
}

.attachment-group__title {
  display: block;
  margin-bottom: var(--ceams-space-xs);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  font-weight: var(--ceams-font-weight-semibold);
  line-height: var(--ceams-line-height-tight);
}

/* 缩略图：120rpx（约 60px）方形、圆角，超出自动换行 */
.attachment-images {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ceams-space-xs);
}

.attachment-thumb {
  width: 120rpx;
  height: 120rpx;
  overflow: hidden;
  background-color: var(--ceams-bg-subtle);
  border-radius: var(--ceams-radius-sm);
}

.attachment-thumb__image {
  width: 120rpx;
  height: 120rpx;
}

.attachment-thumb__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 0 var(--ceams-space-2xs);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
  box-sizing: border-box;
}

/* 文件行：文件名单行省略，查看按钮靠右 */
.attachment-files {
  display: flex;
  flex-direction: column;
  gap: var(--ceams-space-xs);
}

.attachment-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ceams-space-2xs) 0;
}

.attachment-file__name {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-sm);
  overflow: hidden;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  line-height: var(--ceams-line-height-normal);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-file__action {
  flex-shrink: 0;
  padding: var(--ceams-space-2xs) var(--ceams-space-sm);
  border: var(--ceams-border-width) solid var(--ceams-color-primary);
  border-radius: var(--ceams-radius-pill);
  color: var(--ceams-color-primary);
  font-size: var(--ceams-font-size-mini);
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
