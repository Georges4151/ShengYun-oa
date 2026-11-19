<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { fetchBlob } from '@/services/http'
import { previewBlob } from '@/utils/file'

defineOptions({ name: 'RepairImageThumbs' })

const props = withDefaults(
  defineProps<{
    /** 文件服务返回的图片文件名列表。 */
    names: string[]
    /** 缩略图边长（px）。 */
    size?: number
  }>(),
  { size: 64 }
)

interface Thumb {
  name: string
  url: string
  blob: Blob | null
  failed: boolean
}

const thumbs = ref<Thumb[]>([])
/** 加载序号：names 变化或组件卸载后，丢弃过期的异步结果。 */
let loadToken = 0

/** 释放当前所有 objectURL 并清空缩略图。 */
function releaseAll(): void {
  thumbs.value.forEach((item) => {
    if (item.url) URL.revokeObjectURL(item.url)
  })
  thumbs.value = []
}

/** 逐个拉取图片 Blob 并生成 objectURL；失败时保留占位块。 */
async function loadAll(names: string[]): Promise<void> {
  const token = ++loadToken
  releaseAll()
  thumbs.value = names.map((name) => ({ name, url: '', blob: null, failed: false }))
  await Promise.all(
    thumbs.value.map(async (item) => {
      try {
        const blob = await fetchBlob(`/api/v1/files/${encodeURIComponent(item.name)}`)
        if (token !== loadToken) return
        item.blob = blob
        item.url = URL.createObjectURL(blob)
      } catch {
        if (token !== loadToken) return
        item.failed = true
      }
    })
  )
}

watch(
  () => props.names,
  (names) => {
    void loadAll(names ?? [])
  },
  { immediate: true, deep: true }
)

function onPreview(item: Thumb): void {
  if (item.blob) previewBlob(item.blob)
}

onBeforeUnmount(() => {
  loadToken += 1
  releaseAll()
})
</script>

<template>
  <div v-if="thumbs.length === 0" class="thumbs-empty">-</div>
  <div v-else class="thumbs">
    <div
      v-for="(item, index) in thumbs"
      :key="`${item.name}-${index}`"
      class="thumb"
      :style="{ width: `${size}px`, height: `${size}px` }"
    >
      <img
        v-if="item.url"
        :src="item.url"
        :alt="item.name"
        :title="item.name"
        class="thumb__image"
        @click="onPreview(item)"
      />
      <span v-else class="thumb__placeholder">{{ item.failed ? '加载失败' : '加载中' }}</span>
    </div>
  </div>
</template>

<style scoped>
/* 缩略图统一圆角与间距 */
.thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ceams-space-2);
}
.thumb {
  overflow: hidden;
  border: 1px solid var(--ceams-border-base);
  border-radius: var(--ceams-radius-md);
  background: var(--ceams-bg-elevated);
}
.thumb__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}
.thumb__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-xs);
}
.thumbs-empty {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-sm);
}
</style>
