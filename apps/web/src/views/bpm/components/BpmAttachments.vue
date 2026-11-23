<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElButton, ElImageViewer, ElMessage } from 'element-plus'
import { fetchInstanceFile } from '@/services/http'
import type { BizAttachmentGroupVO, BizAttachmentVO } from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'
import { saveBlob } from '@/utils/file'

defineOptions({ name: 'BpmAttachments' })

/**
 * 审批单据附件展示。
 *
 * 附件下载接口要求登录态（Authorization），无法用 `<img src>` / `window.open` 直接访问：
 * - 图片：先拉取 Blob 并生成 objectURL 作为缩略图，点击后用 el-image-viewer 在大图间切换；
 * - PDF / OFFICE / OTHER：点击「查看」时拉取 Blob，在新窗口打开；弹窗被拦截时退化为下载。
 * 所有 objectURL 在组件卸载（弹窗关闭）时统一回收，避免内存泄漏。
 */
const props = defineProps<{
  /** 流程实例 ID：受控附件接口按实例校验业务归属。 */
  instanceId: string
  /** 附件分组（后端无附件时为空数组）。 */
  groups: BizAttachmentGroupVO[]
}>()

/** 单张图片的加载状态：加载中 / 已就绪 / 失败 */
interface ImageState {
  status: 'loading' | 'ready' | 'error'
  url?: string
}

const imageStates = reactive<Record<string, ImageState>>({})
/** 已生成的 objectURL，供卸载时统一回收 */
const objectUrls = new Set<string>()
/** 组件是否已卸载：用于丢弃迟到的异步结果 */
let disposed = false
/** 加载批次号：分组整体替换后，旧批次迟到的结果直接丢弃 */
let generation = 0

function imageKey(groupIndex: number, itemIndex: number): string {
  return `${groupIndex}-${itemIndex}`
}

/** 图片是否就绪（仅就绪项参与大图预览与点击） */
function imageUrl(groupIndex: number, itemIndex: number): string | undefined {
  return imageStates[imageKey(groupIndex, itemIndex)]?.url
}

/** 拉取单张图片并生成 objectURL；失败只标记该图，不影响同组其他附件。 */
async function loadImage(
  groupIndex: number,
  itemIndex: number,
  item: BizAttachmentVO,
  currentGeneration: number
): Promise<void> {
  const key = imageKey(groupIndex, itemIndex)
  imageStates[key] = { status: 'loading' }
  try {
    const blob = await fetchInstanceFile(props.instanceId, item.name)
    if (disposed || currentGeneration !== generation) return
    const url = URL.createObjectURL(blob)
    objectUrls.add(url)
    imageStates[key] = { status: 'ready', url }
  } catch {
    if (disposed || currentGeneration !== generation) return
    imageStates[key] = { status: 'error' }
  }
}

/** 回收全部 objectURL 并清空图片状态（分组变化或组件卸载时调用）。 */
function resetImages(): void {
  for (const url of objectUrls) {
    URL.revokeObjectURL(url)
  }
  objectUrls.clear()
  for (const key of Object.keys(imageStates)) {
    delete imageStates[key]
  }
}

/** 按当前分组加载全部图片（逐张独立加载，互不阻塞）。 */
function loadAllImages(): void {
  generation += 1
  const currentGeneration = generation
  resetImages()
  props.groups.forEach((group, groupIndex) => {
    group.items.forEach((item, itemIndex) => {
      if (item.kind !== 'IMAGE') return
      void loadImage(groupIndex, itemIndex, item, currentGeneration)
    })
  })
}

onMounted(loadAllImages)

// 详情弹窗复用组件时分组可能整体替换，重新加载避免展示旧实例的附件
watch(() => props.groups, loadAllImages)

onBeforeUnmount(() => {
  disposed = true
  resetImages()
})

// ---------------- 大图预览 ----------------
const viewerVisible = ref(false)
const viewerUrls = ref<string[]>([])
const viewerIndex = ref(0)

/** 打开同组内的大图预览：仅纳入已加载成功的图片，并定位到当前点击项。 */
function openViewer(groupIndex: number, itemIndex: number): void {
  const group = props.groups[groupIndex]
  if (!group) return
  const readyIndexes = group.items
    .map((item, index) => ({ item, index }))
    .filter(({ item, index }) => item.kind === 'IMAGE' && imageStates[imageKey(groupIndex, index)]?.status === 'ready')
    .map(({ index }) => index)
  const position = readyIndexes.indexOf(itemIndex)
  if (position < 0) return
  viewerUrls.value = readyIndexes.map((index) => imageStates[imageKey(groupIndex, index)]!.url!)
  viewerIndex.value = position
  viewerVisible.value = true
}

// ---------------- 非图片附件 ----------------
const fileLoading = reactive<Record<string, boolean>>({})

function fileKey(groupIndex: number, itemIndex: number): string {
  return `${groupIndex}-${itemIndex}`
}

/**
 * 打开非图片附件：拉取 Blob 后新窗口展示。
 * 浏览器拦截弹窗（window.open 返回 null）时退化为本地下载，保证用户仍能拿到文件。
 */
async function openFile(groupIndex: number, itemIndex: number, item: BizAttachmentVO): Promise<void> {
  const key = fileKey(groupIndex, itemIndex)
  fileLoading[key] = true
  try {
    const blob = await fetchInstanceFile(props.instanceId, item.name)
    const url = URL.createObjectURL(blob)
    const win = window.open(url, '_blank')
    if (!win) {
      URL.revokeObjectURL(url)
      saveBlob(blob, item.fileName || item.name)
    } else {
      // 新窗口加载需要时间，延迟释放 objectURL
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    }
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '无法预览，请下载'))
  } finally {
    fileLoading[key] = false
  }
}
</script>

<template>
  <div v-if="groups.length" class="bpm-attachments">
    <div v-for="(group, groupIndex) in groups" :key="`${groupIndex}-${group.groupName}`" class="attachment-group">
      <div class="attachment-group__title">{{ group.groupName }}</div>

      <!-- 图片：固定尺寸缩略图网格，点击打开大图预览 -->
      <div v-if="group.items.some((item) => item.kind === 'IMAGE')" class="image-grid">
        <template v-for="(item, itemIndex) in group.items" :key="`img-${itemIndex}`">
          <div v-if="item.kind === 'IMAGE'" class="thumb">
            <img
              v-if="imageStates[imageKey(groupIndex, itemIndex)]?.status === 'ready'"
              :src="imageUrl(groupIndex, itemIndex)"
              :alt="item.fileName || item.name"
              class="thumb__image"
              @click="openViewer(groupIndex, itemIndex)"
            >
            <div v-else-if="imageStates[imageKey(groupIndex, itemIndex)]?.status === 'error'" class="thumb__placeholder">
              加载失败
            </div>
            <div v-else class="thumb__placeholder">加载中…</div>
          </div>
        </template>
      </div>

      <!-- PDF / OFFICE / OTHER：文件名 + 查看 -->
      <div v-if="group.items.some((item) => item.kind !== 'IMAGE')" class="file-list">
        <template v-for="(item, itemIndex) in group.items" :key="`file-${itemIndex}`">
          <div v-if="item.kind !== 'IMAGE'" class="file-row">
            <span class="file-row__name" :title="item.fileName || item.name">
              {{ item.fileName || item.name }}
            </span>
            <el-button
              link
              type="primary"
              size="small"
              :loading="fileLoading[fileKey(groupIndex, itemIndex)]"
              @click="openFile(groupIndex, itemIndex, item)"
            >
              查看
            </el-button>
          </div>
        </template>
      </div>
    </div>

    <el-image-viewer
      v-if="viewerVisible"
      :url-list="viewerUrls"
      :initial-index="viewerIndex"
      teleported
      hide-on-click-modal
      @close="viewerVisible = false"
    />
  </div>
</template>

<style scoped>
/* 附件区域：与上方字段列表保持 16px 间距 */
.bpm-attachments {
  margin-top: var(--ceams-space-4, 16px);
}

.attachment-group {
  margin-bottom: var(--ceams-space-3, 12px);
}

.attachment-group:last-child {
  margin-bottom: 0;
}

.attachment-group__title {
  margin-bottom: var(--ceams-space-2, 8px);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-sm, 14px);
  font-weight: 600;
}

/* 缩略图网格：固定 88px 方形，圆角，超出换行 */
.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ceams-space-2, 8px);
}

.thumb {
  width: 88px;
  height: 88px;
  overflow: hidden;
  border: 1px solid var(--ceams-border-divider);
  border-radius: var(--ceams-radius-sm, 6px);
  background: var(--ceams-bg-elevated);
}

.thumb__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: zoom-in;
}

.thumb__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 6px;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
  text-align: center;
}

/* 文件行：文件名省略展示，操作按钮靠右 */
.file-list {
  display: flex;
  flex-direction: column;
  gap: var(--ceams-space-1, 4px);
}

.file-row {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2, 8px);
  font-size: var(--ceams-font-size-sm, 14px);
}

.file-row__name {
  overflow: hidden;
  color: var(--ceams-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
