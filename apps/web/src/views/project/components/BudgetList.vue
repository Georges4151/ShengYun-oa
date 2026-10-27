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
import type { BudgetStatus, BudgetVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { budgetStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'BudgetList' })

const statusOptions = toOptions(budgetStatusLabels)

const query = reactive<{ projectId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<BudgetVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<BudgetVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  budgetName: '',
  amount: '',
  projectId: '',
  contractId: '',
  startDate: '',
  endDate: '',
  status: 'DRAFT' as BudgetStatus
})

const rules: FormRules<typeof form> = {
  budgetName: [{ required: true, message: '请输入预算名称', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
  status: [{ required: true, message: '请选择预算状态', trigger: 'change' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.budgetPage({
      page: page.value,
      size: size.value,
      projectId: query.projectId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载预算失败'))
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
    budgetName: '',
    amount: '',
    projectId: '',
    contractId: '',
    startDate: '',
    endDate: '',
    status: 'DRAFT'
  })
  dialogVisible.value = true
}

function openEdit(row: BudgetVO): void {
  editingId.value = row.id
  form.budgetName = row.budgetName
  form.amount = row.amount
  form.projectId = row.projectId ?? ''
  form.contractId = row.contractId ?? ''
  form.startDate = row.startDate ?? ''
  form.endDate = row.endDate ?? ''
  form.status = row.status
  dialogVisible.value = true
}

function openDetail(row: BudgetVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.budgetUpdate(editingId.value, {
        budgetName: form.budgetName,
        amount: form.amount,
        projectId: form.projectId || undefined,
        contractId: form.contractId || undefined,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        status: form.status
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.budgetCreate({
        budgetName: form.budgetName,
        amount: form.amount,
        projectId: form.projectId || null,
        contractId: form.contractId || null,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        status: form.status
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: BudgetVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除预算「${row.budgetName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.budgetRemove(row.id)
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
      <el-input v-model="query.projectId" placeholder="项目 ID（可选）" clearable style="width: 220px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增预算</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="budgetName" label="预算名称" min-width="180" />
      <el-table-column prop="amount" label="预算金额" width="120" />
      <el-table-column prop="projectId" label="项目 ID" width="180" />
      <el-table-column prop="contractId" label="合同 ID" width="180" />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ budgetStatusLabels[(row as BudgetVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="usedAmount" label="已用金额" width="120" />
      <el-table-column prop="remainingAmount" label="剩余金额" width="120" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as BudgetVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as BudgetVO)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as BudgetVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑预算' : '新增预算'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="预算名称" prop="budgetName">
          <el-input v-model="form.budgetName" placeholder="请输入预算名称" />
        </el-form-item>
        <el-form-item label="预算金额" prop="amount">
          <el-input v-model="form.amount" placeholder="金额（字符串，如 500000.00）" />
        </el-form-item>
        <el-form-item label="项目 ID">
          <el-input v-model="form.projectId" placeholder="项目 ID（可选）" />
        </el-form-item>
        <el-form-item label="合同 ID">
          <el-input v-model="form.contractId" placeholder="合同 ID（可选）" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="预算详情" width="560px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="预算名称">{{ detail.budgetName }}</el-descriptions-item>
        <el-descriptions-item label="预算金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="项目 ID">{{ detail.projectId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同 ID">{{ detail.contractId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail.startDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detail.endDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ budgetStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="已用金额">{{ detail.usedAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="剩余金额">{{ detail.remainingAmount ?? '-' }}</el-descriptions-item>
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
