<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
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
import type { EquipmentLeaseVO, PaymentStatus } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { paymentStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'EquipmentLeaseList' })

const paymentOptions = toOptions(paymentStatusLabels)

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
  equipmentId: [{ required: true, message: '请输入设备 ID', trigger: 'blur' }],
  projectId: [{ required: true, message: '请输入项目 ID', trigger: 'blur' }],
  leaseStartDate: [{ required: true, message: '请选择租赁开始日期', trigger: 'change' }],
  leaseEndDate: [{ required: true, message: '请选择租赁结束日期', trigger: 'change' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.masterdata.leasePage({
      page: page.value,
      size: size.value,
      equipmentId: query.equipmentId,
      projectId: query.projectId
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

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.equipmentId" placeholder="设备 ID（可选）" clearable style="width: 200px" />
      <el-input v-model="query.projectId" placeholder="项目 ID（可选）" clearable style="width: 200px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增租赁</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="equipmentId" label="设备 ID" width="180" />
      <el-table-column prop="projectId" label="项目 ID" width="180" />
      <el-table-column prop="lessorId" label="出租方 ID" width="180" />
      <el-table-column prop="leaseStartDate" label="开始日期" width="120" />
      <el-table-column prop="leaseEndDate" label="结束日期" width="120" />
      <el-table-column prop="dailyRent" label="日租金" width="110" />
      <el-table-column prop="totalRent" label="总租金" width="120" />
      <el-table-column label="付款状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.paymentStatus" size="small">{{ paymentStatusLabels[(row as EquipmentLeaseVO).paymentStatus!] }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑设备租赁' : '新增设备租赁'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="设备 ID" prop="equipmentId">
          <el-input v-model="form.equipmentId" :disabled="!!editingId" placeholder="请输入设备 ID" />
        </el-form-item>
        <el-form-item label="项目 ID" prop="projectId">
          <el-input v-model="form.projectId" placeholder="请输入项目 ID" />
        </el-form-item>
        <el-form-item label="出租方 ID">
          <el-input v-model="form.lessorId" placeholder="往来单位 ID（可选）" />
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
        <el-descriptions-item label="设备 ID">{{ detail.equipmentId }}</el-descriptions-item>
        <el-descriptions-item label="项目 ID">{{ detail.projectId }}</el-descriptions-item>
        <el-descriptions-item label="出租方 ID">{{ detail.lessorId ?? '-' }}</el-descriptions-item>
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
