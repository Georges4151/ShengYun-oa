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
import type { ContractStatus, SubcontractorContractVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { contractStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'SubcontractorContractList' })

const statusOptions = toOptions(contractStatusLabels)

const query = reactive<{ subcontractorId?: string; projectId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<SubcontractorContractVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<SubcontractorContractVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<{
  subcontractorId: string
  projectId: string
  contractName: string
  contractNumber: string
  contractAmount: string
  startDate: string
  endDate: string
  status?: ContractStatus
  description: string
}>({
  subcontractorId: '',
  projectId: '',
  contractName: '',
  contractNumber: '',
  contractAmount: '',
  startDate: '',
  endDate: '',
  status: undefined,
  description: ''
})

const rules: FormRules<typeof form> = {
  subcontractorId: [{ required: true, message: '请输入分包商 ID', trigger: 'blur' }],
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  contractNumber: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  contractAmount: [{ required: true, message: '请输入合同金额', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.contractPage({
      page: page.value,
      size: size.value,
      subcontractorId: query.subcontractorId,
      projectId: query.projectId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载分包合同失败'))
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
    subcontractorId: '',
    projectId: '',
    contractName: '',
    contractNumber: '',
    contractAmount: '',
    startDate: '',
    endDate: '',
    status: undefined,
    description: ''
  })
  dialogVisible.value = true
}

function openEdit(row: SubcontractorContractVO): void {
  editingId.value = row.id
  form.subcontractorId = row.subcontractorId
  form.projectId = row.projectId ?? ''
  form.contractName = row.contractName
  form.contractNumber = row.contractNumber
  form.contractAmount = row.contractAmount
  form.startDate = row.startDate ?? ''
  form.endDate = row.endDate ?? ''
  form.status = row.status
  form.description = row.description ?? ''
  dialogVisible.value = true
}

function openDetail(row: SubcontractorContractVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.contractUpdate(editingId.value, {
        projectId: form.projectId || undefined,
        contractName: form.contractName,
        contractAmount: form.contractAmount,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        status: form.status,
        description: form.description || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.contractCreate({
        subcontractorId: form.subcontractorId,
        projectId: form.projectId || null,
        contractName: form.contractName,
        contractNumber: form.contractNumber,
        contractAmount: form.contractAmount,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        status: form.status,
        description: form.description || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: SubcontractorContractVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除分包合同「${row.contractName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.contractRemove(row.id)
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
      <el-input v-model="query.subcontractorId" placeholder="分包商 ID（可选）" clearable style="width: 200px" />
      <el-input v-model="query.projectId" placeholder="项目 ID（可选）" clearable style="width: 200px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增合同</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="contractName" label="合同名称" min-width="180" />
      <el-table-column prop="contractNumber" label="合同编号" width="160" />
      <el-table-column prop="subcontractorId" label="分包商 ID" width="180" />
      <el-table-column prop="projectId" label="项目 ID" width="180" />
      <el-table-column prop="contractAmount" label="合同金额" width="120" />
      <el-table-column prop="paidAmount" label="已付金额" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.status" size="small">{{ contractStatusLabels[(row as SubcontractorContractVO).status!] }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as SubcontractorContractVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as SubcontractorContractVO)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as SubcontractorContractVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分包合同' : '新增分包合同'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="分包商 ID" prop="subcontractorId">
          <el-input v-model="form.subcontractorId" :disabled="!!editingId" placeholder="请输入分包商 ID" />
        </el-form-item>
        <el-form-item label="项目 ID">
          <el-input v-model="form.projectId" placeholder="项目 ID（可选）" />
        </el-form-item>
        <el-form-item label="合同名称" prop="contractName">
          <el-input v-model="form.contractName" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="合同编号" prop="contractNumber">
          <el-input v-model="form.contractNumber" :disabled="!!editingId" placeholder="请输入合同编号" />
        </el-form-item>
        <el-form-item label="合同金额" prop="contractAmount">
          <el-input v-model="form.contractAmount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="合同状态">
          <el-select v-model="form.status" clearable placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="分包合同详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="合同名称">{{ detail.contractName }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ detail.contractNumber }}</el-descriptions-item>
        <el-descriptions-item label="分包商 ID">{{ detail.subcontractorId }}</el-descriptions-item>
        <el-descriptions-item label="项目 ID">{{ detail.projectId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同金额">{{ detail.contractAmount }}</el-descriptions-item>
        <el-descriptions-item label="已付金额">{{ detail.paidAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detail.status ? contractStatusLabels[detail.status] : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail.startDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detail.endDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ detail.fileName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
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
