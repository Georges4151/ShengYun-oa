<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
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
import type { FormInstance, FormRules } from 'element-plus'
import { formatDateTime, formatMoney } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import type {
  EmployeeVO,
  HrSalaryChangeStatus,
  HrSalaryChangeType,
  HrSalaryStatus,
  SalaryChangeApplyRequest,
  SalaryChangeVO,
  SalaryCreateRequest,
  SalaryVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import {
  hrSalaryChangeStatusLabels,
  hrSalaryChangeTypeLabels,
  hrSalaryStatusLabels,
  toOptions
} from '@/utils/labels'

defineOptions({ name: 'SalaryList' })

/** 金额字符串校验：非负、最多两位小数（金额全程以字符串交互，不做浮点运算）。 */
const MONEY_PATTERN = /^\d+(\.\d{1,2})?$/

function moneyValidator(
  _rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void {
  const text = String(value ?? '').trim()
  if (!text) {
    callback()
    return
  }
  if (!MONEY_PATTERN.test(text)) {
    callback(new Error('请输入非负金额，最多两位小数'))
    return
  }
  callback()
}

const salaryStatusOptions = toOptions(hrSalaryStatusLabels)
const changeTypeOptions = toOptions(hrSalaryChangeTypeLabels)
const changeStatusOptions = toOptions(hrSalaryChangeStatusLabels)

const canChange = computed(() => hasPermission('hr:salary:change'))

const employees = ref<EmployeeVO[]>([])
const employeeNameMap = computed(
  () => new Map(employees.value.map((item) => [item.id, item.name]))
)

function employeeLabel(row: SalaryVO | SalaryChangeVO): string {
  const name = row.employeeName || employeeNameMap.value.get(row.employeeId)
  return name ? `${name}（${row.employeeNo}）` : row.employeeId
}

function salaryTagType(status: HrSalaryStatus): 'success' | 'info' {
  return status === 'ACTIVE' ? 'success' : 'info'
}

function changeTagType(
  status: HrSalaryChangeStatus
): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'APPROVING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

async function loadEmployees(): Promise<void> {
  try {
    const res = await ceamsApi.hr.employeePage({ page: 1, size: 100 })
    employees.value = res.records
  } catch {
    // 员工下拉加载失败不阻断薪资台账
  }
}

// ---------- 薪资台账 ----------
const query = reactive<{ employeeId: string; status: HrSalaryStatus | '' }>({
  employeeId: '',
  status: ''
})
const { page, size, total, resetPage } = usePage()
const list = ref<SalaryVO[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.hr.salaryPage({
      page: page.value,
      size: size.value,
      employeeId: query.employeeId || undefined,
      status: query.status || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载薪资台账失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

// ---------- 新增薪资档案 ----------
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  employeeId: '',
  effectiveDate: '',
  baseSalary: '',
  postSalary: '',
  performanceBase: '',
  allowance: '',
  remark: ''
})

const createRules: FormRules<typeof createForm> = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  baseSalary: [
    { required: true, message: '请输入基本工资', trigger: 'blur' },
    { validator: moneyValidator, trigger: 'blur' }
  ],
  postSalary: [{ validator: moneyValidator, trigger: 'blur' }],
  performanceBase: [{ validator: moneyValidator, trigger: 'blur' }],
  allowance: [{ validator: moneyValidator, trigger: 'blur' }]
}

function openCreate(): void {
  Object.assign(createForm, {
    employeeId: '',
    effectiveDate: '',
    baseSalary: '',
    postSalary: '',
    performanceBase: '',
    allowance: '',
    remark: ''
  })
  createVisible.value = true
}

async function submitCreate(): Promise<void> {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: SalaryCreateRequest = {
    employeeId: createForm.employeeId,
    effectiveDate: createForm.effectiveDate,
    baseSalary: createForm.baseSalary.trim(),
    postSalary: createForm.postSalary.trim() || undefined,
    performanceBase: createForm.performanceBase.trim() || undefined,
    allowance: createForm.allowance.trim() || undefined,
    remark: createForm.remark || undefined
  }

  try {
    await ceamsApi.hr.salaryCreate(payload)
    ElMessage.success('薪资档案创建成功')
    createVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '创建薪资档案失败'))
  }
}

// ---------- 发起薪资变动 ----------
const changeVisible = ref(false)
const changeFormRef = ref<FormInstance>()
const changeSource = ref<SalaryVO | null>(null)
const changeForm = reactive({
  employeeId: '',
  changeType: 'ADJUST' as HrSalaryChangeType,
  effectiveDate: '',
  baseSalary: '',
  postSalary: '',
  performanceBase: '',
  allowance: '',
  reason: ''
})

const changeRules: FormRules<typeof changeForm> = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  changeType: [{ required: true, message: '请选择变动类型', trigger: 'change' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  baseSalary: [{ validator: moneyValidator, trigger: 'blur' }],
  postSalary: [{ validator: moneyValidator, trigger: 'blur' }],
  performanceBase: [{ validator: moneyValidator, trigger: 'blur' }],
  allowance: [{ validator: moneyValidator, trigger: 'blur' }]
}

function openChange(row?: SalaryVO): void {
  changeSource.value = row ?? null
  Object.assign(changeForm, {
    employeeId: row?.employeeId ?? '',
    changeType: 'ADJUST',
    effectiveDate: '',
    // 默认带出当前生效金额，便于 HR 只改动其中几项
    baseSalary: row?.baseSalary ?? '',
    postSalary: row?.postSalary ?? '',
    performanceBase: row?.performanceBase ?? '',
    allowance: row?.allowance ?? '',
    reason: ''
  })
  changeVisible.value = true
}

async function submitChange(): Promise<void> {
  if (!changeFormRef.value) return
  const valid = await changeFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: SalaryChangeApplyRequest = {
    employeeId: changeForm.employeeId,
    changeType: changeForm.changeType,
    effectiveDate: changeForm.effectiveDate,
    baseSalary: changeForm.baseSalary.trim() || undefined,
    postSalary: changeForm.postSalary.trim() || undefined,
    performanceBase: changeForm.performanceBase.trim() || undefined,
    allowance: changeForm.allowance.trim() || undefined,
    reason: changeForm.reason || undefined
  }

  try {
    await ceamsApi.hr.salaryChangeApply(payload)
    ElMessage.success('已发起审批，请在待办中心跟踪流程进度')
    changeVisible.value = false
    await Promise.all([load(), loadChanges()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '发起薪资变动失败'))
  }
}

// ---------- 薪资变动记录 ----------
const changeQuery = reactive<{
  employeeId: string
  changeType: HrSalaryChangeType | ''
  status: HrSalaryChangeStatus | ''
}>({ employeeId: '', changeType: '', status: '' })
const {
  page: changePage,
  size: changeSize,
  total: changeTotal,
  resetPage: resetChangePage
} = usePage()
const changeList = ref<SalaryChangeVO[]>([])
const changeLoading = ref(false)

async function loadChanges(): Promise<void> {
  changeLoading.value = true
  try {
    const res = await ceamsApi.hr.salaryChangePage({
      page: changePage.value,
      size: changeSize.value,
      employeeId: changeQuery.employeeId || undefined,
      changeType: changeQuery.changeType || undefined,
      status: changeQuery.status || undefined
    })
    changeList.value = res.records
    changeTotal.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载薪资变动记录失败'))
  } finally {
    changeLoading.value = false
  }
}

function onSearchChanges(): void {
  resetChangePage()
  void loadChanges()
}

onMounted(() => {
  void loadEmployees()
  void load()
  void loadChanges()
})
</script>

<template>
  <div>
    <!-- 薪资台账 -->
    <div class="section">
      <div class="section-title">薪资台账</div>
      <div class="toolbar">
        <el-select
          v-model="query.employeeId"
          placeholder="员工"
          clearable
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="item in employees"
            :key="item.id"
            :label="`${item.name}（${item.employeeNo}）`"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="台账状态" clearable style="width: 140px">
          <el-option
            v-for="opt in salaryStatusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <template v-if="canChange">
          <el-button @click="openCreate">新增档案</el-button>
          <el-button @click="openChange()">发起变动</el-button>
        </template>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column label="员工" min-width="160">
          <template #default="{ row }">{{ employeeLabel(row as SalaryVO) }}</template>
        </el-table-column>
        <el-table-column prop="effectiveDate" label="生效日期" width="120" />
        <el-table-column label="基本工资" width="120" align="right">
          <template #default="{ row }">{{ formatMoney((row as SalaryVO).baseSalary) }}</template>
        </el-table-column>
        <el-table-column label="岗位工资" width="120" align="right">
          <template #default="{ row }">{{ formatMoney((row as SalaryVO).postSalary) }}</template>
        </el-table-column>
        <el-table-column label="绩效基数" width="120" align="right">
          <template #default="{ row }">{{ formatMoney((row as SalaryVO).performanceBase) }}</template>
        </el-table-column>
        <el-table-column label="固定补贴" width="120" align="right">
          <template #default="{ row }">{{ formatMoney((row as SalaryVO).allowance) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="salaryTagType((row as SalaryVO).status)">
              {{ hrSalaryStatusLabels[(row as SalaryVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column v-if="canChange" label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openChange(row as SalaryVO)">
              发起变动
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
    </div>

    <!-- 薪资变动记录 -->
    <div class="section">
      <div class="section-title">薪资变动记录</div>
      <div class="toolbar">
        <el-select
          v-model="changeQuery.employeeId"
          placeholder="员工"
          clearable
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="item in employees"
            :key="item.id"
            :label="`${item.name}（${item.employeeNo}）`"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="changeQuery.changeType" placeholder="变动类型" clearable style="width: 140px">
          <el-option
            v-for="opt in changeTypeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-select v-model="changeQuery.status" placeholder="审批状态" clearable style="width: 140px">
          <el-option
            v-for="opt in changeStatusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button type="primary" @click="onSearchChanges">查询</el-button>
      </div>

      <el-table v-loading="changeLoading" :data="changeList" border>
        <el-table-column label="员工" min-width="160">
          <template #default="{ row }">{{ employeeLabel(row as SalaryChangeVO) }}</template>
        </el-table-column>
        <el-table-column label="变动类型" width="110">
          <template #default="{ row }">
            {{ hrSalaryChangeTypeLabels[(row as SalaryChangeVO).changeType] }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="changeTagType((row as SalaryChangeVO).status)">
              {{ hrSalaryChangeStatusLabels[(row as SalaryChangeVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="110">
          <template #default="{ row }">{{ (row as SalaryChangeVO).applicantName ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="发起时间" width="180">
          <template #default="{ row }">{{ formatDateTime((row as SalaryChangeVO).createTime) }}</template>
        </el-table-column>
        <el-table-column label="BPM 实例状态" width="200">
          <template #default="{ row }">
            <span v-if="(row as SalaryChangeVO).bpmInstanceId">
              #{{ (row as SalaryChangeVO).bpmInstanceId }} ·
              {{ hrSalaryChangeStatusLabels[(row as SalaryChangeVO).status] }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="effectiveDate" label="生效日期" width="120" />
        <el-table-column label="变更摘要" min-width="240">
          <template #default="{ row }">
            <div v-if="(row as SalaryChangeVO).beforeSummary" class="summary-line">
              变更前：{{ (row as SalaryChangeVO).beforeSummary }}
            </div>
            <div v-if="(row as SalaryChangeVO).afterSummary" class="summary-line">
              变更后：{{ (row as SalaryChangeVO).afterSummary }}
            </div>
            <span
              v-if="!(row as SalaryChangeVO).beforeSummary && !(row as SalaryChangeVO).afterSummary"
            >
              -
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="160" />
      </el-table>

      <el-pagination
        v-model:current-page="changePage"
        v-model:page-size="changeSize"
        :total="changeTotal"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="onSearchChanges"
        @current-change="loadChanges"
      />
    </div>

    <!-- 新增薪资档案 -->
    <el-drawer v-model="createVisible" title="新增薪资档案" size="520px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="员工" prop="employeeId">
          <el-select
            v-model="createForm.employeeId"
            placeholder="请选择员工"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in employees"
              :key="item.id"
              :label="`${item.name}（${item.employeeNo}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="createForm.effectiveDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="基本工资" prop="baseSalary">
          <el-input v-model="createForm.baseSalary" placeholder="请输入基本工资（元）" />
        </el-form-item>
        <el-form-item label="岗位工资" prop="postSalary">
          <el-input v-model="createForm.postSalary" placeholder="请输入岗位工资（元，可空）" />
        </el-form-item>
        <el-form-item label="绩效基数" prop="performanceBase">
          <el-input v-model="createForm.performanceBase" placeholder="请输入绩效基数（元，可空）" />
        </el-form-item>
        <el-form-item label="固定补贴" prop="allowance">
          <el-input v-model="createForm.allowance" placeholder="请输入固定补贴（元，可空）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-drawer>

    <!-- 发起薪资变动 -->
    <el-drawer v-model="changeVisible" title="发起薪资变动" size="520px">
      <el-form ref="changeFormRef" :model="changeForm" :rules="changeRules" label-width="120px">
        <el-form-item label="员工" prop="employeeId">
          <el-select
            v-model="changeForm.employeeId"
            placeholder="请选择员工"
            filterable
            :disabled="!!changeSource"
            style="width: 100%"
          >
            <el-option
              v-for="item in employees"
              :key="item.id"
              :label="`${item.name}（${item.employeeNo}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="变动类型" prop="changeType">
          <el-select v-model="changeForm.changeType" style="width: 100%">
            <el-option
              v-for="opt in changeTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="changeForm.effectiveDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="基本工资" prop="baseSalary">
          <el-input v-model="changeForm.baseSalary" placeholder="变动后基本工资（留空沿用当前）" />
        </el-form-item>
        <el-form-item label="岗位工资" prop="postSalary">
          <el-input v-model="changeForm.postSalary" placeholder="变动后岗位工资（留空沿用当前）" />
        </el-form-item>
        <el-form-item label="绩效基数" prop="performanceBase">
          <el-input v-model="changeForm.performanceBase" placeholder="变动后绩效基数（留空沿用当前）" />
        </el-form-item>
        <el-form-item label="固定补贴" prop="allowance">
          <el-input v-model="changeForm.allowance" placeholder="变动后固定补贴（留空沿用当前）" />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input
            v-model="changeForm.reason"
            type="textarea"
            :rows="2"
            placeholder="申请原因（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChange">提交审批</el-button>
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
.summary-line {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}
</style>
