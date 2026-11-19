<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
  ElDrawer,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import type { FormInstance } from 'element-plus'
import { formatDateTime } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import type {
  DeptVO,
  EmployeeVO,
  HrPerformanceReviewStatus,
  HrPeriodType,
  PerformanceBatchResultVO,
  PerformanceItem,
  PerformanceReviewVO,
  PerformanceScoreRule,
  PerformanceTemplateSaveRequest,
  PerformanceTemplateVO,
  PositionVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import {
  hrPerformanceReviewStatusLabels,
  hrPeriodTypeLabels,
  hrTemplateStatusLabels,
  toOptions
} from '@/utils/labels'

defineOptions({ name: 'PerformancePanel' })

/** 分数 / 系数 / 权重均为后端 BigDecimal 字符串，展示时不做浮点运算。 */
const SCORE_PATTERN = /^\d+(\.\d{1,2})?$/

const canManageTemplate = computed(() => hasPermission('hr:performance:template'))
const canOperate = computed(() => hasPermission('hr:performance:operate'))

const periodTypeOptions = toOptions(hrPeriodTypeLabels)
const templateStatusOptions = toOptions(hrTemplateStatusLabels)
const reviewStatusOptions = toOptions(hrPerformanceReviewStatusLabels)

const employees = ref<EmployeeVO[]>([])
const positions = ref<PositionVO[]>([])
const depts = ref<{ value: string; label: string }[]>([])
const positionNameMap = computed(() => new Map(positions.value.map((item) => [item.id, item.name])))

function positionName(id?: string | null): string {
  if (!id) return '通用'
  return positionNameMap.value.get(id) ?? id
}

/** 后端分数字段为 BigDecimal，可能为 null；缺失时统一展示 -。 */
function scoreText(value?: string | null): string {
  return value === null || value === undefined || value === '' ? '-' : value
}

function reviewTagType(
  status: HrPerformanceReviewStatus
): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'DRAFT':
    case 'SELF_SUBMITTED':
      return 'warning'
    case 'LEADER_DONE':
      return 'info'
    default:
      return 'danger'
  }
}

function flattenDept(
  nodes: DeptVO[],
  depth = 0,
  result: { value: string; label: string }[] = []
): { value: string; label: string }[] {
  for (const node of nodes) {
    result.push({ value: node.id, label: `${'　'.repeat(depth)}${node.name}` })
    if (node.children?.length) flattenDept(node.children, depth + 1, result)
  }
  return result
}

async function loadReferences(): Promise<void> {
  const results = await Promise.allSettled([
    ceamsApi.hr.employeePage({ page: 1, size: 100 }).then((res) => {
      employees.value = res.records
    }),
    ceamsApi.hr.positionList().then((list) => {
      positions.value = list
    }),
    ceamsApi.iam.deptTree().then((tree) => {
      depts.value = flattenDept(tree)
    })
  ])
  // 下拉数据仅用于筛选 / 表单，加载失败不阻断主流程
  void results
}

// ---------- 模板 ----------
const templates = ref<PerformanceTemplateVO[]>([])
const templateLoading = ref(false)

async function loadTemplates(): Promise<void> {
  templateLoading.value = true
  try {
    templates.value = await ceamsApi.hr.performanceTemplateList()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载绩效模板失败'))
  } finally {
    templateLoading.value = false
  }
}

interface TemplateFormItem {
  name: string
  weight: string
  minScore: string
  maxScore: string
  description: string
}

interface TemplateFormRule {
  minScore: string
  maxScore: string
  coefficient: string
}

const templateDrawerVisible = ref(false)
const templateEditingId = ref<string | null>(null)
const templateForm = reactive({
  code: '',
  name: '',
  positionId: '',
  periodType: 'MONTH' as HrPeriodType,
  status: 'ENABLED',
  items: [] as TemplateFormItem[],
  scoreRules: [] as TemplateFormRule[]
})

/** 权重合计（仅用于校验，不参与落库计算）。 */
const weightSum = computed(() =>
  templateForm.items.reduce((sum, item) => sum + (Number(item.weight) || 0), 0)
)
const weightValid = computed(
  () => templateForm.items.length > 0 && Math.abs(weightSum.value - 100) < 1e-6
)

/** 系数区间问题描述：空串表示校验通过。 */
const scoreRuleIssue = computed<string>(() => {
  if (templateForm.scoreRules.length === 0) return '请至少配置一条系数区间'
  if (
    templateForm.scoreRules.some(
      (rule) => !rule.minScore.trim() || !rule.maxScore.trim() || !rule.coefficient.trim()
    )
  ) {
    return '请完整填写系数区间的最低分、最高分与系数'
  }
  const parsed = templateForm.scoreRules.map((rule) => ({
    min: Number(rule.minScore),
    max: Number(rule.maxScore)
  }))
  for (const rule of parsed) {
    if (!Number.isFinite(rule.min) || !Number.isFinite(rule.max)) {
      return '系数区间的分数必须填写为数字'
    }
    if (rule.min < 0 || rule.max > 100) return '系数区间的分数范围应在 0 ~ 100'
    if (rule.min > rule.max) return '系数区间的起始分不能大于结束分'
  }
  const sorted = [...parsed].sort((a, b) => a.min - b.min)
  if (sorted[0].min !== 0) return '系数区间需从 0 分开始'
  if (sorted[sorted.length - 1].max !== 100) return '系数区间需覆盖到 100 分'
  for (let i = 1; i < sorted.length; i += 1) {
    if (sorted[i].min < sorted[i - 1].max) return '系数区间存在重叠，请调整最低分 / 最高分'
    if (sorted[i].min > sorted[i - 1].max) return '系数区间存在空档，请保持首尾相接'
  }
  return ''
})

const templateSubmitDisabled = computed(
  () => !weightValid.value || scoreRuleIssue.value !== ''
)

function resetTemplateForm(): void {
  Object.assign(templateForm, {
    code: '',
    name: '',
    positionId: '',
    periodType: 'MONTH',
    status: 'ENABLED',
    items: [
      { name: '', weight: '100', minScore: '0', maxScore: '100', description: '' }
    ] as TemplateFormItem[],
    scoreRules: [{ minScore: '0', maxScore: '100', coefficient: '1' }] as TemplateFormRule[]
  })
}

function openTemplateCreate(): void {
  templateEditingId.value = null
  resetTemplateForm()
  templateDrawerVisible.value = true
}

function openTemplateEdit(row: PerformanceTemplateVO): void {
  templateEditingId.value = row.id
  Object.assign(templateForm, {
    code: row.code,
    name: row.name,
    positionId: row.positionId ?? '',
    periodType: row.periodType,
    status: row.status ?? 'ENABLED',
    items: (row.items ?? []).map<TemplateFormItem>((item) => ({
      name: item.name,
      weight: item.weight,
      minScore: item.minScore ?? '',
      maxScore: item.maxScore ?? '',
      description: item.description ?? ''
    })),
    scoreRules: (row.scoreRules ?? []).map<TemplateFormRule>((rule) => ({
      minScore: rule.minScore,
      maxScore: rule.maxScore,
      coefficient: rule.coefficient
    }))
  })
  if (templateForm.items.length === 0) {
    templateForm.items.push({ name: '', weight: '', minScore: '', maxScore: '', description: '' })
  }
  if (templateForm.scoreRules.length === 0) {
    templateForm.scoreRules.push({ minScore: '0', maxScore: '100', coefficient: '1' })
  }
  templateDrawerVisible.value = true
}

function addItem(): void {
  templateForm.items.push({ name: '', weight: '', minScore: '', maxScore: '', description: '' })
}

function removeItem(index: number): void {
  templateForm.items.splice(index, 1)
}

function addScoreRule(): void {
  templateForm.scoreRules.push({ minScore: '', maxScore: '', coefficient: '' })
}

function removeScoreRule(index: number): void {
  templateForm.scoreRules.splice(index, 1)
}

async function submitTemplate(): Promise<void> {
  if (!templateForm.code.trim() || !templateForm.name.trim()) {
    ElMessage.warning('请填写模板编码与名称')
    return
  }
  if (templateForm.items.some((item) => !item.name.trim())) {
    ElMessage.warning('考评项名称不能为空')
    return
  }
  if (templateForm.items.some((item) => !item.weight.trim())) {
    ElMessage.warning('请填写考评项权重')
    return
  }
  if (
    templateForm.items.some((item) => {
      const value = Number(item.weight)
      return !Number.isFinite(value) || value < 0
    })
  ) {
    ElMessage.warning('考评项权重必须为非负数字')
    return
  }
  if (!weightValid.value) {
    ElMessage.warning(`考评项权重合计必须为 100，当前为 ${weightSum.value}`)
    return
  }
  if (scoreRuleIssue.value) {
    ElMessage.warning(scoreRuleIssue.value)
    return
  }

  const items: PerformanceItem[] = templateForm.items.map((item) => ({
    name: item.name.trim(),
    weight: item.weight.trim(),
    minScore: item.minScore.trim() || null,
    maxScore: item.maxScore.trim() || null,
    description: item.description.trim() || null
  }))
  const scoreRules: PerformanceScoreRule[] = templateForm.scoreRules.map((rule) => ({
    minScore: rule.minScore.trim(),
    maxScore: rule.maxScore.trim(),
    coefficient: rule.coefficient.trim()
  }))

  const payload: PerformanceTemplateSaveRequest = {
    code: templateForm.code.trim(),
    name: templateForm.name.trim(),
    positionId: templateForm.positionId || undefined,
    periodType: templateForm.periodType,
    items,
    scoreRules,
    status: templateForm.status
  }

  try {
    if (templateEditingId.value) {
      await ceamsApi.hr.performanceTemplateUpdate(templateEditingId.value, payload)
      ElMessage.success('模板更新成功')
    } else {
      await ceamsApi.hr.performanceTemplateCreate(payload)
      ElMessage.success('模板创建成功')
    }
    templateDrawerVisible.value = false
    await loadTemplates()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存绩效模板失败'))
  }
}

// ---------- 批量生成考评单 ----------
const batchForm = reactive({ templateId: '', period: '', deptId: '' })
const batchResult = ref<PerformanceBatchResultVO | null>(null)
const batchLoading = ref(false)

async function submitBatch(): Promise<void> {
  if (!batchForm.templateId) {
    ElMessage.warning('请选择绩效模板')
    return
  }
  if (!batchForm.period) {
    ElMessage.warning('请选择考核周期')
    return
  }
  batchLoading.value = true
  try {
    batchResult.value = await ceamsApi.hr.performanceReviewBatch({
      templateId: batchForm.templateId,
      period: batchForm.period,
      deptId: batchForm.deptId || undefined
    })
    ElMessage.success(
      `生成完成：共 ${batchResult.value.total} 人，新建 ${batchResult.value.created}，跳过 ${batchResult.value.skipped}`
    )
    await loadReviews()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '批量生成考评单失败'))
  } finally {
    batchLoading.value = false
  }
}

// ---------- 考评单 ----------
const reviewQuery = reactive<{
  templateId: string
  period: string
  status: HrPerformanceReviewStatus | ''
  employeeId: string
}>({ templateId: '', period: '', status: '', employeeId: '' })
const { page, size, total, resetPage } = usePage()
const reviews = ref<PerformanceReviewVO[]>([])
const reviewLoading = ref(false)

async function loadReviews(): Promise<void> {
  reviewLoading.value = true
  try {
    const res = await ceamsApi.hr.performanceReviewPage({
      page: page.value,
      size: size.value,
      templateId: reviewQuery.templateId || undefined,
      period: reviewQuery.period || undefined,
      status: reviewQuery.status || undefined,
      employeeId: reviewQuery.employeeId || undefined
    })
    reviews.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载考评单失败'))
  } finally {
    reviewLoading.value = false
  }
}

function onSearchReviews(): void {
  resetPage()
  void loadReviews()
}

const reviewDrawerVisible = ref(false)
const reviewFormRef = ref<FormInstance>()
const reviewTarget = ref<PerformanceReviewVO | null>(null)
const reviewForm = reactive({ finalScore: '', comment: '' })

function openReview(row: PerformanceReviewVO): void {
  reviewTarget.value = row
  Object.assign(reviewForm, {
    finalScore: row.finalScore ?? row.leaderScore ?? row.selfScore ?? '',
    comment: ''
  })
  reviewDrawerVisible.value = true
}

async function submitReview(): Promise<void> {
  if (!reviewTarget.value) return
  const score = reviewForm.finalScore.trim()
  if (!SCORE_PATTERN.test(score) || Number(score) < 0 || Number(score) > 100) {
    ElMessage.warning('定稿分数需为 0 ~ 100 的数字')
    return
  }
  try {
    await ceamsApi.hr.performanceReview(reviewTarget.value.id, {
      finalScore: score,
      comment: reviewForm.comment || undefined
    })
    ElMessage.success('复核定稿成功')
    reviewDrawerVisible.value = false
    await loadReviews()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '复核定稿失败'))
  }
}

onMounted(() => {
  void loadReferences()
  void loadTemplates()
  void loadReviews()
})
</script>

<template>
  <div>
    <!-- 绩效模板 -->
    <div class="section">
      <div class="section-title">绩效模板</div>
      <div class="toolbar">
        <el-button v-if="canManageTemplate" @click="openTemplateCreate">新建模板</el-button>
      </div>

      <el-table v-loading="templateLoading" :data="templates" border>
        <el-table-column prop="code" label="模板编码" width="150" />
        <el-table-column prop="name" label="模板名称" min-width="160" />
        <el-table-column label="考核周期" width="120">
          <template #default="{ row }">{{ hrPeriodTypeLabels[(row as PerformanceTemplateVO).periodType] }}</template>
        </el-table-column>
        <el-table-column label="适用岗位" width="140">
          <template #default="{ row }">{{ positionName((row as PerformanceTemplateVO).positionId) }}</template>
        </el-table-column>
        <el-table-column label="考评项" width="90" align="center">
          <template #default="{ row }">{{ (row as PerformanceTemplateVO).items?.length ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="权重合计" width="100" align="center">
          <template #default="{ row }">{{ (row as PerformanceTemplateVO).totalWeight ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="(row as PerformanceTemplateVO).status === 'ENABLED' ? 'success' : 'info'">
              {{ hrTemplateStatusLabels[(row as PerformanceTemplateVO).status ?? ''] ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime((row as PerformanceTemplateVO).updateTime) }}</template>
        </el-table-column>
        <el-table-column v-if="canManageTemplate" label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openTemplateEdit(row as PerformanceTemplateVO)">
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 批量生成考评单 -->
    <div class="section">
      <div class="section-title">批量生成考评单</div>
      <div v-if="canOperate" class="toolbar">
        <el-select v-model="batchForm.templateId" placeholder="绩效模板" filterable style="width: 220px">
          <el-option
            v-for="item in templates"
            :key="item.id"
            :label="`${item.name}（${item.code}）`"
            :value="item.id"
          />
        </el-select>
        <el-date-picker
          v-model="batchForm.period"
          type="month"
          value-format="YYYY-MM"
          placeholder="考核周期（yyyy-MM）"
          style="width: 180px"
        />
        <el-select v-model="batchForm.deptId" placeholder="部门（默认全员）" clearable style="width: 190px">
          <el-option v-for="opt in depts" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button type="primary" :loading="batchLoading" @click="submitBatch">生成考评单</el-button>
      </div>
      <el-alert
        v-if="batchResult"
        type="success"
        show-icon
        :closable="false"
        class="result-alert"
        :title="`生成完成：共 ${batchResult.total} 人，新建 ${batchResult.created}，跳过 ${batchResult.skipped}`"
      />
      <el-alert
        v-else-if="!canOperate"
        type="info"
        show-icon
        :closable="false"
        title="当前账号无批量生成考评单权限"
      />
    </div>

    <!-- 考评单 -->
    <div class="section">
      <div class="section-title">绩效考评单</div>
      <div class="toolbar">
        <el-select v-model="reviewQuery.templateId" placeholder="模板" clearable style="width: 200px">
          <el-option
            v-for="item in templates"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-date-picker
          v-model="reviewQuery.period"
          type="month"
          value-format="YYYY-MM"
          placeholder="周期"
          style="width: 160px"
        />
        <el-select v-model="reviewQuery.status" placeholder="状态" clearable style="width: 150px">
          <el-option
            v-for="opt in reviewStatusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-select v-model="reviewQuery.employeeId" placeholder="员工" clearable filterable style="width: 190px">
          <el-option
            v-for="item in employees"
            :key="item.id"
            :label="`${item.name}（${item.employeeNo}）`"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" @click="onSearchReviews">查询</el-button>
      </div>

      <el-table v-loading="reviewLoading" :data="reviews" border>
        <el-table-column prop="employeeName" label="员工" width="110" />
        <el-table-column prop="templateName" label="模板" min-width="140" />
        <el-table-column prop="period" label="周期" width="100" />
        <el-table-column label="自评分" width="90" align="right">
          <template #default="{ row }">{{ scoreText((row as PerformanceReviewVO).selfScore) }}</template>
        </el-table-column>
        <el-table-column label="上级分" width="90" align="right">
          <template #default="{ row }">{{ scoreText((row as PerformanceReviewVO).leaderScore) }}</template>
        </el-table-column>
        <el-table-column label="最终分" width="90" align="right">
          <template #default="{ row }">{{ scoreText((row as PerformanceReviewVO).finalScore) }}</template>
        </el-table-column>
        <el-table-column label="系数" width="90" align="right">
          <template #default="{ row }">{{ scoreText((row as PerformanceReviewVO).coefficient) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="reviewTagType((row as PerformanceReviewVO).status)">
              {{ hrPerformanceReviewStatusLabels[(row as PerformanceReviewVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canOperate && (row as PerformanceReviewVO).status === 'LEADER_DONE'"
              link
              type="primary"
              size="small"
              @click="openReview(row as PerformanceReviewVO)"
            >
              HR 复核
            </el-button>
            <span v-else>-</span>
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
        @size-change="onSearchReviews"
        @current-change="loadReviews"
      />
    </div>

    <!-- 模板新建 / 编辑 -->
    <el-drawer
      v-model="templateDrawerVisible"
      :title="templateEditingId ? '编辑绩效模板' : '新建绩效模板'"
      size="760px"
    >
      <el-form :model="templateForm" label-width="100px">
        <el-form-item label="模板编码" required>
          <el-input v-model="templateForm.code" placeholder="唯一编码，如 MONTH_KPI" maxlength="64" />
        </el-form-item>
        <el-form-item label="模板名称" required>
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" maxlength="128" />
        </el-form-item>
        <el-form-item label="适用岗位">
          <el-select v-model="templateForm.positionId" placeholder="不选表示通用" clearable style="width: 100%">
            <el-option v-for="item in positions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考核周期">
          <el-select v-model="templateForm.periodType" style="width: 100%">
            <el-option
              v-for="opt in periodTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模板状态">
          <el-select v-model="templateForm.status" style="width: 100%">
            <el-option
              v-for="opt in templateStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <div class="sub-title">
          考评项
          <span class="sub-hint">
            权重合计：
            <span :class="weightValid ? 'ok' : 'error'">{{ weightSum }}</span>
            （必须为 100）
          </span>
        </div>
        <el-alert
          v-if="!weightValid"
          type="error"
          show-icon
          :closable="false"
          title="考评项权重合计必须等于 100，否则无法提交"
          class="inline-alert"
        />
        <div v-for="(item, index) in templateForm.items" :key="`item-${index}`" class="row-block">
          <el-input v-model="item.name" placeholder="考评项名称" style="width: 180px" maxlength="64" />
          <el-input v-model="item.weight" placeholder="权重" style="width: 90px" />
          <el-input v-model="item.minScore" placeholder="最低分" style="width: 90px" />
          <el-input v-model="item.maxScore" placeholder="最高分" style="width: 90px" />
          <el-input
            v-model="item.description"
            placeholder="说明（可选）"
            style="width: 180px"
            maxlength="255"
          />
          <el-button link type="danger" size="small" @click="removeItem(index)">删除</el-button>
        </div>
        <el-button size="small" class="add-button" @click="addItem">新增考评项</el-button>

        <div class="sub-title">系数区间</div>
        <el-alert
          v-if="scoreRuleIssue"
          type="error"
          show-icon
          :closable="false"
          :title="scoreRuleIssue"
          class="inline-alert"
        />
        <div class="rule-head">
          <span>最低分</span>
          <span>最高分</span>
          <span>系数</span>
          <span>操作</span>
        </div>
        <div v-for="(rule, index) in templateForm.scoreRules" :key="`rule-${index}`" class="row-block">
          <el-input v-model="rule.minScore" placeholder="最低分" style="width: 90px" />
          <el-input v-model="rule.maxScore" placeholder="最高分" style="width: 90px" />
          <el-input v-model="rule.coefficient" placeholder="系数" style="width: 90px" />
          <el-button link type="danger" size="small" @click="removeScoreRule(index)">删除</el-button>
        </div>
        <el-button size="small" class="add-button" @click="addScoreRule">新增系数区间</el-button>
      </el-form>
      <template #footer>
        <el-button @click="templateDrawerVisible = false">取消</el-button>
        <el-button type="primary" :disabled="templateSubmitDisabled" @click="submitTemplate">
          保存模板
        </el-button>
      </template>
    </el-drawer>

    <!-- HR 复核定稿 -->
    <el-drawer v-model="reviewDrawerVisible" title="HR 复核定稿" size="460px">
      <div v-if="reviewTarget" class="target-tip">
        员工：{{ reviewTarget.employeeName ?? reviewTarget.employeeId }} · 周期 {{ reviewTarget.period }}
        · 上级评分 {{ scoreText(reviewTarget.leaderScore) }}
      </div>
      <el-form ref="reviewFormRef" :model="reviewForm" label-width="90px">
        <el-form-item label="定稿分数" required>
          <el-input v-model="reviewForm.finalScore" placeholder="0 ~ 100" />
        </el-form-item>
        <el-form-item label="复核意见">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="3"
            placeholder="复核意见（可选，追加到上级评语后）"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDrawerVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认定稿</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.section {
  margin-bottom: 24px;
}
.section-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.result-alert {
  margin-bottom: 12px;
}
.sub-title {
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}
.sub-hint {
  margin-left: 12px;
  font-size: 12px;
  font-weight: 400;
  color: #6b7280;
}
.ok {
  color: #16a34a;
  font-weight: 600;
}
.error {
  color: #dc2626;
  font-weight: 600;
}
.inline-alert {
  margin-bottom: 8px;
}
.row-block {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.add-button {
  margin-bottom: 8px;
}
.rule-head {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #6b7280;
}
.rule-head span {
  width: 90px;
}
.rule-head span:last-child {
  width: auto;
}
.target-tip {
  margin-bottom: 12px;
  color: #4b5563;
  font-size: 13px;
}
</style>
