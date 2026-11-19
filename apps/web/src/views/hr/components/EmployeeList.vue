<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
  ElDialog,
  ElDrawer,
  ElEmpty,
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
  ElTimeline,
  ElTimelineItem
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type {
  DeptVO,
  EmployeeChangeRequest,
  EmployeeChangeVO,
  EmployeeUpdateRequest,
  EmployeeVO,
  HrChangeType,
  HrEmployeeStatus,
  HrEmploymentType,
  HrGender,
  PositionVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import {
  hrApprovalStatusLabels,
  hrChangeTypeLabels,
  hrEmployeeStatusLabels,
  hrEmploymentTypeLabels,
  hrGenderLabels,
  toOptions
} from '@/utils/labels'

defineOptions({ name: 'EmployeeList' })

const statusOptions = toOptions(hrEmployeeStatusLabels)
const genderOptions = toOptions(hrGenderLabels)
const employmentTypeOptions = toOptions(hrEmploymentTypeLabels)

const query = reactive<{
  keyword: string
  deptId: string
  status: HrEmployeeStatus | ''
}>({ keyword: '', deptId: '', status: '' })
const { page, size, total, resetPage } = usePage()
const list = ref<EmployeeVO[]>([])
const loading = ref(false)

/** 部门下拉（展平组织树，label 保留层级缩进） */
interface DeptOption {
  value: string
  label: string
  name: string
}
const deptOptions = ref<DeptOption[]>([])
const positionOptions = ref<PositionVO[]>([])
const deptNameMap = computed(() => new Map(deptOptions.value.map((item) => [item.value, item.name])))
const positionNameMap = computed(
  () => new Map(positionOptions.value.map((item) => [item.id, item.name]))
)

function flattenDept(nodes: DeptVO[], depth = 0, result: DeptOption[] = []): DeptOption[] {
  for (const node of nodes) {
    result.push({ value: node.id, label: `${'　'.repeat(depth)}${node.name}`, name: node.name })
    if (node.children?.length) flattenDept(node.children, depth + 1, result)
  }
  return result
}

function deptName(id?: string | null): string {
  if (!id) return '-'
  return deptNameMap.value.get(id) ?? id
}

function positionName(id?: string | null): string {
  if (!id) return '-'
  return positionNameMap.value.get(id) ?? id
}

function statusTagType(status: HrEmployeeStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'PROBATION':
      return 'warning'
    case 'LEAVE':
      return 'info'
    default:
      return 'danger'
  }
}

function approvalTagType(
  status: EmployeeChangeVO['approvalStatus']
): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

async function loadDepartments(): Promise<void> {
  try {
    deptOptions.value = flattenDept(await ceamsApi.iam.deptTree())
  } catch {
    // 部门树加载失败不阻断员工列表
  }
}

async function loadPositions(): Promise<void> {
  try {
    positionOptions.value = await ceamsApi.hr.positionList()
  } catch {
    // 岗位列表加载失败不阻断员工列表
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.hr.employeePage({
      page: page.value,
      size: size.value,
      keyword: query.keyword || undefined,
      deptId: query.deptId || undefined,
      status: query.status || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载员工档案失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

// ---------- 新增 / 编辑抽屉 ----------
const drawerVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  userId: '',
  employeeNo: '',
  name: '',
  gender: '' as HrGender | '',
  birthDate: '',
  idCard: '',
  phone: '',
  email: '',
  deptId: '',
  positionId: '',
  employmentType: '' as HrEmploymentType | '',
  hireDate: '',
  probationEndDate: '',
  emergencyContact: '',
  emergencyPhone: '',
  remark: ''
})

const rules: FormRules<typeof form> = {
  employeeNo: [{ required: true, message: '请输入员工工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

function resetForm(): void {
  Object.assign(form, {
    userId: '',
    employeeNo: '',
    name: '',
    gender: '',
    birthDate: '',
    idCard: '',
    phone: '',
    email: '',
    deptId: '',
    positionId: '',
    employmentType: '',
    hireDate: '',
    probationEndDate: '',
    emergencyContact: '',
    emergencyPhone: '',
    remark: ''
  })
}

function openCreate(): void {
  editingId.value = null
  resetForm()
  drawerVisible.value = true
}

function openEdit(row: EmployeeVO): void {
  editingId.value = row.id
  resetForm()
  form.userId = row.userId ?? ''
  form.employeeNo = row.employeeNo
  form.name = row.name
  form.gender = row.gender ?? ''
  form.birthDate = row.birthDate ?? ''
  form.deptId = row.deptId ?? ''
  form.positionId = row.positionId ?? ''
  form.employmentType = row.employmentType ?? ''
  form.hireDate = row.hireDate ?? ''
  form.probationEndDate = row.probationEndDate ?? ''
  form.emergencyContact = row.emergencyContact ?? ''
  form.remark = row.remark ?? ''
  // 身份证 / 手机号 / 邮箱 / 紧急联系电话后端仅返回脱敏值，留空表示不修改
  drawerVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      const payload: EmployeeUpdateRequest = {
        employeeNo: form.employeeNo || undefined,
        name: form.name || undefined,
        gender: form.gender || undefined,
        birthDate: form.birthDate || undefined,
        deptId: form.deptId || undefined,
        positionId: form.positionId || undefined,
        employmentType: form.employmentType || undefined,
        hireDate: form.hireDate || undefined,
        probationEndDate: form.probationEndDate || undefined,
        emergencyContact: form.emergencyContact || undefined,
        remark: form.remark || undefined
      }
      // 敏感字段仅在有输入时提交，留空即不修改（与后端 update 语义一致）
      if (form.userId) payload.userId = form.userId
      if (form.idCard) payload.idCard = form.idCard
      if (form.phone) payload.phone = form.phone
      if (form.email) payload.email = form.email
      if (form.emergencyPhone) payload.emergencyPhone = form.emergencyPhone
      await ceamsApi.hr.employeeUpdate(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.hr.employeeCreate({
        userId: form.userId || null,
        employeeNo: form.employeeNo,
        name: form.name,
        gender: form.gender || undefined,
        birthDate: form.birthDate || undefined,
        idCard: form.idCard || undefined,
        phone: form.phone || undefined,
        email: form.email || undefined,
        deptId: form.deptId || undefined,
        positionId: form.positionId || undefined,
        employmentType: form.employmentType || undefined,
        hireDate: form.hireDate || undefined,
        probationEndDate: form.probationEndDate || undefined,
        emergencyContact: form.emergencyContact || undefined,
        emergencyPhone: form.emergencyPhone || undefined,
        remark: form.remark || undefined
      })
      ElMessage.success('创建成功')
    }
    drawerVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存员工档案失败'))
  }
}

// ---------- 转正 / 调岗 / 离职 ----------
const changeVisible = ref(false)
const changeType = ref<HrChangeType>('REGULAR')
const changeEmployee = ref<EmployeeVO | null>(null)
const changeFormRef = ref<FormInstance>()
const changeForm = reactive({
  effectiveDate: '',
  reason: '',
  targetDeptId: '',
  targetPositionId: '',
  remark: ''
})

const changeTitle = computed(() => `${hrChangeTypeLabels[changeType.value]}申请`)

function openChange(row: EmployeeVO, type: HrChangeType): void {
  changeEmployee.value = row
  changeType.value = type
  Object.assign(changeForm, {
    effectiveDate: '',
    reason: '',
    targetDeptId: '',
    targetPositionId: '',
    remark: ''
  })
  changeVisible.value = true
}

async function submitChange(): Promise<void> {
  if (!changeEmployee.value) return
  if (!changeForm.effectiveDate) {
    ElMessage.warning('请选择生效日期')
    return
  }
  if (changeType.value === 'TRANSFER' && !changeForm.targetDeptId && !changeForm.targetPositionId) {
    ElMessage.warning('调岗申请需指定目标部门或目标岗位')
    return
  }

  const body: EmployeeChangeRequest = {
    effectiveDate: changeForm.effectiveDate,
    reason: changeForm.reason || undefined,
    remark: changeForm.remark || undefined,
    targetDeptId: changeType.value === 'TRANSFER' ? changeForm.targetDeptId || undefined : undefined,
    targetPositionId:
      changeType.value === 'TRANSFER' ? changeForm.targetPositionId || undefined : undefined
  }

  try {
    if (changeType.value === 'REGULAR') {
      await ceamsApi.hr.employeeRegular(changeEmployee.value.id, body)
    } else if (changeType.value === 'TRANSFER') {
      await ceamsApi.hr.employeeTransfer(changeEmployee.value.id, body)
    } else {
      await ceamsApi.hr.employeeResign(changeEmployee.value.id, body)
    }
    ElMessage.success('已发起审批')
    changeVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '发起申请失败'))
  }
}

// ---------- 变更记录 ----------
const changeListVisible = ref(false)
const changeListLoading = ref(false)
const changeRecords = ref<EmployeeChangeVO[]>([])
const changeRecordEmployee = ref<EmployeeVO | null>(null)

async function openChanges(row: EmployeeVO): Promise<void> {
  changeRecordEmployee.value = row
  changeListVisible.value = true
  changeListLoading.value = true
  changeRecords.value = []
  try {
    changeRecords.value = await ceamsApi.hr.employeeChanges(row.id)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载变更记录失败'))
  } finally {
    changeListLoading.value = false
  }
}

onMounted(() => {
  void loadDepartments()
  void loadPositions()
  void load()
})
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="关键字（姓名/工号）"
        clearable
        style="width: 200px"
      />
      <el-select v-model="query.deptId" placeholder="部门" clearable style="width: 180px">
        <el-option
          v-for="opt in deptOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-select v-model="query.status" placeholder="员工状态" clearable style="width: 140px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增员工</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="employeeNo" label="工号" width="120" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column label="部门" min-width="140">
        <template #default="{ row }">{{ deptName((row as EmployeeVO).deptId) }}</template>
      </el-table-column>
      <el-table-column label="岗位" min-width="140">
        <template #default="{ row }">{{ positionName((row as EmployeeVO).positionId) }}</template>
      </el-table-column>
      <el-table-column prop="hireDate" label="入职日期" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType((row as EmployeeVO).status)">
            {{ hrEmployeeStatusLabels[(row as EmployeeVO).status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column label="操作" width="330" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row as EmployeeVO)">编辑</el-button>
          <el-button link type="primary" size="small" @click="openChange(row as EmployeeVO, 'REGULAR')">转正</el-button>
          <el-button link type="primary" size="small" @click="openChange(row as EmployeeVO, 'TRANSFER')">调岗</el-button>
          <el-button link type="danger" size="small" @click="openChange(row as EmployeeVO, 'RESIGN')">离职</el-button>
          <el-button link type="info" size="small" @click="openChanges(row as EmployeeVO)">变更记录</el-button>
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

    <el-drawer v-model="drawerVisible" :title="editingId ? '编辑员工档案' : '新增员工档案'" size="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="工号" prop="employeeNo">
          <el-input v-model="form.employeeNo" placeholder="请输入员工工号" />
        </el-form-item>
        <el-form-item label="绑定用户 ID">
          <el-input v-model="form.userId" placeholder="系统用户 ID（可空，编辑时填 0 表示解绑）" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" placeholder="请选择性别" clearable style="width: 100%">
            <el-option v-for="opt in genderOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input
            v-model="form.idCard"
            :placeholder="editingId ? '后端仅返回脱敏值，留空表示不修改' : '请输入身份证号'"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="form.phone"
            :placeholder="editingId ? '后端仅返回脱敏值，留空表示不修改' : '请输入手机号'"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="form.email"
            :placeholder="editingId ? '后端仅返回脱敏值，留空表示不修改' : '请输入邮箱'"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" placeholder="请选择部门" clearable style="width: 100%">
            <el-option
              v-for="opt in deptOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.positionId" placeholder="请选择岗位" clearable style="width: 100%">
            <el-option
              v-for="opt in positionOptions"
              :key="opt.id"
              :label="opt.name"
              :value="opt.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用工类型">
          <el-select v-model="form.employmentType" placeholder="请选择用工类型" clearable style="width: 100%">
            <el-option
              v-for="opt in employmentTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker
            v-model="form.hireDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="试用期结束">
          <el-date-picker
            v-model="form.probationEndDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="紧急联系人">
          <el-input v-model="form.emergencyContact" placeholder="请输入紧急联系人" />
        </el-form-item>
        <el-form-item label="紧急联系电话">
          <el-input
            v-model="form.emergencyPhone"
            :placeholder="editingId ? '后端仅返回脱敏值，留空表示不修改' : '请输入紧急联系电话'"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="changeVisible" :title="changeTitle" width="520px">
      <div v-if="changeEmployee" class="change-tip">
        员工：{{ changeEmployee.name }}（{{ changeEmployee.employeeNo }}）
      </div>
      <el-form ref="changeFormRef" :model="changeForm" label-width="100px">
        <el-form-item label="生效日期" required>
          <el-date-picker
            v-model="changeForm.effectiveDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <template v-if="changeType === 'TRANSFER'">
          <el-form-item label="目标部门">
            <el-select v-model="changeForm.targetDeptId" placeholder="请选择目标部门" clearable style="width: 100%">
              <el-option
                v-for="opt in deptOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="目标岗位">
            <el-select
              v-model="changeForm.targetPositionId"
              placeholder="请选择目标岗位"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="opt in positionOptions"
                :key="opt.id"
                :label="opt.name"
                :value="opt.id"
              />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="申请原因">
          <el-input v-model="changeForm.reason" type="textarea" :rows="2" placeholder="申请原因（可选）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="changeForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChange">提交审批</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="changeListVisible" title="变更记录" size="480px">
      <div v-loading="changeListLoading">
        <div v-if="changeRecordEmployee" class="change-tip">
          员工：{{ changeRecordEmployee.name }}（{{ changeRecordEmployee.employeeNo }}）
        </div>
        <el-empty v-if="!changeListLoading && changeRecords.length === 0" description="暂无变更记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="item in changeRecords"
            :key="item.id"
            :timestamp="item.createTime ?? ''"
            placement="top"
          >
            <div class="record-title">
              <span>{{ hrChangeTypeLabels[item.changeType] }}</span>
              <el-tag size="small" :type="approvalTagType(item.approvalStatus)">
                {{ hrApprovalStatusLabels[item.approvalStatus] }}
              </el-tag>
            </div>
            <div class="record-line">生效日期：{{ item.effectiveDate }}</div>
            <div v-if="item.targetDeptId || item.targetPositionId" class="record-line">
              目标：{{ deptName(item.targetDeptId) }} / {{ positionName(item.targetPositionId) }}
            </div>
            <div v-if="item.reason" class="record-line">原因：{{ item.reason }}</div>
            <div v-if="item.remark" class="record-line">备注：{{ item.remark }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
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
.change-tip {
  margin-bottom: 12px;
  color: #4b5563;
  font-size: 13px;
}
.record-title {
  display: flex;
  gap: 8px;
  align-items: center;
  font-weight: 500;
}
.record-line {
  margin-top: 4px;
  color: #4b5563;
  font-size: 13px;
}
</style>
