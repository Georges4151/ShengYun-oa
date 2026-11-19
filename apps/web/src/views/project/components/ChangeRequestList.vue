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
import type { ApprovalStatus, ChangeRequestVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { approvalStatusLabels, toOptions } from '@/utils/labels'
import BpmInstanceDrawer from '@/views/bpm/components/BpmInstanceDrawer.vue'

defineOptions({ name: 'ChangeRequestList' })

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
const list = ref<ChangeRequestVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<ChangeRequestVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  projectId: '',
  requestType: '',
  description: '',
  estimatedCost: ''
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
  requestType: [{ required: true, message: '请输入变更类型', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.changePage({
      page: page.value,
      size: size.value,
      projectId: query.projectId,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载变更请求失败'))
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
  Object.assign(form, { projectId: '', requestType: '', description: '', estimatedCost: '' })
  dialogVisible.value = true
  void loadProjectOptions()
}

function openEdit(row: ChangeRequestVO): void {
  editingId.value = row.id
  // 行数据带名称时作为兜底选项，保证编辑态下拉能显示名称而不是 ID
  if (row.projectId) {
    projectOptions.value = addFallback(projectOptions.value, {
      id: row.projectId,
      label: row.projectName || row.projectId
    })
  }
  form.projectId = row.projectId ?? ''
  form.requestType = row.requestType
  form.description = row.description ?? ''
  form.estimatedCost = row.estimatedCost ?? ''
  dialogVisible.value = true
  void loadProjectOptions()
}

function openDetail(row: ChangeRequestVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.changeUpdate(editingId.value, {
        requestType: form.requestType,
        description: form.description || undefined,
        estimatedCost: form.estimatedCost || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.changeCreate({
        projectId: form.projectId,
        requestType: form.requestType,
        description: form.description || null,
        estimatedCost: form.estimatedCost || undefined
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: ChangeRequestVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该变更请求吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.changeRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

/** 提交审批：发起 BPM 项目变更流程（部门审批 → 结束）。 */
async function submitChange(row: ChangeRequestVO): Promise<void> {
  try {
    await ElMessageBox.confirm('提交后进入部门审批，确定提交吗？', '提交变更', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.changeSubmit(row.id)
    ElMessage.success('已提交，请到待办中心跟进审批')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '提交失败'))
  }
}

const drawerVisible = ref(false)
const drawerInstanceId = ref<string | null>(null)

function openInstance(instanceId?: string | null): void {
  if (!instanceId) return
  drawerInstanceId.value = instanceId
  drawerVisible.value = true
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
          <span class="card-title">变更请求列表</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增变更</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column label="项目" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ (row as ChangeRequestVO).projectName || (row as ChangeRequestVO).projectId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="requestType" label="变更类型" width="140" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="estimatedCost" label="预估费用" width="120" align="right" class-name="amount-col" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType((row as ChangeRequestVO).status)">
              {{ approvalStatusLabels[(row as ChangeRequestVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestedBy" label="申请人" width="110" />
        <el-table-column prop="approvedBy" label="审批人" width="110" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as ChangeRequestVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as ChangeRequestVO)">编辑</el-button>
            <el-button
              v-if="(row as ChangeRequestVO).status === 'DRAFT' || (row as ChangeRequestVO).status === 'REJECTED'"
              link
              type="success"
              size="small"
              @click="submitChange(row as ChangeRequestVO)"
            >
              提交审批
            </el-button>
            <el-button
              v-if="(row as ChangeRequestVO).bpmInstanceId"
              link
              type="primary"
              size="small"
              @click="openInstance((row as ChangeRequestVO).bpmInstanceId)"
            >
              查看流程
            </el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as ChangeRequestVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑变更请求' : '新增变更请求'" width="520px">
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
        <el-form-item label="变更类型" prop="requestType">
          <el-input v-model="form.requestType" placeholder="如：设计变更 / 工期变更" />
        </el-form-item>
        <el-form-item label="预估费用">
          <el-input v-model="form.estimatedCost" placeholder="金额（可选）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="变更描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="变更请求详情" width="520px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="项目">{{ detail.projectName || detail.projectId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="变更类型">{{ detail.requestType }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ detail.description ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="预估费用">{{ detail.estimatedCost ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ approvalStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.requestedBy ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.approvedBy ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ detail.approvedAt ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <BpmInstanceDrawer v-model="drawerVisible" :instance-id="drawerInstanceId" />
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
