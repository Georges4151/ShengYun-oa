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
import type { BudgetStatus, BudgetVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { budgetStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'BudgetList' })

const statusOptions = toOptions(budgetStatusLabels)

/** 状态标签配色映射：仅决定颜色，状态值与文案保持原样。 */
function statusTagType(status: BudgetStatus): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'ACTIVE') return 'warning'
  return 'info'
}

/** 下拉选项：id 为雪花 ID 字符串，label 为展示名称。 */
interface SelectOption {
  id: string
  label: string
}

const projectOptions = ref<SelectOption[]>([])
const contractOptions = ref<SelectOption[]>([])
const projectOptionsLoading = ref(false)
const contractOptionsLoading = ref(false)
/** 选项加载失败只提示一次，避免远程搜索反复弹窗。 */
const projectOptionsErrorShown = ref(false)
const contractOptionsErrorShown = ref(false)

/** 远程搜索后把当前已选中的选项补回列表，避免展示退化成 ID。 */
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

async function loadProjectOptions(keyword = ''): Promise<void> {
  projectOptionsLoading.value = true
  try {
    const res = await ceamsApi.project.projectPage({
      page: 1,
      size: 50,
      keyword: keyword || undefined
    })
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

function onProjectSearch(keyword: string): void {
  void loadProjectOptions(keyword)
}

/**
 * 加载合同选项：后端合同分页不支持 keyword，故一次性取当前项目下的合同，
 * 由 el-select 的 filterable 做本地过滤。返回本次拉取结果，供选择项目后判断是否清空。
 */
async function loadContractOptions(): Promise<SelectOption[] | null> {
  contractOptionsLoading.value = true
  try {
    const res = await ceamsApi.project.contractPage({
      page: 1,
      size: 100,
      projectId: form.projectId || undefined
    })
    const fetched = res.records.map((item) => ({ id: item.id, label: item.contractName }))
    contractOptions.value = keepSelected(contractOptions.value, fetched, [form.contractId])
    return fetched
  } catch (err) {
    if (!contractOptionsErrorShown.value) {
      contractOptionsErrorShown.value = true
      ElMessage.error(apiErrorMessage(err, '合同选项加载失败'))
    }
    return null
  } finally {
    contractOptionsLoading.value = false
  }
}

/** 切换项目后重新加载合同选项；已选合同不属于新项目时清空。 */
async function onProjectChange(): Promise<void> {
  const previousContractId = form.contractId
  const fetched = await loadContractOptions()
  if (fetched && previousContractId && !fetched.some((item) => item.id === previousContractId)) {
    form.contractId = ''
  }
}

const query = reactive<{ projectId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<BudgetVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<BudgetVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  budgetName: '',
  amount: '',
  projectId: '',
  contractId: '',
  startDate: '',
  endDate: '',
  status: 'DRAFT' as BudgetStatus
})

const rules: FormRules<typeof form> = {
  budgetName: [{ required: true, message: '请输入预算名称', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
  status: [{ required: true, message: '请选择预算状态', trigger: 'change' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.budgetPage({
      page: page.value,
      size: size.value,
      projectId: query.projectId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载预算失败'))
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
    budgetName: '',
    amount: '',
    projectId: '',
    contractId: '',
    startDate: '',
    endDate: '',
    status: 'DRAFT'
  })
  dialogVisible.value = true
  void loadContractOptions()
}

function openEdit(row: BudgetVO): void {
  editingId.value = row.id
  // 行数据带名称时作为兜底选项，保证编辑态下拉能显示名称而不是 ID
  if (row.projectId) {
    projectOptions.value = addFallback(projectOptions.value, {
      id: row.projectId,
      label: row.projectName || row.projectId
    })
  }
  if (row.contractId) {
    contractOptions.value = addFallback(contractOptions.value, {
      id: row.contractId,
      label: row.contractName || row.contractId
    })
  }
  form.budgetName = row.budgetName
  form.amount = row.amount
  form.projectId = row.projectId ?? ''
  form.contractId = row.contractId ?? ''
  form.startDate = row.startDate ?? ''
  form.endDate = row.endDate ?? ''
  form.status = row.status
  dialogVisible.value = true
  void loadContractOptions()
}

function openDetail(row: BudgetVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.budgetUpdate(editingId.value, {
        budgetName: form.budgetName,
        amount: form.amount,
        projectId: form.projectId || undefined,
        contractId: form.contractId || undefined,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        status: form.status
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.budgetCreate({
        budgetName: form.budgetName,
        amount: form.amount,
        projectId: form.projectId || null,
        contractId: form.contractId || null,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        status: form.status
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: BudgetVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除预算「${row.budgetName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.budgetRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(() => {
  void load()
  void loadProjectOptions()
})
</script>

<template>
  <div class="list-page">
    <!-- 筛选卡片：控件 12px 间距，按钮组靠右 -->
    <div class="data-card filter-card">
      <el-select
        v-model="query.projectId"
        placeholder="项目（可选）"
        clearable
        filterable
        remote
        remote-show-suffix
        :remote-method="onProjectSearch"
        :loading="projectOptionsLoading"
        style="width: 220px"
      >
        <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部标题、条数与主操作 -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">预算列表</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增预算</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="budgetName" label="预算名称" min-width="180" />
        <el-table-column prop="amount" label="预算金额" width="120" align="right" class-name="amount-col" />
        <el-table-column label="项目" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ (row as BudgetVO).projectName || (row as BudgetVO).projectId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="合同" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ (row as BudgetVO).contractName || (row as BudgetVO).contractId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType((row as BudgetVO).status)">
              {{ budgetStatusLabels[(row as BudgetVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="usedAmount" label="已用金额" width="120" align="right" class-name="amount-col" />
        <el-table-column prop="remainingAmount" label="剩余金额" width="120" align="right" class-name="amount-col" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as BudgetVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as BudgetVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as BudgetVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑预算' : '新增预算'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="预算名称" prop="budgetName">
          <el-input v-model="form.budgetName" placeholder="请输入预算名称" />
        </el-form-item>
        <el-form-item label="预算金额" prop="amount">
          <el-input v-model="form.amount" placeholder="金额（字符串，如 500000.00）" />
        </el-form-item>
        <el-form-item label="项目">
          <el-select
            v-model="form.projectId"
            clearable
            filterable
            remote
            remote-show-suffix
            :remote-method="onProjectSearch"
            :loading="projectOptionsLoading"
            placeholder="项目（可选）"
            style="width: 100%"
            @change="onProjectChange"
          >
            <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同">
          <el-select
            v-model="form.contractId"
            clearable
            filterable
            :loading="contractOptionsLoading"
            placeholder="合同（可选）"
            style="width: 100%"
          >
            <el-option v-for="opt in contractOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="预算详情" width="560px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="预算名称">{{ detail.budgetName }}</el-descriptions-item>
        <el-descriptions-item label="预算金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="项目">{{ detail.projectName || detail.projectId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同">{{ detail.contractName || detail.contractId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail.startDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detail.endDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ budgetStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="已用金额">{{ detail.usedAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="剩余金额">{{ detail.remainingAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 筛选卡片：16px 20px 内边距，控件 12px 间距，按钮组靠右 */
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
  align-items: center;
  gap: var(--ceams-space-2);
  margin-left: auto;
}

/* 表格卡片：20px 内边距，头部放标题、条数与主操作 */
.table-card {
  padding: 20px;
}

.card-head {
  display: flex;
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
  align-items: center;
  gap: var(--ceams-space-2);
}

/* 金额列右对齐并使用等宽数字，避免数字跳动 */
:deep(.amount-col .cell) {
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}

/* 分页右对齐，距表格 16px */
.pagination {
  justify-content: flex-end;
  margin-top: var(--ceams-space-4);
}

/* 空态居中灰色，上下 40px 留白 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}

/* 弹窗：标题加粗、内容区 20px 24px、底部操作条靠右并带顶部细边框 */
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
