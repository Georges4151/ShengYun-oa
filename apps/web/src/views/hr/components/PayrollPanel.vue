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
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { formatMoney } from '@ceams/utils'
import { apiBaseUrl, ceamsApi, refreshAccessToken } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type {
  HrPayrollStatus,
  PayrollCalcResultVO,
  SalaryCalcVO,
  SalarySlipPublishResultVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import { hrPayrollStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'PayrollPanel' })

/** 金额（两位小数）/ 绩效系数（四位小数）字符串校验，全程不做浮点运算。 */
const MONEY_PATTERN = /^\d+(\.\d{1,2})?$/
const COEFFICIENT_PATTERN = /^\d+(\.\d{1,4})?$/

const canConfirm = computed(() => hasPermission('hr:salary:confirm'))
const canExport = computed(() => hasPermission('hr:salary-slip:export'))
const statusOptions = toOptions(hrPayrollStatusLabels)

const auth = useAuthStore()

function pad(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

function currentMonth(): string {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}`
}

function statusTagType(status: HrPayrollStatus): 'success' | 'warning' | 'info' {
  switch (status) {
    case 'CONFIRMED':
      return 'success'
    case 'LOCKED':
      return 'info'
    default:
      return 'warning'
  }
}

// ---------- 核算明细 ----------
const month = ref(currentMonth())
const query = reactive<{ status: HrPayrollStatus | '' }>({ status: '' })
const { page, size, total, resetPage } = usePage()
const list = ref<SalaryCalcVO[]>([])
const loading = ref(false)
const calcResult = ref<PayrollCalcResultVO | null>(null)
const publishResult = ref<SalarySlipPublishResultVO | null>(null)
const calculating = ref(false)
const confirming = ref(false)
const publishing = ref(false)
const exporting = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.hr.payrollCalcPage({
      page: page.value,
      size: size.value,
      calcMonth: month.value || undefined,
      status: query.status || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载工资核算明细失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

function onMonthChange(): void {
  calcResult.value = null
  publishResult.value = null
  onSearch()
}

// ---------- 按月核算 ----------
async function runCalc(): Promise<void> {
  if (!month.value) {
    ElMessage.warning('请选择核算月')
    return
  }
  calculating.value = true
  try {
    calcResult.value = await ceamsApi.hr.payrollCalc({ calcMonth: month.value })
    publishResult.value = null
    ElMessage.success('核算完成')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '工资核算失败'))
  } finally {
    calculating.value = false
  }
}

// ---------- 批量确认 ----------
async function confirmAll(): Promise<void> {
  if (!month.value) {
    ElMessage.warning('请选择核算月')
    return
  }
  const confirmed = await ElMessageBox.confirm(
    `确认后将锁定 ${month.value} 的全部草稿核算记录，且不可直接修改，是否继续？`,
    '批量确认',
    { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
  ).catch(() => false)
  if (!confirmed) return

  confirming.value = true
  try {
    const count = await ceamsApi.hr.payrollConfirm({ calcMonth: month.value })
    ElMessage.success(`已确认 ${count} 条核算记录`)
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '批量确认失败'))
  } finally {
    confirming.value = false
  }
}

// ---------- 发布工资条 ----------
async function publishSlips(): Promise<void> {
  if (!month.value) {
    ElMessage.warning('请选择核算月')
    return
  }
  publishing.value = true
  try {
    publishResult.value = await ceamsApi.hr.salarySlipPublish({ calcMonth: month.value })
    calcResult.value = null
    ElMessage.success(
      `工资条发布完成：共 ${publishResult.value.total} 条，发布 ${publishResult.value.published}，跳过 ${publishResult.value.skipped}`
    )
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '发布工资条失败'))
  } finally {
    publishing.value = false
  }
}

// ---------- 导出 xlsx ----------
/** 二进制流无法经过 JSON 统一解包，这里按 ai.ts 的既有做法用 fetch 自建鉴权请求。 */
function authHeaders(token?: string | null): Record<string, string> {
  const value = token ?? auth.token
  return value ? { Authorization: `Bearer ${value}` } : {}
}

/** 提取后端 JSON 错误消息，非 JSON 时返回原文片段。 */
async function readError(res: Response): Promise<string> {
  try {
    const text = await res.text()
    try {
      const parsed = JSON.parse(text) as { message?: unknown }
      if (typeof parsed.message === 'string' && parsed.message) return parsed.message
    } catch {
      // 非 JSON：按原文片段返回
    }
    return text ? text.slice(0, 200) : `导出失败（HTTP ${res.status}）`
  } catch {
    return `导出失败（HTTP ${res.status}）`
  }
}

async function exportSlips(): Promise<void> {
  if (!month.value) {
    ElMessage.warning('请选择核算月')
    return
  }
  exporting.value = true
  const url = `${apiBaseUrl.replace(/\/+$/, '')}${ceamsApi.hr.salarySlipExportPath(month.value)}`
  try {
    let res = await fetch(url, { headers: authHeaders() })
    if (res.status === 401) {
      const token = await refreshAccessToken()
      res = await fetch(url, { headers: authHeaders(token) })
    }
    if (!res.ok) {
      throw new Error(await readError(res))
    }
    const blob = await res.blob()
    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `salary-slip-${month.value}.xlsx`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(objectUrl)
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '导出工资条失败'))
  } finally {
    exporting.value = false
  }
}

// ---------- 手工调整（仅 DRAFT） ----------
const adjustVisible = ref(false)
const adjustTarget = ref<SalaryCalcVO | null>(null)
const adjustForm = reactive({
  allowance: '',
  attendanceDeduction: '',
  performanceCoefficient: '',
  socialInsurance: '',
  housingFund: '',
  tax: ''
})

function openAdjust(row: SalaryCalcVO): void {
  if (row.status !== 'DRAFT') return
  adjustTarget.value = row
  Object.assign(adjustForm, {
    allowance: row.allowance ?? '',
    attendanceDeduction: row.attendanceDeduction ?? '',
    performanceCoefficient: row.performanceCoefficient ?? '',
    socialInsurance: row.socialInsurance ?? '',
    housingFund: row.housingFund ?? '',
    tax: row.tax ?? ''
  })
  adjustVisible.value = true
}

function validateAdjust(): string {
  const moneyFields: Array<[string, string]> = [
    [adjustForm.allowance, '补贴'],
    [adjustForm.attendanceDeduction, '考勤扣款'],
    [adjustForm.socialInsurance, '社保'],
    [adjustForm.housingFund, '公积金'],
    [adjustForm.tax, '个人所得税']
  ]
  for (const [value, label] of moneyFields) {
    const text = value.trim()
    if (text && !MONEY_PATTERN.test(text)) return `${label}请输入非负金额，最多两位小数`
  }
  const coefficient = adjustForm.performanceCoefficient.trim()
  if (coefficient && !COEFFICIENT_PATTERN.test(coefficient)) {
    return '绩效系数请输入非负数字，最多四位小数'
  }
  return ''
}

async function submitAdjust(): Promise<void> {
  if (!adjustTarget.value) return
  const issue = validateAdjust()
  if (issue) {
    ElMessage.warning(issue)
    return
  }
  try {
    await ceamsApi.hr.payrollCalcAdjust(adjustTarget.value.id, {
      allowance: adjustForm.allowance.trim() || undefined,
      attendanceDeduction: adjustForm.attendanceDeduction.trim() || undefined,
      performanceCoefficient: adjustForm.performanceCoefficient.trim() || undefined,
      socialInsurance: adjustForm.socialInsurance.trim() || undefined,
      housingFund: adjustForm.housingFund.trim() || undefined,
      tax: adjustForm.tax.trim() || undefined
    })
    ElMessage.success('调整成功，应发与实发已重新计算')
    adjustVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '调整工资核算失败'))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-date-picker
        v-model="month"
        type="month"
        value-format="YYYY-MM"
        placeholder="核算月"
        style="width: 160px"
        @change="onMonthChange"
      />
      <el-select v-model="query.status" placeholder="核算状态" clearable style="width: 140px">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <template v-if="canConfirm">
        <el-button type="primary" :loading="calculating" @click="runCalc">核算</el-button>
        <el-button :loading="confirming" @click="confirmAll">批量确认</el-button>
        <el-button :loading="publishing" @click="publishSlips">发布工资条</el-button>
      </template>
      <el-button v-if="canExport" :loading="exporting" @click="exportSlips">导出 xlsx</el-button>
    </div>

    <!-- 核算结果 -->
    <el-alert
      v-if="calcResult"
      type="success"
      show-icon
      :closable="false"
      class="result-alert"
      :title="`核算完成：总数 ${calcResult.total}，新建 ${calcResult.created}，重算 ${calcResult.recalculated}，跳过 ${calcResult.skipped}，锁定 ${calcResult.locked}`"
    />
    <el-alert
      v-for="(warning, index) in calcResult?.warnings ?? []"
      :key="`calc-warning-${index}`"
      type="warning"
      show-icon
      :closable="false"
      class="result-alert"
      :title="warning"
    />

    <!-- 工资条发布结果 -->
    <el-alert
      v-if="publishResult"
      type="success"
      show-icon
      :closable="false"
      class="result-alert"
      :title="`工资条发布完成：共 ${publishResult.total} 条，发布 ${publishResult.published}，跳过 ${publishResult.skipped}`"
    />
    <el-alert
      v-for="(warning, index) in publishResult?.warnings ?? []"
      :key="`publish-warning-${index}`"
      type="warning"
      show-icon
      :closable="false"
      class="result-alert"
      :title="warning"
    />

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="employeeName" label="员工" width="110" fixed="left">
        <template #default="{ row }">
          {{ (row as SalaryCalcVO).employeeName }}（{{ (row as SalaryCalcVO).employeeNo }}）
        </template>
      </el-table-column>
      <el-table-column prop="calcMonth" label="核算月" width="100" />
      <el-table-column label="考勤" width="130">
        <template #default="{ row }">
          出勤 {{ (row as SalaryCalcVO).attendanceDays }} / 缺勤 {{ (row as SalaryCalcVO).absentDays }} /
          迟到 {{ (row as SalaryCalcVO).lateCount ?? 0 }}
        </template>
      </el-table-column>
      <el-table-column label="基本工资" width="110" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).baseSalary) }}</template>
      </el-table-column>
      <el-table-column label="岗位工资" width="110" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).postSalary) }}</template>
      </el-table-column>
      <el-table-column label="绩效系数" width="95" align="right">
        <template #default="{ row }">{{ (row as SalaryCalcVO).performanceCoefficient }}</template>
      </el-table-column>
      <el-table-column label="绩效工资" width="110" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).performanceAmount) }}</template>
      </el-table-column>
      <el-table-column label="补贴" width="100" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).allowance) }}</template>
      </el-table-column>
      <el-table-column label="考勤扣款" width="110" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).attendanceDeduction) }}</template>
      </el-table-column>
      <el-table-column label="社保" width="100" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).socialInsurance) }}</template>
      </el-table-column>
      <el-table-column label="公积金" width="100" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).housingFund) }}</template>
      </el-table-column>
      <el-table-column label="个税" width="100" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).tax) }}</template>
      </el-table-column>
      <el-table-column label="应发" width="110" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).grossPay) }}</template>
      </el-table-column>
      <el-table-column label="实发" width="110" align="right">
        <template #default="{ row }">{{ formatMoney((row as SalaryCalcVO).netPay) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType((row as SalaryCalcVO).status)">
            {{ hrPayrollStatusLabels[(row as SalaryCalcVO).status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canConfirm" label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="(row as SalaryCalcVO).status === 'DRAFT'"
            link
            type="primary"
            size="small"
            @click="openAdjust(row as SalaryCalcVO)"
          >
            调整
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
      @size-change="onSearch"
      @current-change="load"
    />

    <!-- 手工调整 -->
    <el-drawer v-model="adjustVisible" title="调整工资核算" size="480px">
      <div v-if="adjustTarget" class="target-tip">
        员工：{{ adjustTarget.employeeName }}（{{ adjustTarget.employeeNo }}） · 核算月
        {{ adjustTarget.calcMonth }}
      </div>
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="补贴">
          <el-input v-model="adjustForm.allowance" placeholder="补贴（元）" />
        </el-form-item>
        <el-form-item label="考勤扣款">
          <el-input v-model="adjustForm.attendanceDeduction" placeholder="考勤扣款（元）" />
        </el-form-item>
        <el-form-item label="绩效系数">
          <el-input v-model="adjustForm.performanceCoefficient" placeholder="绩效系数（最多四位小数）" />
        </el-form-item>
        <el-form-item label="社保">
          <el-input v-model="adjustForm.socialInsurance" placeholder="社保（元）" />
        </el-form-item>
        <el-form-item label="公积金">
          <el-input v-model="adjustForm.housingFund" placeholder="公积金（元）" />
        </el-form-item>
        <el-form-item label="个人所得税">
          <el-input v-model="adjustForm.tax" placeholder="个人所得税（元）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">保存调整</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.result-alert {
  margin-bottom: 8px;
}
.target-tip {
  margin-bottom: 12px;
  color: #4b5563;
  font-size: 13px;
}
</style>
