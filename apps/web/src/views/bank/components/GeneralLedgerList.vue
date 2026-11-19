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
  ElTableColumn
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { GeneralLedgerVO, TransactionType } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { toOptions, transactionTypeLabels } from '@/utils/labels'

defineOptions({ name: 'GeneralLedgerList' })

const typeOptions = toOptions(transactionTypeLabels)

const query = reactive<{
  accountName?: string
  transactionType?: TransactionType
  status?: string
}>({})
const { page, size, total, resetPage } = usePage()
const list = ref<GeneralLedgerVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<GeneralLedgerVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  accountName: '',
  accountNumber: '',
  bankName: '',
  balance: '',
  accountType: '',
  summary: '',
  transactionTime: '',
  transactionType: '' as TransactionType | '',
  amount: '',
  status: ''
})

const rules: FormRules<typeof form> = {
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }]
}

/** 金额着色：仅按交易类型上色，不改变数值本身；类型缺失时保持默认色。 */
function amountClass(type?: TransactionType | null): string {
  if (type === 'INCOME') return 'amount-in'
  if (type === 'EXPENSE') return 'amount-out'
  return ''
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.bank.ledgerPage({
      page: page.value,
      size: size.value,
      accountName: query.accountName,
      transactionType: query.transactionType,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载总账失败'))
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
    accountName: '',
    accountNumber: '',
    bankName: '',
    balance: '',
    accountType: '',
    summary: '',
    transactionTime: '',
    transactionType: '',
    amount: '',
    status: ''
  })
  dialogVisible.value = true
}

function openEdit(row: GeneralLedgerVO): void {
  editingId.value = row.id
  form.accountName = row.accountName
  form.accountNumber = row.accountNumber ?? ''
  form.bankName = row.bankName ?? ''
  form.balance = row.balance ?? ''
  form.accountType = row.accountType ?? ''
  form.summary = row.summary ?? ''
  form.transactionTime = row.transactionTime ?? ''
  form.transactionType = row.transactionType ?? ''
  form.amount = row.amount ?? ''
  form.status = row.status ?? ''
  dialogVisible.value = true
}

function openDetail(row: GeneralLedgerVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.bank.ledgerUpdate(editingId.value, {
        accountName: form.accountName,
        accountNumber: form.accountNumber || null,
        bankName: form.bankName || null,
        balance: form.balance || null,
        accountType: form.accountType || null,
        summary: form.summary || null,
        transactionTime: form.transactionTime || null,
        transactionType: form.transactionType || undefined,
        amount: form.amount || null,
        status: form.status || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.bank.ledgerCreate({
        accountName: form.accountName,
        accountNumber: form.accountNumber || null,
        bankName: form.bankName || null,
        balance: form.balance || null,
        accountType: form.accountType || null,
        summary: form.summary || null,
        transactionTime: form.transactionTime || null,
        transactionType: form.transactionType || undefined,
        amount: form.amount || null,
        status: form.status || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: GeneralLedgerVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除总账「${row.accountName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.bank.ledgerRemove(row.id)
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
      <el-input v-model="query.accountName" placeholder="账户名称" clearable style="width: 200px" />
      <el-select v-model="query.transactionType" placeholder="交易类型" clearable style="width: 140px">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-input v-model="query.status" placeholder="状态" clearable style="width: 140px" />
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="openCreate">新增总账</el-button>
      </div>
    </div>

    <div class="data-card">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="accountName" label="账户名称" min-width="150" />
        <el-table-column prop="accountNumber" label="账号（需加密·阶段4）" width="170" show-overflow-tooltip />
        <el-table-column prop="bankName" label="开户行" width="130" />
        <el-table-column prop="balance" label="余额" width="130" align="right" class-name="amount-col" />
        <el-table-column prop="accountType" label="账户类型" width="110" />
        <el-table-column prop="summary" label="摘要" min-width="140" show-overflow-tooltip />
        <el-table-column prop="transactionTime" label="交易时间" width="180" />
        <el-table-column label="交易类型" width="100">
          <template #default="{ row }">
            {{ (row as GeneralLedgerVO).transactionType ? transactionTypeLabels[(row as GeneralLedgerVO).transactionType!] : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right" class-name="amount-col">
          <template #default="{ row }">
            <span :class="amountClass((row as GeneralLedgerVO).transactionType)">{{ (row as GeneralLedgerVO).amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as GeneralLedgerVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as GeneralLedgerVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as GeneralLedgerVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑总账' : '新增总账'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="账户名称" prop="accountName">
          <el-input v-model="form.accountName" placeholder="请输入账户名称" />
        </el-form-item>
        <el-form-item label="账号（需加密·阶段4）">
          <el-input v-model="form.accountNumber" placeholder="账号【需加密】（阶段4）" />
        </el-form-item>
        <el-form-item label="开户行">
          <el-input v-model="form.bankName" placeholder="开户行（可选）" />
        </el-form-item>
        <el-form-item label="余额">
          <el-input v-model="form.balance" placeholder="余额（可选）" />
        </el-form-item>
        <el-form-item label="账户类型">
          <el-input v-model="form.accountType" placeholder="账户类型（可选）" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" placeholder="摘要（可选）" />
        </el-form-item>
        <el-form-item label="交易时间">
          <el-date-picker v-model="form.transactionTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="交易类型">
          <el-select v-model="form.transactionType" placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input v-model="form.amount" placeholder="金额（可选）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model="form.status" placeholder="状态（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="总账详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="账户名称">{{ detail.accountName }}</el-descriptions-item>
        <el-descriptions-item label="账号（需加密·阶段4）">{{ detail.accountNumber ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="开户行">{{ detail.bankName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="余额">{{ detail.balance ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="账户类型">{{ detail.accountType ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易类型">{{ detail.transactionType ? transactionTypeLabels[detail.transactionType] : '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.amount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易时间">{{ detail.transactionTime ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">{{ detail.summary ?? '-' }}</el-descriptions-item>
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
