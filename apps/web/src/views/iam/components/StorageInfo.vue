<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElEmpty,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import type { FileStorageInfo, StorageDirectoryStat } from '@ceams/types'
import { ceamsApi } from '@/services/http'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'StorageInfo' })

const info = ref<FileStorageInfo | null>(null)
const loading = ref(false)
const error = ref('')

/** 目录存在且可写视为正常，其余情况（不存在 / 不可写）均为异常 */
const directoryHealthy = computed(() => Boolean(info.value?.exists && info.value?.writable))

/** 磁盘使用率（0-100）：由总容量与可用容量推导，仅用于进度条展示，不改动取数 */
const diskUsagePercent = computed<number | null>(() => {
  const total = info.value?.diskTotalBytes
  const usable = info.value?.diskUsableBytes
  if (total === null || total === undefined || usable === null || usable === undefined) return null
  if (total <= 0) return null
  const used = Math.min(Math.max(total - usable, 0), total)
  return Math.round((used / total) * 100)
})

/**
 * 字节数格式化：B / KB / MB / GB，保留 1 位小数。
 * 空值（null / undefined / NaN）显示 '-'，用于磁盘容量无法探测的场景。
 */
function formatBytes(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined || Number.isNaN(bytes)) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let unitIndex = 0
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }
  return `${value.toFixed(1)} ${units[unitIndex]}`
}

/** 加载存储信息：失败时记录错误文案，由模板提供「重试」入口 */
async function load(): Promise<void> {
  if (loading.value) return
  loading.value = true
  error.value = ''
  try {
    info.value = await ceamsApi.file.storageInfo()
  } catch (err) {
    info.value = null
    error.value = apiErrorMessage(err, '存储信息加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="storage-page">
    <!-- 信息卡片：标题 + 刷新主操作 -->
    <div v-loading="loading" class="data-card storage-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">存储信息</span>
        </div>
        <div class="card-actions">
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>

      <el-alert v-if="error" type="error" :closable="false" show-icon :title="error" class="error-alert">
        <el-button link type="primary" size="small" @click="load">重试</el-button>
      </el-alert>

      <template v-if="info">
        <!-- 字段网格：label / value 两列 -->
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">配置目录</span>
            <span class="info-value">{{ info.configuredPath || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">实际路径</span>
            <span class="info-value">{{ info.absolutePath || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">目录状态</span>
            <span class="info-value">
              <el-tag v-if="directoryHealthy" type="success" size="small">正常</el-tag>
              <el-tag v-else type="danger" size="small">异常</el-tag>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">文件总数</span>
            <span class="info-value">{{ info.fileCount }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">占用空间</span>
            <span class="info-value">{{ formatBytes(info.totalSizeBytes) }}</span>
          </div>
        </div>

        <!-- 磁盘容量：圆角进度条 + 右侧数值 -->
        <div class="capacity">
          <div class="capacity-head">
            <span class="capacity-label">磁盘占用</span>
            <span class="capacity-value">
              {{ formatBytes(info.diskUsableBytes) }} 可用 / {{ formatBytes(info.diskTotalBytes) }}
              <span v-if="diskUsagePercent !== null" class="capacity-percent">已用 {{ diskUsagePercent }}%</span>
            </span>
          </div>
          <div class="capacity-track">
            <div class="capacity-fill" :style="{ width: `${diskUsagePercent ?? 0}%` }" />
          </div>
        </div>

        <el-table :data="info.directories" border class="directory-table">
          <el-table-column prop="name" label="目录 / 分类" min-width="200" show-overflow-tooltip />
          <el-table-column label="文件数" width="120" align="right" class-name="number-col">
            <template #default="{ row }">{{ (row as StorageDirectoryStat).fileCount }}</template>
          </el-table-column>
          <el-table-column label="占用空间" width="160" align="right" class-name="number-col">
            <template #default="{ row }">{{ formatBytes((row as StorageDirectoryStat).sizeBytes) }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无目录数据" />
          </template>
        </el-table>

        <p class="hint">
          如需更换存储目录，请修改后端 FILE_UPLOAD_DIR 环境变量或容器数据卷挂载后重启服务，本页仅展示当前路径。
        </p>
      </template>

      <el-empty v-else-if="!loading && !error" class="empty-state" description="暂无存储信息" />
    </div>
  </div>
</template>

<style scoped>
/* 信息卡片：20px 内边距 */
.storage-card {
  padding: 20px;
}

.card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-4);
}

.card-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--ceams-space-2);
}

.card-title {
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2);
}

.error-alert {
  margin-bottom: var(--ceams-space-4);
}

/* 字段网格：两列 label / value */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ceams-space-3) var(--ceams-space-4);
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: var(--ceams-space-3);
  padding: 12px 14px;
  background-color: var(--ceams-bg-elevated);
  border-radius: var(--ceams-radius-md);
}

.info-label {
  flex-shrink: 0;
  width: 72px;
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
}

.info-value {
  min-width: 0;
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-primary);
  word-break: break-all;
}

/* 容量条：标题行 + 圆角进度条，数值右对齐 */
.capacity {
  margin-top: var(--ceams-space-4);
  padding: 14px 16px;
  background-color: var(--ceams-bg-elevated);
  border-radius: var(--ceams-radius-md);
}

.capacity-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--ceams-space-2);
  margin-bottom: var(--ceams-space-2);
}

.capacity-label {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
}

.capacity-value {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-primary);
  font-variant-numeric: tabular-nums;
  text-align: right;
}

.capacity-percent {
  margin-left: var(--ceams-space-2);
  color: var(--ceams-color-primary);
  font-weight: 600;
}

.capacity-track {
  height: 8px;
  overflow: hidden;
  background-color: #e2e8f0;
  border-radius: 999px;
}

.capacity-fill {
  height: 100%;
  background-color: var(--ceams-color-primary);
  border-radius: 999px;
  transition: width 0.24s ease;
}

.directory-table {
  margin-top: var(--ceams-space-4);
}

/* 数字列右对齐等宽 */
:deep(.number-col .cell) {
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}

/* 空态：居中灰色 + 40px 留白 */
.empty-state {
  padding: 40px 0;
}

:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}

.hint {
  margin-top: var(--ceams-space-3);
  font-size: var(--ceams-font-size-xs);
  line-height: 1.6;
  color: var(--ceams-text-secondary);
}

/* 窄屏时字段网格降为单列 */
@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
