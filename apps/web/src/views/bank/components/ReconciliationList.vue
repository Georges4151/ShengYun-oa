<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
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
  ReconciliationResultVO,
  ReconciliationStatus,
  ResolutionStatus
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import {
  discrepancyTypeLabels,
  reconciliationStatusLabels,
  resolutionStatusLabels,
  toOptions
} from '@/utils/labels'

defineOptions({ name: 'ReconciliationList' })

const statusOptions = toOptions(reconciliationStatusLabels)
const resolutionOptions = toOptions(resolutionStatusLabels)

const query = reactive<{
  accountId?: string
  status?: ReconciliationStatus
  resolutionStatus?: ResolutionStatus
}>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ReconciliationResultVO[]>([])
const loading = ref(false)

const detailVisible = ref(false)
const matchVisible = ref(false)
const detail = ref<ReconciliationResultVO | null>(null)
const matchFormRef = ref<FormInstance>()
const matchForm = reactive({
  accountId: '',
  daysTolerance: 3
})
const matchResult = ref<{ matched: number; unmatched: number; partialMatched: number } | null>(null)
const matching = ref(false)

const matchRules: FormRules<typeof matchForm> = {
  accountId: [{ required: true, message: '请输入银行账户 ID', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.bank.reconciliationPage({
      page: page.value,
      size: size.value,
      accountId: query.accountId,
      status: query.status,
      resolutionStatus: query.resolutionStatus
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载对账结果失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

function openDetail(row: ReconciliationResultVO): void {
  detail.value = row
  detailVisible.value = true
}

function openMatch(): void {
  matchForm.accountId = ''
  matchForm.daysTolerance = 3
  matchResult.value = null
  matchVisible.value = true
}

async function submitMatch(): Promise<void> {
  if (!matchFormRef.value) return
  const valid = await matchFormRef.value.validate().catch(() => false)
  if (!valid) return

  matching.value = true
  try {
    const res = await ceamsApi.bank.reconciliationMatch({
      accountId: matchForm.accountId,
      daysTolerance: matchForm.daysTolerance
    })
    matchResult.value = res
    ElMessage.success('自动匹配完成')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '自动匹配失败'))
  } finally {
    matching.value = false
  }
}

async function onResolve(row: ReconciliationResultVO): Promise<void> {
  let notes = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入处理说明', '标记已处理', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })
    notes = value ?? ''
  } catch {
    return
  }
  try {
    await ceamsApi.bank.reconciliationResolve(row.id, { resolutionNotes: notes || null })
    ElMessage.success('已标记处理')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '标记处理失败'))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.accountId" placeholder="银行账户 ID" clearable style="width: 200px" />
      <el-select v-model="query.status" placeholder="匹配状态" clearable style="width: 140px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="query.resolutionStatus" placeholder="处理状态" clearable style="width: 140px">
        <el-option v-for="opt in resolutionOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openMatch">自动匹配</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="accountId" label="银行账户 ID" width="180" show-overflow-tooltip />
      <el-table-column label="匹配状态" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ reconciliationStatusLabels[(row as ReconciliationResultVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ resolutionStatusLabels[(row as ReconciliationResultVO).resolutionStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="差异类型" width="130">
        <template #default="{ row }">
          {{ (row as ReconciliationResultVO).discrepancyType ? discrepancyTypeLabels[(row as ReconciliationResultVO).discrepancyType!] : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="bankAmount" label="银行金额" width="110" />
      <el-table-column prop="systemAmount" label="系统金额" width="110" />
      <el-table-column prop="statementReference" label="银行流水号" width="160" show-overflow-tooltip />
      <el-table-column prop="transactionReference" label="系统流水号" width="160" show-overflow-tooltip />
      <el-table-column prop="reconciliationDate" label="对账日期" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as ReconciliationResultVO)">详情</el-button>
          <el-button
            v-if="(row as ReconciliationResultVO).resolutionStatus === 'UNRESOLVED'"
            link
            type="success"
            size="small"
            @click="onResolve(row as ReconciliationResultVO)"
          >
            标记已处理
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

    <el-dialog v-model="detailVisible" title="对账结果详情" width="640px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="银行账户 ID">{{ detail.accountId }}</el-descriptions-item>
        <el-descriptions-item label="匹配状态">{{ reconciliationStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">{{ resolutionStatusLabels[detail.resolutionStatus] }}</el-descriptions-item>
        <el-descriptions-item label="差异类型">{{ detail.discrepancyType ? discrepancyTypeLabels[detail.discrepancyType] : '-' }}</el-descriptions-item>
        <el-descriptions-item label="银行金额">{{ detail.bankAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="系统金额">{{ detail.systemAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="银行日期">{{ detail.bankDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="系统日期">{{ detail.systemDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="银行描述">{{ detail.bankDescription ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="系统描述">{{ detail.systemDescription ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="银行流水号">{{ detail.statementReference ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="系统流水号">{{ detail.transactionReference ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理说明" :span="2">{{ detail.resolutionNotes ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="对账日期">{{ detail.reconciliationDate ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="matchVisible" title="自动匹配" width="520px">
      <el-form ref="matchFormRef" :model="matchForm" :rules="matchRules" label-width="120px">
        <el-form-item label="银行账户 ID" prop="accountId">
          <el-input v-model="matchForm.accountId" placeholder="请输入银行账户 ID" />
        </el-form-item>
        <el-form-item label="日期容忍天数">
          <el-input-number v-model="matchForm.daysTolerance" :min="0" :max="30" style="width: 100%" />
        </el-form-item>
      </el-form>
      <el-descriptions v-if="matchResult" :column="3" border size="small" class="match-result">
        <el-descriptions-item label="已匹配">{{ matchResult.matched }}</el-descriptions-item>
        <el-descriptions-item label="未匹配">{{ matchResult.unmatched }}</el-descriptions-item>
        <el-descriptions-item label="部分匹配">{{ matchResult.partialMatched }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="matchVisible = false">关闭</el-button>
        <el-button type="primary" :loading="matching" @click="submitMatch">开始匹配</el-button>
      </template>
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
.match-result {
  margin-top: 8px;
}
</style>
