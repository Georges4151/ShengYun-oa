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
import type {
  PaymentMethod,
  ReceiptApprovalStatus,
  ReceiptType,
  ReceiptVO,
  VerificationStatus
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
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
  description: ''
})

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
    userId: '',
    reporterName: '',
    projectId: '',
    receiptDate: '',
    amount: '',
    type: 'TRANSPORTATION',
    paymentMethod: '',
    description: ''
  })
  dialogVisible.value = true
}

function openEdit(row: ReceiptVO): void {
  editingId.value = row.id
  form.receiptNumber = row.receiptNumber
  form.userId = row.userId ?? ''
  form.reporterName = row.reporterName ?? ''
  form.projectId = row.projectId ?? ''
  form.receiptDate = row.receiptDate ?? ''
  form.amount = row.amount
  form.type = row.type
  form.paymentMethod = row.paymentMethod ?? ''
  form.description = row.description ?? ''
  dialogVisible.value = true
}

function openDetail(row: ReceiptVO): void {
  detail.value = row
  detailVisible.value = true
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
        receiptDate: form.receiptDate || null,
        amount: form.amount,
        type: form.type,
        paymentMethod: form.paymentMethod || null,
        description: form.description || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.expense.receiptCreate({
        receiptNumber: form.receiptNumber,
        userId: form.userId || null,
        reporterName: form.reporterName || null,
        projectId: form.projectId || null,
        receiptDate: form.receiptDate || null,
        amount: form.amount,
        type: form.type,
        paymentMethod: form.paymentMethod || null,
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
    <div class="toolbar">
      <el-input v-model="query.receiptNumber" placeholder="票据编号" clearable style="width: 200px" />
      <el-select v-model="query.verificationStatus" placeholder="核验状态" clearable style="width: 150px">
        <el-option v-for="opt in verificationOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="query.approvalStatus" placeholder="审批状态" clearable style="width: 150px">
        <el-option v-for="opt in approvalOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增票据</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="receiptNumber" label="票据编号" min-width="160" />
      <el-table-column prop="reporterName" label="报销人" width="110" />
      <el-table-column prop="amount" label="金额" width="110" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ receiptTypeLabels[(row as ReceiptVO).type] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="核验状态" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ verificationStatusLabels[(row as ReceiptVO).verificationStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款状态" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ paymentStatusLabels[(row as ReceiptVO).paymentStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批状态" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ receiptApprovalStatusLabels[(row as ReceiptVO).approvalStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="receiptDate" label="票据日期" width="120" />
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑票据' : '新增票据'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="票据编号" prop="receiptNumber">
          <el-input v-model="form.receiptNumber" :disabled="!!editingId" placeholder="请输入票据编号" />
        </el-form-item>
        <el-form-item label="用户 ID">
          <el-input v-model="form.userId" placeholder="用户 ID（可选）" />
        </el-form-item>
        <el-form-item label="报销人">
          <el-input v-model="form.reporterName" placeholder="报销人姓名（可选）" />
        </el-form-item>
        <el-form-item label="项目 ID">
          <el-input v-model="form.projectId" placeholder="项目 ID（可选）" />
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
        <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="已付金额">{{ detail.paidAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="票据类型">{{ receiptTypeLabels[detail.type] }}</el-descriptions-item>
        <el-descriptions-item label="核验状态">{{ verificationStatusLabels[detail.verificationStatus] }}</el-descriptions-item>
        <el-descriptions-item label="付款状态">{{ paymentStatusLabels[detail.paymentStatus] }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">{{ receiptApprovalStatusLabels[detail.approvalStatus] }}</el-descriptions-item>
        <el-descriptions-item label="票据日期">{{ detail.receiptDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.paymentMethod ? paymentMethodLabels[detail.paymentMethod] : '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目 ID">{{ detail.projectId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户 ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="流程实例">{{ detail.bpmInstanceId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <BpmInstanceDrawer v-model="drawerVisible" :instance-id="drawerInstanceId" />
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
