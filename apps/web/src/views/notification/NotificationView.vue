<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElButton, ElCard, ElEmpty, ElMessage, ElPagination, ElTag } from 'element-plus'
import type { UserNotificationVO } from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { PageContainer } from '@ceams/ui-web'
import { ceamsApi } from '@/services/http'
import { apiErrorMessage } from '@/utils/error'
import { pushChannelLabels } from '@/utils/labels'

defineOptions({ name: 'NotificationView' })

/** 日报摘要深链前缀：命中后展示「查看日报摘要」，并派发全局事件由 AI 抽屉处理 */
const SUMMARY_DEEP_LINK = 'ceams://ai/summary'
/** 正文超过该长度（或超过 3 行）时折叠，避免长通知撑高列表 */
const CONTENT_CLAMP_LENGTH = 120

const records = ref<UserNotificationVO[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const unreadCount = ref(0)
const loading = ref(false)
/** 正文展开状态（按用户通知 id 记录） */
const expanded = ref<Record<string, boolean>>({})

const hasRecords = computed(() => records.value.length > 0)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.notification.userNotificationMine({ page: page.value, size: size.value })
    records.value = res.records
    total.value = res.total
    unreadCount.value = res.unreadCount
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载通知失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  page.value = 1
  void load()
}

function displayTitle(item: UserNotificationVO): string {
  return item.title || '系统通知'
}

function displayTime(item: UserNotificationVO): string {
  return formatDateTime(item.publishTime ?? item.createTime)
}

function channelLabel(item: UserNotificationVO): string {
  return pushChannelLabels[item.channel ?? 'SYSTEM']
}

function isSummaryLink(item: UserNotificationVO): boolean {
  return (item.linkUrl ?? '').startsWith(SUMMARY_DEEP_LINK)
}

/** 从 linkUrl 查询串解析 date；缺失返回空串 */
function summaryDate(item: UserNotificationVO): string {
  const linkUrl = item.linkUrl ?? ''
  const queryIndex = linkUrl.indexOf('?')
  if (queryIndex === -1) return ''
  const params = new URLSearchParams(linkUrl.slice(queryIndex + 1))
  return params.get('date')?.trim() ?? ''
}

function isLongContent(item: UserNotificationVO): boolean {
  const content = item.content ?? ''
  return content.length > CONTENT_CLAMP_LENGTH || content.split('\n').length > 3
}

function isExpanded(item: UserNotificationVO): boolean {
  return expanded.value[item.id] === true
}

function toggleContent(item: UserNotificationVO): void {
  expanded.value = { ...expanded.value, [item.id]: !isExpanded(item) }
}

async function markRead(item: UserNotificationVO): Promise<void> {
  if (item.isRead === 1) return
  try {
    await ceamsApi.notification.read(item.id)
    item.isRead = 1
    item.readTime = new Date().toISOString()
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '标记已读失败'))
  }
}

async function markAllRead(): Promise<void> {
  if (unreadCount.value === 0) return
  try {
    await ceamsApi.notification.readAll()
    ElMessage.success('已全部标记为已读')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '全部已读失败'))
  }
}

/**
 * 查看日报摘要：先标记已读，再通过全局事件交给 AI 抽屉打开摘要视图。
 * date 从深链查询参数解析，无则不带。
 */
async function openSummary(item: UserNotificationVO): Promise<void> {
  await markRead(item)
  const date = summaryDate(item)
  window.dispatchEvent(
    new CustomEvent('ceams:open-ai-summary', { detail: date ? { date } : {} })
  )
}

onMounted(() => {
  void load()
})
</script>

<template>
  <PageContainer title="通知" subtitle="系统与审批通知">
    <template #extra>
      <el-button size="small" :disabled="unreadCount === 0 || loading" @click="markAllRead">全部已读</el-button>
    </template>

    <div class="notification-view">
      <div class="notification-view__summary">
        <span class="notification-view__label">未读</span>
        <span class="notification-view__count">{{ unreadCount }}</span>
        <span class="notification-view__unit">条</span>
      </div>

      <el-card v-loading="loading" shadow="never" class="notification-view__panel">
        <el-empty v-if="!loading && !hasRecords" description="暂无通知" />

        <div v-else>
          <div
            v-for="item in records"
            :key="item.id"
            class="notification-item"
            :class="{ 'notification-item--unread': item.isRead === 0 }"
          >
            <span v-if="item.isRead === 0" class="notification-item__bar" />

            <div class="notification-item__body">
              <div class="notification-item__head">
                <div class="notification-item__title-line">
                  <span class="notification-item__title">{{ displayTitle(item) }}</span>
                  <el-tag size="small" type="info" effect="plain">{{ channelLabel(item) }}</el-tag>
                  <el-tag v-if="item.isRead === 0" size="small" type="warning" effect="plain">未读</el-tag>
                  <el-tag v-else size="small" type="info" effect="plain">已读</el-tag>
                </div>
                <span class="notification-item__time">{{ displayTime(item) }}</span>
              </div>

              <template v-if="item.content">
                <p
                  class="notification-item__content"
                  :class="{ 'notification-item__content--clamp': isLongContent(item) && !isExpanded(item) }"
                >
                  {{ item.content }}
                </p>
                <el-button
                  v-if="isLongContent(item)"
                  link
                  type="primary"
                  size="small"
                  class="notification-item__toggle"
                  @click="toggleContent(item)"
                >
                  {{ isExpanded(item) ? '收起' : '展开' }}
                </el-button>
              </template>

              <div class="notification-item__actions">
                <el-button v-if="item.isRead === 0" link type="primary" size="small" @click="markRead(item)">
                  标记已读
                </el-button>
                <el-button v-if="isSummaryLink(item)" link type="primary" size="small" @click="openSummary(item)">
                  查看日报摘要
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="notification-view__pagination"
        @size-change="onSearch"
        @current-change="load"
      />
    </div>
  </PageContainer>
</template>

<style scoped>
.notification-view__summary {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 12px;
}
.notification-view__label,
.notification-view__unit {
  color: #909399;
  font-size: 13px;
}
.notification-view__count {
  color: #409eff;
  font-size: 20px;
  font-weight: 600;
}
.notification-view__panel {
  min-height: 120px;
}
.notification-view__pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

.notification-item {
  position: relative;
  display: flex;
  padding: 12px 0 12px 12px;
  border-bottom: 1px solid #ebeef5;
}
.notification-item:last-child {
  border-bottom: none;
}
.notification-item__bar {
  position: absolute;
  top: 16px;
  bottom: 16px;
  left: 0;
  width: 3px;
  border-radius: 2px;
  background: #409eff;
}
.notification-item__body {
  flex: 1;
  min-width: 0;
}
.notification-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.notification-item__title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}
.notification-item__title {
  overflow: hidden;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.notification-item--unread .notification-item__title {
  color: #303133;
}
.notification-item__time {
  flex-shrink: 0;
  color: #909399;
  font-size: 12px;
}
.notification-item__content {
  margin: 8px 0 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.notification-item__content--clamp {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}
.notification-item__toggle {
  margin-top: 4px;
  padding: 0;
}
.notification-item__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 24px;
  margin-top: 4px;
}
</style>
