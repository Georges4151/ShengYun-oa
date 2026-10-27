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
  ElPagination,
  ElTable,
  ElTableColumn
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { SettlementProofVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'SettlementProofList' })

const query = reactive<{ contractId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<SettlementProofVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<SettlementProofVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  contractId: '',
  subcontractorId: '',
  proofName: '',
  proofNumber: '',
  proofDate: '',
  settlementAmount: '',
  description: ''
})

const rules: FormRules<typeof form> = {
  contractId: [{ required: true, message: '请输入合同 ID', trigger: 'blur' }],
  proofName: [{ required: true, message: '请输入凭证名称', trigger: 'blur' }],
  proofNumber: [{ required: true, message: '请输入凭证编号', trigger: 'blur' }],
  settlementAmount: [{ required: true, message: '请输入结算金额', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.proofPage({
      page: page.value,
      size: size.value,
      contractId: query.contractId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载结算凭证失败'))
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
    contractId: '',
    subcontractorId: '',
    proofName: '',
    proofNumber: '',
    proofDate: '',
    settlementAmount: '',
    description: ''
  })
  dialogVisible.value = true
}

function openEdit(row: SettlementProofVO): void {
  editingId.value = row.id
  form.contractId = row.contractId
  form.subcontractorId = row.subcontractorId ?? ''
  form.proofName = row.proofName
  form.proofNumber = row.proofNumber
  form.proofDate = row.proofDate ?? ''
  form.settlementAmount = row.settlementAmount
  form.description = row.description ?? ''
  dialogVisible.value = true
}

function openDetail(row: SettlementProofVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.proofUpdate(editingId.value, {
        proofName: form.proofName,
        proofNumber: form.proofNumber,
        proofDate: form.proofDate || undefined,
        settlementAmount: form.settlementAmount,
        description: form.description || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.proofCreate({
        contractId: form.contractId,
        subcontractorId: form.subcontractorId || null,
        proofName: form.proofName,
        proofNumber: form.proofNumber,
        proofDate: form.proofDate || undefined,
        settlementAmount: form.settlementAmount,
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

async function onRemove(row: SettlementProofVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除结算凭证「${row.proofName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.proofRemove(row.id)
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
      <el-input v-model="query.contractId" placeholder="合同 ID（可选）" clearable style="width: 220px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增凭证</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="proofName" label="凭证名称" min-width="180" />
      <el-table-column prop="proofNumber" label="凭证编号" width="160" />
      <el-table-column prop="contractId" label="合同 ID" width="180" />
      <el-table-column prop="subcontractorId" label="分包商 ID" width="180" />
      <el-table-column prop="proofDate" label="凭证日期" width="120" />
      <el-table-column prop="settlementAmount" label="结算金额" width="120" />
      <el-table-column prop="fileName" label="附件" width="140" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as SettlementProofVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as SettlementProofVO)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as SettlementProofVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑结算凭证' : '新增结算凭证'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="合同 ID" prop="contractId">
          <el-input v-model="form.contractId" :disabled="!!editingId" placeholder="请输入合同 ID" />
        </el-form-item>
        <el-form-item label="分包商 ID">
          <el-input v-model="form.subcontractorId" placeholder="分包商 ID（可选）" />
        </el-form-item>
        <el-form-item label="凭证名称" prop="proofName">
          <el-input v-model="form.proofName" placeholder="请输入凭证名称" />
        </el-form-item>
        <el-form-item label="凭证编号" prop="proofNumber">
          <el-input v-model="form.proofNumber" placeholder="请输入凭证编号" />
        </el-form-item>
        <el-form-item label="凭证日期">
          <el-date-picker v-model="form.proofDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结算金额" prop="settlementAmount">
          <el-input v-model="form.settlementAmount" placeholder="金额（字符串）" />
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

    <el-dialog v-model="detailVisible" title="结算凭证详情" width="540px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="凭证名称">{{ detail.proofName }}</el-descriptions-item>
        <el-descriptions-item label="凭证编号">{{ detail.proofNumber }}</el-descriptions-item>
        <el-descriptions-item label="合同 ID">{{ detail.contractId }}</el-descriptions-item>
        <el-descriptions-item label="分包商 ID">{{ detail.subcontractorId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="凭证日期">{{ detail.proofDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="结算金额">{{ detail.settlementAmount }}</el-descriptions-item>
        <el-descriptions-item label="附件">{{ detail.fileName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ detail.description ?? '-' }}</el-descriptions-item>
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
