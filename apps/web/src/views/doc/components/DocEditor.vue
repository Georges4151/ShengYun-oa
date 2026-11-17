<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ElAlert, ElButton, ElDialog } from 'element-plus'
import { ApiError } from '@ceams/api-client'
import { ceamsApi, apiBaseUrl } from '@/services/http'
import type { EditorConfigVO, Id } from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'DocEditor' })

const props = defineProps<{
  modelValue: boolean
  fileId: Id | null
  /** edit 需要先加锁；view 为只读预览 */
  mode: 'edit' | 'view'
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

interface DocEditorInstance {
  destroyEditor?: () => void
}

interface DocsApiNamespace {
  DocEditor: new (elementId: string, config: Record<string, unknown>) => DocEditorInstance
}

/** 全局仅注入一次 api.js：按最终地址缓存加载 Promise，多文件 / 重复打开复用。 */
const apiLoaders = new Map<string, Promise<void>>()

/** 唯一容器 ID（同一页面可能复用组件实例）。 */
let editorSeq = 0
const containerId = `ceams-doc-editor-${(editorSeq += 1)}`

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const loading = ref(false)
/** 降级态：503（在线编辑不可用）或任一二义失败时显示提示 + 下载入口，不抛全局错误。 */
const degraded = ref(false)
const degradedMessage = ref('')
const editor = ref<DocEditorInstance | null>(null)
/** 本次是否由当前用户加锁（关闭时负责释放） */
const lockedByMe = ref(false)

function docsApi(): DocsApiNamespace | undefined {
  return (window as unknown as { DocsAPI?: DocsApiNamespace }).DocsAPI
}

function loadScript(src: string): Promise<void> {
  const href = new URL(src, window.location.href).href
  const cached = apiLoaders.get(href)
  if (cached) return cached

  const promise = new Promise<void>((resolve, reject) => {
    const existed = Array.from(document.scripts).find((item) => item.src === href)
    if (existed) {
      if (docsApi()) {
        resolve()
        return
      }
      existed.addEventListener('load', () => resolve(), { once: true })
      existed.addEventListener('error', () => reject(new Error('OnlyOffice api.js 加载失败')), {
        once: true
      })
      return
    }
    const script = document.createElement('script')
    script.src = href
    script.async = true
    script.dataset.ceamsOnlyoffice = 'true'
    script.addEventListener('load', () => resolve(), { once: true })
    script.addEventListener('error', () => reject(new Error('OnlyOffice api.js 加载失败')), {
      once: true
    })
    document.head.appendChild(script)
  })

  apiLoaders.set(href, promise)
  // 加载失败允许下次重试
  promise.catch(() => apiLoaders.delete(href))
  return promise
}

function destroyEditor(): void {
  try {
    editor.value?.destroyEditor?.()
  } catch {
    // 销毁失败不影响关闭流程
  }
  editor.value = null
}

async function releaseLock(): Promise<void> {
  if (!lockedByMe.value || !props.fileId) return
  const fileId = props.fileId
  lockedByMe.value = false
  try {
    await ceamsApi.doc.locks.unlock(fileId)
  } catch {
    // 锁可能已被保存回调释放或已过期，忽略
  }
}

function degrade(err: unknown, fallback: string): void {
  degraded.value = true
  // 503 探活失败与无编辑锁等业务失败均在此提示（不走全局错误弹窗）
  degradedMessage.value =
    err instanceof ApiError && err.status === 503
      ? '在线编辑暂不可用，可下载后查看'
      : apiErrorMessage(err, fallback)
}

/** 等待容器渲染完成（el-dialog 懒渲染，最多等待 1s）。 */
async function waitForContainer(): Promise<void> {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    await nextTick()
    if (document.getElementById(containerId)) return
    await new Promise<void>((resolve) => {
      window.setTimeout(resolve, 50)
    })
  }
}

async function openEditor(): Promise<void> {
  if (!props.fileId) return
  // 非 Office 类型不进入编辑器（页面侧已按扩展名过滤，此处兜底）
  destroyEditor()
  degraded.value = false
  degradedMessage.value = ''
  loading.value = true
  try {
    // 编辑模式：后端要求本人持锁才能生成 editor-config
    if (props.mode === 'edit') {
      await ceamsApi.doc.locks.lock(props.fileId)
      lockedByMe.value = true
    }
    const config: EditorConfigVO = await ceamsApi.doc.editorConfig(props.fileId)
    await loadScript(
      `${config.browserDocumentServerUrl.replace(/\/+$/, '')}/web-apps/apps/api/documents/api.js`
    )
    const namespace = docsApi()
    if (!namespace) {
      throw new Error('OnlyOffice api.js 加载失败')
    }
    await waitForContainer()
    editor.value = new namespace.DocEditor(containerId, {
      ...config.config,
      token: config.token
    })
  } catch (err) {
    degrade(err, '在线编辑暂不可用，可下载后查看')
    await releaseLock()
  } finally {
    loading.value = false
  }
}

/** 降级时的一键下载：一次性 token + 匿名下载地址。 */
async function downloadCurrent(): Promise<void> {
  if (!props.fileId) return
  try {
    const token = await ceamsApi.doc.files.issueDownloadToken(props.fileId)
    window.open(`${apiBaseUrl}${ceamsApi.doc.files.downloadUrl(props.fileId, token)}`, '_blank')
  } catch (err) {
    degradedMessage.value = apiErrorMessage(err, '下载失败，请稍后重试')
  }
}

watch(
  () => [props.modelValue, props.fileId, props.mode] as const,
  async ([value]) => {
    if (value) {
      await openEditor()
      return
    }
    destroyEditor()
    await releaseLock()
  }
)

function onClose(): void {
  visible.value = false
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'edit' ? '在线编辑' : '在线预览'"
    width="88%"
    top="4vh"
    :close-on-click-modal="false"
  >
    <div v-loading="loading" class="editor-wrapper">
      <el-alert
        v-if="degraded"
        type="warning"
        :closable="false"
        show-icon
        :title="degradedMessage"
        class="degraded-alert"
      >
        <el-button type="primary" link @click="downloadCurrent">下载文件</el-button>
      </el-alert>
      <!-- OnlyOffice 挂载容器：降级时隐藏 -->
      <div v-show="!degraded" :id="containerId" class="editor-container" />
    </div>
    <template #footer>
      <el-button @click="onClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.editor-wrapper {
  min-height: 70vh;
}
.editor-container {
  height: 70vh;
}
.degraded-alert {
  margin-bottom: 12px;
}
</style>
