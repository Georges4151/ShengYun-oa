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
import type { ApprovalStatus, ProgressPaymentVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { approvalStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'ProgressPaymentList' })

const statusOptions = toOptions(approvalStatusLabels)

const query = reactive<{ projectId?: string; status?: ApprovalStatus }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ProgressPaymentVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<ProgressPaymentVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  projectId: '',
  paymentPeriod: '',
  applicationAmount: '',
  applicationDate: ''
})

const rules: FormRules<typeof form> = {
  projectId: [{ required: true, message: '请输入项目 ID', trigger: 'blur' }],
  paymentPeriod: [{ required: true, message: '请输入付款期次', trigger: 'blur' }],
  applicationAmount: [{ required: true, message: '请输入申请金额', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.paymentPage({
      page: page.value,
      size: size.value,
      projectId: query.projectId,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载进度款失败'))
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
  Object.assign(form, { projectId: '', paymentPeriod: '', applicationAmount: '', applicationDate: '' })
  dialogVisible.value = true
}

function openEdit(row: ProgressPaymentVO): void {
  editingId.value = row.id
  form.projectId = row.projectId
  form.paymentPeriod = row.paymentPeriod
  form.applicationAmount = row.applicationAmount ?? ''
  form.applicationDate = row.applicationDate ?? ''
  dialogVisible.value = true
}

function openDetail(row: ProgressPaymentVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.paymentUpdate(editingId.value, {
        paymentPeriod: form.paymentPeriod,
        applicationAmount: form.applicationAmount || undefined,
        applicationDate: form.applicationDate || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.paymentCreate({
        projectId: form.projectId,
        paymentPeriod: form.paymentPeriod,
        applicationAmount: form.applicationAmount,
        applicationDate: form.applicationDate || undefined
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: ProgressPaymentVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该进度款记录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.paymentRemove(row.id)
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
      <el-input v-model="query.projectId" placeholder="项目 ID（可选）" clearable style="width: 200px" />
      <el-select v-model="query.status" placeholder="审批状态" clearable style="width: 160px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增进度款</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="projectId" label="项目 ID" width="180" />
      <el-table-column prop="paymentPeriod" label="付款期次" width="120" />
      <el-table-column prop="applicationAmount" label="申请金额" width="120" />
      <el-table-column prop="approvedAmount" label="批准金额" width="120" />
      <el-table-column prop="paymentAmount" label="实付金额" width="120" />
      <el-table-column prop="applicationDate" label="申请日期" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ approvalStatusLabels[(row as ProgressPaymentVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applicationBy" label="申请人" width="110" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as ProgressPaymentVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as ProgressPaymentVO)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as ProgressPaymentVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑进度款' : '新增进度款'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目 ID" prop="projectId">
          <el-input v-model="form.projectId" :disabled="!!editingId" placeholder="请输入项目 ID" />
        </el-form-item>
        <el-form-item label="付款期次" prop="paymentPeriod">
          <el-input v-model="form.paymentPeriod" placeholder="如：第 1 期" />
        </el-form-item>
        <el-form-item label="申请金额" prop="applicationAmount">
          <el-input v-model="form.applicationAmount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="申请日期">
          <el-date-picker v-model="form.applicationDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="进度款详情" width="520px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="项目 ID">{{ detail.projectId }}</el-descriptions-item>
        <el-descriptions-item label="付款期次">{{ detail.paymentPeriod }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ detail.applicationAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="批准金额">{{ detail.approvedAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ detail.paymentAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请日期">{{ detail.applicationDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ approvalStatusLabels[detail.status] }}</el-descriptions-item>
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
