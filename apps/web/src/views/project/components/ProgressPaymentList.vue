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
import type { ApprovalStatus, ProgressPaymentVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { approvalStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'ProgressPaymentList' })

const statusOptions = toOptions(approvalStatusLabels)

/** 状态标签配色映射：仅决定颜色，状态值与文案保持原样。 */
function statusTagType(status: ApprovalStatus): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'APPROVED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED' || status === 'CANCELLED') return 'danger'
  return 'info'
}

const query = reactive<{ projectId?: string; status?: ApprovalStatus }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ProgressPaymentVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<ProgressPaymentVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  projectId: '',
  paymentPeriod: '',
  applicationAmount: '',
  applicationDate: ''
})

/** 项目下拉选项：id 为雪花 ID 字符串，label 为项目名称。 */
interface SelectOption {
  id: string
  label: string
}

const projectOptions = ref<SelectOption[]>([])
const projectOptionsLoading = ref(false)
/** 选项加载失败只提示一次，避免远程搜索反复弹窗。 */
const projectOptionsErrorShown = ref(false)

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

const rules: FormRules<typeof form> = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  paymentPeriod: [{ required: true, message: '请输入付款期次', trigger: 'blur' }],
  applicationAmount: [{ required: true, message: '请输入申请金额', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.paymentPage({
      page: page.value,
      size: size.value,
      projectId: query.projectId,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载进度款失败'))
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
  Object.assign(form, { projectId: '', paymentPeriod: '', applicationAmount: '', applicationDate: '' })
  dialogVisible.value = true
  void loadProjectOptions()
}

function openEdit(row: ProgressPaymentVO): void {
  editingId.value = row.id
  // 行数据带名称时作为兜底选项，保证编辑态下拉能显示名称而不是 ID
  if (row.projectId) {
    projectOptions.value = addFallback(projectOptions.value, {
      id: row.projectId,
      label: row.projectName || row.projectId
    })
  }
  form.projectId = row.projectId ?? ''
  form.paymentPeriod = row.paymentPeriod
  form.applicationAmount = row.applicationAmount ?? ''
  form.applicationDate = row.applicationDate ?? ''
  dialogVisible.value = true
  void loadProjectOptions()
}

function openDetail(row: ProgressPaymentVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.paymentUpdate(editingId.value, {
        paymentPeriod: form.paymentPeriod,
        applicationAmount: form.applicationAmount || undefined,
        applicationDate: form.applicationDate || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.paymentCreate({
        projectId: form.projectId,
        paymentPeriod: form.paymentPeriod,
        applicationAmount: form.applicationAmount,
        applicationDate: form.applicationDate || undefined
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: ProgressPaymentVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该进度款记录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.paymentRemove(row.id)
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
        style="width: 200px"
      >
        <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="审批状态" clearable style="width: 160px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部标题、条数与主操作 -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">进度款列表</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增进度款</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column label="项目" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ (row as ProgressPaymentVO).projectName || (row as ProgressPaymentVO).projectId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="paymentPeriod" label="付款期次" width="120" />
        <el-table-column prop="applicationAmount" label="申请金额" width="120" align="right" class-name="amount-col" />
        <el-table-column prop="approvedAmount" label="批准金额" width="120" align="right" class-name="amount-col" />
        <el-table-column prop="paymentAmount" label="实付金额" width="120" align="right" class-name="amount-col" />
        <el-table-column prop="applicationDate" label="申请日期" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType((row as ProgressPaymentVO).status)">
              {{ approvalStatusLabels[(row as ProgressPaymentVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicationBy" label="申请人" width="110" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as ProgressPaymentVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as ProgressPaymentVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as ProgressPaymentVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑进度款' : '新增进度款'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目" prop="projectId">
          <el-select
            v-model="form.projectId"
            :disabled="!!editingId"
            filterable
            remote
            remote-show-suffix
            :remote-method="onProjectSearch"
            :loading="projectOptionsLoading"
            placeholder="请选择项目"
            style="width: 100%"
          >
            <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款期次" prop="paymentPeriod">
          <el-input v-model="form.paymentPeriod" placeholder="如：第 1 期" />
        </el-form-item>
        <el-form-item label="申请金额" prop="applicationAmount">
          <el-input v-model="form.applicationAmount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="申请日期">
          <el-date-picker v-model="form.applicationDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="进度款详情" width="520px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="项目">{{ detail.projectName || detail.projectId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="付款期次">{{ detail.paymentPeriod }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ detail.applicationAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="批准金额">{{ detail.approvedAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ detail.paymentAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请日期">{{ detail.applicationDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ approvalStatusLabels[detail.status] }}</el-descriptions-item>
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
