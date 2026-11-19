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
import type { SubcontractorVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'SubcontractorList' })

const query = reactive<{ keyword?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<SubcontractorVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<SubcontractorVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  contactPerson: '',
  phone: '',
  address: '',
  qualificationLevel: '',
  creditRating: '',
  businessScope: '',
  taxIdentificationNumber: '',
  bankAccount: '',
  description: ''
})

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入分包商名称', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.subcontractorPage({
      page: page.value,
      size: size.value,
      keyword: query.keyword
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载分包商失败'))
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
    name: '',
    contactPerson: '',
    phone: '',
    address: '',
    qualificationLevel: '',
    creditRating: '',
    businessScope: '',
    taxIdentificationNumber: '',
    bankAccount: '',
    description: ''
  })
  dialogVisible.value = true
}

function openEdit(row: SubcontractorVO): void {
  editingId.value = row.id
  form.name = row.name
  form.contactPerson = row.contactPerson ?? ''
  form.phone = row.phone ?? ''
  form.address = row.address ?? ''
  form.qualificationLevel = row.qualificationLevel ?? ''
  form.creditRating = row.creditRating ?? ''
  form.businessScope = row.businessScope ?? ''
  form.taxIdentificationNumber = row.taxIdentificationNumber ?? ''
  form.bankAccount = row.bankAccount ?? ''
  form.description = row.description ?? ''
  dialogVisible.value = true
}

function openDetail(row: SubcontractorVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.subcontractorUpdate(editingId.value, {
        name: form.name,
        contactPerson: form.contactPerson || undefined,
        phone: form.phone || undefined,
        address: form.address || undefined,
        qualificationLevel: form.qualificationLevel || undefined,
        creditRating: form.creditRating || undefined,
        businessScope: form.businessScope || undefined,
        taxIdentificationNumber: form.taxIdentificationNumber || undefined,
        bankAccount: form.bankAccount || undefined,
        description: form.description || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.subcontractorCreate({
        name: form.name,
        contactPerson: form.contactPerson || null,
        phone: form.phone || null,
        address: form.address || null,
        qualificationLevel: form.qualificationLevel || null,
        creditRating: form.creditRating || null,
        businessScope: form.businessScope || null,
        taxIdentificationNumber: form.taxIdentificationNumber || null,
        bankAccount: form.bankAccount || null,
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

async function onRemove(row: SubcontractorVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除分包商「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.subcontractorRemove(row.id)
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
    <!-- 筛选卡片：控件 12px 间距，按钮组靠右 -->
    <div class="data-card filter-card">
      <el-input v-model="query.keyword" placeholder="关键字（名称/联系人）" clearable style="width: 220px" />
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部标题、条数与主操作 -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">分包商列表</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增分包商</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="name" label="分包商名称" min-width="180" />
        <el-table-column prop="contactPerson" label="联系人" width="120" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="qualificationLevel" label="资质等级" width="120" />
        <el-table-column prop="creditRating" label="信用等级" width="120" />
        <el-table-column prop="businessScope" label="经营范围" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as SubcontractorVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as SubcontractorVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as SubcontractorVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分包商' : '新增分包商'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="分包商名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分包商名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" placeholder="联系人" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="电话" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="地址" />
        </el-form-item>
        <el-form-item label="资质等级">
          <el-input v-model="form.qualificationLevel" placeholder="如：一级 / 二级" />
        </el-form-item>
        <el-form-item label="信用等级">
          <el-input v-model="form.creditRating" placeholder="如：AAA" />
        </el-form-item>
        <el-form-item label="经营范围">
          <el-input v-model="form.businessScope" placeholder="经营范围" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="form.taxIdentificationNumber" placeholder="税号" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="form.bankAccount" placeholder="银行账号" />
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

    <el-dialog v-model="detailVisible" title="分包商详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactPerson ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ detail.phone ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.address ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="资质等级">{{ detail.qualificationLevel ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="信用等级">{{ detail.creditRating ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="税号">{{ detail.taxIdentificationNumber ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="银行账号">{{ detail.bankAccount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="经营范围" :span="2">{{ detail.businessScope ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 筛选卡片：16px 20px 内边距，控件 12px 间距，按钮组靠右 */
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
  align-items: center;
  gap: var(--ceams-space-2);
  margin-left: auto;
}

/* 表格卡片：20px 内边距，头部放标题、条数与主操作 */
.table-card {
  padding: 20px;
}

.card-head {
  display: flex;
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
  align-items: center;
  gap: var(--ceams-space-2);
}

/* 分页右对齐，距表格 16px */
.pagination {
  justify-content: flex-end;
  margin-top: var(--ceams-space-4);
}

/* 空态居中灰色，上下 40px 留白 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}

/* 弹窗：标题加粗、内容区 20px 24px、底部操作条靠右并带顶部细边框 */
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
