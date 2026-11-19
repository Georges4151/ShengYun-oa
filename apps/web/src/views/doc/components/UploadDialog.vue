<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDialog,
  ElMessage,
  ElProgress,
  ElTag,
  ElUpload
} from 'element-plus'
import type { UploadFile, UploadInstance, UploadUserFile } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { Id, UploadInitVO } from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'UploadDialog' })

const props = defineProps<{
  modelValue: boolean
  /** 目标目录 ID（为空时不可上传） */
  dirId: Id | null
  /** 目标目录名称（仅展示） */
  dirName: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  uploaded: []
}>()

/** 单文件上限 200MB（与后端 ceams.doc.max-file-size 默认值一致）。 */
const MAX_FILE_SIZE = 200 * 1024 * 1024
/** 扩展名白名单（与后端 DocUploadServiceImpl.ALLOWED_EXTENSIONS 一致）。 */
const ALLOWED_EXTENSIONS = [
  'doc',
  'docx',
  'xls',
  'xlsx',
  'ppt',
  'pptx',
  'pdf',
  'txt',
  'md',
  'csv',
  'zip',
  'png',
  'jpg',
  'jpeg',
  'webp'
]
/** 单个分片失败的最大重试次数。 */
const MAX_RETRY = 3
/** 分片大小兜底（后端 init 未返回时使用 5MB）。 */
const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const file = ref<File | null>(null)
/** 阶段：idle 待上传 / hashing 计算校验和 / uploading 上传中 / done 完成 / error 失败 */
const phase = ref<'idle' | 'hashing' | 'uploading' | 'done' | 'error'>('idle')
const progress = ref(0)
const completedChunks = ref(0)
const totalChunks = ref(0)
const errorMessage = ref('')

const extensionOf = (name: string): string => {
  const index = name.lastIndexOf('.')
  return index < 0 ? '' : name.slice(index + 1).toLowerCase()
}

function resetState(): void {
  fileList.value = []
  file.value = null
  phase.value = 'idle'
  progress.value = 0
  completedChunks.value = 0
  totalChunks.value = 0
  errorMessage.value = ''
  uploadRef.value?.clearFiles()
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) resetState()
  }
)

function onFileChange(uploadFile: UploadFile): void {
  const raw = uploadFile.raw ?? null
  fileList.value = raw ? [uploadFile] : []
  file.value = raw
  errorMessage.value = ''
  phase.value = 'idle'
  progress.value = 0
  if (!raw) return

  const ext = extensionOf(raw.name)
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    ElMessage.error(`不支持的文件类型：${ext || '未知'}（仅支持 ${ALLOWED_EXTENSIONS.join(' / ')}）`)
    file.value = null
    fileList.value = []
    return
  }
  if (raw.size <= 0) {
    ElMessage.error('文件内容为空，无法上传')
    file.value = null
    fileList.value = []
    return
  }
  if (raw.size > MAX_FILE_SIZE) {
    ElMessage.error('单个文件不能超过 200MB')
    file.value = null
    fileList.value = []
  }
}

function onExceed(): void {
  uploadRef.value?.clearFiles()
  ElMessage.info('仅支持单文件上传，已清空重新选择')
}

/** 计算整文件 SHA-256（200MB 以内直接整文件读取，浏览器端可接受）。 */
async function computeSha256(target: File): Promise<string> {
  if (!window.crypto?.subtle) return ''
  const buffer = await target.arrayBuffer()
  const digest = await window.crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('')
}

function delay(millis: number): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, millis)
  })
}

/** 单分片上传，失败自动重试（最多 MAX_RETRY 次）。 */
async function uploadChunkWithRetry(uploadId: string, index: number, blob: Blob): Promise<void> {
  let lastError: unknown = null
  for (let attempt = 1; attempt <= MAX_RETRY; attempt += 1) {
    try {
      await ceamsApi.doc.upload.uploadChunk(uploadId, index, blob)
      return
    } catch (err) {
      lastError = err
      await delay(300 * attempt)
    }
  }
  throw lastError ?? new Error(`第 ${index + 1} 个分片上传失败`)
}

async function startUpload(): Promise<void> {
  const target = file.value
  const dirId = props.dirId
  if (!target || !dirId) return

  errorMessage.value = ''
  phase.value = 'hashing'
  progress.value = 0
  completedChunks.value = 0
  totalChunks.value = 0

  try {
    const checksum = await computeSha256(target)
    const init: UploadInitVO = await ceamsApi.doc.upload.initUpload({
      dirId,
      fileName: target.name,
      size: target.size,
      checksum: checksum || undefined
    })

    if (init.instant) {
      phase.value = 'done'
      progress.value = 100
      ElMessage.success('秒传成功：服务器已存在相同内容文件')
      emit('uploaded')
      return
    }
    if (!init.uploadId) {
      throw new Error('上传初始化失败：缺少 uploadId')
    }

    const chunkSize = init.chunkSize > 0 ? init.chunkSize : DEFAULT_CHUNK_SIZE
    const total = Math.max(1, Math.ceil(target.size / chunkSize))
    const uploaded = new Set(init.uploaded)
    totalChunks.value = total
    completedChunks.value = uploaded.size
    progress.value = Math.round((uploaded.size / total) * 100)
    phase.value = 'uploading'

    for (let index = 0; index < total; index += 1) {
      if (uploaded.has(index)) continue
      const start = index * chunkSize
      const blob = target.slice(start, Math.min(start + chunkSize, target.size))
      await uploadChunkWithRetry(init.uploadId, index, blob)
      completedChunks.value += 1
      progress.value = Math.round((completedChunks.value / total) * 100)
    }

    await ceamsApi.doc.upload.completeUpload(init.uploadId)
    phase.value = 'done'
    progress.value = 100
    ElMessage.success('上传完成')
    emit('uploaded')
  } catch (err) {
    phase.value = 'error'
    errorMessage.value = apiErrorMessage(err, '上传失败，请重试')
  }
}

function onClose(): void {
  if (phase.value === 'uploading' || phase.value === 'hashing') {
    ElMessage.warning('正在上传，请等待完成后再关闭')
    return
  }
  visible.value = false
  resetState()
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="上传文件"
    width="580px"
    :close-on-click-modal="false"
    :before-close="() => onClose()"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      :title="`目标目录：${dirName || '未选择目录'}`"
      description="单文件不超过 200MB；支持 doc/docx、xls/xlsx、ppt/pptx、pdf、txt/md/csv、zip、png/jpg/jpeg/webp。"
    />

    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      class="upload-area"
      drag
      :auto-upload="false"
      :limit="1"
      :on-change="onFileChange"
      :on-exceed="onExceed"
      :accept="ALLOWED_EXTENSIONS.map((ext) => `.${ext}`).join(',')"
    >
      <div class="upload-tip">将文件拖到此处，或<em>点击选择</em></div>
    </el-upload>

    <div v-if="phase === 'hashing'" class="progress-area">
      <el-alert type="info" :closable="false" show-icon title="正在计算 SHA-256 校验和，用于秒传与完整性校验" />
    </div>

    <div v-if="phase === 'uploading' || phase === 'done'" class="progress-area">
      <el-progress :percentage="progress" :status="phase === 'done' ? 'success' : ''" />
      <div class="chunk-tip">
        已上传分片 {{ completedChunks }} / {{ totalChunks }}
        <el-tag v-if="phase === 'done'" size="small" type="success">已完成</el-tag>
      </div>
    </div>

    <div v-if="phase === 'error'" class="progress-area">
      <el-alert type="error" :closable="false" show-icon :title="errorMessage" />
      <div class="chunk-tip">
        <el-button size="small" @click="startUpload">重试上传</el-button>
      </div>
    </div>

    <template #footer>
      <el-button @click="onClose">关闭</el-button>
      <el-button
        type="primary"
        :loading="phase === 'hashing' || phase === 'uploading'"
        :disabled="!file || !dirId || phase === 'done'"
        @click="startUpload"
      >
        开始上传
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.upload-area {
  margin-top: 12px;
}
.upload-tip {
  color: #606266;
}
.upload-tip em {
  color: #409eff;
  font-style: normal;
}
.progress-area {
  margin-top: 12px;
}
.chunk-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
