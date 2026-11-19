<script setup lang="ts">
/**
 * 知识库（PUBLIC 库）只读浏览：
 * 目录树 → 文件列表 → 文件元信息与预览地址复制。
 * 仅使用只读接口（目录树 / 文件分页 / 预览地址），不做上传、编辑与 OnlyOffice 回写。
 */
import { computed, ref } from 'vue'
import { onReachBottom } from '@dcloudio/uni-app'
import type { DirectoryNodeVO, DocFileVO } from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { apiClient } from '../../platform/http'
import { API_BASE_URL } from '../../platform/config'
import { apiErrorMessage } from '../../utils/error'

interface VisibleDirectory {
  node: DirectoryNodeVO
  depth: number
}

const SIZE = 20

const loadingTree = ref(false)
const tree = ref<DirectoryNodeVO[]>([])
const expanded = ref<Record<string, boolean>>({})

const activeDir = ref<DirectoryNodeVO | null>(null)
const loadingFiles = ref(false)
const files = ref<DocFileVO[]>([])
const page = ref(0)
const total = ref(0)
const finished = ref(false)

const selectedFile = ref<DocFileVO | null>(null)

/** 展开状态展开为扁平列表，便于小程序列表渲染 */
const visibleDirectories = computed<VisibleDirectory[]>(() => {
  const result: VisibleDirectory[] = []
  const walk = (nodes: DirectoryNodeVO[], depth: number): void => {
    for (const node of nodes) {
      result.push({ node, depth })
      if (expanded.value[node.id] && node.children?.length) {
        walk(node.children, depth + 1)
      }
    }
  }
  walk(tree.value, 0)
  return result
})

const treeEmpty = computed<boolean>(() => !loadingTree.value && tree.value.length === 0)
const filesEmpty = computed<boolean>(
  () => !loadingFiles.value && activeDir.value !== null && files.value.length === 0
)
const noMore = computed<boolean>(() => files.value.length > 0 && finished.value)

function formatSize(size: string | null): string {
  const bytes = Number(size ?? '')
  if (!Number.isFinite(bytes) || bytes <= 0) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function fullUrl(path: string): string {
  return path.startsWith('http') ? path : `${API_BASE_URL}${path}`
}

async function loadTree(): Promise<void> {
  if (loadingTree.value) return
  loadingTree.value = true
  try {
    tree.value = await apiClient.doc.directories.tree({ libraryType: 'PUBLIC' })
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载知识库失败'), icon: 'none' })
  } finally {
    loadingTree.value = false
  }
}

async function loadFiles(reset: boolean): Promise<void> {
  const dir = activeDir.value
  if (!dir || loadingFiles.value) return
  if (!reset && finished.value) return
  loadingFiles.value = true
  const nextPage = reset ? 1 : page.value + 1
  try {
    const res = await apiClient.doc.files.list({ dirId: dir.id, page: nextPage, size: SIZE })
    files.value = reset ? res.records : files.value.concat(res.records)
    page.value = nextPage
    total.value = res.total
    finished.value = files.value.length >= res.total || res.records.length < SIZE
  } catch (err) {
    uni.showToast({ title: apiErrorMessage(err, '加载文件失败'), icon: 'none' })
    if (reset) {
      files.value = []
      page.value = 0
      total.value = 0
      finished.value = false
    }
  } finally {
    loadingFiles.value = false
  }
}

function toggleDirectory(node: DirectoryNodeVO): void {
  if (node.children?.length) {
    expanded.value = { ...expanded.value, [node.id]: !expanded.value[node.id] }
  }
  // 目录既有子目录也可能直接存放文件：展开与选中同时生效
  selectDirectory(node)
}

function selectDirectory(node: DirectoryNodeVO): void {
  activeDir.value = node
  selectedFile.value = null
  files.value = []
  page.value = 0
  total.value = 0
  finished.value = false
  void loadFiles(true)
}

/** 仅返回预览地址（需登录态）；小程序无法携带 Authorization 打开 webview，故复制地址并提示 */
function copyPreviewUrl(): void {
  const file = selectedFile.value
  if (!file) return
  const url = fullUrl(apiClient.doc.files.previewUrl(file.id))
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: '预览地址已复制，请在 Web 端登录后打开', icon: 'none' })
  })
}

void loadTree()

onReachBottom(() => {
  if (!activeDir.value || loadingFiles.value || finished.value) return
  void loadFiles(false)
})
</script>

<template>
  <view class="ceams-page">
    <view class="ceams-card">
      <view class="ceams-section-title section-title">公开知识库</view>
      <text class="section-hint">仅支持浏览与查看，上传、编辑请前往 Web 端</text>

      <view v-if="loadingTree && tree.length === 0" class="state">加载中...</view>
      <empty-state
        v-else-if="treeEmpty"
        title="暂无公开知识库"
        description="管理员在 Web 端建立公开目录后会在此显示"
      />

      <view v-else class="tree">
        <view
          v-for="item in visibleDirectories"
          :key="item.node.id"
          class="tree__node"
          :class="{ 'tree__node--active': activeDir?.id === item.node.id }"
          :style="{ paddingLeft: item.depth * 32 + 'rpx' }"
          @click="toggleDirectory(item.node)"
        >
          <text class="tree__icon">{{ item.node.children?.length ? (expanded[item.node.id] ? '▾' : '▸') : '·' }}</text>
          <text class="tree__name ceams-ellipsis">{{ item.node.name }}</text>
        </view>
      </view>
    </view>

    <view v-if="activeDir" class="ceams-card">
      <view class="ceams-section-title section-title">{{ activeDir.name }}</view>

      <view v-if="loadingFiles && files.length === 0" class="state">加载中...</view>
      <empty-state v-else-if="filesEmpty" title="暂无文件" description="该目录下还没有文件" />

      <view
        v-for="item in files"
        :key="item.id"
        class="file"
        :class="{ 'file--active': selectedFile?.id === item.id }"
        @click="selectedFile = item"
      >
        <view class="file__main">
          <text class="file__name ceams-ellipsis">{{ item.name }}</text>
          <text class="file__meta">
            {{ formatSize(item.size) }} · 第 {{ item.currentVersionNo ?? 1 }} 版 · 更新
            {{ formatDateTime(item.updateTime ?? item.createTime) }}
          </text>
        </view>
        <text class="ceams-badge file__badge">{{ item.ext || '文件' }}</text>
      </view>

      <view v-if="noMore" class="loadmore">没有更多了</view>
      <view v-else-if="files.length > 0" class="loadmore">上拉加载更多</view>
    </view>

    <text v-if="!activeDir && !treeEmpty" class="tip">请选择左侧目录查看文件</text>

    <view v-if="selectedFile" class="ceams-card">
      <view class="preview__head">
        <view class="ceams-section-title section-title section-title--flush">文件信息</view>
        <text class="link" @click="selectedFile = null">关闭</text>
      </view>
      <view class="row">
        <text class="row__label">文件名</text>
        <text class="row__value ceams-ellipsis">{{ selectedFile.name }}</text>
      </view>
      <view class="row">
        <text class="row__label">大小</text>
        <text class="row__value">{{ formatSize(selectedFile.size) }}</text>
      </view>
      <view class="row">
        <text class="row__label">版本</text>
        <text class="row__value">第 {{ selectedFile.currentVersionNo ?? 1 }} 版</text>
      </view>
      <view class="row">
        <text class="row__label">更新时间</text>
        <text class="row__value">{{ formatDateTime(selectedFile.updateTime ?? selectedFile.createTime) }}</text>
      </view>

      <button class="ceams-btn ceams-btn--ghost preview__btn" @click="copyPreviewUrl">复制预览地址</button>
      <text class="preview__hint">小程序端不支持在线预览，请在 Web 端登录后打开；如需下载同样请前往 Web 端。</text>
    </view>
  </view>
</template>

<style scoped>
/* 分组标题沿用全局 .ceams-section-title */
.section-title {
  margin-bottom: var(--ceams-space-xs);
}

.section-title--flush {
  margin-bottom: 0;
}

.section-hint {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
}

.tree {
  margin-top: var(--ceams-space-sm);
}

.tree__node {
  display: flex;
  align-items: center;
  padding: var(--ceams-space-sm) var(--ceams-space-2xs);
  border-radius: var(--ceams-radius-md);
}

.tree__node--active {
  background-color: var(--ceams-color-primary-tint);
}

.tree__icon {
  width: 40rpx;
  flex-shrink: 0;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  text-align: center;
}

.tree__name {
  flex: 1;
  min-width: 0;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
}

/* 文件行：与 list-card 保持一致的圆角与内边距 */
.file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--ceams-space-sm);
  padding: 24rpx;
  background-color: var(--ceams-bg-subtle);
  border-radius: 16rpx;
  border: var(--ceams-border-width) solid transparent;
}

.file--active {
  border-color: var(--ceams-color-primary);
}

.file__main {
  flex: 1;
  min-width: 0;
  margin-right: var(--ceams-space-sm);
}

.file__name {
  display: block;
  color: var(--ceams-text-primary);
  font-size: 28rpx;
  font-weight: var(--ceams-font-weight-semibold);
}

.file__meta {
  display: block;
  margin-top: var(--ceams-space-2xs);
  color: var(--ceams-text-secondary);
  font-size: 24rpx;
}

/* 文件类型徽标沿用全局 .ceams-badge，仅控制收缩与间距 */
.file__badge {
  flex-shrink: 0;
  margin-left: var(--ceams-space-sm);
}

.preview__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ceams-space-xs);
}

.link {
  color: var(--ceams-text-link);
  font-size: var(--ceams-font-size-caption);
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ceams-space-xs) 0;
  border-bottom: var(--ceams-border-width) solid var(--ceams-divider);
}

.row:last-of-type {
  border-bottom: none;
}

.row__label {
  flex-shrink: 0;
  margin-right: var(--ceams-space-md);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-caption);
}

.row__value {
  flex: 1;
  min-width: 0;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-caption);
  text-align: right;
}

.preview__btn {
  margin-top: var(--ceams-space-md);
}

.preview__hint {
  display: block;
  margin-top: var(--ceams-space-xs);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-mini);
  line-height: var(--ceams-line-height-normal);
  text-align: center;
}

.tip {
  display: block;
  padding: var(--ceams-space-xl) 0;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-caption);
  text-align: center;
}

.state {
  padding: var(--ceams-space-xl) 0;
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
