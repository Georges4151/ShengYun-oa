<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElUpload
} from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { ceamsApi, fetchBlob } from '@/services/http'
import type { SettlementProofVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { previewBlob, saveBlob } from '@/utils/file'

defineOptions({ name: 'SettlementProofList' })

const query = reactive<{ contractId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<SettlementProofVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<SettlementProofVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  contractId: '',
  subcontractorId: '',
  proofName: '',
  proofNumber: '',
  proofDate: '',
  settlementAmount: '',
  description: '',
  filePath: '',
  fileName: ''
})

/** 本次弹窗内新上传、尚未保存到业务记录的文件名，用于取消/失败时补偿删除。 */
const pendingFilePath = ref('')

const rules: FormRules<typeof form> = {
  contractId: [{ required: true, message: '请选择合同', trigger: 'change' }],
  proofName: [{ required: true, message: '请输入凭证名称', trigger: 'blur' }],
  proofNumber: [{ required: true, message: '请输入凭证编号', trigger: 'blur' }],
  settlementAmount: [{ required: true, message: '请输入结算金额', trigger: 'blur' }]
}

/** 下拉选项：id 为雪花 ID 字符串，label 为展示名称。 */
interface SelectOption {
  id: string
  label: string
}

/** 合同选项额外保留分包商信息，便于选合同时自动带出分包商。 */
interface ContractOption extends SelectOption {
  subcontractorId?: string | null
  subcontractorName?: string | null
}

const contractOptions = ref<ContractOption[]>([])
const subcontractorOptions = ref<SelectOption[]>([])
const contractOptionsLoading = ref(false)
const subcontractorOptionsLoading = ref(false)
/** 选项加载失败只提示一次，避免反复弹窗、也不阻塞列表展示。 */
const contractOptionsErrorShown = ref(false)
const subcontractorOptionsErrorShown = ref(false)

/** 远程搜索后把当前已选中的选项补回列表，避免展示退化成 ID。 */
function keepSelected(
  prev: SelectOption[],
  next: SelectOption[],
  ids: Array<string | null | undefined>
): SelectOption[] {
  for (const id of ids) {
    if (!id || next.some((item) => item.id === id)) continue
    const old = prev.find((item) => item.id === id)
    if (old) next.push(old)
  }
  return next
}

/** 合并兜底选项（行数据里的名称），按 id 去重。 */
function addFallback<T extends SelectOption>(options: T[], fallback?: T | null): T[] {
  if (!fallback || !fallback.id) return options
  if (options.some((item) => item.id === fallback.id)) return options
  return [fallback, ...options]
}

/** 合同接口没有关键字参数：分页循环加载全部合同（每页 100，最多 10 页）。 */
const CONTRACT_OPTION_PAGE_SIZE = 100
const CONTRACT_OPTION_MAX_PAGES = 10

async function loadContractOptions(): Promise<void> {
  contractOptionsLoading.value = true
  const loaded: ContractOption[] = []
  try {
    for (let pageNo = 1; pageNo <= CONTRACT_OPTION_MAX_PAGES; pageNo += 1) {
      const res = await ceamsApi.project.contractPage({
        page: pageNo,
        size: CONTRACT_OPTION_PAGE_SIZE
      })
      for (const item of res.records) {
        loaded.push({
          id: item.id,
          label: `${item.contractName}（${item.contractNumber}）`,
          subcontractorId: item.subcontractorId,
          subcontractorName: item.subcontractorName ?? null
        })
      }
      if (res.records.length < CONTRACT_OPTION_PAGE_SIZE || loaded.length >= res.total) break
    }
    contractOptions.value = keepSelected(contractOptions.value, loaded, [query.contractId, form.contractId]) as ContractOption[]
  } catch (err) {
    // 已加载的部分仍然可用，不阻塞页面
    contractOptions.value = keepSelected(contractOptions.value, loaded, [query.contractId, form.contractId]) as ContractOption[]
    if (!contractOptionsErrorShown.value) {
      contractOptionsErrorShown.value = true
      ElMessage.error(apiErrorMessage(err, '合同选项加载失败'))
    }
  } finally {
    contractOptionsLoading.value = false
  }
}

async function loadSubcontractorOptions(keyword = ''): Promise<void> {
  subcontractorOptionsLoading.value = true
  try {
    const res = await ceamsApi.project.subcontractorPage({
      page: 1,
      size: 50,
      keyword: keyword || undefined
    })
    subcontractorOptions.value = keepSelected(
      subcontractorOptions.value,
      res.records.map((item) => ({ id: item.id, label: item.name })),
      [form.subcontractorId]
    )
  } catch (err) {
    if (!subcontractorOptionsErrorShown.value) {
      subcontractorOptionsErrorShown.value = true
      ElMessage.error(apiErrorMessage(err, '分包商选项加载失败'))
    }
  } finally {
    subcontractorOptionsLoading.value = false
  }
}

function onSubcontractorSearch(keyword: string): void {
  void loadSubcontractorOptions(keyword)
}

/** 新增时选中合同后，若分包商为空则自动带出该合同的分包商（仍可手动修改）。 */
function onContractChange(contractId: string): void {
  if (editingId.value) return
  const contract = contractOptions.value.find((item) => item.id === contractId)
  if (!contract) return
  if (!form.subcontractorId && contract.subcontractorId) {
    form.subcontractorId = contract.subcontractorId
    subcontractorOptions.value = addFallback(subcontractorOptions.value, {
      id: contract.subcontractorId,
      label: contract.subcontractorName || contract.subcontractorId
    })
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.proofPage({
      page: page.value,
      size: size.value,
      contractId: query.contractId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载结算凭证失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

function openCreate(): void {
  editingId.value = null
  pendingFilePath.value = ''
  Object.assign(form, {
    contractId: '',
    subcontractorId: '',
    proofName: '',
    proofNumber: '',
    proofDate: '',
    settlementAmount: '',
    description: '',
    filePath: '',
    fileName: ''
  })
  dialogVisible.value = true
}

function openEdit(row: SettlementProofVO): void {
  editingId.value = row.id
  pendingFilePath.value = ''
  // 行数据带名称时作为兜底选项，保证编辑态下拉能显示名称而不是 ID
  contractOptions.value = addFallback(contractOptions.value, {
    id: row.contractId,
    label: row.contractName || row.contractId,
    subcontractorId: row.subcontractorId ?? null,
    subcontractorName: row.subcontractorName ?? null
  })
  if (row.subcontractorId) {
    subcontractorOptions.value = addFallback(subcontractorOptions.value, {
      id: row.subcontractorId,
      label: row.subcontractorName || row.subcontractorId
    })
  }
  form.contractId = row.contractId
  form.subcontractorId = row.subcontractorId ?? ''
  form.proofName = row.proofName
  form.proofNumber = row.proofNumber
  form.proofDate = row.proofDate ?? ''
  form.settlementAmount = row.settlementAmount
  form.description = row.description ?? ''
  form.filePath = row.filePath ?? ''
  form.fileName = row.fileName ?? ''
  dialogVisible.value = true
}

/** 凭证文件上传：关闭 el-upload 自动上传，选中文件后直接调用上传接口。 */
async function onProofFileChange(uploadFile: UploadFile): Promise<void> {
  const raw = uploadFile.raw
  if (!raw) return
  if (raw.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    return
  }
  try {
    const uploaded = await ceamsApi.file.upload(raw)
    if (pendingFilePath.value && pendingFilePath.value !== uploaded.name) {
      // 替换上传：先删掉上一次刚上传但尚未保存的文件
      await removePendingFile()
    }
    pendingFilePath.value = uploaded.name
    form.filePath = uploaded.name
    form.fileName = raw.name
    ElMessage.success('上传成功')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '上传失败'))
  }
}

/** 删除本次弹窗内已上传但未保存到业务表的文件；失败不阻断主流程。 */
async function removePendingFile(): Promise<void> {
  if (!pendingFilePath.value) return
  const name = pendingFilePath.value
  pendingFilePath.value = ''
  try {
    await ceamsApi.file.remove(name)
  } catch (err) {
    // 补偿删除失败不阻断主流程，后端还有兜底
    console.warn('补偿删除文件失败', name, err)
  }
}

/** 弹窗关闭（取消按钮、右上角关闭、遮罩、ESC）时补偿删除未保存文件。 */
function onDialogClose(): void {
  void removePendingFile()
}

/** 移除附件：本次新上传的文件需删服务器文件，编辑前已保存的附件仅清空表单引用。 */
async function removeProofFile(): Promise<void> {
  if (form.filePath && form.filePath === pendingFilePath.value) {
    await removePendingFile()
  }
  form.filePath = ''
  form.fileName = ''
}

// ---------- 附件预览 / 下载 / 导出 ----------
async function onPreviewFile(row: SettlementProofVO): Promise<void> {
  if (!row.filePath) return
  try {
    const blob = await fetchBlob(`/api/v1/files/${encodeURIComponent(row.filePath)}`)
    previewBlob(blob)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '附件加载失败'))
  }
}

async function onDownloadFile(row: SettlementProofVO): Promise<void> {
  if (!row.filePath) return
  try {
    const blob = await fetchBlob(`/api/v1/files/${encodeURIComponent(row.filePath)}`)
    saveBlob(blob, row.fileName || 'proof')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '附件加载失败'))
  }
}

const exporting = ref(false)

async function onExport(): Promise<void> {
  const params = new URLSearchParams()
  if (query.contractId) params.append('contractId', query.contractId)
  const search = params.toString()
  exporting.value = true
  try {
    const blob = await fetchBlob(`/api/v1/settlement-proofs/export${search ? `?${search}` : ''}`)
    saveBlob(blob, '结算凭证列表.xlsx')
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '导出失败'))
  } finally {
    exporting.value = false
  }
}

function openDetail(row: SettlementProofVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.proofUpdate(editingId.value, {
        proofName: form.proofName,
        proofNumber: form.proofNumber,
        proofDate: form.proofDate || undefined,
        settlementAmount: form.settlementAmount,
        description: form.description || undefined,
        filePath: form.filePath || null,
        fileName: form.fileName || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.proofCreate({
        contractId: form.contractId,
        subcontractorId: form.subcontractorId || null,
        proofName: form.proofName,
        proofNumber: form.proofNumber,
        proofDate: form.proofDate || undefined,
        settlementAmount: form.settlementAmount,
        description: form.description || null,
        filePath: form.filePath || null,
        fileName: form.fileName || null
      })
      ElMessage.success('创建成功')
    }
    pendingFilePath.value = ''
    dialogVisible.value = false
    await load()
  } catch (err) {
    // 保存失败：补偿删除本次上传文件，并清空表单中的失效引用
    await removePendingFile()
    form.filePath = ''
    form.fileName = ''
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: SettlementProofVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除结算凭证「${row.proofName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.proofRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(() => {
  void load()
  void loadContractOptions()
  void loadSubcontractorOptions()
})
</script>

<template>
  <div>
    <div class="toolbar">
      <el-select
        v-model="query.contractId"
        placeholder="合同（可选）"
        clearable
        filterable
        :loading="contractOptionsLoading"
        style="width: 220px"
      >
        <el-option v-for="opt in contractOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增凭证</el-button>
      <el-button :loading="exporting" @click="onExport">导出 Excel</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="proofName" label="凭证名称" min-width="180" />
      <el-table-column prop="proofNumber" label="凭证编号" width="160" />
      <el-table-column label="合同名称" width="180">
        <template #default="{ row }">
          {{ (row as SettlementProofVO).contractName || (row as SettlementProofVO).contractId || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="分包商" width="180">
        <template #default="{ row }">
          {{ (row as SettlementProofVO).subcontractorName || (row as SettlementProofVO).subcontractorId || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="proofDate" label="凭证日期" width="120" />
      <el-table-column prop="settlementAmount" label="结算金额" width="120" />
      <el-table-column label="附件" width="160">
        <template #default="{ row }">
          <template v-if="(row as SettlementProofVO).filePath">
            <span class="file-name">{{ (row as SettlementProofVO).fileName || '查看' }}</span>
            <el-button link type="primary" size="small" @click="onPreviewFile(row as SettlementProofVO)">预览</el-button>
            <el-button link type="primary" size="small" @click="onDownloadFile(row as SettlementProofVO)">下载</el-button>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as SettlementProofVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as SettlementProofVO)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as SettlementProofVO)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑结算凭证' : '新增结算凭证'"
      width="540px"
      @close="onDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="合同" prop="contractId">
          <el-select
            v-model="form.contractId"
            :disabled="!!editingId"
            filterable
            :loading="contractOptionsLoading"
            placeholder="请选择合同"
            style="width: 100%"
            @change="onContractChange"
          >
            <el-option v-for="opt in contractOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分包商">
          <el-select
            v-model="form.subcontractorId"
            :disabled="!!editingId"
            clearable
            filterable
            remote
            remote-show-suffix
            :remote-method="onSubcontractorSearch"
            :loading="subcontractorOptionsLoading"
            placeholder="分包商（可选）"
            style="width: 100%"
          >
            <el-option v-for="opt in subcontractorOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="凭证名称" prop="proofName">
          <el-input v-model="form.proofName" placeholder="请输入凭证名称" />
        </el-form-item>
        <el-form-item label="凭证编号" prop="proofNumber">
          <el-input v-model="form.proofNumber" placeholder="请输入凭证编号" />
        </el-form-item>
        <el-form-item label="凭证日期">
          <el-date-picker v-model="form.proofDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结算金额" prop="settlementAmount">
          <el-input v-model="form.settlementAmount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述（可选）" />
        </el-form-item>
        <el-form-item label="凭证文件">
          <div class="file-field">
            <div class="file-field__row">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".pdf,.jpg,.jpeg,.png,.webp"
                :on-change="onProofFileChange"
              >
                <el-button size="small">{{ form.filePath ? '重新上传' : '选择文件' }}</el-button>
              </el-upload>
              <span v-if="form.fileName" class="file-name">{{ form.fileName }}</span>
              <el-button v-if="form.filePath" link type="danger" size="small" @click="removeProofFile">
                移除
              </el-button>
            </div>
            <div class="form-tip">支持 PDF/JPG/PNG/WEBP，单文件不超过 20MB</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="结算凭证详情" width="540px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="凭证名称">{{ detail.proofName }}</el-descriptions-item>
        <el-descriptions-item label="凭证编号">{{ detail.proofNumber }}</el-descriptions-item>
        <el-descriptions-item label="合同">{{ detail.contractName || detail.contractId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分包商">{{ detail.subcontractorName || detail.subcontractorId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="凭证日期">{{ detail.proofDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="结算金额">{{ detail.settlementAmount }}</el-descriptions-item>
        <el-descriptions-item label="附件">
          <template v-if="detail.filePath">
            <span class="file-name">{{ detail.fileName || '查看' }}</span>
            <el-button link type="primary" size="small" @click="onPreviewFile(detail)">预览</el-button>
            <el-button link type="primary" size="small" @click="onDownloadFile(detail)">下载</el-button>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ detail.description ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
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
.file-field {
  width: 100%;
}
.file-field__row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.file-name {
  max-width: 180px;
  overflow: hidden;
  color: #606266;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}
</style>
