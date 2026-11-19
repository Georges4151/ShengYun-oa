<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
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
  ElTag,
  ElUpload
} from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { ceamsApi, fetchBlob } from '@/services/http'
import type {
  ContractRiskHintVO,
  ContractRiskItemVO,
  ContractRiskLevel,
  ContractStatus,
  SubcontractorContractVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { previewBlob, saveBlob } from '@/utils/file'
import { contractStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'SubcontractorContractList' })

/** ElTag 支持的语义色 */
type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 综合风险等级 -> 标签文案与颜色（HIGH 红 / MEDIUM 橙 / LOW 灰 / NONE 绿） */
const riskLevelMeta: Record<ContractRiskLevel, { label: string; type: TagType }> = {
  HIGH: { label: '高风险', type: 'danger' },
  MEDIUM: { label: '中风险', type: 'warning' },
  LOW: { label: '低风险', type: 'info' },
  NONE: { label: '无风险', type: 'success' }
}

/** 单条风险等级 -> 标签文案与颜色 */
const riskItemLevelMeta: Record<ContractRiskItemVO['level'], { label: string; type: TagType }> = {
  HIGH: { label: '高', type: 'danger' },
  MEDIUM: { label: '中', type: 'warning' },
  LOW: { label: '低', type: 'info' }
}

const statusOptions = toOptions(contractStatusLabels)

/** 状态标签配色映射：仅决定颜色，状态值与文案保持原样。 */
function statusTagType(status: ContractStatus): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'ACTIVE') return 'warning'
  if (status === 'TERMINATED') return 'danger'
  return 'info'
}

/** 下拉选项：id 为雪花 ID 字符串，label 为展示名称。 */
interface SelectOption {
  id: string
  label: string
}

const subcontractorOptions = ref<SelectOption[]>([])
const projectOptions = ref<SelectOption[]>([])
const subcontractorOptionsLoading = ref(false)
const projectOptionsLoading = ref(false)
/** 选项加载失败只提示一次，避免远程搜索反复弹窗。 */
const subcontractorOptionsErrorShown = ref(false)
const projectOptionsErrorShown = ref(false)

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
function addFallback(options: SelectOption[], fallback?: SelectOption | null): SelectOption[] {
  if (!fallback || !fallback.id) return options
  if (options.some((item) => item.id === fallback.id)) return options
  return [fallback, ...options]
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
      [query.subcontractorId, form.subcontractorId]
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

async function loadProjectOptions(keyword = ''): Promise<void> {
  projectOptionsLoading.value = true
  try {
    const res = await ceamsApi.project.projectPage({
      page: 1,
      size: 50,
      keyword: keyword || undefined
    })
    projectOptions.value = keepSelected(
      projectOptions.value,
      res.records.map((item) => ({ id: item.id, label: item.projectName })),
      [query.projectId, form.projectId]
    )
  } catch (err) {
    if (!projectOptionsErrorShown.value) {
      projectOptionsErrorShown.value = true
      ElMessage.error(apiErrorMessage(err, '项目选项加载失败'))
    }
  } finally {
    projectOptionsLoading.value = false
  }
}

function onProjectSearch(keyword: string): void {
  void loadProjectOptions(keyword)
}

const query = reactive<{ subcontractorId?: string; projectId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<SubcontractorContractVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<SubcontractorContractVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<{
  subcontractorId: string
  projectId: string
  contractName: string
  contractNumber: string
  contractAmount: string
  startDate: string
  endDate: string
  status?: ContractStatus
  description: string
  filePath: string
  fileName: string
}>({
  subcontractorId: '',
  projectId: '',
  contractName: '',
  contractNumber: '',
  contractAmount: '',
  startDate: '',
  endDate: '',
  status: undefined,
  description: '',
  filePath: '',
  fileName: ''
})

/** 本次弹窗内新上传、尚未保存到业务记录的文件名，用于取消/失败时补偿删除。 */
const pendingFilePath = ref('')

const rules: FormRules<typeof form> = {
  subcontractorId: [{ required: true, message: '请选择分包商', trigger: 'change' }],
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  contractNumber: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  contractAmount: [{ required: true, message: '请输入合同金额', trigger: 'blur' }]
}

// ---------- 合同要点与风险提示 ----------
const riskVisible = ref(false)
const riskLoading = ref(false)
const riskHint = ref<ContractRiskHintVO | null>(null)
const riskError = ref('')

async function openRiskHints(row: SubcontractorContractVO): Promise<void> {
  riskHint.value = null
  riskError.value = ''
  riskVisible.value = true
  riskLoading.value = true
  try {
    riskHint.value = await ceamsApi.ai.contractRiskHints(row.id)
  } catch (err) {
    riskError.value = apiErrorMessage(err, '风险提示加载失败')
    ElMessage.error(riskError.value)
  } finally {
    riskLoading.value = false
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.contractPage({
      page: page.value,
      size: size.value,
      subcontractorId: query.subcontractorId,
      projectId: query.projectId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载分包合同失败'))
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
    subcontractorId: '',
    projectId: '',
    contractName: '',
    contractNumber: '',
    contractAmount: '',
    startDate: '',
    endDate: '',
    status: undefined,
    description: '',
    filePath: '',
    fileName: ''
  })
  dialogVisible.value = true
}

function openEdit(row: SubcontractorContractVO): void {
  editingId.value = row.id
  pendingFilePath.value = ''
  // 行数据带名称时作为兜底选项，保证编辑态下拉能显示名称而不是 ID
  subcontractorOptions.value = addFallback(subcontractorOptions.value, {
    id: row.subcontractorId,
    label: row.subcontractorName || row.subcontractorId
  })
  if (row.projectId) {
    projectOptions.value = addFallback(projectOptions.value, {
      id: row.projectId,
      label: row.projectName || row.projectId
    })
  }
  form.subcontractorId = row.subcontractorId
  form.projectId = row.projectId ?? ''
  form.contractName = row.contractName
  form.contractNumber = row.contractNumber
  form.contractAmount = row.contractAmount
  form.startDate = row.startDate ?? ''
  form.endDate = row.endDate ?? ''
  form.status = row.status
  form.description = row.description ?? ''
  form.filePath = row.filePath ?? ''
  form.fileName = row.fileName ?? ''
  dialogVisible.value = true
}

/** 合同文件上传：关闭 el-upload 自动上传，选中文件后直接调用上传接口。 */
async function onContractFileChange(uploadFile: UploadFile): Promise<void> {
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
async function removeContractFile(): Promise<void> {
  if (form.filePath && form.filePath === pendingFilePath.value) {
    await removePendingFile()
  }
  form.filePath = ''
  form.fileName = ''
}

// ---------- 附件预览 / 下载 / 导出 ----------
async function onPreviewFile(row: SubcontractorContractVO): Promise<void> {
  if (!row.filePath) return
  try {
    const blob = await fetchBlob(`/api/v1/files/${encodeURIComponent(row.filePath)}`)
    previewBlob(blob)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '附件加载失败'))
  }
}

async function onDownloadFile(row: SubcontractorContractVO): Promise<void> {
  if (!row.filePath) return
  try {
    const blob = await fetchBlob(`/api/v1/files/${encodeURIComponent(row.filePath)}`)
    saveBlob(blob, row.fileName || 'contract')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '附件加载失败'))
  }
}

const exporting = ref(false)

async function onExport(): Promise<void> {
  const params = new URLSearchParams()
  if (query.subcontractorId) params.append('subcontractorId', query.subcontractorId)
  if (query.projectId) params.append('projectId', query.projectId)
  const search = params.toString()
  exporting.value = true
  try {
    const blob = await fetchBlob(`/api/v1/subcontractor-contracts/export${search ? `?${search}` : ''}`)
    saveBlob(blob, '分包合同列表.xlsx')
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '导出失败'))
  } finally {
    exporting.value = false
  }
}

function openDetail(row: SubcontractorContractVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.contractUpdate(editingId.value, {
        projectId: form.projectId || undefined,
        contractName: form.contractName,
        contractAmount: form.contractAmount,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        status: form.status,
        description: form.description || undefined,
        filePath: form.filePath || null,
        fileName: form.fileName || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.contractCreate({
        subcontractorId: form.subcontractorId,
        projectId: form.projectId || null,
        contractName: form.contractName,
        contractNumber: form.contractNumber,
        contractAmount: form.contractAmount,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        status: form.status,
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

async function onRemove(row: SubcontractorContractVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除分包合同「${row.contractName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.contractRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(() => {
  void load()
  void loadSubcontractorOptions()
  void loadProjectOptions()
})
</script>

<template>
  <div class="list-page">
    <!-- 筛选卡片：控件 12px 间距，按钮组靠右 -->
    <div class="data-card filter-card">
      <el-select
        v-model="query.subcontractorId"
        placeholder="分包商（可选）"
        clearable
        filterable
        remote
        remote-show-suffix
        :remote-method="onSubcontractorSearch"
        :loading="subcontractorOptionsLoading"
        style="width: 200px"
      >
        <el-option v-for="opt in subcontractorOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
      </el-select>
      <el-select
        v-model="query.projectId"
        placeholder="项目（可选）"
        clearable
        filterable
        remote
        remote-show-suffix
        :remote-method="onProjectSearch"
        :loading="projectOptionsLoading"
        style="width: 200px"
      >
        <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部标题、条数与主操作 -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">分包合同列表</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增合同</el-button>
          <el-button :loading="exporting" @click="onExport">导出 Excel</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="contractName" label="合同名称" min-width="180" />
        <el-table-column prop="contractNumber" label="合同编号" width="160" />
        <el-table-column label="分包商" width="180">
          <template #default="{ row }">
            {{ (row as SubcontractorContractVO).subcontractorName || (row as SubcontractorContractVO).subcontractorId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="项目" width="180">
          <template #default="{ row }">
            {{ (row as SubcontractorContractVO).projectName || (row as SubcontractorContractVO).projectId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="contractAmount" label="合同金额" width="120" align="right" class-name="amount-col" />
        <el-table-column prop="paidAmount" label="已付金额" width="120" align="right" class-name="amount-col" />
        <el-table-column label="附件" width="160">
          <template #default="{ row }">
            <template v-if="(row as SubcontractorContractVO).filePath">
              <span class="file-name">{{ (row as SubcontractorContractVO).fileName || '查看' }}</span>
              <el-button link type="primary" size="small" @click="onPreviewFile(row as SubcontractorContractVO)">预览</el-button>
              <el-button link type="primary" size="small" @click="onDownloadFile(row as SubcontractorContractVO)">下载</el-button>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.status" size="small" :type="statusTagType((row as SubcontractorContractVO).status!)">
              {{ contractStatusLabels[(row as SubcontractorContractVO).status!] }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as SubcontractorContractVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openRiskHints(row as SubcontractorContractVO)">风险提示</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as SubcontractorContractVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as SubcontractorContractVO)">删除</el-button>
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
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑分包合同' : '新增分包合同'"
      width="600px"
      @close="onDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="分包商" prop="subcontractorId">
          <el-select
            v-model="form.subcontractorId"
            :disabled="!!editingId"
            filterable
            remote
            remote-show-suffix
            :remote-method="onSubcontractorSearch"
            :loading="subcontractorOptionsLoading"
            placeholder="请选择分包商"
            style="width: 100%"
          >
            <el-option v-for="opt in subcontractorOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目">
          <el-select
            v-model="form.projectId"
            :disabled="!!editingId"
            clearable
            filterable
            remote
            remote-show-suffix
            :remote-method="onProjectSearch"
            :loading="projectOptionsLoading"
            placeholder="项目（可选）"
            style="width: 100%"
          >
            <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同名称" prop="contractName">
          <el-input v-model="form.contractName" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="合同编号" prop="contractNumber">
          <el-input v-model="form.contractNumber" :disabled="!!editingId" placeholder="请输入合同编号" />
        </el-form-item>
        <el-form-item label="合同金额" prop="contractAmount">
          <el-input v-model="form.contractAmount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="合同状态">
          <el-select v-model="form.status" clearable placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述（可选）" />
        </el-form-item>
        <el-form-item label="合同文件">
          <div class="file-field">
            <div class="file-field__row">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".pdf,.jpg,.jpeg,.png,.webp"
                :on-change="onContractFileChange"
              >
                <el-button size="small">{{ form.filePath ? '重新上传' : '选择文件' }}</el-button>
              </el-upload>
              <span v-if="form.fileName" class="file-name">{{ form.fileName }}</span>
              <el-button v-if="form.filePath" link type="danger" size="small" @click="removeContractFile">
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

    <el-dialog v-model="detailVisible" title="分包合同详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="合同名称">{{ detail.contractName }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ detail.contractNumber }}</el-descriptions-item>
        <el-descriptions-item label="分包商">{{ detail.subcontractorName || detail.subcontractorId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目">{{ detail.projectName || detail.projectId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同金额">{{ detail.contractAmount }}</el-descriptions-item>
        <el-descriptions-item label="已付金额">{{ detail.paidAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detail.status ? contractStatusLabels[detail.status] : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail.startDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detail.endDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同文件">
          <template v-if="detail.filePath">
            <span class="file-name">{{ detail.fileName || '查看' }}</span>
            <el-button link type="primary" size="small" @click="onPreviewFile(detail)">预览</el-button>
            <el-button link type="primary" size="small" @click="onDownloadFile(detail)">下载</el-button>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="riskVisible" title="合同要点与风险提示" width="760px">
      <div v-loading="riskLoading" class="risk-body">
        <el-alert v-if="riskError" type="warning" :closable="false" show-icon :title="riskError" />
        <template v-if="riskHint">
          <div class="risk-section">
            <div class="risk-section__title">条款要点</div>
            <el-descriptions v-if="riskHint.clausePoints.length" :column="2" border size="small">
              <el-descriptions-item
                v-for="(point, index) in riskHint.clausePoints"
                :key="index"
                :label="point.label"
              >
                {{ point.value }}
              </el-descriptions-item>
            </el-descriptions>
            <div v-else class="risk-empty">暂无可提取的条款要点</div>
          </div>

          <div class="risk-section">
            <div class="risk-section__title">
              风险提示
              <el-tag
                v-if="riskHint.riskLevel !== 'NONE'"
                size="small"
                effect="plain"
                :type="riskLevelMeta[riskHint.riskLevel].type"
              >
                {{ riskLevelMeta[riskHint.riskLevel].label }}
              </el-tag>
            </div>
            <div v-if="riskHint.riskLevel === 'NONE' || riskHint.risks.length === 0" class="risk-empty">
              未发现明显风险
            </div>
            <el-table v-else :data="riskHint.risks" border size="small">
              <el-table-column label="等级" width="80">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain" :type="riskItemLevelMeta[(row as ContractRiskItemVO).level].type">
                    {{ riskItemLevelMeta[(row as ContractRiskItemVO).level].label }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="风险" width="180" show-overflow-tooltip />
              <el-table-column prop="detail" label="说明" min-width="260" />
            </el-table>
          </div>

          <div v-if="riskHint.aiNote" class="risk-section">
            <div class="risk-section__title">AI 说明，请核实</div>
            <div class="risk-note">{{ riskHint.aiNote }}</div>
          </div>

          <el-alert class="risk-disclaimer" type="info" :closable="false" show-icon :title="riskHint.disclaimer" />
        </template>
      </div>
      <template #footer>
        <el-button @click="riskVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 筛选卡片：16px 20px 内边距，控件 12px 间距，按钮组靠右 */
.filter-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ceams-space-3);
  padding: 16px 20px;
  margin-bottom: var(--ceams-space-4);
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2);
  margin-left: auto;
}

/* 表格卡片：20px 内边距，头部放标题、条数与主操作 */
.table-card {
  padding: 20px;
}

.card-head {
  display: flex;
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

.card-sub {
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
}

.card-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2);
}

/* 金额列右对齐并使用等宽数字，避免数字跳动 */
:deep(.amount-col .cell) {
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}

/* 分页右对齐，距表格 16px */
.pagination {
  justify-content: flex-end;
  margin-top: var(--ceams-space-4);
}

/* 空态居中灰色，上下 40px 留白 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}

/* 弹窗：标题加粗、内容区 20px 24px、底部操作条靠右并带顶部细边框 */
:deep(.el-dialog__title) {
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-dialog__footer) {
  padding: 12px 24px 18px;
  border-top: 1px solid var(--ceams-border-divider);
  text-align: right;
}

.risk-body {
  min-height: 120px;
}
.risk-section {
  margin-bottom: 16px;
}
.risk-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-base);
  font-weight: 600;
}
.risk-empty {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs);
}
.risk-note {
  padding: 8px 12px;
  border-radius: var(--ceams-radius-sm);
  background: var(--ceams-bg-elevated);
  color: var(--ceams-text-primary);
  font-size: var(--ceams-font-size-xs);
  line-height: 1.7;
  white-space: pre-wrap;
}
.risk-disclaimer {
  margin-top: 4px;
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
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.form-tip {
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-xs);
  line-height: 1.5;
}
</style>
