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
import type { ProjectStatus, ProjectVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { projectStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'ProjectList' })

const statusOptions = toOptions(projectStatusLabels)

/** 状态标签配色映射：仅决定颜色，状态值与文案保持原样。 */
function statusTagType(status: ProjectStatus): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS' || status === 'PAUSED') return 'warning'
  return 'info'
}

const query = reactive<{ keyword?: string; status?: ProjectStatus }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ProjectVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<ProjectVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  projectName: '',
  clientName: '',
  projectAddress: '',
  projectType: '',
  startDate: '',
  endDate: '',
  contractAmount: '',
  budgetAmount: '',
  status: 'PLANNING' as ProjectStatus,
  description: '',
  managerId: ''
})

const rules: FormRules<typeof form> = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  clientName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  projectType: [{ required: true, message: '请输入项目类型', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  contractAmount: [{ required: true, message: '请输入合同金额', trigger: 'blur' }],
  budgetAmount: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
  status: [{ required: true, message: '请选择项目状态', trigger: 'change' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.projectPage({
      page: page.value,
      size: size.value,
      keyword: query.keyword,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载项目失败'))
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
    projectName: '',
    clientName: '',
    projectAddress: '',
    projectType: '',
    startDate: '',
    endDate: '',
    contractAmount: '',
    budgetAmount: '',
    status: 'PLANNING',
    description: '',
    managerId: ''
  })
  dialogVisible.value = true
}

function openEdit(row: ProjectVO): void {
  editingId.value = row.id
  form.projectName = row.projectName
  form.clientName = row.clientName ?? ''
  form.projectAddress = row.projectAddress ?? ''
  form.projectType = row.projectType
  form.startDate = row.startDate ?? ''
  form.endDate = row.endDate ?? ''
  form.contractAmount = row.contractAmount ?? ''
  form.budgetAmount = row.budgetAmount ?? ''
  form.status = row.status
  form.description = row.description ?? ''
  form.managerId = row.managerId ?? ''
  dialogVisible.value = true
}

function openDetail(row: ProjectVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.projectUpdate(editingId.value, {
        projectName: form.projectName,
        clientName: form.clientName,
        projectAddress: form.projectAddress || undefined,
        projectType: form.projectType,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        contractAmount: form.contractAmount,
        budgetAmount: form.budgetAmount,
        status: form.status,
        description: form.description || undefined,
        managerId: form.managerId || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.projectCreate({
        projectName: form.projectName,
        clientName: form.clientName,
        projectAddress: form.projectAddress || null,
        projectType: form.projectType,
        startDate: form.startDate,
        endDate: form.endDate || null,
        contractAmount: form.contractAmount,
        budgetAmount: form.budgetAmount,
        status: form.status,
        description: form.description || null,
        managerId: form.managerId || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: ProjectVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除项目「${row.projectName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.projectRemove(row.id)
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
    <!-- 筛选卡片：控件 12px 间距，按钮组靠右 -->
    <div class="data-card filter-card">
      <el-input v-model="query.keyword" placeholder="关键字（项目名称/客户）" clearable style="width: 220px" />
      <el-select v-model="query.status" placeholder="项目状态" clearable style="width: 160px">
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
          <span class="card-title">项目列表</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增项目</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="projectName" label="项目名称" min-width="180" />
        <el-table-column prop="clientName" label="客户" width="140" />
        <el-table-column prop="projectType" label="类型" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType((row as ProjectVO).status)">
              {{ projectStatusLabels[(row as ProjectVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contractAmount" label="合同金额" width="120" align="right" class-name="amount-col" />
        <el-table-column prop="budgetAmount" label="预算金额" width="120" align="right" class-name="amount-col" />
        <el-table-column prop="managerName" label="负责人" width="110" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as ProjectVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as ProjectVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as ProjectVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑项目' : '新增项目'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="客户名称" prop="clientName">
          <el-input v-model="form.clientName" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="项目地址">
          <el-input v-model="form.projectAddress" placeholder="项目地址（可选）" />
        </el-form-item>
        <el-form-item label="项目类型" prop="projectType">
          <el-input v-model="form.projectType" placeholder="如：房建 / 市政 / 装饰" />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="合同金额" prop="contractAmount">
          <el-input v-model="form.contractAmount" placeholder="金额（字符串，如 1200000.00）" />
        </el-form-item>
        <el-form-item label="预算金额" prop="budgetAmount">
          <el-input v-model="form.budgetAmount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="项目状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人 ID">
          <el-input v-model="form.managerId" placeholder="用户 ID（可选）" />
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

    <el-dialog v-model="detailVisible" title="项目详情" width="640px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="项目名称">{{ detail.projectName }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.clientName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目类型">{{ detail.projectType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ projectStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail.startDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detail.endDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同金额">{{ detail.contractAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="预算金额">{{ detail.budgetAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际金额">{{ detail.actualAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detail.managerName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.projectAddress ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
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
