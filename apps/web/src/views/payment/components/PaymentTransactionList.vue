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
import type { AccountType, PaymentTransactionVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { accountTypeLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'PaymentTransactionList' })

const typeOptions = toOptions(accountTypeLabels)

const query = reactive<{ accountId?: string; type?: AccountType }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<PaymentTransactionVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const executeVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<PaymentTransactionVO | null>(null)
const formRef = ref<FormInstance>()
const executeFormRef = ref<FormInstance>()

const form = reactive({
  accountId: '',
  receiptId: '',
  projectId: '',
  amount: '',
  type: 'INCOME' as AccountType,
  transactionDate: '',
  description: ''
})

const executeForm = reactive({
  accountId: '',
  receiptId: '',
  projectId: '',
  amount: '',
  type: 'INCOME' as AccountType,
  transactionDate: '',
  description: ''
})

const rules: FormRules<typeof form> = {
  accountId: [{ required: true, message: '请输入付款账户 ID', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  type: [{ required: true, message: '请选择收支类型', trigger: 'change' }],
  transactionDate: [{ required: true, message: '请选择交易日期', trigger: 'change' }]
}

const executeRules: FormRules<typeof executeForm> = {
  accountId: [{ required: true, message: '请输入付款账户 ID', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  type: [{ required: true, message: '请选择收支类型', trigger: 'change' }],
  transactionDate: [{ required: true, message: '请选择交易日期', trigger: 'change' }]
}

/** 金额着色：收入绿色、支出红色，不改变原值与正负号。 */
function amountClass(type: AccountType): string {
  return type === 'INCOME' ? 'amount-in' : 'amount-out'
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.payment.transactionPage({
      page: page.value,
      size: size.value,
      accountId: query.accountId,
      type: query.type
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载付款交易失败'))
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
    receiptId: '',
    projectId: '',
    amount: '',
    type: 'INCOME',
    transactionDate: '',
    description: ''
  })
  dialogVisible.value = true
}

function openEdit(row: PaymentTransactionVO): void {
  editingId.value = row.id
  form.accountId = row.accountId
  form.receiptId = row.receiptId ?? ''
  form.projectId = row.projectId ?? ''
  form.amount = row.amount
  form.type = row.type
  form.transactionDate = row.transactionDate
  form.description = row.description ?? ''
  dialogVisible.value = true
}

function openDetail(row: PaymentTransactionVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.payment.transactionUpdate(editingId.value, {
        accountId: form.accountId,
        receiptId: form.receiptId || null,
        projectId: form.projectId || null,
        amount: form.amount,
        type: form.type,
        transactionDate: form.transactionDate,
        description: form.description || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.payment.transactionCreate({
        accountId: form.accountId,
        receiptId: form.receiptId || null,
        projectId: form.projectId || null,
        amount: form.amount,
        type: form.type,
        transactionDate: form.transactionDate,
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

async function onRemove(row: PaymentTransactionVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该付款交易吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.payment.transactionRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

function openExecute(row: PaymentTransactionVO): void {
  Object.assign(executeForm, {
    accountId: row.accountId,
    receiptId: row.receiptId ?? '',
    projectId: row.projectId ?? '',
    amount: row.amount,
    type: row.type,
    transactionDate: row.transactionDate,
    description: row.description ?? ''
  })
  executeVisible.value = true
}

async function submitExecute(): Promise<void> {
  if (!executeFormRef.value) return
  const valid = await executeFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ceamsApi.payment.paymentExecute({
      accountId: executeForm.accountId,
      receiptId: executeForm.receiptId || null,
      projectId: executeForm.projectId || null,
      amount: executeForm.amount,
      type: executeForm.type,
      transactionDate: executeForm.transactionDate,
      description: executeForm.description || null
    })
    ElMessage.success('付款执行已提交（HMAC 自动签名）')
    executeVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '付款执行失败'))
  }
}

onMounted(load)
</script>

<template>
  <div class="list-page">
    <div class="filter-bar">
      <el-input v-model="query.accountId" placeholder="付款账户 ID" clearable style="width: 200px" />
      <el-select v-model="query.type" placeholder="收支类型" clearable style="width: 140px">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="openCreate">新增交易</el-button>
      </div>
    </div>

    <div class="data-card">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="accountId" label="付款账户 ID" width="180" show-overflow-tooltip />
        <el-table-column label="金额" width="130" align="right" class-name="amount-col">
          <template #default="{ row }">
            <span :class="amountClass((row as PaymentTransactionVO).type)">{{ (row as PaymentTransactionVO).amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收支类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="(row as PaymentTransactionVO).type === 'INCOME' ? 'success' : 'danger'">
              {{ accountTypeLabels[(row as PaymentTransactionVO).type] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="transactionDate" label="交易日期" width="120" />
        <el-table-column prop="receiptId" label="票据 ID" width="180" show-overflow-tooltip />
        <el-table-column prop="projectId" label="项目 ID" width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as PaymentTransactionVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as PaymentTransactionVO)">编辑</el-button>
            <el-button link type="success" size="small" @click="openExecute(row as PaymentTransactionVO)">付款执行</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as PaymentTransactionVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑付款交易' : '新增付款交易'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="付款账户 ID" prop="accountId">
          <el-input v-model="form.accountId" placeholder="请输入付款账户 ID" />
        </el-form-item>
        <el-form-item label="票据 ID">
          <el-input v-model="form.receiptId" placeholder="票据 ID（可选）" />
        </el-form-item>
        <el-form-item label="项目 ID">
          <el-input v-model="form.projectId" placeholder="项目 ID（可选）" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="form.amount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="收支类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易日期" prop="transactionDate">
          <el-date-picker v-model="form.transactionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
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

    <el-dialog v-model="detailVisible" title="付款交易详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="付款账户 ID">{{ detail.accountId }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="收支类型">{{ accountTypeLabels[detail.type] }}</el-descriptions-item>
        <el-descriptions-item label="交易日期">{{ detail.transactionDate }}</el-descriptions-item>
        <el-descriptions-item label="票据 ID">{{ detail.receiptId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目 ID">{{ detail.projectId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="executeVisible" title="付款执行" width="560px">
      <el-form ref="executeFormRef" :model="executeForm" :rules="executeRules" label-width="110px">
        <el-form-item label="付款账户 ID" prop="accountId">
          <el-input v-model="executeForm.accountId" placeholder="请输入付款账户 ID" />
        </el-form-item>
        <el-form-item label="票据 ID">
          <el-input v-model="executeForm.receiptId" placeholder="票据 ID（可选）" />
        </el-form-item>
        <el-form-item label="项目 ID">
          <el-input v-model="executeForm.projectId" placeholder="项目 ID（可选）" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="executeForm.amount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="收支类型" prop="type">
          <el-select v-model="executeForm.type" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易日期" prop="transactionDate">
          <el-date-picker v-model="executeForm.transactionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="executeForm.description" type="textarea" :rows="2" placeholder="描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExecute">执行付款</el-button>
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
</style>
