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
  ContractVO,
  EmployeeVO,
  HrContractStatus,
  HrContractType
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { previewBlob, saveBlob } from '@/utils/file'
import { hrContractStatusLabels, hrContractTypeLabels, toOptions } from '@/utils/labels'

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
  <div>
    <el-alert
      v-if="expiringCount > 0"
      :title="`近 ${EXPIRING_DAYS} 天内有 ${expiringCount} 份劳动合同即将到期，请及时续签`"
      type="warning"
      show-icon
      :closable="false"
      class="expiring-alert"
    />

    <div class="toolbar">
      <el-select v-model="query.status" placeholder="合同状态" clearable style="width: 160px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增合同</el-button>
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
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row as ContractVO)">编辑</el-button>
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
  </div>
</template>

<style scoped>
.expiring-alert {
  margin-bottom: 12px;
}
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
}
.file-name {
  color: #303133;
  word-break: break-all;
}
.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.empty-file {
  color: #909399;
}
</style>
