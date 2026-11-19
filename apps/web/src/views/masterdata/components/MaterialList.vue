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
  ElTreeSelect
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { CategoryVO, ContactVO, MaterialVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'MaterialList' })

const query = reactive<{ keyword?: string; categoryId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<MaterialVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<MaterialVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  materialCode: '',
  materialName: '',
  specification: '',
  unit: '',
  currentPrice: '',
  supplierId: '',
  categoryId: ''
})

const rules: FormRules<typeof form> = {
  materialCode: [{ required: true, message: '请输入材料编码', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入材料名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }]
}

const supplierOptions = ref<ContactVO[]>([])
const categoryOptions = ref<CategoryVO[]>([])
const supplierLoading = ref(false)

async function loadSuppliers(keyword = ''): Promise<void> {
  supplierLoading.value = true
  try {
    const res = await ceamsApi.masterdata.contactPage({
      page: 1,
      size: 20,
      type: 'SUPPLIER',
      keyword: keyword || undefined
    })
    supplierOptions.value = res.records
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载供应商失败'))
  } finally {
    supplierLoading.value = false
  }
}

function onSupplierSearch(keyword: string): void {
  void loadSuppliers(keyword)
}

async function loadCategoryTree(): Promise<void> {
  try {
    categoryOptions.value = await ceamsApi.masterdata.categoryTree({ type: 'MATERIAL' })
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载分类失败'))
  }
}

async function initSupplierOptions(row?: MaterialVO): Promise<void> {
  await loadSuppliers()
  if (row?.supplierId && row.supplierName && !supplierOptions.value.some((item) => item.id === row.supplierId)) {
    const fallback: ContactVO = { id: row.supplierId, name: row.supplierName, type: 'SUPPLIER' }
    supplierOptions.value = [fallback, ...supplierOptions.value]
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.masterdata.materialPage({
      page: page.value,
      size: size.value,
      keyword: query.keyword,
      categoryId: query.categoryId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载材料失败'))
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
    materialCode: '',
    materialName: '',
    specification: '',
    unit: '',
    currentPrice: '',
    supplierId: '',
    categoryId: ''
  })
  dialogVisible.value = true
  void initSupplierOptions()
  void loadCategoryTree()
}

function openEdit(row: MaterialVO): void {
  editingId.value = row.id
  form.materialCode = row.materialCode
  form.materialName = row.materialName
  form.specification = row.specification ?? ''
  form.unit = row.unit
  form.currentPrice = row.currentPrice ?? ''
  form.supplierId = row.supplierId ?? ''
  form.categoryId = row.categoryId ?? ''
  dialogVisible.value = true
  void initSupplierOptions(row)
  void loadCategoryTree()
}

function openDetail(row: MaterialVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.masterdata.materialUpdate(editingId.value, {
        materialName: form.materialName,
        specification: form.specification || null,
        unit: form.unit,
        currentPrice: form.currentPrice || null,
        supplierId: form.supplierId || null,
        categoryId: form.categoryId || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.masterdata.materialCreate({
        materialCode: form.materialCode,
        materialName: form.materialName,
        specification: form.specification || null,
        unit: form.unit,
        currentPrice: form.currentPrice || undefined,
        supplierId: form.supplierId || null,
        categoryId: form.categoryId || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: MaterialVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除材料「${row.materialName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.masterdata.materialRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(() => {
  void load()
  void loadSuppliers()
  void loadCategoryTree()
})
</script>

<template>
  <div>
    <!-- 筛选卡片：控件 12px 间距，查询按钮靠右 -->
    <div class="data-card filter-card">
      <el-input v-model="query.keyword" placeholder="关键字（编码/名称）" clearable style="width: 220px" />
      <el-tree-select
        v-model="query.categoryId"
        :data="categoryOptions"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        check-strictly
        clearable
        filterable
        placeholder="分类"
        style="width: 200px"
      />
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部标题、条数与主操作，表格四周 20px 内边距 -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">材料列表</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增材料</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="materialCode" label="编码" width="140" />
        <el-table-column prop="materialName" label="材料名称" min-width="160" />
        <el-table-column prop="specification" label="规格" min-width="140" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="当前价" width="120" align="right">
          <template #default="{ row }">
            <span v-if="(row as MaterialVO).currentPrice" class="amount">{{ (row as MaterialVO).currentPrice }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="供应商" width="160">
          <template #default="{ row }">{{ (row as MaterialVO).supplierName ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="分类" width="160">
          <template #default="{ row }">{{ (row as MaterialVO).categoryName ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as MaterialVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as MaterialVO)">编辑</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as MaterialVO)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无材料数据" />
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑材料' : '新增材料'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="材料编码" prop="materialCode">
          <el-input v-model="form.materialCode" :disabled="!!editingId" placeholder="请输入材料编码" />
        </el-form-item>
        <el-form-item label="材料名称" prop="materialName">
          <el-input v-model="form.materialName" placeholder="请输入材料名称" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.specification" placeholder="规格型号（可选）" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：吨 / 米 / 个" />
        </el-form-item>
        <el-form-item label="当前价">
          <el-input v-model="form.currentPrice" placeholder="金额（字符串，如 1280.50）" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select
            v-model="form.supplierId"
            filterable
            remote
            clearable
            :remote-method="onSupplierSearch"
            :loading="supplierLoading"
            placeholder="请选择供应商（可选）"
            style="width: 100%"
          >
            <el-option v-for="opt in supplierOptions" :key="opt.id" :label="opt.name" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="form.categoryId"
            :data="categoryOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            filterable
            default-expand-all
            placeholder="请选择分类（可选）"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="材料详情" width="540px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="编码">{{ detail.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.materialName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ detail.specification ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ detail.unit }}</el-descriptions-item>
        <el-descriptions-item label="当前价">{{ detail.currentPrice ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.supplierName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.categoryName ?? '-' }}</el-descriptions-item>
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
