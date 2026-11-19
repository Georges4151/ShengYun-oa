<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
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
  <div v-loading="loading">
    <div class="toolbar">
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="error" type="error" :closable="false" show-icon :title="error" class="error-alert">
      <el-button link type="primary" size="small" @click="load">重试</el-button>
    </el-alert>

    <template v-if="info">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="配置目录">{{ info.configuredPath || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际路径">{{ info.absolutePath || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目录状态">
          <el-tag v-if="directoryHealthy" type="success" size="small">正常</el-tag>
          <el-tag v-else type="danger" size="small">异常</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="文件总数">{{ info.fileCount }}</el-descriptions-item>
        <el-descriptions-item label="占用空间">{{ formatBytes(info.totalSizeBytes) }}</el-descriptions-item>
        <el-descriptions-item label="磁盘可用 / 总容量">
          {{ formatBytes(info.diskUsableBytes) }} / {{ formatBytes(info.diskTotalBytes) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="info.directories" border class="directory-table">
        <el-table-column prop="name" label="目录 / 分类" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件数" width="120">
          <template #default="{ row }">{{ (row as StorageDirectoryStat).fileCount }}</template>
        </el-table-column>
        <el-table-column label="占用空间" width="160">
          <template #default="{ row }">{{ formatBytes((row as StorageDirectoryStat).sizeBytes) }}</template>
        </el-table-column>
      </el-table>

      <p class="hint">
        如需更换存储目录，请修改后端 FILE_UPLOAD_DIR 环境变量或容器数据卷挂载后重启服务，本页仅展示当前路径。
      </p>
    </template>

    <el-empty v-else-if="!loading && !error" description="暂无存储信息" />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.error-alert {
  margin-bottom: 12px;
}
.directory-table {
  margin-top: 16px;
}
.hint {
  margin-top: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
