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
import type { AccountStatus, AccountType, AccountVO } from '@ceams/types'
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
}

function openEdit(row: AccountVO): void {
  editingId.value = row.id
  form.projectId = row.projectId ?? ''
  form.transactionDate = row.transactionDate ?? ''
  form.amount = row.amount
  form.type = row.type
  form.paymentAccountId = row.paymentAccountId ?? ''
  form.description = row.description ?? ''
  dialogVisible.value = true
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

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.projectId" placeholder="项目 ID" clearable style="width: 180px" />
      <el-select v-model="query.type" placeholder="收支类型" clearable style="width: 140px">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增流水</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="projectId" label="项目 ID" width="170" show-overflow-tooltip />
      <el-table-column prop="transactionDate" label="交易日期" width="120" />
      <el-table-column prop="amount" label="金额" width="110" />
      <el-table-column label="收支类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ accountTypeLabels[(row as AccountVO).type] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ accountStatusLabels[(row as AccountVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分摊状态" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ allocationStatusLabels[(row as AccountVO).allocationStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="paymentAccountId" label="付款账户 ID" width="170" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑收支流水' : '新增收支流水'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="项目 ID">
          <el-input v-model="form.projectId" placeholder="项目 ID（可选）" />
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
        <el-form-item label="付款账户 ID">
          <el-input v-model="form.paymentAccountId" placeholder="付款账户 ID（可选）" />
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
        <el-descriptions-item label="项目 ID">{{ detail.projectId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易日期">{{ detail.transactionDate }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="收支类型">{{ accountTypeLabels[detail.type] }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ accountStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="分摊状态">{{ allocationStatusLabels[detail.allocationStatus] }}</el-descriptions-item>
        <el-descriptions-item label="付款账户 ID">{{ detail.paymentAccountId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="票据 ID">{{ detail.receiptId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同 ID">{{ detail.contractId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付类型">{{ detail.paymentType ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
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
</style>
