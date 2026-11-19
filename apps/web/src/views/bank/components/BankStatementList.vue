<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
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
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type {
  BankMatchCandidatesVO,
  BankStatementStatus,
  BankStatementVO,
  MatchCandidateVO,
  StatementImportResultVO,
  TransactionType
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { formatDate } from '@ceams/utils'
import { apiErrorMessage } from '@/utils/error'
import { bankStatementStatusLabels, toOptions, transactionTypeLabels } from '@/utils/labels'

defineOptions({ name: 'BankStatementList' })

const typeOptions = toOptions(transactionTypeLabels)
const statusOptions = toOptions(bankStatementStatusLabels)
/** 匹配候选来源 -> 中文标签 */
const matchSourceLabels: Record<MatchCandidateVO['source'], string> = {
  PAYMENT_TRANSACTION: '付款交易',
  GENERAL_LEDGER: '总账'
}

const query = reactive<{
  accountId?: string
  status?: BankStatementStatus
  type?: TransactionType
}>({})
const { page, size, total, resetPage } = usePage()
const list = ref<BankStatementVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const importVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<BankStatementVO | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  accountId: '',
  transactionDate: '',
  amount: '',
  type: 'INCOME' as TransactionType,
  description: '',
  counterparty: '',
  bankReference: '',
  balance: ''
})

const rules: FormRules<typeof form> = {
  accountId: [{ required: true, message: '请输入银行账户 ID', trigger: 'blur' }],
  transactionDate: [{ required: true, message: '请选择交易日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  type: [{ required: true, message: '请选择收支类型', trigger: 'change' }]
}

/** 对账状态标签视觉映射：仅决定颜色，状态值与文案保持原样。 */
function statusTagType(status: BankStatementStatus): 'success' | 'warning' | 'info' {
  if (status === 'UNRECONCILED') return 'warning'
  return 'success'
}

/** 金额着色：收入绿色、支出红色，不改变原值与正负号。 */
function amountClass(type: TransactionType): string {
  return type === 'INCOME' ? 'amount-in' : 'amount-out'
}

// ---------- 匹配候选建议 ----------
const matchVisible = ref(false)
const matchLoading = ref(false)
const matchResult = ref<BankMatchCandidatesVO | null>(null)
const matchError = ref('')

async function openMatchCandidates(row: BankStatementVO): Promise<void> {
  matchResult.value = null
  matchError.value = ''
  matchVisible.value = true
  matchLoading.value = true
  try {
    matchResult.value = await ceamsApi.bank.statementMatchCandidates(row.id)
  } catch (err) {
    matchError.value = apiErrorMessage(err, '匹配建议加载失败')
    ElMessage.error(matchError.value)
  } finally {
    matchLoading.value = false
  }
}

// ---------- CSV 导入 ----------
const importAccountId = ref('')
const importFile = ref<File | null>(null)
const importResult = ref<StatementImportResultVO | null>(null)
const importing = ref(false)
const fileInputRef = ref<HTMLInputElement>()

function triggerFileSelect(): void {
  fileInputRef.value?.click()
}

function onFileChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  importFile.value = file ?? null
  input.value = ''
}

async function doImport(): Promise<void> {
  const accountId = importAccountId.value.trim()
  if (!accountId) {
    ElMessage.warning('请输入导入账户 ID')
    return
  }
  if (!importFile.value) {
    ElMessage.warning('请选择 CSV 文件')
    return
  }
  importing.value = true
  try {
    const res = await ceamsApi.bank.statementImport(accountId, importFile.value, importFile.value.name)
    importResult.value = res
    ElMessage.success(`导入完成：成功 ${res.success}，跳过 ${res.skipped}，失败 ${res.failed}`)
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '导入失败'))
  } finally {
    importing.value = false
  }
}

function openImport(): void {
  importAccountId.value = ''
  importFile.value = null
  importResult.value = null
  importVisible.value = true
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.bank.statementPage({
      page: page.value,
      size: size.value,
      accountId: query.accountId,
      status: query.status,
      type: query.type
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载银行流水失败'))
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
    accountId: '',
    transactionDate: '',
    amount: '',
    type: 'INCOME',
    description: '',
    counterparty: '',
    bankReference: '',
    balance: ''
  })
  dialogVisible.value = true
}

function openEdit(row: BankStatementVO): void {
  editingId.value = row.id
  form.accountId = row.accountId
  form.transactionDate = row.transactionDate
  form.amount = row.amount
  form.type = row.type
  form.description = row.description ?? ''
  form.counterparty = row.counterparty ?? ''
  form.bankReference = row.bankReference ?? ''
  form.balance = row.balance ?? ''
  dialogVisible.value = true
}

function openDetail(row: BankStatementVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.bank.statementUpdate(editingId.value, {
        accountId: form.accountId,
        transactionDate: form.transactionDate,
        amount: form.amount,
        type: form.type,
        description: form.description || null,
        counterparty: form.counterparty || null,
        bankReference: form.bankReference || null,
        balance: form.balance || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.bank.statementCreate({
        accountId: form.accountId,
        transactionDate: form.transactionDate,
        amount: form.amount,
        type: form.type,
        description: form.description || null,
        counterparty: form.counterparty || null,
        bankReference: form.bankReference || null,
        balance: form.balance || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: BankStatementVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该银行流水吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.bank.statementRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(load)
</script>

<template>
  <div class="list-page">
    <div class="filter-bar">
      <el-input v-model="query.accountId" placeholder="银行账户 ID" clearable style="width: 200px" />
      <el-select v-model="query.status" placeholder="对账状态" clearable style="width: 140px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="query.type" placeholder="收支类型" clearable style="width: 140px">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="openCreate">新增流水</el-button>
        <el-button @click="openImport">导入 CSV</el-button>
      </div>
    </div>

    <div class="data-card">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="accountId" label="银行账户 ID" width="180" show-overflow-tooltip />
        <el-table-column prop="transactionDate" label="交易日期" width="120" />
        <el-table-column label="金额" width="130" align="right" class-name="amount-col">
          <template #default="{ row }">
            <span :class="amountClass((row as BankStatementVO).type)">{{ (row as BankStatementVO).amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收支类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="(row as BankStatementVO).type === 'INCOME' ? 'success' : 'danger'">
              {{ transactionTypeLabels[(row as BankStatementVO).type] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="对账状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType((row as BankStatementVO).status)">
              {{ bankStatementStatusLabels[(row as BankStatementVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="counterparty" label="对方" width="140" show-overflow-tooltip />
        <el-table-column prop="bankReference" label="银行流水号" width="160" show-overflow-tooltip />
        <el-table-column prop="balance" label="余额" width="130" align="right" class-name="amount-col" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as BankStatementVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openMatchCandidates(row as BankStatementVO)">匹配建议</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as BankStatementVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as BankStatementVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑银行流水' : '新增银行流水'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="银行账户 ID" prop="accountId">
          <el-input v-model="form.accountId" placeholder="请输入银行账户 ID" />
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
        <el-form-item label="对方">
          <el-input v-model="form.counterparty" placeholder="对方（可选）" />
        </el-form-item>
        <el-form-item label="银行流水号">
          <el-input v-model="form.bankReference" placeholder="银行流水号（可选）" />
        </el-form-item>
        <el-form-item label="余额">
          <el-input v-model="form.balance" placeholder="余额（可选）" />
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

    <el-dialog v-model="detailVisible" title="银行流水详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="银行账户 ID">{{ detail.accountId }}</el-descriptions-item>
        <el-descriptions-item label="交易日期">{{ detail.transactionDate }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="收支类型">{{ transactionTypeLabels[detail.type] }}</el-descriptions-item>
        <el-descriptions-item label="对账状态">{{ bankStatementStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="余额">{{ detail.balance ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="对方">{{ detail.counterparty ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="银行流水号">{{ detail.bankReference ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="matchVisible" title="匹配建议" width="900px">
      <div v-loading="matchLoading" class="match-body">
        <el-alert v-if="matchError" type="warning" :closable="false" show-icon :title="matchError" />
        <template v-if="matchResult">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="交易日期">{{ formatDate(matchResult.transactionDate) }}</el-descriptions-item>
            <el-descriptions-item label="金额">{{ matchResult.amount }}</el-descriptions-item>
            <el-descriptions-item label="收支方向">{{ transactionTypeLabels[matchResult.type] }}</el-descriptions-item>
            <el-descriptions-item label="对方户名">{{ matchResult.counterparty ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="摘要" :span="2">{{ matchResult.description ?? '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-alert class="match-disclaimer" type="info" :closable="false" show-icon :title="matchResult.disclaimer" />
          <div v-if="matchResult.truncated" class="match-truncated">
            仅展示前 {{ matchResult.candidates.length }} 条
          </div>
          <el-empty
            v-if="matchResult.candidates.length === 0"
            description="未找到候选，建议人工核对或补录"
            :image-size="60"
          />
          <el-table v-else :data="matchResult.candidates" border size="small">
            <el-table-column label="来源" width="100">
              <template #default="{ row }">{{ matchSourceLabels[(row as MatchCandidateVO).source] }}</template>
            </el-table-column>
            <el-table-column label="日期" width="110">
              <template #default="{ row }">{{ formatDate((row as MatchCandidateVO).transactionDate) }}</template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="110" align="right" />
            <el-table-column prop="amountDiff" label="金额差异" width="110" align="right" />
            <el-table-column prop="dateDiffDays" label="日期差（天）" width="110" align="right" />
            <el-table-column prop="score" label="匹配分" width="90" align="right" />
            <el-table-column label="理由" min-width="260">
              <template #default="{ row }">
                <el-tag
                  v-for="(reason, index) in (row as MatchCandidateVO).reasons"
                  :key="index"
                  size="small"
                  effect="plain"
                  class="match-reason"
                >
                  {{ reason }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
      <template #footer>
        <el-button @click="matchVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="CSV 导入银行流水" width="560px">
      <el-form label-width="110px">
        <el-form-item label="导入账户 ID" required>
          <el-input v-model="importAccountId" placeholder="请输入银行账户 ID" />
        </el-form-item>
        <el-form-item label="CSV 文件" required>
          <div class="file-row">
            <el-button @click="triggerFileSelect">选择文件</el-button>
            <span class="file-name">{{ importFile ? importFile.name : '未选择文件' }}</span>
            <input ref="fileInputRef" type="file" accept=".csv" style="display: none" @change="onFileChange" />
          </div>
        </el-form-item>
      </el-form>
      <div v-if="importResult" class="import-result">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="总数">{{ importResult.total }}</el-descriptions-item>
          <el-descriptions-item label="成功">{{ importResult.success }}</el-descriptions-item>
          <el-descriptions-item label="跳过">{{ importResult.skipped }}</el-descriptions-item>
          <el-descriptions-item label="失败">{{ importResult.failed }}</el-descriptions-item>
        </el-descriptions>
        <ul v-if="importResult.errors && importResult.errors.length" class="error-list">
          <li v-for="(item, index) in importResult.errors" :key="index">第 {{ item.row }} 行：{{ item.reason }}</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="doImport">导入</el-button>
      </template>
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
.file-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.file-name {
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-sm, 13px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.import-result {
  margin-top: 8px;
}
.error-list {
  margin: 8px 0 0;
  padding-left: 20px;
  color: var(--ceams-color-danger);
  font-size: var(--ceams-font-size-xs, 12px);
}
.match-body {
  min-height: 120px;
}
.match-disclaimer {
  margin-top: 12px;
}
.match-truncated {
  margin-top: 8px;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
}
.match-reason {
  margin: 2px 4px 2px 0;
}
</style>
