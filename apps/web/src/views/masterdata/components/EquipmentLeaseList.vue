<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { Ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElEmpty,
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
import type { EquipmentLeaseVO, PaymentStatus } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { paymentStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'EquipmentLeaseList' })

const paymentOptions = toOptions(paymentStatusLabels)

/** 下拉选项：id 为雪花 ID 字符串，label 为展示名称。 */
interface SelectOption {
  id: string
  label: string
}

const equipmentOptions = ref<SelectOption[]>([])
const projectOptions = ref<SelectOption[]>([])
const lessorOptions = ref<SelectOption[]>([])

/** 选项列表未覆盖当前记录时补一条，避免编辑时下拉退化成 ID。 */
function keepOption(options: Ref<SelectOption[]>, id?: string | null, label?: string | null): void {
  if (!id || options.value.some((item) => item.id === id)) return
  options.value = [...options.value, { id, label: label || id }]
}

async function loadOptions(): Promise<void> {
  try {
    const [equipments, projects, contacts] = await Promise.all([
      ceamsApi.masterdata.equipmentPage({ page: 1, size: 100 }),
      ceamsApi.project.projectPage({ page: 1, size: 100 }),
      ceamsApi.masterdata.contactPage({ page: 1, size: 100 })
    ])
    equipmentOptions.value = equipments.records.map((item) => ({ id: item.id, label: item.name }))
    projectOptions.value = projects.records.map((item) => ({
      id: item.id,
      label: item.projectName
    }))
    lessorOptions.value = contacts.records.map((item) => ({ id: item.id, label: item.name }))
  } catch {
    // 选项加载失败不阻断列表展示，仍可通过名称回填展示
  }
}

const query = reactive<{ equipmentId?: string; projectId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<EquipmentLeaseVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<EquipmentLeaseVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<{
  equipmentId: string
  projectId: string
  lessorId: string
  leaseStartDate: string
  leaseEndDate: string
  dailyRent: string
  totalRent: string
  paymentStatus?: PaymentStatus
}>({
  equipmentId: '',
  projectId: '',
  lessorId: '',
  leaseStartDate: '',
  leaseEndDate: '',
  dailyRent: '',
  totalRent: '',
  paymentStatus: undefined
})

const rules: FormRules<typeof form> = {
  equipmentId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  leaseStartDate: [{ required: true, message: '请选择租赁开始日期', trigger: 'change' }],
  leaseEndDate: [{ required: true, message: '请选择租赁结束日期', trigger: 'change' }]
}

/** 付款状态标签配色：仅影响展示，不改变状态枚举与文案。 */
function paymentTagType(status?: PaymentStatus): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'PAID':
      return 'success'
    case 'PARTIAL':
      return 'warning'
    case 'UNPAID':
      return 'danger'
    default:
      return 'info'
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.masterdata.leasePage({
      page: page.value,
      size: size.value,
      equipmentId: query.equipmentId || undefined,
      projectId: query.projectId || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载设备租赁失败'))
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
    equipmentId: '',
    projectId: '',
    lessorId: '',
    leaseStartDate: '',
    leaseEndDate: '',
    dailyRent: '',
    totalRent: '',
    paymentStatus: undefined
  })
  dialogVisible.value = true
}

function openEdit(row: EquipmentLeaseVO): void {
  editingId.value = row.id
  keepOption(equipmentOptions, row.equipmentId, row.equipmentName)
  keepOption(projectOptions, row.projectId, row.projectName)
  keepOption(lessorOptions, row.lessorId, row.lessorName)
  form.equipmentId = row.equipmentId
  form.projectId = row.projectId
  form.lessorId = row.lessorId ?? ''
  form.leaseStartDate = row.leaseStartDate ?? ''
  form.leaseEndDate = row.leaseEndDate ?? ''
  form.dailyRent = row.dailyRent ?? ''
  form.totalRent = row.totalRent ?? ''
  form.paymentStatus = row.paymentStatus
  dialogVisible.value = true
}

function openDetail(row: EquipmentLeaseVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.masterdata.leaseUpdate(editingId.value, {
        projectId: form.projectId || undefined,
        lessorId: form.lessorId || undefined,
        leaseStartDate: form.leaseStartDate || undefined,
        leaseEndDate: form.leaseEndDate || undefined,
        dailyRent: form.dailyRent || undefined,
        totalRent: form.totalRent || undefined,
        paymentStatus: form.paymentStatus
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.masterdata.leaseCreate({
        equipmentId: form.equipmentId,
        projectId: form.projectId,
        lessorId: form.lessorId || null,
        leaseStartDate: form.leaseStartDate,
        leaseEndDate: form.leaseEndDate,
        dailyRent: form.dailyRent || undefined,
        totalRent: form.totalRent || undefined,
        paymentStatus: form.paymentStatus
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: EquipmentLeaseVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该设备租赁记录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.masterdata.leaseRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(() => {
  void loadOptions()
  void load()
})
</script>

<template>
  <div>
    <!-- 筛选卡片：控件 12px 间距，查询按钮靠右 -->
    <div class="data-card filter-card">
      <el-select v-model="query.equipmentId" placeholder="设备（可选）" clearable filterable style="width: 200px">
        <el-option v-for="opt in equipmentOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
      </el-select>
      <el-select v-model="query.projectId" placeholder="项目（可选）" clearable filterable style="width: 200px">
        <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部标题、条数与主操作，表格四周 20px 内边距 -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">设备租赁</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增租赁</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column label="设备" min-width="160">
          <template #default="{ row }">
            {{ (row as EquipmentLeaseVO).equipmentName || (row as EquipmentLeaseVO).equipmentId }}
          </template>
        </el-table-column>
        <el-table-column label="项目" min-width="160">
          <template #default="{ row }">
            {{ (row as EquipmentLeaseVO).projectName || (row as EquipmentLeaseVO).projectId }}
          </template>
        </el-table-column>
        <el-table-column label="出租方" min-width="160">
          <template #default="{ row }">
            {{ (row as EquipmentLeaseVO).lessorName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="leaseStartDate" label="开始日期" width="120" />
        <el-table-column prop="leaseEndDate" label="结束日期" width="120" />
        <el-table-column label="日租金" width="110" align="right">
          <template #default="{ row }">
            <span v-if="(row as EquipmentLeaseVO).dailyRent" class="amount">
              {{ (row as EquipmentLeaseVO).dailyRent }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="总租金" width="120" align="right">
          <template #default="{ row }">
            <span v-if="(row as EquipmentLeaseVO).totalRent" class="amount">
              {{ (row as EquipmentLeaseVO).totalRent }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="付款状态" width="110">
          <template #default="{ row }">
            <el-tag
              v-if="(row as EquipmentLeaseVO).paymentStatus"
              size="small"
              :type="paymentTagType((row as EquipmentLeaseVO).paymentStatus)"
            >
              {{ paymentStatusLabels[(row as EquipmentLeaseVO).paymentStatus!] }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as EquipmentLeaseVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as EquipmentLeaseVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as EquipmentLeaseVO)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无设备租赁数据" />
        </template>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑设备租赁' : '新增设备租赁'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="设备" prop="equipmentId">
          <el-select
            v-model="form.equipmentId"
            :disabled="!!editingId"
            placeholder="请选择设备"
            filterable
            style="width: 100%"
          >
            <el-option v-for="opt in equipmentOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" filterable style="width: 100%">
            <el-option v-for="opt in projectOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出租方">
          <el-select
            v-model="form.lessorId"
            placeholder="请选择出租方（可选）"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option v-for="opt in lessorOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="leaseStartDate">
          <el-date-picker v-model="form.leaseStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" prop="leaseEndDate">
          <el-date-picker v-model="form.leaseEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日租金">
          <el-input v-model="form.dailyRent" placeholder="金额（可选）" />
        </el-form-item>
        <el-form-item label="总租金">
          <el-input v-model="form.totalRent" placeholder="金额（可选）" />
        </el-form-item>
        <el-form-item label="付款状态">
          <el-select v-model="form.paymentStatus" clearable placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in paymentOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="设备租赁详情" width="540px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="设备">{{ detail.equipmentName || detail.equipmentId }}</el-descriptions-item>
        <el-descriptions-item label="项目">{{ detail.projectName || detail.projectId }}</el-descriptions-item>
        <el-descriptions-item label="出租方">{{ detail.lessorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail.leaseStartDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detail.leaseEndDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="日租金">{{ detail.dailyRent ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="总租金">{{ detail.totalRent ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="付款状态">
          {{ detail.paymentStatus ? paymentStatusLabels[detail.paymentStatus] : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 筛选卡片：控件 12px 间距，查询按钮靠右 */
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
  gap: var(--ceams-space-2);
  align-items: center;
  margin-left: auto;
}

/* 表格卡片：头部标题、条数与主操作，表格四周 20px 内边距 */
.table-card {
  padding: 20px;
}

.card-head {
  display: flex;
  flex-wrap: wrap;
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
  gap: var(--ceams-space-2);
  align-items: center;
}

/* 金额列：右对齐 + 等宽数字 */
.amount {
  display: inline-block;
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
  font-weight: 600;
  color: var(--ceams-text-primary);
}

/* 分页右对齐，距表格 16px */
.pagination {
  margin-top: var(--ceams-space-4);
  justify-content: flex-end;
}

/* 空态：居中灰色，上下 40px 留白 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}

:deep(.el-empty) {
  padding: 0;
}

/* 弹窗：标题加粗，内容区 20px 24px，底部操作条靠右并带顶部细边框 */
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
