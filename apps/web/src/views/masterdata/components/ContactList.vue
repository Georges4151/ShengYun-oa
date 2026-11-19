<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
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
import type { ContactType, ContactVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { contactTypeLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'ContactList' })

const typeOptions = toOptions(contactTypeLabels)

const query = reactive<{ keyword?: string; type?: ContactType }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ContactVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<ContactVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  type: 'SUPPLIER' as ContactType,
  contactPerson: '',
  phone: '',
  email: '',
  address: '',
  taxIdentificationNumber: '',
  bankAccount: ''
})

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择单位类型', trigger: 'change' }]
}

/** 单位类型标签配色：仅影响展示，不改变类型枚举与文案。 */
function contactTypeTagType(type: ContactType): 'primary' | 'success' | 'warning' | 'info' {
  switch (type) {
    case 'SUPPLIER':
      return 'primary'
    case 'CUSTOMER':
      return 'success'
    case 'SUBCONTRACTOR':
      return 'warning'
    default:
      return 'info'
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.masterdata.contactPage({
      page: page.value,
      size: size.value,
      keyword: query.keyword,
      type: query.type
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载往来单位失败'))
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
    type: 'SUPPLIER',
    contactPerson: '',
    phone: '',
    email: '',
    address: '',
    taxIdentificationNumber: '',
    bankAccount: ''
  })
  dialogVisible.value = true
}

function openEdit(row: ContactVO): void {
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.contactPerson = row.contactPerson ?? ''
  form.phone = row.phone ?? ''
  form.email = row.email ?? ''
  form.address = row.address ?? ''
  form.taxIdentificationNumber = row.taxIdentificationNumber ?? ''
  form.bankAccount = row.bankAccount ?? ''
  dialogVisible.value = true
}

function openDetail(row: ContactVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.masterdata.contactUpdate(editingId.value, {
        name: form.name,
        type: form.type,
        contactPerson: form.contactPerson || undefined,
        phone: form.phone || undefined,
        email: form.email || undefined,
        address: form.address || undefined,
        taxIdentificationNumber: form.taxIdentificationNumber || undefined,
        bankAccount: form.bankAccount || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.masterdata.contactCreate({
        name: form.name,
        type: form.type,
        contactPerson: form.contactPerson || null,
        phone: form.phone || null,
        email: form.email || null,
        address: form.address || null,
        taxIdentificationNumber: form.taxIdentificationNumber || null,
        bankAccount: form.bankAccount || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: ContactVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除往来单位「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.masterdata.contactRemove(row.id)
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
    <!-- 筛选卡片：控件 12px 间距，查询按钮靠右 -->
    <div class="data-card filter-card">
      <el-input v-model="query.keyword" placeholder="关键字（名称/联系人/电话）" clearable style="width: 220px" />
      <el-select v-model="query.type" placeholder="单位类型" clearable style="width: 160px">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部标题、条数与主操作，表格四周 20px 内边距 -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">往来单位</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增单位</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="name" label="单位名称" min-width="180" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="contactTypeTagType((row as ContactVO).type)">
              {{ contactTypeLabels[(row as ContactVO).type] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="120" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as ContactVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as ContactVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as ContactVO)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无往来单位数据" />
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑往来单位' : '新增往来单位'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="单位名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入单位名称" />
        </el-form-item>
        <el-form-item label="单位类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" placeholder="联系人" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="地址" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="form.taxIdentificationNumber" placeholder="税号" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="form.bankAccount" placeholder="银行账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="往来单位详情" width="560px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="单位名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ contactTypeLabels[detail.type] }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactPerson ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ detail.phone ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.address ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="税号">{{ detail.taxIdentificationNumber ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="银行账号">{{ detail.bankAccount ?? '-' }}</el-descriptions-item>
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
