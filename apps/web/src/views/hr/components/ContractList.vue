<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
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
  ContractApprovalStatus,
  ContractVO,
  EmployeeVO,
  HrContractStatus,
  HrContractType
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { previewBlob, saveBlob } from '@/utils/file'
import { hrContractStatusLabels, hrContractTypeLabels, toOptions } from '@/utils/labels'
import BpmInstanceDrawer from '@/views/bpm/components/BpmInstanceDrawer.vue'

defineOptions({ name: 'ContractList' })

/** 到期预警天数 */
const EXPIRING_DAYS = 30

const statusOptions = toOptions(hrContractStatusLabels)
const typeOptions = toOptions(hrContractTypeLabels)

const query = reactive<{ status: HrContractStatus | '' }>({ status: '' })
const { page, size, total, resetPage } = usePage()
const list = ref<ContractVO[]>([])
const loading = ref(false)
const expiringCount = ref(0)

const employees = ref<EmployeeVO[]>([])
const employeeNameMap = computed(
  () => new Map(employees.value.map((item) => [item.id, item.name]))
)

function employeeName(id: string): string {
  return employeeNameMap.value.get(id) ?? id
}

function statusTagType(status: HrContractStatus): 'success' | 'info' | 'danger' {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'EXPIRED':
      return 'danger'
    default:
      return 'info'
  }
}

/** 合同审批状态 -> 文案与标签颜色（NONE 未提交在表格中以占位符展示）。 */
const contractApprovalMeta: Record<ContractApprovalStatus, { label: string; type: 'success' | 'info' | 'warning' | 'danger' }> = {
  NONE: { label: '-', type: 'info' },
  PENDING: { label: '审批中', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

/** 未提交或已驳回的合同允许再次提交审批；审批中/已通过不允许重复提交。 */
function canSubmitApproval(row: ContractVO): boolean {
  return !row.approvalStatus || row.approvalStatus === 'NONE' || row.approvalStatus === 'REJECTED'
}

async function loadEmployees(): Promise<void> {
  try {
    const res = await ceamsApi.hr.employeePage({ page: 1, size: 100 })
    employees.value = res.records
  } catch {
    // 员工下拉加载失败不阻断合同列表
  }
}

async function loadExpiring(): Promise<void> {
  try {
    expiringCount.value = (await ceamsApi.hr.contractExpiring(EXPIRING_DAYS)).length
  } catch {
    // 预警数量加载失败不阻断合同列表
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.hr.contractPage({
      page: page.value,
      size: size.value,
      status: query.status || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载合同台账失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

// ---------------- 合同审批 ----------------
const drawerVisible = ref(false)
const drawerInstanceId = ref<string | null>(null)

/** 提交合同审批：二次确认后调用后端发起 BPM，成功后刷新列表同步审批状态。 */
async function submitApproval(row: ContractVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定提交合同「${row.contractNo}」审批吗？提交后可到待办中心跟踪流程。`,
      '提交审批',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await ceamsApi.hr.contractSubmitApproval(row.id)
    ElMessage.success('已提交审批')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '提交审批失败'))
  }
}

function openInstance(instanceId?: string | null): void {
  if (!instanceId) return
  drawerInstanceId.value = instanceId
  drawerVisible.value = true
}

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  employeeId: '',
  contractNo: '',
  contractType: 'FIXED' as HrContractType,
  startDate: '',
  endDate: '',
  signDate: '',
  renewalRemindDate: '',
  status: 'ACTIVE' as HrContractStatus,
  filePath: '',
  fileName: '',
  remark: ''
})

const rules: FormRules<typeof form> = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  contractType: [{ required: true, message: '请选择合同类型', trigger: 'change' }]
}

function resetForm(): void {
  Object.assign(form, {
    employeeId: '',
    contractNo: '',
    contractType: 'FIXED',
    startDate: '',
    endDate: '',
    signDate: '',
    renewalRemindDate: '',
    status: 'ACTIVE',
    filePath: '',
    fileName: '',
    remark: ''
  })
}

function openCreate(): void {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ContractVO): void {
  editingId.value = row.id
  resetForm()
  form.employeeId = row.employeeId
  form.contractNo = row.contractNo
  form.contractType = row.contractType
  form.startDate = row.startDate ?? ''
  form.endDate = row.endDate ?? ''
  form.signDate = row.signDate ?? ''
  form.renewalRemindDate = row.renewalRemindDate ?? ''
  form.status = row.status
  form.filePath = row.filePath ?? ''
  form.fileName = row.fileName ?? ''
  form.remark = row.remark ?? ''
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.hr.contractUpdate(editingId.value, {
        contractNo: form.contractNo,
        contractType: form.contractType,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        signDate: form.signDate || undefined,
        renewalRemindDate: form.renewalRemindDate || undefined,
        status: form.status,
        filePath: form.filePath || undefined,
        fileName: form.fileName || undefined,
        remark: form.remark || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.hr.contractCreate({
        employeeId: form.employeeId,
        contractNo: form.contractNo,
        contractType: form.contractType,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        signDate: form.signDate || undefined,
        renewalRemindDate: form.renewalRemindDate || undefined,
        status: form.status,
        filePath: form.filePath || undefined,
        fileName: form.fileName || undefined,
        remark: form.remark || undefined
      })
      ElMessage.success('创建成功')
    }
    // 附件已随业务记录保存，关闭弹窗时不再补偿删除
    pendingFilePath.value = ''
    dialogVisible.value = false
    await Promise.all([load(), loadExpiring()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存合同失败'))
  }
}

onMounted(() => {
  void loadEmployees()
  void loadExpiring()
  void load()
})

// ---------- 合同原件上传 / 预览 ----------

/** 本次弹窗内新上传、尚未随业务记录保存的文件存储名（用于取消时补偿删除） */
const pendingFilePath = ref('')

async function onContractFileChange(uploadFile: UploadFile): Promise<void> {
  const raw = uploadFile.raw
  if (!raw) return
  if (raw.size > 20 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 20MB')
    return
  }
  try {
    const uploaded = await ceamsApi.file.upload(raw)
    // 重新上传时先删掉上一次刚上传但尚未保存的文件
    if (pendingFilePath.value && pendingFilePath.value !== uploaded.name) {
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
    console.warn('补偿删除文件失败', name, err)
  }
}

/** 弹窗关闭（取消、右上角关闭、ESC）时补偿删除未保存文件。 */
function onDialogClose(): void {
  void removePendingFile()
}

/** 移除附件：本次新上传的文件需删服务器文件，已保存的附件仅清空表单引用。 */
async function removeContractFile(): Promise<void> {
  if (form.filePath && form.filePath === pendingFilePath.value) {
    await removePendingFile()
  }
  form.filePath = ''
  form.fileName = ''
}

async function onPreviewFile(row: ContractVO): Promise<void> {
  if (!row.filePath) return
  try {
    const blob = await fetchBlob(`/api/v1/files/${encodeURIComponent(row.filePath)}`)
    previewBlob(blob)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '合同原件加载失败'))
  }
}

async function onDownloadFile(row: ContractVO): Promise<void> {
  if (!row.filePath) return
  try {
    const blob = await fetchBlob(`/api/v1/files/${encodeURIComponent(row.filePath)}`)
    saveBlob(blob, row.fileName || 'contract')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '合同原件加载失败'))
  }
}
</script>

<template>
  <div class="list-page">
    <el-alert
      v-if="expiringCount > 0"
      :title="`近 ${EXPIRING_DAYS} 天内有 ${expiringCount} 份劳动合同即将到期，请及时续签`"
      type="warning"
      show-icon
      :closable="false"
      class="expiring-alert"
    />

    <!-- 筛选区：白色圆角卡片，控件 12px 间距，操作按钮靠右 -->
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="合同状态" clearable style="width: 160px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部放置标题、条数与主操作 -->
    <div class="data-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">合同台账</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增合同</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="contractNo" label="合同编号" width="160" />
        <el-table-column label="员工" width="120">
          <template #default="{ row }">{{ employeeName((row as ContractVO).employeeId) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            {{ hrContractTypeLabels[(row as ContractVO).contractType] }}
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="signDate" label="签订日期" width="120" />
        <el-table-column prop="renewalRemindDate" label="续签提醒" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType((row as ContractVO).status)">
              {{ hrContractStatusLabels[(row as ContractVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" width="100">
          <template #default="{ row }">
            <el-tag
              v-if="(row as ContractVO).approvalStatus && (row as ContractVO).approvalStatus !== 'NONE'"
              size="small"
              :type="contractApprovalMeta[(row as ContractVO).approvalStatus!].type"
            >
              {{ contractApprovalMeta[(row as ContractVO).approvalStatus!].label }}
            </el-tag>
            <span v-else class="empty-file">-</span>
          </template>
        </el-table-column>
        <el-table-column label="合同原件" width="120">
          <template #default="{ row }">
            <template v-if="(row as ContractVO).filePath">
              <el-button link type="primary" size="small" @click="onPreviewFile(row as ContractVO)">
                查看
              </el-button>
              <el-button link size="small" @click="onDownloadFile(row as ContractVO)">下载</el-button>
            </template>
            <span v-else class="empty-file">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row as ContractVO)">编辑</el-button>
            <el-button
              v-if="canSubmitApproval(row as ContractVO)"
              link
              type="success"
              size="small"
              @click="submitApproval(row as ContractVO)"
            >
              提交审批
            </el-button>
            <el-button
              v-if="(row as ContractVO).bpmInstanceId"
              link
              type="primary"
              size="small"
              @click="openInstance((row as ContractVO).bpmInstanceId)"
            >
              查看流程
            </el-button>
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
      :title="editingId ? '编辑合同' : '新增合同'"
      width="600px"
      @closed="onDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="员工" prop="employeeId">
          <el-select
            v-model="form.employeeId"
            placeholder="请选择员工"
            filterable
            :disabled="!!editingId"
            style="width: 100%"
          >
            <el-option
              v-for="item in employees"
              :key="item.id"
              :label="`${item.name}（${item.employeeNo}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="合同编号" prop="contractNo">
          <el-input v-model="form.contractNo" placeholder="请输入合同编号" />
        </el-form-item>
        <el-form-item label="合同类型" prop="contractType">
          <el-select v-model="form.contractType" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="签订日期">
          <el-date-picker
            v-model="form.signDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="续签提醒">
          <el-date-picker
            v-model="form.renewalRemindDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="合同状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同原件">
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
              <el-button
                v-if="form.filePath"
                link
                type="danger"
                size="small"
                @click="removeContractFile"
              >
                移除
              </el-button>
            </div>
            <div class="form-tip">支持 PDF/JPG/PNG/WEBP，单文件不超过 20MB</div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <BpmInstanceDrawer v-model="drawerVisible" :instance-id="drawerInstanceId" />
  </div>
</template>

<style scoped>
/* 到期预警提示与下方内容保持 16px 间距 */
.expiring-alert {
  margin-bottom: var(--ceams-space-4, 16px);
}

/* 筛选卡片：白色圆角、16px 20px 内边距，操作按钮靠右 */
.filter-bar {
  padding: 16px 20px;
}

.filter-actions {
  display: flex;
  gap: var(--ceams-space-2, 8px);
  margin-left: auto;
}

/* 表格卡片：20px 内边距 */
.data-card {
  padding: 20px;
}

/* 卡片头部：左侧标题与条数，右侧主操作 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3, 12px);
  margin-bottom: var(--ceams-space-4, 16px);
}

.card-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--ceams-space-2, 8px);
}

.card-title {
  font-size: var(--ceams-font-size-md, 16px);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-sub {
  font-size: var(--ceams-font-size-xs, 12px);
  color: var(--ceams-text-secondary);
}

.card-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--ceams-space-2, 8px);
}

/* 分页右对齐，距表格 16px */
.pagination {
  margin-top: var(--ceams-space-4, 16px);
  justify-content: flex-end;
}

/* 空态居中灰色，上下 40px 留白 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}

/* 附件字段 */
.file-field {
  width: 100%;
}

.file-field__row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ceams-space-2, 8px);
}

.file-name {
  color: var(--ceams-text-primary);
  word-break: break-all;
}

.form-tip {
  margin-top: var(--ceams-space-1, 4px);
  font-size: var(--ceams-font-size-xs, 12px);
  color: var(--ceams-text-placeholder);
}

.empty-file {
  color: var(--ceams-text-secondary);
}

/* 弹窗：标题加粗，内容 20px 24px，底部操作条靠右并带顶部细边框 */
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
