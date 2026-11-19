<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElPagination,
  ElTable,
  ElTableColumn
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { ElectronicSealStampVO, SealStampResultVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'ElectronicSealStampList' })

const query = reactive<{ sealId?: string; projectId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ElectronicSealStampVO[]>([])
const loading = ref(false)

const detailVisible = ref(false)
const stampVisible = ref(false)
const detail = ref<ElectronicSealStampVO | null>(null)
const stampFormRef = ref<FormInstance>()
const stampForm = reactive({
  sealId: '',
  applicationId: '',
  stampPosition: '',
  sealPassword: ''
})
const stampRules: FormRules<typeof stampForm> = {
  sealId: [{ required: true, message: '请输入印章 ID', trigger: 'blur' }],
  applicationId: [{ required: true, message: '请输入用印申请 ID', trigger: 'blur' }]
}

/** 当前印章是否启用密码保护（按印章详情懒加载，仅在启用时展示密码框） */
const sealPasswordProtected = ref(false)
const checkedSealId = ref('')

async function refreshSealPasswordState(sealId: string): Promise<void> {
  const id = sealId.trim()
  if (!id) {
    sealPasswordProtected.value = false
    checkedSealId.value = ''
    return
  }
  if (checkedSealId.value === id) return
  try {
    const detail = await ceamsApi.seal.sealDetail(id)
    checkedSealId.value = id
    sealPasswordProtected.value = detail.passwordProtected === true
  } catch {
    // 印章详情获取失败时不阻断输入，提交时由后端兜底校验
    checkedSealId.value = ''
    sealPasswordProtected.value = false
  }
}

const pdfFile = ref<File | null>(null)
const sealImageFile = ref<File | null>(null)
const stampResult = ref<SealStampResultVO | null>(null)
const stamping = ref(false)
const pdfFileInputRef = ref<HTMLInputElement>()
const sealImageInputRef = ref<HTMLInputElement>()

function readAsBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result
      if (typeof result === 'string') {
        const commaIndex = result.indexOf(',')
        resolve(commaIndex >= 0 ? result.slice(commaIndex + 1) : result)
      } else {
        reject(new Error('读取文件失败'))
      }
    }
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsDataURL(file)
  })
}

function downloadUrl(id: string): string {
  return ceamsApi.seal.stampDownloadUrl(id)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.seal.stampPage({
      page: page.value,
      size: size.value,
      sealId: query.sealId,
      projectId: query.projectId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载盖章记录失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

function openDetail(row: ElectronicSealStampVO): void {
  detail.value = row
  detailVisible.value = true
}

function openStamp(): void {
  Object.assign(stampForm, { sealId: '', applicationId: '', stampPosition: '', sealPassword: '' })
  sealPasswordProtected.value = false
  checkedSealId.value = ''
  pdfFile.value = null
  sealImageFile.value = null
  stampResult.value = null
  stampVisible.value = true
}

function onPdfChange(event: Event): void {
  const input = event.target as HTMLInputElement
  pdfFile.value = input.files?.[0] ?? null
  input.value = ''
}

function onSealImageChange(event: Event): void {
  const input = event.target as HTMLInputElement
  sealImageFile.value = input.files?.[0] ?? null
  input.value = ''
}

async function submitStamp(): Promise<void> {
  if (!stampFormRef.value) return
  const valid = await stampFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!pdfFile.value) {
    ElMessage.warning('请选择源 PDF 文件')
    return
  }
  if (!sealImageFile.value) {
    ElMessage.warning('请选择印章图片')
    return
  }
  // 提交前确认印章是否启用密码保护；启用时必须输入密码
  await refreshSealPasswordState(stampForm.sealId)
  if (sealPasswordProtected.value && !stampForm.sealPassword) {
    ElMessage.warning('该印章已启用密码保护，请输入印章密码')
    return
  }

  stamping.value = true
  try {
    const sourcePdfBase64 = await readAsBase64(pdfFile.value)
    const sealImageBase64 = await readAsBase64(sealImageFile.value)
    const res = await ceamsApi.seal.stamp({
      sealId: stampForm.sealId,
      applicationId: stampForm.applicationId,
      sourcePdfBase64,
      sealImageBase64,
      stampPosition: stampForm.stampPosition || null,
      sealPassword: sealPasswordProtected.value ? stampForm.sealPassword : undefined
    })
    stampResult.value = res
    ElMessage.success('盖章成功（HMAC 自动签名）')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '盖章失败'))
  } finally {
    stamping.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.sealId" placeholder="印章 ID" clearable style="width: 180px" />
      <el-input v-model="query.projectId" placeholder="项目 ID" clearable style="width: 180px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button type="success" @click="openStamp">PDF 盖章</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="sealId" label="印章 ID" width="170" show-overflow-tooltip />
      <el-table-column prop="sealName" label="印章名称" width="130" />
      <el-table-column prop="projectName" label="项目名称" width="130" show-overflow-tooltip />
      <el-table-column prop="contractName" label="合同名称" width="130" show-overflow-tooltip />
      <el-table-column prop="stampPosition" label="盖章位置" width="120" />
      <el-table-column prop="stampTime" label="盖章时间" width="180" />
      <el-table-column prop="userName" label="操作人" width="100" />
      <el-table-column prop="ipAddress" label="IP 地址" width="140" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as ElectronicSealStampVO)">详情</el-button>
          <a :href="downloadUrl((row as ElectronicSealStampVO).id)" target="_blank" rel="noopener" class="download-link">下载</a>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="onSearch"
      @current-change="load"
    />

    <el-dialog v-model="detailVisible" title="盖章记录详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="印章 ID">{{ detail.sealId }}</el-descriptions-item>
        <el-descriptions-item label="印章名称">{{ detail.sealName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ detail.projectName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同名称">{{ detail.contractName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="盖章位置">{{ detail.stampPosition ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="盖章时间">{{ detail.stampTime ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.userName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ detail.ipAddress ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="下载链接" :span="2">
          <a :href="downloadUrl(detail.id)" target="_blank" rel="noopener">{{ downloadUrl(detail.id) }}</a>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="stampVisible" title="PDF 盖章" width="600px">
      <el-form ref="stampFormRef" :model="stampForm" :rules="stampRules" label-width="110px">
        <el-form-item label="印章 ID" prop="sealId">
          <el-input
            v-model="stampForm.sealId"
            placeholder="请输入印章 ID"
            @blur="refreshSealPasswordState(stampForm.sealId)"
          />
        </el-form-item>
        <el-form-item label="用印申请 ID" prop="applicationId">
          <el-input v-model="stampForm.applicationId" placeholder="请输入用印申请 ID" />
        </el-form-item>
        <el-form-item label="盖章位置">
          <el-input v-model="stampForm.stampPosition" placeholder="盖章位置（可选）" />
        </el-form-item>
        <el-form-item v-if="sealPasswordProtected" label="印章密码" required>
          <el-input
            v-model="stampForm.sealPassword"
            type="password"
            show-password
            autocomplete="off"
            placeholder="该印章已启用密码保护，请输入印章密码"
          />
        </el-form-item>
        <el-form-item label="源 PDF 文件" required>
          <div class="file-row">
            <el-button @click="pdfFileInputRef?.click()">选择 PDF</el-button>
            <span class="file-name">{{ pdfFile ? pdfFile.name : '未选择文件' }}</span>
            <input ref="pdfFileInputRef" type="file" accept="application/pdf" style="display: none" @change="onPdfChange" />
          </div>
        </el-form-item>
        <el-form-item label="印章图片" required>
          <div class="file-row">
            <el-button @click="sealImageInputRef?.click()">选择图片</el-button>
            <span class="file-name">{{ sealImageFile ? sealImageFile.name : '未选择文件' }}</span>
            <input ref="sealImageInputRef" type="file" accept="image/*" style="display: none" @change="onSealImageChange" />
          </div>
        </el-form-item>
      </el-form>
      <el-descriptions v-if="stampResult" :column="2" border size="small" class="stamp-result">
        <el-descriptions-item label="盖章 ID">{{ stampResult.stampId }}</el-descriptions-item>
        <el-descriptions-item label="下载路径">{{ stampResult.downloadPath }}</el-descriptions-item>
        <el-descriptions-item label="下载链接" :span="2">
          <a :href="downloadUrl(stampResult.stampId)" target="_blank" rel="noopener">{{ downloadUrl(stampResult.stampId) }}</a>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="stampVisible = false">关闭</el-button>
        <el-button type="primary" :loading="stamping" @click="submitStamp">执行盖章</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.file-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.file-name {
  color: #909399;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.download-link {
  margin-left: 8px;
  font-size: 12px;
  color: #409eff;
  text-decoration: none;
}
.stamp-result {
  margin-top: 8px;
}
</style>
