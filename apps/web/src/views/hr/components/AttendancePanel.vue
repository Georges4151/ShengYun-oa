<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTimePicker
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type {
  CheckinVO,
  EmployeeVO,
  HrCheckinStatus,
  HrScheduleType,
  ScheduleVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import {
  hrCheckinSourceLabels,
  hrCheckinStatusLabels,
  hrCheckinTypeLabels,
  hrScheduleTypeLabels,
  toOptions
} from '@/utils/labels'

defineOptions({ name: 'AttendancePanel' })

const employees = ref<EmployeeVO[]>([])
const employeeNameMap = computed(
  () => new Map(employees.value.map((item) => [item.id, item.name]))
)
const checkinStatusOptions = toOptions(hrCheckinStatusLabels)
const scheduleTypeOptions = toOptions(hrScheduleTypeLabels)

function employeeName(id: string): string {
  return employeeNameMap.value.get(id) ?? id
}

function checkinTagType(status: HrCheckinStatus): 'success' | 'warning' | 'danger' {
  switch (status) {
    case 'NORMAL':
      return 'success'
    case 'LATE':
    case 'EARLY':
      return 'warning'
    default:
      return 'danger'
  }
}

async function loadEmployees(): Promise<void> {
  try {
    const res = await ceamsApi.hr.employeePage({ page: 1, size: 100 })
    employees.value = res.records
  } catch {
    // 员工下拉加载失败不阻断考勤面板
  }
}

// ---------- 排班 ----------
const scheduleQuery = reactive<{ employeeId: string; month: string }>({ employeeId: '', month: '' })
const schedules = ref<ScheduleVO[]>([])
const scheduleLoading = ref(false)

async function loadSchedules(): Promise<void> {
  scheduleLoading.value = true
  try {
    schedules.value = await ceamsApi.hr.scheduleList({
      employeeId: scheduleQuery.employeeId || undefined,
      month: scheduleQuery.month || undefined
    })
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载排班失败'))
  } finally {
    scheduleLoading.value = false
  }
}

const scheduleVisible = ref(false)
const scheduleEditingId = ref<string | null>(null)
const scheduleFormRef = ref<FormInstance>()
const scheduleForm = reactive({
  employeeId: '',
  workDate: '',
  shiftCode: '',
  startTime: '',
  endTime: '',
  scheduleType: 'FIXED' as HrScheduleType,
  remark: ''
})

const scheduleRules: FormRules<typeof scheduleForm> = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择排班日期', trigger: 'change' }]
}

function resetScheduleForm(): void {
  Object.assign(scheduleForm, {
    employeeId: '',
    workDate: '',
    shiftCode: '',
    startTime: '',
    endTime: '',
    scheduleType: 'FIXED',
    remark: ''
  })
}

function openScheduleCreate(): void {
  scheduleEditingId.value = null
  resetScheduleForm()
  scheduleVisible.value = true
}

function openScheduleEdit(row: ScheduleVO): void {
  scheduleEditingId.value = row.id
  resetScheduleForm()
  scheduleForm.employeeId = row.employeeId
  scheduleForm.workDate = row.workDate
  scheduleForm.shiftCode = row.shiftCode ?? ''
  scheduleForm.startTime = row.startTime ?? ''
  scheduleForm.endTime = row.endTime ?? ''
  scheduleForm.scheduleType = row.scheduleType ?? 'FIXED'
  scheduleForm.remark = row.remark ?? ''
  scheduleVisible.value = true
}

async function submitSchedule(): Promise<void> {
  if (!scheduleFormRef.value) return
  const valid = await scheduleFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (scheduleEditingId.value) {
      await ceamsApi.hr.scheduleUpdate(scheduleEditingId.value, {
        shiftCode: scheduleForm.shiftCode || undefined,
        startTime: scheduleForm.startTime || undefined,
        endTime: scheduleForm.endTime || undefined,
        scheduleType: scheduleForm.scheduleType,
        remark: scheduleForm.remark || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.hr.scheduleCreate({
        employeeId: scheduleForm.employeeId,
        workDate: scheduleForm.workDate,
        shiftCode: scheduleForm.shiftCode || undefined,
        startTime: scheduleForm.startTime || undefined,
        endTime: scheduleForm.endTime || undefined,
        scheduleType: scheduleForm.scheduleType,
        remark: scheduleForm.remark || undefined
      })
      ElMessage.success('创建成功')
    }
    scheduleVisible.value = false
    await loadSchedules()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存排班失败'))
  }
}

// ---------- 批量排班 ----------
const batchVisible = ref(false)
const batchForm = reactive({
  employeeIds: [] as string[],
  startDate: '',
  endDate: '',
  shiftCode: '',
  startTime: '',
  endTime: ''
})

function openBatch(): void {
  Object.assign(batchForm, {
    employeeIds: [],
    startDate: '',
    endDate: '',
    shiftCode: '',
    startTime: '',
    endTime: ''
  })
  batchVisible.value = true
}

async function submitBatch(): Promise<void> {
  if (batchForm.employeeIds.length === 0) {
    ElMessage.warning('请选择员工')
    return
  }
  if (!batchForm.startDate || !batchForm.endDate) {
    ElMessage.warning('请选择日期区间')
    return
  }
  try {
    const count = await ceamsApi.hr.scheduleBatch({
      employeeIds: batchForm.employeeIds,
      startDate: batchForm.startDate,
      endDate: batchForm.endDate,
      shiftCode: batchForm.shiftCode || undefined,
      startTime: batchForm.startTime || undefined,
      endTime: batchForm.endTime || undefined
    })
    ElMessage.success(`批量排班完成，共处理 ${count} 条`)
    batchVisible.value = false
    await loadSchedules()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '批量排班失败'))
  }
}

// ---------- 打卡记录 ----------
const checkinQuery = reactive<{
  employeeId: string
  startDate: string
  endDate: string
  status: HrCheckinStatus | ''
}>({ employeeId: '', startDate: '', endDate: '', status: '' })
const { page, size, total, resetPage } = usePage()
const checkins = ref<CheckinVO[]>([])
const checkinLoading = ref(false)

async function loadCheckins(): Promise<void> {
  checkinLoading.value = true
  try {
    const res = await ceamsApi.hr.checkinPage({
      page: page.value,
      size: size.value,
      employeeId: checkinQuery.employeeId || undefined,
      startDate: checkinQuery.startDate || undefined,
      endDate: checkinQuery.endDate || undefined,
      status: checkinQuery.status || undefined
    })
    checkins.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载打卡记录失败'))
  } finally {
    checkinLoading.value = false
  }
}

function onCheckinSearch(): void {
  resetPage()
  void loadCheckins()
}

onMounted(() => {
  void loadEmployees()
  void loadSchedules()
  void loadCheckins()
})
</script>

<template>
  <div class="panel-page">
    <!-- 排班管理 -->
    <div class="section">
      <div class="filter-bar">
        <el-select v-model="scheduleQuery.employeeId" placeholder="员工" clearable filterable style="width: 180px">
          <el-option
            v-for="item in employees"
            :key="item.id"
            :label="`${item.name}（${item.employeeNo}）`"
            :value="item.id"
          />
        </el-select>
        <el-date-picker
          v-model="scheduleQuery.month"
          type="month"
          value-format="YYYY-MM"
          placeholder="月份（默认当月）"
          style="width: 160px"
        />
        <div class="filter-actions">
          <el-button type="primary" @click="loadSchedules">查询</el-button>
        </div>
      </div>

      <div class="data-card">
        <div class="card-head">
          <div class="card-heading">
            <span class="card-title">排班管理</span>
            <span class="card-sub">共 {{ schedules.length }} 条</span>
          </div>
          <div class="card-actions">
            <el-button @click="openScheduleCreate">新增排班</el-button>
            <el-button @click="openBatch">批量排班</el-button>
          </div>
        </div>

        <el-table v-loading="scheduleLoading" :data="schedules" border>
          <el-table-column label="员工" width="140">
            <template #default="{ row }">{{ employeeName((row as ScheduleVO).employeeId) }}</template>
          </el-table-column>
          <el-table-column prop="workDate" label="排班日期" width="130" />
          <el-table-column prop="shiftCode" label="班次编码" width="120" />
          <el-table-column prop="startTime" label="上班时间" width="110" />
          <el-table-column prop="endTime" label="下班时间" width="110" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              {{ (row as ScheduleVO).scheduleType ? hrScheduleTypeLabels[(row as ScheduleVO).scheduleType!] : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140" />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openScheduleEdit(row as ScheduleVO)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 打卡记录 -->
    <div class="section">
      <div class="filter-bar">
        <el-select v-model="checkinQuery.employeeId" placeholder="员工" clearable filterable style="width: 180px">
          <el-option
            v-for="item in employees"
            :key="item.id"
            :label="`${item.name}（${item.employeeNo}）`"
            :value="item.id"
          />
        </el-select>
        <el-date-picker
          v-model="checkinQuery.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
          style="width: 150px"
        />
        <el-date-picker
          v-model="checkinQuery.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
          style="width: 150px"
        />
        <el-select v-model="checkinQuery.status" placeholder="打卡状态" clearable style="width: 140px">
          <el-option v-for="opt in checkinStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="onCheckinSearch">查询</el-button>
        </div>
      </div>

      <div class="data-card">
        <div class="card-head">
          <div class="card-heading">
            <span class="card-title">打卡记录</span>
            <span class="card-sub">共 {{ total }} 条</span>
          </div>
        </div>

        <el-table v-loading="checkinLoading" :data="checkins" border>
          <el-table-column label="员工" width="140">
            <template #default="{ row }">{{ employeeName((row as CheckinVO).employeeId) }}</template>
          </el-table-column>
          <el-table-column prop="checkinDate" label="打卡日期" width="120" />
          <el-table-column prop="checkinTime" label="打卡时间" width="170" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ hrCheckinTypeLabels[(row as CheckinVO).checkinType] }}</template>
          </el-table-column>
          <el-table-column label="来源" width="110">
            <template #default="{ row }">
              {{ (row as CheckinVO).source ? hrCheckinSourceLabels[(row as CheckinVO).source!] : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="checkinTagType((row as CheckinVO).status)">
                {{ hrCheckinStatusLabels[(row as CheckinVO).status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="address" label="打卡地址" min-width="160" />
          <el-table-column prop="remark" label="备注" min-width="120" />
        </el-table>

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          class="pagination"
          @size-change="onCheckinSearch"
          @current-change="loadCheckins"
        />
      </div>
    </div>

    <!-- 补卡 / 请假 -->
    <div class="section">
      <div class="data-card">
        <div class="card-head">
          <div class="card-heading">
            <span class="card-title">补卡 / 请假</span>
          </div>
        </div>
        <el-alert
          title="补卡、请假等审批请在待办中心处理"
          description="本页仅提供排班与打卡记录管理；补卡、请假申请由移动端发起，审批进度请在待办中心查看。"
          type="info"
          show-icon
          :closable="false"
        />
      </div>
    </div>

    <!-- 排班新增 / 编辑 -->
    <el-dialog v-model="scheduleVisible" :title="scheduleEditingId ? '编辑排班' : '新增排班'" width="520px">
      <el-form ref="scheduleFormRef" :model="scheduleForm" :rules="scheduleRules" label-width="100px">
        <el-form-item label="员工" prop="employeeId">
          <el-select
            v-model="scheduleForm.employeeId"
            placeholder="请选择员工"
            filterable
            :disabled="!!scheduleEditingId"
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
        <el-form-item label="排班日期" prop="workDate">
          <el-date-picker
            v-model="scheduleForm.workDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled="!!scheduleEditingId"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="班次编码">
          <el-input v-model="scheduleForm.shiftCode" placeholder="如：A / B / 常白班" />
        </el-form-item>
        <el-form-item label="上班时间">
          <el-time-picker
            v-model="scheduleForm.startTime"
            value-format="HH:mm:ss"
            placeholder="上班时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="下班时间">
          <el-time-picker
            v-model="scheduleForm.endTime"
            value-format="HH:mm:ss"
            placeholder="下班时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排班类型">
          <el-select v-model="scheduleForm.scheduleType" style="width: 100%">
            <el-option
              v-for="opt in scheduleTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="scheduleForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSchedule">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量排班 -->
    <el-dialog v-model="batchVisible" title="批量排班" width="560px">
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="员工" required>
          <el-select
            v-model="batchForm.employeeIds"
            multiple
            filterable
            collapse-tags
            placeholder="请选择员工（可多选）"
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
        <el-form-item label="开始日期" required>
          <el-date-picker
            v-model="batchForm.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker
            v-model="batchForm.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="班次编码">
          <el-input v-model="batchForm.shiftCode" placeholder="如：A / B / 常白班" />
        </el-form-item>
        <el-form-item label="上班时间">
          <el-time-picker
            v-model="batchForm.startTime"
            value-format="HH:mm:ss"
            placeholder="上班时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="下班时间">
          <el-time-picker
            v-model="batchForm.endTime"
            value-format="HH:mm:ss"
            placeholder="下班时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatch">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 多个区块之间保持 16px 间距 */
.section {
  margin-bottom: var(--ceams-space-4, 16px);
}

.section:last-child {
  margin-bottom: 0;
}

/* 筛选卡片：白色圆角、16px 20px 内边距，操作按钮靠右 */
.filter-bar {
  padding: 16px 20px;
}

.filter-actions {
  display: flex;
  gap: var(--ceams-space-2, 8px);
  margin-left: auto;
}

/* 表格卡片：20px 内边距 */
.data-card {
  padding: 20px;
}

/* 卡片头部：左侧标题与条数，右侧主操作 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3, 12px);
  margin-bottom: var(--ceams-space-4, 16px);
}

.card-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--ceams-space-2, 8px);
}

.card-title {
  font-size: var(--ceams-font-size-md, 16px);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-sub {
  font-size: var(--ceams-font-size-xs, 12px);
  color: var(--ceams-text-secondary);
}

.card-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--ceams-space-2, 8px);
}

.card-actions .el-button + .el-button {
  margin-left: 0;
}

/* 分页右对齐，距表格 16px */
.pagination {
  margin-top: var(--ceams-space-4, 16px);
  justify-content: flex-end;
}

/* 空态居中灰色，上下 40px 留白 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}

/* 弹窗：标题加粗，内容 20px 24px，底部操作条靠右并带顶部细边框 */
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
