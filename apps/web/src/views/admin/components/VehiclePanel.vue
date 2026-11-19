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
  ElInputNumber,
  ElMessage,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { formatDate, formatDateTime, formatMoney } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import type {
  VehicleVO,
  Id,
  VehicleApplyVO,
  VehicleApplyStatus,
  VehicleExpenseVO,
  VehicleExpenseType,
  VehicleStatus
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import {
  toOptions,
  vehicleApplyStatusLabels,
  vehicleExpenseTypeLabels,
  vehicleStatusLabels
} from '@/utils/labels'

defineOptions({ name: 'VehiclePanel' })

const canManage = computed(() => hasPermission('admin:vehicle:manage'))
const statusOptions = toOptions(vehicleStatusLabels)
const expenseTypeOptions = toOptions(vehicleExpenseTypeLabels)

function vehicleTagType(status: VehicleStatus): 'success' | 'warning' | 'info' {
  switch (status) {
    case 'IDLE':
      return 'success'
    case 'USING':
    case 'MAINTENANCE':
      return 'warning'
    default:
      return 'info'
  }
}

function applyTagType(status: VehicleApplyStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'APPROVING':
    case 'USING':
      return 'warning'
    case 'APPROVED':
    case 'RETURNED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

function remainClass(days: number | null): string {
  if (days === null) return ''
  if (days < 0) return 'remain-danger'
  if (days <= 30) return 'remain-warning'
  return ''
}

/** 剩余天数展示，已过期时显示中文提示。 */
function remainText(days: number | null): string {
  if (days === null) return '-'
  return days < 0 ? '已过期' : `${days} 天`
}

// ---------- 到期预警 ----------
const alerts = ref<VehicleVO[]>([])
const alertsLoading = ref(false)

async function loadAlerts(): Promise<void> {
  alertsLoading.value = true
  try {
    alerts.value = await ceamsApi.admin.vehicleAlerts()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载车辆到期预警失败'))
  } finally {
    alertsLoading.value = false
  }
}

// ---------- 车辆列表 ----------
const query = reactive<{ keyword: string; status: VehicleStatus | '' }>({ keyword: '', status: '' })
const { page, size, total, resetPage } = usePage()
const list = ref<VehicleVO[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.admin.vehiclePage({
      page: page.value,
      size: size.value,
      keyword: query.keyword.trim() || undefined,
      status: query.status || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载车辆列表失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

// ---------- 新建 / 编辑车辆 ----------
const formVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<Id | null>(null)
const form = reactive({
  plateNo: '',
  brandModel: '',
  seats: 5,
  purchaseDate: '',
  insuranceExpire: '',
  inspectionExpire: '',
  remark: ''
})
const formRules: FormRules<typeof form> = {
  plateNo: [{ required: true, message: '请输入车牌号', trigger: 'blur' }]
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, {
    plateNo: '',
    brandModel: '',
    seats: 5,
    purchaseDate: '',
    insuranceExpire: '',
    inspectionExpire: '',
    remark: ''
  })
  formVisible.value = true
}

function openEdit(row: VehicleVO): void {
  editingId.value = row.id
  Object.assign(form, {
    plateNo: row.plateNo,
    brandModel: row.brandModel ?? '',
    seats: row.seats ?? 0,
    purchaseDate: row.purchaseDate ?? '',
    insuranceExpire: row.insuranceExpire ?? '',
    inspectionExpire: row.inspectionExpire ?? '',
    remark: row.remark ?? ''
  })
  formVisible.value = true
}

async function submitForm(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.admin.vehicleUpdate(editingId.value, {
        brandModel: form.brandModel.trim() || undefined,
        seats: form.seats || undefined,
        purchaseDate: form.purchaseDate || undefined,
        insuranceExpire: form.insuranceExpire || undefined,
        inspectionExpire: form.inspectionExpire || undefined,
        remark: form.remark.trim() || undefined
      })
      ElMessage.success('车辆信息已更新')
    } else {
      await ceamsApi.admin.vehicleCreate({
        plateNo: form.plateNo.trim(),
        brandModel: form.brandModel.trim() || undefined,
        seats: form.seats || undefined,
        purchaseDate: form.purchaseDate || undefined,
        insuranceExpire: form.insuranceExpire || undefined,
        inspectionExpire: form.inspectionExpire || undefined,
        remark: form.remark.trim() || undefined
      })
      ElMessage.success('车辆已创建')
    }
    formVisible.value = false
    await Promise.all([load(), loadAlerts(), loadApplyVehicles()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存车辆失败'))
  }
}

// ---------- 用车申请 ----------
const applyVehicles = ref<VehicleVO[]>([])
const applyVisible = ref(false)
const applyFormRef = ref<FormInstance>()
const applyForm = reactive({
  vehicleId: '',
  purpose: '',
  destination: '',
  startTime: '',
  endTime: ''
})
const applyRules: FormRules<typeof applyForm> = {
  vehicleId: [{ required: true, message: '请选择车辆', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

async function loadApplyVehicles(): Promise<void> {
  try {
    const res = await ceamsApi.admin.vehiclePage({ page: 1, size: 200, status: 'IDLE' })
    applyVehicles.value = res.records
  } catch {
    // 车辆下拉加载失败不阻断列表
  }
}

function openApply(): void {
  Object.assign(applyForm, {
    vehicleId: '',
    purpose: '',
    destination: '',
    startTime: '',
    endTime: ''
  })
  applyVisible.value = true
}

async function submitApply(): Promise<void> {
  if (!applyFormRef.value) return
  const valid = await applyFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (applyForm.startTime && applyForm.endTime && applyForm.startTime >= applyForm.endTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  try {
    await ceamsApi.admin.vehicleApply({
      vehicleId: applyForm.vehicleId,
      purpose: applyForm.purpose.trim() || undefined,
      destination: applyForm.destination.trim() || undefined,
      startTime: applyForm.startTime,
      endTime: applyForm.endTime
    })
    ElMessage.success('用车申请已提交，请在待办中心跟踪审批')
    applyVisible.value = false
    await loadMyApplies()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '提交用车申请失败'))
  }
}

// ---------- 我的用车 ----------
const myApplies = ref<VehicleApplyVO[]>([])
const myLoading = ref(false)

async function loadMyApplies(): Promise<void> {
  myLoading.value = true
  try {
    myApplies.value = await ceamsApi.admin.myVehicleApplies()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载我的用车记录失败'))
  } finally {
    myLoading.value = false
  }
}

// ---------- 还车 ----------
const returnVisible = ref(false)
const returnApply = ref<VehicleApplyVO | null>(null)
const returnFormRef = ref<FormInstance>()
const returnForm = reactive({ returnTime: '', mileage: 0 })
const returnRules: FormRules<typeof returnForm> = {
  mileage: [{ required: true, message: '请输入还车里程', trigger: 'blur' }]
}

function openReturn(row: VehicleApplyVO): void {
  returnApply.value = row
  Object.assign(returnForm, {
    returnTime: formatDateTime(new Date(), '').replace(' ', 'T'),
    mileage: row.mileage === null ? 0 : Number(row.mileage)
  })
  returnVisible.value = true
}

async function submitReturn(): Promise<void> {
  if (!returnApply.value || !returnFormRef.value) return
  const valid = await returnFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (returnForm.mileage < 0) {
    ElMessage.warning('还车里程不能为负数')
    return
  }
  try {
    await ceamsApi.admin.vehicleReturn(returnApply.value.id, {
      returnTime: returnForm.returnTime || undefined,
      mileage: returnForm.mileage
    })
    ElMessage.success('还车成功')
    returnVisible.value = false
    await Promise.all([loadMyApplies(), load(), loadAlerts()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '还车失败'))
  }
}

// ---------- 费用登记 / 查看 ----------
const expenseVisible = ref(false)
const expenseVehicle = ref<VehicleVO | null>(null)
const expenseForm = reactive({
  expenseType: 'FUEL' as VehicleExpenseType,
  amount: '',
  occurDate: formatDate(new Date(), ''),
  attachmentName: '',
  remark: ''
})

function openExpense(row: VehicleVO): void {
  expenseVehicle.value = row
  Object.assign(expenseForm, {
    expenseType: 'FUEL',
    amount: '',
    occurDate: formatDate(new Date(), ''),
    attachmentName: '',
    remark: ''
  })
  expenseVisible.value = true
}

async function submitExpense(): Promise<void> {
  if (!expenseVehicle.value) return
  const amount = Number(expenseForm.amount)
  if (!expenseForm.amount.trim() || Number.isNaN(amount) || amount <= 0) {
    ElMessage.warning('请输入大于 0 的费用金额')
    return
  }
  if (!expenseForm.occurDate) {
    ElMessage.warning('请选择费用发生日期')
    return
  }
  try {
    await ceamsApi.admin.vehicleExpenseCreate(expenseVehicle.value.id, {
      expenseType: expenseForm.expenseType,
      amount,
      occurDate: expenseForm.occurDate,
      attachmentName: expenseForm.attachmentName.trim() || undefined,
      remark: expenseForm.remark.trim() || undefined
    })
    ElMessage.success('费用登记成功')
    expenseVisible.value = false
    if (expenseListVehicle.value?.id === expenseVehicle.value.id) {
      await loadExpenses(expenseVehicle.value.id)
    }
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '费用登记失败'))
  }
}

const expenseListVisible = ref(false)
const expenseListVehicle = ref<VehicleVO | null>(null)
const expenses = ref<VehicleExpenseVO[]>([])
const expenseLoading = ref(false)

async function loadExpenses(id: Id): Promise<void> {
  expenseLoading.value = true
  try {
    expenses.value = await ceamsApi.admin.vehicleExpenses(id)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载车辆费用失败'))
  } finally {
    expenseLoading.value = false
  }
}

async function openExpenseList(row: VehicleVO): Promise<void> {
  expenseListVehicle.value = row
  expenseListVisible.value = true
  await loadExpenses(row.id)
}

onMounted(() => {
  void loadAlerts()
  void load()
  void loadApplyVehicles()
  void loadMyApplies()
})
</script>

<template>
  <div class="vehicle-panel">
    <!-- 到期预警卡片 -->
    <div class="data-card panel-card">
      <div class="card-header">
        <div class="card-heading">
          <span class="card-title">保险 / 年检到期预警</span>
          <span class="card-count">共 {{ alerts.length }} 辆</span>
        </div>
      </div>
      <el-table v-if="alerts.length > 0" v-loading="alertsLoading" :data="alerts" border size="small">
        <el-table-column prop="plateNo" label="车牌号" width="130" />
        <el-table-column prop="brandModel" label="品牌车型" min-width="140">
          <template #default="{ row }">{{ (row as VehicleVO).brandModel ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="保险到期" width="130">
          <template #default="{ row }">{{ (row as VehicleVO).insuranceExpire ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="保险剩余天数" width="130" align="right">
          <template #default="{ row }">
            <span :class="remainClass((row as VehicleVO).insuranceRemainDays)">
              {{ remainText((row as VehicleVO).insuranceRemainDays) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="年检到期" width="130">
          <template #default="{ row }">{{ (row as VehicleVO).inspectionExpire ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="年检剩余天数" width="130" align="right">
          <template #default="{ row }">
            <span :class="remainClass((row as VehicleVO).inspectionRemainDays)">
              {{ remainText((row as VehicleVO).inspectionRemainDays) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
      <el-alert v-else title="当前没有临近到期的保险或年检" type="success" :closable="false" show-icon />
    </div>

    <!-- 车辆筛选区 -->
    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="车牌号 / 品牌车型"
        clearable
        style="width: 220px"
        @keyup.enter="onSearch"
      />
      <el-select v-model="query.status" placeholder="车辆状态" clearable style="width: 140px">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button v-if="canManage" @click="openApply">用车申请</el-button>
      </div>
    </div>

    <!-- 车辆台账卡片 -->
    <div class="data-card panel-card">
      <div class="card-header">
        <div class="card-heading">
          <span class="card-title">车辆台账</span>
          <span class="card-count">共 {{ total }} 辆</span>
        </div>
        <el-button v-if="canManage" type="primary" @click="openCreate">新增车辆</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="plateNo" label="车牌号" width="130" />
        <el-table-column prop="brandModel" label="品牌车型" min-width="150">
          <template #default="{ row }">{{ (row as VehicleVO).brandModel ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="seats" label="座位数" width="90" align="right">
          <template #default="{ row }">{{ (row as VehicleVO).seats ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="保险到期" width="120">
          <template #default="{ row }">{{ (row as VehicleVO).insuranceExpire ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="年检到期" width="120">
          <template #default="{ row }">{{ (row as VehicleVO).inspectionExpire ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="vehicleTagType((row as VehicleVO).status)">
              {{ vehicleStatusLabels[(row as VehicleVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row as VehicleVO)">
              编辑
            </el-button>
            <el-button link type="primary" size="small" @click="openExpense(row as VehicleVO)">
              费用登记
            </el-button>
            <el-button link type="primary" size="small" @click="openExpenseList(row as VehicleVO)">
              查看费用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页右对齐，距表 16px -->
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

    <!-- 我的用车卡片 -->
    <div class="data-card panel-card">
      <div class="card-header">
        <div class="card-heading">
          <span class="card-title">我的用车</span>
          <span class="card-count">共 {{ myApplies.length }} 条</span>
        </div>
      </div>
      <el-table v-loading="myLoading" :data="myApplies" border>
        <el-table-column prop="vehiclePlateNo" label="车牌号" width="120" />
        <el-table-column prop="purpose" label="用车事由" min-width="140">
          <template #default="{ row }">{{ (row as VehicleApplyVO).purpose ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="destination" label="目的地" min-width="130">
          <template #default="{ row }">{{ (row as VehicleApplyVO).destination ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="用车时间" min-width="210">
          <template #default="{ row }">
            {{ formatDateTime((row as VehicleApplyVO).startTime) }} 至
            {{ formatDateTime((row as VehicleApplyVO).endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="applyTagType((row as VehicleApplyVO).status)">
              {{ vehicleApplyStatusLabels[(row as VehicleApplyVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="还车时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime((row as VehicleApplyVO).returnTime) }}
          </template>
        </el-table-column>
        <el-table-column label="里程" width="100" align="right">
          <template #default="{ row }">{{ (row as VehicleApplyVO).mileage ?? '-' }}</template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="
                (row as VehicleApplyVO).status === 'USING' ||
                (row as VehicleApplyVO).status === 'APPROVED'
              "
              link
              type="primary"
              size="small"
              @click="openReturn(row as VehicleApplyVO)"
            >
              还车
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新建 / 编辑车辆 -->
    <el-dialog
      v-model="formVisible"
      class="admin-dialog"
      :title="editingId ? '编辑车辆' : '新增车辆'"
      width="560px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="form.plateNo" :disabled="!!editingId" placeholder="请输入车牌号" />
        </el-form-item>
        <el-form-item label="品牌车型">
          <el-input v-model="form.brandModel" placeholder="品牌车型（可选）" />
        </el-form-item>
        <el-form-item label="座位数">
          <el-input-number v-model="form.seats" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker
            v-model="form.purchaseDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="保险到期">
          <el-date-picker
            v-model="form.insuranceExpire"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="年检到期">
          <el-date-picker
            v-model="form.inspectionExpire"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用车申请 -->
    <el-dialog v-model="applyVisible" class="admin-dialog" title="用车申请" width="560px">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="100px">
        <el-form-item label="车辆" prop="vehicleId">
          <el-select v-model="applyForm.vehicleId" filterable placeholder="请选择空闲车辆" style="width: 100%">
            <el-option
              v-for="item in applyVehicles"
              :key="item.id"
              :label="`${item.plateNo}（${item.brandModel ?? '未登记车型'}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用车事由">
          <el-input v-model="applyForm.purpose" placeholder="用车事由（可选）" />
        </el-form-item>
        <el-form-item label="目的地">
          <el-input v-model="applyForm.destination" placeholder="目的地（可选）" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="applyForm.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="applyForm.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 还车 -->
    <el-dialog v-model="returnVisible" class="admin-dialog" title="还车登记" width="480px">
      <el-form ref="returnFormRef" :model="returnForm" :rules="returnRules" label-width="100px">
        <el-form-item label="车辆">
          <span>{{ returnApply?.vehiclePlateNo ?? '-' }}</span>
        </el-form-item>
        <el-form-item label="还车时间">
          <el-date-picker
            v-model="returnForm.returnTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="还车里程" prop="mileage">
          <el-input-number v-model="returnForm.mileage" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReturn">确认还车</el-button>
      </template>
    </el-dialog>

    <!-- 费用登记 -->
    <el-dialog v-model="expenseVisible" class="admin-dialog" title="车辆费用登记" width="520px">
      <el-form :model="expenseForm" label-width="100px">
        <el-form-item label="车辆">
          <span>{{ expenseVehicle?.plateNo ?? '-' }}</span>
        </el-form-item>
        <el-form-item label="费用类型">
          <el-select v-model="expenseForm.expenseType" style="width: 100%">
            <el-option
              v-for="opt in expenseTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input v-model="expenseForm.amount" placeholder="费用金额（元）" />
        </el-form-item>
        <el-form-item label="发生日期">
          <el-date-picker
            v-model="expenseForm.occurDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="附件名称">
          <el-input v-model="expenseForm.attachmentName" placeholder="附件名称（可选）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="expenseForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="expenseVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExpense">确定</el-button>
      </template>
    </el-dialog>

    <!-- 费用明细 -->
    <el-dialog v-model="expenseListVisible" class="admin-dialog" title="车辆费用明细" width="720px">
      <el-table v-loading="expenseLoading" :data="expenses" border size="small">
        <el-table-column label="费用类型" width="120">
          <template #default="{ row }">
            {{ vehicleExpenseTypeLabels[(row as VehicleExpenseVO).expenseType] }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney((row as VehicleExpenseVO).amount) }}</template>
        </el-table-column>
        <el-table-column prop="occurDate" label="发生日期" width="120">
          <template #default="{ row }">{{ (row as VehicleExpenseVO).occurDate ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="attachmentName" label="附件" min-width="140">
          <template #default="{ row }">{{ (row as VehicleExpenseVO).attachmentName ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160">
          <template #default="{ row }">{{ (row as VehicleExpenseVO).remark ?? '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 模块容器：各卡片之间保持 16px 间距 */
.vehicle-panel {
  display: flex;
  flex-direction: column;
  gap: var(--ceams-space-4);
}

/* 筛选卡片：16px 20px 内边距，控件 12px 间距 */
.filter-bar {
  padding: 16px 20px;
  margin-bottom: 0;
  gap: var(--ceams-space-3);
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-3);
  margin-left: auto;
}

/* 列表卡片：20px 内边距 */
.panel-card {
  padding: var(--ceams-space-5);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-4);
}

.card-heading {
  display: flex;
  align-items: baseline;
  gap: var(--ceams-space-2);
  min-width: 0;
}

.card-title {
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-count {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
}

/* 分页右对齐，距表 16px */
.pagination {
  margin-top: var(--ceams-space-4);
  justify-content: flex-end;
}

/* 空态居中灰色、40px 留白 */
.panel-card :deep(.el-table__empty-text) {
  color: var(--ceams-text-placeholder);
}

.panel-card :deep(.el-table__empty-block) {
  padding: var(--ceams-space-10) 0;
  min-height: 0;
}

/* 到期提醒使用语义功能色 */
.remain-danger {
  color: var(--ceams-color-danger);
  font-weight: 600;
}

.remain-warning {
  color: var(--ceams-color-warning);
  font-weight: 600;
}

/* 弹窗内容 20px 24px */
:global(.admin-dialog .el-dialog__body) {
  padding: 20px 24px;
}
</style>
