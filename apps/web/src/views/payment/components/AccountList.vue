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
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { AccountStatus, AccountType, AccountVO, AllocationStatus } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import {
  accountStatusLabels,
  accountTypeLabels,
  allocationStatusLabels,
  toOptions
} from '@/utils/labels'

defineOptions({ name: 'AccountList' })

const typeOptions = toOptions(accountTypeLabels)
const statusOptions = toOptions(accountStatusLabels)

const query = reactive<{ projectId?: string; type?: AccountType; status?: AccountStatus }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<AccountVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<AccountVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  projectId: '',
  transactionDate: '',
  amount: '',
  type: 'INCOME' as AccountType,
  paymentAccountId: '',
  description: ''
})

const rules: FormRules<typeof form> = {
  transactionDate: [{ required: true, message: '请选择交易日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  type: [{ required: true, message: '请选择收支类型', trigger: 'change' }]
}

/** 下拉选项：id 为雪花 ID 字符串，label 为展示名称。 */
interface SelectOption {
  id: string
  label: string
}

const projectOptions = ref<SelectOption[]>([])
const paymentAccountOptions = ref<SelectOption[]>([])
const projectOptionsLoading = ref(false)
const paymentAccountOptionsLoading = ref(false)
/** 选项加载失败只提示一次，避免重复弹窗。 */
const projectOptionsErrorShown = ref(false)
const paymentAccountOptionsErrorShown = ref(false)

/** 重新加载后把当前已选中的选项补回列表，避免展示退化成 ID。 */
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

/** 加载项目选项：一次性取 100 条，由 el-select 的 filterable 做本地过滤。 */
async function loadProjectOptions(): Promise<void> {
  projectOptionsLoading.value = true
  try {
    const res = await ceamsApi.project.projectPage({ page: 1, size: 100 })
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

/** 加载付款账户选项：一次性取 100 条，由 el-select 的 filterable 做本地过滤。 */
async function loadPaymentAccountOptions(): Promise<void> {
  paymentAccountOptionsLoading.value = true
  try {
    const res = await ceamsApi.payment.paymentAccountPage({ page: 1, size: 100 })
    paymentAccountOptions.value = keepSelected(
      paymentAccountOptions.value,
      res.records.map((item) => ({ id: item.id, label: item.name })),
      [form.paymentAccountId]
    )
  } catch (err) {
    if (!paymentAccountOptionsErrorShown.value) {
      paymentAccountOptionsErrorShown.value = true
      ElMessage.error(apiErrorMessage(err, '付款账户选项加载失败'))
    }
  } finally {
    paymentAccountOptionsLoading.value = false
  }
}

/** 首次加载选项；已有数据时不重复请求。 */
function ensureOptions(): void {
  if (!projectOptions.value.length) void loadProjectOptions()
  if (!paymentAccountOptions.value.length) void loadPaymentAccountOptions()
}

/** 状态标签视觉映射：仅决定颜色，状态值与文案保持原样。 */
function statusTagType(status: AccountStatus): 'success' | 'warning' | 'info' {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'DRAFT') return 'warning'
  return 'info'
}

/** 分摊状态标签视觉映射。 */
function allocationTagType(status: AllocationStatus): 'success' | 'warning' | 'info' {
  if (status === 'ALLOCATED') return 'success'
  if (status === 'PARTIAL') return 'warning'
  return 'info'
}

/** 金额着色：收入绿色、支出红色，不改变原值与正负号。 */
function amountClass(type: AccountType): string {
  return type === 'INCOME' ? 'amount-in' : 'amount-out'
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.payment.accountPage({
      page: page.value,
      size: size.value,
      projectId: query.projectId,
      type: query.type,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载收支流水失败'))
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
    projectId: '',
    transactionDate: '',
    amount: '',
    type: 'INCOME',
    paymentAccountId: '',
    description: ''
  })
  dialogVisible.value = true
  ensureOptions()
}

function openEdit(row: AccountVO): void {
  editingId.value = row.id
  // 行数据带名称时作为兜底选项，保证编辑态下拉能显示名称而不是 ID
  if (row.projectId) {
    projectOptions.value = addFallback(projectOptions.value, {
      id: row.projectId,
      label: row.projectName || row.projectId
    })
  }
  if (row.paymentAccountId) {
    paymentAccountOptions.value = addFallback(paymentAccountOptions.value, {
      id: row.paymentAccountId,
      label: row.paymentAccountName || row.paymentAccountId
    })
  }
  form.projectId = row.projectId ?? ''
  form.transactionDate = row.transactionDate ?? ''
  form.amount = row.amount
  form.type = row.type
  form.paymentAccountId = row.paymentAccountId ?? ''
  form.description = row.description ?? ''
  dialogVisible.value = true
  ensureOptions()
}

function openDetail(row: AccountVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.payment.accountUpdate(editingId.value, {
        projectId: form.projectId || null,
        transactionDate: form.transactionDate,
        amount: form.amount,
        type: form.type,
        paymentAccountId: form.paymentAccountId || null,
        description: form.description || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.payment.accountCreate({
        projectId: form.projectId || null,
        transactionDate: form.transactionDate,
        amount: form.amount,
        type: form.type,
        paymentAccountId: form.paymentAccountId || null,
        description: form.description || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: AccountVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该收支流水记录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.payment.accountRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

async function onConfirm(row: AccountVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定确认该收支流水吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.payment.accountConfirm(row.id)
    ElMessage.success('确认成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '确认失败'))
  }
}

async function onCancel(row: AccountVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定取消该收支流水吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.payment.accountCancel(row.id)
    ElMessage.success('已取消')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '取消失败'))
  }
}

onMounted(() => {
  void load()
  ensureOptions()
})
</script>

<template>
  <div class="list-page">
    <div class="filter-bar">
      <el-select
        v-model="query.projectId"
        placeholder="项目"
        clearable
        filterable
        :loading="projectOptionsLoading"
        style="width: 200px"
      >
        <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
      </el-select>
      <el-select v-model="query.type" placeholder="收支类型" clearable style="width: 140px">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="openCreate">新增流水</el-button>
      </div>
    </div>

    <div class="data-card">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column label="项目" width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ (row as AccountVO).projectName ?? (row as AccountVO).projectId ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="transactionDate" label="交易日期" width="120" />
        <el-table-column label="金额" width="130" align="right" class-name="amount-col">
          <template #default="{ row }">
            <span :class="amountClass((row as AccountVO).type)">{{ (row as AccountVO).amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收支类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="(row as AccountVO).type === 'INCOME' ? 'success' : 'danger'">
              {{ accountTypeLabels[(row as AccountVO).type] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType((row as AccountVO).status)">
              {{ accountStatusLabels[(row as AccountVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分摊状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="allocationTagType((row as AccountVO).allocationStatus)">
              {{ allocationStatusLabels[(row as AccountVO).allocationStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="付款账户" width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ (row as AccountVO).paymentAccountName ?? (row as AccountVO).paymentAccountId ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as AccountVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as AccountVO)">编辑</el-button>
            <el-button
              v-if="(row as AccountVO).status === 'DRAFT'"
              link
              type="success"
              size="small"
              @click="onConfirm(row as AccountVO)"
            >
              确认
            </el-button>
            <el-button
              v-if="(row as AccountVO).status === 'DRAFT' || (row as AccountVO).status === 'CONFIRMED'"
              link
              type="warning"
              size="small"
              @click="onCancel(row as AccountVO)"
            >
              取消
            </el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as AccountVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑收支流水' : '新增收支流水'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="项目">
          <el-select
            v-model="form.projectId"
            clearable
            filterable
            :loading="projectOptionsLoading"
            placeholder="项目（可选）"
            style="width: 100%"
          >
            <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易日期" prop="transactionDate">
          <el-date-picker v-model="form.transactionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="form.amount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="收支类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款账户">
          <el-select
            v-model="form.paymentAccountId"
            clearable
            filterable
            :loading="paymentAccountOptionsLoading"
            placeholder="付款账户（可选）"
            style="width: 100%"
          >
            <el-option v-for="opt in paymentAccountOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
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

    <el-dialog v-model="detailVisible" title="收支流水详情" width="620px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="项目">{{ detail.projectName ?? detail.projectId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易日期">{{ detail.transactionDate }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="收支类型">{{ accountTypeLabels[detail.type] }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ accountStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="分摊状态">{{ allocationStatusLabels[detail.allocationStatus] }}</el-descriptions-item>
        <el-descriptions-item label="付款账户">
          {{ detail.paymentAccountName ?? detail.paymentAccountId ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="票据编号">{{ detail.receiptNumber ?? detail.receiptId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同">{{ detail.contractName ?? detail.contractId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付类型">{{ detail.paymentType ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
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
/* 金额列：右对齐 + 等宽数字，收入绿色、支出红色 */
:deep(.amount-col .cell) {
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}
.amount-in {
  color: var(--ceams-color-success);
}
.amount-out {
  color: var(--ceams-color-danger);
}
.pagination {
  margin-top: 16px;
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
</style>
