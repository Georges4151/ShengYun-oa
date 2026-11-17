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
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type {
  ContractVO,
  EmployeeVO,
  HrContractStatus,
  HrContractType
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { hrContractStatusLabels, hrContractTypeLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'ContractList' })

/** 到期预警天数 */
const EXPIRING_DAYS = 30

const statusOptions = toOptions(hrContractStatusLabels)
const typeOptions = toOptions(hrContractTypeLabels)

const query = reactive<{ status: HrContractStatus | '' }>({ status: '' })
const { page, size, total, resetPage } = usePage()
const list = ref<ContractVO[]>([])
const loading = ref(false)
const expiringCount = ref(0)

const employees = ref<EmployeeVO[]>([])
const employeeNameMap = computed(
  () => new Map(employees.value.map((item) => [item.id, item.name]))
)

function employeeName(id: string): string {
  return employeeNameMap.value.get(id) ?? id
}

function statusTagType(status: HrContractStatus): 'success' | 'info' | 'danger' {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'EXPIRED':
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
    // 员工下拉加载失败不阻断合同列表
  }
}

async function loadExpiring(): Promise<void> {
  try {
    expiringCount.value = (await ceamsApi.hr.contractExpiring(EXPIRING_DAYS)).length
  } catch {
    // 预警数量加载失败不阻断合同列表
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.hr.contractPage({
      page: page.value,
      size: size.value,
      status: query.status || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载合同台账失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  employeeId: '',
  contractNo: '',
  contractType: 'FIXED' as HrContractType,
  startDate: '',
  endDate: '',
  signDate: '',
  renewalRemindDate: '',
  status: 'ACTIVE' as HrContractStatus,
  fileId: '',
  remark: ''
})

const rules: FormRules<typeof form> = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  contractType: [{ required: true, message: '请选择合同类型', trigger: 'change' }]
}

function resetForm(): void {
  Object.assign(form, {
    employeeId: '',
    contractNo: '',
    contractType: 'FIXED',
    startDate: '',
    endDate: '',
    signDate: '',
    renewalRemindDate: '',
    status: 'ACTIVE',
    fileId: '',
    remark: ''
  })
}

function openCreate(): void {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ContractVO): void {
  editingId.value = row.id
  resetForm()
  form.employeeId = row.employeeId
  form.contractNo = row.contractNo
  form.contractType = row.contractType
  form.startDate = row.startDate ?? ''
  form.endDate = row.endDate ?? ''
  form.signDate = row.signDate ?? ''
  form.renewalRemindDate = row.renewalRemindDate ?? ''
  form.status = row.status
  form.fileId = row.fileId ?? ''
  form.remark = row.remark ?? ''
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.hr.contractUpdate(editingId.value, {
        contractNo: form.contractNo,
        contractType: form.contractType,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        signDate: form.signDate || undefined,
        renewalRemindDate: form.renewalRemindDate || undefined,
        status: form.status,
        fileId: form.fileId || undefined,
        remark: form.remark || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.hr.contractCreate({
        employeeId: form.employeeId,
        contractNo: form.contractNo,
        contractType: form.contractType,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        signDate: form.signDate || undefined,
        renewalRemindDate: form.renewalRemindDate || undefined,
        status: form.status,
        fileId: form.fileId || undefined,
        remark: form.remark || undefined
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await Promise.all([load(), loadExpiring()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存合同失败'))
  }
}

onMounted(() => {
  void loadEmployees()
  void loadExpiring()
  void load()
})
</script>

<template>
  <div>
    <el-alert
      v-if="expiringCount > 0"
      :title="`近 ${EXPIRING_DAYS} 天内有 ${expiringCount} 份劳动合同即将到期，请及时续签`"
      type="warning"
      show-icon
      :closable="false"
      class="expiring-alert"
    />

    <div class="toolbar">
      <el-select v-model="query.status" placeholder="合同状态" clearable style="width: 160px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增合同</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="contractNo" label="合同编号" width="160" />
      <el-table-column label="员工" width="120">
        <template #default="{ row }">{{ employeeName((row as ContractVO).employeeId) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          {{ hrContractTypeLabels[(row as ContractVO).contractType] }}
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="signDate" label="签订日期" width="120" />
      <el-table-column prop="renewalRemindDate" label="续签提醒" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType((row as ContractVO).status)">
            {{ hrContractStatusLabels[(row as ContractVO).status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row as ContractVO)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑合同' : '新增合同'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="员工" prop="employeeId">
          <el-select
            v-model="form.employeeId"
            placeholder="请选择员工"
            filterable
            :disabled="!!editingId"
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
        <el-form-item label="合同编号" prop="contractNo">
          <el-input v-model="form.contractNo" placeholder="请输入合同编号" />
        </el-form-item>
        <el-form-item label="合同类型" prop="contractType">
          <el-select v-model="form.contractType" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="签订日期">
          <el-date-picker
            v-model="form.signDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="续签提醒">
          <el-date-picker
            v-model="form.renewalRemindDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="合同状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件 ID">
          <!-- 前端暂未接入统一文件上传组件，先以文本形式填写后端 fileId -->
          <el-input v-model="form.fileId" placeholder="文件 ID（可选，来自文件服务）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.expiring-alert {
  margin-bottom: 12px;
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
</style>
