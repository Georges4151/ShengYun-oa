<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElEmpty,
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
import BpmFlowPreviewDialog from '@/views/bpm/components/BpmFlowPreviewDialog.vue'
import type {
  PaymentMethod,
  PaymentStatus,
  ReceiptApprovalStatus,
  ReceiptType,
  ReceiptVO,
  VerificationStatus
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { useAuthStore } from '@/stores/auth'
import { apiErrorMessage } from '@/utils/error'
import { previewBlob, saveBlob } from '@/utils/file'
import {
  paymentMethodLabels,
  paymentStatusLabels,
  receiptApprovalStatusLabels,
  receiptTypeLabels,
  toOptions,
  verificationStatusLabels
} from '@/utils/labels'
import BpmInstanceDrawer from '@/views/bpm/components/BpmInstanceDrawer.vue'

defineOptions({ name: 'ReceiptList' })

// ---------------- 状态标签配色（仅视觉映射，不改动状态值） ----------------
type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 核验状态 -> 标签颜色：已核验成功、驳回危险、待核验警告 */
function verificationTagType(status: VerificationStatus): TagType {
  if (status === 'VERIFIED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

/** 付款状态 -> 标签颜色：已付清成功，未付/部分支付警告 */
function paymentTagType(status: PaymentStatus): TagType {
  return status === 'PAID' ? 'success' : 'warning'
}

/** 审批状态 -> 标签颜色：审批中警告、通过成功、驳回危险、草稿/终止中性 */
function approvalTagType(status: ReceiptApprovalStatus): TagType {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

const typeOptions = toOptions(receiptTypeLabels)
const verificationOptions = toOptions(verificationStatusLabels)
const approvalOptions = toOptions(receiptApprovalStatusLabels)
const paymentMethodOptions = toOptions(paymentMethodLabels)

const query = reactive<{
  receiptNumber?: string
  verificationStatus?: VerificationStatus
  approvalStatus?: ReceiptApprovalStatus
}>({})
const { page, size, total, resetPage } = usePage()
const auth = useAuthStore()
/** 当前登录用户：票据仅支持本人提交，用户与报销人始终是本账号。 */
const currentUserName = computed(() => auth.name || auth.username || '')
const list = ref<ReceiptVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<ReceiptVO | null>(null)
const formRef = ref<FormInstance>()
const drawerVisible = ref(false)
const drawerInstanceId = ref<string | null>(null)

const form = reactive({
  receiptNumber: '',
  userId: '',
  reporterName: '',
  projectId: '',
  receiptDate: '',
  amount: '',
  type: 'TRANSPORTATION' as ReceiptType,
  paymentMethod: '' as PaymentMethod | '',
  description: '',
  filePath: '',
  fileName: '',
  ocrData: ''
})

const uploading = ref(false)
const ocrLoading = ref(false)

interface NameOption {
  id: string
  name: string
}

const projectOptions = ref<NameOption[]>([])
const projectLoading = ref(false)

/** 远程搜索项目，用于表单下拉。 */
async function searchProjects(keyword = ''): Promise<void> {
  projectLoading.value = true
  try {
    const res = await ceamsApi.project.projectPage({
      page: 1,
      size: 20,
      keyword: keyword || undefined
    })
    projectOptions.value = res.records.map((item) => ({ id: item.id, name: item.projectName }))
  } catch {
    ElMessage.error('加载项目列表失败')
  } finally {
    projectLoading.value = false
  }
}

/** 编辑时保证已选项目能显示名称。 */
async function ensureSelectedOptions(): Promise<void> {
  if (form.projectId && !projectOptions.value.some((item) => item.id === form.projectId)) {
    let name = form.projectId
    try {
      const project = await ceamsApi.project.projectDetail(form.projectId)
      name = project.projectName
    } catch {
      // 项目不可读时退回显示 ID
    }
    projectOptions.value.unshift({ id: form.projectId, name })
  }
}

const rules: FormRules<typeof form> = {
  receiptNumber: [{ required: true, message: '请输入票据编号', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  type: [{ required: true, message: '请选择票据类型', trigger: 'change' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.expense.receiptPage({
      page: page.value,
      size: size.value,
      receiptNumber: query.receiptNumber,
      verificationStatus: query.verificationStatus,
      approvalStatus: query.approvalStatus
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载票据失败'))
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
  Object.assign(form, {
    receiptNumber: '',
    userId: auth.user?.id ?? '',
    reporterName: currentUserName.value,
    projectId: '',
    receiptDate: '',
    amount: '',
    type: 'TRANSPORTATION',
    paymentMethod: '',
    description: '',
    filePath: '',
    fileName: '',
    ocrData: ''
  })
  dialogVisible.value = true
  void searchProjects()
}

async function openEdit(row: ReceiptVO): Promise<void> {
  editingId.value = row.id
  form.receiptNumber = row.receiptNumber
  form.userId = row.userId ?? ''
  form.reporterName = row.userId && row.userId === auth.user?.id ? currentUserName.value : (row.reporterName ?? '')
  form.projectId = row.projectId ?? ''
  form.receiptDate = normalizeDate(row.receiptDate ?? '')
  form.amount = row.amount
  form.type = row.type
  form.paymentMethod = row.paymentMethod ?? ''
  form.description = row.description ?? ''
  form.filePath = row.filePath ?? ''
  form.fileName = ''
  form.ocrData = row.ocrData ?? ''
  dialogVisible.value = true
  await searchProjects()
  await ensureSelectedOptions()
}

// ---------- 票据附件上传 / 预览 / 下载 ----------
const ACCEPT_EXTENSIONS = /\.(jpe?g|png|webp|pdf)$/i

/** 附件下载名：优先表单记录的原始文件名，其次取 filePath 末段。 */
function fileNameOf(filePath: string, fileName?: string): string {
  if (fileName) return fileName
  const segment = filePath.split('/').pop()
  return segment || 'receipt'
}

/** 上传：关闭 el-upload 自动上传，选中文件后直接调用上传接口并回填 filePath。 */
async function onReceiptFileChange(uploadFile: UploadFile): Promise<void> {
  const raw = uploadFile.raw
  if (!raw) return
  if (!ACCEPT_EXTENSIONS.test(raw.name)) {
    ElMessage.error('仅支持 JPG/JPEG/PNG/WEBP/PDF 格式')
    return
  }
  if (raw.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    return
  }
  uploading.value = true
  try {
    const uploaded = await ceamsApi.file.upload(raw)
    form.filePath = uploaded.name
    form.fileName = raw.name
    ElMessage.success('上传成功')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '上传失败'))
  } finally {
    uploading.value = false
  }
}

function removeReceiptFile(): void {
  form.filePath = ''
  form.fileName = ''
}

async function previewReceiptFile(filePath: string): Promise<void> {
  if (!filePath) return
  try {
    const blob = await fetchBlob(`/api/v1/files/${encodeURIComponent(filePath)}`)
    previewBlob(blob)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '附件加载失败'))
  }
}

async function downloadReceiptFile(filePath: string, fileName?: string): Promise<void> {
  if (!filePath) return
  try {
    const blob = await fetchBlob(`/api/v1/files/${encodeURIComponent(filePath)}`)
    saveBlob(blob, fileNameOf(filePath, fileName))
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '附件下载失败'))
  }
}

// ---------- AI 识别回填 ----------
/** 把识别出的日期规整为 el-date-picker 的 value-format：YYYY-MM-DD。 */
function normalizeDate(value: string): string {
  const matched = value.match(/(\d{4})\D?(\d{1,2})\D?(\d{1,2})/)
  if (!matched) return ''
  const [, year, month, day] = matched
  return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`
}

async function onRecognize(): Promise<void> {
  if (!form.filePath) return
  ocrLoading.value = true
  try {
    const result = await ceamsApi.ai.receiptOcr({ filePath: form.filePath })
    if (result.receiptNumber) form.receiptNumber = result.receiptNumber
    if (result.amount) form.amount = result.amount
    const receiptDate = result.receiptDate ? normalizeDate(result.receiptDate) : ''
    if (receiptDate) form.receiptDate = receiptDate
    if (result.type && result.type in receiptTypeLabels) {
      form.type = result.type as ReceiptType
    }
    if (result.paymentMethod && result.paymentMethod in paymentMethodLabels) {
      form.paymentMethod = result.paymentMethod as PaymentMethod
    }
    if (result.raw) form.ocrData = result.raw
    ElMessage.success('已根据票据图片回填，请核对后提交')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, 'AI 识别失败'))
  } finally {
    ocrLoading.value = false
  }
}

function openDetail(row: ReceiptVO): void {
  detail.value = row
  detailVisible.value = true
}

/** 后端 receiptDate 为 LocalDateTime，日期选择器只产出 YYYY-MM-DD，提交时补零时刻。 */
function toDateTime(value: string): string | null {
  if (!value) return null
  return value.includes('T') ? value : `${value}T00:00:00`
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.expense.receiptUpdate(editingId.value, {
        reporterName: form.reporterName || null,
        projectId: form.projectId || null,
        receiptDate: toDateTime(form.receiptDate),
        amount: form.amount,
        type: form.type,
        paymentMethod: form.paymentMethod || null,
        description: form.description || null,
        filePath: form.filePath || null,
        ocrData: form.ocrData || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.expense.receiptCreate({
        receiptNumber: form.receiptNumber,
        userId: auth.user?.id || form.userId || null,
        reporterName: currentUserName.value || form.reporterName || null,
        projectId: form.projectId || null,
        receiptDate: toDateTime(form.receiptDate),
        amount: form.amount,
        type: form.type,
        paymentMethod: form.paymentMethod || null,
        description: form.description || null,
        filePath: form.filePath || null,
        ocrData: form.ocrData || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

/** 发起前流程预览：按当前票据解析部门审批/财务核验的预计审批人。 */
const previewVisible = ref(false)
const previewReceiptId = ref<string | null>(null)

function openPreview(row: ReceiptVO): void {
  previewReceiptId.value = row.id
  previewVisible.value = true
}

async function onRemove(row: ReceiptVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除票据「${row.receiptNumber}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.expense.receiptRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

/** 提交审批：发起 BPM 报销流程（部门审批 → 财务核验 → 结束）。 */
async function submitReceipt(row: ReceiptVO): Promise<void> {
  try {
    await ElMessageBox.confirm('提交后进入部门审批与财务核验，确定上报吗？', '上报审批', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await ceamsApi.expense.receiptSubmit(row.id)
    ElMessage.success('已上报，请到待办中心跟进审批')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '上报失败'))
  }
}

function openInstance(instanceId?: string | null): void {
  if (!instanceId) return
  drawerInstanceId.value = instanceId
  drawerVisible.value = true
}

onMounted(load)
</script>

<template>
  <div>
    <!-- 筛选卡片：控件 12px 间距，查询按钮靠右 -->
    <div class="data-card filter-card">
      <el-input v-model="query.receiptNumber" placeholder="票据编号" clearable style="width: 200px" />
      <el-select v-model="query.verificationStatus" placeholder="核验状态" clearable style="width: 150px">
        <el-option v-for="opt in verificationOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="query.approvalStatus" placeholder="审批状态" clearable style="width: 150px">
        <el-option v-for="opt in approvalOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部标题与主操作，表格四周留 20px -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">票据列表</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增票据</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="receiptNumber" label="票据编号" min-width="160" />
        <el-table-column prop="reporterName" label="报销人" width="110" />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">{{ (row as ReceiptVO).amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ receiptTypeLabels[(row as ReceiptVO).type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核验状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="verificationTagType((row as ReceiptVO).verificationStatus)">
              {{ verificationStatusLabels[(row as ReceiptVO).verificationStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="付款状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="paymentTagType((row as ReceiptVO).paymentStatus)">
              {{ paymentStatusLabels[(row as ReceiptVO).paymentStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="approvalTagType((row as ReceiptVO).approvalStatus)">
              {{ receiptApprovalStatusLabels[(row as ReceiptVO).approvalStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiptDate" label="票据日期" width="120" />
        <el-table-column label="附件" width="90">
          <template #default="{ row }">
            <el-button
              v-if="(row as ReceiptVO).filePath"
              link
              type="primary"
              size="small"
              @click="previewReceiptFile((row as ReceiptVO).filePath!)"
            >
              查看
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as ReceiptVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as ReceiptVO)">编辑</el-button>
            <el-button
              v-if="(row as ReceiptVO).approvalStatus === 'DRAFT' || (row as ReceiptVO).approvalStatus === 'REJECTED'"
              link
              type="success"
              size="small"
              @click="submitReceipt(row as ReceiptVO)"
            >
              提交审批
            </el-button>
            <el-button
              v-if="(row as ReceiptVO).approvalStatus === 'DRAFT' || (row as ReceiptVO).approvalStatus === 'REJECTED'"
              link
              type="primary"
              size="small"
              @click="openPreview(row as ReceiptVO)"
            >
              流程预览
            </el-button>
            <el-button
              v-if="(row as ReceiptVO).bpmInstanceId"
              link
              type="primary"
              size="small"
              @click="openInstance((row as ReceiptVO).bpmInstanceId)"
            >
              查看流程
            </el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as ReceiptVO)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无票据数据" />
        </template>
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

    <BpmFlowPreviewDialog
      v-model="previewVisible"
      template-code="EXPENSE"
      biz-type="EXPENSE"
      :biz-id="previewReceiptId"
    />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑票据' : '新增票据'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="票据附件">
          <div class="file-field">
            <el-upload
              class="receipt-uploader"
              drag
              :auto-upload="false"
              :show-file-list="false"
              :disabled="uploading"
              accept=".jpg,.jpeg,.png,.webp,.pdf"
              :on-change="onReceiptFileChange"
            >
              <div class="dropzone">
                <div class="dropzone__title">
                  {{ uploading ? '正在上传…' : '将票据拖拽到此处，或点击选择文件' }}
                </div>
                <div class="dropzone__hint">支持 JPG/JPEG/PNG/WEBP/PDF，单文件不超过 20MB</div>
              </div>
            </el-upload>
            <div v-if="form.filePath" class="file-field__row file-field__actions">
              <span class="file-name" :title="form.fileName || form.filePath">{{ form.fileName || form.filePath }}</span>
              <el-button link type="primary" size="small" @click="previewReceiptFile(form.filePath)">查看</el-button>
              <el-button link type="primary" size="small" @click="downloadReceiptFile(form.filePath, form.fileName)">下载</el-button>
              <el-button link type="danger" size="small" @click="removeReceiptFile">移除</el-button>
            </div>
            <div class="file-field__row">
              <el-button
                type="primary"
                size="small"
                :loading="ocrLoading"
                :disabled="!form.filePath || ocrLoading"
                @click="onRecognize"
              >
                AI 识别
              </el-button>
              <span class="form-tip">建议上传原始票据图片以启用 AI 识别</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="票据编号" prop="receiptNumber">
          <el-input v-model="form.receiptNumber" :disabled="!!editingId" placeholder="请输入票据编号" />
        </el-form-item>
        <el-form-item label="报销人">
          <el-input
            v-model="form.reporterName"
            disabled
            placeholder="自动带出当前登录用户姓名"
          />
        </el-form-item>
        <el-form-item label="项目">
          <el-select
            v-model="form.projectId"
            filterable
            remote
            clearable
            :remote-method="searchProjects"
            :loading="projectLoading"
            placeholder="请选择项目（可选）"
            style="width: 100%"
          >
            <el-option
              v-for="item in projectOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="票据日期">
          <el-date-picker v-model="form.receiptDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="form.amount" placeholder="金额（字符串，如 1280.50）" />
        </el-form-item>
        <el-form-item label="票据类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="form.paymentMethod" placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in paymentMethodOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="票据详情" width="640px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="票据编号">{{ detail.receiptNumber }}</el-descriptions-item>
        <el-descriptions-item label="报销人">{{ detail.reporterName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额"><span class="amount">{{ detail.amount }}</span></el-descriptions-item>
        <el-descriptions-item label="已付金额"><span class="amount">{{ detail.paidAmount ?? '-' }}</span></el-descriptions-item>
        <el-descriptions-item label="票据类型">{{ receiptTypeLabels[detail.type] }}</el-descriptions-item>
        <el-descriptions-item label="核验状态">{{ verificationStatusLabels[detail.verificationStatus] }}</el-descriptions-item>
        <el-descriptions-item label="付款状态">{{ paymentStatusLabels[detail.paymentStatus] }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">{{ receiptApprovalStatusLabels[detail.approvalStatus] }}</el-descriptions-item>
        <el-descriptions-item label="票据日期">{{ detail.receiptDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.paymentMethod ? paymentMethodLabels[detail.paymentMethod] : '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目 ID">{{ detail.projectId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.reporterName || detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="流程实例">{{ detail.bpmInstanceId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="票据附件">
          <template v-if="detail.filePath">
            <el-button link type="primary" size="small" @click="previewReceiptFile(detail.filePath!)">
              查看
            </el-button>
            <el-button link type="primary" size="small" @click="downloadReceiptFile(detail.filePath!)">
              下载
            </el-button>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <BpmInstanceDrawer v-model="drawerVisible" :instance-id="drawerInstanceId" />
  </div>
</template>

<style scoped>
/* 筛选卡片：控件 12px 间距，按钮组靠右 */
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
  gap: var(--ceams-space-2);
  align-items: center;
  margin-left: auto;
}

/* 表格卡片：头部标题与操作，表格四周 20px 内边距 */
.table-card {
  padding: 20px;
}

.card-head {
  display: flex;
  flex-wrap: wrap;
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
  gap: var(--ceams-space-2);
  align-items: center;
}

/* 金额：右对齐 + 等宽数字 */
.amount {
  display: inline-block;
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.pagination {
  margin-top: var(--ceams-space-4);
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
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.receipt-uploader {
  width: 100%;
}
.receipt-uploader :deep(.el-upload) {
  width: 100%;
}
.receipt-uploader :deep(.el-upload-dragger) {
  width: 100%;
  padding: 18px 12px;
  border-radius: var(--ceams-radius-md);
}
.dropzone__title {
  font-size: var(--ceams-font-size-sm);
  line-height: 1.6;
  color: var(--ceams-text-secondary);
}
.dropzone__hint {
  margin-top: 4px;
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-placeholder);
}
.file-field__actions {
  margin-top: 8px;
}
.form-tip {
  font-size: var(--ceams-font-size-xs);
  line-height: 1.5;
  color: var(--ceams-text-placeholder);
}

/* 弹窗：标题加粗，内容区 20px 24px，底部操作条靠右并带顶部细边框 */
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
</style>
