<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
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
import type { CostAllocationVO, TargetType } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { targetTypeLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'CostAllocationList' })

const targetTypeOptions = toOptions(targetTypeLabels)

const query = reactive<{ accountRecordId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<CostAllocationVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<CostAllocationVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  accountRecordId: '',
  targetType: 'PROJECT' as TargetType,
  targetId: '',
  allocationAmount: '',
  allocationRatio: ''
})

const rules: FormRules<typeof form> = {
  accountRecordId: [{ required: true, message: '请输入流水记录 ID', trigger: 'blur' }],
  targetType: [{ required: true, message: '请选择分摊目标类型', trigger: 'change' }],
  targetId: [{ required: true, message: '请输入分摊目标 ID', trigger: 'blur' }],
  allocationAmount: [{ required: true, message: '请输入分摊金额', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.payment.costAllocationPage({
      page: page.value,
      size: size.value,
      accountRecordId: query.accountRecordId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载成本分摊失败'))
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
    accountRecordId: '',
    targetType: 'PROJECT',
    targetId: '',
    allocationAmount: '',
    allocationRatio: ''
  })
  dialogVisible.value = true
}

function openEdit(row: CostAllocationVO): void {
  editingId.value = row.id
  form.accountRecordId = row.accountRecordId
  form.targetType = row.targetType
  form.targetId = row.targetId
  form.allocationAmount = row.allocationAmount
  form.allocationRatio = row.allocationRatio ?? ''
  dialogVisible.value = true
}

function openDetail(row: CostAllocationVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.payment.costAllocationUpdate(editingId.value, {
        targetType: form.targetType,
        targetId: form.targetId,
        allocationAmount: form.allocationAmount,
        allocationRatio: form.allocationRatio || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.payment.costAllocationCreate({
        accountRecordId: form.accountRecordId,
        targetType: form.targetType,
        targetId: form.targetId,
        allocationAmount: form.allocationAmount,
        allocationRatio: form.allocationRatio || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: CostAllocationVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该成本分摊记录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.payment.costAllocationRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(load)
</script>

<template>
  <div class="list-page">
    <div class="filter-bar">
      <el-input v-model="query.accountRecordId" placeholder="流水记录 ID" clearable style="width: 220px" />
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="openCreate">新增分摊</el-button>
      </div>
    </div>

    <div class="data-card">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="accountRecordId" label="流水记录 ID" width="180" />
        <el-table-column label="目标类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ targetTypeLabels[(row as CostAllocationVO).targetType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标 ID" width="180" />
        <el-table-column prop="allocationAmount" label="分摊金额" width="130" align="right" class-name="amount-col" />
        <el-table-column prop="allocationRatio" label="分摊比例" width="110" align="right" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as CostAllocationVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as CostAllocationVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as CostAllocationVO)">删除</el-button>
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
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑成本分摊' : '新增成本分摊'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="流水记录 ID" prop="accountRecordId">
          <el-input v-model="form.accountRecordId" :disabled="!!editingId" placeholder="请输入流水记录 ID" />
        </el-form-item>
        <el-form-item label="目标类型" prop="targetType">
          <el-select v-model="form.targetType" style="width: 100%">
            <el-option v-for="opt in targetTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标 ID" prop="targetId">
          <el-input v-model="form.targetId" placeholder="请输入分摊目标 ID" />
        </el-form-item>
        <el-form-item label="分摊金额" prop="allocationAmount">
          <el-input v-model="form.allocationAmount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="分摊比例">
          <el-input v-model="form.allocationRatio" placeholder="比例（可选，字符串）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="成本分摊详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="流水记录 ID">{{ detail.accountRecordId }}</el-descriptions-item>
        <el-descriptions-item label="目标类型">{{ targetTypeLabels[detail.targetType] }}</el-descriptions-item>
        <el-descriptions-item label="目标 ID">{{ detail.targetId }}</el-descriptions-item>
        <el-descriptions-item label="分摊金额">{{ detail.allocationAmount }}</el-descriptions-item>
        <el-descriptions-item label="分摊比例">{{ detail.allocationRatio ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
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
/* 金额列使用等宽数字，避免数字跳动 */
:deep(.amount-col .cell) {
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}
.pagination {
  margin-top: 16px;
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
</style>
