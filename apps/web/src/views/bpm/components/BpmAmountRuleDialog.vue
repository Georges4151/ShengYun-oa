<script setup lang="ts">
/**
 * 报销额度分级规则维护弹窗。
 * 列表 + 新增/编辑 + 删除，入口由 BpmView 的「流程模板」页签提供。
 * 规则按金额区间匹配报销审批流程模板，部门/项目为空表示不限。
 */
import { computed, nextTick, reactive, ref, watch } from 'vue'
import {
  ElButton,
  ElDialog,
  ElEmpty,
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
import type { AmountRuleSaveRequest, AmountRuleStatus, AmountRuleVO, DeptVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'BpmAmountRuleDialog' })

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

/** 下拉选项：id 为雪花 ID（部门/项目）或模板编码（流程模板）。 */
interface SelectOption {
  id: string
  label: string
}

const statusLabels: Record<AmountRuleStatus, string> = {
  ENABLED: '启用',
  DISABLED: '停用'
}

const statusOptions: { value: AmountRuleStatus; label: string }[] = [
  { value: 'ENABLED', label: '启用' },
  { value: 'DISABLED', label: '停用' }
]

// ---------------- 列表 ----------------
const { page, size, total } = usePage(20)
const list = ref<AmountRuleVO[]>([])
const listLoading = ref(false)

async function loadList(): Promise<void> {
  listLoading.value = true
  try {
    const res = await ceamsApi.expense.amountRuleList({ page: page.value, size: size.value })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载额度分级规则失败'))
  } finally {
    listLoading.value = false
  }
}

/** 金额展示：后端返回两位小数字符串，异常值原样展示。 */
function formatMoney(value: string | null | undefined): string {
  if (value === null || value === undefined || value === '') return '-'
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toFixed(2) : value
}

/** 金额区间：最高金额为空表示无上限。 */
function amountRange(row: AmountRuleVO): string {
  const min = formatMoney(row.minAmount)
  if (row.maxAmount === null || row.maxAmount === undefined || row.maxAmount === '') {
    return `${min} ~ 不限`
  }
  return `${min} ~ ${formatMoney(row.maxAmount)}`
}

function statusText(row: AmountRuleVO): string {
  return statusLabels[row.status] ?? row.status
}

// ---------------- 下拉选项 ----------------
const deptOptions = ref<SelectOption[]>([])
const projectOptions = ref<SelectOption[]>([])
const templateOptions = ref<SelectOption[]>([])

/** 部门树扁平化为下拉选项，用缩进体现层级。 */
function flattenDept(nodes: DeptVO[], depth = 0, result: SelectOption[] = []): SelectOption[] {
  for (const node of nodes) {
    result.push({ id: node.id, label: `${'　'.repeat(depth)}${node.name}` })
    if (node.children?.length) flattenDept(node.children, depth + 1, result)
  }
  return result
}

/** 编辑态已选值可能不在前 100 条内，用行数据名称兜底，避免下拉显示成 ID。 */
function ensureOption(options: SelectOption[], id?: string | null, label?: string | null): void {
  if (!id || options.some((item) => item.id === id)) return
  options.push({ id, label: label || id })
}

/** 加载下拉选项：部门取部门树，项目/流程模板分页均限制在 100 条以内。 */
async function loadOptions(): Promise<void> {
  try {
    const [deptTree, projectRes, templateRes] = await Promise.all([
      ceamsApi.iam.deptTree(),
      ceamsApi.project.projectPage({ page: 1, size: 100 }),
      ceamsApi.bpm.templatePage({ page: 1, size: 100 })
    ])
    deptOptions.value = flattenDept(deptTree)
    projectOptions.value = projectRes.records.map((item) => ({ id: item.id, label: item.projectName }))
    // 仅保留报销业务且已启用的流程模板作为可选审批模板
    templateOptions.value = templateRes.records
      .filter((item) => item.bizType === 'EXPENSE' && item.status === 'ENABLED')
      .map((item) => ({ id: item.code, label: `${item.name}（${item.code}）` }))
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载部门/项目/流程模板失败'))
  }
}

// ---------------- 表单 ----------------
const formVisible = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<{
  name: string
  minAmount: number | null
  maxAmount: number | null
  deptId: string
  projectId: string
  templateCode: string
  priority: number
  status: AmountRuleStatus
  remark: string
}>({
  name: '',
  minAmount: null,
  maxAmount: null,
  deptId: '',
  projectId: '',
  templateCode: '',
  priority: 0,
  status: 'ENABLED',
  remark: ''
})

function validateMinAmount(
  _rule: unknown,
  value: unknown,
  callback: (error?: string | Error) => void
): void {
  if (value === null || value === undefined || value === '') {
    callback(new Error('请输入最低金额'))
    return
  }
  const min = Number(value)
  if (!Number.isFinite(min)) {
    callback(new Error('最低金额格式不正确'))
    return
  }
  if (min < 0) {
    callback(new Error('最低金额不能小于 0'))
    return
  }
  callback()
}

function validateMaxAmount(
  _rule: unknown,
  value: unknown,
  callback: (error?: string | Error) => void
): void {
  // 留空表示无上限，不参与校验
  if (value === null || value === undefined || value === '') {
    callback()
    return
  }
  const max = Number(value)
  if (!Number.isFinite(max)) {
    callback(new Error('最高金额格式不正确'))
    return
  }
  const min = form.minAmount
  if (min !== null && min !== undefined && max <= min) {
    callback(new Error('最高金额必须大于最低金额'))
    return
  }
  callback()
}

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  minAmount: [{ validator: validateMinAmount, trigger: 'change' }],
  maxAmount: [{ validator: validateMaxAmount, trigger: 'change' }],
  templateCode: [{ required: true, message: '请选择流程模板', trigger: 'change' }]
}

/** 最低金额变化时联动校验最高金额，避免区间倒挂。 */
function onMinAmountChange(): void {
  void formRef.value?.validateField('maxAmount').catch(() => undefined)
}

function parseAmount(value: string | null | undefined): number | null {
  if (value === null || value === undefined || value === '') return null
  const amount = Number(value)
  return Number.isFinite(amount) ? amount : null
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, {
    name: '',
    minAmount: null,
    maxAmount: null,
    deptId: '',
    projectId: '',
    templateCode: '',
    priority: 0,
    status: 'ENABLED' as AmountRuleStatus,
    remark: ''
  })
  formVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: AmountRuleVO): void {
  editingId.value = row.id
  ensureOption(deptOptions.value, row.deptId, row.deptName)
  ensureOption(projectOptions.value, row.projectId, row.projectName)
  ensureOption(templateOptions.value, row.templateCode, row.templateName)
  Object.assign(form, {
    name: row.name,
    minAmount: parseAmount(row.minAmount),
    maxAmount: parseAmount(row.maxAmount),
    deptId: row.deptId ?? '',
    projectId: row.projectId ?? '',
    templateCode: row.templateCode,
    priority: row.priority ?? 0,
    status: row.status ?? 'ENABLED',
    remark: row.remark ?? ''
  })
  formVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

async function submitForm(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const minAmount = form.minAmount
  if (minAmount === null) return
  const body: AmountRuleSaveRequest = {
    name: form.name.trim(),
    minAmount: minAmount.toFixed(2),
    maxAmount: typeof form.maxAmount === 'number' ? form.maxAmount.toFixed(2) : null,
    deptId: form.deptId || null,
    projectId: form.projectId || null,
    templateCode: form.templateCode,
    priority: form.priority ?? 0,
    status: form.status,
    remark: form.remark.trim() || null
  }
  saving.value = true
  try {
    if (editingId.value) {
      await ceamsApi.expense.amountRuleUpdate(editingId.value, body)
      ElMessage.success('规则已更新')
    } else {
      await ceamsApi.expense.amountRuleCreate(body)
      ElMessage.success('规则已创建')
    }
    formVisible.value = false
    await loadList()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存额度分级规则失败'))
  } finally {
    saving.value = false
  }
}

async function removeRule(row: AmountRuleVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除额度分级规则「${row.name}」吗？删除后不可恢复。`,
      '删除规则',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await ceamsApi.expense.amountRuleRemove(row.id)
    ElMessage.success('删除成功')
    // 删除当前页最后一条时回退一页，避免停留在空页
    if (list.value.length === 1 && page.value > 1) page.value -= 1
    await loadList()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

// 每次打开弹窗都回到第一页并刷新列表与下拉选项
watch(
  () => props.modelValue,
  (opened) => {
    if (!opened) return
    page.value = 1
    void Promise.all([loadOptions(), loadList()])
  },
  { immediate: true }
)
</script>

<template>
  <el-dialog v-model="visible" title="报销额度分级规则" width="1080px" top="6vh">
    <div class="rule-toolbar">
      <span class="rule-hint">
        按报销金额区间匹配审批流程模板；部门/项目留空表示不限，优先级越大越优先
      </span>
      <div class="rule-actions">
        <el-button @click="loadList">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增规则</el-button>
      </div>
    </div>

    <el-table v-loading="listLoading" :data="list" border max-height="420">
      <el-table-column prop="name" label="规则名称" width="160" show-overflow-tooltip />
      <el-table-column label="金额区间" width="170">
        <template #default="{ row }">
          {{ amountRange(row as AmountRuleVO) }}
        </template>
      </el-table-column>
      <el-table-column label="适用部门" width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ (row as AmountRuleVO).deptName || '不限' }}
        </template>
      </el-table-column>
      <el-table-column label="适用项目" width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ (row as AmountRuleVO).projectName || '不限' }}
        </template>
      </el-table-column>
      <el-table-column label="流程模板" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ (row as AmountRuleVO).templateName || (row as AmountRuleVO).templateCode }}
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="(row as AmountRuleVO).status === 'ENABLED' ? 'success' : 'info'">
            {{ statusText(row as AmountRuleVO) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ (row as AmountRuleVO).remark || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row as AmountRuleVO)">
            编辑
          </el-button>
          <el-button link type="danger" size="small" @click="removeRule(row as AmountRuleVO)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无额度分级规则" />
      </template>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="rule-pagination"
      @size-change="loadList"
      @current-change="loadList"
    />

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="formVisible"
    :title="editingId ? '编辑额度分级规则' : '新增额度分级规则'"
    width="560px"
    append-to-body
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="规则名称" prop="name">
        <el-input v-model="form.name" placeholder="如 5000 元以下报销审批" maxlength="64" />
      </el-form-item>
      <el-form-item label="最低金额" prop="minAmount">
        <el-input-number
          v-model="form.minAmount"
          :min="0"
          :precision="2"
          :step="100"
          placeholder="请输入最低金额"
          style="width: 100%"
          @change="onMinAmountChange"
        />
      </el-form-item>
      <el-form-item label="最高金额" prop="maxAmount">
        <el-input-number
          v-model="form.maxAmount"
          :min="0"
          :precision="2"
          :step="100"
          placeholder="留空表示无上限"
          style="width: 100%"
        />
        <div class="field-tip">留空表示无上限；填写时必须大于最低金额</div>
      </el-form-item>
      <el-form-item label="适用部门" prop="deptId">
        <el-select
          v-model="form.deptId"
          filterable
          clearable
          placeholder="不限（全部部门）"
          style="width: 100%"
        >
          <el-option v-for="item in deptOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="适用项目" prop="projectId">
        <el-select
          v-model="form.projectId"
          filterable
          clearable
          placeholder="不限（全部项目）"
          style="width: 100%"
        >
          <el-option v-for="item in projectOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="流程模板" prop="templateCode">
        <el-select
          v-model="form.templateCode"
          filterable
          placeholder="请选择启用的报销流程模板"
          style="width: 100%"
        >
          <el-option v-for="item in templateOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
        <div v-if="!templateOptions.length" class="field-tip">
          暂无启用的报销流程模板，请先在「流程模板」页签发布并启用
        </div>
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input-number v-model="form.priority" :min="0" :precision="0" :step="1" style="width: 100%" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" style="width: 100%">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="可选，如 部门经理审批后转财务复核"
          maxlength="255"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
/* 弹窗工具条：说明文字 + 靠右操作按钮 */
.rule-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-4);
}

.rule-hint {
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
}

.rule-actions {
  display: flex;
  gap: var(--ceams-space-2);
  align-items: center;
  margin-left: auto;
}

.rule-pagination {
  margin-top: var(--ceams-space-4);
  justify-content: flex-end;
}

/* 表单字段补充说明 */
.field-tip {
  width: 100%;
  margin-top: 4px;
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
}
</style>
