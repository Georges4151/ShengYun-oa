<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ElButton,
  ElCard,
  ElEmpty,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { ApiError } from '@ceams/api-client'
import { formatDateTime } from '@ceams/utils'
import type {
  Id,
  WecomMessageStatus,
  WecomMessageVO,
  WecomSyncLogVO,
  WecomSyncStatus
} from '@ceams/types'
import { ceamsApi } from '@/services/http'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'WeComView' })

const canSync = computed(() => hasPermission('integration:wecom:sync'))

const PAGE_SIZES = [10, 20, 50]

// ---------- 枚举文案 / 标签色 ----------

const syncStatusLabels: Record<WecomSyncStatus, string> = {
  RUNNING: '进行中',
  SUCCESS: '成功',
  PARTIAL: '部分成功',
  FAILED: '失败'
}

const syncTypeLabels: Record<string, string> = {
  MANUAL: '手动',
  SCHEDULED: '定时'
}

const messageStatusLabels: Record<WecomMessageStatus, string> = {
  PENDING: '待发送',
  SENT: '已发送',
  FAILED: '失败'
}

function syncStatusTagType(status: WecomSyncStatus): 'success' | 'warning' | 'danger' | 'primary' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'PARTIAL':
      return 'warning'
    case 'FAILED':
      return 'danger'
    default:
      return 'primary'
  }
}

function messageStatusTagType(status: WecomMessageStatus): 'info' | 'success' | 'danger' {
  switch (status) {
    case 'SENT':
      return 'success'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

// ---------- 最近一次同步 ----------

const latestLog = ref<WecomSyncLogVO | null>(null)
const latestLoading = ref(false)

async function loadLatest(): Promise<void> {
  latestLoading.value = true
  try {
    const res = await ceamsApi.integration.pageWecomSyncLogs({ page: 1, size: 1 })
    latestLog.value = res.records[0] ?? null
  } catch {
    // 概览加载失败不阻断下方两张明细表
    latestLog.value = null
  } finally {
    latestLoading.value = false
  }
}

// ---------- 手动同步 ----------

const syncing = ref(false)

async function onTriggerSync(): Promise<void> {
  syncing.value = true
  try {
    const result = await ceamsApi.integration.triggerWecomSync()
    ElMessage.success(
      `同步完成：批次 ${result.batchNo}，状态 ${syncStatusLabels[result.status] ?? result.status}`
    )
    await refreshAll()
  } catch (err) {
    if (err instanceof ApiError && err.status === 503) {
      ElMessage.warning('企业微信集成未启用，请配置 WECOM_APP_* 环境变量')
    } else {
      ElMessage.error(apiErrorMessage(err, '触发同步失败'))
    }
  } finally {
    syncing.value = false
  }
}

// ---------- 同步批次 ----------

const syncStatusOptions: Array<{ value: WecomSyncStatus; label: string }> = [
  { value: 'SUCCESS', label: '成功' },
  { value: 'PARTIAL', label: '部分成功' },
  { value: 'FAILED', label: '失败' },
  { value: 'RUNNING', label: '进行中' }
]

const syncFilter = ref<WecomSyncStatus | ''>('')
const syncLogs = ref<WecomSyncLogVO[]>([])
const syncLoading = ref(false)
const {
  page: syncPage,
  size: syncSize,
  total: syncTotal,
  resetPage: resetSyncPage
} = usePage(10)

async function loadSyncLogs(): Promise<void> {
  syncLoading.value = true
  try {
    const res = await ceamsApi.integration.pageWecomSyncLogs({
      page: syncPage.value,
      size: syncSize.value,
      status: syncFilter.value || undefined
    })
    syncLogs.value = res.records
    syncTotal.value = res.total
  } catch (err) {
    syncLogs.value = []
    syncTotal.value = 0
    ElMessage.error(apiErrorMessage(err, '加载同步批次失败'))
  } finally {
    syncLoading.value = false
  }
}

function onSyncFilterChange(): void {
  resetSyncPage()
  void loadSyncLogs()
}

function onSyncSizeChange(): void {
  resetSyncPage()
  void loadSyncLogs()
}

// ---------- 应用消息 ----------

const messages = ref<WecomMessageVO[]>([])
const messageLoading = ref(false)
const retryingId = ref<Id | null>(null)
const {
  page: messagePage,
  size: messageSize,
  total: messageTotal,
  resetPage: resetMessagePage
} = usePage(10)

async function loadMessages(): Promise<void> {
  messageLoading.value = true
  try {
    const res = await ceamsApi.integration.pageWecomMessages({
      page: messagePage.value,
      size: messageSize.value
    })
    messages.value = res.records
    messageTotal.value = res.total
  } catch (err) {
    messages.value = []
    messageTotal.value = 0
    ElMessage.error(apiErrorMessage(err, '加载应用消息失败'))
  } finally {
    messageLoading.value = false
  }
}

function onMessageSizeChange(): void {
  resetMessagePage()
  void loadMessages()
}

async function onRetry(row: WecomMessageVO): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认重投消息（接收人 ${row.wecomUserid || row.targetUserId}）？重投后将由调度器重新发送。`,
    '消息重投',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return

  retryingId.value = row.id
  try {
    await ceamsApi.integration.retryWecomMessage(row.id)
    ElMessage.success('已重置为待发送，稍后由调度器投递')
    await loadMessages()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '消息重投失败'))
  } finally {
    retryingId.value = null
  }
}

// ---------- 汇总刷新 ----------

async function refreshAll(): Promise<void> {
  await Promise.all([loadLatest(), loadSyncLogs(), loadMessages()])
}

onMounted(() => {
  void refreshAll()
})
</script>

<template>
  <div class="wecom-view">
    <el-card class="overview-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">企业微信集成</span>
          <el-button
            v-if="canSync"
            type="primary"
            :loading="syncing"
            @click="onTriggerSync"
          >
            立即同步
          </el-button>
        </div>
      </template>

      <p class="overview-desc">
        企业微信组织单向同步：以 CEAMS 为主数据源，部门与成员从企业微信拉取后写入本系统；
        本地手工创建的账号不会被同步禁用。应用消息由站内通知入队后异步投递。
      </p>

      <div v-loading="latestLoading" class="latest-row">
        <span class="latest-label">最近一次同步：</span>
        <template v-if="latestLog">
          <el-tag :type="syncStatusTagType(latestLog.status)" size="small">
            {{ syncStatusLabels[latestLog.status] ?? latestLog.status }}
          </el-tag>
          <span>批次 {{ latestLog.batchNo }}</span>
          <span>
            成功 {{ latestLog.successCount }} / 失败 {{ latestLog.failedCount }}（共
            {{ latestLog.total }}）
          </span>
          <span>开始 {{ formatDateTime(latestLog.startedAt) }}</span>
        </template>
        <span v-else class="latest-empty">暂无同步记录</span>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">同步批次</span>
          <div class="header-actions">
            <el-select
              v-model="syncFilter"
              placeholder="全部状态"
              clearable
              style="width: 150px"
              @change="onSyncFilterChange"
            >
              <el-option
                v-for="opt in syncStatusOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-button @click="loadSyncLogs">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="syncLoading" :data="syncLogs" border size="small">
        <el-table-column prop="batchNo" label="批次号" min-width="170" show-overflow-tooltip />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            {{ syncTypeLabels[(row as WecomSyncLogVO).syncType] ?? (row as WecomSyncLogVO).syncType }}
          </template>
        </el-table-column>
        <el-table-column label="总数" width="80">
          <template #default="{ row }">{{ (row as WecomSyncLogVO).total }}</template>
        </el-table-column>
        <el-table-column label="成功" width="80">
          <template #default="{ row }">{{ (row as WecomSyncLogVO).successCount }}</template>
        </el-table-column>
        <el-table-column label="失败" width="80">
          <template #default="{ row }">{{ (row as WecomSyncLogVO).failedCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="syncStatusTagType((row as WecomSyncLogVO).status)" size="small">
              {{ syncStatusLabels[(row as WecomSyncLogVO).status] ?? (row as WecomSyncLogVO).status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="165">
          <template #default="{ row }">{{ formatDateTime((row as WecomSyncLogVO).startedAt) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="165">
          <template #default="{ row }">{{ formatDateTime((row as WecomSyncLogVO).finishedAt) }}</template>
        </el-table-column>
        <el-table-column
          prop="errorMsg"
          label="错误信息"
          min-width="180"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ (row as WecomSyncLogVO).errorMsg ?? '-' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无同步批次" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="syncPage"
        v-model:page-size="syncSize"
        :total="syncTotal"
        :page-sizes="PAGE_SIZES"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="onSyncSizeChange"
        @current-change="loadSyncLogs"
      />
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">应用消息</span>
          <el-button @click="loadMessages">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="messageLoading" :data="messages" border size="small">
        <el-table-column prop="targetUserId" label="接收人ID" min-width="150" show-overflow-tooltip />
        <el-table-column prop="wecomUserid" label="企微 userid" min-width="120" show-overflow-tooltip />
        <el-table-column prop="msgType" label="类型" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="messageStatusTagType((row as WecomMessageVO).status)" size="small">
              {{ messageStatusLabels[(row as WecomMessageVO).status] ?? (row as WecomMessageVO).status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="重试次数" width="90">
          <template #default="{ row }">{{ (row as WecomMessageVO).retryCount }}</template>
        </el-table-column>
        <el-table-column label="下次重试" width="165">
          <template #default="{ row }">{{ formatDateTime((row as WecomMessageVO).nextRetryAt) }}</template>
        </el-table-column>
        <el-table-column label="发送时间" width="165">
          <template #default="{ row }">{{ formatDateTime((row as WecomMessageVO).sentAt) }}</template>
        </el-table-column>
        <el-table-column
          prop="errorMsg"
          label="错误信息"
          min-width="180"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ (row as WecomMessageVO).errorMsg ?? '-' }}</template>
        </el-table-column>
        <el-table-column v-if="canSync" label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="(row as WecomMessageVO).status === 'FAILED'"
              link
              type="primary"
              size="small"
              :loading="retryingId === (row as WecomMessageVO).id"
              @click="onRetry(row as WecomMessageVO)"
            >
              重投
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无应用消息" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="messagePage"
        v-model:page-size="messageSize"
        :total="messageTotal"
        :page-sizes="PAGE_SIZES"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="onMessageSizeChange"
        @current-change="loadMessages"
      />
    </el-card>
  </div>
</template>

<style scoped>
.wecom-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.overview-desc {
  margin: 0 0 12px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}
.latest-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #606266;
}
.latest-label {
  color: #303133;
  font-weight: 500;
}
.latest-empty {
  color: #909399;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
