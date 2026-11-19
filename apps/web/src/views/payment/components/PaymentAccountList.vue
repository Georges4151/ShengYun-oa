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
  ElPagination,
  ElTable,
  ElTableColumn
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { PaymentAccountVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'PaymentAccountList' })

const query = reactive<{ name?: string; type?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<PaymentAccountVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<PaymentAccountVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  type: '',
  balance: '',
  accountNumber: '',
  description: ''
})

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请输入账户类型', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.payment.paymentAccountPage({
      page: page.value,
      size: size.value,
      name: query.name,
      type: query.type
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载付款账户失败'))
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
  Object.assign(form, { name: '', type: '', balance: '', accountNumber: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row: PaymentAccountVO): void {
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.balance = row.balance ?? ''
  form.accountNumber = row.accountNumber ?? ''
  form.description = row.description ?? ''
  dialogVisible.value = true
}

function openDetail(row: PaymentAccountVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.payment.paymentAccountUpdate(editingId.value, {
        name: form.name,
        type: form.type,
        accountNumber: form.accountNumber || null,
        description: form.description || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.payment.paymentAccountCreate({
        name: form.name,
        type: form.type,
        balance: form.balance || null,
        accountNumber: form.accountNumber || null,
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

async function onRemove(row: PaymentAccountVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除付款账户「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.payment.paymentAccountRemove(row.id)
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
      <el-input v-model="query.name" placeholder="账户名称" clearable style="width: 200px" />
      <el-input v-model="query.type" placeholder="账户类型" clearable style="width: 160px" />
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="openCreate">新增账户</el-button>
      </div>
    </div>

    <div class="data-card">
      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="name" label="账户名称" min-width="160" />
        <el-table-column prop="type" label="账户类型" width="120" />
        <el-table-column prop="balance" label="余额" width="130" align="right" class-name="amount-col" />
        <el-table-column prop="accountNumber" label="账号" width="160" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as PaymentAccountVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as PaymentAccountVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as PaymentAccountVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑付款账户' : '新增付款账户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="账户名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入账户名称" />
        </el-form-item>
        <el-form-item label="账户类型" prop="type">
          <el-input v-model="form.type" placeholder="账户类型（代码值）" />
        </el-form-item>
        <el-form-item label="初始余额">
          <el-input v-model="form.balance" :disabled="!!editingId" placeholder="余额只读（编辑时不外改）" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="form.accountNumber" placeholder="账号【需加密】（阶段4）" />
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

    <el-dialog v-model="detailVisible" title="付款账户详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="账户名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="账户类型">{{ detail.type }}</el-descriptions-item>
        <el-descriptions-item label="余额">{{ detail.balance ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ detail.accountNumber ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detail.createTime ?? '-' }}</el-descriptions-item>
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
