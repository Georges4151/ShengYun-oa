<script setup lang="ts">
import { computed } from 'vue'
import { ElButton, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { formatDateTime } from '@ceams/utils'
import type { DocFileVO, Id } from '@ceams/types'
import { docFileStatusLabels } from '@/utils/labels'

defineOptions({ name: 'FileTable' })

const props = defineProps<{
  files: DocFileVO[]
  loading: boolean
  /** 上传人 ID -> 姓名（用户列表加载失败时回退显示 ID） */
  userNames: Record<string, string>
  canEdit: boolean
  canDelete: boolean
  canPermission: boolean
}>()

const emit = defineEmits<{
  preview: [row: DocFileVO]
  download: [row: DocFileVO]
  edit: [row: DocFileVO]
  versions: [row: DocFileVO]
  permission: [row: DocFileVO]
  remove: [row: DocFileVO]
}>()

/** 可在线编辑的 Office 扩展名（与后端 EDITABLE_EXTENSIONS 保持一致）。 */
const OFFICE_EXTENSIONS = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']
/** 可浏览器内联预览的扩展名（与后端 previewContentType 保持一致）。 */
const PREVIEW_EXTENSIONS = ['png', 'jpg', 'jpeg', 'webp', 'pdf', 'txt', 'md', 'csv']

function extOf(row: DocFileVO): string {
  return (row.ext ?? '').toLowerCase()
}

function isOffice(row: DocFileVO): boolean {
  return OFFICE_EXTENSIONS.includes(extOf(row))
}

function canPreview(row: DocFileVO): boolean {
  return PREVIEW_EXTENSIONS.includes(extOf(row))
}

/**
 * 预览按钮显隐：
 * - 图片 / PDF / 文本：浏览器内联预览
 * - Office：无编辑权限时也允许只读预览（走 OnlyOffice，mode=view）
 */
function showPreview(row: DocFileVO): boolean {
  return canPreview(row) || (isOffice(row) && !props.canEdit)
}

/** 文件类型角标：Office / 图片 / PDF / 文本 / 压缩包 / 其他。 */
function fileBadge(row: DocFileVO): { text: string; type: string } {
  const ext = extOf(row)
  if (['doc', 'docx'].includes(ext)) return { text: 'W', type: 'word' }
  if (['xls', 'xlsx'].includes(ext)) return { text: 'X', type: 'excel' }
  if (['ppt', 'pptx'].includes(ext)) return { text: 'P', type: 'ppt' }
  if (ext === 'pdf') return { text: 'PDF', type: 'pdf' }
  if (['png', 'jpg', 'jpeg', 'webp', 'gif', 'bmp'].includes(ext)) {
    return { text: 'IMG', type: 'image' }
  }
  if (['txt', 'md', 'csv'].includes(ext)) return { text: 'TXT', type: 'text' }
  if (['zip', 'rar', '7z'].includes(ext)) return { text: 'ZIP', type: 'archive' }
  return { text: 'FILE', type: 'other' }
}

/** 字节数格式化为 B / KB / MB / GB（后端 size 为字符串 Long）。 */
function formatSize(value: string | null): string {
  if (value === null || value === '') return '-'
  const bytes = Number(value)
  if (!Number.isFinite(bytes) || bytes < 0) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`
}

function uploaderName(id: Id | null): string {
  if (!id) return '-'
  return props.userNames[id] ?? id
}

/** 行内按钮总数，用于估算操作列宽度。 */
const actionColumnWidth = computed(() => {
  const count = 2 + (props.canEdit ? 1 : 0) + (props.canPermission ? 1 : 0) + (props.canDelete ? 1 : 0)
  return `${Math.max(180, count * 52)}px`
})
</script>

<template>
  <el-table v-loading="loading" :data="files" border>
    <el-table-column label="名称" min-width="260">
      <template #default="{ row }">
        <div class="file-name">
          <span class="file-badge" :class="`badge-${fileBadge(row as DocFileVO).type}`">
            {{ fileBadge(row as DocFileVO).text }}
          </span>
          <span class="file-text">{{ (row as DocFileVO).name }}</span>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="大小" width="110" align="right">
      <template #default="{ row }">{{ formatSize((row as DocFileVO).size) }}</template>
    </el-table-column>
    <el-table-column label="版本" width="100">
      <template #default="{ row }">
        <span class="version-text">v{{ (row as DocFileVO).currentVersionNo ?? 1 }}</span>
        <el-tag
          v-if="(row as DocFileVO).status === 'LOCKED'"
          size="small"
          type="warning"
          class="lock-tag"
        >
          {{ docFileStatusLabels.LOCKED }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="上传人" width="110">
      <template #default="{ row }">{{ uploaderName((row as DocFileVO).uploadedBy) }}</template>
    </el-table-column>
    <el-table-column label="更新时间" width="170">
      <template #default="{ row }">{{ formatDateTime((row as DocFileVO).updateTime) }}</template>
    </el-table-column>
    <el-table-column label="操作" :width="actionColumnWidth" fixed="right">
      <template #default="{ row }">
        <el-button
          v-if="showPreview(row as DocFileVO)"
          link
          type="primary"
          size="small"
          @click="emit('preview', row as DocFileVO)"
        >
          预览
        </el-button>
        <el-button link type="primary" size="small" @click="emit('download', row as DocFileVO)">
          下载
        </el-button>
        <el-button
          v-if="isOffice(row as DocFileVO) && canEdit"
          link
          type="primary"
          size="small"
          @click="emit('edit', row as DocFileVO)"
        >
          编辑
        </el-button>
        <el-button link type="primary" size="small" @click="emit('versions', row as DocFileVO)">
          版本
        </el-button>
        <el-button
          v-if="canPermission"
          link
          type="primary"
          size="small"
          @click="emit('permission', row as DocFileVO)"
        >
          授权
        </el-button>
        <el-button
          v-if="canDelete"
          link
          type="danger"
          size="small"
          @click="emit('remove', row as DocFileVO)"
        >
          删除
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.file-name {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2);
  min-width: 0;
}

.file-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 文件类型角标：统一圆角与字号 */
.file-badge {
  flex: none;
  width: 34px;
  height: 22px;
  border-radius: var(--ceams-radius-xs);
  color: var(--ceams-text-inverse);
  font-size: var(--ceams-font-size-xs);
  font-weight: 600;
  line-height: 22px;
  text-align: center;
  background: var(--ceams-color-info);
}

/* 各文件类型保留品牌色以便快速识别 */
.badge-word {
  background: #2b579a;
}

.badge-excel {
  background: #217346;
}

.badge-ppt {
  background: #b7472a;
}

.badge-pdf {
  background: #d93025;
}

.badge-image {
  background: #7b61ff;
}

.badge-text {
  background: var(--ceams-color-info);
}

.badge-archive {
  background: #d48806;
}

.version-text {
  color: var(--ceams-text-primary);
}

.lock-tag {
  margin-left: var(--ceams-space-2);
}

/* 空态居中灰色、40px 留白 */
:deep(.el-table__empty-text) {
  color: var(--ceams-text-placeholder);
}

:deep(.el-table__empty-block) {
  padding: var(--ceams-space-10) 0;
  min-height: 0;
}
</style>
